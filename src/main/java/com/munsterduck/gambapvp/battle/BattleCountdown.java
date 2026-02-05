package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.network.BattleRequestPacket;
import net.minecraft.network.packet.s2c.play.ClearTitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages countdown for battles before they become active.
 * 5-second countdown with visual titles and sound effects.
 */
public class BattleCountdown {
    private static final Map<String, CountdownState> countdowns = new ConcurrentHashMap<>();

    private static class CountdownState {
        final String battleId;
        int ticksRemaining;
        int lastShownSecond; // Track which second we last displayed to avoid duplicates

        CountdownState(String battleId) {
            this.battleId = battleId;
            this.ticksRemaining = 5 * 20 + 1; // 5 seconds + 1 tick to show "5" immediately
            this.lastShownSecond = -1;
        }
    }

    /**
     * Start a countdown for a battle.
     */
    public static void startCountdown(String battleId) {
        countdowns.put(battleId, new CountdownState(battleId));
        GambaPVP.LOGGER.info("Started countdown for battle {}", battleId);
    }

    /**
     * Called every server tick to update all countdowns.
     */
    public static void tickCountdowns(MinecraftServer server) {
        List<String> completed = new ArrayList<>();

        for (CountdownState state : countdowns.values()) {
            BattleData battle = BattleManager.getBattle(state.battleId);
            if (battle == null) {
                completed.add(state.battleId);
                continue;
            }

            state.ticksRemaining--;

            if (state.ticksRemaining <= 0) {
                // Countdown complete - show FIGHT!
                showFightMessage(battle, server);
                battle.setCountdownComplete(true);
                completed.add(state.battleId);
                GambaPVP.LOGGER.info("Countdown complete for battle {}", state.battleId);
            } else {
                // Calculate current second (5, 4, 3, 2, 1)
                int currentSecond = (state.ticksRemaining + 19) / 20; // Ceiling division

                // Only show if we haven't shown this second yet
                if (currentSecond != state.lastShownSecond && currentSecond >= 1 && currentSecond <= 5) {
                    state.lastShownSecond = currentSecond;
                    showCountdownNumber(battle, currentSecond, server);
                }
            }
        }

        // Remove completed countdowns
        for (String battleId : completed) {
            countdowns.remove(battleId);
        }
    }

    /**
     * Display countdown number to all participants.
     */
    private static void showCountdownNumber(BattleData battle, int number, MinecraftServer server) {
        Formatting color = getColorForNumber(number);
        Text titleText = Text.literal(String.valueOf(number))
                .styled(s -> s.withColor(color).withBold(true));

        for (UUID playerUuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                // Set title timing (fade in, stay, fade out in ticks)
                // 0 fade in for instant appear, 18 stay, 0 fade out (next title will replace)
                player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 18, 0));
                player.networkHandler.sendPacket(new TitleS2CPacket(titleText));

                // Play sound
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER,
                        1.0f, getPitchForNumber(number));
            }
        }
    }

    /**
     * Display FIGHT! message when countdown completes.
     */
    private static void showFightMessage(BattleData battle, MinecraftServer server) {
        Text titleText = Text.literal("FIGHT!")
                .styled(s -> s.withColor(Formatting.RED).withBold(true));

        for (UUID playerUuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                player.networkHandler.sendPacket(new TitleFadeS2CPacket(0, 30, 10));
                player.networkHandler.sendPacket(new TitleS2CPacket(titleText));

                // Play dragon growl sound for dramatic effect
                player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.MASTER, 0.5f, 1.0f);
            }
        }
    }

    /**
     * Get color based on countdown number.
     * 5,4 = Green, 3,2 = Yellow, 1 = Red
     */
    private static Formatting getColorForNumber(int number) {
        if (number >= 4) {
            return Formatting.GREEN;
        } else if (number >= 2) {
            return Formatting.YELLOW;
        } else {
            return Formatting.RED;
        }
    }

    /**
     * Get pitch for countdown sound, ascending as number decreases.
     */
    private static float getPitchForNumber(int number) {
        return switch (number) {
            case 5 -> 0.5f;
            case 4 -> 0.7f;
            case 3 -> 0.9f;
            case 2 -> 1.1f;
            case 1 -> 1.3f;
            default -> 1.0f;
        };
    }

    /**
     * Cancel a countdown for a battle.
     */
    public static void cancelCountdown(String battleId) {
        countdowns.remove(battleId);
    }

    /**
     * Check if a battle is in countdown.
     */
    public static boolean isInCountdown(String battleId) {
        return countdowns.containsKey(battleId);
    }
}

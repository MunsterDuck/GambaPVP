package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.util.ArenaManager;
import com.munsterduck.gambapvp.util.CurrencyHelper;
import com.munsterduck.gambapvp.util.InventoryManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Handles battle end logic including cleanup, restoration, and wager distribution.
 */
public class BattleEndHandler {

    /**
     * End a battle with a winner.
     */
    public static void endBattle(String battleId, UUID winnerId, MinecraftServer server) {
        BattleData battle = BattleManager.getBattle(battleId);
        if (battle == null) {
            GambaPVP.LOGGER.warn("Tried to end non-existent battle: {}", battleId);
            return;
        }

        GambaPVP.LOGGER.info("Ending battle {} with winner {}", battleId, winnerId);

        // 1. Hide HUD for all participants
        BattleHudManager.hideHudForBattle(battle, server);

        // 2. Cancel any ongoing countdown
        BattleCountdown.cancelCountdown(battleId);

        // 3. Clean up placed blocks and tracked entities (crystals, items, etc.)
        cleanupPlacedBlocks(battle, server);
        cleanupBattleEntities(battle, server);

        // 4. Restore inventories if kit was used
        if (battle.getKitName() != null && !battle.getKitName().isEmpty()) {
            restoreInventories(battle, server);
        }

        // 5. Teleport back to original positions if arena was used
        if (battle.hasArena()) {
            teleportToOriginalPositions(battle, server);
        }

        // 6. Distribute winnings
        Map<UUID, WagerData> distribution = distributeWinnings(battle, winnerId, server);

        // 7. Show battle results
        showBattleResults(battle, winnerId, distribution, server);

        // 8. Remove battle from manager
        BattleManager.endBattle(battleId, server);
    }

    /**
     * Cancel a battle without a winner (return all wagers).
     */
    public static void cancelBattle(String battleId, MinecraftServer server, String reason) {
        BattleData battle = BattleManager.getBattle(battleId);
        if (battle == null) {
            return;
        }

        GambaPVP.LOGGER.info("Cancelling battle {} - reason: {}", battleId, reason);

        // Hide HUD
        BattleHudManager.hideHudForBattle(battle, server);

        // Cancel countdown
        BattleCountdown.cancelCountdown(battleId);

        // Clean up placed blocks and tracked entities
        cleanupPlacedBlocks(battle, server);
        cleanupBattleEntities(battle, server);

        // Restore inventories
        if (battle.getKitName() != null && !battle.getKitName().isEmpty()) {
            restoreInventories(battle, server);
        }

        // Teleport back
        if (battle.hasArena()) {
            teleportToOriginalPositions(battle, server);
        }

        // Return all wagers
        returnAllWagers(battle, server);

        // Notify players
        for (UUID uuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Text.literal("Battle cancelled: " + reason)
                        .styled(s -> s.withColor(0xFF5555)), false);
            }
        }

        // Remove battle
        BattleManager.endBattle(battleId, server);
    }

    /**
     * Clean up all blocks placed during the battle.
     */
    private static void cleanupPlacedBlocks(BattleData battle, MinecraftServer server) {
        Set<BlockPos> placedBlocks = battle.getPlacedBlocks();
        if (placedBlocks.isEmpty()) {
            return;
        }

        ServerWorld world = null;
        if (battle.getBattleWorld() != null) {
            world = server.getWorld(battle.getBattleWorld());
        }

        if (world == null) {
            // Try to get world from first online participant
            for (UUID uuid : battle.getParticipants()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null) {
                    world = player.getServerWorld();
                    break;
                }
            }
        }

        if (world != null) {
            for (BlockPos pos : placedBlocks) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }
            GambaPVP.LOGGER.debug("Cleaned up {} placed blocks", placedBlocks.size());
        }
    }

    /**
     * Remove all tracked entities (end crystals, dropped items, undetonated TNT, etc.)
     */
    private static void cleanupBattleEntities(BattleData battle, MinecraftServer server) {
        Set<UUID> entityUuids = battle.getPlacedEntityUuids();
        if (entityUuids.isEmpty()) {
            return;
        }

        ServerWorld world = null;
        if (battle.getBattleWorld() != null) {
            world = server.getWorld(battle.getBattleWorld());
        }

        if (world == null) {
            for (UUID uuid : battle.getParticipants()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null) {
                    world = player.getServerWorld();
                    break;
                }
            }
        }

        if (world != null) {
            int removed = 0;
            for (UUID entityUuid : entityUuids) {
                Entity entity = world.getEntity(entityUuid);
                if (entity != null) {
                    entity.discard();
                    removed++;
                }
            }
            GambaPVP.LOGGER.debug("Cleaned up {} tracked battle entities", removed);
        }
    }

    /**
     * Restore inventories for all participants.
     */
    private static void restoreInventories(BattleData battle, MinecraftServer server) {
        for (UUID uuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                InventoryManager.restoreInventoryFromBackup(player);
            }
        }
    }

    /**
     * Teleport all participants back to their original positions.
     */
    private static void teleportToOriginalPositions(BattleData battle, MinecraftServer server) {
        for (UUID uuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            BattleData.OriginalPosition pos = battle.getOriginalPosition(uuid);
            if (player != null && pos != null) {
                ArenaManager.returnPlayerToOriginalPosition(player, pos, server);
            }
        }
    }

    /**
     * Distribute winnings using poker-style matching.
     * Currency was already deducted at battle start.
     * Winner only receives matched amounts; losers get unmatched amounts refunded.
     */
    private static Map<UUID, WagerData> distributeWinnings(BattleData battle, UUID winnerId, MinecraftServer server) {
        if (!battle.hasWagers()) {
            return Collections.emptyMap();
        }

        WagerPot pot = battle.getWagerPot();
        Map<UUID, WagerData> distribution = pot.distributeWinnings(winnerId);

        // Give each participant their distribution (items + currency)
        for (Map.Entry<UUID, WagerData> entry : distribution.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) continue;

            WagerData payout = entry.getValue();

            // Give items
            for (ItemStack stack : payout.getItems()) {
                if (!player.getInventory().insertStack(stack.copy())) {
                    player.dropItem(stack.copy(), false);
                }
            }

            // Give currency
            if (payout.getCurrency() > 0) {
                CurrencyHelper.deposit(player, payout.getCurrency());
                if (entry.getKey().equals(winnerId)) {
                    player.sendMessage(Text.literal("You won " + payout.getCurrency() + " coins!")
                            .styled(s -> s.withColor(0x55FF55).withBold(true)), false);
                } else {
                    player.sendMessage(Text.literal(payout.getCurrency() + " coins returned (unmatched).")
                            .styled(s -> s.withColor(0xFFAA00)), false);
                }
            }
        }

        return distribution;
    }

    /**
     * Return all wagers when battle is cancelled.
     * Currency was already deducted at battle start, so we need to return it.
     */
    private static void returnAllWagers(BattleData battle, MinecraftServer server) {
        if (!battle.hasWagers()) {
            return;
        }

        WagerPot pot = battle.getWagerPot();
        Map<UUID, WagerData> returns = pot.returnAllWagers();

        for (Map.Entry<UUID, WagerData> entry : returns.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) continue;

            WagerData wager = entry.getValue();

            // Give items back
            for (ItemStack stack : wager.getItems()) {
                if (!player.getInventory().insertStack(stack.copy())) {
                    player.dropItem(stack.copy(), false);
                }
            }

            // Return currency (was deducted at battle start)
            if (wager.getCurrency() > 0) {
                CurrencyHelper.deposit(player, wager.getCurrency());
            }

            player.sendMessage(Text.literal("Your wager has been returned.")
                    .styled(s -> s.withColor(0xFFAA00)), false);
        }
    }

    /**
     * Show battle results to all participants (Hypixel-style).
     */
    private static void showBattleResults(BattleData battle, UUID winnerId,
                                           Map<UUID, WagerData> distribution, MinecraftServer server) {
        ServerPlayerEntity winnerPlayer = server.getPlayerManager().getPlayer(winnerId);
        String winnerName = winnerPlayer != null ? winnerPlayer.getName().getString() : "Unknown";
        String divider = "\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC";

        Map<UUID, Integer> scores = battle.getScores();
        int winsRequired = battle.getWinsRequired();
        String kitName = battle.getKitName();

        // Build the results message for each player
        for (UUID uuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) continue;

            boolean isWinner = uuid.equals(winnerId);

            MutableText msg = Text.empty()
                .append(Text.literal(divider + "\n").styled(s -> s.withColor(0xFFAA00).withStrikethrough(true)));

            // Title
            if (kitName != null && !kitName.isEmpty()) {
                msg.append(Text.literal("  Duel - " + kitName + "\n").styled(s -> s.withColor(0xFFAA00).withBold(true)));
            } else {
                msg.append(Text.literal("  Duel\n").styled(s -> s.withColor(0xFFAA00).withBold(true)));
            }
            msg.append(Text.literal("\n"));

            // Winner announcement
            msg.append(Text.literal("  Winner: ").styled(s -> s.withColor(0xAAAAAA)));
            msg.append(Text.literal(winnerName + "\n").styled(s -> s.withColor(0x55FF55).withBold(true)));
            msg.append(Text.literal("\n"));

            // Score display with pips for each player
            for (UUID pUuid : battle.getParticipants()) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(pUuid);
                String pName = p != null ? p.getName().getString() : pUuid.toString().substring(0, 8);
                int score = scores.getOrDefault(pUuid, 0);
                boolean pIsWinner = pUuid.equals(winnerId);

                int nameColor = pIsWinner ? 0x55FF55 : 0xFF5555;
                msg.append(Text.literal("  ").styled(s -> s.withColor(0xAAAAAA)));
                msg.append(Text.literal(pName + " ").styled(s -> s.withColor(nameColor)));

                // Score pips
                for (int i = 0; i < winsRequired; i++) {
                    boolean filled = i < score;
                    int pipColor = filled ? (pIsWinner ? 0x55FF55 : 0xFFAA00) : 0x444444;
                    msg.append(Text.literal(filled ? "\u25CF" : "\u25CB").styled(s -> s.withColor(pipColor)));
                }

                msg.append(Text.literal(" " + score + "/" + winsRequired + "\n")
                    .styled(s -> s.withColor(0x888888)));
            }

            // Wager results
            if (battle.hasWagers()) {
                WagerPot pot = battle.getWagerPot();
                WagerData myOriginalWager = pot.getPlayerWager(uuid);
                WagerData myPayout = distribution.get(uuid);

                msg.append(Text.literal("\n"));
                if (isWinner) {
                    // Show winnings
                    if (myPayout != null && !myPayout.isEmpty()) {
                        msg.append(Text.literal("  \u2B50 Winnings: ").styled(s -> s.withColor(0xFFAA00).withBold(true)));
                        if (myPayout.getCurrency() > 0) {
                            msg.append(Text.literal("+" + myPayout.getCurrency() + " coins").styled(s -> s.withColor(0x55FF55)));
                        }
                        if (!myPayout.getItems().isEmpty()) {
                            if (myPayout.getCurrency() > 0) {
                                msg.append(Text.literal(" + ").styled(s -> s.withColor(0xAAAAAA)));
                            }
                            msg.append(Text.literal(myPayout.getItems().size() + " item" + (myPayout.getItems().size() > 1 ? "s" : ""))
                                .styled(s -> s.withColor(0x55FF55)));
                        }
                        msg.append(Text.literal("\n"));
                    }
                } else {
                    // Show losses
                    int lostCurrency = myOriginalWager.getCurrency();
                    int returnedCurrency = myPayout != null ? myPayout.getCurrency() : 0;
                    int netLoss = lostCurrency - returnedCurrency;

                    if (netLoss > 0) {
                        msg.append(Text.literal("  \u2620 Lost: ").styled(s -> s.withColor(0xFF5555).withBold(true)));
                        msg.append(Text.literal("-" + netLoss + " coins").styled(s -> s.withColor(0xFF5555)));
                        msg.append(Text.literal("\n"));
                    }
                    if (returnedCurrency > 0) {
                        msg.append(Text.literal("  \u21B5 Returned: ").styled(s -> s.withColor(0xAAAAAA)));
                        msg.append(Text.literal(returnedCurrency + " coins (unmatched)").styled(s -> s.withColor(0xFFAA00)));
                        msg.append(Text.literal("\n"));
                    }
                    if (myPayout != null && !myPayout.getItems().isEmpty()) {
                        msg.append(Text.literal("  \u21B5 Returned: ").styled(s -> s.withColor(0xAAAAAA)));
                        msg.append(Text.literal(myPayout.getItems().size() + " item" + (myPayout.getItems().size() > 1 ? "s" : "") + " (unmatched)")
                            .styled(s -> s.withColor(0xFFAA00)));
                        msg.append(Text.literal("\n"));
                    }
                }
            }

            // Personal result
            msg.append(Text.literal("\n"));
            if (isWinner) {
                msg.append(Text.literal("  VICTORY!\n").styled(s -> s.withColor(0x55FF55).withBold(true)));
            } else {
                msg.append(Text.literal("  DEFEAT\n").styled(s -> s.withColor(0xFF5555).withBold(true)));
            }

            msg.append(Text.literal(divider).styled(s -> s.withColor(0xFFAA00).withStrikethrough(true)));

            player.sendMessage(msg, false);
        }
    }
}

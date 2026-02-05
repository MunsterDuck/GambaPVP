package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.GambaPVP;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Handles battle-related events such as block protection and disconnect handling.
 * Note: Death handling is done via BattleDeathPreventMixin -> BattleDeathHandler.
 */
public class BattleEventHandler {

    /**
     * Register all event handlers.
     */
    public static void register() {
        // Register block break protection
        PlayerBlockBreakEvents.BEFORE.register(BattleEventHandler::onBlockBreak);
    }

    /**
     * Handle block break events - only allow breaking blocks placed during battle.
     */
    private static boolean onBlockBreak(World world, PlayerEntity player, BlockPos pos,
                                         BlockState state, BlockEntity blockEntity) {
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        BattleData battle = BattleManager.getPlayerBattle(serverPlayer.getUuid());
        if (battle == null || !battle.isActive()) {
            return true; // Not in battle, allow normal behavior
        }

        // Only allow breaking blocks that were placed during the battle
        if (!battle.getPlacedBlocks().contains(pos)) {
            serverPlayer.sendMessage(Text.literal("You can only break blocks placed during the battle!")
                    .styled(s -> s.withColor(0xFF5555)), true);
            return false;
        }

        return true;
    }

    /**
     * Handle a player leaving during battle (disconnect).
     */
    public static void onPlayerDisconnect(ServerPlayerEntity player, MinecraftServer server) {
        BattleData battle = BattleManager.getPlayerBattle(player.getUuid());
        if (battle == null || !battle.isActive()) {
            return;
        }

        // Player disconnected during battle - treat as forfeit
        GambaPVP.LOGGER.info("Player {} disconnected during battle {}", player.getName().getString(), battle.getBattleId());

        // Find remaining opponent to be winner (any other participant wins by forfeit)
        UUID winnerId = null;
        for (UUID uuid : battle.getParticipants()) {
            if (!uuid.equals(player.getUuid())) {
                winnerId = uuid;
                break;
            }
        }

        if (winnerId != null) {
            // Notify remaining players
            for (UUID uuid : battle.getParticipants()) {
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
                if (p != null) {
                    p.sendMessage(Text.literal(player.getName().getString() + " disconnected! Battle ending...")
                            .styled(s -> s.withColor(0xFFAA00)), false);
                }
            }
            BattleEndHandler.endBattle(battle.getBattleId(), winnerId, server);
        } else {
            // No valid winner, just end the battle and return wagers
            BattleEndHandler.cancelBattle(battle.getBattleId(), server, "All opponents disconnected");
        }
    }
}

package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.util.ArenaManager;
import com.munsterduck.gambapvp.util.InventoryManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

        // 3. Clean up placed blocks
        cleanupPlacedBlocks(battle, server);

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

        // Clean up placed blocks
        cleanupPlacedBlocks(battle, server);

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
     * Distribute winnings to the winner and return unmatched amounts to losers.
     */
    private static Map<UUID, WagerData> distributeWinnings(BattleData battle, UUID winnerId, MinecraftServer server) {
        if (!battle.hasWagers()) {
            return Collections.emptyMap();
        }

        WagerPot pot = battle.getWagerPot();
        Map<UUID, WagerData> distribution = pot.distributeWinnings(winnerId);

        for (Map.Entry<UUID, WagerData> entry : distribution.entrySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) continue;

            WagerData wager = entry.getValue();

            // Give items
            for (ItemStack stack : wager.getItems()) {
                if (!player.getInventory().insertStack(stack.copy())) {
                    // Drop on ground if inventory full
                    player.dropItem(stack.copy(), false);
                }
            }

            // Give currency if NotchCurrency is available
            if (wager.getCurrency() > 0) {
                giveCurrency(player, wager.getCurrency());
            }
        }

        return distribution;
    }

    /**
     * Return all wagers when battle is cancelled.
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

            // Give currency back
            if (wager.getCurrency() > 0) {
                giveCurrency(player, wager.getCurrency());
            }

            player.sendMessage(Text.literal("Your wager has been returned.")
                    .styled(s -> s.withColor(0xFFAA00)), false);
        }
    }

    /**
     * Give currency using NotchCurrency if available.
     */
    private static void giveCurrency(ServerPlayerEntity player, int amount) {
        if (!FabricLoader.getInstance().isModLoaded("notchcurrency")) {
            GambaPVP.LOGGER.warn("NotchCurrency not available, cannot give {} currency to {}",
                    amount, player.getName().getString());
            return;
        }

        try {
            Class<?> currencyApiClass = Class.forName("com.tronbyte.notchcurrency.CurrencyApi");
            currencyApiClass.getMethod("addBalance", net.minecraft.server.network.ServerPlayerEntity.class, int.class)
                    .invoke(null, player, amount);
        } catch (Exception e) {
            GambaPVP.LOGGER.error("Failed to give currency via NotchCurrency", e);
        }
    }

    /**
     * Show battle results to all participants.
     */
    private static void showBattleResults(BattleData battle, UUID winnerId,
                                           Map<UUID, WagerData> distribution, MinecraftServer server) {
        ServerPlayerEntity winner = server.getPlayerManager().getPlayer(winnerId);
        String winnerName = winner != null ? winner.getName().getString() : "Unknown";

        // Build results message
        StringBuilder stats = new StringBuilder();
        stats.append("\n");
        stats.append("=== Battle Results ===\n");
        stats.append("Winner: ").append(winnerName).append("\n");
        stats.append("\nScores:\n");

        Map<UUID, Integer> scores = battle.getScores();
        for (UUID uuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            String name = player != null ? player.getName().getString() : uuid.toString().substring(0, 8);
            int score = scores.getOrDefault(uuid, 0);
            stats.append("  ").append(name).append(": ").append(score).append("\n");
        }

        if (!distribution.isEmpty()) {
            stats.append("\nWinnings:\n");
            WagerData winnerWager = distribution.get(winnerId);
            if (winnerWager != null) {
                if (!winnerWager.getItems().isEmpty()) {
                    stats.append("  Items: ").append(winnerWager.getItems().size()).append(" stacks\n");
                }
                if (winnerWager.getCurrency() > 0) {
                    stats.append("  Currency: ").append(winnerWager.getCurrency()).append("\n");
                }
            }
        }

        stats.append("======================");

        Text resultsText = Text.literal(stats.toString())
                .styled(s -> s.withColor(Formatting.GOLD));

        // Send to all participants
        for (UUID uuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                if (uuid.equals(winnerId)) {
                    player.sendMessage(Text.literal("VICTORY!")
                            .styled(s -> s.withColor(Formatting.GREEN).withBold(true)), false);
                } else {
                    player.sendMessage(Text.literal("DEFEAT")
                            .styled(s -> s.withColor(Formatting.RED).withBold(true)), false);
                }
                player.sendMessage(resultsText, false);
            }
        }
    }
}

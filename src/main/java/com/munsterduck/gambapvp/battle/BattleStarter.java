package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.util.ArenaManager;
import com.munsterduck.gambapvp.util.InventoryManager;
import com.munsterduck.gambapvp.util.PendingDuelManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.*;

/**
 * Orchestrates the start of a battle.
 * Handles creating battle, transferring wagers, teleporting, applying kits, and starting countdown.
 */
public class BattleStarter {

    /**
     * Start a battle from a pending session.
     */
    public static void startBattle(PendingBattleSession session, MinecraftServer server) {
        PendingDuelManager.DuelRequest request = session.getOriginalRequest();
        Set<UUID> acceptedPlayers = session.getAcceptedPlayers();
        List<UUID> participants = new ArrayList<>(acceptedPlayers);

        GambaPVP.LOGGER.info("Starting battle with {} participants: kit={}, arena={}, winsRequired={}",
                participants.size(), request.kitName, request.arenaId, request.winsRequired);

        // Determine battle world from request coordinates
        RegistryKey<World> battleWorld = null;
        ArenaManager.ArenaLocation arenaLocation = null;
        if (request.hasArenaCoordinates()) {
            battleWorld = RegistryKey.of(RegistryKeys.WORLD, new Identifier(request.arenaDimension));
            arenaLocation = new ArenaManager.ArenaLocation(
                    request.arenaX, request.arenaY, request.arenaZ,
                    request.arenaYaw, request.arenaPitch, battleWorld
            );
            GambaPVP.LOGGER.info("Arena location: {}, {}, {} in {}", request.arenaX, request.arenaY, request.arenaZ, request.arenaDimension);
        }

        // Create the battle
        String battleId = BattleManager.createBattle(
                participants,
                request.kitName,
                request.winsRequired,
                request.keepInventory,
                request.arenaId,
                battleWorld
        );

        BattleData battle = BattleManager.getBattle(battleId);
        if (battle == null) {
            GambaPVP.LOGGER.error("Failed to create battle");
            notifyError(participants, server, "Failed to create battle");
            return;
        }

        // Transfer wagers from pending manager to battle
        Map<UUID, WagerData> wagers = PendingDuelManager.getAllWagers(request.requestId);
        for (Map.Entry<UUID, WagerData> entry : wagers.entrySet()) {
            battle.addWager(entry.getKey(), entry.getValue());
        }

        // Calculate spawn points for arena if applicable
        ArenaManager.ArenaLocation[] spawnPoints = null;
        if (arenaLocation != null) {
            spawnPoints = calculateSpawnPoints(arenaLocation, participants.size());
        }

        // Process each participant
        int playerIndex = 0;
        for (UUID playerUuid : participants) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player == null) {
                GambaPVP.LOGGER.warn("Player {} is offline, skipping", playerUuid);
                continue;
            }

            // Save original position if we're teleporting
            if (spawnPoints != null) {
                battle.saveOriginalPosition(playerUuid, BattleData.OriginalPosition.fromPlayer(player));
            }

            // Apply kit if specified
            if (request.kitName != null && !request.kitName.isEmpty()) {
                boolean kitLoaded = InventoryManager.loadKitForBattle(player, request.kitName, server);
                if (!kitLoaded) {
                    player.sendMessage(Text.literal("Warning: Failed to load kit '" + request.kitName + "'")
                            .styled(s -> s.withColor(0xFFAA00)), false);
                }
            }

            // Teleport to arena if applicable
            if (spawnPoints != null && playerIndex < spawnPoints.length) {
                ArenaManager.teleportPlayerToArena(player, spawnPoints[playerIndex], server);
                // Save arena spawn for respawning
                battle.saveArenaSpawn(playerUuid, new BattleData.OriginalPosition(
                        spawnPoints[playerIndex].x,
                        spawnPoints[playerIndex].y,
                        spawnPoints[playerIndex].z,
                        spawnPoints[playerIndex].yaw,
                        spawnPoints[playerIndex].pitch,
                        spawnPoints[playerIndex].dimension
                ));
            }

            // Notify player
            player.sendMessage(Text.literal("Battle starting! Get ready...")
                    .styled(s -> s.withColor(0x55FF55).withBold(true)), false);

            playerIndex++;
        }

        // Clear pending wagers
        PendingDuelManager.clearWagers(request.requestId);

        // Show HUD immediately
        BattleHudManager.showHudForBattle(battle, server);

        // Start countdown
        BattleCountdown.startCountdown(battleId);

        GambaPVP.LOGGER.info("Battle {} started with {} participants", battleId, participants.size());
    }

    /**
     * Start a battle directly from request data (for 1v1 immediate accept).
     */
    public static void startBattleDirect(UUID initiatorUuid, UUID acceptorUuid,
                                          PendingDuelManager.DuelRequest request,
                                          MinecraftServer server) {
        List<UUID> participants = Arrays.asList(initiatorUuid, acceptorUuid);

        GambaPVP.LOGGER.info("Starting direct 1v1 battle: {} vs {}", initiatorUuid, acceptorUuid);

        // Determine battle world from request coordinates
        RegistryKey<World> battleWorld = null;
        ArenaManager.ArenaLocation arenaLocation = null;
        if (request.hasArenaCoordinates()) {
            battleWorld = RegistryKey.of(RegistryKeys.WORLD, new Identifier(request.arenaDimension));
            arenaLocation = new ArenaManager.ArenaLocation(
                    request.arenaX, request.arenaY, request.arenaZ,
                    request.arenaYaw, request.arenaPitch, battleWorld
            );
            GambaPVP.LOGGER.info("Arena location: {}, {}, {} in {}", request.arenaX, request.arenaY, request.arenaZ, request.arenaDimension);
        }

        // Create the battle
        String battleId = BattleManager.createBattle(
                participants,
                request.kitName,
                request.winsRequired,
                request.keepInventory,
                request.arenaId,
                battleWorld
        );

        BattleData battle = BattleManager.getBattle(battleId);
        if (battle == null) {
            GambaPVP.LOGGER.error("Failed to create battle");
            notifyError(participants, server, "Failed to create battle");
            return;
        }

        // Transfer wagers
        Map<UUID, WagerData> wagers = PendingDuelManager.getAllWagers(request.requestId);
        for (Map.Entry<UUID, WagerData> entry : wagers.entrySet()) {
            battle.addWager(entry.getKey(), entry.getValue());
        }

        // Calculate spawn points
        ArenaManager.ArenaLocation[] spawnPoints = null;
        if (arenaLocation != null) {
            spawnPoints = calculateSpawnPoints(arenaLocation, 2);
        }

        // Process each participant
        int index = 0;
        for (UUID playerUuid : participants) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player == null) continue;

            // Save original position
            if (spawnPoints != null) {
                battle.saveOriginalPosition(playerUuid, BattleData.OriginalPosition.fromPlayer(player));
            }

            // Apply kit
            if (request.kitName != null && !request.kitName.isEmpty()) {
                InventoryManager.loadKitForBattle(player, request.kitName, server);
            }

            // Teleport to arena
            if (spawnPoints != null && index < spawnPoints.length) {
                ArenaManager.teleportPlayerToArena(player, spawnPoints[index], server);
                // Save arena spawn for respawning
                battle.saveArenaSpawn(playerUuid, new BattleData.OriginalPosition(
                        spawnPoints[index].x,
                        spawnPoints[index].y,
                        spawnPoints[index].z,
                        spawnPoints[index].yaw,
                        spawnPoints[index].pitch,
                        spawnPoints[index].dimension
                ));
            }

            player.sendMessage(Text.literal("Battle starting! Get ready...")
                    .styled(s -> s.withColor(0x55FF55).withBold(true)), false);

            index++;
        }

        // Clear pending wagers
        PendingDuelManager.clearWagers(request.requestId);

        // Show HUD immediately
        BattleHudManager.showHudForBattle(battle, server);

        // Start countdown
        BattleCountdown.startCountdown(battleId);
    }

    private static void notifyError(List<UUID> players, MinecraftServer server, String message) {
        for (UUID uuid : players) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Text.literal(message).styled(s -> s.withColor(0xFF5555)), false);
            }
        }
    }

    /**
     * Calculate spawn points for multiple players in an arena.
     * Offsets players so they don't spawn on top of each other.
     */
    private static ArenaManager.ArenaLocation[] calculateSpawnPoints(ArenaManager.ArenaLocation baseLocation, int playerCount) {
        ArenaManager.ArenaLocation[] spawns = new ArenaManager.ArenaLocation[playerCount];
        for (int i = 0; i < playerCount; i++) {
            // Offset players so they face each other
            double offsetX = (i % 2 == 0 ? 3 : -3);
            float yaw = i % 2 == 0 ? baseLocation.yaw : baseLocation.yaw + 180;
            spawns[i] = new ArenaManager.ArenaLocation(
                    baseLocation.x + offsetX,
                    baseLocation.y,
                    baseLocation.z,
                    yaw,
                    baseLocation.pitch,
                    baseLocation.dimension
            );
        }
        return spawns;
    }
}

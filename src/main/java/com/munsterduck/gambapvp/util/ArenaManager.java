package com.munsterduck.gambapvp.util;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.battle.BattleData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;


/**
 * Manages arena teleportation and position restoration.
 * Integrates with LocationTooltip mod when available.
 */
public class ArenaManager {

    /**
     * Arena location data.
     */
    public static class ArenaLocation {
        public final double x, y, z;
        public final float yaw, pitch;
        public final RegistryKey<World> dimension;

        public ArenaLocation(double x, double y, double z, float yaw, float pitch, RegistryKey<World> dimension) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.dimension = dimension;
        }
    }

    /**
     * Get arena location by ID using LocationTooltip integration.
     * Returns null if arena not found or LocationTooltip not installed.
     */
    public static ArenaLocation getArenaLocation(String arenaId, MinecraftServer server) {
        if (arenaId == null || arenaId.isEmpty()) {
            return null;
        }

        if (!FabricLoader.getInstance().isModLoaded("locationtooltip")) {
            GambaPVP.LOGGER.warn("LocationTooltip mod not loaded, cannot get arena location");
            return null;
        }

        try {
            // Access LocationTooltip's LocationManager via reflection
            Class<?> locationManagerClass = Class.forName("net.tronbyte.locationtooltip.LocationManager");
            Object locationManager = locationManagerClass.getMethod("getInstance").invoke(null);

            // Get the location by name
            Object location = locationManagerClass.getMethod("getLocation", String.class)
                    .invoke(locationManager, arenaId);

            if (location == null) {
                GambaPVP.LOGGER.warn("Arena '{}' not found in LocationTooltip", arenaId);
                return null;
            }

            // Extract coordinates from the location object
            Class<?> locationClass = location.getClass();
            double x = (double) locationClass.getMethod("getX").invoke(location);
            double y = (double) locationClass.getMethod("getY").invoke(location);
            double z = (double) locationClass.getMethod("getZ").invoke(location);
            float yaw = ((Number) locationClass.getMethod("getYaw").invoke(location)).floatValue();
            float pitch = ((Number) locationClass.getMethod("getPitch").invoke(location)).floatValue();
            String worldId = (String) locationClass.getMethod("getWorld").invoke(location);

            RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, new Identifier(worldId));

            return new ArenaLocation(x, y, z, yaw, pitch, dimension);
        } catch (Exception e) {
            GambaPVP.LOGGER.error("Failed to get arena location via LocationTooltip", e);
            return null;
        }
    }

    /**
     * Teleport a player to an arena location.
     * Uses safe teleportation to prevent "moved too fast" warnings.
     * @return true if successful
     */
    public static boolean teleportPlayerToArena(ServerPlayerEntity player, ArenaLocation arena, MinecraftServer server) {
        if (arena == null) {
            return false;
        }

        try {
            ServerWorld targetWorld = server.getWorld(arena.dimension);
            if (targetWorld == null) {
                GambaPVP.LOGGER.error("Target world {} not found", arena.dimension);
                return false;
            }

            // Reset velocity before teleport to prevent "moved too fast" warnings
            player.setVelocity(0, 0, 0);
            player.velocityModified = true;

            player.teleport(targetWorld, arena.x, arena.y, arena.z, arena.yaw, arena.pitch);

            // Reset velocity after teleport and sync position
            player.setVelocity(0, 0, 0);
            player.velocityModified = true;
            player.networkHandler.syncWithPlayerPosition();

            return true;
        } catch (Exception e) {
            GambaPVP.LOGGER.error("Failed to teleport player to arena", e);
            return false;
        }
    }

    /**
     * Return a player to their original position.
     * Uses safe teleportation to prevent "moved too fast" warnings.
     * @return true if successful
     */
    public static boolean returnPlayerToOriginalPosition(ServerPlayerEntity player,
                                                          BattleData.OriginalPosition pos,
                                                          MinecraftServer server) {
        if (pos == null) {
            return false;
        }

        try {
            ServerWorld targetWorld = server.getWorld(pos.dimension);
            if (targetWorld == null) {
                GambaPVP.LOGGER.error("Original world {} not found", pos.dimension);
                return false;
            }

            // Reset velocity before teleport to prevent "moved too fast" warnings
            player.setVelocity(0, 0, 0);
            player.velocityModified = true;

            player.teleport(targetWorld, pos.x, pos.y, pos.z, pos.yaw, pos.pitch);

            // Reset velocity after teleport and sync position
            player.setVelocity(0, 0, 0);
            player.velocityModified = true;
            player.networkHandler.syncWithPlayerPosition();

            return true;
        } catch (Exception e) {
            GambaPVP.LOGGER.error("Failed to return player to original position", e);
            return false;
        }
    }

    /**
     * Get spawn points for multiple players in an arena.
     * For now, returns the same location for all players.
     * Could be extended to support multiple spawn points per arena.
     */
    public static ArenaLocation[] getArenaSpawnPoints(String arenaId, int playerCount, MinecraftServer server) {
        ArenaLocation baseLocation = getArenaLocation(arenaId, server);
        if (baseLocation == null) {
            return null;
        }

        ArenaLocation[] spawns = new ArenaLocation[playerCount];
        for (int i = 0; i < playerCount; i++) {
            // Offset players slightly so they don't spawn on top of each other
            double offsetX = (i % 2 == 0 ? 2 : -2) * ((i / 2) + 1);
            spawns[i] = new ArenaLocation(
                    baseLocation.x + offsetX,
                    baseLocation.y,
                    baseLocation.z,
                    baseLocation.yaw + (i % 2 == 0 ? 0 : 180), // Face each other
                    baseLocation.pitch,
                    baseLocation.dimension
            );
        }
        return spawns;
    }
}

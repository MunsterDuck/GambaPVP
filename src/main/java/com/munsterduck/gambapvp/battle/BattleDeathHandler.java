package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.util.ArenaManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Handles "fake deaths" during battles.
 * Instead of actually dying (which causes inventory drops, tombstones, etc.),
 * we reset the player's health and teleport them back to their spawn.
 */
public class BattleDeathHandler {

    /**
     * Handle a player being "killed" during a battle.
     * This doesn't actually kill them - it resets their state and records the kill.
     */
    public static void handleBattleDeath(ServerPlayerEntity player, DamageSource source, BattleData battle) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        UUID killedUuid = player.getUuid();

        // Find the killer
        UUID killerUuid = null;
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            if (battle.getParticipants().contains(attacker.getUuid())) {
                killerUuid = attacker.getUuid();
            }
        }

        // If no valid killer found, pick a random opponent
        if (killerUuid == null) {
            for (UUID uuid : battle.getParticipants()) {
                if (!uuid.equals(killedUuid)) {
                    killerUuid = uuid;
                    break;
                }
            }
        }

        if (killerUuid == null) {
            GambaPVP.LOGGER.warn("Could not determine killer for battle death");
            return;
        }

        // Record the win
        battle.recordWin(killerUuid);

        // Get player names for messages
        ServerPlayerEntity killer = server.getPlayerManager().getPlayer(killerUuid);
        String killerName = killer != null ? killer.getName().getString() : "Unknown";
        String killedName = player.getName().getString();

        // Play death effects for the "killed" player
        playDeathEffects(player);

        // Reset the "killed" player
        resetPlayer(player);

        // Teleport back to arena spawn
        teleportToArenaSpawn(player, battle, server);

        // Update HUD
        BattleHudManager.updateScore(battle, killerUuid, server);

        // Notify all participants
        for (UUID participantUuid : battle.getParticipants()) {
            ServerPlayerEntity participant = server.getPlayerManager().getPlayer(participantUuid);
            if (participant != null) {
                participant.sendMessage(Text.literal(killerName + " killed " + killedName + "! (" +
                        battle.getScore(killerUuid) + "/" + battle.getWinsRequired() + ")")
                        .styled(s -> s.withColor(0x55FF55)), false);
            }
        }

        // Check if there's a winner
        if (battle.hasWinner()) {
            GambaPVP.LOGGER.info("Battle {} has winner: {}", battle.getBattleId(), killerUuid);
            // Small delay before ending to let the message be seen
            final UUID finalWinner = killerUuid;
            server.execute(() -> {
                BattleEndHandler.endBattle(battle.getBattleId(), finalWinner, server);
            });
        }
    }

    /**
     * Play death effects (sound, particles) without actually dying.
     */
    private static void playDeathEffects(ServerPlayerEntity player) {
        // Play death sound
        player.getWorld().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_DEATH,
                SoundCategory.PLAYERS,
                1.0f, 1.0f
        );

        // Flash the screen red briefly by giving a very short damage effect
        // (The damage was already cancelled, so this is just visual)
    }

    /**
     * Reset a player's state after a "death" in battle.
     */
    private static void resetPlayer(ServerPlayerEntity player) {
        // Restore full health
        player.setHealth(player.getMaxHealth());

        // Restore hunger
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(5.0f);

        // Clear negative effects
        player.clearStatusEffects();

        // Clear fire
        player.setOnFire(false);
        player.setFireTicks(0);

        // Give brief spawn immunity (2 seconds)
        player.timeUntilRegen = 40;
        player.hurtTime = 0;
    }

    /**
     * Teleport player back to their arena spawn point.
     * If no arena was set, teleport to their personal spawn point (bed/world spawn).
     */
    private static void teleportToArenaSpawn(ServerPlayerEntity player, BattleData battle, MinecraftServer server) {
        BattleData.OriginalPosition spawn = battle.getArenaSpawn(player.getUuid());
        if (spawn != null) {
            // Has arena - teleport to arena spawn
            teleportSafely(player, spawn, server);
        } else {
            // No arena - teleport to player's personal spawn point
            teleportToPersonalSpawn(player, server);
        }
    }

    /**
     * Teleport player safely without triggering "moved too fast" warnings.
     */
    private static void teleportSafely(ServerPlayerEntity player, BattleData.OriginalPosition pos, MinecraftServer server) {
        if (pos == null) return;

        var targetWorld = server.getWorld(pos.dimension);
        if (targetWorld == null) {
            targetWorld = server.getOverworld();
        }

        // Reset velocity before teleport to prevent "moved too fast" warnings
        player.setVelocity(0, 0, 0);
        player.velocityModified = true;

        // Use teleport with resetCamera=true to properly sync client
        player.teleport(targetWorld, pos.x, pos.y, pos.z, pos.yaw, pos.pitch);

        // Reset velocity again after teleport
        player.setVelocity(0, 0, 0);
        player.velocityModified = true;
        player.networkHandler.syncWithPlayerPosition();
    }

    /**
     * Teleport player to their personal spawn point (bed or world spawn).
     */
    private static void teleportToPersonalSpawn(ServerPlayerEntity player, MinecraftServer server) {
        // Get player's spawn point (bed location or world spawn)
        var spawnPos = player.getSpawnPointPosition();
        var spawnDimension = player.getSpawnPointDimension();
        var spawnWorld = server.getWorld(spawnDimension);

        if (spawnWorld == null) {
            spawnWorld = server.getOverworld();
        }

        double x, y, z;
        float yaw = 0, pitch = 0;

        if (spawnPos != null) {
            // Has a bed/respawn anchor
            x = spawnPos.getX() + 0.5;
            y = spawnPos.getY();
            z = spawnPos.getZ() + 0.5;
        } else {
            // Use world spawn
            var worldSpawn = spawnWorld.getSpawnPos();
            x = worldSpawn.getX() + 0.5;
            y = worldSpawn.getY();
            z = worldSpawn.getZ() + 0.5;
        }

        // Reset velocity before teleport
        player.setVelocity(0, 0, 0);
        player.velocityModified = true;

        player.teleport(spawnWorld, x, y, z, yaw, pitch);

        // Reset velocity after teleport
        player.setVelocity(0, 0, 0);
        player.velocityModified = true;
        player.networkHandler.syncWithPlayerPosition();
    }
}

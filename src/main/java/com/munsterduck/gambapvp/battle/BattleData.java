package com.munsterduck.gambapvp.battle;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class BattleData {
    private final String battleId;
    private final List<UUID> participants;
    private final Map<UUID, Integer> scores;
    private final String kitName;
    private final int winsRequired;
    private final boolean keepInventory;
    private final Set<BlockPos> placedBlocks;
    private final Set<UUID> placedEntityUuids; // Entities (crystals, TNT) placed by battle participants
    private final WagerPot wagerPot;
    private final Map<UUID, OriginalPosition> originalPositions;
    private final Map<UUID, OriginalPosition> arenaSpawns;
    private final Map<UUID, Long> spawnImmunityUntil; // System time when immunity expires
    private final String arenaId;
    private final RegistryKey<World> battleWorld;
    private boolean active;
    private boolean countdownComplete;

    /**
     * Stores a player's original position before teleporting to arena.
     */
    public static class OriginalPosition {
        public final double x, y, z;
        public final float yaw, pitch;
        public final RegistryKey<World> dimension;

        public OriginalPosition(double x, double y, double z, float yaw, float pitch, RegistryKey<World> dimension) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.dimension = dimension;
        }

        public static OriginalPosition fromPlayer(ServerPlayerEntity player) {
            return new OriginalPosition(
                    player.getX(), player.getY(), player.getZ(),
                    player.getYaw(), player.getPitch(),
                    player.getWorld().getRegistryKey()
            );
        }
    }

    public BattleData(String battleId, List<UUID> participants, String kitName,
                      int winsRequired, boolean keepInventory, String arenaId, RegistryKey<World> battleWorld) {
        this.battleId = battleId;
        this.participants = new ArrayList<>(participants);
        this.scores = new HashMap<>();
        this.kitName = kitName;
        this.winsRequired = winsRequired;
        this.keepInventory = keepInventory;
        this.placedBlocks = new HashSet<>();
        this.placedEntityUuids = new HashSet<>();
        this.wagerPot = new WagerPot();
        this.originalPositions = new HashMap<>();
        this.arenaSpawns = new HashMap<>();
        this.spawnImmunityUntil = new HashMap<>();
        this.arenaId = arenaId;
        this.battleWorld = battleWorld;
        this.active = true;
        this.countdownComplete = false;

        for (UUID uuid : participants) {
            scores.put(uuid, 0);
        }
    }

    // Constructor for battles without arena
    public BattleData(String battleId, List<UUID> participants, String kitName,
                      int winsRequired, boolean keepInventory) {
        this(battleId, participants, kitName, winsRequired, keepInventory, null, null);
    }

    public void recordWin(UUID playerUuid) {
        scores.put(playerUuid, scores.get(playerUuid) + 1);
    }

    public boolean hasWinner() {
        return scores.values().stream().anyMatch(score -> score >= winsRequired);
    }

    public UUID getWinner() {
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() >= winsRequired)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void addPlacedBlock(BlockPos pos) {
        placedBlocks.add(pos.toImmutable());
    }

    public Set<BlockPos> getPlacedBlocks() {
        return placedBlocks;
    }

    public void addPlacedEntity(UUID entityUuid) {
        placedEntityUuids.add(entityUuid);
    }

    public boolean isPlacedEntity(UUID entityUuid) {
        return placedEntityUuids.contains(entityUuid);
    }

    public Set<UUID> getPlacedEntityUuids() {
        return placedEntityUuids;
    }

    public void addWager(UUID playerUuid, WagerData wager) {
        wagerPot.addWager(playerUuid, wager);
    }

    public WagerPot getWagerPot() {
        return wagerPot;
    }

    public boolean hasWagers() {
        return !wagerPot.isEmpty();
    }

    public void saveOriginalPosition(UUID playerUuid, OriginalPosition position) {
        originalPositions.put(playerUuid, position);
    }

    public OriginalPosition getOriginalPosition(UUID playerUuid) {
        return originalPositions.get(playerUuid);
    }

    public Map<UUID, OriginalPosition> getAllOriginalPositions() {
        return new HashMap<>(originalPositions);
    }

    public void saveArenaSpawn(UUID playerUuid, OriginalPosition position) {
        arenaSpawns.put(playerUuid, position);
    }

    public OriginalPosition getArenaSpawn(UUID playerUuid) {
        return arenaSpawns.get(playerUuid);
    }

    /**
     * Grant spawn immunity to a player for a duration in milliseconds.
     */
    public void grantSpawnImmunity(UUID playerUuid, long durationMs) {
        spawnImmunityUntil.put(playerUuid, System.currentTimeMillis() + durationMs);
    }

    /**
     * Check if a player currently has spawn immunity.
     */
    public boolean hasSpawnImmunity(UUID playerUuid) {
        Long until = spawnImmunityUntil.get(playerUuid);
        return until != null && System.currentTimeMillis() < until;
    }

    public boolean hasArena() {
        return arenaId != null && !arenaId.isEmpty();
    }

    public int getWinsRequired() {
        return winsRequired;
    }

    public Map<UUID, Integer> getScores() {
        return new HashMap<>(scores);
    }

    public int getScore(UUID playerUuid) {
        return scores.getOrDefault(playerUuid, 0);
    }

    public boolean isCountdownComplete() {
        return countdownComplete;
    }

    public void setCountdownComplete(boolean complete) {
        this.countdownComplete = complete;
    }

    public String getBattleId() { return battleId; }
    public List<UUID> getParticipants() { return participants; }
    public String getKitName() { return kitName; }
    public boolean isKeepInventory() { return keepInventory; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getArenaId() { return arenaId; }
    public RegistryKey<World> getBattleWorld() { return battleWorld; }
}

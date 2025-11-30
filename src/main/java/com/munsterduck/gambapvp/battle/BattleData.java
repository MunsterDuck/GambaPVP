package com.munsterduck.gambapvp.battle;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BattleData {
    private final String battleId;
    private final List<UUID> participants;
    private final Map<UUID, Integer> scores;
    private final String kitName;
    private final int winsRequired;
    private final boolean keepInventory;
    private final Set<BlockPos> placedBlocks;
    private boolean active;

    public BattleData(String battleId, List<UUID> participants, String kitName,
                      int winsRequired, boolean keepInventory) {
        this.battleId = battleId;
        this.participants = new ArrayList<>(participants);
        this.scores = new HashMap<>();
        this.kitName = kitName;
        this.winsRequired = winsRequired;
        this.keepInventory = keepInventory;
        this.placedBlocks = new HashSet<>();
        this.active = true;

        for (UUID uuid : participants) {
            scores.put(uuid, 0);
        }
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

    public String getBattleId() { return battleId; }
    public List<UUID> getParticipants() { return participants; }
    public String getKitName() { return kitName; }
    public boolean isKeepInventory() { return keepInventory; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

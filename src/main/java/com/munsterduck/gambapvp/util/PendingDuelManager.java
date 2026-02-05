package com.munsterduck.gambapvp.util;

import com.munsterduck.gambapvp.battle.WagerData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PendingDuelManager {
    // Map of receiver UUID -> List of requests
    private static final Map<UUID, List<DuelRequest>> pendingRequests = new ConcurrentHashMap<>();

    // Map of request ID -> Map of player UUID -> WagerData (for tracking wagers during setup)
    private static final Map<UUID, Map<UUID, WagerData>> pendingWagers = new ConcurrentHashMap<>();

    public static class DuelRequest {
        public final UUID senderUuid;
        public final String senderName;
        public final String kitName;
        public final int winsRequired;
        public final boolean keepInventory;
        public final String arenaId;
        public final double arenaX, arenaY, arenaZ;
        public final float arenaYaw, arenaPitch;
        public final String arenaDimension;
        public final long timestamp;
        public final UUID requestId; // Unique ID for this specific request

        public DuelRequest(UUID senderUuid, String senderName, String kitName, int winsRequired,
                          boolean keepInventory, String arenaId,
                          double arenaX, double arenaY, double arenaZ,
                          float arenaYaw, float arenaPitch, String arenaDimension) {
            this.senderUuid = senderUuid;
            this.senderName = senderName;
            this.kitName = kitName;
            this.winsRequired = winsRequired;
            this.keepInventory = keepInventory;
            this.arenaId = arenaId;
            this.arenaX = arenaX;
            this.arenaY = arenaY;
            this.arenaZ = arenaZ;
            this.arenaYaw = arenaYaw;
            this.arenaPitch = arenaPitch;
            this.arenaDimension = arenaDimension;
            this.timestamp = System.currentTimeMillis();
            this.requestId = UUID.randomUUID();
        }

        public boolean hasArenaCoordinates() {
            return arenaId != null && !arenaId.isEmpty() && arenaDimension != null && !arenaDimension.isEmpty();
        }
    }

    public static void addRequest(UUID receiverUuid, DuelRequest request) {
        pendingRequests.computeIfAbsent(receiverUuid, k -> new ArrayList<>()).add(request);
    }

    public static DuelRequest findRequest(UUID receiverUuid, String senderName) {
        List<DuelRequest> requests = pendingRequests.get(receiverUuid);
        if (requests == null || requests.isEmpty()) {
            return null;
        }

        // Find the most recent request from this sender
        return requests.stream()
            .filter(req -> req.senderName.equals(senderName))
            .max(Comparator.comparingLong(req -> req.timestamp))
            .orElse(null);
    }

    public static boolean removeRequest(UUID receiverUuid, UUID requestId) {
        List<DuelRequest> requests = pendingRequests.get(receiverUuid);
        if (requests == null) {
            return false;
        }

        boolean removed = requests.removeIf(req -> req.requestId.equals(requestId));

        // Clean up empty lists
        if (requests.isEmpty()) {
            pendingRequests.remove(receiverUuid);
        }

        return removed;
    }

    public static void removeAllRequestsFrom(UUID senderUuid) {
        pendingRequests.values().forEach(list ->
            list.removeIf(req -> req.senderUuid.equals(senderUuid))
        );
    }

    public static void removeAllRequestsTo(UUID receiverUuid) {
        pendingRequests.remove(receiverUuid);
    }

    // Wager management
    public static void setWager(UUID requestId, UUID playerUuid, WagerData wager) {
        pendingWagers.computeIfAbsent(requestId, k -> new ConcurrentHashMap<>())
                .put(playerUuid, wager.copy());
    }

    public static WagerData getWager(UUID requestId, UUID playerUuid) {
        Map<UUID, WagerData> wagers = pendingWagers.get(requestId);
        if (wagers != null) {
            WagerData wager = wagers.get(playerUuid);
            return wager != null ? wager.copy() : null;
        }
        return null;
    }

    public static Map<UUID, WagerData> getAllWagers(UUID requestId) {
        Map<UUID, WagerData> wagers = pendingWagers.get(requestId);
        if (wagers != null) {
            Map<UUID, WagerData> copy = new HashMap<>();
            for (Map.Entry<UUID, WagerData> entry : wagers.entrySet()) {
                copy.put(entry.getKey(), entry.getValue().copy());
            }
            return copy;
        }
        return new HashMap<>();
    }

    public static void clearWagers(UUID requestId) {
        pendingWagers.remove(requestId);
    }

    // Clean up old requests (older than 5 minutes)
    public static void cleanupExpiredRequests() {
        long now = System.currentTimeMillis();
        long expirationTime = 5 * 60 * 1000; // 5 minutes

        pendingRequests.values().forEach(list ->
            list.removeIf(req -> {
                boolean expired = now - req.timestamp > expirationTime;
                if (expired) {
                    // Clean up associated wagers
                    pendingWagers.remove(req.requestId);
                }
                return expired;
            })
        );

        // Remove empty lists
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
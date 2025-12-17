package com.munsterduck.gambapvp.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PendingDuelManager {
    // Map of receiver UUID -> List of requests
    private static final Map<UUID, List<DuelRequest>> pendingRequests = new ConcurrentHashMap<>();

    public static class DuelRequest {
        public final UUID senderUuid;
        public final String senderName;
        public final String kitName;
        public final int winsRequired;
        public final boolean keepInventory;
        public final long timestamp;
        public final UUID requestId; // Unique ID for this specific request

        public DuelRequest(UUID senderUuid, String senderName, String kitName, int winsRequired, boolean keepInventory) {
            this.senderUuid = senderUuid;
            this.senderName = senderName;
            this.kitName = kitName;
            this.winsRequired = winsRequired;
            this.keepInventory = keepInventory;
            this.timestamp = System.currentTimeMillis();
            this.requestId = UUID.randomUUID();
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

    // Clean up old requests (older than 5 minutes)
    public static void cleanupExpiredRequests() {
        long now = System.currentTimeMillis();
        long expirationTime = 5 * 60 * 1000; // 5 minutes

        pendingRequests.values().forEach(list ->
            list.removeIf(req -> now - req.timestamp > expirationTime)
        );

        // Remove empty lists
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
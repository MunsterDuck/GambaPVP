package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.util.PendingDuelManager;

import java.util.*;

/**
 * Tracks acceptance state for a multi-player battle request.
 * First accept starts 30-second window. Battle starts when all accept or window expires.
 */
public class PendingBattleSession {
    private static final long ACCEPTANCE_WINDOW_MS = 30_000; // 30 seconds

    private final UUID sessionId;
    private final UUID initiatorUuid;
    private final String initiatorName;
    private final Set<UUID> invitedPlayers;
    private final Set<UUID> acceptedPlayers;
    private final Set<UUID> declinedPlayers;
    private final PendingDuelManager.DuelRequest originalRequest;
    private Long acceptanceWindowEnd; // null until first accept

    public PendingBattleSession(UUID initiatorUuid, String initiatorName,
                                Set<UUID> invitedPlayers,
                                PendingDuelManager.DuelRequest originalRequest) {
        this.sessionId = UUID.randomUUID();
        this.initiatorUuid = initiatorUuid;
        this.initiatorName = initiatorName;
        this.invitedPlayers = new HashSet<>(invitedPlayers);
        this.acceptedPlayers = new HashSet<>();
        this.declinedPlayers = new HashSet<>();
        this.originalRequest = originalRequest;
        this.acceptanceWindowEnd = null;

        // Initiator is automatically accepted
        acceptedPlayers.add(initiatorUuid);
    }

    /**
     * Record that a player accepted the duel request.
     * First acceptance starts the 30-second window.
     * @return true if this was a valid acceptance
     */
    public boolean recordAccept(UUID playerUuid) {
        if (!invitedPlayers.contains(playerUuid) && !playerUuid.equals(initiatorUuid)) {
            return false;
        }
        if (declinedPlayers.contains(playerUuid)) {
            return false; // Can't accept after declining
        }

        boolean firstAccept = acceptedPlayers.size() == 1; // Only initiator so far
        acceptedPlayers.add(playerUuid);

        // Start the window on first non-initiator accept
        if (firstAccept && acceptanceWindowEnd == null) {
            acceptanceWindowEnd = System.currentTimeMillis() + ACCEPTANCE_WINDOW_MS;
        }

        return true;
    }

    /**
     * Record that a player declined the duel request.
     * @return true if this was a valid decline
     */
    public boolean recordDecline(UUID playerUuid) {
        if (!invitedPlayers.contains(playerUuid)) {
            return false;
        }
        if (acceptedPlayers.contains(playerUuid) && !playerUuid.equals(initiatorUuid)) {
            acceptedPlayers.remove(playerUuid); // Allow changing mind before battle starts
        }

        declinedPlayers.add(playerUuid);

        // Start window if this is the first response
        if (acceptanceWindowEnd == null && !acceptedPlayers.isEmpty()) {
            acceptanceWindowEnd = System.currentTimeMillis() + ACCEPTANCE_WINDOW_MS;
        }

        return true;
    }

    /**
     * Check if the acceptance window has expired.
     */
    public boolean isWindowExpired() {
        return acceptanceWindowEnd != null && System.currentTimeMillis() >= acceptanceWindowEnd;
    }

    /**
     * Check if all invited players have responded (accepted or declined).
     */
    public boolean allResponded() {
        for (UUID invited : invitedPlayers) {
            if (!acceptedPlayers.contains(invited) && !declinedPlayers.contains(invited)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the battle can start (at least 2 players accepted).
     */
    public boolean canStartBattle() {
        return acceptedPlayers.size() >= 2;
    }

    /**
     * Get players who haven't responded yet.
     */
    public Set<UUID> getPendingPlayers() {
        Set<UUID> pending = new HashSet<>();
        for (UUID invited : invitedPlayers) {
            if (!acceptedPlayers.contains(invited) && !declinedPlayers.contains(invited)) {
                pending.add(invited);
            }
        }
        return pending;
    }

    /**
     * Get remaining time in seconds until window expires.
     */
    public int getRemainingSeconds() {
        if (acceptanceWindowEnd == null) {
            return -1; // Window not started
        }
        long remaining = acceptanceWindowEnd - System.currentTimeMillis();
        return Math.max(0, (int) (remaining / 1000));
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getInitiatorUuid() {
        return initiatorUuid;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public Set<UUID> getInvitedPlayers() {
        return new HashSet<>(invitedPlayers);
    }

    public Set<UUID> getAcceptedPlayers() {
        return new HashSet<>(acceptedPlayers);
    }

    public Set<UUID> getDeclinedPlayers() {
        return new HashSet<>(declinedPlayers);
    }

    public PendingDuelManager.DuelRequest getOriginalRequest() {
        return originalRequest;
    }

    public Long getAcceptanceWindowEnd() {
        return acceptanceWindowEnd;
    }
}

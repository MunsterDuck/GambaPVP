package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.util.PendingDuelManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pending battle sessions and the acceptance flow.
 */
public class PendingBattleManager {
    // Session ID -> Session
    private static final Map<UUID, PendingBattleSession> pendingSessions = new ConcurrentHashMap<>();
    // Player UUID -> Session ID (for quick lookup)
    private static final Map<UUID, UUID> playerToSession = new ConcurrentHashMap<>();
    // Request ID -> Session ID (to link DuelRequest to session)
    private static final Map<UUID, UUID> requestToSession = new ConcurrentHashMap<>();

    /**
     * Create a new pending battle session from a duel request.
     */
    public static PendingBattleSession createSession(UUID initiatorUuid, String initiatorName,
                                                      Set<UUID> invitedPlayers,
                                                      PendingDuelManager.DuelRequest request) {
        PendingBattleSession session = new PendingBattleSession(
                initiatorUuid, initiatorName, invitedPlayers, request);

        pendingSessions.put(session.getSessionId(), session);
        playerToSession.put(initiatorUuid, session.getSessionId());

        for (UUID invited : invitedPlayers) {
            playerToSession.put(invited, session.getSessionId());
        }

        requestToSession.put(request.requestId, session.getSessionId());

        return session;
    }

    /**
     * Get a session by its ID.
     */
    public static PendingBattleSession getSession(UUID sessionId) {
        return pendingSessions.get(sessionId);
    }

    /**
     * Get the session a player is part of.
     */
    public static PendingBattleSession getPlayerSession(UUID playerUuid) {
        UUID sessionId = playerToSession.get(playerUuid);
        return sessionId != null ? pendingSessions.get(sessionId) : null;
    }

    /**
     * Get the session for a specific request ID.
     */
    public static PendingBattleSession getSessionByRequest(UUID requestId) {
        UUID sessionId = requestToSession.get(requestId);
        return sessionId != null ? pendingSessions.get(sessionId) : null;
    }

    /**
     * Process an acceptance from a player.
     * @return SessionResult indicating what happened
     */
    public static SessionResult processAccept(UUID playerUuid, UUID requestId, MinecraftServer server) {
        PendingBattleSession session = getSessionByRequest(requestId);
        if (session == null) {
            return SessionResult.NOT_FOUND;
        }

        if (!session.recordAccept(playerUuid)) {
            return SessionResult.INVALID;
        }

        // Check if we should start the battle
        if (session.allResponded() && session.canStartBattle()) {
            return SessionResult.READY_TO_START;
        }

        // Notify initiator of acceptance
        ServerPlayerEntity initiator = server.getPlayerManager().getPlayer(session.getInitiatorUuid());
        ServerPlayerEntity accepter = server.getPlayerManager().getPlayer(playerUuid);
        if (initiator != null && accepter != null) {
            initiator.sendMessage(Text.literal(accepter.getName().getString() + " accepted your duel request! (" +
                    session.getAcceptedPlayers().size() + "/" + (session.getInvitedPlayers().size() + 1) + ")")
                    .styled(s -> s.withColor(0x55FF55)), false);
        }

        return SessionResult.WAITING;
    }

    /**
     * Process a decline from a player.
     * @return SessionResult indicating what happened
     */
    public static SessionResult processDecline(UUID playerUuid, UUID requestId, MinecraftServer server) {
        PendingBattleSession session = getSessionByRequest(requestId);
        if (session == null) {
            return SessionResult.NOT_FOUND;
        }

        if (!session.recordDecline(playerUuid)) {
            return SessionResult.INVALID;
        }

        // Notify initiator of decline
        ServerPlayerEntity initiator = server.getPlayerManager().getPlayer(session.getInitiatorUuid());
        ServerPlayerEntity decliner = server.getPlayerManager().getPlayer(playerUuid);
        if (initiator != null && decliner != null) {
            initiator.sendMessage(Text.literal(decliner.getName().getString() + " declined your duel request.")
                    .styled(s -> s.withColor(0xFF5555)), false);
        }

        // Check if battle can still happen
        if (session.allResponded()) {
            if (session.canStartBattle()) {
                return SessionResult.READY_TO_START;
            } else {
                return SessionResult.CANCELLED_NOT_ENOUGH;
            }
        }

        return SessionResult.WAITING;
    }

    /**
     * Check expired sessions and either start them or cancel them.
     * Called every tick from server tick handler.
     */
    public static void checkExpiredSessions(MinecraftServer server) {
        List<UUID> toProcess = new ArrayList<>();

        for (PendingBattleSession session : pendingSessions.values()) {
            if (session.isWindowExpired() && !session.allResponded()) {
                toProcess.add(session.getSessionId());
            }
        }

        for (UUID sessionId : toProcess) {
            PendingBattleSession session = pendingSessions.get(sessionId);
            if (session == null) continue;

            if (session.canStartBattle()) {
                // Start with whoever accepted
                GambaPVP.LOGGER.info("Starting battle session {} with {} players (timeout)",
                        sessionId, session.getAcceptedPlayers().size());

                // Notify pending players that they missed it
                for (UUID pending : session.getPendingPlayers()) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending);
                    if (player != null) {
                        player.sendMessage(Text.literal("You didn't respond in time. The battle started without you.")
                                .styled(s -> s.withColor(0xFFAA00)), false);
                    }
                }

                BattleStarter.startBattle(session, server);
                removeSession(session);
            } else {
                // Not enough players, cancel
                GambaPVP.LOGGER.info("Cancelling battle session {} - not enough acceptances", sessionId);

                // Notify everyone
                notifySessionCancelled(session, server, "Not enough players accepted the duel.");
                removeSession(session);
            }
        }
    }

    /**
     * Remove a session and clean up mappings.
     */
    public static void removeSession(PendingBattleSession session) {
        pendingSessions.remove(session.getSessionId());
        playerToSession.remove(session.getInitiatorUuid());
        for (UUID invited : session.getInvitedPlayers()) {
            playerToSession.remove(invited);
        }
        requestToSession.remove(session.getOriginalRequest().requestId);
        PendingDuelManager.clearWagers(session.getOriginalRequest().requestId);
    }

    /**
     * Cancel a session and notify all players.
     */
    public static void cancelSession(UUID sessionId, MinecraftServer server, String reason) {
        PendingBattleSession session = pendingSessions.get(sessionId);
        if (session != null) {
            notifySessionCancelled(session, server, reason);
            removeSession(session);
        }
    }

    private static void notifySessionCancelled(PendingBattleSession session, MinecraftServer server, String reason) {
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.add(session.getInitiatorUuid());
        allPlayers.addAll(session.getInvitedPlayers());

        for (UUID playerUuid : allPlayers) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                player.sendMessage(Text.literal("Duel cancelled: " + reason)
                        .styled(s -> s.withColor(0xFF5555)), false);
            }
        }
    }

    /**
     * Check if a player is in a pending session.
     */
    public static boolean isInPendingSession(UUID playerUuid) {
        return playerToSession.containsKey(playerUuid);
    }

    public enum SessionResult {
        NOT_FOUND,
        INVALID,
        WAITING,
        READY_TO_START,
        CANCELLED_NOT_ENOUGH
    }
}

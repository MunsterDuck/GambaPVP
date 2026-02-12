package com.munsterduck.gambapvp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.munsterduck.gambapvp.battle.*;
import com.munsterduck.gambapvp.network.BattleRequestPacket;
import com.munsterduck.gambapvp.util.CurrencyHelper;
import com.munsterduck.gambapvp.util.PendingDuelManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GDuelCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
            CommandManager.literal("gduel")
                .executes(GDuelCommand::run)
                .then(CommandManager.literal("accept")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .then(CommandManager.argument("requestId", StringArgumentType.word())
                            .executes(GDuelCommand::accept))))
                .then(CommandManager.literal("decline")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .then(CommandManager.argument("requestId", StringArgumentType.word())
                            .executes(GDuelCommand::decline))))
        );
    }

    public static int run(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("Only players can use this command!"));
            return 0;
        }

        BattleRequestPacket.CHANNEL.serverHandle(player).send(new BattleRequestPacket.OpenBattleScreen());
        return 1;
    }

    public static int accept(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String senderName = StringArgumentType.getString(context, "player");
        String requestIdStr = StringArgumentType.getString(context, "requestId");
        MinecraftServer server = context.getSource().getServer();

        UUID requestId;
        try {
            requestId = UUID.fromString(requestIdStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("Invalid request!").styled(style ->
                    style.withColor(0xFF5555)), false);
            return 0;
        }

        // Check if player is already in a battle
        if (BattleManager.isInBattle(player.getUuid())) {
            player.sendMessage(Text.literal("You are already in a battle!")
                    .styled(style -> style.withColor(0xFF5555)), false);
            return 0;
        }

        // Find and verify the request exists
        PendingDuelManager.DuelRequest request = PendingDuelManager.findRequest(player.getUuid(), senderName);

        if (request == null || !request.requestId.equals(requestId)) {
            player.sendMessage(Text.literal("This duel request has already been responded to or expired!")
                    .styled(style -> style.withColor(0xFF5555)), false);
            return 0;
        }

        ServerPlayerEntity sender = server.getPlayerManager().getPlayer(senderName);

        if (sender == null) {
            player.sendMessage(Text.literal("Player " + senderName + " is not online!")
                    .styled(style -> style.withColor(0xFF5555)), false);
            PendingDuelManager.removeRequest(player.getUuid(), requestId);
            return 0;
        }

        // Check if there's already a pending session for this request
        PendingBattleSession existingSession = PendingBattleManager.getSessionByRequest(requestId);

        if (existingSession != null) {
            // Multi-player request - auto-match wager (partial if insufficient)
            WagerData senderWager = PendingDuelManager.getWager(requestId, request.senderUuid);
            if (senderWager != null && senderWager.getCurrency() > 0) {
                int acceptorBalance = CurrencyHelper.getBalance(player);
                int contribution = Math.min(senderWager.getCurrency(), acceptorBalance);

                WagerData acceptorWager = new WagerData();
                acceptorWager.setCurrency(contribution);
                PendingDuelManager.setWager(requestId, player.getUuid(), acceptorWager);
            }

            PendingBattleManager.SessionResult result = PendingBattleManager.processAccept(
                    player.getUuid(), requestId, server);

            switch (result) {
                case READY_TO_START -> {
                    player.sendMessage(Text.literal("✓ You accepted the duel request!")
                            .styled(style -> style.withColor(0x55FF55)), false);
                    BattleStarter.startBattle(existingSession, server);
                    PendingBattleManager.removeSession(existingSession);
                }
                case WAITING -> {
                    player.sendMessage(Text.literal("✓ You accepted the duel request! Waiting for others...")
                            .styled(style -> style.withColor(0x55FF55)), false);
                }
                case NOT_FOUND, INVALID -> {
                    player.sendMessage(Text.literal("This request is no longer valid!")
                            .styled(style -> style.withColor(0xFF5555)), false);
                }
            }
        } else {
            // First acceptance - 1v1 direct start
            // Auto-match the sender's currency wager (partial if insufficient)
            WagerData senderWager = PendingDuelManager.getWager(requestId, request.senderUuid);
            if (senderWager != null && senderWager.getCurrency() > 0) {
                int acceptorBalance = CurrencyHelper.getBalance(player);
                int contribution = Math.min(senderWager.getCurrency(), acceptorBalance);

                WagerData acceptorWager = new WagerData();
                acceptorWager.setCurrency(contribution);
                PendingDuelManager.setWager(requestId, player.getUuid(), acceptorWager);
            }

            // Remove the request so it can't be accepted/declined again
            PendingDuelManager.removeRequest(player.getUuid(), requestId);

            player.sendMessage(Text.literal("\u2713 You accepted the duel request from " + senderName)
                    .styled(style -> style.withColor(0x55FF55)), false);

            sender.sendMessage(Text.literal(player.getName().getString() + " accepted your duel request!")
                    .styled(style -> style.withColor(0x55FF55)), false);

            // Start the battle directly for 1v1
            BattleStarter.startBattleDirect(sender.getUuid(), player.getUuid(), request, server);
        }

        return 1;
    }

    public static int decline(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String senderName = StringArgumentType.getString(context, "player");
        String requestIdStr = StringArgumentType.getString(context, "requestId");
        MinecraftServer server = context.getSource().getServer();

        UUID requestId;
        try {
            requestId = UUID.fromString(requestIdStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("Invalid request!").styled(style ->
                    style.withColor(0xFF5555)), false);
            return 0;
        }

        // Find and verify the request exists
        PendingDuelManager.DuelRequest request = PendingDuelManager.findRequest(player.getUuid(), senderName);

        if (request == null || !request.requestId.equals(requestId)) {
            player.sendMessage(Text.literal("This duel request has already been responded to or expired!")
                    .styled(style -> style.withColor(0xFF5555)), false);
            return 0;
        }

        // Check if there's a pending session
        PendingBattleSession existingSession = PendingBattleManager.getSessionByRequest(requestId);

        if (existingSession != null) {
            // Multi-player request - process decline
            PendingBattleManager.SessionResult result = PendingBattleManager.processDecline(
                    player.getUuid(), requestId, server);

            player.sendMessage(Text.literal("✗ You declined the duel request from " + senderName)
                    .styled(style -> style.withColor(0xFF5555)), false);

            switch (result) {
                case READY_TO_START -> {
                    // Everyone responded and there's enough players
                    BattleStarter.startBattle(existingSession, server);
                    PendingBattleManager.removeSession(existingSession);
                }
                case CANCELLED_NOT_ENOUGH -> {
                    // Not enough players accepted
                    PendingBattleManager.cancelSession(existingSession.getSessionId(), server,
                            "Not enough players accepted");
                }
                // WAITING - do nothing, still waiting for others
            }
        } else {
            // Simple 1v1 decline
            ServerPlayerEntity sender = server.getPlayerManager().getPlayer(senderName);

            // Remove the request
            PendingDuelManager.removeRequest(player.getUuid(), requestId);

            player.sendMessage(Text.literal("✗ You declined the duel request from " + senderName)
                    .styled(style -> style.withColor(0xFF5555)), false);

            if (sender != null) {
                sender.sendMessage(Text.literal(player.getName().getString() + " declined your duel request.")
                        .styled(style -> style.withColor(0xFF5555)), false);
            }
        }

        return 1;
    }
}
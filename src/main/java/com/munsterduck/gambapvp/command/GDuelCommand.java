package com.munsterduck.gambapvp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.munsterduck.gambapvp.battle.BattleManager;
import com.munsterduck.gambapvp.network.BattleRequestPacket;
import com.munsterduck.gambapvp.util.PendingDuelManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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

        ServerPlayerEntity sender = context.getSource().getServer().getPlayerManager().getPlayer(senderName);

        if (sender == null) {
            player.sendMessage(Text.literal("Player " + senderName + " is not online!")
                    .styled(style -> style.withColor(0xFF5555)), false);
            PendingDuelManager.removeRequest(player.getUuid(), requestId);
            return 0;
        }

        // Remove the request so it can't be accepted/declined again
        PendingDuelManager.removeRequest(player.getUuid(), requestId);

        player.sendMessage(Text.literal("✓ You accepted the duel request from " + senderName)
                .styled(style -> style.withColor(0x55FF55)), false);

        sender.sendMessage(Text.literal(player.getName().getString() + " accepted your duel request!")
                .styled(style -> style.withColor(0x55FF55)), false);

        // TODO: Start the actual duel/battle with request.kitName, request.winsRequired, etc.

        return 1;
    }

    public static int decline(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String senderName = StringArgumentType.getString(context, "player");
        String requestIdStr = StringArgumentType.getString(context, "requestId");

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

        ServerPlayerEntity sender = context.getSource().getServer().getPlayerManager().getPlayer(senderName);

        // Remove the request so it can't be accepted/declined again
        PendingDuelManager.removeRequest(player.getUuid(), requestId);

        player.sendMessage(Text.literal("✗ You declined the duel request from " + senderName)
                .styled(style -> style.withColor(0xFF5555)), false);

        if (sender != null) {
            sender.sendMessage(Text.literal(player.getName().getString() + " declined your duel request.")
                    .styled(style -> style.withColor(0xFF5555)), false);
        }

        return 1;
    }
}
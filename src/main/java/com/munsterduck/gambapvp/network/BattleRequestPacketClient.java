package com.munsterduck.gambapvp.network;

import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.gui.BattleSetupScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class BattleRequestPacketClient {

    public static void init() {

        BattleRequestPacket.CHANNEL.registerClientbound(
            BattleRequestPacket.OpenBattleScreen.class,
            (message, access) -> {
                MinecraftClient.getInstance()
                    .setScreen(new BattleSetupScreen());
            }
        );

        BattleRequestPacket.CHANNEL.registerClientbound(
            BattleRequestPacket.BattleRequest.class,
            (message, access) -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    MutableText acceptButton = Text.literal("[Accept]")
                        .styled(style -> style
                            .withColor(0x55FF55)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/gduel accept " + message.senderName() + " " + message.requestId()
                            ))
                            .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Accept battle request")
                            ))
                        );

                    MutableText declineButton = Text.literal("[Decline]")
                        .styled(style -> style
                            .withColor(0xFF5555)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/gduel decline " + message.senderName() + " " + message.requestId()
                            ))
                            .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Decline battle request")
                            ))
                        );

                    // Build the message
                    MutableText battleMessage = Text.literal("⚔ ")
                            .styled(style -> style.withColor(0xFFFF55))
                            .append(Text.literal(message.senderName())
                                .styled(style -> style.withColor(0x55FFFF).withBold(true)))
                            .append(Text.literal(" challenges you to a duel!"))
                            .append(Text.literal("\n"))
                            .append(Text.literal("Kit: ").styled(style -> style.withColor(0xAAAAAA)))
                            .append(Text.literal(message.kitName().isEmpty() ? "No Kit" : message.kitName())
                                .styled(style -> style.withColor(0xFFFFFF)))
                            .append(Text.literal(" | "))
                            .append(Text.literal("First to: " + message.winsRequired())
                                .styled(style -> style.withColor(0xFFFFFF)));

                    // Conditionally add Keep Inventory info only if no kit
                    if (message.kitName().isEmpty()) {
                        battleMessage.append(Text.literal(" | Keep Inventory: " + (message.keepInventory() ? "Yes" : "No"))
                            .styled(style -> style.withColor(0xFFFFFF)));
                    }

                    // Add buttons
                    battleMessage.append(Text.literal("\n"))
                        .append(acceptButton)
                        .append(Text.literal("  "))
                        .append(declineButton);

                    client.player.sendMessage(battleMessage, false);
                }
            }
        );

        BattleRequestPacket.CHANNEL.registerClientbound(
            BattleRequestPacket.SendKits.class,
            (message, access) -> {
                ClientKitCache.clear();
                message.kits().forEach(
                    kit -> ClientKitCache.addKit(kit.name(), kit.icon(), kit.items())
                );
                if (MinecraftClient.getInstance().currentScreen instanceof BattleSetupScreen screen) {
                    screen.refreshKits();
                }
            }
        );
    }
}

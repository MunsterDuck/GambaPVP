package com.munsterduck.gambapvp.network;

import com.munsterduck.gambapvp.client.BattleHudRenderer;
import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.gui.BattleSetupScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class BattleRequestPacketClient {

    // Cached balance from server
    private static int cachedBalance = 0;

    public static int getCachedBalance() {
        return cachedBalance;
    }

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
                    String divider = "\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC";

                    MutableText acceptButton = Text.literal("[ACCEPT]")
                        .styled(style -> style
                            .withColor(0x55FF55)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/gduel accept " + message.senderName() + " " + message.requestId()
                            ))
                            .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Click to accept this duel")
                            ))
                        );

                    MutableText declineButton = Text.literal("[DECLINE]")
                        .styled(style -> style
                            .withColor(0xFF5555)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/gduel decline " + message.senderName() + " " + message.requestId()
                            ))
                            .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Click to decline this duel")
                            ))
                        );

                    MutableText battleMessage = Text.empty()
                        .append(Text.literal(divider).styled(s -> s.withColor(0xFFAA00).withStrikethrough(true)))
                        .append(Text.literal("\n"))
                        .append(Text.literal("  \u2694 Duel Request \u2694\n").styled(s -> s.withColor(0xFFAA00).withBold(true)))
                        .append(Text.literal("\n"))
                        .append(Text.literal("  ").styled(s -> s.withColor(0xAAAAAA)))
                        .append(Text.literal(message.senderName()).styled(s -> s.withColor(0x55FFFF).withBold(true)))
                        .append(Text.literal(" wants to duel you!\n").styled(s -> s.withColor(0xFFFFFF)))
                        .append(Text.literal("\n"));

                    // Settings line
                    String kitText = message.kitName().isEmpty() ? "No Kit" : message.kitName();
                    battleMessage
                        .append(Text.literal("  Kit: ").styled(s -> s.withColor(0xAAAAAA)))
                        .append(Text.literal(kitText).styled(s -> s.withColor(0xFFFFFF)))
                        .append(Text.literal("  \u2502  ").styled(s -> s.withColor(0x555555)))
                        .append(Text.literal("First to ").styled(s -> s.withColor(0xAAAAAA)))
                        .append(Text.literal(String.valueOf(message.winsRequired())).styled(s -> s.withColor(0xFFFF55).withBold(true)))
                        .append(Text.literal("\n"));

                    if (message.kitName().isEmpty()) {
                        battleMessage
                            .append(Text.literal("  Keep Inventory: ").styled(s -> s.withColor(0xAAAAAA)))
                            .append(Text.literal(message.keepInventory() ? "Yes" : "No")
                                .styled(s -> s.withColor(message.keepInventory() ? 0x55FF55 : 0xFF5555)))
                            .append(Text.literal("\n"));
                    }

                    // Wager info
                    if (message.wagerCurrency() > 0 || message.wagerItemCount() > 0) {
                        battleMessage.append(Text.literal("\n"));
                        battleMessage.append(Text.literal("  \u26A0 Wager: ").styled(s -> s.withColor(0xFFAA00).withBold(true)));
                        if (message.wagerCurrency() > 0) {
                            battleMessage.append(Text.literal(message.wagerCurrency() + " coins").styled(s -> s.withColor(0xFFFF55)));
                        }
                        if (message.wagerCurrency() > 0 && message.wagerItemCount() > 0) {
                            battleMessage.append(Text.literal(" + ").styled(s -> s.withColor(0xAAAAAA)));
                        }
                        if (message.wagerItemCount() > 0) {
                            battleMessage.append(Text.literal(message.wagerItemCount() + " item" + (message.wagerItemCount() > 1 ? "s" : ""))
                                .styled(s -> s.withColor(0x55FF55)));
                        }
                        battleMessage.append(Text.literal("\n"));
                    }

                    // Buttons
                    battleMessage
                        .append(Text.literal("\n"))
                        .append(Text.literal("  "))
                        .append(acceptButton)
                        .append(Text.literal("     "))
                        .append(declineButton)
                        .append(Text.literal("\n"))
                        .append(Text.literal(divider).styled(s -> s.withColor(0xFFAA00).withStrikethrough(true)));

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

        // Battle HUD packets
        BattleRequestPacket.CHANNEL.registerClientbound(
            BattleRequestPacket.ShowBattleHud.class,
            (message, access) -> {
                BattleHudRenderer.showHud(message);
            }
        );

        BattleRequestPacket.CHANNEL.registerClientbound(
            BattleRequestPacket.UpdateBattleScore.class,
            (message, access) -> {
                BattleHudRenderer.updateScore(message);
            }
        );

        BattleRequestPacket.CHANNEL.registerClientbound(
            BattleRequestPacket.HideBattleHud.class,
            (message, access) -> {
                BattleHudRenderer.hideHud(message.battleId());
            }
        );

        BattleRequestPacket.CHANNEL.registerClientbound(
            BattleRequestPacket.SendBalance.class,
            (message, access) -> {
                cachedBalance = message.balance();
            }
        );
    }
}

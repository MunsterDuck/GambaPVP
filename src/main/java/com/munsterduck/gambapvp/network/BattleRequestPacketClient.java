package com.munsterduck.gambapvp.network;

import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.gui.BattleSetupScreen;
import net.minecraft.client.MinecraftClient;

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
                    System.out.println("Received battle request from: " + message.senderName());
                }
        );

        BattleRequestPacket.CHANNEL.registerClientbound(
                BattleRequestPacket.SendKits.class,
                (message, access) -> {
                    ClientKitCache.clear();
                    message.kits().forEach(
                            kit -> ClientKitCache.addKit(kit.name(), kit.icon())
                    );

                    if (MinecraftClient.getInstance().currentScreen
                            instanceof BattleSetupScreen screen) {
                        screen.refreshKits();
                    }
                }
        );
    }
}

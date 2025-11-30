package com.munsterduck.gambapvp.network;

import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.gui.BattleSetupScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public class NetworkHandler {

    public static void registerServerPackets() {
        RequestKitsPacket.register();
        KitSelectedPacket.register();
    }

    public static void registerClientPackets() {
        // Register open battle screen packet
        ClientPlayNetworking.registerGlobalReceiver(OpenBattleScreenPacket.ID, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                client.setScreen(new BattleSetupScreen(client.currentScreen));
            });
        });

        // Register send kits packet - stores kits in cache for the custom GUI to use
        ClientPlayNetworking.registerGlobalReceiver(SendKitsPacket.ID, (client, handler, buf, responseSender) -> {
            int kitCount = buf.readInt();

            // Clear old cache
            ClientKitCache.clear();

            for (int i = 0; i < kitCount; i++) {
                String kitName = buf.readString();
                ItemStack iconStack = buf.readItemStack();

                client.execute(() -> {
                    ClientKitCache.addKit(kitName, iconStack);
                });
            }
        });
    }

    public static void requestKits() {
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(RequestKitsPacket.ID, PacketByteBufs.create());
        }
    }

    public static void sendKitSelection(String kitName) {
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            ClientPlayNetworking.send(KitSelectedPacket.ID, KitSelectedPacket.encode(kitName));
        }
    }
}

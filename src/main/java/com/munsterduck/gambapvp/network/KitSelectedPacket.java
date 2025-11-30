package com.munsterduck.gambapvp.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class KitSelectedPacket {
    public static final Identifier ID = new Identifier("gambapvp", "kit_selected");

    public static PacketByteBuf encode(String kitName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(kitName);
        return buf;
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            String kitName = buf.readString();

            server.execute(() -> {
                // TODO: Store selected kit and proceed to next step
                // For now, just send feedback
                player.sendMessage(Text.literal("TODO: Selected kit: " + kitName), false);
            });
        });
    }
}

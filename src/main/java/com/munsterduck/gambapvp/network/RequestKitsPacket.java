package com.munsterduck.gambapvp.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class RequestKitsPacket {
    public static final Identifier ID = new Identifier("gambapvp", "request_kits");

    public static PacketByteBuf encode() {
        return PacketByteBufs.create();
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                // Send kit list back to client
                SendKitsPacket.send(player);
            });
        });
    }
}

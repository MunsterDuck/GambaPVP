package com.munsterduck.gambapvp.network;

import com.munsterduck.gambapvp.util.KitData;
import com.munsterduck.gambapvp.util.KitManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public class SendKitsPacket {
    public static final Identifier ID = new Identifier("gambapvp", "send_kits");

    public static void send(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();

        List<KitData> kits = KitManager.getAllKitsWithIcons(player.getServer());

        // Write number of kits
        buf.writeInt(kits.size());

        // Write each kit's data
        for (KitData kit : kits) {
            buf.writeString(kit.getName());
            buf.writeItemStack(kit.getIconItem());
        }

        ServerPlayNetworking.send(player, ID, buf);
    }
}

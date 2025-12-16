package com.munsterduck.gambapvp.network;

import com.munsterduck.gambapvp.util.KitData;
import com.munsterduck.gambapvp.util.KitManager;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.munsterduck.gambapvp.GambaPVP.MOD_ID;

public class BattleRequestPacket {

    public static final OwoNetChannel CHANNEL =
            OwoNetChannel.create(new Identifier(MOD_ID, "main"));

    public static void registerPacketsCommon() {
        // Clientbound packets (deferred on server)
        CHANNEL.registerClientboundDeferred(OpenBattleScreen.class);
        CHANNEL.registerClientboundDeferred(BattleRequest.class);
        CHANNEL.registerClientboundDeferred(SendKits.class);

        // Serverbound packets (real handlers)
        CHANNEL.registerServerbound(BattleRequestSend.class, (message, access) -> {
            ServerPlayerEntity sender = access.player();
            List<ServerPlayerEntity> targets = new ArrayList<>();

            for (String player : message.selectedOpponents()) {
                ServerPlayerEntity serverPlayer = sender.server.getPlayerManager().getPlayer(player);
                targets.add(serverPlayer);
            };

            if (!targets.isEmpty()) {
                for (ServerPlayerEntity target : targets) {
                    CHANNEL.serverHandle(target).send(new BattleRequest(
                            sender.getName().getString(),
                            message.kitName(),
                            message.winsRequired(),
                            message.keepInventory()
                    ));
                }
            }
        });

        CHANNEL.registerServerbound(RequestKits.class, (message, access) -> {
            List<SendKits.KitInfo> kits = KitManager.getAllKitsWithIcons(access.player().getServer()).stream()
                    .map(kit -> new SendKits.KitInfo(kit.getName(), kit.getIcon(), kit.getItemsList()))
                    .toList();

            CHANNEL.serverHandle(access.player()).send(new SendKits(kits));
        });
    }

    public record OpenBattleScreen() {}

    public record BattleRequestSend(
            List<String> selectedOpponents,
            String kitName,
            int winsRequired,
            boolean keepInventory
    ) {}

    public record BattleRequest(
            String senderName,
            String kitName,
            int winsRequired,
            boolean keepInventory
    ) {}

    public record BattleResponse(
            String requesterName,
            boolean accepted
    ) {}

    // Kit packets
    public record RequestKits() {}

    public record SendKits(List<KitInfo> kits) {
        public record KitInfo(String name, ItemStack icon, List<ItemStack> items) {}
    }
}

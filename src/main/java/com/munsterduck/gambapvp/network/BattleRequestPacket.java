package com.munsterduck.gambapvp.network;

import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.gui.NewBattleSetupScreen;
import com.munsterduck.gambapvp.util.KitData;
import com.munsterduck.gambapvp.util.KitManager;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static com.munsterduck.gambapvp.GambaPVP.MOD_ID;

/**
 * All networking using owo-lib
 */
public class BattleRequestPacket {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(new Identifier(MOD_ID, "main"));

    public static void init() {
        // Open battle screen
        CHANNEL.registerClientbound(OpenBattleScreen.class, (message, access) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                client.setScreen(new NewBattleSetupScreen());
            });
        });

        // Battle request flow
        CHANNEL.registerServerbound(BattleRequestSend.class, (message, access) -> {
            System.out.println("Server received battle request:");
            System.out.println("  Target: " + message.targetPlayerName());
            System.out.println("  Kit: " + message.kitName());
            System.out.println("  Wins: " + message.winsRequired());
            System.out.println("  Keep Inventory: " + message.keepInventory());

            ServerPlayerEntity sender = access.player();
            ServerPlayerEntity target = sender.server.getPlayerManager().getPlayer(message.targetPlayerName());

            if (target != null) {
                CHANNEL.serverHandle(target).send(new BattleRequest(
                        sender.getName().getString(),
                        message.kitName(),
                        message.winsRequired(),
                        message.keepInventory()
                ));
            }
        });

        CHANNEL.registerClientbound(BattleRequest.class, (message, access) -> {
            System.out.println("Received battle request from: " + message.senderName());
            // TODO: Show accept/decline screen
        });

        CHANNEL.registerServerbound(BattleResponse.class, (message, access) -> {
            System.out.println("Player " + (message.accepted() ? "accepted" : "rejected") + " battle");
        });

        // Kit networking
        CHANNEL.registerServerbound(RequestKits.class, (message, access) -> {
            // Get your actual kits from KitManager
            List<KitInfo> kits = new ArrayList<>();

             for (KitData kit : KitManager.getAllKitsWithIcons(access.player().getServer())) {
                 kits.add(new KitInfo(kit.getName(), kit.getIconItem()));
             }

            CHANNEL.serverHandle(access.player()).send(new SendKits(kits));
        });

        CHANNEL.registerClientbound(SendKits.class, (message, access) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                ClientKitCache.clear();
                for (KitInfo kit : message.kits()) {
                    ClientKitCache.addKit(kit.name(), kit.icon());
                }

                // Refresh the screen if it's open
                if (client.currentScreen instanceof NewBattleSetupScreen screen) {
                    screen.refreshKits();
                }
            });
        });

        CHANNEL.registerServerbound(KitSelected.class, (message, access) -> {
            System.out.println("Player selected kit: " + message.kitName());
            // TODO: Store kit selection or apply it
        });
    }

    // Screen packets
    public record OpenBattleScreen() {}

    // Battle request packets
    public record BattleRequestSend(
            String targetPlayerName,
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

    public record SendKits(List<KitInfo> kits) {}

    public record KitInfo(String name, ItemStack icon) {}

    public record KitSelected(String kitName) {}
}
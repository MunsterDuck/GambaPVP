package com.munsterduck.gambapvp.network;

import com.munsterduck.gambapvp.battle.WagerData;
import com.munsterduck.gambapvp.util.CurrencyHelper;
import com.munsterduck.gambapvp.util.KitData;
import com.munsterduck.gambapvp.util.KitManager;
import com.munsterduck.gambapvp.util.PendingDuelManager;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
        CHANNEL.registerClientboundDeferred(ShowBattleHud.class);
        CHANNEL.registerClientboundDeferred(UpdateBattleScore.class);
        CHANNEL.registerClientboundDeferred(HideBattleHud.class);
        CHANNEL.registerClientboundDeferred(SendBalance.class);

        // Serverbound packets (real handlers)
        CHANNEL.registerServerbound(BattleRequestSend.class, (message, access) -> {
            ServerPlayerEntity sender = access.player();
            List<ServerPlayerEntity> targets = new ArrayList<>();

            for (String player : message.selectedOpponents()) {
                ServerPlayerEntity serverPlayer = sender.server.getPlayerManager().getPlayer(player);
                if (serverPlayer != null) {
                    targets.add(serverPlayer);
                }
            }

            if (!targets.isEmpty()) {
                for (ServerPlayerEntity target : targets) {
                    // Create and store the request
                    PendingDuelManager.DuelRequest request = new PendingDuelManager.DuelRequest(
                            sender.getUuid(),
                            sender.getName().getString(),
                            message.kitName(),
                            message.winsRequired(),
                            message.keepInventory(),
                            message.arenaId(),
                            message.arenaX(),
                            message.arenaY(),
                            message.arenaZ(),
                            message.arenaYaw(),
                            message.arenaPitch(),
                            message.arenaDimension(),
                            message.arenaRegionWidth(),
                            message.arenaRegionLength()
                    );

                    PendingDuelManager.addRequest(target.getUuid(), request);

                    // Store sender's wager
                    int wagerCurrency = 0;
                    int wagerItemCount = 0;
                    if (message.wagerData() != null) {
                        WagerData senderWager = WagerData.fromNbt(message.wagerData());
                        PendingDuelManager.setWager(request.requestId, sender.getUuid(), senderWager);
                        wagerCurrency = senderWager.getCurrency();
                        wagerItemCount = senderWager.getItems().size();
                    }

                    // Send to target with request ID and wager info
                    CHANNEL.serverHandle(target).send(new BattleRequest(
                            sender.getName().getString(),
                            message.kitName(),
                            message.winsRequired(),
                            message.keepInventory(),
                            request.requestId.toString(),
                            wagerCurrency,
                            wagerItemCount
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

        CHANNEL.registerServerbound(RequestBalance.class, (message, access) -> {
            int balance = CurrencyHelper.getBalance(access.player());
            CHANNEL.serverHandle(access.player()).send(new SendBalance(balance));
        });
    }

    public record OpenBattleScreen() {}

    public record BattleRequestSend(
            List<String> selectedOpponents,
            String kitName,
            int winsRequired,
            boolean keepInventory,
            String arenaId,
            double arenaX,
            double arenaY,
            double arenaZ,
            float arenaYaw,
            float arenaPitch,
            String arenaDimension,
            int arenaRegionWidth,
            int arenaRegionLength,
            NbtCompound wagerData
    ) {}

    public record BattleRequest(
            String senderName,
            String kitName,
            int winsRequired,
            boolean keepInventory,
            String requestId,
            int wagerCurrency,
            int wagerItemCount
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

    // Battle HUD packets
    public record PlayerScoreData(String playerUuid, String playerName, int score) {}

    public record ShowBattleHud(String battleId, int winsRequired, List<PlayerScoreData> players) {}

    public record UpdateBattleScore(String battleId, String playerName, int newScore) {}

    public record HideBattleHud(String battleId) {}

    // Currency balance packets
    public record RequestBalance() {}
    public record SendBalance(int balance) {}
}

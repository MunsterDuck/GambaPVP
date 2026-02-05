package com.munsterduck.gambapvp.battle;

import com.munsterduck.gambapvp.network.BattleRequestPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Manages the battle HUD scoreboard for participants.
 */
public class BattleHudManager {

    /**
     * Show the battle HUD to all participants.
     */
    public static void showHudForBattle(BattleData battle, MinecraftServer server) {
        List<BattleRequestPacket.PlayerScoreData> playerScores = new ArrayList<>();

        for (UUID playerUuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            String playerName = player != null ? player.getName().getString() : playerUuid.toString().substring(0, 8);
            playerScores.add(new BattleRequestPacket.PlayerScoreData(
                    playerUuid.toString(),
                    playerName,
                    battle.getScore(playerUuid)
            ));
        }

        BattleRequestPacket.ShowBattleHud packet = new BattleRequestPacket.ShowBattleHud(
                battle.getBattleId(),
                battle.getWinsRequired(),
                playerScores
        );

        for (UUID playerUuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                BattleRequestPacket.CHANNEL.serverHandle(player).send(packet);
            }
        }
    }

    /**
     * Update a player's score on the HUD for all participants.
     */
    public static void updateScore(BattleData battle, UUID scoredPlayerUuid, MinecraftServer server) {
        ServerPlayerEntity scoredPlayer = server.getPlayerManager().getPlayer(scoredPlayerUuid);
        String playerName = scoredPlayer != null ? scoredPlayer.getName().getString() : scoredPlayerUuid.toString().substring(0, 8);

        BattleRequestPacket.UpdateBattleScore packet = new BattleRequestPacket.UpdateBattleScore(
                battle.getBattleId(),
                playerName,
                battle.getScore(scoredPlayerUuid)
        );

        for (UUID playerUuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                BattleRequestPacket.CHANNEL.serverHandle(player).send(packet);
            }
        }
    }

    /**
     * Hide the battle HUD for all participants.
     */
    public static void hideHudForBattle(BattleData battle, MinecraftServer server) {
        BattleRequestPacket.HideBattleHud packet = new BattleRequestPacket.HideBattleHud(battle.getBattleId());

        for (UUID playerUuid : battle.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null) {
                BattleRequestPacket.CHANNEL.serverHandle(player).send(packet);
            }
        }
    }
}

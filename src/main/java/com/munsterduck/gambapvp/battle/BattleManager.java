package com.munsterduck.gambapvp.battle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BattleManager {
    private static final Map<String, BattleData> activeBattles = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerBattles = new ConcurrentHashMap<>();

    public static String createBattle(List<UUID> participants, String kitName,
                                      int winsRequired, boolean keepInventory) {
        String battleId = UUID.randomUUID().toString();
        BattleData battle = new BattleData(battleId, participants, kitName, winsRequired, keepInventory);

        activeBattles.put(battleId, battle);
        for (UUID uuid : participants) {
            playerBattles.put(uuid, battleId);
        }

        return battleId;
    }

    public static BattleData getBattle(String battleId) {
        return activeBattles.get(battleId);
    }

    public static BattleData getPlayerBattle(UUID playerUuid) {
        String battleId = playerBattles.get(playerUuid);
        return battleId != null ? activeBattles.get(battleId) : null;
    }

    public static void endBattle(String battleId, MinecraftServer server) {
        BattleData battle = activeBattles.remove(battleId);
        if (battle != null) {
            battle.setActive(false);

            // Remove player mappings
            for (UUID uuid : battle.getParticipants()) {
                playerBattles.remove(uuid);
            }

            // Clean up placed blocks
            // TODO: Implement block cleanup based on battle.getPlacedBlocks()
        }
    }

    public static boolean isInBattle(UUID playerUuid) {
        return playerBattles.containsKey(playerUuid);
    }
}

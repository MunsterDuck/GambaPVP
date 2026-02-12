package com.munsterduck.gambapvp.battle;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BattleManager {
    private static final Map<String, BattleData> activeBattles = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerBattles = new ConcurrentHashMap<>();

    public static String createBattle(List<UUID> participants, String kitName,
                                      int winsRequired, boolean keepInventory) {
        return createBattle(participants, kitName, winsRequired, keepInventory, null, null);
    }

    public static String createBattle(List<UUID> participants, String kitName,
                                      int winsRequired, boolean keepInventory,
                                      String arenaId, RegistryKey<World> battleWorld) {
        String battleId = UUID.randomUUID().toString();
        BattleData battle = new BattleData(battleId, participants, kitName, winsRequired, keepInventory, arenaId, battleWorld);

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

    /**
     * Find the battle that a placed entity (crystal, TNT, etc.) belongs to.
     */
    public static BattleData findBattleByPlacedEntity(UUID entityUuid) {
        for (BattleData battle : activeBattles.values()) {
            if (battle.isActive() && battle.isPlacedEntity(entityUuid)) {
                return battle;
            }
        }
        return null;
    }

    /**
     * Find the battle that a placed block belongs to.
     */
    public static BattleData findBattleByPlacedBlock(BlockPos pos) {
        for (BattleData battle : activeBattles.values()) {
            if (battle.isActive() && battle.getPlacedBlocks().contains(pos)) {
                return battle;
            }
        }
        return null;
    }
}

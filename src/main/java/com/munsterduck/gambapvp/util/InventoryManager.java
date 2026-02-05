package com.munsterduck.gambapvp.util;

import com.munsterduck.gambapvp.GambaPVP;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Manages inventory operations for battles including kit loading and restoration.
 */
public class InventoryManager {

    /**
     * Load a kit for a battle participant, backing up their current inventory.
     * @return true if kit was successfully loaded
     */
    public static boolean loadKitForBattle(ServerPlayerEntity player, String kitName, MinecraftServer server) {
        if (kitName == null || kitName.isEmpty()) {
            return false; // No kit specified
        }

        IEntityDataSaver playerData = (IEntityDataSaver) player;
        Inventory playerInv = player.getInventory();

        if (playerInv == null) {
            GambaPVP.LOGGER.error("Player {} has null inventory", player.getName().getString());
            return false;
        }

        // Load kit data
        NbtCompound kitData = KitManager.loadKit(server, kitName);
        if (kitData == null) {
            GambaPVP.LOGGER.error("Kit '{}' not found", kitName);
            return false;
        }

        // Only backup if not already backed up
        if (!playerData.getPersistentData().contains("inventory_backup")) {
            backupInventory(player);
        }

        // Clear current inventory
        playerInv.clear();

        // Load kit inventory into player
        NbtList kitItems = kitData.getList("Items", 10); // 10 = NbtCompound type
        for (int i = 0; i < kitItems.size(); i++) {
            NbtCompound itemTag = kitItems.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;

            if (slot >= 0 && slot < playerInv.size()) {
                ItemStack stack = ItemStack.fromNbt(itemTag);
                playerInv.setStack(slot, stack);
            }
        }

        GambaPVP.LOGGER.debug("Loaded kit '{}' for player {}", kitName, player.getName().getString());
        return true;
    }

    /**
     * Backup a player's current inventory.
     */
    public static void backupInventory(ServerPlayerEntity player) {
        IEntityDataSaver playerData = (IEntityDataSaver) player;
        Inventory playerInv = player.getInventory();

        NbtCompound backupData = new NbtCompound();
        NbtList backupItems = new NbtList();

        for (int i = 0; i < playerInv.size(); i++) {
            if (!playerInv.getStack(i).isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte) i);
                playerInv.getStack(i).writeNbt(itemTag);
                backupItems.add(itemTag);
            }
        }

        backupData.put("Items", backupItems);
        playerData.getPersistentData().put("inventory_backup", backupData);

        GambaPVP.LOGGER.debug("Backed up inventory for player {}", player.getName().getString());
    }

    /**
     * Restore a player's inventory from backup.
     * @return true if restoration was successful
     */
    public static boolean restoreInventoryFromBackup(ServerPlayerEntity player) {
        IEntityDataSaver playerData = (IEntityDataSaver) player;
        Inventory playerInv = player.getInventory();

        if (playerInv == null) {
            GambaPVP.LOGGER.error("Player {} has null inventory", player.getName().getString());
            return false;
        }

        // Check if backup exists
        if (!playerData.getPersistentData().contains("inventory_backup")) {
            GambaPVP.LOGGER.warn("No inventory backup found for player {}", player.getName().getString());
            return false;
        }

        // Load backup data
        NbtCompound backupData = playerData.getPersistentData().getCompound("inventory_backup");
        NbtList backupItems = backupData.getList("Items", 10);

        // Clear current inventory
        playerInv.clear();

        // Restore backup
        for (int i = 0; i < backupItems.size(); i++) {
            NbtCompound itemTag = backupItems.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;

            if (slot >= 0 && slot < playerInv.size()) {
                ItemStack stack = ItemStack.fromNbt(itemTag);
                playerInv.setStack(slot, stack);
            }
        }

        // Remove backup after restoring
        playerData.getPersistentData().remove("inventory_backup");

        GambaPVP.LOGGER.debug("Restored inventory for player {}", player.getName().getString());
        return true;
    }

    /**
     * Check if a player has a backed up inventory.
     */
    public static boolean hasBackup(ServerPlayerEntity player) {
        IEntityDataSaver playerData = (IEntityDataSaver) player;
        return playerData.getPersistentData().contains("inventory_backup");
    }

    /**
     * Clear a player's inventory backup without restoring.
     */
    public static void clearBackup(ServerPlayerEntity player) {
        IEntityDataSaver playerData = (IEntityDataSaver) player;
        playerData.getPersistentData().remove("inventory_backup");
    }
}

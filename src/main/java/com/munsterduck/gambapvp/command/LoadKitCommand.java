package com.munsterduck.gambapvp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.munsterduck.gambapvp.util.IEntityDataSaver;
import com.munsterduck.gambapvp.util.KitManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class LoadKitCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("gkit")
                    .then(CommandManager.literal("load")
                    .then(CommandManager.argument("kit_name", StringArgumentType.greedyString())
                    .suggests(KitManager.KIT_SUGGESTIONS)
                    .executes(LoadKitCommand::run)))
        );
    }

    public static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        IEntityDataSaver playerData = (IEntityDataSaver) player;
        Inventory playerInv = player.getInventory();

        if (playerInv == null) {
            context.getSource().sendFeedback(() -> Text.literal("Null Inventory, please try again."), false);
            return 0;
        }

        String kitName = StringArgumentType.getString(context, "kit_name").trim();

        if (kitName.isBlank()) {
            context.getSource().sendFeedback(() -> Text.literal("Kit Name must not be blank."), false);
            return 0;
        }

        // Load kit from server storage
        NbtCompound kitData = KitManager.loadKit(context.getSource().getServer(), kitName);

        if (kitData == null) {
            context.getSource().sendFeedback(() -> Text.literal("Kit '" + kitName + "' not found!"), false);
            return 0;
        }

        if (!playerData.getPersistentData().contains("inventory_backup")) {
            // Save current player inventory as backup
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

        context.getSource().sendFeedback(() -> Text.literal("Kit '" + kitName + "' loaded!"), false);
        return 1;
    }
}
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

public class LoadPreKitPlayerInventoryCommand {

    // Suggestion provider for kit names
    public static final SuggestionProvider<ServerCommandSource> KIT_SUGGESTIONS = (context, builder) -> {
        KitManager.getKitNames(context.getSource().getServer()).forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("gkit")
                    .then(CommandManager.literal("backup")
                    .executes(LoadPreKitPlayerInventoryCommand::run))
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

        // Check if backup exists
        if (!playerData.getPersistentData().contains("inventory_backup")) {
            context.getSource().sendFeedback(() -> Text.literal("No backed up player inventory found!"), false);
            return 0;
        }

        // Load backup data
        NbtCompound backupData = playerData.getPersistentData().getCompound("inventory_backup");
        NbtList backupItems = backupData.getList("Items", 10); // 10 = NbtCompound type

        if (backupItems.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("Backup inventory is empty!"), false);
            return 0;
        }

        // Clear current inventory
        playerInv.clear();

        // Load backup inventory into player
        for (int i = 0; i < backupItems.size(); i++) {
            NbtCompound itemTag = backupItems.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;

            if (slot >= 0 && slot < playerInv.size()) {
                ItemStack stack = ItemStack.fromNbt(itemTag);
                playerInv.setStack(slot, stack);
            }
        }

        // Remove backup after loading (optional - comment out if you want to keep it)
        playerData.getPersistentData().remove("inventory_backup");

        context.getSource().sendFeedback(() -> Text.literal("Backup inventory loaded!"), false);
        return 1;
    }
}
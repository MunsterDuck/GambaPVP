package com.munsterduck.gambapvp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.munsterduck.gambapvp.util.KitData;
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

public class SaveKitCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("gkit")
                        .then(CommandManager.literal("save")
                                .then(CommandManager.argument("kit_name", StringArgumentType.string())
                                        .executes(SaveKitCommand::run)))
        );
    }

    public static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        Inventory playerInv = player.getInventory();

        if (playerInv == null) {
            context.getSource().sendFeedback(() -> Text.literal("Null Inventory, please try again."), false);
            return 0;
        }

        String kitName = StringArgumentType.getString(context, "kit_name");

        // Serialize inventory to NBT
        NbtCompound kitData = new NbtCompound();
        NbtList itemsList = new NbtList();

        for (int i = 0; i < playerInv.size(); i++) {
            if (!playerInv.getStack(i).isEmpty()) {
                NbtCompound itemTag = new NbtCompound();
                itemTag.putByte("Slot", (byte) i);
                playerInv.getStack(i).writeNbt(itemTag);
                itemsList.add(itemTag);
            }
        }

        kitData.put("Items", itemsList);

        // Use main hand item as icon (or first item in hotbar)
        ItemStack iconItem = player.getMainHandStack();
        if (iconItem.isEmpty() && !playerInv.getStack(0).isEmpty()) {
            iconItem = playerInv.getStack(0);
        }

        // Save kit with icon
        KitData kit = new KitData(kitName, kitData, iconItem.copy());
        KitManager.saveKit(context.getSource().getServer(), kit);

        context.getSource().sendFeedback(() -> Text.literal("Kit '" + kitName + "' saved with icon!"), true);
        return 1;
    }
}
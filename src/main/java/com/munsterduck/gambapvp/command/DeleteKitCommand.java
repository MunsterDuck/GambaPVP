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

public class DeleteKitCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess,
                                CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("gkit")
                    .then(CommandManager.literal("delete")
                    .then(CommandManager.argument("kit_name", StringArgumentType.greedyString())
                    .suggests(KitManager.KIT_SUGGESTIONS)
                    .executes(DeleteKitCommand::run)))
        );
    }

    public static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String kitName = StringArgumentType.getString(context, "kit_name").trim();

        if (kitName.isBlank()) {
            context.getSource().sendFeedback(() -> Text.literal("Kit Name must not be blank."), false);
            return 0;
        }

        boolean success = KitManager.deleteKit(context.getSource().getServer(), kitName);

        if (success) {
            context.getSource().sendFeedback(() -> Text.literal("Kit '" + kitName + "' deleted successfully!"), true);
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.literal("Kit '" + kitName + "' not found!"), false);
            return 0;
        }
    }
}
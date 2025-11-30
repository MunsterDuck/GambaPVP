package com.munsterduck.gambapvp.util;

import com.munsterduck.gambapvp.block.ModBlocks;
import com.munsterduck.gambapvp.command.*;
import com.munsterduck.gambapvp.item.ModItems;
import com.munsterduck.gambapvp.network.NetworkHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModRegistries {
    public static void registerModStuffs() {
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        NetworkHandler.registerServerPackets();
        registerCommands();
    }
    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(GBattleCommand::register);
        CommandRegistrationCallback.EVENT.register(SaveKitCommand::register);
        CommandRegistrationCallback.EVENT.register(LoadKitCommand::register);
        CommandRegistrationCallback.EVENT.register(LoadPreKitPlayerInventoryCommand::register);
        CommandRegistrationCallback.EVENT.register(DeleteKitCommand::register);
    }
}

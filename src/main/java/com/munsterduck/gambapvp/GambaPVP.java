package com.munsterduck.gambapvp;

import com.mojang.brigadier.context.CommandContext;
import com.munsterduck.gambapvp.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.munsterduck.gambapvp.screen.BattleSetupScreenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class GambaPVP implements ModInitializer {
	public static final String MOD_ID = "gambapvp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing GambaPVP mod");

        ModItems.registerModItems();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("gbattle").executes(this::executeCommand)
            );
        });
    }

    // Register the screen handler type
    public static final ScreenHandlerType<BattleSetupScreenHandler> BATTLE_SETUP_SCREEN_HANDLER =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    new Identifier(MOD_ID, "battle_setup"),
                    new ExtendedScreenHandlerType<>(BattleSetupScreenHandler::new)
            );

    private int executeCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();

            if (source.getPlayer() == null) {
                source.sendError(Text.literal("Only players can use this command!"));
                return 0;
            }

            ServerPlayerEntity player = source.getPlayer();

            // Collect online player names
            List<String> playerNames = player.getServer()
                    .getPlayerManager()
                    .getPlayerList()
                    .stream()
                    .map(PlayerEntity::getEntityName)
                    .toList();

            // Open the GUI with player list
            player.openHandledScreen(new BattleSetupScreenHandler.Factory(playerNames));

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing gbattle command", e);
            context.getSource().sendError(Text.literal("Error opening battle GUI: " + e.getMessage()));
            return 0;
        }
    }
}
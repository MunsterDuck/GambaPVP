package com.munsterduck.gambapvp;

import com.munsterduck.gambapvp.util.ModRegistries;
import com.munsterduck.gambapvp.util.ModTags;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import com.munsterduck.gambapvp.screen.BattleSetupScreenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class GambaPVP implements ModInitializer {
	public static final String MOD_ID = "gambapvp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // This will be used to determine whether the betting item is in the TAG for gamblable items.
    private boolean isGamblableCurrency(Item item) {
        return item.getDefaultStack().isIn(ModTags.Items.GAMBAPVP_GAMBLABLE_CURRENCIES);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing GambaPVP mod");

        ModRegistries.registerModStuffs();
    }

    // Register the screen handler type
    public static final ScreenHandlerType<BattleSetupScreenHandler> BATTLE_SETUP_SCREEN_HANDLER =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    new Identifier(MOD_ID, "battle_setup"),
                    new ExtendedScreenHandlerType<>(BattleSetupScreenHandler::new)
            );

    public static int openBattleSetup(ServerPlayerEntity player) {
        try {
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
            LOGGER.error("Error opening battle setup GUI", e);
            return 0;
        }
    }
}

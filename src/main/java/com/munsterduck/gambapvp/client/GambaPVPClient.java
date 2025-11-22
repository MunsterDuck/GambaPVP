package com.munsterduck.gambapvp.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.client.screen.BattleSetupScreen;

public class GambaPVPClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register the screen with the screen handler
        HandledScreens.register(GambaPVP.BATTLE_SETUP_SCREEN_HANDLER, BattleSetupScreen::new);

        GambaPVP.LOGGER.info("GambaPVP client initialized");
    }
}
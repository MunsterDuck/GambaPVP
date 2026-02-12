package com.munsterduck.gambapvp.client;

import com.munsterduck.gambapvp.network.BattleRequestPacketClient;
import net.fabricmc.api.ClientModInitializer;
import com.munsterduck.gambapvp.GambaPVP;
public class GambaPVPClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GambaPVP.LOGGER.info("GambaPVP client initialized");

        BattleRequestPacketClient.init();
    }
}
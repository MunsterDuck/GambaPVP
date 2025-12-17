package com.munsterduck.gambapvp;

import com.munsterduck.gambapvp.network.BattleRequestPacket;
import com.munsterduck.gambapvp.util.ModRegistries;
import com.munsterduck.gambapvp.util.ModTags;
import com.munsterduck.gambapvp.util.PendingDuelManager;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.hud.Hud;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GambaPVP implements ModInitializer {
	public static final String MOD_ID = "gambapvp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing GambaPVP mod");

        ModRegistries.registerModStuffs();
        BattleRequestPacket.registerPacketsCommon();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Clean up every 60 seconds
            if (server.getTicks() % 1200 == 0) {
                PendingDuelManager.cleanupExpiredRequests();
            }
        });
    }
}

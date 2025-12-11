package com.munsterduck.gambapvp.client;

import com.munsterduck.gambapvp.gui.NewBattleSetupScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.hud.Hud;
import net.fabricmc.api.ClientModInitializer;
import com.munsterduck.gambapvp.GambaPVP;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class GambaPVPClient implements ClientModInitializer {
    private static final KeyBinding TOGGLE_HUD = new KeyBinding("key.gambapvp.toggle_hud", GLFW.GLFW_KEY_H, "key.categories.misc");
    private static final KeyBinding OPEN_BATTLE = new KeyBinding("key.gambapvp.open_battle", GLFW.GLFW_KEY_G, "key.categories.misc");
    private static final Identifier HUD_ID = new Identifier("gambapvp", "hud");

    @Override
    public void onInitializeClient() {
        GambaPVP.LOGGER.info("GambaPVP client initialized");

        KeyBindingHelper.registerKeyBinding(TOGGLE_HUD);
        KeyBindingHelper.registerKeyBinding(OPEN_BATTLE);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_BATTLE.wasPressed()) {
                client.setScreen(new NewBattleSetupScreen());
            }
            while (TOGGLE_HUD.wasPressed()) {
                if (Hud.getComponent(HUD_ID) == null) {
                    this.addHud();
                } else {
                    Hud.remove(HUD_ID);
                }
            }
        });
    }

    private void addHud() {
        Hud.add(HUD_ID, () ->
                Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(
                                Text.empty()
                                        .append(Text.literal("! ").formatted(Formatting.YELLOW, Formatting.BOLD))
                                        .append(" Press ")
                                        .append(KeyBindingHelper.getBoundKeyOf(TOGGLE_HUD).getLocalizedText().copy().formatted(Formatting.BLUE))
                                        .append(" to\ntoggle GambaPVP HUD!")
                        ).horizontalTextAlignment(HorizontalAlignment.CENTER).shadow(true))
                        .surface(Surface.flat(0x77000000).and(Surface.outline(0xFF121212)))
                        .padding(Insets.of(5))
                        .positioning(Positioning.relative(100, 35))
        );
    }
}
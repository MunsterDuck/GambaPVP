package com.munsterduck.gambapvp.gui;

import com.munsterduck.gambapvp.network.BattleRequestPacket;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.munsterduck.gambapvp.GambaPVP.MOD_ID;

/**
 * Example screen shown when a player receives a battle request
 * This demonstrates how to handle incoming battle requests
 */
public class BattleAcceptScreen extends BaseUIModelScreen<FlowLayout> {
    private final BattleRequestPacket.BattleRequest request;

    public BattleAcceptScreen(BattleRequestPacket.BattleRequest request) {
        super(FlowLayout.class, DataSource.asset(new Identifier(MOD_ID, "battle_accept")));
        this.request = request;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // Update labels with request info
        rootComponent.childById(LabelComponent.class, "sender-label")
                .text(Text.literal(request.senderName() + " challenges you!"));

        rootComponent.childById(LabelComponent.class, "kit-label")
                .text(Text.literal("Kit: " + request.kitName()));

        rootComponent.childById(LabelComponent.class, "rules-label")
                .text(Text.literal("First to " + request.winsRequired() + " wins"));

        rootComponent.childById(LabelComponent.class, "inventory-label")
                .text(Text.literal("Keep Inventory: " + (request.keepInventory() ? "ON" : "OFF")));

        // Accept button
        rootComponent.childById(ButtonComponent.class, "accept-button").onPress(button -> {
            respondToBattle(true);
        });

        // Decline button
        rootComponent.childById(ButtonComponent.class, "decline-button").onPress(button -> {
            respondToBattle(false);
        });
    }

    private void respondToBattle(boolean accepted) {
        // Send response to server
        BattleRequestPacket.CHANNEL.clientHandle().send(
                new BattleRequestPacket.BattleResponse(
                        request.senderName(),
                        accepted
                )
        );

        this.close();
    }

    @Override
    public boolean shouldPause() {
        return true; // Pause game while deciding
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Force player to accept or decline
    }
}

/**
 * Example of handling incoming battle requests in your mod initializer:
 *
 * In BattleRequestPacket.init():
 *
 * CHANNEL.registerClientbound(BattleRequest.class, (message, access) -> {
 *     MinecraftClient client = MinecraftClient.getInstance();
 *     client.execute(() -> {
 *         client.setScreen(new BattleAcceptScreen(message));
 *     });
 * });
 */
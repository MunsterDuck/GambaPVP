package com.munsterduck.gambapvp.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.screen.BattleSetupScreenHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattleSetupScreen extends HandledScreen<BattleSetupScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(GambaPVP.MOD_ID, "textures/gui/battle_setup.png");

    private final Map<String, CheckboxWidget> playerCheckboxes = new HashMap<>();
    private final List<String> selectedPlayers = new ArrayList<>();

    public BattleSetupScreen(BattleSetupScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 166;
        this.backgroundWidth = 166;
    }

    @Override
    protected void init() {
        super.init();

        // Center the title
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;

        // Clear previous selections
        playerCheckboxes.clear();
        selectedPlayers.clear();

        // Get player names from the handler
        List<String> playerNames = this.handler.getPlayerNames();

        // Create checkboxes for each player
        // int startY = this.y + 30;
        // int currentY = startY;
        int currentY = this.y + 30;

        for (String playerName : playerNames) {
            CheckboxWidget checkbox = new CheckboxWidget(
                    this.x + 20,
                    currentY,
                    20,
                    20,
                    Text.literal(playerName),
                    false
            );

            // Store reference to checkbox
            playerCheckboxes.put(playerName, checkbox);

            // Add to screen
            this.addDrawableChild(checkbox);

            currentY += 25;
        }

        // Add Start Battle button
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Start Battle"), button -> {
                            // Collect selected players
                            selectedPlayers.clear();
                            for (Map.Entry<String, CheckboxWidget> entry : playerCheckboxes.entrySet()) {
                                if (entry.getValue().isChecked()) {
                                    selectedPlayers.add(entry.getKey());
                                }
                            }

                            // Validate selection
                            if (selectedPlayers.size() < 2) {
                                // Show error - need at least 2 players
                                GambaPVP.LOGGER.info("Need at least 2 players selected!");
                                return;
                            }

                            // TODO: Send packet to server with selected players
                            GambaPVP.LOGGER.info("Starting battle with players: " + selectedPlayers);
                            this.close();
                        })
                        .dimensions(this.x + 15, this.y + Math.max(currentY + 10, this.y + 80), 136, 20)
                        .build()
        );

        // Add Cancel button
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Cancel"), button -> {
                            this.close();
                        })
                        .dimensions(this.x + 15, this.y + Math.max(currentY + 35, this.y + 100), 136, 20)
                        .build()
        );
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(
                TEXTURE,
                x, y,
                0, 0,
                this.backgroundWidth,
                this.backgroundHeight,
                this.backgroundWidth,
                this.backgroundHeight
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw instruction text
        context.drawText(
                this.textRenderer,
                Text.literal("Select players (min 2):"),
                this.x + 20,
                this.y + 15,
                0xFFFFFF,
                false
        );

        // Draw error message if needed
        if (playerCheckboxes.isEmpty()) {
            context.drawText(
                    this.textRenderer,
                    Text.literal("No players online!"),
                    this.x + 20,
                    this.y + 40,
                    0xFF5555,
                    false
            );
        }
    }
}
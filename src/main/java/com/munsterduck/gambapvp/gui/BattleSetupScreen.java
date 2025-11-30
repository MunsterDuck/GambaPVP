package com.munsterduck.gambapvp.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class BattleSetupScreen extends Screen {
    private final Screen parent;
    private int setupStep = 0;
    private String selectedKit = null;
    private int winsRequired = 3;
    private boolean keepInventory = true;

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;

    public BattleSetupScreen(Screen parent) {
        super(Text.literal("Battle Setup"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        switch (setupStep) {
            case 0 -> initKitSelection();
            case 1 -> initRulesSetup();
            case 2 -> initPlayerSelection();
            case 3 -> initWagerScreen();
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"),
                        button -> {
                            if (setupStep > 0) {
                                setupStep--;
                                this.clearAndInit();
                            } else {
                                this.close();
                            }
                        })
                .dimensions(10, this.height - 30, 80, 20)
                .build());
    }

    private void initKitSelection() {
        // Immediately open the chest-style kit selection GUI
        this.client.setScreen(new KitSelectionScreen(this, kitName -> {
            this.selectedKit = kitName;
            this.nextStep();
        }));
    }

    private void initRulesSetup() {
        int y = 60;

        // Show selected kit
        String kitDisplay = selectedKit != null ? selectedKit : "No Kit (Vanilla)";
        // context -> context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Selected: " + kitDisplay), this.width / 2, y - 20, 0xFFFFFF);

        y += 20;

        for (int wins : new int[]{1, 3, 5, 7}) {
            final int w = wins;
            String label = "First to " + wins + (winsRequired == wins ? " ✓" : "");
            this.addDrawableChild(ButtonWidget.builder(Text.literal(label),
                            button -> {
                                winsRequired = w;
                                this.clearAndInit();
                            })
                    .dimensions(this.width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            y += 25;
        }

        y += 20;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Keep Inventory: " + (keepInventory ? "ON" : "OFF")),
                        button -> {
                            keepInventory = !keepInventory;
                            this.clearAndInit();
                        })
                .dimensions(this.width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        y += 40;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Next"),
                        button -> nextStep())
                .dimensions(this.width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void initPlayerSelection() {
        int y = 60;

        // TODO: Get actual online players
        for (String player : new String[]{"Player1", "Player2", "Player3"}) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal(player),
                            button -> nextStep())
                    .dimensions(this.width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
            y += 25;
        }
    }

    private void initWagerScreen() {
        int y = 80;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Wager Items (Coming Soon)"),
                        button -> {})
                .dimensions(this.width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        y += 60;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Send Battle Request"),
                        button -> this.close())
                .dimensions(this.width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void nextStep() {
        setupStep++;
        this.clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        String title = switch (setupStep) {
            case 0 -> "Select Your Kit";
            case 1 -> "Battle Rules";
            case 2 -> "Select Opponent";
            case 3 -> "Wager Items";
            default -> "Battle Setup";
        };

        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
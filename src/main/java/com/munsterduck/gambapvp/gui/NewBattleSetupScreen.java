package com.munsterduck.gambapvp.gui;

import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.network.BattleRequestPacket;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;

import static com.munsterduck.gambapvp.GambaPVP.MOD_ID;

public class NewBattleSetupScreen extends BaseUIModelScreen<FlowLayout> {
    private int setupStep = 0;
    private String selectedKit = null;
    private String selectedOpponent = null;
    private int winsRequired = 3;
    private boolean keepInventory = true;

    // Component references
    private FlowLayout rootComponent;
    private LabelComponent titleLabel;
    private LabelComponent selectedKitLabel;
    private FlowLayout kitSelectionStep;
    private FlowLayout rulesStep;
    private FlowLayout playerSelectionStep;
    private FlowLayout wagerStep;
    private ButtonComponent keepInventoryButton;
    private FlowLayout kitGrid;

    public NewBattleSetupScreen() {
        super(FlowLayout.class, DataSource.asset(new Identifier(MOD_ID, "battle_setup")));
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = rootComponent;

        titleLabel = rootComponent.childById(LabelComponent.class, "title-label");
        selectedKitLabel = rootComponent.childById(LabelComponent.class, "selected-kit-label");

        kitSelectionStep = rootComponent.childById(FlowLayout.class, "kit-selection-step");
        rulesStep = rootComponent.childById(FlowLayout.class, "rules-step");
        playerSelectionStep = rootComponent.childById(FlowLayout.class, "player-selection-step");
        wagerStep = rootComponent.childById(FlowLayout.class, "wager-step");
        kitGrid = rootComponent.childById(FlowLayout.class, "kit-grid");

        rootComponent.childById(ButtonComponent.class, "back-button").onPress(button -> {
            if (setupStep > 0) {
                setupStep--;
                updateStep();
            } else {
                this.close();
            }
        });

        setupKitSelection();
        setupRulesStep(rootComponent);
        setupPlayerSelection(rootComponent);
        setupWagerStep(rootComponent);

        updateStep();
    }

    private void setupKitSelection() {
        // Request kits from server
        BattleRequestPacket.CHANNEL.clientHandle().send(new BattleRequestPacket.RequestKits());

        // Populate kit grid
        populateKitGrid();
    }

    public void refreshKits() {
        populateKitGrid();
    }

    private void populateKitGrid() {
        kitGrid.clearChildren();

        var kits = ClientKitCache.getKits();

        // Add each kit using the template
        for (var kit : kits) {
            String kitName = kit.name;

            Identifier itemId = Registries.ITEM.getId(kit.icon.getItem());

            FlowLayout kitButton = this.model.expandTemplate(
                    FlowLayout.class,
                    "kit-button",
                    Map.of(
                            "kit-name", kitName,
                            "kit-stack", itemId.toString()
                    )
            );

            // Make clickable
            kitButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    selectedKit = kitName;
                    BattleRequestPacket.CHANNEL.clientHandle().send(new BattleRequestPacket.KitSelected(kitName));
                    nextStep();
                    return true;
                }
                return false;
            });

            kitGrid.child(kitButton);
        }

        // Add "No Kit" option using template
        FlowLayout noKitButton = this.model.expandTemplate(
                FlowLayout.class,
                "kit-button@gambapvp:battle_setup",
                Map.of(
                        "kit-name", "No Kit",
                        "kit-stack", "minecraft:barrier"
                )
        );

        noKitButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button == 0) {
                selectedKit = null;
                nextStep();
                return true;
            }
            return false;
        });

        kitGrid.child(noKitButton);
    }

    private void setupRulesStep(FlowLayout root) {
        setupWinButton(root, "wins-1-button", 1);
        setupWinButton(root, "wins-3-button", 3);
        setupWinButton(root, "wins-5-button", 5);
        setupWinButton(root, "wins-7-button", 7);

        keepInventoryButton = root.childById(ButtonComponent.class, "keep-inventory-button");
        keepInventoryButton.onPress(button -> {
            keepInventory = !keepInventory;
            updateKeepInventoryButton();
        });

        root.childById(ButtonComponent.class, "rules-next-button").onPress(button -> {
            nextStep();
        });
    }

    private void setupWinButton(FlowLayout root, String id, int wins) {
        root.childById(ButtonComponent.class, id).onPress(button -> {
            winsRequired = wins;
            updateWinButtons();
        });
    }

    private void setupPlayerSelection(FlowLayout root) {
        FlowLayout playerList = root.childById(FlowLayout.class, "player-list");

        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            for (PlayerListEntry entry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
                String playerName = entry.getProfile().getName();

                if (playerName.equals(MinecraftClient.getInstance().player.getName().getString())) {
                    continue;
                }

                ButtonComponent playerButton = Components.button(
                        Text.literal(playerName),
                        button -> {
                            selectedOpponent = playerName;
                            nextStep();
                        }
                );

                playerButton.horizontalSizing(Sizing.fixed(200));
                playerList.child(playerButton);
            }
        } else {
            for (String player : new String[]{"Player1", "Player2", "Player3"}) {
                ButtonComponent playerButton = Components.button(
                        Text.literal(player),
                        button -> {
                            selectedOpponent = player;
                            nextStep();
                        }
                );
                playerButton.horizontalSizing(Sizing.fixed(200));
                playerList.child(playerButton);
            }
        }
    }

    private void setupWagerStep(FlowLayout root) {
        root.childById(ButtonComponent.class, "add-wager-button").onPress(button -> {
            // TODO: Open wager item selection
        });

        root.childById(ButtonComponent.class, "send-request-button").onPress(button -> {
            sendBattleRequest();
            this.close();
        });
    }

    private void sendBattleRequest() {
        if (selectedOpponent == null) {
            System.err.println("No opponent selected!");
            return;
        }

        String kitName = selectedKit != null ? selectedKit : "vanilla";

        BattleRequestPacket.CHANNEL.clientHandle().send(
                new BattleRequestPacket.BattleRequestSend(
                        selectedOpponent,
                        kitName,
                        winsRequired,
                        keepInventory
                )
        );
    }

    private void updateStep() {
        FlowLayout contentContainer = rootComponent.childById(FlowLayout.class, "content-container");
        contentContainer.clearChildren();

        FlowLayout current = switch (setupStep) {
            case 0 -> kitSelectionStep;
            case 1 -> {
                updateSelectedKitLabel();
                updateWinButtons();
                updateKeepInventoryButton();
                yield rulesStep;
            }
            case 2 -> playerSelectionStep;
            case 3 -> wagerStep;
            default -> kitSelectionStep;
        };

        contentContainer.child(current);

        // Update title
        String title = switch (setupStep) {
            case 0 -> "Select Your Kit";
            case 1 -> "Battle Rules";
            case 2 -> "Select Opponent";
            case 3 -> "Wager Items";
            default -> "Battle Setup";
        };
        titleLabel.text(Text.literal(title));
    }

    private void nextStep() {
        setupStep++;
        updateStep();
    }

    private void updateSelectedKitLabel() {
        String kitDisplay = selectedKit != null ? selectedKit : "No Kit (Vanilla)";
        selectedKitLabel.text(Text.literal("Selected: " + kitDisplay));
    }

    private void updateWinButtons() {
        updateWinButtonText("wins-1-button", 1);
        updateWinButtonText("wins-3-button", 3);
        updateWinButtonText("wins-5-button", 5);
        updateWinButtonText("wins-7-button", 7);
    }

    private void updateWinButtonText(String id, int wins) {
        ButtonComponent button = rulesStep.childById(ButtonComponent.class, id);
        String label = "First to " + wins + (winsRequired == wins ? " ✓" : "");
        button.setMessage(Text.literal(label));
    }

    private void updateKeepInventoryButton() {
        keepInventoryButton.setMessage(Text.literal("Keep Inventory: " + (keepInventory ? "ON" : "OFF")));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
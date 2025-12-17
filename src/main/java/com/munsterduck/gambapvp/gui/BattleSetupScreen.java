package com.munsterduck.gambapvp.gui;

import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.network.BattleRequestPacket;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.reflect.Array;
import java.util.*;

import static com.munsterduck.gambapvp.GambaPVP.MOD_ID;

public class BattleSetupScreen extends BaseUIModelScreen<FlowLayout> {
    private int setupStep = 0;
    private String selectedKit = null;
    private List<String> selectedOpponents = new ArrayList<>();
    private int winsRequired = 3;
    private boolean keepInventory = true;

    // Component references
    private FlowLayout rootComponent;
    private LabelComponent titleLabel;
    private FlowLayout kitSelectionStep;
    private FlowLayout rulesStep;
    private FlowLayout playerSelectionStep;
    private FlowLayout wagerStep;
    private FlowLayout keepInventoryButton;
    private FlowLayout kitGrid;
    private FlowLayout rulesGrid;
    private ButtonComponent backButton;
    private ButtonComponent nextButton;
    private Map<FlowLayout, Integer> winButtonValues = new HashMap<>();

    public BattleSetupScreen() {
        super(FlowLayout.class, DataSource.asset(new Identifier(MOD_ID, "battle_setup")));
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = rootComponent;

        titleLabel = rootComponent.childById(LabelComponent.class, "title-label");

        kitSelectionStep = rootComponent.childById(FlowLayout.class, "kit-selection-step");
        rulesStep = rootComponent.childById(FlowLayout.class, "rules-step");
        playerSelectionStep = rootComponent.childById(FlowLayout.class, "player-selection-step");
        wagerStep = rootComponent.childById(FlowLayout.class, "wager-step");
        kitGrid = rootComponent.childById(FlowLayout.class, "kit-grid");
        rulesGrid = rootComponent.childById(FlowLayout.class, "rules-grid");
        nextButton = rootComponent.childById(ButtonComponent.class, "next-button");
        backButton = rootComponent.childById(ButtonComponent.class, "back-button");

        //Create Back Button
        backButton.onPress(button -> {
            lastStep();
        });

        //Create Next Button
        nextButton.onPress(button -> {
            nextStep();
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
            if (kit.icon.getItem() == Items.AIR) {
                itemId = Registries.ITEM.getId(Items.BARRIER);
            }

            FlowLayout kitButton = this.model.expandTemplate(
                    FlowLayout.class,
                    "kit-button",
                    Map.of(
                            "kit-name", kitName,
                            "kit-stack", itemId.toString()
                    )
            );

            // Create tooltip with kit contents
            List<Text> tooltipLines = new ArrayList<>();
            tooltipLines.add(Text.literal(kitName).styled(style -> style.withColor(0xFFFFFF)));

            if (kit.items.isEmpty()) {
                tooltipLines.add(Text.literal("Empty kit").styled(style -> style.withColor(0x888888).withItalic(true)));
            } else {
                // Group identical items and preserve one stack for rarity color
                Map<String, ItemStackInfo> itemInfo = new LinkedHashMap<>();

                for (ItemStack stack : kit.items) {
                    String key = stack.getItem().toString();
                    if (!itemInfo.containsKey(key)) {
                        itemInfo.put(key, new ItemStackInfo(stack.copy(), 0));
                    }
                    itemInfo.get(key).count += stack.getCount();
                }

                // Add to tooltip with rarity colors
                int count = 0;
                for (Map.Entry<String, ItemStackInfo> entry : itemInfo.entrySet()) {
                    if (count >= 10) {
                        tooltipLines.add(Text.literal("... and " + (itemInfo.size() - 10) + " more")
                                .styled(style -> style.withColor(0x888888).withItalic(true)));
                        break;
                    }

                    ItemStack displayStack = entry.getValue().stack;
                    int totalCount = entry.getValue().count;

                    // Use item's rarity color
                    int rarityColor = displayStack.getRarity().formatting.getColorValue() != null
                            ? displayStack.getRarity().formatting.getColorValue()
                            : 0xAAAAAA;

                    String itemName = displayStack.getName().getString();
                    String countText = totalCount > 1 ? " ×" + totalCount : "";

                    tooltipLines.add(Text.literal("■ " + itemName + countText).styled(style -> style.withColor(rarityColor)));

                    count++;
                }

                tooltipLines.add(Text.literal(""));
                tooltipLines.add(Text.literal("Total: " + itemInfo.size() + " item type" +
                                (itemInfo.size() == 1 ? "" : "s"))
                        .styled(style -> style.withColor(0x888888)));
            }

            // Apply the tooltip
            kitButton.tooltip(tooltipLines);

            // Make clickable
            kitButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    selectedKit = kitName;
                    nextStep();
                    return true;
                }
                return false;
            });

            kitGrid.child(kitButton);
        }

        // Add "No Kit" option
        FlowLayout noKitButton = this.model.expandTemplate(
            FlowLayout.class,
            "kit-button@gambapvp:battle_setup",
            Map.of(
                "kit-name", "No Kit",
                "kit-stack", "minecraft:barrier"
            )
        );

        noKitButton.tooltip(List.of(
                Text.literal("No Kit").styled(style -> style.withColor(0xFFFFFF)),
                Text.literal("Use your own Inventory!").styled(style -> style.withColor(0xAAAAAA))
        ));

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

    // Helper class to store stack and count
    private static class ItemStackInfo {
        ItemStack stack;
        int count;

        ItemStackInfo(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }

    private void setupRulesStep(FlowLayout root) {

        int[] winCons = new int[]{1, 3, 5, 7};

        for (int winCount : winCons) {
            setupWinButton(winCount);
        }

        keepInventoryButton = root.childById(FlowLayout.class, "keep-inventory-button");
        keepInventoryButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button == 0) {
                keepInventory = !keepInventory;
                updateKeepInventoryButton();
                return true;
            }
            return false;
        });
    }

    private void setupWinButton(int wins) {
        FlowLayout winButton = this.model.expandTemplate(
            FlowLayout.class,
            "win-button",
            Map.of(
                    "wins", String.valueOf(wins)
            )
        );

        // Store the win button and value for future reference
        winButtonValues.put(winButton, wins);

        winButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button == 0) {
                winsRequired = wins;
                updateWinButtons();
                return true;
            }
            return false;
        });

        rulesGrid.child(winButton);
    }

    private void setupPlayerSelection(FlowLayout root) {
        FlowLayout playerList = root.childById(FlowLayout.class, "player-list");
        selectedOpponents.clear();

        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            Collection<PlayerListEntry> serverPlayerList = MinecraftClient.getInstance().getNetworkHandler().getPlayerList();
            if (serverPlayerList.size() > 1) {
                playerList.clearChildren();
            }
            for (PlayerListEntry entry : serverPlayerList) {
                String playerName = entry.getProfile().getName();

                if (playerName.equals(MinecraftClient.getInstance().player.getName().getString())) {
                    continue;
                }

                FlowLayout playerButton = model.expandTemplate(
                        FlowLayout.class,
                        "player-button",
                        Map.of("player-name", playerName)
                );

                // Set skin texture
                Identifier skin = entry.getSkinTexture();
                if (skin == null) {
                    skin = DefaultSkinHelper.getTexture(entry.getProfile().getId());
                }
                TextureComponent face = Components.texture(skin, 20, 20, 20, 20, 160, 160);
                TextureComponent hat = Components.texture(skin, 100, 20, 20, 20, 160, 160);
                StackLayout faceStack = playerButton.childById(StackLayout.class, "player-face-stack");

                faceStack.clearChildren();

                // Add base face and hat layers
                faceStack.child(face);
                faceStack.child(hat);

                // Make clickable
                playerButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    if (button == 0) {
                        CheckboxComponent checkbox = playerButton.childById(CheckboxComponent.class, "player-checkbox");
                        if (selectedOpponents.contains(playerName)) {
                            selectedOpponents.remove(playerName);
                            checkbox.checked(false);
                        } else {
                            selectedOpponents.add(playerName);
                            checkbox.checked(true);
                        }
                        shouldHideNext();
                        return true;
                    }
                    return false;
                });

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
        if (selectedOpponents == null) {
            System.err.println("No opponent(s) selected!");
            return;
        }

        String kitName = selectedKit != null ? selectedKit : "";

        BattleRequestPacket.CHANNEL.clientHandle().send(
                new BattleRequestPacket.BattleRequestSend(
                        selectedOpponents,
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
                updateWinButtons();
                updateKeepInventoryButton();
                yield rulesStep;
            }
            case 2 -> playerSelectionStep;
            case 3 -> wagerStep;
            default -> kitSelectionStep;
        };

        shouldHideNext();

        contentContainer.child(current);

        // Update title
        String title = switch (setupStep) {
            case 0 -> "Select Your Kit";
            case 1 -> "Win Conditions";
            case 2 -> "Select Opponent(s)";
            case 3 -> "Wager Items";
            default -> "Battle Setup";
        };
        titleLabel.text(Text.literal(title));

        if (selectedKit == null) {
            keepInventoryButton.positioning(Positioning.layout());
        } else {
            //Kit Inventories should always keep
            keepInventory = true;
            keepInventoryButton.positioning(Positioning.absolute(0, -500));
        }
    }

    private void nextStep() {
        setupStep++;
        updateStep();
    }

    private void lastStep() {
        if (setupStep > 0) {
            setupStep--;
            updateStep();
        } else {
            this.close();
        }
    }

    private void shouldHideNext() {
        if (setupStep < 1) {
            editNextButton(false, false);
        } else if (setupStep == 2 && selectedOpponents.isEmpty()) {
            editNextButton(false, true);
        } else {
            editNextButton(true, true);
        }
    }

    private void editNextButton(boolean isActive, boolean isVisible) {
        nextButton.active(isActive);
        nextButton.visible = isVisible;
        nextButton.horizontalSizing(isVisible ? Sizing.content() : Sizing.fixed(0));
    }

    private void updateWinButtons() {
        for (FlowLayout winButton : winButtonValues.keySet()) {
            CheckboxComponent checkbox = winButton.childById(CheckboxComponent.class, "win-checkbox");
            int buttonWins = winButtonValues.get(winButton);

            checkbox.checked(winsRequired == buttonWins);
        }
    }

    private void updateKeepInventoryButton() {
        CheckboxComponent keepInventoryCheckbox = keepInventoryButton.childById(CheckboxComponent.class, "keep-inventory-checkbox");
        keepInventoryCheckbox.checked(keepInventory);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
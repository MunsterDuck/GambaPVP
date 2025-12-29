package com.munsterduck.gambapvp.gui;

import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.network.BattleRequestPacket;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static com.munsterduck.gambapvp.GambaPVP.MOD_ID;

public class BattleSetupScreen extends BaseUIModelScreen<FlowLayout> {
    private int setupStep = 0;
    private String selectedKit = null;
    private List<String> selectedOpponents = new ArrayList<>();
    private int winsRequired = 3;
    private boolean keepInventory = true;
    private String selectedArena = null;

    // Check if Location Tooltip mod is loaded
    private static final boolean LOCATION_TOOLTIP_LOADED = FabricLoader.getInstance().isModLoaded("locationtooltip");

    // Component references
    private FlowLayout rootComponent;
    private LabelComponent titleLabel;
    private FlowLayout kitSelectionStep;
    private FlowLayout rulesStep;
    private FlowLayout arenaSelectionStep;
    private FlowLayout playerSelectionStep;
    private FlowLayout wagerStep;
    private FlowLayout keepInventoryButton;
    private FlowLayout kitGrid;
    private FlowLayout rulesGrid;
    private FlowLayout arenaGrid;
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

        // Initialize arena selection if mod is loaded
        if (LOCATION_TOOLTIP_LOADED) {
            arenaSelectionStep = rootComponent.childById(FlowLayout.class, "arena-selection-step");
            arenaGrid = rootComponent.childById(FlowLayout.class, "arena-grid");
        }

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
        if (LOCATION_TOOLTIP_LOADED) {
            setupArenaSelection();
        }
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

    private void setupArenaSelection() {
        arenaGrid.clearChildren();
        System.out.println("[GambaPVP] Setting up arena selection...");
        System.out.println("[GambaPVP] Location Tooltip loaded: " + LOCATION_TOOLTIP_LOADED);
        // Request region cache and poll for updates
        requestLocationTooltipArenas();
        // Populate arena list initially
        populateArenaList();
    }

    //Populates the arena grid with arenas from the cache
    private void populateArenaList() {
        arenaGrid.clearChildren();

        // Get arenas directly from Location Tooltip
        List<ArenaInfo> arenas = getArenasFromLocationTooltipCache();

        System.out.println("[GambaPVP] Found " + arenas.size() + " arenas");
        for (int i = 0; i < arenas.size(); i++) {
            ArenaInfo arena = arenas.get(i);
            System.out.println("[GambaPVP] Arena " + i + ": id=" + arena.id + ", name=" + arena.name +
                    ", coords=" + arena.coordinates + ", dim=" + arena.dimension + ", allowPvP=" + arena.allowPvP);
        }

        // Add each arena as a button
        for (ArenaInfo arena : arenas) {
            String path = arena.dimension.getPath();
            Identifier arena_background = switch (path) {
                case "the_nether" -> Identifier.of("minecraft", "block/netherrack");
                case "the_end"    -> Identifier.of("minecraft", "block/end_stone");
                default           -> Identifier.of("minecraft", "block/dirt");
            };

            FlowLayout arenaButton = this.model.expandTemplate(
                FlowLayout.class,
                "arena-button",
                Map.of(
                    "arena-name", arena.name,
                    "arena-coords", arena.coordinates,
                    "arena-background", arena_background.toString()
                )
            );

            // Add tooltip with full details
            List<Text> tooltipLines = new ArrayList<>();
            tooltipLines.add(Text.literal(arena.name).styled(style -> style.withColor(0xFFFFFF)));
            tooltipLines.add(Text.literal(arena.coordinates).styled(style -> style.withColor(0xAAAAAA)));
            tooltipLines.add(Text.literal("Dimension: " + getDimensionName(arena.dimension)).styled(style -> style.withColor(0x888888)));

            arenaButton.tooltip(tooltipLines);

            // Make clickable
            final String arenaId = arena.id;
            arenaButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    selectedArena = arenaId;
                    nextStep();
                    return true;
                }
                return false;
            });

            arenaGrid.child(arenaButton);
        }

        // Add "Any Location" option
        FlowLayout anyLocationButton = this.model.expandTemplate(
            FlowLayout.class,
            "arena-button",
            Map.of(
                "arena-name", "Any Location",
                "arena-coords", "Current Location",
                "arena-background", "minecraft:block/dirt"
            )
        );

        anyLocationButton.tooltip(List.of(
                Text.literal("Any Location").styled(style -> style.withColor(0xFFFFFF)),
                Text.literal("Battle at current location").styled(style -> style.withColor(0xAAAAAA))
        ));

        anyLocationButton.mouseDown().subscribe((mouseX, mouseY, button) -> {
            if (button == 0) {
                selectedArena = null;
                nextStep();
                return true;
            }
            return false;
        });

        arenaGrid.child(anyLocationButton);
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
        if (selectedOpponents == null || selectedOpponents.isEmpty()) {
            System.err.println("No opponent(s) selected!");
            return;
        }

        String kitName = selectedKit != null ? selectedKit : "";
        String arenaId = selectedArena != null ? selectedArena : "";

        BattleRequestPacket.CHANNEL.clientHandle().send(
                new BattleRequestPacket.BattleRequestSend(
                        selectedOpponents,
                        kitName,
                        winsRequired,
                        keepInventory,
                        arenaId
                )
        );
    }

    private void updateStep() {
        FlowLayout contentContainer = rootComponent.childById(FlowLayout.class, "content-container");
        contentContainer.clearChildren();

        // Adjust step numbers based on whether arena selection is included
        FlowLayout current = switch (setupStep) {
            case 0 -> kitSelectionStep;
            case 1 -> {
                updateWinButtons();
                updateKeepInventoryButton();
                yield rulesStep;
            }
            case 2 -> LOCATION_TOOLTIP_LOADED ? arenaSelectionStep : playerSelectionStep;
            case 3 -> LOCATION_TOOLTIP_LOADED ? playerSelectionStep : wagerStep;
            case 4 -> wagerStep;
            default -> kitSelectionStep;
        };

        shouldHideNext();

        contentContainer.child(current);

        // Update title
        String title = switch (setupStep) {
            case 0 -> "Select Your Kit";
            case 1 -> "Win Conditions";
            case 2 -> LOCATION_TOOLTIP_LOADED ? "Select Arena" : "Select Opponent(s)";
            case 3 -> LOCATION_TOOLTIP_LOADED ? "Select Opponent(s)" : "Wager Items";
            case 4 -> "Wager Items";
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
        } else if (getPlayerSelectionStep() == setupStep && selectedOpponents.isEmpty()) {
            editNextButton(false, true);
        } else {
            editNextButton(true, true);
        }
    }

    private int getPlayerSelectionStep() {
        return LOCATION_TOOLTIP_LOADED ? 3 : 2;
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

    // Other Helpers

    private String getDimensionName(Identifier dim) {
        String path = dim.getPath();
        return switch (path) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path.substring(0, 1).toUpperCase() + path.substring(1);
        };
    }

    // Retrieves arena/location data from Location Tooltip mod's AdminClientCache
    private List<ArenaInfo> getArenasFromLocationTooltipCache() {
        List<ArenaInfo> arenas = new ArrayList<>();

        System.out.println("[GambaPVP] Attempting to load arenas from Location Tooltip...");

        try {
            // Access AdminClientCache.get()
            Class<?> cacheClass = Class.forName("com.fugginbeenus.locationtooltip.client.AdminClientCache");

            Method getMethod = cacheClass.getMethod("get");

            Object rowsObj = getMethod.invoke(null);

            if (rowsObj == null) {
                System.out.println("[GambaPVP] AdminClientCache.get() returned null - cache may be empty");
                return arenas;
            }

            if (!rowsObj.getClass().isArray()) {
                System.out.println("[GambaPVP] Result is not an array, type: " + rowsObj.getClass().getName());
                return arenas;
            }

            int length = Array.getLength(rowsObj);

            for (int i = 0; i < length; i++) {
                Object row = Array.get(rowsObj, i);

                try {
                    // AdminClientCache.Row has: id, name, dim, a, b, allowPvP
                    Class<?> rowClass = row.getClass();

                    // List all fields for debugging
                    Field idField = rowClass.getField("id");
                    Field nameField = rowClass.getField("name");
                    Field dimField = rowClass.getField("dim");
                    Field aField = rowClass.getField("a");
                    Field bField = rowClass.getField("b");
                    Field pvpField = rowClass.getField("allowPvP");

                    String id = (String) idField.get(row);
                    String name = (String) nameField.get(row);
                    Identifier dim = (Identifier) dimField.get(row);
                    BlockPos a = (BlockPos) aField.get(row);
                    BlockPos b = (BlockPos) bField.get(row);
                    boolean allowPvP = Boolean.TRUE.equals(pvpField.get(row));

                    // Calculate center position
                    int centerX = (a.getX() + b.getX()) / 2;
                    int centerY = (a.getY() + b.getY()) / 2;
                    int centerZ = (a.getZ() + b.getZ()) / 2;

                    ArenaInfo arenaInfo = new ArenaInfo(id, name, centerX, centerY, centerZ, dim, allowPvP);

                    if (allowPvP) {
                        arenas.add(arenaInfo);
                    }
                } catch (Exception e) {
                    System.err.println("[GambaPVP] Failed to extract location row " + i + " data: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("[GambaPVP] AdminClientCache not found - LocationTooltip may not be loaded properly");
            System.out.println("[GambaPVP] Error: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            System.err.println("[GambaPVP] AdminClientCache.get() method not found: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[GambaPVP] Error accessing LocationTooltip data: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[GambaPVP] Finished loading arenas, total: " + arenas.size());
        return arenas;
    }

    // Requests the arena list from Location Tooltip mod
    private void requestLocationTooltipArenas() {
        try {
            System.out.println("[GambaPVP] Requesting ALL arenas from Location Tooltip...");

            // Register a one-time listener for when the cache is updated
            registerCacheUpdateListener();

            // Call LTPacketsClient.requestAdminList with -1 to request ALL regions
            Class<?> ltPacketsClientClass = Class.forName("com.fugginbeenus.locationtooltip.net.client.LTPacketsClient");
            Method requestMethod = ltPacketsClientClass.getMethod("requestAllAdminList");

            // Use -1 to request all arenas (not just nearby)
            requestMethod.invoke(null);
            System.out.println("[GambaPVP] Successfully sent request for all arenas");

        } catch (ClassNotFoundException e) {
            System.err.println("[GambaPVP] LTPacketsClient class not found: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            System.err.println("[GambaPVP] requestAdminList method not found: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[GambaPVP] Error requesting arena list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Registers a listener to refresh the arena list when the cache is updated. Uses a polling approach since AdminClientCache doesn't have built-in callbacks.
    private void registerCacheUpdateListener() {
        // Create a background task that checks for cache updates
        new Thread(() -> {
            int attempts = 0;
            int maxAttempts = 50; // 5 seconds max wait (50 * 100ms)
            int lastCacheSize = 0;

            try {
                Class<?> cacheClass = Class.forName("com.fugginbeenus.locationtooltip.client.AdminClientCache");
                Method getMethod = cacheClass.getMethod("get");

                while (attempts < maxAttempts) {
                    Thread.sleep(100); // Check every 100ms

                    Object rowsObj = getMethod.invoke(null);
                    int currentSize = (rowsObj != null && rowsObj.getClass().isArray())
                            ? Array.getLength(rowsObj)
                            : 0;

                    // If cache size changed and is non-zero, refresh the UI
                    if (currentSize > 0 && currentSize != lastCacheSize) {
                        System.out.println("[GambaPVP] Cache updated with " + currentSize + " regions, refreshing UI");

                        // Schedule UI update on the client thread
                        MinecraftClient.getInstance().execute(() -> {
                            populateArenaList();
                        });

                        break; // Done
                    }

                    lastCacheSize = currentSize;
                    attempts++;
                }

                if (attempts >= maxAttempts) {
                    System.err.println("[GambaPVP] Timeout waiting for arena cache to update");
                }

            } catch (Exception e) {
                System.err.println("[GambaPVP] Error in cache update listener: " + e.getMessage());
            }
        }, "GambaPVP-ArenaCache-Listener").start();
    }

    // Helper class to store arena information from Location Tooltip
    private static class ArenaInfo {
        final String id;
        final String name;
        final int x, y, z;
        final Identifier dimension;
        final String coordinates;
        final boolean allowPvP;

        ArenaInfo(String id, String name, int x, int y, int z, Identifier dimension, boolean allowPvP) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.coordinates = String.format("(%d, %d, %d)", x, y, z);
            this.allowPvP = allowPvP;
        }
    }
}
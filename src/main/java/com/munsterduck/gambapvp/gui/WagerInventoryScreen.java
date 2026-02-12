package com.munsterduck.gambapvp.gui;

import com.munsterduck.gambapvp.battle.WagerData;
import com.munsterduck.gambapvp.network.BattleRequestPacket;
import com.munsterduck.gambapvp.network.BattleRequestPacketClient;
import com.munsterduck.gambapvp.util.ModTags;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

import static com.munsterduck.gambapvp.GambaPVP.MOD_ID;

/**
 * Client-side wager screen that allows players to select items and currency to wager.
 * Uses direct inventory manipulation without a ScreenHandler for reliable client-side operation.
 */
public class WagerInventoryScreen extends Screen {
    private static final Identifier TEXTURE = new Identifier(MOD_ID, "textures/gui/wager_screen.png");
    private static final Identifier FALLBACK_TEXTURE = new Identifier("minecraft", "textures/gui/container/dispenser.png");
    private static final Identifier SLOT_TEXTURE = new Identifier("minecraft", "textures/gui/container/generic_54.png");

    private static final boolean NOTCH_CURRENCY_LOADED = FabricLoader.getInstance().isModLoaded("notchcurrency");

    private static final int BACKGROUND_WIDTH = 176;
    private static final int BACKGROUND_HEIGHT = 166;

    private final String opponentName;
    private final Consumer<WagerData> onNext;
    private final Runnable onBack;
    private final PlayerInventory playerInventory;

    // 3x3 wager grid (9 slots)
    private final ItemStack[] wagerSlots = new ItemStack[9];

    // Currently held item (for dragging)
    private ItemStack heldStack = ItemStack.EMPTY;

    private TextFieldWidget currencyField;
    private ButtonWidget nextButton;
    private ButtonWidget backButton;

    private int x, y;

    public WagerInventoryScreen(
        String opponentName,
        Consumer<WagerData> onNext,
        Runnable onBack
    ) {
        super(Text.literal("Set Your Wager"));
        this.opponentName = opponentName;
        this.onNext = onNext;
        this.onBack = onBack;
        this.playerInventory = MinecraftClient.getInstance().player.getInventory();

        // Initialize empty wager slots
        for (int i = 0; i < 9; i++) {
            wagerSlots[i] = ItemStack.EMPTY;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - BACKGROUND_WIDTH) / 2;
        this.y = (this.height - BACKGROUND_HEIGHT) / 2;

        // Request balance from server
        if (NOTCH_CURRENCY_LOADED) {
            BattleRequestPacket.CHANNEL.clientHandle().send(new BattleRequestPacket.RequestBalance());
        }

        // Currency input field (only if Notch Currency is loaded)
        if (NOTCH_CURRENCY_LOADED) {
            currencyField = new TextFieldWidget(
                this.textRenderer,
                x + 70,
                y + 70,
                60,
                18,
                Text.literal("Currency Amount")
            );
            currencyField.setMaxLength(10);
            currencyField.setText("0");
            currencyField.setPlaceholder(Text.literal("0"));
            this.addSelectableChild(currencyField);
        }

        // Navigation buttons at the bottom
        int buttonY = y + BACKGROUND_HEIGHT + 5;

        // Back button
        backButton = ButtonWidget.builder(
            Text.literal("Back"),
            button -> handleBack()
        ).dimensions(x + 10, buttonY, 65, 20).build();
        this.addDrawableChild(backButton);

        // Done button
        nextButton = ButtonWidget.builder(
            Text.literal("Done"),
            button -> handleDone()
        ).dimensions(x + BACKGROUND_WIDTH - 75, buttonY, 65, 20).build();
        this.addDrawableChild(nextButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // Draw background texture
        try {
            context.drawTexture(TEXTURE, x, y, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        } catch (Exception e) {
            context.drawTexture(FALLBACK_TEXTURE, x, y, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        }

        // Draw title
        String titleText = "Set Your Wager";
        int titleX = x + (BACKGROUND_WIDTH - this.textRenderer.getWidth(titleText)) / 2;
        context.drawText(this.textRenderer, titleText, titleX, y + 6, 0x404040, false);

        // Draw opponent info
        String vsText = "vs " + opponentName;
        context.drawText(this.textRenderer, vsText, x + BACKGROUND_WIDTH - this.textRenderer.getWidth(vsText) - 8, y + 6, 0x404040, false);

        // Draw 3x3 wager grid
        drawWagerGrid(context, mouseX, mouseY);

        // Draw instructions
        String instructionText = "Click items to add to wager";
        int instructionX = x + (BACKGROUND_WIDTH - this.textRenderer.getWidth(instructionText)) / 2;
        context.drawText(this.textRenderer, instructionText, instructionX, y + 73, 0x555555, false);

        // Draw player inventory
        drawPlayerInventory(context, mouseX, mouseY);

        // Draw currency section if loaded
        if (NOTCH_CURRENCY_LOADED && currencyField != null) {
            int currencyY = y + 70;
            context.drawText(this.textRenderer, "Currency:", x + 10, currencyY + 5, 0x404040, false);
            currencyField.render(context, mouseX, mouseY, delta);

            int balance = getCurrencyBalance();
            String balanceText = "Bal: " + balance;
            context.drawText(this.textRenderer, balanceText, x + 135, currencyY + 5, 0x555555, false);
        }

        // Draw widgets (buttons)
        super.render(context, mouseX, mouseY, delta);

        // Draw held item at cursor
        if (!heldStack.isEmpty()) {
            context.drawItem(heldStack, mouseX - 8, mouseY - 8);
            context.drawItemInSlot(this.textRenderer, heldStack, mouseX - 8, mouseY - 8);
        }

        // Draw tooltips
        drawTooltips(context, mouseX, mouseY);
    }

    private void drawWagerGrid(DrawContext context, int mouseX, int mouseY) {
        int startX = x + 62;
        int startY = y + 18;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = startX + col * 18;
                int slotY = startY + row * 18;
                int index = col + row * 3;

                // Draw slot background (darker for wager slots)
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
                context.fill(slotX + 1, slotY + 1, slotX + 15, slotY + 15, 0xFF373737);

                // Draw item in slot
                ItemStack stack = wagerSlots[index];
                if (!stack.isEmpty()) {
                    context.drawItem(stack, slotX, slotY);
                    context.drawItemInSlot(this.textRenderer, stack, slotX, slotY);
                }

                // Highlight on hover
                if (isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                    context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
                }
            }
        }
    }

    private void drawPlayerInventory(DrawContext context, int mouseX, int mouseY) {
        // Main inventory (3 rows)
        int invStartX = x + 8;
        int invStartY = y + 84;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = invStartX + col * 18;
                int slotY = invStartY + row * 18;
                int index = col + row * 9 + 9; // Inventory slots 9-35

                // Draw slot background
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
                context.fill(slotX + 1, slotY + 1, slotX + 15, slotY + 15, 0xFF373737);

                // Draw item
                ItemStack stack = playerInventory.getStack(index);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, slotX, slotY);
                    context.drawItemInSlot(this.textRenderer, stack, slotX, slotY);

                    // Dim non-gambleable items
                    if (!ModTags.isGamblableCurrency(stack.getItem())) {
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80000000);
                    }
                }

                // Highlight on hover
                if (isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                    context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
                }
            }
        }

        // Hotbar
        int hotbarY = y + 142;
        for (int col = 0; col < 9; col++) {
            int slotX = invStartX + col * 18;
            int slotY = hotbarY;

            // Draw slot background
            context.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
            context.fill(slotX + 1, slotY + 1, slotX + 15, slotY + 15, 0xFF373737);

            // Draw item
            ItemStack stack = playerInventory.getStack(col);
            if (!stack.isEmpty()) {
                context.drawItem(stack, slotX, slotY);
                context.drawItemInSlot(this.textRenderer, stack, slotX, slotY);

                // Dim non-gambleable items
                if (!ModTags.isGamblableCurrency(stack.getItem())) {
                    context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80000000);
                }
            }

            // Highlight on hover
            if (isMouseOverSlot(slotX, slotY, mouseX, mouseY)) {
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
            }
        }
    }

    private void drawTooltips(DrawContext context, int mouseX, int mouseY) {
        // Check wager slots
        int startX = x + 62;
        int startY = y + 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = startX + col * 18;
                int slotY = startY + row * 18;
                int index = col + row * 3;

                if (isMouseOverSlot(slotX, slotY, mouseX, mouseY) && !wagerSlots[index].isEmpty() && heldStack.isEmpty()) {
                    context.drawItemTooltip(this.textRenderer, wagerSlots[index], mouseX, mouseY);
                    return;
                }
            }
        }

        // Check inventory slots
        int invStartX = x + 8;
        int invStartY = y + 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = invStartX + col * 18;
                int slotY = invStartY + row * 18;
                int index = col + row * 9 + 9;

                ItemStack stack = playerInventory.getStack(index);
                if (isMouseOverSlot(slotX, slotY, mouseX, mouseY) && !stack.isEmpty() && heldStack.isEmpty()) {
                    context.drawItemTooltip(this.textRenderer, stack, mouseX, mouseY);
                    return;
                }
            }
        }

        // Check hotbar
        int hotbarY = y + 142;
        for (int col = 0; col < 9; col++) {
            int slotX = invStartX + col * 18;
            int slotY = hotbarY;

            ItemStack stack = playerInventory.getStack(col);
            if (isMouseOverSlot(slotX, slotY, mouseX, mouseY) && !stack.isEmpty() && heldStack.isEmpty()) {
                context.drawItemTooltip(this.textRenderer, stack, mouseX, mouseY);
                return;
            }
        }
    }

    private boolean isMouseOverSlot(int slotX, int slotY, int mouseX, int mouseY) {
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check wager slots
            int startX = x + 62;
            int startY = y + 18;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int slotX = startX + col * 18;
                    int slotY = startY + row * 18;
                    int index = col + row * 3;

                    if (isMouseOverSlot(slotX, slotY, (int) mouseX, (int) mouseY)) {
                        handleWagerSlotClick(index);
                        return true;
                    }
                }
            }

            // Check inventory slots
            int invStartX = x + 8;
            int invStartY = y + 84;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotX = invStartX + col * 18;
                    int slotY = invStartY + row * 18;
                    int invIndex = col + row * 9 + 9;

                    if (isMouseOverSlot(slotX, slotY, (int) mouseX, (int) mouseY)) {
                        handleInventorySlotClick(invIndex);
                        return true;
                    }
                }
            }

            // Check hotbar
            int hotbarY = y + 142;
            for (int col = 0; col < 9; col++) {
                int slotX = invStartX + col * 18;
                int slotY = hotbarY;

                if (isMouseOverSlot(slotX, slotY, (int) mouseX, (int) mouseY)) {
                    handleInventorySlotClick(col);
                    return true;
                }
            }
        }

        // Handle currency field
        if (currencyField != null) {
            currencyField.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleWagerSlotClick(int index) {
        ItemStack slotStack = wagerSlots[index];

        if (heldStack.isEmpty()) {
            // Pick up from wager slot
            if (!slotStack.isEmpty()) {
                heldStack = slotStack.copy();
                wagerSlots[index] = ItemStack.EMPTY;
            }
        } else {
            // Try to place in wager slot
            if (slotStack.isEmpty()) {
                wagerSlots[index] = heldStack.copy();
                heldStack = ItemStack.EMPTY;
            } else if (ItemStack.canCombine(slotStack, heldStack)) {
                // Combine stacks
                int space = slotStack.getMaxCount() - slotStack.getCount();
                int transfer = Math.min(space, heldStack.getCount());
                slotStack.increment(transfer);
                heldStack.decrement(transfer);
                if (heldStack.isEmpty()) {
                    heldStack = ItemStack.EMPTY;
                }
            } else {
                // Swap
                ItemStack temp = slotStack.copy();
                wagerSlots[index] = heldStack.copy();
                heldStack = temp;
            }
        }
    }

    private void handleInventorySlotClick(int invIndex) {
        ItemStack invStack = playerInventory.getStack(invIndex);

        if (heldStack.isEmpty()) {
            // Pick up from inventory (only gambleable items)
            if (!invStack.isEmpty() && ModTags.isGamblableCurrency(invStack.getItem())) {
                heldStack = invStack.copy();
                playerInventory.setStack(invIndex, ItemStack.EMPTY);
            } else if (!invStack.isEmpty()) {
                // Show warning for non-gambleable items
                if (client != null && client.player != null) {
                    client.player.sendMessage(
                        Text.literal("This item cannot be wagered!").styled(style -> style.withColor(0xFF5555)),
                        true
                    );
                }
            }
        } else {
            // Return held item to inventory
            if (invStack.isEmpty()) {
                playerInventory.setStack(invIndex, heldStack.copy());
                heldStack = ItemStack.EMPTY;
            } else if (ItemStack.canCombine(invStack, heldStack)) {
                // Combine stacks
                int space = invStack.getMaxCount() - invStack.getCount();
                int transfer = Math.min(space, heldStack.getCount());
                invStack.increment(transfer);
                heldStack.decrement(transfer);
                if (heldStack.isEmpty()) {
                    heldStack = ItemStack.EMPTY;
                }
            } else {
                // Swap (only if inventory item is gambleable or we're just returning)
                if (ModTags.isGamblableCurrency(invStack.getItem())) {
                    ItemStack temp = invStack.copy();
                    playerInventory.setStack(invIndex, heldStack.copy());
                    heldStack = temp;
                } else {
                    // Can't swap with non-gambleable item, just place held item
                    playerInventory.setStack(invIndex, heldStack.copy());
                    heldStack = ItemStack.EMPTY;
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currencyField != null && currencyField.isFocused()) {
            return currencyField.keyPressed(keyCode, scanCode, modifiers);
        }

        // ESC to go back
        if (keyCode == 256) {
            handleBack();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (currencyField != null && currencyField.isFocused()) {
            // Only allow numbers
            if (Character.isDigit(chr)) {
                return currencyField.charTyped(chr, modifiers);
            }
            return false;
        }
        return super.charTyped(chr, modifiers);
    }

    private void handleBack() {
        // Return all wager items to player inventory
        returnItemsToPlayer();

        if (onBack != null) {
            onBack.run();
        }
    }

    private void handleDone() {
        System.out.println("[GambaPVP] WagerInventoryScreen.handleDone() called");

        if (onNext == null) {
            System.err.println("[GambaPVP] onNext callback is null!");
            return;
        }

        // Return held item first
        if (!heldStack.isEmpty()) {
            playerInventory.offerOrDrop(heldStack);
            heldStack = ItemStack.EMPTY;
        }

        // Collect wager data
        WagerData wager = new WagerData();

        // Add items from wager slots
        for (int i = 0; i < 9; i++) {
            if (!wagerSlots[i].isEmpty()) {
                wager.addItem(wagerSlots[i].copy());
            }
        }

        // Add currency if available
        if (NOTCH_CURRENCY_LOADED && currencyField != null) {
            try {
                int amount = Integer.parseInt(currencyField.getText().trim());
                if (amount > 0) {
                    int balance = getCurrencyBalance();
                    if (amount > balance) {
                        if (client != null && client.player != null) {
                            client.player.sendMessage(
                                Text.literal("Insufficient currency! You have: " + balance).styled(style -> style.withColor(0xFF5555)),
                                false
                                );
                        }
                        return;
                    }
                    wager.setCurrency(amount);
                }
            } catch (NumberFormatException e) {
                wager.setCurrency(0);
            }
        }

        // Clear wager slots (items are now in WagerData)
        for (int i = 0; i < 9; i++) {
            wagerSlots[i] = ItemStack.EMPTY;
        }

        System.out.println("[GambaPVP] Invoking onNext callback with wager: " + wager);
        onNext.accept(wager);
        System.out.println("[GambaPVP] onNext callback completed");
    }

    @Override
    public void close() {
        returnItemsToPlayer();
        super.close();
    }

    private void returnItemsToPlayer() {
        // Return held item
        if (!heldStack.isEmpty()) {
            playerInventory.offerOrDrop(heldStack);
            heldStack = ItemStack.EMPTY;
        }

        // Return items from wager grid
        for (int i = 0; i < 9; i++) {
            if (!wagerSlots[i].isEmpty()) {
                playerInventory.offerOrDrop(wagerSlots[i]);
                wagerSlots[i] = ItemStack.EMPTY;
            }
        }
    }

    private int getCurrencyBalance() {
        if (!NOTCH_CURRENCY_LOADED) {
            return 0;
        }
        return BattleRequestPacketClient.getCachedBalance();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

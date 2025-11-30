package com.munsterduck.gambapvp.gui;

import com.munsterduck.gambapvp.client.ClientKitCache;
import com.munsterduck.gambapvp.network.NetworkHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class KitSelectionScreen extends Screen {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/gui/container/generic_54.png");
    private final Screen parent;
    private final BattleSetupCallback callback;
    private List<ClientKitCache.KitEntry> kits;

    private static final int BACKGROUND_WIDTH = 176;
    private static final int BACKGROUND_HEIGHT = 140; // Reduced height (no player inventory)
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 9;
    private static final int ROWS = 6;

    private int x;
    private int y;

    public interface BattleSetupCallback {
        void onKitSelected(String kitName);
    }

    public KitSelectionScreen(Screen parent, BattleSetupCallback callback) {
        super(Text.literal("Select Your Kit"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - BACKGROUND_WIDTH) / 2;
        this.y = (this.height - BACKGROUND_HEIGHT) / 2;

        // Load kits from cache
        this.kits = ClientKitCache.getKits();

        // If no kits cached, request them
        if (this.kits.isEmpty()) {
            NetworkHandler.requestKits();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // Draw chest background (only top portion, no player inventory)
        context.drawTexture(TEXTURE, x, y, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);

        // Draw title
        context.drawText(this.textRenderer, this.title, x + 8, y + 6, 4210752, false);

        // Draw kit items in slots
        int slotIndex = 0;
        for (ClientKitCache.KitEntry kit : kits) {
            if (slotIndex >= SLOTS_PER_ROW * ROWS) break;

            int row = slotIndex / SLOTS_PER_ROW;
            int col = slotIndex % SLOTS_PER_ROW;

            int slotX = x + 8 + col * SLOT_SIZE;
            int slotY = y + 18 + row * SLOT_SIZE;

            // Draw the kit icon
            context.drawItem(kit.icon, slotX, slotY);

            // Draw hover effect
            if (isMouseOverSlot(mouseX, mouseY, slotX, slotY)) {
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF);
            }

            slotIndex++;
        }

        // Draw "No Kit" option in last slot of bottom row
        int noKitSlot = SLOTS_PER_ROW * ROWS - 1; // Bottom right slot
        int noKitRow = noKitSlot / SLOTS_PER_ROW;
        int noKitCol = noKitSlot % SLOTS_PER_ROW;
        int noKitX = x + 8 + noKitCol * SLOT_SIZE;
        int noKitY = y + 18 + noKitRow * SLOT_SIZE;

        ItemStack barrierStack = new ItemStack(Items.BARRIER);
        context.drawItem(barrierStack, noKitX, noKitY);

        if (isMouseOverSlot(mouseX, mouseY, noKitX, noKitY)) {
            context.fill(noKitX, noKitY, noKitX + 16, noKitY + 16, 0x80FFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);

        // Draw tooltips
        renderTooltips(context, mouseX, mouseY);
    }

    private void renderTooltips(DrawContext context, int mouseX, int mouseY) {
        // Check kit slots for hover
        int slotIndex = 0;
        for (ClientKitCache.KitEntry kit : kits) {
            if (slotIndex >= SLOTS_PER_ROW * ROWS) break;

            int row = slotIndex / SLOTS_PER_ROW;
            int col = slotIndex % SLOTS_PER_ROW;

            int slotX = x + 8 + col * SLOT_SIZE;
            int slotY = y + 18 + row * SLOT_SIZE;

            if (isMouseOverSlot(mouseX, mouseY, slotX, slotY)) {
                context.drawTooltip(this.textRenderer, Text.literal(kit.name), mouseX, mouseY);
                return;
            }

            slotIndex++;
        }

        // Check "No Kit" slot
        int noKitSlot = SLOTS_PER_ROW * ROWS - 1;
        int noKitRow = noKitSlot / SLOTS_PER_ROW;
        int noKitCol = noKitSlot % SLOTS_PER_ROW;
        int noKitX = x + 8 + noKitCol * SLOT_SIZE;
        int noKitY = y + 18 + noKitRow * SLOT_SIZE;

        if (isMouseOverSlot(mouseX, mouseY, noKitX, noKitY)) {
            context.drawTooltip(this.textRenderer, Text.literal("No Kit (Vanilla)"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check kit slots
            int slotIndex = 0;
            for (ClientKitCache.KitEntry kit : kits) {
                if (slotIndex >= SLOTS_PER_ROW * ROWS) break;

                int row = slotIndex / SLOTS_PER_ROW;
                int col = slotIndex % SLOTS_PER_ROW;

                int slotX = x + 8 + col * SLOT_SIZE;
                int slotY = y + 18 + row * SLOT_SIZE;

                if (isMouseOverSlot((int)mouseX, (int)mouseY, slotX, slotY)) {
                    // Send kit selection back to battle setup
                    NetworkHandler.sendKitSelection(kit.name);
                    callback.onKitSelected(kit.name);
                    this.close();
                    return true;
                }

                slotIndex++;
            }

            // Check "No Kit" slot
            int noKitSlot = SLOTS_PER_ROW * ROWS - 1;
            int noKitRow = noKitSlot / SLOTS_PER_ROW;
            int noKitCol = noKitSlot % SLOTS_PER_ROW;
            int noKitX = x + 8 + noKitCol * SLOT_SIZE;
            int noKitY = y + 18 + noKitRow * SLOT_SIZE;

            if (isMouseOverSlot((int)mouseX, (int)mouseY, noKitX, noKitY)) {
                callback.onKitSelected(null);
                this.close();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + 16 &&
                mouseY >= slotY && mouseY < slotY + 16;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Allow ESC to close
        if (keyCode == 256) { // ESC key
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}

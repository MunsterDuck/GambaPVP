package com.munsterduck.gambapvp.gui;

import com.munsterduck.gambapvp.util.ModTags;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.munsterduck.gambapvp.GambaPVP.MOD_ID;

/**
 * Screen handler for the wager inventory screen.
 * Provides a 3x3 grid for gambleable items plus full player inventory access.
 */
public class WagerInventoryScreenHandler extends ScreenHandler {
    public static ScreenHandlerType<WagerInventoryScreenHandler> WAGER_SCREEN_HANDLER_TYPE;

    private final Inventory wagerInventory;
    private final PlayerInventory playerInventory;
    private final String opponentName;

    // Client constructor
    public WagerInventoryScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, new SimpleInventory(9), buf.readString());
    }

    // Server constructor
    public WagerInventoryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory wagerInventory, String opponentName) {
        super(WAGER_SCREEN_HANDLER_TYPE, syncId);
        this.wagerInventory = wagerInventory;
        this.playerInventory = playerInventory;
        this.opponentName = opponentName;

        checkSize(wagerInventory, 9);
        wagerInventory.onOpen(playerInventory.player);

        // Add 3x3 wager slots (slots 0-8)
        // Positioned at top of screen
        int wagerStartX = 62;
        int wagerStartY = 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new WagerSlot(wagerInventory, index, wagerStartX + col * 18, wagerStartY + row * 18));
            }
        }

        // Add player inventory (slots 9-35)
        int playerInvY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }

        // Add player hotbar (slots 36-44)
        int hotbarY = 142;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);

        if (slotObj != null && slotObj.hasStack()) {
            ItemStack originalStack = slotObj.getStack();
            newStack = originalStack.copy();

            if (slot < 9) {
                // Moving from wager slots to player inventory
                if (!this.insertItem(originalStack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to wager slots
                // Only allow gambleable items
                if (ModTags.isGamblableCurrency(originalStack.getItem())) {
                    if (!this.insertItem(originalStack, 0, 9, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slotObj.setStack(ItemStack.EMPTY);
            } else {
                slotObj.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.wagerInventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // Don't return items here - we'll handle that in the screen
    }

    public Inventory getWagerInventory() {
        return wagerInventory;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public static void register() {
        WAGER_SCREEN_HANDLER_TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(MOD_ID, "wager_screen"),
            new ExtendedScreenHandlerType<>((syncId, inventory, buf) ->
                new WagerInventoryScreenHandler(syncId, inventory, buf))
        );
    }

    /**
     * Custom slot that only accepts gambleable items
     */
    private static class WagerSlot extends Slot {
        public WagerSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return ModTags.isGamblableCurrency(stack.getItem());
        }

        @Override
        public int getMaxItemCount() {
            return 64;
        }
    }
}

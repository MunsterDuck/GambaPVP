package com.munsterduck.gambapvp.battle;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.*;

/**
 * Represents a player's wager in a duel.
 * Contains items and optionally currency from Notch Currency mod.
 */
public class WagerData {
    private final List<ItemStack> items;
    private int currency;

    public WagerData() {
        this.items = new ArrayList<>();
        this.currency = 0;
    }

    public WagerData(List<ItemStack> items, int currency) {
        this.items = new ArrayList<>(items);
        this.currency = currency;
    }

    public List<ItemStack> getItems() {
        return new ArrayList<>(items);
    }

    public int getCurrency() {
        return currency;
    }

    public void setCurrency(int currency) {
        this.currency = currency;
    }

    public void addItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            items.add(stack.copy());
        }
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void clear() {
        items.clear();
        currency = 0;
    }

    public boolean isEmpty() {
        return items.isEmpty() && currency == 0;
    }

    /**
     * Serialize to NBT for network transmission or storage
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        // Serialize items
        NbtList itemsList = new NbtList();
        for (ItemStack stack : items) {
            NbtCompound itemNbt = new NbtCompound();
            stack.writeNbt(itemNbt);
            itemsList.add(itemNbt);
        }
        nbt.put("Items", itemsList);

        // Serialize currency
        nbt.putInt("Currency", currency);

        return nbt;
    }

    /**
     * Deserialize from NBT
     */
    public static WagerData fromNbt(NbtCompound nbt) {
        WagerData wager = new WagerData();

        // Deserialize items
        NbtList itemsList = nbt.getList("Items", 10); // 10 = Compound type
        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound itemNbt = itemsList.getCompound(i);
            ItemStack stack = ItemStack.fromNbt(itemNbt);
            if (!stack.isEmpty()) {
                wager.items.add(stack);
            }
        }

        // Deserialize currency
        wager.currency = nbt.getInt("Currency");

        return wager;
    }

    /**
     * Create a copy of this wager data
     */
    public WagerData copy() {
        List<ItemStack> copiedItems = new ArrayList<>();
        for (ItemStack stack : items) {
            copiedItems.add(stack.copy());
        }
        return new WagerData(copiedItems, currency);
    }

    @Override
    public String toString() {
        return "WagerData{items=" + items.size() + ", currency=" + currency + "}";
    }
}

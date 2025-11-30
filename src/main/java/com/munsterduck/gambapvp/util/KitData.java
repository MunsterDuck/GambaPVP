package com.munsterduck.gambapvp.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class KitData {
    private final String name;
    private final NbtCompound inventoryData;
    private final ItemStack iconItem;

    public KitData(String name, NbtCompound inventoryData, ItemStack iconItem) {
        this.name = name;
        this.inventoryData = inventoryData;
        this.iconItem = iconItem;
    }

    public String getName() {
        return name;
    }

    public NbtCompound getInventoryData() {
        return inventoryData;
    }

    public ItemStack getIconItem() {
        return iconItem;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", name);
        nbt.put("inventory", inventoryData);

        NbtCompound iconNbt = new NbtCompound();
        iconItem.writeNbt(iconNbt);
        nbt.put("icon", iconNbt);

        return nbt;
    }

    public static KitData fromNbt(NbtCompound nbt) {
        String name = nbt.getString("name");
        NbtCompound inventory = nbt.getCompound("inventory");
        ItemStack icon = ItemStack.fromNbt(nbt.getCompound("icon"));

        return new KitData(name, inventory, icon);
    }
}

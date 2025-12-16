package com.munsterduck.gambapvp.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

public class KitData {
    private final String name;
    private final NbtCompound inventoryData;
    private final ItemStack icon;

    public KitData(String name, NbtCompound inventoryData, ItemStack icon) {
        this.name = name;
        this.inventoryData = inventoryData;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public NbtCompound getInventoryData() {
        return inventoryData;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", name);
        nbt.put("inventory", inventoryData);

        NbtCompound iconNbt = new NbtCompound();
        icon.writeNbt(iconNbt);
        nbt.put("icon", iconNbt);

        return nbt;
    }

    public static KitData fromNbt(NbtCompound nbt) {
        String name = nbt.getString("name");
        NbtCompound inventory = nbt.getCompound("inventory");
        ItemStack icon = ItemStack.fromNbt(nbt.getCompound("icon"));

        return new KitData(name, inventory, icon);
    }

    public List<ItemStack> getItemsList() {
        List<ItemStack> items = new ArrayList<>();
        NbtCompound invData = getInventoryData();
        NbtList itemsList = invData.getList("Items", 10); // 10 = Compound type

        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound itemTag = itemsList.getCompound(i);
            ItemStack stack = ItemStack.fromNbt(itemTag);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        return items;
    }
}

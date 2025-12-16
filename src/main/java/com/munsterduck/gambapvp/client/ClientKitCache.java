package com.munsterduck.gambapvp.client;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClientKitCache {
    private static final List<KitEntry> cachedKits = new ArrayList<>();

    public static class KitEntry {
        public final String name;
        public final ItemStack icon;
        public final List<ItemStack> items;

        public KitEntry(String name, ItemStack icon, List<ItemStack> items) {
            this.name = name;
            this.icon = icon;
            this.items = items != null ? items : new ArrayList<>();
        }
    }

    public static void clear() {
        cachedKits.clear();
    }

    public static void addKit(String name, ItemStack icon, List<ItemStack> items) {
        cachedKits.add(new KitEntry(name, icon, items));
    }

    public static List<KitEntry> getKits() {
        return new ArrayList<>(cachedKits);
    }

    public static boolean isEmpty() {
        return cachedKits.isEmpty();
    }
}

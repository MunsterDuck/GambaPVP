package com.munsterduck.gambapvp.item;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(GambaPVP.MOD_ID, name), item);
    }

    private static void itemGroupIngredients(FabricItemGroupEntries entries) {
        entries.add(ModBlocks.DUELING_PODIUM);
    }

    public static void registerModItems() {
        GambaPVP.LOGGER.info("Registering Mod Items for  " + GambaPVP.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(ModItems::itemGroupIngredients);
    }
}

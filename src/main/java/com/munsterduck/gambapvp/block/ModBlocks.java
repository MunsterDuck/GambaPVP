package com.munsterduck.gambapvp.block;

import com.munsterduck.gambapvp.GambaPVP;
import com.munsterduck.gambapvp.block.custom.DuelingPodium;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class ModBlocks {

    public static final Block DUELING_PODIUM = registerBlock("dueling_podium",
            new DuelingPodium(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)));

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, new Identifier(GambaPVP.MOD_ID, name),
                new BlockItem(block, new FabricItemSettings()));
    }

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, new Identifier(GambaPVP.MOD_ID, name), block);
    }

    public static void registerModBlocks() {
        GambaPVP.LOGGER.info("Registering ModBlocks for " + GambaPVP.MOD_ID);
    }
}

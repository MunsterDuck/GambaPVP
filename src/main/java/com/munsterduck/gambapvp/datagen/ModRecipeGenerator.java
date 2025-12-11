package com.munsterduck.gambapvp.datagen;

import com.munsterduck.gambapvp.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class ModRecipeGenerator extends FabricRecipeProvider {
    public ModRecipeGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(Consumer<RecipeJsonProvider> consumer) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.DUELING_PODIUM)
                .pattern("LDL")
                .pattern(" L ")
                .pattern("III")

                .input('L', ItemTags.LOGS)
                .input('I', Items.IRON_BLOCK)
                .input('D', Items.DIAMOND_SWORD)

                .criterion("has_logs", conditionsFromTag(ItemTags.LOGS))
                .criterion(hasItem(Items.IRON_BLOCK), conditionsFromItem(Items.IRON_BLOCK))
                .criterion(hasItem(Items.DIAMOND_SWORD), conditionsFromItem(Items.DIAMOND_SWORD))

                //TODO: Never unlocks Recipe
                .offerTo(consumer, new Identifier(getRecipeName(ModBlocks.DUELING_PODIUM)));
    }
}

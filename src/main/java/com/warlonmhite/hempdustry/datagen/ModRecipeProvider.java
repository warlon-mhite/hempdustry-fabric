package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        createStairsRecipe(ModBlocks.HEMP_BRICKS_STAIRS, Ingredient.ofItems(ModBlocks.HEMP_BRICKS_BLOCK)).criterion(hasItem(ModBlocks.HEMP_BRICKS_BLOCK), conditionsFromItem(ModBlocks.HEMP_BRICKS_BLOCK)).offerTo(exporter);
        offerWallRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_BRICKS_WALL, ModBlocks.HEMP_BRICKS_BLOCK);

        createStairsRecipe(ModBlocks.HEMP_PLANKS_STAIRS, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        createDoorRecipe(ModBlocks.HEMP_PLANKS_DOOR, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        createTrapdoorRecipe(ModBlocks.HEMP_PLANKS_TRAPDOOR, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        createFenceRecipe(ModBlocks.HEMP_PLANKS_FENCE, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        createFenceGateRecipe(ModBlocks.HEMP_PLANKS_FENCE_GATE, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        offerPressurePlateRecipe(exporter, ModBlocks.HEMP_PLANKS_PRESSURE_PLATE, ModBlocks.HEMP_PLANKS);
        offerSlabRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_PLANKS_SLAB, ModBlocks.HEMP_PLANKS);
    }

}

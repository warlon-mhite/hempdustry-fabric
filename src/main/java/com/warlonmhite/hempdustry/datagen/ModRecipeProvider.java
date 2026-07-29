package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.block.Blocks;
import net.minecraft.data.server.recipe.ComplexRecipeJsonBuilder;
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        // ---------------------------------------------------------------------
        // Building-block variants (already datagen'd) — stairs/slab/wall/etc.
        // These auto-emit their own ingredient-triggered unlock advancements.
        // ---------------------------------------------------------------------
        createStairsRecipe(ModBlocks.HEMP_BRICKS_STAIRS, Ingredient.ofItems(ModBlocks.HEMP_BRICKS_BLOCK)).criterion(hasItem(ModBlocks.HEMP_BRICKS_BLOCK), conditionsFromItem(ModBlocks.HEMP_BRICKS_BLOCK)).offerTo(exporter);
        offerSlabRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_BRICKS_SLAB, ModBlocks.HEMP_BRICKS_BLOCK);
        offerWallRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_BRICKS_WALL, ModBlocks.HEMP_BRICKS_BLOCK);

        createStairsRecipe(ModBlocks.HEMP_PLANKS_STAIRS, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        offerSlabRecipe(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_PLANKS_SLAB, ModBlocks.HEMP_PLANKS);
        createDoorRecipe(ModBlocks.HEMP_PLANKS_DOOR, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        createTrapdoorRecipe(ModBlocks.HEMP_PLANKS_TRAPDOOR, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        createFenceRecipe(ModBlocks.HEMP_PLANKS_FENCE, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        createFenceGateRecipe(ModBlocks.HEMP_PLANKS_FENCE_GATE, Ingredient.ofItems(ModBlocks.HEMP_PLANKS)).criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS)).offerTo(exporter);
        offerPressurePlateRecipe(exporter, ModBlocks.HEMP_PLANKS_PRESSURE_PLATE, ModBlocks.HEMP_PLANKS);

        // ---------------------------------------------------------------------
        // Core hemp processing chain (migrated from hand-written JSON).
        // Every recipe carries an inventory-changed criterion on its main
        // ingredient, so the recipe book unlocks it the moment the player
        // obtains that ingredient — vanilla "get a log, discover planks" style.
        // ---------------------------------------------------------------------

        // Stem -> fiber, hempcrete, bale
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HEMP_FIBER, 4)
                .input(ModItems.HEMP_STEM)
                .criterion(hasItem(ModItems.HEMP_STEM), conditionsFromItem(ModItems.HEMP_STEM))
                .offerTo(exporter, id("hemp_fiber"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HEMPCRETE, 2)
                .pattern("##")
                .pattern("##")
                .input('#', ModItems.HEMP_STEM)
                .criterion(hasItem(ModItems.HEMP_STEM), conditionsFromItem(ModItems.HEMP_STEM))
                .offerTo(exporter, id("hempcrete"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.HEMP_BALE)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .input('#', ModItems.HEMP_STEM)
                .criterion(hasItem(ModItems.HEMP_STEM), conditionsFromItem(ModItems.HEMP_STEM))
                .offerTo(exporter, id("hemp_bale"));

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HEMP_STEM, 9)
                .input(ModBlocks.HEMP_BALE)
                .criterion(hasItem(ModBlocks.HEMP_BALE), conditionsFromItem(ModBlocks.HEMP_BALE))
                .offerTo(exporter, id("hemp_stem_from_bale"));

        // Hempcrete -> planks, brick, powder block
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.HEMP_PLANKS)
                .pattern("###")
                .pattern("###")
                .input('#', ModItems.HEMPCRETE)
                .criterion(hasItem(ModItems.HEMPCRETE), conditionsFromItem(ModItems.HEMPCRETE))
                .offerTo(exporter, id("hemp_planks"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HEMP_BRICK)
                .pattern("##")
                .pattern("##")
                .input('#', ModItems.HEMPCRETE)
                .criterion(hasItem(ModItems.HEMPCRETE), conditionsFromItem(ModItems.HEMPCRETE))
                .offerTo(exporter, id("hemp_brick"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModBlocks.HEMPCRETE_POWDER_BLOCK, 4)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .input('#', ModItems.HEMPCRETE)
                .criterion(hasItem(ModItems.HEMPCRETE), conditionsFromItem(ModItems.HEMPCRETE))
                .offerTo(exporter, id("hempcrete_powder_block"));

        // Brick -> block of hemp bricks
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_BRICKS_BLOCK)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .input('#', ModItems.HEMP_BRICK)
                .criterion(hasItem(ModItems.HEMP_BRICK), conditionsFromItem(ModItems.HEMP_BRICK))
                .offerTo(exporter, id("hemp_bricks_block"));

        // Planks -> button (redstone)
        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, ModBlocks.HEMP_PLANKS_BUTTON)
                .input(ModBlocks.HEMP_PLANKS)
                .criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS))
                .offerTo(exporter, id("hemp_planks_button"));

        // ---------------------------------------------------------------------
        // Wood set: boats & signs
        // ---------------------------------------------------------------------
        ShapedRecipeJsonBuilder.create(RecipeCategory.TRANSPORTATION, ModItems.HEMP_BOAT)
                .group("boat")
                .pattern("# #")
                .pattern("###")
                .input('#', ModBlocks.HEMP_PLANKS)
                .criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS))
                .offerTo(exporter, id("hemp_boat"));

        ShapelessRecipeJsonBuilder.create(RecipeCategory.TRANSPORTATION, ModItems.HEMP_CHEST_BOAT)
                .group("chest_boat")
                .input(Items.CHEST)
                .input(ModItems.HEMP_BOAT)
                .criterion(hasItem(ModItems.HEMP_BOAT), conditionsFromItem(ModItems.HEMP_BOAT))
                .offerTo(exporter, id("hemp_chest_boat"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, ModItems.HEMP_PLANKS_SIGN, 3)
                .group("wooden_sign")
                .pattern("###")
                .pattern("###")
                .pattern(" X ")
                .input('#', ModBlocks.HEMP_PLANKS)
                .input('X', Items.STICK)
                .criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS))
                .offerTo(exporter, id("hemp_planks_sign"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, ModItems.HEMP_PLANKS_HANGING_SIGN, 6)
                .group("hanging_sign")
                .pattern("X X")
                .pattern("###")
                .pattern("###")
                .input('#', ModBlocks.HEMP_PLANKS)
                .input('X', Items.CHAIN)
                .criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS))
                .offerTo(exporter, id("hemp_planks_hanging_sign"));

        // ---------------------------------------------------------------------
        // Smoking gear
        // ---------------------------------------------------------------------
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.INDICA_SPLIFF)
                .pattern("BB")
                .pattern("PP")
                .input('B', ModItems.INDICA_BUDS)
                .input('P', Items.PAPER)
                .criterion(hasItem(ModItems.INDICA_BUDS), conditionsFromItem(ModItems.INDICA_BUDS))
                .offerTo(exporter, id("indica_spliff"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.SATIVA_SPLIFF)
                .pattern("BB")
                .pattern("PP")
                .input('B', ModItems.SATIVA_BUDS)
                .input('P', Items.PAPER)
                .criterion(hasItem(ModItems.SATIVA_BUDS), conditionsFromItem(ModItems.SATIVA_BUDS))
                .offerTo(exporter, id("sativa_spliff"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.WOODEN_PIPE)
                .pattern("P  ")
                .pattern("SSS")
                .input('P', ItemTags.PLANKS)
                .input('S', Items.STICK)
                .criterion(hasItem(Items.STICK), conditionsFromItem(Items.STICK))
                .offerTo(exporter, id("wooden_pipe"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.BONG)
                .pattern(" PI")
                .pattern("GWG")
                .pattern(" G ")
                .input('P', Blocks.GLASS_PANE)
                .input('I', Items.IRON_NUGGET)
                .input('G', Blocks.GLASS)
                .input('W', Items.WATER_BUCKET)
                .criterion(hasItem(Blocks.GLASS), conditionsFromItem(Blocks.GLASS))
                .offerTo(exporter, id("bong"));

        // Pack an empty pipe/bong with a strain's buds in the crafting grid. Special recipe so the
        // device's durability + enchantments carry over onto the packed result (see PackingRecipe).
        ComplexRecipeJsonBuilder.create(PackingRecipe::new).offerTo(exporter, id("packing"));

        // ---------------------------------------------------------------------
        // Hemp fiber armor set
        // ---------------------------------------------------------------------
        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.HEMP_BEANNIE)
                .pattern("###")
                .pattern("# #")
                .input('#', ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("hemp_beannie"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.HEMP_SHIRT)
                .pattern("# #")
                .pattern("###")
                .pattern("###")
                .input('#', ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("hemp_shirt"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.HEMP_HAREM_PANTS)
                .pattern("###")
                .pattern("# #")
                .pattern("# #")
                .input('#', ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("hemp_harem_pants"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.FLIP_FLOPS)
                .pattern("# #")
                .pattern("# #")
                .input('#', ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("flip_flops"));

        // ---------------------------------------------------------------------
        // Edibles & fiber by-products (hemp as an ingredient for vanilla items)
        // ---------------------------------------------------------------------
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.HEMP_FLOUR)
                .input(ModTags.Items.HEMP_SEEDS)
                .criterion("has_hemp_seeds", conditionsFromTag(ModTags.Items.HEMP_SEEDS))
                .offerTo(exporter, id("hemp_flour"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.FOOD, Items.BREAD)
                .pattern(" W ")
                .pattern("###")
                .input('W', Items.WATER_BUCKET)
                .input('#', ModItems.HEMP_FLOUR)
                .criterion(hasItem(ModItems.HEMP_FLOUR), conditionsFromItem(ModItems.HEMP_FLOUR))
                .offerTo(exporter, id("bread"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.FOOD, Items.COOKIE, 4)
                .pattern("#C#")
                .input('#', ModItems.HEMP_FLOUR)
                .input('C', Items.COCOA_BEANS)
                .criterion(hasItem(ModItems.HEMP_FLOUR), conditionsFromItem(ModItems.HEMP_FLOUR))
                .offerTo(exporter, id("cookie"));

        CookingRecipeJsonBuilder.createSmelting(Ingredient.ofItems(ModItems.HEMP_STEM), RecipeCategory.MISC, Items.GREEN_DYE, 1.0F, 200)
                .group("dye")
                .criterion(hasItem(ModItems.HEMP_STEM), conditionsFromItem(ModItems.HEMP_STEM))
                .offerTo(exporter, id("green_dye"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.GREEN_WOOL)
                .pattern("##")
                .pattern("##")
                .input('#', ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("green_wool"));

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Items.STRING)
                .input(ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("string"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.PAPER, 2)
                .pattern("###")
                .input('#', ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("paper"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, Items.FLOWER_POT)
                .pattern("# #")
                .pattern(" # ")
                .input('#', ModItems.HEMP_BRICK)
                .criterion(hasItem(ModItems.HEMP_BRICK), conditionsFromItem(ModItems.HEMP_BRICK))
                .offerTo(exporter, id("flower_pot"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.STICK)
                .pattern("#")
                .pattern("#")
                .input('#', ModItems.HEMP_STEM)
                .criterion(hasItem(ModItems.HEMP_STEM), conditionsFromItem(ModItems.HEMP_STEM))
                .offerTo(exporter, id("stick_from_stem"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.STICK, 6)
                .pattern("#")
                .pattern("#")
                .input('#', ModBlocks.HEMP_PLANKS)
                .criterion(hasItem(ModBlocks.HEMP_PLANKS), conditionsFromItem(ModBlocks.HEMP_PLANKS))
                .offerTo(exporter, id("stick_from_planks"));

        // ---------------------------------------------------------------------
        // Hempcrete-dyed concrete powders (8 hempcrete + 1 dye -> 4 powder).
        // Blue/brown dye assignments are corrected here (the hand-written JSONs
        // had them swapped).
        // ---------------------------------------------------------------------
        offerConcretePowder(exporter, Items.WHITE_DYE, Items.WHITE_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.ORANGE_DYE, Items.ORANGE_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.MAGENTA_DYE, Items.MAGENTA_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.LIGHT_BLUE_DYE, Items.LIGHT_BLUE_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.YELLOW_DYE, Items.YELLOW_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.LIME_DYE, Items.LIME_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.PINK_DYE, Items.PINK_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.GRAY_DYE, Items.GRAY_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.LIGHT_GRAY_DYE, Items.LIGHT_GRAY_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.CYAN_DYE, Items.CYAN_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.PURPLE_DYE, Items.PURPLE_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.BLUE_DYE, Items.BLUE_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.BROWN_DYE, Items.BROWN_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.GREEN_DYE, Items.GREEN_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.RED_DYE, Items.RED_CONCRETE_POWDER);
        offerConcretePowder(exporter, Items.BLACK_DYE, Items.BLACK_CONCRETE_POWDER);
    }

    private void offerConcretePowder(RecipeExporter exporter, ItemConvertible dye, ItemConvertible result) {
        ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, result, 4)
                .input(ModItems.HEMPCRETE, 8)
                .input(dye)
                .group("hempdustry_concrete_powder")
                .criterion(hasItem(ModItems.HEMPCRETE), conditionsFromItem(ModItems.HEMPCRETE))
                .offerTo(exporter, id(Registries.ITEM.getId(result.asItem()).getPath()));
    }

    private static Identifier id(String path) {
        return Identifier.of(Hempdustry.MOD_ID, path);
    }
}

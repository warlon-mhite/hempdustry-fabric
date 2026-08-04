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

        // Fibre -> canvas, woven the same 2x2 way vanilla weaves string into wool, and costing the
        // same, because they are both just cloth. What canvas buys over wool is standing in for
        // leather.
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HEMP_CANVAS)
                .pattern("##")
                .pattern("##")
                .input('#', ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("hemp_canvas"));

        // Canvas -> hemp wool. The block is the *bale*, not a lighter fabric: four sheets of cloth
        // stacked into a cubic metre of it, which is vanilla's 4-items-into-a-block grammar (clay
        // balls, snowballs, quartz, prismarine shards). Two out rather than vanilla's strict one,
        // matching this mod's own hempcrete recipe, so a building block lands at 8 fibre = 2 stems
        // instead of 16 fibre = 4 — a cloth block nobody can afford to build with isn't a block.
        ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_WOOL, 2)
                .pattern("##")
                .pattern("##")
                .input('#', ModItems.HEMP_CANVAS)
                .criterion(hasItem(ModItems.HEMP_CANVAS), conditionsFromItem(ModItems.HEMP_CANVAS))
                .offerTo(exporter, id("hemp_wool"));

        // Vanilla's carpet recipe with our wool in it — two wide, three out, exactly the ratio every
        // vanilla carpet uses. Keyed on the hemp_wool item rather than a tag, so sheep wool can't
        // produce hemp carpet.
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, ModBlocks.HEMP_CARPET, 3)
                .pattern("##")
                .input('#', ModBlocks.HEMP_WOOL)
                .criterion(hasItem(ModBlocks.HEMP_WOOL), conditionsFromItem(ModBlocks.HEMP_WOOL))
                .offerTo(exporter, id("hemp_carpet"));

        // The two reversals, each going back exactly one step to the thing it was made from —
        // vanilla's only un-craft recipe, 1 wool -> 4 string, works the same way, and nothing in
        // vanilla un-crafts *through* an intermediate. A wool -> 8 fibre shortcut was considered
        // and rejected: it would make canvas harder to reach from wool than fibre is, which is
        // backwards for the item sitting between them.
        //
        // Both are lossless. Un-weaving isn't a real process, but the recycling is: before wood
        // pulp took over in the 1800s, European paper was made from recycled linen and hemp rags,
        // and the rag trade existed precisely to feed worn cloth back into fibre stock.
        //
        // NOTE these are the only shapeless single-ingredient recipes hemp wool and canvas may ever
        // have. A second one on either item collides with these and silently never fires.
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HEMP_CANVAS, 2)
                .input(ModBlocks.HEMP_WOOL)
                .criterion(hasItem(ModBlocks.HEMP_WOOL), conditionsFromItem(ModBlocks.HEMP_WOOL))
                .offerTo(exporter, id("hemp_canvas_from_hemp_wool"));

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.HEMP_FIBER, 4)
                .input(ModItems.HEMP_CANVAS)
                .criterion(hasItem(ModItems.HEMP_CANVAS), conditionsFromItem(ModItems.HEMP_CANVAS))
                .offerTo(exporter, id("hemp_fiber_from_hemp_canvas"));

        // Vanilla's painting is 8 sticks around #minecraft:wool, so hemp wool is what belongs in
        // the middle — not canvas, which took vanilla's *leather* slot in the item frame below.
        // Those two vanilla recipes are the same 3x3 ring and differ only by the centre item, so
        // one hemp cloth item per slot is what keeps them from colliding (CLAUDE.md §5 #17).
        //
        // An explicit recipe rather than a tag join, since hemp wool deliberately stays out of
        // #minecraft:wool — and note that putting it in that tag would make this recipe and
        // vanilla's own painting recipe collide.
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, Items.PAINTING)
                .pattern("###")
                .pattern("#X#")
                .pattern("###")
                .input('#', Items.STICK)
                .input('X', ModBlocks.HEMP_WOOL)
                .criterion(hasItem(ModBlocks.HEMP_WOOL), conditionsFromItem(ModBlocks.HEMP_WOOL))
                .offerTo(exporter, id("painting"));

        // Vanilla's item frame, with the leather swapped for canvas — same eight sticks, same
        // pattern. A second route rather than a replacement: the cow one still works.
        //
        // Canvas takes the leather slot and only the leather slot. Vanilla's painting is the same
        // 3x3 ring with wool in the middle instead, so canvas standing in for both would produce
        // two identical recipes and only one of them could ever fire (it did, and it did — see
        // CLAUDE.md §5 #17). The painting is hemp wool's, since wool is what vanilla puts there.
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, Items.ITEM_FRAME)
                .pattern("###")
                .pattern("#X#")
                .pattern("###")
                .input('#', Items.STICK)
                .input('X', ModItems.HEMP_CANVAS)
                .criterion(hasItem(ModItems.HEMP_CANVAS), conditionsFromItem(ModItems.HEMP_CANVAS))
                .offerTo(exporter, id("item_frame"));

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

        // Blocks of hemp bricks + a copper ingot -> Decarboxylator.
        //
        // Deliberately the *block*, not the hemp_brick item: seven of them is 63 bricks, 252
        // hempcrete, 504 hemp stem — hundreds of mature plants. That is the whole point. Edibles
        // are meant to be a late goal, not something a player reaches off their first crop the way
        // a spliff is, and the cost has to be paid in the crop itself rather than in some unrelated
        // ore. See CLAUDE.md's Decarboxylator note for the full chain.
        //
        // The copper ingot sits top-centre, the same grid position as the copper flue on the
        // finished block's model. Keeping a hempdustry-exclusive ingredient in the pattern is also
        // what stops this ever ambiguously matching another mod's machine recipe in a
        // kitchen-sink pack — no other mod can reference hemp bricks.
        //
        // The unlock criterion is the hemp bricks block alone, NOT the copper: copper is early-game
        // and unlocking on it would show the recipe in the book long before the player could
        // plausibly build it, which would undercut the gating this recipe exists to create.
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, ModBlocks.DECARBOXYLATOR)
                .pattern("BCB")
                .pattern("B B")
                .pattern("BBB")
                .input('B', ModBlocks.HEMP_BRICKS_BLOCK)
                .input('C', Items.COPPER_INGOT)
                .criterion(hasItem(ModBlocks.HEMP_BRICKS_BLOCK), conditionsFromItem(ModBlocks.HEMP_BRICKS_BLOCK))
                .offerTo(exporter, id("decarboxylator"));

        // Hempcrete + a cauldron -> Infuser. Same grammar as the Decarboxylator (a vanilla utility
        // block wrapped in a ring of hempdustry material) but built on a Cauldron instead of hemp
        // bricks, which gives the second machine its own visual family and echoes the cauldron rim
        // on its model.
        //
        // Deliberately much cheaper than the Decarboxylator — 36 hemp stem against 504. The gate for
        // this whole pipeline is paid at the Decarboxylator; charging heavily twice for one chain
        // would be punitive, and vanilla doesn't escalate every step either (a blast furnace costs
        // far less than the gear you already had by the time you build one).
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, ModBlocks.INFUSER)
                .pattern("HHH")
                .pattern("HCH")
                .pattern("HHH")
                .input('H', ModBlocks.HEMPCRETE_BLOCK)
                .input('C', Items.CAULDRON)
                .criterion(hasItem(ModBlocks.HEMPCRETE_BLOCK), conditionsFromItem(ModBlocks.HEMPCRETE_BLOCK))
                .offerTo(exporter, id("infuser"));

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

        // No 4 fibre -> green wool recipe. It collided byte for byte with the canvas recipe above,
        // and only one of two identical crafting recipes can ever fire. The 2x2 square of a fibre
        // is canvas's, matching vanilla's 4 string -> wool; green wool is still reachable the way
        // vanilla builds every dyed wool — fibre -> string -> white wool, dyed with the green dye
        // this mod already smelts out of hemp stem.

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Items.STRING)
                .input(ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("string"));

        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, Items.PAPER, 2)
                .pattern("###")
                .input('#', ModItems.HEMP_FIBER)
                .criterion(hasItem(ModItems.HEMP_FIBER), conditionsFromItem(ModItems.HEMP_FIBER))
                .offerTo(exporter, id("paper"));

        // Vanilla's bed, with the sheep's wool swapped for hemp. A second route rather than a
        // replacement — every coloured bed recipe is keyed on its own specific wool item, so this
        // adds one and takes nothing away. White because white wool is vanilla's *undyed* wool and
        // hemp cloth is undyed by definition; a true hemp-coloured bed would need its own block,
        // and that is a much larger job than a recipe (CLAUDE.md §5b D9).
        ShapedRecipeJsonBuilder.create(RecipeCategory.DECORATIONS, Items.WHITE_BED)
                .pattern("###")
                .pattern("XXX")
                .input('#', ModBlocks.HEMP_WOOL)
                .input('X', ItemTags.PLANKS)
                .criterion(hasItem(ModBlocks.HEMP_WOOL), conditionsFromItem(ModBlocks.HEMP_WOOL))
                .offerTo(exporter, id("white_bed"));

        // Shapeless, matching vanilla's own book recipe. The leather in a book is the *cover*, and
        // cloth-bound hardbacks are entirely ordinary — so canvas reads right there. Combined with
        // the hemp paper above it makes the book hemp all the way through, which is a better joke
        // than it is a stretch: the Gutenberg Bible was printed on hemp paper in 1456, hemp supplied
        // over 70% of paper before 1883, and it is still what bible paper and cigarette paper are.
        //
        // This is the canvas recipe with real reach, and it was a deliberate call rather than a
        // freebie — see CLAUDE.md. Books gate enchanting through bookshelves, so this takes cows off
        // the critical path for an enchanting setup and cascades to lecterns and chiseled bookshelves.
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, Items.BOOK)
                .input(Items.PAPER)
                .input(Items.PAPER)
                .input(Items.PAPER)
                .input(ModItems.HEMP_CANVAS)
                .criterion(hasItem(ModItems.HEMP_CANVAS), conditionsFromItem(ModItems.HEMP_CANVAS))
                .offerTo(exporter, id("book"));

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

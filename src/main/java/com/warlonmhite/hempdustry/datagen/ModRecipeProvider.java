package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.Strain;
import net.minecraft.advancement.AdvancementRequirements;
import net.minecraft.advancement.AdvancementRewards;
import net.minecraft.advancement.criterion.RecipeUnlockedCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import java.util.List;
import java.util.Map;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.recipe.ContainerCarriedRecipe;
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
        // Wholesome hemp-seed food. No THC in any of this — hemp seed is a food
        // crop in its own right and the mod had nothing to say about it.
        // ---------------------------------------------------------------------

        // Cook the raw thing: vanilla's most-used food verb, and hemp seed had no cooked form.
        // Keyed on the tag so every strain's seed works and a future one needs no new recipe.
        offerFoodCooking(exporter, "toasted_hemp_seeds", Ingredient.fromTag(ModTags.Items.HEMP_SEEDS),
                ModItems.TOASTED_HEMP_SEEDS, ModItems.INDICA_SEEDS, 0.1F);

        // Seeds bound with honey. The bottle comes back on its own.
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.HEMP_FLAPJACK, 2)
                .input(ModItems.TOASTED_HEMP_SEEDS)
                .input(ModItems.TOASTED_HEMP_SEEDS)
                .input(Items.HONEY_BOTTLE)
                .criterion(hasItem(ModItems.TOASTED_HEMP_SEEDS), conditionsFromItem(ModItems.TOASTED_HEMP_SEEDS))
                .offerTo(exporter, id("hemp_flapjack"));

        // Hemp seed milk: boil the seed until the husks crack, press the white pulp through a sieve,
        // throw the husks away. The boiling and straining are abstracted into the craft the way
        // vanilla abstracts baking into a grid.
        //
        // ContainerCarried, not shapeless: WATER_BUCKET carries recipeRemainder(BUCKET), so a plain
        // recipe would hand an empty bucket back *and* leave one inside the hemp milk. Same
        // duplication bhang had. The bucket carries through instead.
        offerContainerCarried(exporter, id("hemp_milk_bucket"), ModItems.HEMP_MILK_BUCKET,
                ModItems.INDICA_SEEDS,
                List.of(Ingredient.fromTag(ModTags.Items.HEMP_SEEDS),
                        Ingredient.fromTag(ModTags.Items.HEMP_SEEDS),
                        Ingredient.fromTag(ModTags.Items.HEMP_SEEDS),
                        Ingredient.ofItems(Items.WATER_BUCKET)));

        // Siemieniotka — the Silesian hemp-seed soup eaten at Wigilia, also called konopionka or
        // siemieniec. Hemp milk thickened with flour and sweetened; it is a *sweet* soup, and the
        // sweetener really is sugar (Polish Wikipedia and every Polish recipe source say cukier;
        // English Wikipedia's "honey" is not corroborated anywhere else, so honey went to the
        // flapjack instead). Groats are the traditional accompaniment rather than an ingredient,
        // which is why there is no wheat in here.
        //
        // Simplified in two places, both deliberate: the real thing adds dairy milk on top of the
        // hemp milk, and finishes with a butter roux. Two milk buckets in one recipe is fussy and
        // would put a cow back in the way of a plant-based dish, and the mod's only butter is
        // cannabutter, which would make a wholesome soup psychoactive.
        //
        // One bowl per craft, because every vanilla stew is maxCount 1 and a recipe cannot emit a
        // stack bigger than its item allows. That makes it bowl + ingredients -> one stew, which is
        // exactly vanilla's stew grammar anyway.
        //
        // A whole bucket of hemp milk per bowl looks extravagant until you notice the bucket comes
        // straight back — HEMP_MILK_BUCKET carries recipeRemainder(BUCKET) — so the real cost is the
        // three hemp seeds that went into it. Safe alongside the bucket it returns when drunk,
        // because only one of those two things can ever happen to a given bucket.
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.SIEMIENIOTKA)
                .input(Items.BOWL)
                .input(ModItems.HEMP_MILK_BUCKET)
                .input(ModItems.HEMP_FLOUR)
                .input(Items.SUGAR)
                .criterion(hasItem(ModItems.HEMP_MILK_BUCKET), conditionsFromItem(ModItems.HEMP_MILK_BUCKET))
                .offerTo(exporter, id("siemieniotka"));

        // ---------------------------------------------------------------------
        // Edibles — cannabutter's first real use.
        //
        // Every one of these is a *fat* recipe: cannabinoids are fat-soluble, which is the whole
        // reason cannabutter exists as a step rather than stirring buds into a bowl. The ladder is
        // dilution — one butter spread across eight cookies, four brownies, seven cake slices, or
        // concentrated into a single dawamesk.
        // ---------------------------------------------------------------------

        // Butter on bread. The cheapest and fastest edible there is -- no hemp flour, no cocoa, no
        // sugar -- and the only one that isn't baked, which is why it keeps a descriptive name
        // rather than joining the Space family.
        //
        // Three to a loaf is not free food: a loaf is 5 nutrition and 6.0 saturation, three slices
        // are 6 and 3.6. Nutrition up, saturation down, and what you actually bought was three
        // doses out of one butter instead of one.
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.CANNABUTTER_TOAST, 3)
                .input(Items.BREAD)
                .input(ModItems.CANNABUTTER)
                .criterion(hasItem(ModItems.CANNABUTTER), conditionsFromItem(ModItems.CANNABUTTER))
                .offerTo(exporter, id("cannabutter_toast"));

        // Vanilla's cookie is wheat-cocoa-wheat for 8. Same row with hemp flour, plus the butter
        // underneath: the cheapest way into edibles and the most dilute.
        ShapedRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.SPACE_COOKIE, 8)
                .pattern("FCF")
                .pattern(" B ")
                .input('F', ModItems.HEMP_FLOUR)
                .input('C', Items.COCOA_BEANS)
                .input('B', ModItems.CANNABUTTER)
                .criterion(hasItem(ModItems.CANNABUTTER), conditionsFromItem(ModItems.CANNABUTTER))
                .offerTo(exporter, id("space_cookie"));

        // A brownie is flour, cocoa, sugar and fat — cocoa-forward where the cookie is flour-
        // forward, which is what the pattern says. Four to a batch, so twice a cookie's dose.
        ShapedRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.SPACE_BROWNIE, 4)
                .pattern("CFC")
                .pattern("SBS")
                .input('C', Items.COCOA_BEANS)
                .input('F', ModItems.HEMP_FLOUR)
                .input('S', Items.SUGAR)
                .input('B', ModItems.CANNABUTTER)
                .criterion(hasItem(ModItems.CANNABUTTER), conditionsFromItem(ModItems.CANNABUTTER))
                .offerTo(exporter, id("space_brownie"));

        // Vanilla's cake layout exactly — milk on top, sugar flanking, grain underneath — with the
        // egg swapped for cannabutter and the wheat for hemp flour. Butter and egg are both the
        // binder-and-fat slot in a real batter, so the swap is the honest one to make, and it is the
        // only one that fits: the grid has no ninth square to add an ingredient to.
        ShapedRecipeJsonBuilder.create(RecipeCategory.FOOD, ModBlocks.SPACE_CAKE)
                .pattern("MMM")
                .pattern("SBS")
                .pattern("FFF")
                .input('M', Items.MILK_BUCKET)
                .input('S', Items.SUGAR)
                .input('B', ModItems.CANNABUTTER)
                .input('F', ModItems.HEMP_FLOUR)
                .criterion(hasItem(ModItems.CANNABUTTER), conditionsFromItem(ModItems.CANNABUTTER))
                .offerTo(exporter, id("space_cake"));

        // Bhang. The drink, and the only edible that skips cannabutter -- the plant goes straight
        // into the milk and the milk's own fat does the extraction, which is how bhang is actually
        // made. Thousands of years of continuous use in India, drunk at Holi and Maha Shivaratri,
        // still sold in licensed government bhang shops; the least slangy thing in this list.
        //
        // Unwashed hemp on purpose: washing exists for the Infuser's Quality axis and this recipe
        // has no quality axis to spend it on, so each intermediate keeps a distinct destination.
        // TWO hemp for one bucket, and the doubling is the accurate part rather than a tax: milk is
        // ~3.5% fat against butter's ~80%, and you compensate for a weak solvent by putting more
        // plant in, not by accepting less out. It also stops bhang undercutting the butter chain on
        // raw hemp per serving.
        //
        // NOT a plain shapeless recipe: MILK_BUCKET carries recipeRemainder(BUCKET), so an ordinary
        // recipe hands the empty bucket straight back -- and since the bhang is itself in a bucket
        // that gives you back on drinking, that is one bucket in and two out. Free iron, repeatable.
        // ContainerCarriedRecipe suppresses the remainder so the bucket carries through: the one you
        // poured the milk from is the one the bhang is in, and it returns exactly once, when drunk.
        offerContainerCarried(exporter, id("bhang_bucket"), ModItems.BHANG_BUCKET,
                ModItems.DECARBOXYLATED_HEMP,
                List.of(Ingredient.ofItems(ModItems.DECARBOXYLATED_HEMP),
                        Ingredient.ofItems(ModItems.DECARBOXYLATED_HEMP),
                        Ingredient.ofItems(Items.MILK_BUCKET),
                        Ingredient.ofItems(Items.SUGAR)));

        // Dawamesk. The one edible here with a documented history rather than a folk name: the
        // Algerian confection the Club des Hashischins ate at the Hotel de Lauzun in the 1840s,
        // served to Gautier, Baudelaire, Dumas and Nerval by Moreau de Tours, who was studying it.
        // The real thing is a paste of sugar, orange, cinnamon, cloves, nutmeg, pistachio and
        // almond around the fat -- none of which vanilla has, so sweet berries stand in for the
        // fruit and honey for the spiced syrup. Shapeless, because it is stirred, not baked.
        ShapelessRecipeJsonBuilder.create(RecipeCategory.FOOD, ModItems.DAWAMESK)
                .input(ModItems.CANNABUTTER)
                .input(Items.SUGAR)
                .input(Items.HONEY_BOTTLE)
                .input(Items.SWEET_BERRIES)
                .criterion(hasItem(ModItems.CANNABUTTER), conditionsFromItem(ModItems.CANNABUTTER))
                .offerTo(exporter, id("dawamesk"));

        // ---------------------------------------------------------------------
        // Smoking gear
        // ---------------------------------------------------------------------
        // Rolling a spliff: N buds over N paper gives a level-N joint, one recipe per strain per
        // dose. Unlike packing a device, these can be plain shaped recipes — a spliff has no
        // durability or enchantments to carry across — so they stay recipe-book and JEI visible.
        //
        // They cannot collide with each other because the ingredient *counts* differ, which is the
        // same disambiguation vanilla relies on and the rule that caught green wool vs. canvas.
        // Paper scaling with dose is deliberate: it is the second resource that stops a level-III
        // spliff undercutting a bong, which gets four hits out of the same three buds.
        for (Strain strain : Strain.ACTIVE) {
            for (int dose = 1; dose <= ModItems.SPLIFF_MAX_DOSE; dose++) {
                offerSpliff(exporter, strain, dose);
            }
        }

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

    /**
     * One spliff recipe: {@code dose} buds stacked over {@code dose} paper, producing a spliff whose
     * {@code smoke_contents} component carries the strain and the dose.
     *
     * <p>Built by hand rather than through {@link ShapedRecipeJsonBuilder} because that builder's
     * result is a bare {@code new ItemStack(item, count)} with no way to attach components. Handing
     * a fully-built {@link ShapedRecipe} to the exporter is the supported route, and it still emits
     * the usual unlock advancement so the recipe book discovers it on picking up the buds.
     */
    private static void offerSpliff(RecipeExporter exporter, Strain strain, int dose) {
        ItemStack result = new ItemStack(ModItems.SPLIFF);
        result.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, dose));

        String buds = String.valueOf('B').repeat(dose);
        String paper = String.valueOf('P').repeat(dose);
        RawShapedRecipe raw = RawShapedRecipe.create(
                Map.of('B', Ingredient.ofItems(strain.buds()), 'P', Ingredient.ofItems(Items.PAPER)),
                buds, paper);

        Identifier recipeId = id("spliff_" + strain.id() + "_" + dose);
        ShapedRecipe recipe = new ShapedRecipe("spliff", CraftingRecipeCategory.MISC, raw, result);
        exporter.accept(recipeId, recipe, exporter.getAdvancementBuilder()
                .criterion("has_the_recipe", RecipeUnlockedCriterion.create(recipeId))
                .criterion(hasItem(strain.buds()), conditionsFromItem(strain.buds()))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(recipeId.withPrefixedPath("recipes/misc/")));
    }


    /**
     * A shapeless recipe whose ingredients' containers stay in the result — see
     * {@link ContainerCarriedRecipe}. Built by hand because {@link ShapelessRecipeJsonBuilder} can
     * only emit vanilla's serializer, and still emits the usual unlock advancement so the recipe
     * book discovers it normally.
     */
    private static void offerContainerCarried(RecipeExporter exporter, Identifier recipeId,
                                              ItemConvertible output, ItemConvertible unlockedBy,
                                              List<Ingredient> inputs) {
        ContainerCarriedRecipe recipe = new ContainerCarriedRecipe("", CraftingRecipeCategory.MISC,
                new ItemStack(output), inputs);
        exporter.accept(recipeId, recipe, exporter.getAdvancementBuilder()
                .criterion("has_the_recipe", RecipeUnlockedCriterion.create(recipeId))
                .criterion(hasItem(unlockedBy), conditionsFromItem(unlockedBy))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .criteriaMerger(AdvancementRequirements.CriterionMerger.OR)
                .build(recipeId.withPrefixedPath("recipes/food/")));
    }


    /**
     * Furnace, smoker and campfire, the way every vanilla food is cookable in all three. Written out
     * rather than using {@code offerFoodCookingRecipe} because that helper only takes a single item
     * as input and this one is keyed on the hemp-seed tag, so every strain works and a future strain
     * needs no new recipe.
     */
    private static void offerFoodCooking(RecipeExporter exporter, String name, Ingredient input,
                                         ItemConvertible output, ItemConvertible unlockedBy, float experience) {
        CookingRecipeJsonBuilder.createSmelting(input, RecipeCategory.FOOD, output, experience, 200)
                .criterion(hasItem(unlockedBy), conditionsFromItem(unlockedBy))
                .offerTo(exporter, id(name));
        CookingRecipeJsonBuilder.createSmoking(input, RecipeCategory.FOOD, output, experience, 100)
                .criterion(hasItem(unlockedBy), conditionsFromItem(unlockedBy))
                .offerTo(exporter, id(name + "_from_smoking"));
        CookingRecipeJsonBuilder.createCampfireCooking(input, RecipeCategory.FOOD, output, experience, 600)
                .criterion(hasItem(unlockedBy), conditionsFromItem(unlockedBy))
                .offerTo(exporter, id(name + "_from_campfire_cooking"));
    }

}

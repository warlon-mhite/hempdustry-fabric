package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.List;
import java.util.Map;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.recipe.ContainerCarriedRecipe;
import com.warlonmhite.hempdustry.recipe.DecarboxylatingRecipe;
import com.warlonmhite.hempdustry.recipe.InfusingRecipe;
import com.warlonmhite.hempdustry.recipe.InfusedShapedRecipe;
import com.warlonmhite.hempdustry.recipe.InfusedShapelessRecipe;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.recipe.PressingRecipe;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {
    /**
     * Fibre from one <b>retted</b> stalk, against a dry one's 4. The gap is the whole point: doing it
     * the real way — soak, then break and scutch — pays about 50% better, and the price is the extra
     * step rather than a scarce resource, because the real cost of retting is hand work and time, not
     * water. Tunable; see the note at the recipe.
     */
    public static final int FIBER_PER_RETTED_STEM = 6;

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    /**
     * Since 1.21.4 the recipes live on a {@link RecipeProvider} rather than on the provider, and the
     * generator is handed the registry lookup up front — which is what the spliff recipes need, since
     * they bake a {@code smoke_contents} component into their result and a strain reference is a
     * {@link Holder} only a lookup can produce ({@link Strain#ENTRY_CODEC} refuses inline
     * entries).
     */
    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput exporter) {
        return new Generator(registries, exporter);
    }

    @Override
    public String getName() {
        return "Recipes";
    }

    private static class Generator extends RecipeProvider {
        Generator(HolderLookup.Provider registries, RecipeOutput exporter) {
            super(registries, exporter);
        }

    @Override
    public void buildRecipes() {
        // ---------------------------------------------------------------------
        // Building-block variants (already datagen'd) — stairs/slab/wall/etc.
        // These auto-emit their own ingredient-triggered unlock advancements.
        // ---------------------------------------------------------------------
        stairBuilder(ModBlocks.HEMP_BRICKS_STAIRS, Ingredient.of(ModBlocks.HEMP_BRICKS_BLOCK)).unlockedBy(getHasName(ModBlocks.HEMP_BRICKS_BLOCK), has(ModBlocks.HEMP_BRICKS_BLOCK)).save(output);
        slab(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_BRICKS_SLAB, ModBlocks.HEMP_BRICKS_BLOCK);
        wall(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_BRICKS_WALL, ModBlocks.HEMP_BRICKS_BLOCK);

        stairBuilder(ModBlocks.HEMP_PLANKS_STAIRS, Ingredient.of(ModBlocks.HEMP_PLANKS)).unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS)).save(output);
        slab(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_PLANKS_SLAB, ModBlocks.HEMP_PLANKS);
        doorBuilder(ModBlocks.HEMP_PLANKS_DOOR, Ingredient.of(ModBlocks.HEMP_PLANKS)).unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS)).save(output);
        trapdoorBuilder(ModBlocks.HEMP_PLANKS_TRAPDOOR, Ingredient.of(ModBlocks.HEMP_PLANKS)).unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS)).save(output);
        fenceBuilder(ModBlocks.HEMP_PLANKS_FENCE, Ingredient.of(ModBlocks.HEMP_PLANKS)).unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS)).save(output);
        fenceGateBuilder(ModBlocks.HEMP_PLANKS_FENCE_GATE, Ingredient.of(ModBlocks.HEMP_PLANKS)).unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS)).save(output);
        pressurePlate(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE, ModBlocks.HEMP_PLANKS);

        // Parquet is vanilla's mosaic recipe — two slabs stacked — and its stairs and slab are cut
        // from it the way bamboo mosaic's are.
        mosaicBuilder(RecipeCategory.DECORATIONS, ModBlocks.HEMP_PARQUET, ModBlocks.HEMP_PLANKS_SLAB);
        stairBuilder(ModBlocks.HEMP_PARQUET_STAIRS, Ingredient.of(ModBlocks.HEMP_PARQUET)).unlockedBy(getHasName(ModBlocks.HEMP_PARQUET), has(ModBlocks.HEMP_PARQUET)).save(output);
        slab(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_PARQUET_SLAB, ModBlocks.HEMP_PARQUET);

        // ---------------------------------------------------------------------
        // Core hemp processing chain (migrated from hand-written JSON).
        // Every recipe carries an inventory-changed criterion on its main
        // ingredient, so the recipe book unlocks it the moment the player
        // obtains that ingredient — vanilla "get a log, discover planks" style.
        // ---------------------------------------------------------------------

        // Stem -> fiber, hempcrete, bale
        //
        // Two routes to fibre, and they are a sidegrade rather than a ladder. Shredding a DRY stalk
        // and keeping what comes loose gives 4 — real, and exactly as wasteful as it sounds. Retting
        // the stalk first (a cauldron, see ModCauldronBehaviors) and then breaking and scutching it
        // gives 6. The extra step is what the extra 50% is paid with; before the retted stalk existed
        // the cauldron handed back finished fibre and was strictly better for the price of one click.
        // Vanilla's stonecutter is the same shape: better ratios, in exchange for needing a block.
        //
        // If the dry route ever looks pointless, lower FIBER_PER_RETTED_STEM rather than raising the
        // water cost — water is free either way, so the water is not what is being balanced.
        shapeless(RecipeCategory.MISC, ModItems.HEMP_FIBER, 4)
                .requires(ModItems.HEMP_STEM)
                .unlockedBy(getHasName(ModItems.HEMP_STEM), has(ModItems.HEMP_STEM))
                .save(output, id("hemp_fiber"));

        shapeless(RecipeCategory.MISC, ModItems.HEMP_FIBER, FIBER_PER_RETTED_STEM)
                .requires(ModItems.RETTED_HEMP_STEM)
                .unlockedBy(getHasName(ModItems.RETTED_HEMP_STEM), has(ModItems.RETTED_HEMP_STEM))
                .save(output, id("hemp_fiber_from_retted_hemp_stem"));

        // Fibre -> canvas, woven the same 2x2 way vanilla weaves string into wool, and costing the
        // same, because they are both just cloth. What canvas buys over wool is standing in for
        // leather.
        shaped(RecipeCategory.MISC, ModItems.HEMP_CANVAS)
                .pattern("##")
                .pattern("##")
                .define('#', ModItems.HEMP_FIBER)
                .unlockedBy(getHasName(ModItems.HEMP_FIBER), has(ModItems.HEMP_FIBER))
                .save(output, id("hemp_canvas"));

        // Canvas -> hemp wool. The block is the *bale*, not a lighter fabric: four sheets of cloth
        // stacked into a cubic metre of it, which is vanilla's 4-items-into-a-block grammar (clay
        // balls, snowballs, quartz, prismarine shards). Two out rather than vanilla's strict one,
        // matching this mod's own hempcrete recipe, so a building block lands at 8 fibre = 2 stems
        // instead of 16 fibre = 4 — a cloth block nobody can afford to build with isn't a block.
        shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_WOOL, 2)
                .pattern("##")
                .pattern("##")
                .define('#', ModItems.HEMP_CANVAS)
                .unlockedBy(getHasName(ModItems.HEMP_CANVAS), has(ModItems.HEMP_CANVAS))
                .save(output, id("hemp_wool"));

        // Vanilla's carpet recipe with our wool in it — two wide, three out, exactly the ratio every
        // vanilla carpet uses. Keyed on the hemp_wool item rather than a tag, so sheep wool can't
        // produce hemp carpet.
        shaped(RecipeCategory.DECORATIONS, ModBlocks.HEMP_CARPET, 3)
                .pattern("##")
                .define('#', ModBlocks.HEMP_WOOL)
                .unlockedBy(getHasName(ModBlocks.HEMP_WOOL), has(ModBlocks.HEMP_WOOL))
                .save(output, id("hemp_carpet"));

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
        shapeless(RecipeCategory.MISC, ModItems.HEMP_CANVAS, 2)
                .requires(ModBlocks.HEMP_WOOL)
                .unlockedBy(getHasName(ModBlocks.HEMP_WOOL), has(ModBlocks.HEMP_WOOL))
                .save(output, id("hemp_canvas_from_hemp_wool"));

        shapeless(RecipeCategory.MISC, ModItems.HEMP_FIBER, 4)
                .requires(ModItems.HEMP_CANVAS)
                .unlockedBy(getHasName(ModItems.HEMP_CANVAS), has(ModItems.HEMP_CANVAS))
                .save(output, id("hemp_fiber_from_hemp_canvas"));

        // Vanilla's painting is 8 sticks around #minecraft:wool, so hemp wool is what belongs in
        // the middle — not canvas, which took vanilla's *leather* slot in the item frame below.
        // Those two vanilla recipes are the same 3x3 ring and differ only by the centre item, so
        // one hemp cloth item per slot is what keeps them from colliding (CLAUDE.md §5 #17).
        //
        // An explicit recipe rather than a tag join, since hemp wool deliberately stays out of
        // #minecraft:wool — and note that putting it in that tag would make this recipe and
        // vanilla's own painting recipe collide.
        shaped(RecipeCategory.DECORATIONS, Items.PAINTING)
                .pattern("###")
                .pattern("#X#")
                .pattern("###")
                .define('#', Items.STICK)
                .define('X', ModBlocks.HEMP_WOOL)
                .unlockedBy(getHasName(ModBlocks.HEMP_WOOL), has(ModBlocks.HEMP_WOOL))
                .save(output, id("painting"));

        // Vanilla's item frame, with the leather swapped for canvas — same eight sticks, same
        // pattern. A second route rather than a replacement: the cow one still works.
        //
        // Canvas takes the leather slot and only the leather slot. Vanilla's painting is the same
        // 3x3 ring with wool in the middle instead, so canvas standing in for both would produce
        // two identical recipes and only one of them could ever fire (it did, and it did — see
        // CLAUDE.md §5 #17). The painting is hemp wool's, since wool is what vanilla puts there.
        shaped(RecipeCategory.DECORATIONS, Items.ITEM_FRAME)
                .pattern("###")
                .pattern("#X#")
                .pattern("###")
                .define('#', Items.STICK)
                .define('X', ModItems.HEMP_CANVAS)
                .unlockedBy(getHasName(ModItems.HEMP_CANVAS), has(ModItems.HEMP_CANVAS))
                .save(output, id("item_frame"));

        shaped(RecipeCategory.MISC, ModItems.HEMPCRETE, 2)
                .pattern("##")
                .pattern("##")
                .define('#', ModItems.HEMP_STEM)
                .unlockedBy(getHasName(ModItems.HEMP_STEM), has(ModItems.HEMP_STEM))
                .save(output, id("hempcrete"));

        shaped(RecipeCategory.MISC, ModBlocks.HEMP_BALE)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.HEMP_STEM)
                .unlockedBy(getHasName(ModItems.HEMP_STEM), has(ModItems.HEMP_STEM))
                .save(output, id("hemp_bale"));

        shapeless(RecipeCategory.MISC, ModItems.HEMP_STEM, 9)
                .requires(ModBlocks.HEMP_BALE)
                .unlockedBy(getHasName(ModBlocks.HEMP_BALE), has(ModBlocks.HEMP_BALE))
                .save(output, id("hemp_stem_from_bale"));

        shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_LEAVES)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.HEMP_LEAF)
                .unlockedBy(getHasName(ModItems.HEMP_LEAF), has(ModItems.HEMP_LEAF))
                .save(output, id("hemp_leaves"));

        shapeless(RecipeCategory.MISC, ModItems.HEMP_LEAF, 9)
                .requires(ModBlocks.HEMP_LEAVES)
                .unlockedBy(getHasName(ModBlocks.HEMP_LEAVES), has(ModBlocks.HEMP_LEAVES))
                .save(output, id("hemp_leaf_from_leaves"));

        // Hempcrete -> planks, brick, powder block
        shaped(RecipeCategory.MISC, ModBlocks.HEMP_PLANKS)
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.HEMPCRETE)
                .unlockedBy(getHasName(ModItems.HEMPCRETE), has(ModItems.HEMPCRETE))
                .save(output, id("hemp_planks"));

        shaped(RecipeCategory.MISC, ModItems.HEMP_BRICK)
                .pattern("##")
                .pattern("##")
                .define('#', ModItems.HEMPCRETE)
                .unlockedBy(getHasName(ModItems.HEMPCRETE), has(ModItems.HEMPCRETE))
                .save(output, id("hemp_brick"));

        shaped(RecipeCategory.MISC, ModBlocks.HEMPCRETE_POWDER_BLOCK, 4)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.HEMPCRETE)
                .unlockedBy(getHasName(ModItems.HEMPCRETE), has(ModItems.HEMPCRETE))
                .save(output, id("hempcrete_powder_block"));

        // Brick -> block of hemp bricks
        shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.HEMP_BRICKS_BLOCK)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.HEMP_BRICK)
                .unlockedBy(getHasName(ModItems.HEMP_BRICK), has(ModItems.HEMP_BRICK))
                .save(output, id("hemp_bricks_block"));

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
        shaped(RecipeCategory.DECORATIONS, ModBlocks.DECARBOXYLATOR)
                .pattern("BCB")
                .pattern("B B")
                .pattern("BBB")
                .define('B', ModBlocks.HEMP_BRICKS_BLOCK)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy(getHasName(ModBlocks.HEMP_BRICKS_BLOCK), has(ModBlocks.HEMP_BRICKS_BLOCK))
                .save(output, id("decarboxylator"));

        // Hempcrete + a cauldron -> Infuser. Same grammar as the Decarboxylator (a vanilla utility
        // block wrapped in a ring of hempdustry material) but built on a Cauldron instead of hemp
        // bricks, which gives the second machine its own visual family and echoes the cauldron rim
        // on its model.
        //
        // Deliberately much cheaper than the Decarboxylator — 36 hemp stem against 504. The gate for
        // this whole pipeline is paid at the Decarboxylator; charging heavily twice for one chain
        // would be punitive, and vanilla doesn't escalate every step either (a blast furnace costs
        // far less than the gear you already had by the time you build one).
        shaped(RecipeCategory.DECORATIONS, ModBlocks.INFUSER)
                .pattern("HHH")
                .pattern("HCH")
                .pattern("HHH")
                .define('H', ModBlocks.HEMPCRETE_BLOCK)
                .define('C', Items.CAULDRON)
                .unlockedBy(getHasName(ModBlocks.HEMPCRETE_BLOCK), has(ModBlocks.HEMPCRETE_BLOCK))
                .save(output, id("infuser"));

        // Planks -> button (redstone)
        shapeless(RecipeCategory.REDSTONE, ModBlocks.HEMP_PLANKS_BUTTON)
                .requires(ModBlocks.HEMP_PLANKS)
                .unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS))
                .save(output, id("hemp_planks_button"));

        // ---------------------------------------------------------------------
        // Wood set: boats & signs
        // ---------------------------------------------------------------------
        shaped(RecipeCategory.TRANSPORTATION, ModItems.HEMP_BOAT)
                .group("boat")
                .pattern("# #")
                .pattern("###")
                .define('#', ModBlocks.HEMP_PLANKS)
                .unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS))
                .save(output, id("hemp_boat"));

        shapeless(RecipeCategory.TRANSPORTATION, ModItems.HEMP_CHEST_BOAT)
                .group("chest_boat")
                .requires(Items.CHEST)
                .requires(ModItems.HEMP_BOAT)
                .unlockedBy(getHasName(ModItems.HEMP_BOAT), has(ModItems.HEMP_BOAT))
                .save(output, id("hemp_chest_boat"));

        shaped(RecipeCategory.DECORATIONS, ModItems.HEMP_PLANKS_SIGN, 3)
                .group("wooden_sign")
                .pattern("###")
                .pattern("###")
                .pattern(" X ")
                .define('#', ModBlocks.HEMP_PLANKS)
                .define('X', Items.STICK)
                .unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS))
                .save(output, id("hemp_planks_sign"));

        shaped(RecipeCategory.DECORATIONS, ModItems.HEMP_PLANKS_HANGING_SIGN, 6)
                .group("hanging_sign")
                .pattern("X X")
                .pattern("###")
                .pattern("###")
                .define('#', ModBlocks.HEMP_PLANKS)
                .define('X', Items.IRON_CHAIN)
                .unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS))
                .save(output, id("hemp_planks_hanging_sign"));

        // Vanilla's shelf is six stripped logs. Hemp has no log, so the planks take their place,
        // exactly as they do in the hanging sign above.
        shelf(ModBlocks.HEMP_PLANKS_SHELF, ModBlocks.HEMP_PLANKS);

        // ---------------------------------------------------------------------
        // Wholesome hemp-seed food. No THC in any of this — hemp seed is a food
        // crop in its own right and the mod had nothing to say about it.
        // ---------------------------------------------------------------------

        // Cook the raw thing: vanilla's most-used food verb, and hemp seed had no cooked form.
        // Keyed on the tag so every strain's seed works and a future one needs no new recipe --
        // the unlock has to be tag-based too, or a Lemon Haze-only player never discovers this.
        offerFoodCooking("toasted_hemp_seeds", ofTag(ModTags.Items.HEMP_SEEDS),
                ModItems.TOASTED_HEMP_SEEDS, ModTags.Items.HEMP_SEEDS, 0.1F);

        // Seeds bound with honey. The bottle comes back on its own.
        shapeless(RecipeCategory.FOOD, ModItems.HEMP_FLAPJACK, 2)
                .requires(ModItems.TOASTED_HEMP_SEEDS)
                .requires(ModItems.TOASTED_HEMP_SEEDS)
                .requires(Items.HONEY_BOTTLE)
                .unlockedBy(getHasName(ModItems.TOASTED_HEMP_SEEDS), has(ModItems.TOASTED_HEMP_SEEDS))
                .save(output, id("hemp_flapjack"));

        // Hemp seed milk: boil the seed until the husks crack, press the white pulp through a sieve,
        // throw the husks away. The boiling and straining are abstracted into the craft the way
        // vanilla abstracts baking into a grid.
        //
        // ContainerCarried, not shapeless: WATER_BUCKET carries craftRemainder(BUCKET), so a plain
        // recipe would hand an empty bucket back *and* leave one inside the hemp milk. Same
        // duplication bhang had. The bucket carries through instead.
        offerContainerCarried(id("hemp_milk_bucket"), new ItemStack(ModItems.HEMP_MILK_BUCKET),
                ModTags.Items.HEMP_SEEDS,
                List.of(ofTag(ModTags.Items.HEMP_SEEDS),
                        ofTag(ModTags.Items.HEMP_SEEDS),
                        ofTag(ModTags.Items.HEMP_SEEDS),
                        Ingredient.of(Items.WATER_BUCKET)));

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
        // straight back — HEMP_MILK_BUCKET carries craftRemainder(BUCKET) — so the real cost is the
        // three hemp seeds that went into it. Safe alongside the bucket it returns when drunk,
        // because only one of those two things can ever happen to a given bucket.
        shapeless(RecipeCategory.FOOD, ModItems.SIEMIENIOTKA)
                .requires(Items.BOWL)
                .requires(ModItems.HEMP_MILK_BUCKET)
                .requires(ModItems.HEMP_FLOUR)
                .requires(Items.SUGAR)
                .unlockedBy(getHasName(ModItems.HEMP_MILK_BUCKET), has(ModItems.HEMP_MILK_BUCKET))
                .save(output, id("siemieniotka"));

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
        offerInfusedShapeless(id("cannabutter_toast"), ModItems.CANNABUTTER_TOAST, 3,
                List.of(Ingredient.of(Items.BREAD), Ingredient.of(ModItems.CANNABUTTER)));

        // Vanilla's cookie is wheat-cocoa-wheat for 8. Same row with hemp flour, plus the butter
        // underneath: the cheapest way into edibles and the most dilute.
        offerInfusedShaped(id("space_cookie"), ModItems.SPACE_COOKIE, 8,
                Map.of('F', Ingredient.of(ModItems.HEMP_FLOUR),
                       'C', Ingredient.of(Items.COCOA_BEANS),
                       'B', Ingredient.of(ModItems.CANNABUTTER)),
                "FCF", " B ");

        // A brownie is flour, cocoa, sugar and fat — cocoa-forward where the cookie is flour-
        // forward, which is what the pattern says. Four to a batch, so twice a cookie's dose.
        offerInfusedShaped(id("space_brownie"), ModItems.SPACE_BROWNIE, 4,
                Map.of('C', Ingredient.of(Items.COCOA_BEANS),
                       'F', Ingredient.of(ModItems.HEMP_FLOUR),
                       'S', Ingredient.of(Items.SUGAR),
                       'B', Ingredient.of(ModItems.CANNABUTTER)),
                "CFC", "SBS");

        // Vanilla's cake layout, with the butter taking the middle of the *milk* row:
        //
        //     M B M        vanilla:  A A A     A milk   B sugar
        //     S E S                  B E B     C wheat  E egg
        //     F F F                  C C C
        //
        // The egg stays where vanilla put it. An earlier version spent the egg on the butter, which
        // made this the only cake in the game with no egg in it — and it was the wrong slot to take:
        // egg is the *binder*, milk and butter are both the dairy-fat slot, so swapping butter for a
        // measure of the milk is substituting like for like. It is also the truer recipe. A real
        // cannabutter cake is an ordinary cake batter with the butter swapped, not one with the egg
        // pulled out.
        //
        // Net cost change: one milk out, one egg in. A chicken instead of a third cow-trip, which is
        // what vanilla's cake asks for anyway. Milk is the tag, so hemp milk works here as it does
        // in bhang.
        offerInfusedShaped(id("space_cake"), ModBlocks.SPACE_CAKE, 1,
                Map.of('M', ofTag(ModTags.Items.MILK_BUCKETS),
                       'S', Ingredient.of(Items.SUGAR),
                       'E', Ingredient.of(Items.EGG),
                       'B', Ingredient.of(ModItems.CANNABUTTER),
                       'F', Ingredient.of(ModItems.HEMP_FLOUR)),
                "MBM", "SES", "FFF");

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
        // NOT a plain shapeless recipe: MILK_BUCKET carries craftRemainder(BUCKET), so an ordinary
        // recipe hands the empty bucket straight back -- and since the bhang is itself in a bucket
        // that gives you back on drinking, that is one bucket in and two out. Free iron, repeatable.
        // ContainerCarriedRecipe suppresses the remainder so the bucket carries through: the one you
        // poured the milk from is the one the bhang is in, and it returns exactly once, when drunk.
        // Fixed at the floor of both axes rather than inherited: bhang never touches cannabutter, so
        // there is nothing to transfer. Tier I, Rough -- the crudest preparation in the set.
        offerContainerCarried(id("bhang_bucket"), dosed(ModItems.BHANG_BUCKET, 1, Quality.ROUGH),
                ModItems.DECARBOXYLATED_HEMP,
                List.of(Ingredient.of(ModItems.DECARBOXYLATED_HEMP),
                        Ingredient.of(ModItems.DECARBOXYLATED_HEMP),
                        ofTag(ModTags.Items.MILK_BUCKETS),
                        Ingredient.of(Items.SUGAR)));

        // Dawamesk. The one edible here with a documented history rather than a folk name: the
        // Algerian confection the Club des Hashischins ate at the Hotel de Lauzun in the 1840s,
        // served to Gautier, Baudelaire, Dumas and Nerval by Moreau de Tours, who was studying it.
        // The real thing is a paste of sugar, orange, cinnamon, cloves, nutmeg, pistachio and
        // almond around the fat -- none of which vanilla has, so sweet berries stand in for the
        // fruit and honey for the spiced syrup. Shapeless, because it is stirred, not baked.
        offerInfusedShapeless(id("dawamesk"), ModItems.DAWAMESK, 1,
                List.of(Ingredient.of(ModItems.CANNABUTTER), Ingredient.of(Items.SUGAR),
                        Ingredient.of(Items.HONEY_BOTTLE), Ingredient.of(Items.SWEET_BERRIES)));

        // ---------------------------------------------------------------------
        // The two machines' own conversions
        // ---------------------------------------------------------------------
        HolderLookup.RegistryLookup<Strain> strains = Strain.registry(this.registries);
        // These are recipes rather than Java because that is what lets a third-party strain reach
        // the oven, a pack rebalance the chain with KubeJS, and a recipe viewer show either machine
        // at all. Neither goes in the recipe book — it only knows the crafting types — so neither
        // carries an unlock advancement: there would be nothing for it to unlock.
        //
        // Buds come from the built-in strains rather than being named, exactly as the spliff recipes
        // do, so strain #3 is decarboxylatable the moment it exists.
        //
        // Plant strains only: a hash entry's "buds" are resin, and the oven takes plant matter,
        // never resin -- hash is a smoking material and nothing else.
        for (ResourceKey<Strain> key : ModStrains.BUILT_IN) {
            Strain strain = strains.getOrThrow(key).value();
            if (strain.wildFlower().isEmpty()) {
                continue;
            }
            offerDecarboxylating(Ingredient.of(strain.buds()),
                    new ItemStack(ModItems.DECARBOXYLATED_HEMP, DecarboxylatorBlockEntity.BUDS_OUTPUT),
                    BuiltInRegistries.ITEM.getKey(strain.buds()).getPath());
        }
        // Fan leaf: bulk trim, worth a quarter of a bud, and strain-agnostic like everything else
        // downstream of the oven.
        offerDecarboxylating(Ingredient.of(ModItems.HEMP_LEAF),
                new ItemStack(ModItems.DECARBOXYLATED_HEMP, DecarboxylatorBlockEntity.LEAF_OUTPUT),
                "hemp_leaf");
        // Schwag: a bud, at half a bud. The oven is the careful heat, so it never makes scorched
        // hemp out of anything -- that is the furnace's product, and schwag burns to it there like
        // any other bud.
        offerDecarboxylating(Ingredient.of(ModItems.SCHWAG),
                new ItemStack(ModItems.DECARBOXYLATED_HEMP, DecarboxylatorBlockEntity.SCHWAG_OUTPUT),
                "schwag");

        // The furnace's answer to the same heat: ANY PLANT PART, ONE SCORCHED HEMP, ALWAYS ONE.
        //
        // A furnace is not an oven. Cooking hemp that hard drives the THC off and oxidises what is
        // left, so what comes out is worth a quarter of the Decarboxylator's product in the Infuser
        // and drags the grade down (InfuserBlockEntity.SCORCHED_PER_STRENGTH, Quality). That is the
        // point of it: low-grade butter before the 504-stem oven, without the oven ever losing its
        // reason to exist. A bud gives four decarboxylated hemp in there and one scorched hemp here,
        // so the always-one is what keeps the gate honest -- and a bud is still worth more in a pipe
        // than in this furnace, which is the joke a player learns once.
        //
        // Plant matter only, from the same flower filter as the oven's recipes above: hash never
        // smelts, because the family never reaches butter. Furnace only, no smoker or campfire --
        // vanilla keeps those two for food, and the mod's own toasted seeds follow that rule.
        // Vanilla's numbers for the sea pickle and the chorus fruit: 200 ticks, 0.1 experience.
        //
        // One recipe over every input rather than one per input, which is the charcoal shape. The
        // unlock is any of them: a recipe's unlock advancement ORs its criteria.
        List<ItemLike> scorchable = new java.util.ArrayList<>();
        scorchable.add(ModItems.HEMP_LEAF);
        scorchable.add(ModItems.SCHWAG);
        for (ResourceKey<Strain> key : ModStrains.BUILT_IN) {
            Strain strain = strains.getOrThrow(key).value();
            if (strain.wildFlower().isPresent()) {
                scorchable.add(strain.buds());
            }
        }
        SimpleCookingRecipeBuilder scorching = SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(scorchable.toArray(ItemLike[]::new)),
                RecipeCategory.MISC, ModItems.SCORCHED_HEMP, 0.1F, 200);
        for (ItemLike input : scorchable) {
            scorching.unlockedBy(getHasName(input), has(input));
        }
        scorching.save(output, id("scorched_hemp"));

        // NOTHING IN THE HASH FAMILY DECARBOXYLATES, and that is the rule rather than an omission:
        // the oven takes plant matter, the bowl takes resin. Hash is a smoking material and only a
        // smoking material -- it never becomes cannabutter, an edible or anything you swallow.
        //
        // It also settles a question the old hashish -> 32 recipe kept asking badly. A concentrate
        // must not conjure potency, so the oven number had to be exactly what went in; but no
        // multiple of nine hits the bar's whole-batch figure, so the loose piece was always going to
        // be worth a rounded-down pinch and the player was always going to wonder why their
        // concentrate baked to less than its parts. Removing the route removes the question.
        //
        // The fan leaf keeps both of its sinks and they are now a genuine choice rather than a
        // ladder: ~47 leaves is either 47 decarboxylated hemp for the edible chain, or nine
        // smokeable pieces. Different currencies, neither strictly better.

        // Packing nine pieces back into a bar -- vanilla's storage-block grammar, and the reason the
        // fifth cut yields one piece rather than two. A storage block you can only ever destroy is
        // the one storage-block shape vanilla never uses.
        //
        // THERE IS DELIBERATELY NO SHAPELESS UNPACK. Every vanilla storage block has one; this one
        // unpacks by being cut, because the cut is the mechanic. A crafting-grid unpack would make
        // the block, the blade and the durability cost all optional at once.
        shaped(RecipeCategory.MISC, ModBlocks.HASHISH_BAR)
                .pattern("HHH")
                .pattern("HHH")
                .pattern("HHH")
                .define('H', ModItems.HASHISH)
                .unlockedBy(getHasName(ModItems.HASHISH), has(ModItems.HASHISH))
                .save(output, id("hashish_bar"));

        // Nine charas rolled into a ball. UNLIKE THE TWO BARS THIS UNPACKS IN THE GRID TOO, and the
        // asymmetry is the point: a bar is cut with a blade because pressing is what a bar is, and
        // charas is never pressed -- you roll it between your palms and you pull it apart the same
        // way. It is also the one member rare enough that a blade toll on top would sting.
        shaped(RecipeCategory.MISC, ModBlocks.CHARAS_BALL)
                .pattern("CCC")
                .pattern("CCC")
                .pattern("CCC")
                .define('C', ModItems.CHARAS)
                .unlockedBy(getHasName(ModItems.CHARAS), has(ModItems.CHARAS))
                .save(output, id("charas_ball"));

        shapeless(RecipeCategory.MISC, ModItems.CHARAS, 9)
                .requires(ModBlocks.CHARAS_BALL)
                .unlockedBy(getHasName(ModBlocks.CHARAS_BALL), has(ModBlocks.CHARAS_BALL))
                .save(output, id("charas_from_charas_ball"));

        // The blonde bar packs the same way. Two 3x3 squares of a single material would collide if
        // they were the same material -- they are not, and that is the whole rule (materials.md).
        shaped(RecipeCategory.MISC, ModBlocks.FILTERED_HASHISH_BAR)
                .pattern("HHH")
                .pattern("HHH")
                .pattern("HHH")
                .define('H', ModItems.FILTERED_HASHISH)
                .unlockedBy(getHasName(ModItems.FILTERED_HASHISH), has(ModItems.FILTERED_HASHISH))
                .save(output, id("filtered_hashish_bar"));

        // The Sifting Box: vanilla's composter with its two holes filled in. The composter is seven
        // wooden slabs in a U; a sift box is that same slatted box with a screen in it, so filling
        // the U's empty cells with the screen is a deliberate echo rather than a near miss -- and it
        // only lands because these are slabs, not planks.
        //
        // The stack is physically right too. The iron bars sit ABOVE the canvas: a coarse grate you
        // beat the material against, and the fine woven screen underneath that the trichome heads
        // fall through. That is the actual arrangement in a sift box, and it is legible from the
        // recipe alone.
        //
        // One iron bars is a real but small gate, and it is earned -- this is the block that turns
        // waste into a smokeable. The cloth chain (stems -> retting -> fibre -> canvas) was already
        // the main cost; the iron is a nudge, not a wall.
        //
        // The multiset is unique: vanilla's composter is 7 slabs in a different shape with two empty
        // cells, so nothing collides. Note recipe_collisions.py cannot see tags-as-atoms and the
        // composter takes #minecraft:wooden_slabs, which hemp_planks_slab belongs to -- so the
        // scanner reads those two as distinct where a player would not. The extra ingredients are
        // what actually keeps them apart.
        shaped(RecipeCategory.DECORATIONS, ModBlocks.SIFTING_BOX)
                .pattern("PBP")
                .pattern("PCP")
                .pattern("PPP")
                .define('P', ModBlocks.HEMP_PLANKS_SLAB)
                .define('B', Items.IRON_BARS)
                .define('C', ModItems.HEMP_CANVAS)
                .unlockedBy(getHasName(ModItems.HEMP_CANVAS), has(ModItems.HEMP_CANVAS))
                .save(output, id("sifting_box"));

        // One recipe describing the whole tub. Strength and Quality stay in the block entity — they
        // are measurements of the simmer, not of a recipe — but which items play each part is data.
        output.accept(id("infusing"), new InfusingRecipe(
                ofTag(ModTags.Items.MILK_BUCKETS),
                Ingredient.of(ModItems.DECARBOXYLATED_HEMP),
                Ingredient.of(ModItems.WASHED_DECARBOXYLATED_HEMP),
                java.util.Optional.of(Ingredient.of(ModItems.SCORCHED_HEMP)),
                new ItemStack(ModItems.CANNABUTTER)), null);

        // The Hemp Press. THE MOD'S OWN MACHINE GRAMMAR IS "a vanilla utility block wrapped in a
        // ring of hempdustry material" -- the Decarboxylator is 8 hemp bricks round a copper ingot,
        // the Infuser 8 hempcrete round a cauldron -- and this follows it: 8 hemp bricks round a
        // PISTON, which is the vanilla block whose whole job is to press something. A player reading
        // the grid knows what this does before it has a tooltip.
        //
        // Hemp bricks rather than hempcrete puts it in the Decarboxylator's visual family, which is
        // right: those two are the mod's heated blocks. The multiset differs from the
        // Decarboxylator's by the piston, so the two cannot collide.
        //
        // Unlocked by hemp bricks, like the Decarboxylator. The press is now on the path to ANY
        // hashish at all, so gating its discovery behind an endgame item would hide the block a
        // player needs in order to reach the endgame.
        shaped(RecipeCategory.DECORATIONS, ModBlocks.HEMP_PRESS)
                .pattern("BBB")
                .pattern("BPB")
                .pattern("BBB")
                .define('B', ModBlocks.HEMP_BRICKS_BLOCK)
                .define('P', Items.PISTON)
                .unlockedBy(getHasName(ModBlocks.HEMP_BRICKS_BLOCK), has(ModBlocks.HEMP_BRICKS_BLOCK))
                .save(output, id("hemp_press"));

        // A pot of hemp brick with soil in it, in the shape a flower pot is drawn. Any dirt: rooted,
        // coarse, mud, moss, or a modded soil in the tag. Unlocks on the brick, like the press.
        shaped(RecipeCategory.DECORATIONS, ModBlocks.GROW_POT)
                .pattern("B B")
                .pattern("BDB")
                .pattern(" B ")
                .define('B', ModItems.HEMP_BRICK)
                .define('D', ItemTags.DIRT)
                .unlockedBy(getHasName(ModItems.HEMP_BRICK), has(ModItems.HEMP_BRICK))
                .save(output, id("grow_pot"));

        // A redstone lamp in a copper housing (vanilla's copper bulb is the precedent) behind an
        // amethyst lens — purple, the colour every LED grow light is. The lamp needs glowstone, so
        // the Nether gates the best light, which is what lets it pay +2 buds.
        shaped(RecipeCategory.REDSTONE, ModBlocks.GROW_LAMP)
                .pattern("CAC")
                .pattern("ALA")
                .pattern("CAC")
                .define('C', Items.COPPER_INGOT)
                .define('A', Items.AMETHYST_SHARD)
                .define('L', Items.REDSTONE_LAMP)
                .unlockedBy(getHasName(Items.REDSTONE_LAMP), has(Items.REDSTONE_LAMP))
                .save(output, id("grow_lamp"));

        // A Grow Pot plumbed into a copper reservoir with a pump in it. Copper is the mod's
        // electrical metal (the lamp's housing, and vanilla's own copper bulb); the redstone is the
        // pump, which is also what the block needs a signal for once it is placed.
        shaped(RecipeCategory.REDSTONE, ModBlocks.HYDRO_TRAY)
                .pattern("CRC")
                .pattern("CPC")
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('P', ModBlocks.GROW_POT)
                .unlockedBy(getHasName(ModBlocks.GROW_POT), has(ModBlocks.GROW_POT))
                .save(output, id("hydro_tray"));

        // ---------------------------------------------------------------------
        // The Hemp Press's conversions
        // ---------------------------------------------------------------------
        // ALL FOUR ARE 1:1, WHICH IS THE FURNACE'S GRAMMAR AND NOT AN ACCIDENT. Every vanilla
        // machine is one item in, one item out, per cycle; N-into-1 is what the crafting GRID is
        // for. So the press never makes a bar -- nine pressed pieces make a bar in the grid, exactly
        // as nine ingots make a block.
        //
        // KIEF -> HASHISH is the step the mod was missing entirely. In the trade the powder under
        // the screen is kief, and it is *pressed with heat* to become hashish -- two steps and two
        // tools. The Sifting Box used to do both, which was only ever defensible while there was no
        // press. Now there is one, and the vessel makes powder.
        offerPressing(Ingredient.of(ModItems.KIEF),
                new ItemStack(ModItems.HASHISH), "hashish");

        // The washed powder presses to the blonde slab. Same step, cleaner input -- and the
        // cleanliness is the whole payoff for the ice room, cashed out here as filtered hashish's
        // halved green-out rather than as a bigger number.
        offerPressing(Ingredient.of(ModItems.BUBBLE_HASH),
                new ItemStack(ModItems.FILTERED_HASHISH), "filtered_hashish");

        // The dry road's blonde, pressed. It lands on the SAME item the washed powder does, because
        // the two buy the same thing -- smoothness, which is the only purity axis the mod has -- and
        // pressing is pressing. What separates the roads is price, not product: see
        // SiftingBoxBlock.KIEF_CHANCE, which is where the dry one is made to hurt.
        offerPressing(Ingredient.of(ModItems.FILTERED_KIEF),
                new ItemStack(ModItems.FILTERED_HASHISH), "filtered_hashish_from_filtered_kief");

        // Rosin, 1:1. Pressing hash is the highest-yield step in the whole chain -- 60-90% in life,
        // against 10-30% off flower -- so near enough one for one is the honest number, and it is
        // what makes the tree worth building: one filtered hashish becomes one rosin, and one rosin
        // fills a bong on its own where three filtered hashish would have been needed.
        //
        // FILTERED HASHISH ONLY. Pressing plain hashish, charas, kief or flower is deliberately not
        // a recipe -- not "it yields less", it simply is not offered. That makes the ice room the
        // hard gate on the whole concentrate tier, which is what justifies building one; and it
        // keeps the press to one thing to learn per input, with no second route that exists only to
        // be worse than the first.
        offerPressing(Ingredient.of(ModItems.FILTERED_HASHISH),
                new ItemStack(ModItems.ROSIN, HempPressBlockEntity.ROSIN_OUTPUT),
                "rosin");

        // Breaking and scutching, which is the job materials.md has wanted off the crafting grid
        // since 2026-08-22 -- and the second role that earns this block its slot.
        //
        // The shapeless grid recipe stays and still gives FIBER_PER_RETTED_STEM. Taking it away is a
        // balance change to shipped content with nothing to do with concentrates; making the press
        // simply better at it is additive, and is what a machine is for.
        offerPressing(Ingredient.of(ModItems.RETTED_HEMP_STEM),
                new ItemStack(ModItems.HEMP_FIBER, HempPressBlockEntity.PRESSED_FIBER_OUTPUT),
                "hemp_fiber");

        // ---------------------------------------------------------------------
        // Moon rocks
        // ---------------------------------------------------------------------
        // A bud, dipped in rosin, rolled in resin. Three tiers of the mod's own chain in one item,
        // and it needs no invention at all -- it is what a moon rock is.
        //
        // THE COAT IS A CHOICE, AND IT IS THE ONLY ONE. Whatever you roll it in rides along in the
        // load as a second entry, so the coat decides the moon rock's signature while the plant
        // decides everything else. That is three recipes per strain and zero lines of code --
        // SmokeContents already carries a two-entry load and already names the hash half.
        //
        // A COAT MAY BE GIVEN LOOSE OR PRESSED, and it is the same coat either way. Pressing changes
        // a powder's shape and not its chemistry, so kief stands in for hashish, and bubble hash and
        // filtered kief both stand in for filtered hashish -- one Ingredient each rather than a
        // recipe each. That is vanilla's coal-or-charcoal grammar, not a shortcut needing a
        // counterweight: a loose coat saves exactly one press cycle and no material at all, which is
        // why none of them carries a penalty. There is nothing to penalise.
        //
        // Shapeless, because you are coating a nug rather than laying anything out, and because a
        // ShapelessRecipe built by hand can carry components on its result where the builder cannot.
        // Every multiset differs by the bud or by the coat, so none of them can collide.
        //
        // Unlocked by the rosin: by the time a player has any, they have had the buds and the resin
        // for a long while, and the rosin is the piece that was missing.
        for (ResourceKey<Strain> key : ModStrains.BUILT_IN) {
            Holder.Reference<Strain> strain = strains.getOrThrow(key);
            if (strain.value().wildFlower().isEmpty()) {
                continue;
            }
            offerMoonRock(strain, strains.getOrThrow(ModStrains.HASHISH), "hashish",
                    Ingredient.of(ModItems.HASHISH, ModItems.KIEF));
            offerMoonRock(strain, strains.getOrThrow(ModStrains.FILTERED_HASHISH), "filtered_hashish",
                    Ingredient.of(ModItems.FILTERED_HASHISH, ModItems.BUBBLE_HASH,
                            ModItems.FILTERED_KIEF));
            offerMoonRock(strain, strains.getOrThrow(ModStrains.CHARAS), "charas",
                    Ingredient.of(ModItems.CHARAS));
        }

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
        // Built-in strains only, the same boundary the art takes: a datapack can define a strain
        // but it cannot ship a recipe file for it either.
        //
        // Plants only. A hash-family strain has no flower, and a joint of pure hash would have
        // nothing to burn -- this mod has no tobacco. Hash rolls into a spliff alongside two buds
        // instead, which is the six hand-built recipes further down.
        for (ResourceKey<Strain> key : ModStrains.BUILT_IN) {
            Holder.Reference<Strain> strain = strains.getOrThrow(key);
            if (strain.value().wildFlower().isEmpty()) {
                continue;
            }
            for (int dose = 1; dose <= ModItems.SPLIFF_MAX_DOSE; dose++) {
                offerSpliff(strain, dose);
            }
            // ...and the hash spliffs. Two doses, one pinch of hash in each:
            //
            //   BH / PP   dose 2 -- one bud, one pinch
            //   BHB / PPP dose 3 -- two buds with the pinch between them
            //
            // The hash always sits in the MIDDLE of the bud row, which is what a rolled joint looks
            // like: you lay the hash along the middle of the tobacco (here, the flower) rather than
            // at one end, or it burns unevenly and falls out. At dose 2 there is no middle, so it
            // simply sits beside its bud.
            //
            // Paper scales with dose exactly as the plain spliff's does -- that second resource is
            // what stops a level-III spliff undercutting a bong -- so these can never collide with
            // the plain recipes, whose rows are all buds.
            //
            // Deliberately NOT a full dose ladder: there is no 1 bud + 2 hash. Hash's sweet spot is
            // one piece, since Resistance is the only buff in the family that scales (Hunger does
            // too, but it is a cost), so a second pinch would buy almost nothing and double the recipe count again.
            //
            // A CONCENTRATE IS NOT A PINCH, so rosin is excluded and anything like it will be too.
            // dosePerItem > 1 is the mod-wide spelling of "one of these is a whole bowl": a paper
            // has no hot surface to vaporise it off, and rolling one in as a dose-1 additive would
            // quietly undercut the bong exclusive that is the entire reason the Press exists.
            for (ResourceKey<Strain> hashKey : ModStrains.BUILT_IN) {
                Holder.Reference<Strain> hash = strains.getOrThrow(hashKey);
                if (hash.value().wildFlower().isPresent() || hash.value().dosePerItem() > 1) {
                    continue;
                }
                offerHashSpliff(strain, hash, 1);
                offerHashSpliff(strain, hash, 2);
            }
        }

        // Schwag rolls on its own like any bud -- the schwag joint is the archetype -- but it is not
        // a plant strain, so the loop above never sees it, and nothing rolls it in with hash: coating
        // or cutting junk with resin is a waste of the resin.
        Holder.Reference<Strain> schwag = strains.getOrThrow(ModStrains.SCHWAG);
        for (int dose = 1; dose <= ModItems.SPLIFF_MAX_DOSE; dose++) {
            offerSpliff(schwag, dose);
        }

        shaped(RecipeCategory.MISC, ModItems.WOODEN_PIPE)
                .pattern("P  ")
                .pattern("SSS")
                .define('P', ItemTags.PLANKS)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.STICK), has(Items.STICK))
                .save(output, id("wooden_pipe"));

        shaped(RecipeCategory.MISC, ModItems.BONG)
                .pattern(" PI")
                .pattern("GWG")
                .pattern(" G ")
                .define('P', Blocks.GLASS_PANE)
                .define('I', Items.IRON_NUGGET)
                .define('G', Blocks.GLASS)
                .define('W', Items.WATER_BUCKET)
                .unlockedBy(getHasName(Blocks.GLASS), has(Blocks.GLASS))
                .group("bong")
                .save(output, id("bong"));

        // The same shape in every other glass, one recipe per colour the way vanilla's dyed
        // bundles are -- and grouped, so the recipe book gives all eighteen one button, as it does
        // beds and banners. The pane is the matching stained pane; vanilla makes no tinted pane, so
        // the tinted bong takes a plain one. No two share an ingredient multiset: the glass differs.
        for (Item bong : ModItems.COLORED_BONGS) {
            String name = BuiltInRegistries.ITEM.getKey(bong).getPath();
            String color = name.substring(0, name.length() - "_bong".length());
            boolean tinted = color.equals("tinted");
            Item glass = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(tinted ? "tinted_glass" : color + "_stained_glass"));
            Item pane = tinted ? Items.GLASS_PANE : BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color + "_stained_glass_pane"));
            shaped(RecipeCategory.MISC, bong)
                    .pattern(" PI")
                    .pattern("GWG")
                    .pattern(" G ")
                    .define('P', pane)
                    .define('I', Items.IRON_NUGGET)
                    .define('G', glass)
                    .define('W', Items.WATER_BUCKET)
                    .unlockedBy(getHasName(glass), has(glass))
                    .group("bong")
                    .save(output, id(name));
        }

        // Iron chamber, a redstone heating element, and a hemp-plank body. Redstone is in the CRAFT
        // and not in the bowl, which is what the name promises: no vanilla "Redstone X" burns
        // redstone as fuel — the Torch, Lamp, Repeater and Comparator are every one of them crafted
        // with it and none of them consume it. Packing a vaporizer is therefore bud + device,
        // exactly as it is for the pipe and bong, and PackingRecipe needed no change at all.
        //
        // The hemp planks are the anti-collision guard, the same rule the Decarboxylator's recipe
        // above records: no other mod can reference hempdustry:hemp_planks, so this pattern cannot
        // ambiguously match somebody else's in a kitchen-sink pack. They also earn the slot on their
        // own — wooden-bodied dry-herb vaporizers are the iconic form of the thing.
        //
        // Unlock on the hemp planks rather than the iron: iron is early and unlocking on it would
        // put this in the recipe book long before a player could plausibly build it.
        shaped(RecipeCategory.MISC, ModItems.VAPORIZER)
                .pattern(" I ")
                .pattern("IRI")
                .pattern(" P ")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('P', ModBlocks.HEMP_PLANKS)
                .unlockedBy(getHasName(ModBlocks.HEMP_PLANKS), has(ModBlocks.HEMP_PLANKS))
                .save(output, id("vaporizer"));

        // Pack an empty device with a strain's buds in the crafting grid. Special recipe so the
        // device's durability + enchantments carry over onto the packed result (see PackingRecipe).
        SpecialRecipeBuilder.special(PackingRecipe::new).save(output, id("packing"));

        // ---------------------------------------------------------------------
        // Hemp fiber armor set
        // ---------------------------------------------------------------------
        shaped(RecipeCategory.COMBAT, ModItems.HEMP_BEANIE)
                .pattern("###")
                .pattern("# #")
                .define('#', ModItems.HEMP_FIBER)
                .unlockedBy(getHasName(ModItems.HEMP_FIBER), has(ModItems.HEMP_FIBER))
                .save(output, id("hemp_beanie"));

        shaped(RecipeCategory.COMBAT, ModItems.HEMP_SHIRT)
                .pattern("# #")
                .pattern("###")
                .pattern("###")
                .define('#', ModItems.HEMP_FIBER)
                .unlockedBy(getHasName(ModItems.HEMP_FIBER), has(ModItems.HEMP_FIBER))
                .save(output, id("hemp_shirt"));

        shaped(RecipeCategory.COMBAT, ModItems.HEMP_HAREM_PANTS)
                .pattern("###")
                .pattern("# #")
                .pattern("# #")
                .define('#', ModItems.HEMP_FIBER)
                .unlockedBy(getHasName(ModItems.HEMP_FIBER), has(ModItems.HEMP_FIBER))
                .save(output, id("hemp_harem_pants"));

        shaped(RecipeCategory.COMBAT, ModItems.FLIP_FLOPS)
                .pattern("# #")
                .pattern("# #")
                .define('#', ModItems.HEMP_FIBER)
                .unlockedBy(getHasName(ModItems.HEMP_FIBER), has(ModItems.HEMP_FIBER))
                .save(output, id("flip_flops"));

        // ---------------------------------------------------------------------
        // Edibles & fiber by-products (hemp as an ingredient for vanilla items)
        // ---------------------------------------------------------------------
        shapeless(RecipeCategory.FOOD, ModItems.HEMP_FLOUR)
                .requires(ModTags.Items.HEMP_SEEDS)
                .unlockedBy("has_hemp_seeds", has(ModTags.Items.HEMP_SEEDS))
                .save(output, id("hemp_flour"));

        shaped(RecipeCategory.FOOD, Items.BREAD)
                .pattern(" W ")
                .pattern("###")
                .define('W', Items.WATER_BUCKET)
                .define('#', ModItems.HEMP_FLOUR)
                .unlockedBy(getHasName(ModItems.HEMP_FLOUR), has(ModItems.HEMP_FLOUR))
                .save(output, id("bread"));

        shaped(RecipeCategory.FOOD, Items.COOKIE, 4)
                .pattern("#C#")
                .define('#', ModItems.HEMP_FLOUR)
                .define('C', Items.COCOA_BEANS)
                .unlockedBy(getHasName(ModItems.HEMP_FLOUR), has(ModItems.HEMP_FLOUR))
                .save(output, id("cookie"));

        // Vanilla's cake, poured from hemp milk. Cake is the *only* vanilla recipe that uses a milk
        // bucket at all, so this one recipe is what makes "hemp milk works wherever milk works" a
        // true statement rather than a mod-internal courtesy. Same standing rule the cloth chain
        // follows: hemp milk substitutes where vanilla uses milk. It still wants an egg, so this is
        // not a cow-and-chicken-free cake — it is a cow-free one.
        //
        // The milk slot is hemp milk BY ITEM, never #hempdustry:milk_buckets, and that is not a
        // style choice. The tag contains minecraft:milk_bucket, so a tagged version of this recipe
        // would match a grid of three cow milk — exactly what vanilla's own cake recipe matches —
        // and two recipes that can match the same grid means one of them silently never fires. It
        // is also the one collision scripts/recipe_collisions.py cannot see, since it compares tags
        // as opaque atoms. Mixing cow and hemp milk in one cake is the price, and it is the right
        // price to pay.
        shaped(RecipeCategory.FOOD, Items.CAKE)
                .pattern("MMM")
                .pattern("SES")
                .pattern("###")
                .define('M', ModItems.HEMP_MILK_BUCKET)
                .define('S', Items.SUGAR)
                .define('E', Items.EGG)
                .define('#', Items.WHEAT)
                .unlockedBy(getHasName(ModItems.HEMP_MILK_BUCKET), has(ModItems.HEMP_MILK_BUCKET))
                .save(output, id("cake"));

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItems.HEMP_STEM), RecipeCategory.MISC, Items.GREEN_DYE, 1.0F, 200)
                .group("dye")
                .unlockedBy(getHasName(ModItems.HEMP_STEM), has(ModItems.HEMP_STEM))
                .save(output, id("green_dye"));

        // No 4 fibre -> green wool recipe. It collided byte for byte with the canvas recipe above,
        // and only one of two identical crafting recipes can ever fire. The 2x2 square of a fibre
        // is canvas's, matching vanilla's 4 string -> wool; green wool is still reachable the way
        // vanilla builds every dyed wool — fibre -> string -> white wool, dyed with the green dye
        // this mod already smelts out of hemp stem.

        shapeless(RecipeCategory.MISC, Items.STRING)
                .requires(ModItems.HEMP_FIBER)
                .unlockedBy(getHasName(ModItems.HEMP_FIBER), has(ModItems.HEMP_FIBER))
                .save(output, id("string"));

        shaped(RecipeCategory.MISC, Items.PAPER, 2)
                .pattern("###")
                .define('#', ModItems.HEMP_FIBER)
                .unlockedBy(getHasName(ModItems.HEMP_FIBER), has(ModItems.HEMP_FIBER))
                .save(output, id("paper"));

        // The bed hemp wool makes: vanilla's own bed recipe, with hemp wool where each vanilla bed
        // has its wool. It takes the grid over from the hemp-wool white bed this used to make —
        // two recipes on one grid collide, and only one of them could ever be crafted.
        bedFromPlanksAndWool(ModBlocks.HEMP_BED, ModBlocks.HEMP_WOOL);

        // Any vanilla bed dyes into any other colour, but vanilla's sixteen dyeing recipes name the
        // fifteen other beds one by one rather than a tag, so none of them will ever take a hemp
        // bed. These are the same recipes with the hemp bed as the ingredient they are missing, and
        // white dye is now how hemp wool becomes a white bed.
        for (DyeColor color : ModBlocks.DYE_ORDER) {
            Item bed = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getName() + "_bed"));
            shapeless(RecipeCategory.DECORATIONS, bed)
                    .requires(DyeItem.byColor(color))
                    .requires(ModBlocks.HEMP_BED)
                    .group("bed_dye")
                    .unlockedBy(getHasName(ModBlocks.HEMP_BED), has(ModBlocks.HEMP_BED))
                    .save(output, id(color.getName() + "_bed_from_hemp_bed"));
        }

        // The harness hemp wool makes: vanilla's own harness recipe — three leather, two glass —
        // with hemp wool where each vanilla harness has its wool. Vanilla's harness-dyeing recipes
        // name the other harnesses one by one, as the bed ones do, so the dyeing is ours too.
        harness(ModItems.HEMP_HARNESS, ModBlocks.HEMP_WOOL);
        for (DyeColor color : ModBlocks.DYE_ORDER) {
            Item harness = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(color.getName() + "_harness"));
            shapeless(RecipeCategory.TOOLS, harness)
                    .requires(DyeItem.byColor(color))
                    .requires(ModItems.HEMP_HARNESS)
                    .group("harness_dye")
                    .unlockedBy(getHasName(ModItems.HEMP_HARNESS), has(ModItems.HEMP_HARNESS))
                    .save(output, id(color.getName() + "_harness_from_hemp_harness"));
        }

        // Vanilla's bundle, with canvas where it has leather: a bundle is a sack, and a hemp sack is
        // the real thing. A second route that takes nothing away, like the book and the item frame,
        // and it makes vanilla's own bundle, so the dyeing recipes take it as they are.
        shaped(RecipeCategory.TOOLS, Items.BUNDLE)
                .pattern("-")
                .pattern("#")
                .define('-', Items.STRING)
                .define('#', ModItems.HEMP_CANVAS)
                .unlockedBy(getHasName(ModItems.HEMP_CANVAS), has(ModItems.HEMP_CANVAS))
                .save(output, id("bundle"));

        // Shapeless, matching vanilla's own book recipe. The leather in a book is the *cover*, and
        // cloth-bound hardbacks are entirely ordinary — so canvas reads right there. Combined with
        // the hemp paper above it makes the book hemp all the way through, which is a better joke
        // than it is a stretch: the Gutenberg Bible was printed on hemp paper in 1456, hemp supplied
        // over 70% of paper before 1883, and it is still what bible paper and cigarette paper are.
        //
        // This is the canvas recipe with real reach, and it was a deliberate call rather than a
        // freebie — see CLAUDE.md. Books gate enchanting through bookshelves, so this takes cows off
        // the critical path for an enchanting setup and cascades to lecterns and chiseled bookshelves.
        shapeless(RecipeCategory.MISC, Items.BOOK)
                .requires(Items.PAPER)
                .requires(Items.PAPER)
                .requires(Items.PAPER)
                .requires(ModItems.HEMP_CANVAS)
                .unlockedBy(getHasName(ModItems.HEMP_CANVAS), has(ModItems.HEMP_CANVAS))
                .save(output, id("book"));

        shaped(RecipeCategory.DECORATIONS, Items.FLOWER_POT)
                .pattern("# #")
                .pattern(" # ")
                .define('#', ModItems.HEMP_BRICK)
                .unlockedBy(getHasName(ModItems.HEMP_BRICK), has(ModItems.HEMP_BRICK))
                .save(output, id("flower_pot"));

        shaped(RecipeCategory.MISC, Items.STICK)
                .pattern("#")
                .pattern("#")
                .define('#', ModItems.HEMP_STEM)
                .unlockedBy(getHasName(ModItems.HEMP_STEM), has(ModItems.HEMP_STEM))
                .save(output, id("stick_from_stem"));
        // No stick recipe of our own for hemp planks: they are #minecraft:planks, so vanilla's makes
        // four from them like any planks. A "2 hemp planks -> 6" recipe used to sit here and was
        // the same grid as vanilla's -- by hand the grid made 4, from the recipe book 6.

        // ---------------------------------------------------------------------
        // Hempcrete-dyed concrete powders (8 hempcrete + 1 dye -> 4 powder).
        // Blue/brown dye assignments are corrected here (the hand-written JSONs
        // had them swapped).
        // ---------------------------------------------------------------------
        offerConcretePowder(Items.WHITE_DYE, Items.WHITE_CONCRETE_POWDER);
        offerConcretePowder(Items.ORANGE_DYE, Items.ORANGE_CONCRETE_POWDER);
        offerConcretePowder(Items.MAGENTA_DYE, Items.MAGENTA_CONCRETE_POWDER);
        offerConcretePowder(Items.LIGHT_BLUE_DYE, Items.LIGHT_BLUE_CONCRETE_POWDER);
        offerConcretePowder(Items.YELLOW_DYE, Items.YELLOW_CONCRETE_POWDER);
        offerConcretePowder(Items.LIME_DYE, Items.LIME_CONCRETE_POWDER);
        offerConcretePowder(Items.PINK_DYE, Items.PINK_CONCRETE_POWDER);
        offerConcretePowder(Items.GRAY_DYE, Items.GRAY_CONCRETE_POWDER);
        offerConcretePowder(Items.LIGHT_GRAY_DYE, Items.LIGHT_GRAY_CONCRETE_POWDER);
        offerConcretePowder(Items.CYAN_DYE, Items.CYAN_CONCRETE_POWDER);
        offerConcretePowder(Items.PURPLE_DYE, Items.PURPLE_CONCRETE_POWDER);
        offerConcretePowder(Items.BLUE_DYE, Items.BLUE_CONCRETE_POWDER);
        offerConcretePowder(Items.BROWN_DYE, Items.BROWN_CONCRETE_POWDER);
        offerConcretePowder(Items.GREEN_DYE, Items.GREEN_CONCRETE_POWDER);
        offerConcretePowder(Items.RED_DYE, Items.RED_CONCRETE_POWDER);
        offerConcretePowder(Items.BLACK_DYE, Items.BLACK_CONCRETE_POWDER);
    }

    private void offerConcretePowder(ItemLike dye, ItemLike result) {
        shapeless(RecipeCategory.BUILDING_BLOCKS, result, 4)
                .requires(ModItems.HEMPCRETE, 8)
                .requires(dye)
                .group("hempdustry_concrete_powder")
                .unlockedBy(getHasName(ModItems.HEMPCRETE), has(ModItems.HEMPCRETE))
                .save(output, id(BuiltInRegistries.ITEM.getKey(result.asItem()).getPath()));
    }

    /**
     * One tray-load in the Decarboxylator. Named {@code decarboxylating/<input>} so the folder reads
     * as the machine's whole recipe list, and so a datapack overriding one of ours knows where to
     * put the file.
     *
     * <p>No unlock advancement: machine recipes are not in the recipe book, so there is nothing an
     * advancement could reveal. See {@code DecarboxylatingRecipe}.
     */
    private void offerDecarboxylating(Ingredient input, ItemStack result, String name) {
        output.accept(id("decarboxylating/" + name), new DecarboxylatingRecipe(input, result), null);
    }

    /**
     * One squeeze in the Hemp Press. Named {@code pressing/<output>} so the folder reads as the
     * machine's whole recipe list, and so a datapack overriding one of ours knows where to put it.
     *
     * <p>No unlock advancement: machine recipes are not in the recipe book, so there is nothing an
     * advancement could reveal. See {@link PressingRecipe}.
     */
    private void offerPressing(Ingredient input, ItemStack result, String name) {
        output.accept(id("pressing/" + name), new PressingRecipe(input, result), null);
    }

    /**
     * One strain's moon rock in one coat: its bud, one rosin and one pinch of {@code coat},
     * shapeless, producing the coated nug with its {@code smoke_contents} already built — the plant
     * at {@code ModItems.MOON_ROCK_DOSE} with the coat riding along at 1.
     *
     * <p>{@code coatIngredient} is separate from {@code coat} because the two are different
     * questions: what may be <em>put in</em> the grid, and which strain entry the result <em>carries</em>.
     * The hashish coat accepts kief as well, since kief is hashish that has not been pressed yet.
     *
     * <p>Built by hand rather than through {@link ShapelessRecipeBuilder} because that builder's
     * result is a bare {@code new ItemStack(item, count)} with no way to attach components — the
     * same reason {@link #offerSpliff} is built by hand. The stack comes from
     * {@link ModItems#moonRock} so the creative tab and the recipe cannot disagree about what one is.
     */
    private void offerMoonRock(Holder.Reference<Strain> strain, Holder.Reference<Strain> coat,
                               String coatName, Ingredient coatIngredient) {
        ItemStack result = ModItems.moonRock(strain, coat);
        ResourceKey<Recipe<?>> recipeId = id("moon_rock_" + ModStrains.id(strain.key())
                + "_" + coatName);
        ShapelessRecipe recipe = new ShapelessRecipe("moon_rock", CraftingBookCategory.MISC, result,
                List.of(Ingredient.of(strain.value().buds()),
                        Ingredient.of(ModItems.ROSIN),
                        coatIngredient));
        output.accept(recipeId, recipe, output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion(getHasName(ModItems.ROSIN), has(ModItems.ROSIN))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(recipeId.identifier().withPrefix("recipes/misc/")));
    }

    /**
     * An ingredient built from a tag, which since 1.21.5 needs the registry lookup to resolve the
     * tag's contents rather than carrying the {@code TagKey} through to load time.
     */
    private Ingredient ofTag(TagKey<Item> tag) {
        return Ingredient.of(this.registries.lookupOrThrow(Registries.ITEM).getOrThrow(tag));
    }

    /** A recipe is addressed by a {@code ResourceKey<Recipe<?>>} since 1.21.4, not a bare id. */
    private static ResourceKey<Recipe<?>> id(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, path));
    }

    /**
     * One spliff recipe: {@code dose} buds stacked over {@code dose} paper, producing a spliff whose
     * {@code smoke_contents} component carries the strain and the dose.
     *
     * <p>Built by hand rather than through {@link ShapedRecipeBuilder} because that builder's
     * result is a bare {@code new ItemStack(item, count)} with no way to attach components. Handing
     * a fully-built {@link ShapedRecipe} to the exporter is the supported route, and it still emits
     * the usual unlock advancement so the recipe book discovers it on picking up the buds.
     */
    private void offerSpliff(Holder.Reference<Strain> strain, int dose) {
        ItemStack result = new ItemStack(ModItems.SPLIFF);
        result.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, dose));

        Item budItem = strain.value().buds();
        String buds = String.valueOf('B').repeat(dose);
        String paper = String.valueOf('P').repeat(dose);
        ShapedRecipePattern raw = ShapedRecipePattern.of(
                Map.of('B', Ingredient.of(budItem), 'P', Ingredient.of(Items.PAPER)),
                buds, paper);

        ResourceKey<Recipe<?>> recipeId = id("spliff_" + ModStrains.id(strain.key()) + "_" + dose);
        ShapedRecipe recipe = new ShapedRecipe("spliff", CraftingBookCategory.MISC, raw, result);
        output.accept(recipeId, recipe, output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion(getHasName(budItem), has(budItem))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(recipeId.identifier().withPrefix("recipes/misc/")));
    }


    /**
     * A hash spliff: {@code buds} of a plant strain with one pinch of hash in the middle of the row,
     * over as many paper.
     *
     * <p>The load is two entries, so it is the first thing in the mod to exercise the multi-entry
     * shape {@link SmokeContents} has always had. The plant is the primary entry — by count at
     * {@code buds = 2}, and by {@code SmokeContents.primaryEntry}'s explicit plant-wins-a-tie rule
     * at {@code buds = 1} — which is what names and tints the item; the hash rides along in the
     * tooltip.
     *
     * <p>Unlocked by <b>the hash</b> rather than by the buds — by the time a player has hash they
     * have long since had the buds, and the hash is the new thing.
     */
    private void offerHashSpliff(Holder.Reference<Strain> strain, Holder.Reference<Strain> hash,
                                 int buds) {
        ItemStack result = new ItemStack(ModItems.SPLIFF);
        result.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, buds, hash, 1));

        Item hashItem = hash.value().buds();
        // The pinch in the middle: "BH" at one bud, "BHB" at two. A joint is rolled with the hash
        // down the centre line, not at an end.
        String top = buds == 1 ? "BH" : "BHB";
        String paper = String.valueOf('P').repeat(buds + 1);
        ShapedRecipePattern raw = ShapedRecipePattern.of(
                Map.of('B', Ingredient.of(strain.value().buds()),
                        'H', Ingredient.of(hashItem),
                        'P', Ingredient.of(Items.PAPER)),
                top, paper);

        ResourceKey<Recipe<?>> recipeId = id("spliff_" + ModStrains.id(strain.key())
                + "_with_" + ModStrains.id(hash.key()) + "_" + (buds + 1));
        ShapedRecipe recipe = new ShapedRecipe("spliff", CraftingBookCategory.MISC, raw, result);
        output.accept(recipeId, recipe, output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion(getHasName(hashItem), has(hashItem))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(recipeId.identifier().withPrefix("recipes/misc/")));
    }

    /**
     * A shapeless recipe whose ingredients' containers stay in the result — see
     * {@link ContainerCarriedRecipe}. Built by hand because {@link ShapelessRecipeBuilder} can
     * only emit vanilla's serializer, and still emits the usual unlock advancement so the recipe
     * book discovers it normally.
     */
    /** A result stack with a fixed dose baked in, for edibles that inherit nothing. */
    private static ItemStack dosed(ItemLike item, int potency, Quality quality) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.POTENCY, potency);
        stack.set(ModComponents.QUALITY, quality);
        return stack;
    }

    private void offerContainerCarried(ResourceKey<Recipe<?>> recipeId,
                                       ItemStack result, ItemLike unlockedBy,
                                       List<Ingredient> inputs) {
        ContainerCarriedRecipe recipe = new ContainerCarriedRecipe("", CraftingBookCategory.MISC,
                result, inputs);
        output.accept(recipeId, recipe, output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion(getHasName(unlockedBy), has(unlockedBy))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(recipeId.identifier().withPrefix("recipes/food/")));
    }

    /**
     * Same as above, but unlocked by a tag rather than a single item -- for a recipe whose input
     * is itself tag-based, so every strain's item discovers it rather than only whichever one got
     * hardcoded as the unlock.
     */
    private void offerContainerCarried(ResourceKey<Recipe<?>> recipeId,
                                       ItemStack result, TagKey<Item> unlockedByTag,
                                       List<Ingredient> inputs) {
        ContainerCarriedRecipe recipe = new ContainerCarriedRecipe("", CraftingBookCategory.MISC,
                result, inputs);
        output.accept(recipeId, recipe, output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion("has_hemp_seeds", has(unlockedByTag))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(recipeId.identifier().withPrefix("recipes/food/")));
    }


    /**
     * Furnace, smoker and campfire, the way every vanilla food is cookable in all three. Written out
     * rather than using {@code simpleCookingRecipe} because that helper only takes a single item
     * as input and this one is keyed on the hemp-seed tag, so every strain works and a future strain
     * needs no new recipe.
     */
    private void offerFoodCooking(String name, Ingredient input,
                                  ItemLike result, TagKey<Item> unlockedByTag, float experience) {
        SimpleCookingRecipeBuilder.smelting(input, RecipeCategory.FOOD, result, experience, 200)
                .unlockedBy("has_hemp_seeds", has(unlockedByTag))
                .save(output, id(name));
        SimpleCookingRecipeBuilder.smoking(input, RecipeCategory.FOOD, result, experience, 100)
                .unlockedBy("has_hemp_seeds", has(unlockedByTag))
                .save(output, id(name + "_from_smoking"));
        SimpleCookingRecipeBuilder.campfireCooking(input, RecipeCategory.FOOD, result, experience, 600)
                .unlockedBy("has_hemp_seeds", has(unlockedByTag))
                .save(output, id(name + "_from_campfire_cooking"));
    }


    /**
     * Recipes that carry cannabutter's potency and quality onto the edible — see
     * {@link com.warlonmhite.hempdustry.recipe.Infusion}. The edible's step on the potency ladder
     * (+1 concentrates, 0 is neutral, -1 spreads the butter thin) is <b>read from
     * {@link ModItems#INFUSED_EDIBLES}</b> rather than passed in, so the creative tab and the recipe
     * cannot disagree about which tiers an edible can reach.
     *
     * <p>Built by hand because the vanilla builders can only emit vanilla's serializers. The unlock
     * advancement is assembled the same way {@code offerSpliff} does it, so the recipe book still
     * discovers these on obtaining cannabutter.
     */
    private void offerInfusedShaped(ResourceKey<Recipe<?>> recipeId,
                                    ItemLike output, int count,
                                    Map<Character, Ingredient> key, String... pattern) {
        offerInfused(recipeId, new InfusedShapedRecipe("", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(key, pattern), new ItemStack(output, count),
                ModItems.potencyOffsetOf(output)));
    }

    private void offerInfusedShapeless(ResourceKey<Recipe<?>> recipeId,
                                       ItemLike output, int count,
                                       List<Ingredient> inputs) {
        offerInfused(recipeId, new InfusedShapelessRecipe("", CraftingBookCategory.MISC,
                new ItemStack(output, count), inputs, ModItems.potencyOffsetOf(output)));
    }

    private void offerInfused(ResourceKey<Recipe<?>> recipeId, net.minecraft.world.item.crafting.Recipe<?> recipe) {
        output.accept(recipeId, recipe, output.advancement()
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(recipeId))
                .addCriterion(getHasName(ModItems.CANNABUTTER), has(ModItems.CANNABUTTER))
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(recipeId.identifier().withPrefix("recipes/food/")));
    }
    }
}

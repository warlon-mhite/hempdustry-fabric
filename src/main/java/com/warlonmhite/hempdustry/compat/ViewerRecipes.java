package com.warlonmhite.hempdustry.compat;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.ModCauldronBehaviors;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.HashishBarBlock;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.recipe.DecarboxylatingRecipe;
import com.warlonmhite.hempdustry.recipe.InfusingRecipe;
import com.warlonmhite.hempdustry.recipe.PressingRecipe;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import com.warlonmhite.hempdustry.util.ModTags;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a recipe viewer should be shown, worked out once and read by both plugins.
 *
 * <h2>Why this class exists</h2>
 *
 * EMI and JEI have entirely separate APIs, but they are being asked the same question — <em>what
 * does this machine turn into what, and what does the player have to know that the slots do not
 * say</em>. Answering it twice is how the two viewers drift apart, and a mod whose EMI page and JEI
 * page disagree is worse than one with neither. So the answer lives here, in plain Minecraft types,
 * and each plugin is a thin adapter over it.
 *
 * <p><b>Nothing in this package may be referenced from common code.</b> It is only ever loaded by a
 * viewer that is present; see {@code compat/emi} and {@code compat/jei}.
 *
 * <h2>What each category is for</h2>
 *
 * <ul>
 *   <li><b>Decarboxylating</b> and <b>Infusing</b> read the real {@code RecipeType}s, so a pack that
 *       rebalances them with KubeJS gets a truthful page for free.</li>
 *   <li><b>Cauldron</b> has no recipe to read: retting and washing are {@code CauldronInteraction}
 *       entries, which no viewer can see and no datapack can express. They are built here from the
 *       same constants the behaviours use, so the page cannot drift from the code.</li>
 *   <li><b>In the World</b> is the same for everything done to a block or an item in the world —
 *       see {@link #world}.</li>
 *   <li><b>Packing</b> is not a category at all — see {@link #packing}.</li>
 * </ul>
 */
public final class ViewerRecipes {

    private ViewerRecipes() {
    }

    public static final Identifier DECARBOXYLATING = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "decarboxylating");
    public static final Identifier INFUSING = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "infusing");
    public static final Identifier CAULDRON = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "cauldron");
    public static final Identifier PRESSING = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "pressing");
    public static final Identifier SIFTING = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "sifting");
    public static final Identifier ICE_O_LATOR = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "ice_o_lator");
    public static final Identifier WORLD = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "world");

    /**
     * One row in a viewer: what goes in, what comes out, and the lines of text that carry whatever
     * the slots cannot say.
     *
     * <p>{@code notes} is the part that matters most here. Both machines have a precondition no
     * arrangement of slots can express — the Decarboxylator burns fuel, the Infuser needs something
     * hot <em>underneath</em> it — and a player reading a viewer page is exactly the player who has
     * not found that out yet.
     *
     * @param id        unique per row; a viewer uses it for lookups and for its own bookkeeping
     * @param inputs    in display order
     * @param output    the result, already carrying any components it should show
     * @param notes     lines drawn under the slots, already translated
     * @param synthetic whether this row has no matching recipe in the recipe manager. EMI checks,
     *                  and wants a synthetic id's path prefixed with {@code /} so it can tell the
     *                  difference between "made up for display" and "a real recipe that has gone
     *                  missing" — the second being a bug worth reporting and the first not.
     */
    public record Entry(Identifier id, List<Ingredient> inputs, ItemStack output, List<Component> notes,
                        boolean synthetic) {
    }

    /** Every tray-load the oven will accept, read from {@code hempdustry:decarboxylating}. */
    public static List<Entry> decarboxylating(Level world) {
        List<Entry> out = new ArrayList<>();
        for (RecipeHolder<DecarboxylatingRecipe> entry : ModRecipes.allOfType(world, ModRecipes.DECARBOXYLATING_TYPE)) {
            DecarboxylatingRecipe recipe = entry.value();
            out.add(new Entry(entry.id().identifier(), List.of(recipe.ingredient()), recipe.result(),
                    List.of(Component.translatable("hempdustry.category.decarboxylating.info",
                            seconds(DecarboxylatorBlockEntity.cookTime()))),
                    false));
        }
        return out;
    }

    /**
     * The tub's conversion, read from {@code hempdustry:infusing}.
     *
     * <p>Shown as <b>milk + unwashed + washed</b> even though a batch never needs all three: washed
     * and unwashed are interchangeable in the slots and mixing them is the only way to reach the
     * better grades, so listing both is the honest picture of what the machine takes. The output
     * carries no strength or quality component because those are measured per batch, not per recipe
     * — the note says so rather than the page implying a fixed result.
     *
     * <p><b>The milk sits in the row the way the water bucket does on the cauldron page</b>, and the
     * first note says it is poured in rather than placed: the tub has no milk slot, so a player who
     * opens the screen looking for one needs to have read it here.
     */
    public static List<Entry> infusing(Level world) {
        List<Entry> out = new ArrayList<>();
        for (RecipeHolder<InfusingRecipe> entry : ModRecipes.allOfType(world, ModRecipes.INFUSING_TYPE)) {
            InfusingRecipe recipe = entry.value();
            List<Ingredient> inputs = new ArrayList<>(List.of(recipe.container(), recipe.hemp(), recipe.washedHemp()));
            List<Component> notes = new ArrayList<>(List.of(Component.translatable("hempdustry.category.infusing.pour"),
                    Component.translatable("hempdustry.category.infusing.heat"),
                    Component.translatable("hempdustry.category.infusing.batch",
                            InfuserBlockEntity.BATCH_CAP,
                            minutes(InfuserBlockEntity.minTime()),
                            minutes(InfuserBlockEntity.fullTime()))));
            // Scorched hemp's whole price is invisible from its slot: it counts a quarter and holds
            // the grade down. A player comparing it with the oven's hemp on this page is exactly the
            // one who needs telling -- and a pack that dropped it gets neither the slot nor the note.
            recipe.scorchedHemp().ifPresent(scorched -> {
                inputs.add(scorched);
                notes.add(Component.translatable("hempdustry.category.infusing.scorched",
                        InfuserBlockEntity.SCORCHED_PER_STRENGTH));
            });
            out.add(new Entry(entry.id().identifier(), inputs, recipe.result(), notes, false));
        }
        return out;
    }

    /** Every squeeze the press will take, read from {@code hempdustry:pressing}. */
    public static List<Entry> pressing(Level world) {
        List<Entry> out = new ArrayList<>();
        for (RecipeHolder<PressingRecipe> entry : ModRecipes.allOfType(world, ModRecipes.PRESSING_TYPE)) {
            PressingRecipe recipe = entry.value();
            out.add(new Entry(entry.id().identifier(), List.of(recipe.ingredient()), recipe.result(),
                    List.of(Component.translatable("hempdustry.category.pressing.heat"),
                            Component.translatable("hempdustry.category.pressing.info",
                                    seconds(HempPressBlockEntity.pressTime()))),
                    false));
        }
        return out;
    }

    /**
     * The Sifting Box's two modes, neither of which has a recipe to read: it is a vessel, so what it
     * takes and what it makes are constants in a block rather than data anybody can see. Both are
     * built from those same constants, the way the cauldron's page is, so a page cannot drift from
     * the code.
     *
     * <p>Two categories rather than one, because <b>the water changes what comes out</b> and a
     * single page could not say that without lying about one of the two.
     */
    public static List<Entry> sifting(Level world) {
        return List.of(
                screen(world, ModTags.Items.SIFTABLE_FLOWER, SiftingBoxBlock.FLOWER_CHANCE,
                        ModItems.KIEF, "flower"),
                screen(world, ModTags.Items.SIFTABLE_RESINOUS, SiftingBoxBlock.RESINOUS_CHANCE,
                        ModItems.KIEF, "resinous"),
                screen(world, ModTags.Items.SIFTABLE_TRIM, SiftingBoxBlock.TRIM_CHANCE,
                        ModItems.KIEF, "trim"),
                // The re-sift, which is the one row a player will not guess: the same box, a second
                // pass, and a different product out. Its cost is the whole reason the ice room is
                // worth building, so it has to be visible rather than discovered.
                screen(world, ModTags.Items.SIFTABLE_KIEF, SiftingBoxBlock.KIEF_CHANCE,
                        ModItems.FILTERED_KIEF, "kief"));
    }

    private static Entry screen(Level world, TagKey<Item> input, float chance, Item output, String name) {
        return new Entry(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "sifting/" + name),
                List.of(ofTag(world, input)),
                new ItemStack(output, SiftingBoxBlock.YIELD),
                List.of(Component.translatable("hempdustry.category.sifting.info",
                        Math.round(SiftingBoxBlock.FULL_LEVEL / chance))),
                true);
    }

    /**
     * The ice-water wash — the same block with a bucket of water in it and ice on all four sides.
     *
     * <p><b>Three rows, one per jacket</b>, because the jacket is the entire mechanic and a page
     * that showed only "plant in, powder out" would be hiding the one thing a player has to know.
     * The counts are the same arithmetic the block runs: {@code FULL_LEVEL} over the rate.
     */
    public static List<Entry> iceOLator(Level world) {
        return List.of(
                wash(world, Items.BLUE_ICE, SiftingBoxBlock.BLUE_ICE_RATE, ModTags.Items.SIFTABLE_FLOWER, SiftingBoxBlock.FLOWER_CHANCE, ""),
                wash(world, Items.PACKED_ICE, SiftingBoxBlock.PACKED_ICE_RATE, ModTags.Items.SIFTABLE_FLOWER, SiftingBoxBlock.FLOWER_CHANCE, ""),
                wash(world, Items.ICE, SiftingBoxBlock.ICE_RATE, ModTags.Items.SIFTABLE_FLOWER, SiftingBoxBlock.FLOWER_CHANCE, ""),
                // The same three jackets for resinous buds, so the wash page shows what Beldía costs
                // rather than leaving it off the page.
                wash(world, Items.BLUE_ICE, SiftingBoxBlock.BLUE_ICE_RATE, ModTags.Items.SIFTABLE_RESINOUS, SiftingBoxBlock.RESINOUS_CHANCE, "resinous/"),
                wash(world, Items.PACKED_ICE, SiftingBoxBlock.PACKED_ICE_RATE, ModTags.Items.SIFTABLE_RESINOUS, SiftingBoxBlock.RESINOUS_CHANCE, "resinous/"),
                wash(world, Items.ICE, SiftingBoxBlock.ICE_RATE, ModTags.Items.SIFTABLE_RESINOUS, SiftingBoxBlock.RESINOUS_CHANCE, "resinous/"));
    }

    private static Entry wash(Level world, Item jacket, float rate, TagKey<Item> buds, float budChance, String idPrefix) {
        int budCount = Math.round(SiftingBoxBlock.FULL_LEVEL / (budChance * rate));
        int leaves = Math.round(SiftingBoxBlock.FULL_LEVEL / (SiftingBoxBlock.TRIM_CHANCE * rate));
        return new Entry(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "ice_o_lator/" + idPrefix + BuiltInRegistries.ITEM.getKey(jacket).getPath()),
                List.of(ofTag(world, buds),
                        Ingredient.of(Items.WATER_BUCKET),
                        Ingredient.of(jacket)),
                new ItemStack(ModItems.BUBBLE_HASH, SiftingBoxBlock.YIELD),
                List.of(Component.translatable("hempdustry.category.ice_o_lator.jacket")
                                .withStyle(ChatFormatting.DARK_GRAY),
                        Component.translatable("hempdustry.category.ice_o_lator.info", budCount, leaves)),
                true);
    }

    /**
     * An {@code Ingredient} over a tag, resolved against the world the viewer is looking at — which
     * is what makes these pages show a third-party strain's buds the moment a datapack adds them.
     * An unresolvable tag answers empty rather than throwing: a missing tag is a datapack problem,
     * not a reason for a recipe viewer to fail to open.
     */
    private static Ingredient ofTag(Level world, TagKey<Item> tag) {
        return ofTag(world, tag, ModItems.HEMP_LEAF);
    }

    private static Ingredient ofTag(Level world, TagKey<Item> tag, Item fallback) {
        return world.registryAccess().lookupOrThrow(Registries.ITEM).get(tag)
                .map(Ingredient::of).orElse(Ingredient.of(fallback));
    }

    /**
     * The two water-cauldron behaviours, which are the only step in the whole chain a player cannot
     * discover from anywhere else: they are not recipes, so the recipe book cannot show them, and
     * they are code rather than data, so a datapack cannot even list them.
     *
     * <p>Built from {@link ModCauldronBehaviors}' own constants so the numbers on the page are the
     * numbers the block uses.
     */
    public static List<Entry> cauldron() {
        return List.of(
                soak("retting", ModItems.HEMP_STEM, ModItems.RETTED_HEMP_STEM,
                        ModCauldronBehaviors.RET_PER_LEVEL),
                soak("washing", ModItems.DECARBOXYLATED_HEMP, ModItems.WASHED_DECARBOXYLATED_HEMP,
                        ModCauldronBehaviors.WASH_PER_LEVEL));
    }

    private static Entry soak(String name, Item input, Item output, int perLevel) {
        return new Entry(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "cauldron/" + name),
                List.of(Ingredient.of(input), Ingredient.of(Items.WATER_BUCKET)),
                new ItemStack(output),
                List.of(Component.translatable("hempdustry.category.cauldron.info", perLevel)),
                true);
    }

    /**
     * Everything a player gets by doing something to a block or an item rather than putting it in
     * a grid or a machine: charas, the trimmed leaf, the pieces cut off a bar, the vaporizer's
     * scorched hemp and schwag. None of them has a recipe, so without this page each is an item a
     * viewer can say nothing about -- charas least of all, whose only source is a right-click.
     *
     * <p>A plant is drawn as its seeds, because a crop block's item is its seeds. The numbers come
     * from the same constants the blocks and the device read.
     */
    public static List<Entry> world(Level world) {
        List<Holder.Reference<Strain>> strains = Strain.all(world.registryAccess());
        List<Item> seeds = strains.stream().filter(strain -> strain.value().wildFlower().isPresent())
                .flatMap(strain -> strain.value().seedItem().stream()).toList();
        List<Item> spendable = strains.stream().filter(ModStrains::isPlantMatter)
                .map(strain -> strain.value().buds()).toList();
        Ingredient shears = ofTag(world, ConventionalItemTags.SHEAR_TOOLS, Items.SHEARS);

        List<Entry> out = new ArrayList<>();
        // A datapack can take every plant away; an empty Ingredient throws, so the plant rows go too.
        if (!seeds.isEmpty()) {
            Ingredient plant = Ingredient.of(seeds.stream());
            out.add(inWorld("trimming", List.of(plant, shears), new ItemStack(ModItems.HEMP_LEAF),
                    Component.translatable("hempdustry.category.world.trimming"),
                    Component.translatable("hempdustry.category.world.trimming.pays")));
            out.add(inWorld("rubbing", List.of(plant, shears), new ItemStack(ModItems.CHARAS),
                    Component.translatable("hempdustry.category.world.rubbing", Defoliation.CHARAS_CHANCE_ONE_IN),
                    Component.translatable("hempdustry.category.world.rubbing.lamps",
                            Defoliation.CHARAS_CHANCE_ONE_IN_UNDER_LIGHTS)));
            out.add(inWorld("schwag", List.of(plant), new ItemStack(ModItems.SCHWAG),
                    Component.translatable("hempdustry.category.world.schwag"),
                    Component.translatable("hempdustry.category.world.schwag.fortune")));
        }
        Ingredient blades = ofTag(world, ModTags.Items.HASH_CUTTERS, Items.IRON_SWORD);
        out.add(cut(ModBlocks.HASHISH_BAR, ModItems.HASHISH, blades));
        out.add(cut(ModBlocks.FILTERED_HASHISH_BAR, ModItems.FILTERED_HASHISH, blades));
        if (!spendable.isEmpty()) {
            out.add(inWorld("vaporizer",
                    List.of(Ingredient.of(ModItems.VAPORIZER), Ingredient.of(spendable.stream())),
                    new ItemStack(ModItems.SCORCHED_HEMP,
                            DeviceStats.bowl(world.registryAccess(), DeviceType.VAPORIZER).spentYield()),
                    Component.translatable("hempdustry.category.world.vaporizer"),
                    Component.translatable("hempdustry.category.world.vaporizer.plant")));
        }
        return out;
    }

    /** A bar is placed and cut, never unpacked in a grid -- five cuts, the whole bar in pieces. */
    private static Entry cut(net.minecraft.world.level.block.Block bar, Item piece, Ingredient blades) {
        return inWorld("cutting/" + BuiltInRegistries.BLOCK.getKey(bar).getPath(),
                List.of(Ingredient.of(bar), blades),
                new ItemStack(piece, HashishBarBlock.remaining(0)),
                Component.translatable("hempdustry.category.world.cutting"));
    }

    private static Entry inWorld(String name, List<Ingredient> inputs, ItemStack output, Component... notes) {
        return new Entry(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "world/" + name), inputs, output,
                List.of(notes), true);
    }

    /**
     * What a viewer can only say in a paragraph, handed to each viewer's own information page. A
     * packed bong sets down only while sneaking, because a plain right-click is how it is smoked,
     * and no crafting row can say so. The indoor-growing blocks can only be shown being crafted:
     * what each one does is a paragraph, not a row of slots.
     */
    public static Map<Item, Component> info() {
        Map<Item, Component> out = new LinkedHashMap<>();
        for (Item bong : ModItems.bongs()) {
            out.put(bong, Component.translatable("hempdustry.info.bong"));
        }
        out.put(ModBlocks.GROW_POT.asItem(), Component.translatable("hempdustry.info.grow_pot"));
        out.put(ModBlocks.HYDRO_TRAY.asItem(), Component.translatable("hempdustry.info.hydro_tray"));
        out.put(ModBlocks.GROW_LAMP.asItem(), Component.translatable("hempdustry.info.grow_lamp"));
        return out;
    }

    /**
     * Every way a device can be packed: an empty pipe or bong plus 1..{@code maxDose} buds of one
     * strain, yielding that device loaded to that dose.
     *
     * <h2>These are handed to the viewers as ordinary crafting recipes, on purpose</h2>
     *
     * Packing is a {@code CustomRecipe} — it has to be, because durability and enchantments
     * must survive it and the result varies with the bud count — and a special recipe is invisible
     * to the recipe book and to every viewer. The obvious fix is a bespoke "packing" category. The
     * better one is to hand each permutation over as a plain shapeless crafting recipe, because that
     * is <em>what the player actually does</em>: put a device and some buds in the grid. It costs no
     * custom category, it lands in the crafting tab where a player already looks, and both viewers'
     * built-in "move ingredients into the grid" support works on it for free — which is the single
     * feature a player most expects to behave identically across every mod.
     *
     * <p>The permutations are real, not illustrative: the same components the real recipe sets, so
     * the entry's output is byte-for-byte the item crafting one produces.
     *
     * <p>Each input slot is a list of stacks rather than an {@code Ingredient}, because two of them
     * need what an ingredient cannot say: the moon rock carries its load in a component, and the
     * bong's slot takes all eighteen glasses.
     */
    public static List<Packing> packing(HolderLookup.Provider registries) {
        List<Packing> out = new ArrayList<>();
        HolderLookup.RegistryLookup<Strain> registry = Strain.registry(registries);
        // The three coats, in the order their recipes are written. A coat a datapack removed is
        // simply not offered.
        List<Holder.Reference<Strain>> coats = java.util.stream.Stream.of(
                        ModStrains.HASHISH, ModStrains.FILTERED_HASHISH, ModStrains.CHARAS)
                .flatMap(key -> registry.get(key).stream()).toList();
        // The strains with art of their own first, in the creative tab's order; schwag and scorched
        // hemp have none (model_index 0) and would otherwise open every device's list.
        List<Holder.Reference<Strain>> strains = new ArrayList<>(Strain.all(registries));
        strains.sort(java.util.Comparator.comparing(strain -> strain.value().modelIndex() == 0));
        for (Holder.Reference<Strain> strain : strains) {
            // Every device, from the one place that knows them all: a device added later gets its
            // packing rows in both viewers without this file being touched.
            for (Map.Entry<DeviceType, Item> entry : ModItems.devices().entrySet()) {
                DeviceType type = entry.getKey();
                Item device = entry.getValue();
                DeviceStats.Bowl bowl = DeviceStats.bowl(registries, type);
                // Rows climb one ITEM at a time, not one dose point at a time, and a row that the
                // real recipe would refuse is never drawn. Identical for every strain but rosin,
                // whose one piece is worth three -- so it gets a bong row and no others, which is
                // the whole of "concentrates are bong-only" showing up in a viewer for free.
                int step = Math.max(1, strain.value().dosePerItem());
                for (int dose = step; dose <= bowl.maxDose(); dose += step) {
                    out.add(packed(strain, device, type, dose, bowl.hits()));
                }
                // The moon rock: one item, one bowl, and the only row here whose load is two
                // entries -- one row per coat, since the coat is half of what it carries. Skipped
                // where a device's maxDose refuses it, which is a pipe and a vaporizer, and skipped
                // for strains that never grew on a plant.
                if (strain.value().wildFlower().isPresent() && ModItems.MOON_ROCK_DOSE <= bowl.maxDose()) {
                    for (Holder.Reference<Strain> coat : coats) {
                        out.add(packedMoonRock(strain, coat, device, type, bowl.hits()));
                    }
                }
            }
        }
        return out;
    }

    private static Packing packed(Holder.Reference<Strain> strain, Item device,
                                  DeviceType type, int dose, int hits) {
        ItemStack result = new ItemStack(device);
        result.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, dose));
        result.set(ModComponents.CHARGES, hits);

        List<List<ItemStack>> inputs = new ArrayList<>();
        inputs.add(anyOf(type, device));
        // How many of the item, not how many dose points -- see the step above.
        int items = dose / Math.max(1, strain.value().dosePerItem());
        for (int i = 0; i < items; i++) {
            inputs.add(List.of(new ItemStack(strain.value().buds())));
        }
        Identifier id = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID,
                "packing/" + strain.key().identifier().getPath() + "/"
                        + type.name().toLowerCase(java.util.Locale.ROOT) + "_" + dose);
        return new Packing(id, inputs, result);
    }

    /**
     * A moon rock row. The input is the loaded moon rock itself -- {@link ModItems#moonRock}, the
     * stack its own recipe bakes -- so the page draws "Purple Kush Moon Rock" in its tint rather
     * than a bare nug, and a viewer's "move ingredients into the grid" looks for that one.
     */
    private static Packing packedMoonRock(Holder.Reference<Strain> strain,
                                          Holder.Reference<Strain> coat, Item device, DeviceType type,
                                          int hits) {
        ItemStack moonRock = ModItems.moonRock(strain, coat);
        ItemStack result = new ItemStack(device);
        result.set(ModComponents.SMOKE_CONTENTS,
                moonRock.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY));
        result.set(ModComponents.CHARGES, hits);
        Identifier id = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID,
                "packing/" + strain.key().identifier().getPath() + "/"
                        + type.name().toLowerCase(java.util.Locale.ROOT) + "_moon_rock_"
                        + coat.key().identifier().getPath());
        return new Packing(id, List.of(anyOf(type, device), List.of(moonRock)), result);
    }

    /**
     * Every item that is this device. A bong packs the same in any of its eighteen glasses, so its
     * slot offers them all -- which is what puts the packing rows under a stained-glass bong's uses
     * without drawing each row eighteen times. The result stays the clear one.
     */
    private static List<ItemStack> anyOf(DeviceType type, Item device) {
        List<Item> items = type == DeviceType.BONG ? ModItems.bongs() : List.of(device);
        return items.stream().map(ItemStack::new).toList();
    }

    /** One packing permutation, shaped for whatever a viewer calls a shapeless crafting recipe. */
    public record Packing(Identifier id, List<List<ItemStack>> inputs, ItemStack output) {
    }

    /**
     * An id marked as made-up, EMI's way: a leading {@code /} on the path.
     *
     * <p>EMI checks every recipe id against the recipe manager and logs an error for each one it
     * cannot find, because a real recipe that has vanished is a bug worth shouting about. The
     * prefix is how a plugin says "this row was never a recipe" — the cauldron behaviours and the
     * packing permutations both are. Legal in an {@code Identifier}, which allows {@code /} in a
     * path.
     */
    public static Identifier synthetic(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "/" + id.getPath());
    }

    /**
     * What makes two stacks of the same smokeable <b>different items</b> to a recipe viewer.
     *
     * <h2>Why this is needed at all</h2>
     *
     * A viewer indexes by item, and everything a packed pipe carries lives in components — so
     * without being told otherwise, an empty bong, a Purple Kush bong and a Lemon Haze bong are one
     * entry. JEI says so out loud on startup ("5 duplicate items were found in 'Hempdustry' creative
     * tab"); EMI says nothing and simply collapses them. <b>It also breaks this mod's own plugin</b>:
     * the packing entries differ only by their output's components, so ten recipes would fold into
     * one or two.
     *
     * <p>Keyed on {@code smoke_contents} and <b>nothing else</b>. Charges and damage are deliberately
     * excluded: a half-smoked bowl and a chipped bong are the same <em>kind</em> of thing, and
     * including them would split the item list into a separate entry per durability point.
     *
     * <p>An empty device answers the empty string, which is what both viewers read as "no subtype".
     */
    public static String smokeKey(ItemStack stack) {
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        if (contents.isEmpty()) {
            return "";
        }
        StringBuilder key = new StringBuilder();
        for (SmokeContents.Entry entry : contents.entries()) {
            if (!key.isEmpty()) {
                key.append(',');
            }
            key.append(entry.strain().unwrapKey().map(k -> k.identifier().toString()).orElse("?"))
                    .append('x').append(entry.count());
        }
        return key.toString();
    }

    /**
     * The items {@link #smokeKey} applies to — everything that can be packed.
     *
     * <p>Built from {@link ModItems#devices()} rather than listed by hand: this is what tells a
     * viewer that a packed vaporizer and an empty one are different entries, and a device left out
     * of it silently folds every load of that device into the empty one in the item list.
     */
    public static List<Item> smokeables() {
        List<Item> out = new ArrayList<>();
        out.add(ModItems.SPLIFF);
        // A moon rock's strain lives in the same component a spliff's does, so without this every
        // strain's moon rock folds into one entry in the item list -- and the moon rock crafting
        // rows, which differ only by that component, collapse with them.
        out.add(ModItems.MOON_ROCK);
        out.addAll(ModItems.devices().values());
        // Not in devices(), but packed just the same once a player has one.
        out.addAll(ModItems.COLORED_BONGS);
        return out;
    }

    /** Ticks as whole seconds, for a note. */
    private static String seconds(int ticks) {
        return String.valueOf(Math.max(1, ticks / 20));
    }

    /** Ticks as whole minutes of real time, which is what a player waiting at the tub experiences. */
    private static String minutes(int ticks) {
        return String.valueOf(Math.max(1, ticks / (20 * 60)));
    }
}

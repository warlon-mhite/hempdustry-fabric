package com.warlonmhite.hempdustry.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Recipes for other mods' machines, each loaded only when that mod is: Create, Mekanism and MoreMachine,
 * Farmer's Delight, Botany Pots and Immersive Engineering; hemp as a Silent Gear material; and other mods'
 * rope and sacks made from hemp fibre on the crafting table.
 *
 * <p>Every file carries both loaders' conditions, {@code neoforge:conditions} and {@code fabric:load_conditions};
 * each loader reads its own key and passes over the other's, so one file serves both jars, and without the
 * other mod the recipe is never parsed at all (its recipe type would not exist). Where the partner writes its
 * own ingredient differently per loader, as Farmer's Delight writes the knife, one file cannot carry both: each
 * loader gets a twin of its own, and the other loader's condition turns it off.
 *
 * <p>The numbers follow three rules. A machine route is a trade, never better on every count than our own
 * chain: Create's mixer doubles the hand rate, exactly as its own andesite alloy does, and nothing here beats
 * the Hemp Press. A pot or a cloche gives what a field cannot be spared for -- stems, buds, now and then a
 * seed -- and never the leaves, so the trim, the kief and everything after them still need real plants. And
 * nothing here activates anything: heat does that, in the Decarboxylator.
 */
public class ModCompatProvider implements DataProvider {
    private static final String CREATE = "create";
    private static final String MEKANISM = "mekanism";
    private static final String FARMERS_DELIGHT = "farmersdelight";
    private static final String BOTANY_POTS = "botanypots";
    private static final String IMMERSIVE_ENGINEERING = "immersiveengineering";
    private static final String MORE_MACHINE = "mekmm";
    private static final String SILENT_GEAR = "silentgear";
    private static final String SUPPLEMENTARIES = "supplementaries";

    /** A pot's and a cloche's harvest: a bud at two in three, two stems, a seed at one in four. */
    private static final float POT_BUD_CHANCE = 0.667F;
    private static final float POT_SEED_CHANCE = 0.25F;
    /** Twice Botany Pots' default for a crop (1200 ticks): the plant stands three blocks tall. */
    private static final int POT_GROW_TICKS = 2400;
    /** Twice Immersive Engineering's own hemp (800 ticks), for the same reason. */
    private static final int CLOCHE_TICKS = 1600;
    /** Plant oil from a hemp seed, as Immersive Engineering pays its own (wheat seeds give 80). */
    private static final int SEED_OIL_MB = 120;

    private final PackOutput output;
    private final List<CompletableFuture<?>> writes = new ArrayList<>();
    private CachedOutput cache;

    public ModCompatProvider(FabricDataOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.cache = cache;
        this.writes.clear();
        create();
        mekanism();
        farmersDelight();
        botanyPots();
        immersiveEngineering();
        moreMachine();
        silentGear();
        supplementaries();
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Compat recipes";
    }

    // ----- Create -----

    private void create() {
        // Create's runtime generator reads hemp_stem beside hemp_planks as a log and its planks, and saws one
        // stem into six planks; ours take twelve stems a plank. A recipe of ours at Create's own id loads
        // after Create's generated pack and replaces it: the saw strips the stalk into its fibre, as many as
        // by hand.
        JsonObject saw = recipe(CREATE, "create:cutting",
                ingredients(item(ModItems.HEMP_STEM)),
                results(result(ModItems.HEMP_FIBER, 4)));
        saw.addProperty("processing_time", 50);
        write(CREATE, "cutting/runtime_generated/compat/create/tag_runtime_generated/compat/hempdustry/hemp_to_hemp_planks", saw);

        // Hempcrete is hemp hurd, a lime binder and water, mixed cold. The hand recipe leaves the binder out;
        // the mixer adds it (limestone standing in for lime, which Create has none of), so the same stems go
        // twice as far -- the step Create's own andesite alloy takes from grid to mixer.
        JsonArray mix = ingredients(item(ModItems.HEMP_STEM), item(ModItems.HEMP_STEM), item("create:limestone"));
        JsonObject water = new JsonObject();
        water.addProperty("type", "neoforge:single");
        water.addProperty("amount", 250);
        water.addProperty("fluid", "minecraft:water");
        mix.add(water);
        write(CREATE, "mixing/hempcrete", recipe(CREATE, "create:mixing", mix,
                results(result(ModItems.HEMPCRETE, 2))));

        // Water retting, by the fan: what a cauldron does, one stalk for one.
        write(CREATE, "splashing/retted_hemp_stem", recipe(CREATE, "create:splashing",
                ingredients(item(ModItems.HEMP_STEM)), results(result(ModItems.RETTED_HEMP_STEM, 1))));
        // The wash between the Decarboxylator and the Infuser, by the fan, as the cauldron does it.
        write(CREATE, "splashing/washed_decarboxylated_hemp", recipe(CREATE, "create:splashing",
                ingredients(item(ModItems.DECARBOXYLATED_HEMP)),
                results(result(ModItems.WASHED_DECARBOXYLATED_HEMP, 1))));

        // Breaking and scutching a retted stalk: six fibre as by hand, and a half chance of a seventh; the
        // Hemp Press's eight stay the best there is.
        JsonObject crush = recipe(CREATE, "create:crushing", ingredients(item(ModItems.RETTED_HEMP_STEM)),
                results(result(ModItems.HEMP_FIBER, 6), chance(result(ModItems.HEMP_FIBER, 1), 0.5F)));
        crush.addProperty("processing_time", 100);
        write(CREATE, "crushing/hemp_fiber", crush);

        // Seed to flour, a quarter more than by hand, as Create's millstone does for wheat.
        JsonObject mill = recipe(CREATE, "create:milling", ingredients(tag(ModTags.Items.HEMP_SEEDS.location().toString())),
                results(result(ModItems.HEMP_FLOUR, 1), chance(result(ModItems.HEMP_FLOUR, 1), 0.25F)));
        mill.addProperty("processing_time", 100);
        write(CREATE, "milling/hemp_flour", mill);
    }

    // ----- Mekanism -----

    private void mekanism() {
        // Plant matter into bio fuel, at Mekanism's own rate for a crop and a stalk. The seeds already go
        // through c:seeds. Bio fuel is all it ever makes: no crusher of another mod makes kief.
        write(MEKANISM, "crushing/bio_fuel_from_hemp", mekanismCrushing(tag("c:crops/hemp"), 2));
        write(MEKANISM, "crushing/bio_fuel_from_hemp_stem", mekanismCrushing(item(ModItems.HEMP_STEM), 2));
    }

    private JsonObject mekanismCrushing(JsonObject input, int fuel) {
        input.addProperty("count", 1);
        JsonObject out = new JsonObject();
        out.addProperty("count", fuel);
        out.addProperty("id", "mekanism:bio_fuel");
        JsonObject recipe = conditioned(MEKANISM, "mekanism:crushing");
        recipe.add("input", input);
        recipe.add("output", out);
        return recipe;
    }

    // ----- Farmer's Delight -----

    private void farmersDelight() {
        // Stripping a stalk on the cutting board: its fibre, as by hand, and now and then the woody core
        // that is left, as a stick.
        cutting("hemp_fiber", ModItems.HEMP_STEM, 4);
        // A retted stalk on the board is scutching by hand: its 6 fibre, as from the grid, and the
        // shives -- the woody core the fibre is beaten free of -- now and then as a stick.
        cutting("hemp_fiber_from_retted_hemp_stem", ModItems.RETTED_HEMP_STEM, 6);

        // Rope is what hemp was grown for. Farmer's Delight twists two straw in a column into four rope; two
        // hemp fibre give the same, so its rope fence, safety net and climbing rope can be all hemp.
        write(FARMERS_DELIGHT, "rope", shaped(FARMERS_DELIGHT, new String[]{"#", "#"}, fiber(),
                "farmersdelight:rope", 4));
    }

    // ----- Supplementaries -----

    private void supplementaries() {
        // Supplementaries twists three flax in a column into three rope, the rope its pulleys, rope arrows
        // and climbing take; hemp fibre makes it at the same rate. Its own feature flags are not asked: their
        // condition type exists only with Supplementaries loaded, and the loaders parse every condition of a
        // file before testing any, so naming one would fail the file in every pack without it.
        write(SUPPLEMENTARIES, "rope", shaped(SUPPLEMENTARIES, new String[]{"#", "#", "#"}, fiber(),
                "supplementaries:rope", 3));
        // Its sack, from seven fibre and a string tie, as from flax. Hemp sacking is the old thing: jute's
        // burlap took its place only in the nineteenth century.
        JsonObject sack = shaped(SUPPLEMENTARIES, new String[]{"#S#", "# #", "###"}, fiber(),
                "supplementaries:sack", 1);
        sack.getAsJsonObject("key").add("S", tag("c:strings"));
        write(SUPPLEMENTARIES, "sack", sack);
    }

    /** Hemp fibre, ours or Immersive Engineering's, as the mod's own cloth recipes take it. */
    private static JsonObject fiber() {
        return tag(ModTags.Conventional.FIBER_HEMP.location().toString());
    }

    /** A crafting-table recipe for another mod's item: every {@code #} in {@code pattern} is {@code ingredient}. */
    private static JsonObject shaped(String modId, String[] pattern, JsonObject ingredient, String result, int count) {
        JsonObject recipe = conditioned(modId, "minecraft:crafting_shaped");
        recipe.addProperty("category", "misc");
        JsonObject key = new JsonObject();
        key.add("#", ingredient);
        recipe.add("key", key);
        JsonArray rows = new JsonArray();
        for (String row : pattern) {
            rows.add(row);
        }
        recipe.add("pattern", rows);
        recipe.add("result", result(result, count));
        return recipe;
    }

    /**
     * A stalk on Farmer's Delight's cutting board, cut with a knife into {@code fiber} fibre and, at one in four,
     * a stick. One file per loader, because the two Farmer's Delights read the knife differently: NeoForge's
     * takes a list of ingredients, the Fabric one (Refabricated) a {@code fabric:any} ingredient, and neither
     * reads the other's form. The NeoForge file keeps the recipe's id; the Fabric twin sits under
     * {@code fabric/}.
     */
    private void cutting(String name, ItemLike stalk, int fiber) {
        for (boolean fabric : new boolean[]{false, true}) {
            JsonObject recipe = conditionedFor(fabric, FARMERS_DELIGHT, "farmersdelight:cutting");
            recipe.add("ingredients", ingredients(item(stalk)));
            JsonArray result = new JsonArray();
            result.add(farmersDelightResult(ModItems.HEMP_FIBER, fiber, 1.0F));
            result.add(farmersDelightResult("minecraft:stick", 1, 0.25F));
            recipe.add("result", result);
            recipe.add("tool", fabric ? knifeFabric() : knife());
            write(FARMERS_DELIGHT, (fabric ? "fabric/" : "") + "cutting/" + name, recipe);
        }
    }

    /** A knife as NeoForge's Farmer's Delight writes it: anything that can knife-dig, or a {@code c:} knife. */
    private static JsonArray knife() {
        JsonArray tool = new JsonArray();
        JsonObject ability = new JsonObject();
        ability.addProperty("type", "farmersdelight:item_ability");
        ability.addProperty("action", "knife_dig");
        tool.add(ability);
        tool.add(tag("c:tools/knife"));
        return tool;
    }

    /** The same knife as Farmer's Delight Refabricated writes it, on Fabric. */
    private static JsonObject knifeFabric() {
        JsonArray any = new JsonArray();
        JsonObject ability = new JsonObject();
        ability.addProperty("fabric:type", "farmersdelight:item_ability");
        ability.addProperty("action", "knife_dig");
        any.add(ability);
        any.add(tag("c:tools/knife"));
        JsonObject tool = new JsonObject();
        tool.addProperty("fabric:type", "fabric:any");
        tool.add("ingredients", any);
        return tool;
    }

    private static JsonObject farmersDelightResult(Object item, int count, float chance) {
        JsonObject stack = new JsonObject();
        stack.addProperty("count", count);
        stack.addProperty("id", id(item));
        JsonObject entry = new JsonObject();
        entry.add("item", stack);
        if (chance < 1.0F) {
            entry.addProperty("chance", chance);
        }
        return entry;
    }

    // ----- Botany Pots -----

    private void botanyPots() {
        pot("indica", ModItems.INDICA_SEEDS, ModBlocks.INDICA_CROP, ModItems.INDICA_BUDS, "botanypots:soil/dirt");
        pot("sativa", ModItems.SATIVA_SEEDS, ModBlocks.SATIVA_CROP, ModItems.SATIVA_BUDS, "botanypots:soil/dirt");
        // Beldía is the desert's: sand, as in the field.
        pot("beldia", ModItems.BELDIA_SEEDS, ModBlocks.BELDIA_CROP, ModItems.BELDIA_BUDS, "botanypots:soil/sand");
    }

    private void pot(String name, ItemLike seeds, Block crop, ItemLike buds, String soil) {
        JsonObject recipe = conditioned(BOTANY_POTS, "botanypots:crop");
        recipe.add("input", item(seeds));
        recipe.add("soil", tag(soil));
        recipe.addProperty("grow_time", POT_GROW_TICKS);
        JsonObject display = new JsonObject();
        display.addProperty("type", "botanypots:aging");
        display.addProperty("block", id(crop));
        recipe.add("display", display);
        JsonArray items = new JsonArray();
        items.add(potDrop(buds, 1, POT_BUD_CHANCE));
        items.add(potDrop(ModItems.HEMP_STEM, 2, 1.0F));
        items.add(potDrop(seeds, 1, POT_SEED_CHANCE));
        JsonObject drops = new JsonObject();
        drops.addProperty("type", "botanypots:items");
        drops.add("items", items);
        recipe.add("drops", drops);
        write(BOTANY_POTS, "crop/" + name, recipe);
    }

    private static JsonObject potDrop(ItemLike item, int count, float chance) {
        JsonObject entry = new JsonObject();
        if (chance < 1.0F) {
            entry.addProperty("chance", chance);
        }
        entry.add("result", result(item, count));
        return entry;
    }

    // ----- Immersive Engineering -----

    private void immersiveEngineering() {
        // The Garden Cloche: the pot's harvest, in twice the time IE gives its own hemp. Its renders go no
        // taller than two blocks, so sativa is drawn as its lowest block growing.
        cloche("indica", ModItems.INDICA_SEEDS, ModItems.INDICA_BUDS, "minecraft:dirt",
                doubleCrop(ModBlocks.INDICA_CROP));
        cloche("sativa", ModItems.SATIVA_SEEDS, ModItems.SATIVA_BUDS, "minecraft:dirt", crop(ModBlocks.SATIVA_CROP));
        cloche("beldia", ModItems.BELDIA_SEEDS, ModItems.BELDIA_BUDS, "minecraft:sand",
                doubleCrop(ModBlocks.BELDIA_CROP));

        // The Squeezer: hemp seed oil, as much as IE pays its own hemp seed. A hemp kernel is about half fat,
        // which is why it beats wheat's 80.
        squeezer("indica", ModItems.INDICA_SEEDS);
        squeezer("sativa", ModItems.SATIVA_SEEDS);
        squeezer("beldia", ModItems.BELDIA_SEEDS);

        // The Crusher breaks and scutches a retted stalk as Create's crushing wheels do: 6 fibre and a half
        // chance of a seventh, short of the Hemp Press's 8. So a pack with IE and no Create has its fibre line.
        JsonObject crusher = conditioned(IMMERSIVE_ENGINEERING, "immersiveengineering:crusher");
        crusher.addProperty("energy", 1600);
        crusher.add("input", item(ModItems.RETTED_HEMP_STEM));
        JsonObject base = new JsonObject();
        base.add("basePredicate", item(ModItems.HEMP_FIBER));
        base.addProperty("count", 6);
        crusher.add("result", base);
        JsonArray secondaries = new JsonArray();
        JsonObject extra = new JsonObject();
        extra.addProperty("chance", 0.5F);
        extra.add("conditions", new JsonArray());
        extra.add("output", item(ModItems.HEMP_FIBER));
        secondaries.add(extra);
        crusher.add("secondaries", secondaries);
        write(IMMERSIVE_ENGINEERING, "crusher/hemp_fiber", crusher);
    }

    // ----- MoreMachine (Mekanism) -----

    private void moreMachine() {
        // The Planting Station spends its seed every run and gives one output and one chance at a
        // second. A bud, and the seed back four times in five, as it pays wheat: a little more bud than a
        // pot for its power and nutrient solution, and seeds to keep topping up. Never a leaf.
        planting("indica", ModItems.INDICA_SEEDS, ModItems.INDICA_BUDS);
        planting("sativa", ModItems.SATIVA_SEEDS, ModItems.SATIVA_BUDS);
        planting("beldia", ModItems.BELDIA_SEEDS, ModItems.BELDIA_BUDS);
    }

    private void planting(String name, ItemLike seeds, ItemLike buds) {
        JsonObject recipe = conditioned(MORE_MACHINE, "mekmm:planting");
        JsonObject input = item(seeds);
        input.addProperty("count", 1);
        recipe.add("item_input", input);
        JsonObject solution = new JsonObject();
        solution.addProperty("amount", 1);
        solution.addProperty("chemical", "mekmm:nutrient_solution");
        recipe.add("chemical_input", solution);
        JsonObject main = result(buds, 1);
        main.addProperty("count", 1);
        recipe.add("main_output", main);
        JsonObject seed = result(seeds, 1);
        seed.addProperty("count", 1);
        recipe.add("secondary_output", seed);
        recipe.addProperty("secondary_chance", 0.8F);
        recipe.addProperty("per_tick_usage", true);
        write(MORE_MACHINE, "planting/" + name, recipe);
    }

    // ----- Silent Gear -----

    private void silentGear() {
        // Hemp fibre as a gear material, beside Silent Gear's flax, string and sinew. Hemp cord is strong,
        // stretches little and wears well: as a cord it adds a little power and no draw speed (flax draws
        // faster and hits softer, sinew hits harder); as a binding it adds durability with less give than
        // flax. A sidegrade among them, not their better.
        JsonObject material = new JsonObject();
        material.addProperty("type", "silentgear:simple");
        material.addProperty("parent", "silentgear:empty");
        JsonObject crafting = new JsonObject();
        crafting.addProperty("can_salvage", true);
        JsonArray categories = new JsonArray();
        categories.add("organic");
        categories.add("fiber");
        categories.add("basic");
        crafting.add("categories", categories);
        crafting.add("gear_type_blacklist", new JsonArray());
        crafting.add("ingredient", item(ModItems.HEMP_FIBER));
        crafting.add("part_substitutes", new JsonObject());
        material.add("crafting", crafting);
        JsonObject display = new JsonObject();
        display.addProperty("color", "#FF988F77");
        display.addProperty("main_texture_type", "LOW_CONTRAST");
        JsonObject name = new JsonObject();
        name.addProperty("translate", "material.hempdustry.hemp");
        display.add("name", name);
        display.addProperty("name_prefix", "");
        material.add("display", display);
        JsonObject binding = new JsonObject();
        binding.add("armor_durability", silentGearModifier(0.1F));
        binding.add("traits", flexible(2));
        JsonObject cord = new JsonObject();
        cord.add("ranged_damage", silentGearModifier(0.1F));
        JsonObject properties = new JsonObject();
        properties.add("silentgear:binding", binding);
        properties.add("silentgear:cord", cord);
        material.add("properties", properties);
        Path file = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(Hempdustry.MOD_ID)
                .resolve("silentgear_materials").resolve("hemp.json");
        writes.add(DataProvider.saveStable(cache, material, file));
    }

    private static JsonObject silentGearModifier(float value) {
        JsonObject modifier = new JsonObject();
        modifier.addProperty("operation", "MULTIPLY_BASE");
        modifier.addProperty("value", value);
        return modifier;
    }

    private static JsonArray flexible(int level) {
        JsonObject trait = new JsonObject();
        trait.add("conditions", new JsonArray());
        trait.addProperty("level", level);
        trait.addProperty("trait", "silentgear:flexible");
        JsonArray traits = new JsonArray();
        traits.add(trait);
        return traits;
    }

    private void cloche(String name, ItemLike seeds, ItemLike buds, String soil, JsonObject render) {
        JsonObject recipe = conditioned(IMMERSIVE_ENGINEERING, "immersiveengineering:cloche");
        recipe.add("input", item(seeds));
        recipe.add("render", render);
        JsonArray results = new JsonArray();
        results.add(result(ModItems.HEMP_STEM, 2));
        results.add(clocheChance(buds, POT_BUD_CHANCE));
        results.add(clocheChance(seeds, POT_SEED_CHANCE));
        recipe.add("results", results);
        recipe.add("soil", item(soil));
        recipe.addProperty("time", CLOCHE_TICKS);
        write(IMMERSIVE_ENGINEERING, "cloche/" + name, recipe);
    }

    private static JsonObject clocheChance(ItemLike item, float chance) {
        JsonObject entry = new JsonObject();
        entry.addProperty("chance", chance);
        entry.add("output", result(item, 1));
        return entry;
    }

    private static JsonObject doubleCrop(Block block) {
        JsonObject render = new JsonObject();
        render.addProperty("type", "immersiveengineering:doublecrop");
        render.addProperty("block", id(block));
        // The age the plant first stands two blocks tall.
        render.addProperty("doublingAge", 4);
        return render;
    }

    private static JsonObject crop(Block block) {
        JsonObject render = new JsonObject();
        render.addProperty("type", "immersiveengineering:crop");
        render.addProperty("block", id(block));
        return render;
    }

    private void squeezer(String name, ItemLike seeds) {
        JsonObject recipe = conditioned(IMMERSIVE_ENGINEERING, "immersiveengineering:squeezer");
        recipe.addProperty("energy", 6400);
        JsonObject fluid = new JsonObject();
        fluid.addProperty("amount", SEED_OIL_MB);
        fluid.addProperty("id", "immersiveengineering:plantoil");
        recipe.add("fluid", fluid);
        recipe.add("input", item(seeds));
        write(IMMERSIVE_ENGINEERING, "squeezer/" + name + "_seeds", recipe);
    }

    // ----- the JSON -----

    /** A recipe of {@code type}, loaded only with {@code modId}, on either loader. */
    private static JsonObject conditioned(String modId, String type) {
        JsonObject recipe = new JsonObject();
        recipe.add("neoforge:conditions", neoForgeWaitsFor(modId));
        recipe.add("fabric:load_conditions", fabricWaitsFor(modId));
        recipe.addProperty("type", type);
        return recipe;
    }

    /**
     * A recipe of {@code type} for one loader only, Fabric's or NeoForge's, loaded there only with {@code modId}.
     * The other loader is turned off with a condition type of its own: each loader reads only its own key, and
     * a condition type it does not know would fail the file instead of skipping it.
     */
    private static JsonObject conditionedFor(boolean fabric, String modId, String type) {
        JsonObject recipe = new JsonObject();
        JsonArray neoOff = new JsonArray();
        JsonObject neoFalse = new JsonObject();
        neoFalse.addProperty("type", "neoforge:false");
        neoOff.add(neoFalse);
        JsonArray fabricOff = new JsonArray();
        JsonObject not = new JsonObject();
        not.addProperty("condition", "fabric:not");
        JsonObject fabricTrue = new JsonObject();
        fabricTrue.addProperty("condition", "fabric:true");
        not.add("value", fabricTrue);
        fabricOff.add(not);
        recipe.add("neoforge:conditions", fabric ? neoOff : neoForgeWaitsFor(modId));
        recipe.add("fabric:load_conditions", fabric ? fabricWaitsFor(modId) : fabricOff);
        recipe.addProperty("type", type);
        return recipe;
    }

    private static JsonArray neoForgeWaitsFor(String modId) {
        JsonArray neo = new JsonArray();
        JsonObject neoLoaded = new JsonObject();
        neoLoaded.addProperty("type", "neoforge:mod_loaded");
        neoLoaded.addProperty("modid", modId);
        neo.add(neoLoaded);
        return neo;
    }

    private static JsonArray fabricWaitsFor(String modId) {
        JsonArray fabric = new JsonArray();
        JsonObject fabricLoaded = new JsonObject();
        fabricLoaded.addProperty("condition", "fabric:all_mods_loaded");
        JsonArray values = new JsonArray();
        values.add(modId);
        fabricLoaded.add("values", values);
        fabric.add(fabricLoaded);
        return fabric;
    }

    private static JsonObject recipe(String modId, String type, JsonArray ingredients, JsonArray results) {
        JsonObject recipe = conditioned(modId, type);
        recipe.add("ingredients", ingredients);
        recipe.add("results", results);
        return recipe;
    }

    private static JsonArray ingredients(JsonObject... entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            array.add(entry);
        }
        return array;
    }

    private static JsonArray results(JsonObject... entries) {
        return ingredients(entries);
    }

    private static JsonObject item(Object item) {
        JsonObject entry = new JsonObject();
        entry.addProperty("item", id(item));
        return entry;
    }

    private static JsonObject tag(String tag) {
        JsonObject entry = new JsonObject();
        entry.addProperty("tag", tag);
        return entry;
    }

    private static JsonObject result(Object item, int count) {
        JsonObject entry = new JsonObject();
        if (count != 1) {
            entry.addProperty("count", count);
        }
        entry.addProperty("id", id(item));
        return entry;
    }

    private static JsonObject chance(JsonObject result, float chance) {
        result.addProperty("chance", chance);
        return result;
    }

    private static String id(Object item) {
        if (item instanceof String id) {
            return id;
        }
        if (item instanceof Block block) {
            return BuiltInRegistries.BLOCK.getKey(block).toString();
        }
        return BuiltInRegistries.ITEM.getKey(((ItemLike) item).asItem()).toString();
    }

    /**
     * Writes {@code recipe} as {@code hempdustry:compat/<mod>/<path>}, except Create's saw, which must sit at
     * Create's own id to replace the recipe Create generates.
     */
    private void write(String modId, String path, JsonElement recipe) {
        Path root = output.getOutputFolder(PackOutput.Target.DATA_PACK);
        Path file = path.startsWith("cutting/runtime_generated/")
                ? root.resolve(modId).resolve("recipe").resolve(path + ".json")
                : root.resolve(Hempdustry.MOD_ID).resolve("recipe").resolve("compat").resolve(modId).resolve(path + ".json");
        writes.add(DataProvider.saveStable(cache, recipe, file));
    }
}

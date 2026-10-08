package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.recipe.DecarboxylatingRecipe;
import com.warlonmhite.hempdustry.recipe.InfusingRecipe;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.recipe.PressingRecipe;
import com.warlonmhite.hempdustry.util.ModTags;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.RepairableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SingleStackRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Rules every piece of content must keep, checked across the whole registry.
 *
 * <p>Nearly every other test here was written after its own bug and checks the one thing that broke.
 * That stops the bug coming back, and says nothing about the next machine, tag or sound. These walk
 * the registries instead, so new content is held to the rule the day it registers — each rule is one
 * a shipped bug broke, named in its javadoc. What a sweep may not hold to its rule is listed beside
 * it with the reason ({@link Sweep#report}).
 */
public final class SweepGameTest {

    /** Where the block sweeps put each block, one at a time. */
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    /** How many of an ingredient's items the recipe sweeps try, so a wide tag does not run away. */
    private static final int CHOICES = 32;

    /**
     * Every recipe of ours is the only recipe its ingredients make — by hand, only one of two recipes
     * that take the same grid is ever made, and which one flips when any recipe is added.
     *
     * <p>{@code hempdustry:stick_from_planks} made six sticks from two hemp planks in the recipe book
     * and four by hand, on every line, until 2026-09-19; {@code recipe_collisions.py} compares tags as
     * opaque names and cannot see a special recipe at all. This asks the game: every recipe's own
     * ingredients, each choice of every tag tried, against every loaded recipe of its type, vanilla's
     * included. An ingredient nothing can satisfy is a finding too.
     */
    public static void noTwoRecipesTakeTheSameIngredients(TestContext context) {
        ServerWorld world = context.getWorld();
        Map<String, String> findings = new TreeMap<>();
        List<Sample> samples = samples(world, true, findings);
        Map<RecipeType<?>, List<RecipeEntry<?>>> byType = new HashMap<>();
        for (RecipeEntry<?> entry : world.getServer().getRecipeManager().values()) {
            byType.computeIfAbsent(entry.value().getType(), type -> new ArrayList<>()).add(entry);
        }

        for (Sample sample : samples) {
            RecipeType<?> type = sample.recipe().value().getType();
            // A stonecutter offers every recipe its input fits, by design.
            if (type == RecipeType.STONECUTTING) continue;
            boolean itself = false;
            List<String> others = new ArrayList<>();
            for (RecipeEntry<?> candidate : byType.get(type)) {
                if (matches(candidate.value(), sample.input(), world)) {
                    if (candidate == sample.recipe()) itself = true;
                    else others.add(candidate.id().getValue().toString());
                }
            }
            String id = sample.recipe().id().getValue().toString();
            if (!itself) {
                findings.putIfAbsent(id, "does not match its own ingredients: " + sample.describe());
            } else if (!others.isEmpty()) {
                findings.putIfAbsent(id, sample.describe() + " also makes " + others);
            }
        }
        Sweep.atLeast(context, "recipe inputs", samples.size(), 200);
        Sweep.report(context, "Every recipe of ours is the only one its ingredients make", findings, Map.of());
        context.complete();
    }

    /**
     * No recipe of ours makes anything different when a slot holds more than one item — a craft takes
     * one from every occupied slot, whatever the stack size.
     *
     * <p>{@code PackingRecipe} summed {@code getCount()}: three buds stacked in one slot packed a
     * level III bowl for one bud, and a Crafter could repeat it for ever (2.0.0 to 2.0.2).
     */
    public static void noRecipeReadsAStackSize(TestContext context) {
        ServerWorld world = context.getWorld();
        Map<String, String> findings = new TreeMap<>();
        int checked = 0;
        for (Sample sample : samples(world, false, new HashMap<>())) {
            RecipeInput stacked = stacked(sample.input());
            if (stacked == null) continue;
            checked++;
            Recipe<?> recipe = sample.recipe().value();
            String id = sample.recipe().id().getValue().toString();
            if (!matches(recipe, stacked, world)) {
                findings.putIfAbsent(id, "refuses " + sample.describe() + " once a slot holds two");
                continue;
            }
            ItemStack one = craft(recipe, sample.input(), world);
            ItemStack two = craft(recipe, stacked, world);
            if (!ItemStack.areEqual(one, two)) {
                findings.putIfAbsent(id, "makes " + describe(one) + " from " + sample.describe()
                        + " but " + describe(two) + " when a slot holds two");
            } else if (recipe instanceof CraftingRecipe crafting && !sameStacks(
                    crafting.getRecipeRemainders((CraftingRecipeInput) sample.input()),
                    crafting.getRecipeRemainders((CraftingRecipeInput) stacked))) {
                findings.putIfAbsent(id, "hands back different leftovers when a slot holds two");
            }
        }
        Sweep.atLeast(context, "recipes with a stackable ingredient", checked, 100);
        Sweep.report(context, "No recipe of ours reads a stack size", findings, Map.of());
        context.complete();
    }

    /**
     * Every block of ours that holds items drops all of them when it is broken.
     *
     * <p>Since 1.21.5 a block entity is gone before its block's {@code onStateReplaced} runs, so every
     * machine that spilled from the block found nothing: a broken simmering Infuser lost its batch for
     * the whole 1.21.11 line, until 2.0.3. A diamond named after its slot goes in every slot, and
     * every one must come out.
     */
    public static void everyContainerSpillsWhenBroken(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(AT);
        Map<String, String> findings = new TreeMap<>();
        int containers = 0;
        for (Block block : blocksWithEntities()) {
            world.setBlockState(pos, block.getDefaultState(), Block.NOTIFY_LISTENERS);
            if (!(world.getBlockEntity(pos) instanceof Inventory inventory)) {
                world.removeBlock(pos, false);
                take(world, pos);
                continue;
            }
            containers++;
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack marker = new ItemStack(Items.DIAMOND);
                marker.set(DataComponentTypes.CUSTOM_NAME, Text.literal("slot " + slot));
                inventory.setStack(slot, marker);
            }
            world.breakBlock(pos, true);
            List<ItemStack> dropped = take(world, pos);
            for (int slot = 0; slot < inventory.size(); slot++) {
                Text name = Text.literal("slot " + slot);
                if (dropped.stream().noneMatch(drop -> drop.isOf(Items.DIAMOND) && name.equals(drop.get(DataComponentTypes.CUSTOM_NAME)))) {
                    findings.put(Registries.BLOCK.getId(block) + " slot " + slot, "broken, it did not drop what was in this slot");
                }
            }
        }
        Sweep.atLeast(context, "blocks that hold items", containers, 3);
        Sweep.report(context, "Every block of ours that holds items drops them when broken", findings, Map.of(
                "hempdustry:infuser slot 3", "the output slot only ever holds a preview of the batch, which is "
                        + "not an item yet; a broken tub hands back the batch's hemp instead (discardPreview)"));
        context.complete();
    }

    /**
     * Every block entity of ours reads back exactly what it wrote, and a container of ours keeps the
     * name an anvil gave it — on its screen, through a save, and in the block it drops.
     *
     * <p>A field written and never read comes back empty from every save: the machine reloads without
     * it and nothing is logged. The name is {@code a_named_machine_keeps_its_name} held to every
     * container rather than to the Decarboxylator alone.
     */
    public static void everyBlockEntitySurvivesASave(TestContext context) {
        ServerWorld world = context.getWorld();
        DynamicRegistryManager registries = world.getRegistryManager();
        BlockPos pos = context.getAbsolutePos(AT);
        Map<String, String> findings = new TreeMap<>();
        int checked = 0;
        for (Block block : blocksWithEntities()) {
            String id = Registries.BLOCK.getId(block).toString();
            BlockState state = block.getDefaultState();
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity == null) {
                findings.put(id, "placed, it has no block entity");
                continue;
            }
            checked++;
            if (entity instanceof Inventory inventory) {
                for (int slot = 0; slot < inventory.size(); slot++) inventory.setStack(slot, new ItemStack(Items.DIAMOND, slot + 1));
            }
            ItemStack named = new ItemStack(block.asItem());
            if (!named.isEmpty()) {
                named.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Swept"));
                entity.readComponents(named);
            }
            NbtCompound saved = entity.createNbtWithIdentifyingData(registries);
            BlockEntity loaded = BlockEntity.createFromNbt(pos, state, saved, registries);
            if (loaded == null) {
                findings.put(id, "does not load from its own save: " + saved);
            } else if (!saved.equals(loaded.createNbtWithIdentifyingData(registries))) {
                findings.put(id, "saved " + saved + " and, loaded, saves " + loaded.createNbtWithIdentifyingData(registries));
            } else if (entity instanceof Inventory && Sweep.ours(Registries.BLOCK_ENTITY_TYPE.getId(entity.getType()))
                    && Block.getDroppedStacks(state, world, pos, loaded).stream().noneMatch(
                            drop -> drop.isOf(block.asItem()) && Text.literal("Swept").equals(drop.get(DataComponentTypes.CUSTOM_NAME)))) {
                findings.put(id, "loses its anvil name through a save and a break");
            }
            world.removeBlock(pos, false);
            take(world, pos);
        }
        Sweep.atLeast(context, "blocks with a block entity", checked, 5);
        Sweep.report(context, "Every block entity of ours survives a save and a load", findings, Map.of());
        context.complete();
    }

    /**
     * Every stack the creative tab and our recipes build comes back identical from a save (NBT), a
     * datapack or command (JSON) and the network; and every entry of our dynamic registries from its
     * codec.
     *
     * <p>A data component whose codec cannot read what it wrote does worse than lose itself: the
     * components of a stack decode as one map, so one bad value takes the whole stack, and a chest
     * reloads with the slot empty and nothing logged.
     */
    public static void everyStackSurvivesASaveAndTheWire(TestContext context) {
        ServerWorld world = context.getWorld();
        DynamicRegistryManager registries = world.getRegistryManager();
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.updateEntries(new ItemGroup.DisplayContext(
                world.getEnabledFeatures(), true, registries));
        List<ItemStack> stacks = new ArrayList<>(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayStacks());
        stacks.addAll(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabStacks());
        for (Sample sample : samples(world, false, new HashMap<>())) {
            stacks.add(craft(sample.recipe().value(), sample.input(), world));
        }

        Map<String, String> findings = new TreeMap<>();
        DynamicOps<NbtElement> nbt = registries.getOps(NbtOps.INSTANCE);
        DynamicOps<JsonElement> json = registries.getOps(JsonOps.INSTANCE);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            String nbtProblem = roundTrip(ItemStack.CODEC, nbt, stack, ItemStack::areEqual);
            String jsonProblem = roundTrip(ItemStack.CODEC, json, stack, ItemStack::areEqual);
            RegistryByteBuf buffer = new RegistryByteBuf(Unpooled.buffer(), registries);
            ItemStack.PACKET_CODEC.encode(buffer, stack);
            ItemStack sent = ItemStack.PACKET_CODEC.decode(buffer);
            buffer.release();
            if (nbtProblem != null) findings.putIfAbsent(id, describe(stack) + " in a save: " + nbtProblem);
            else if (jsonProblem != null) findings.putIfAbsent(id, describe(stack) + " as JSON: " + jsonProblem);
            else if (!ItemStack.areEqual(stack, sent)) findings.putIfAbsent(id, describe(stack) + " arrives as " + describe(sent));
        }

        // Every dynamic registry with the codec its files are read with (Fabric's list: vanilla's never
        // holds a mod's), then every synced one with the codec it is sent to a client with.
        int dynamic = 0;
        for (List<RegistryLoader.Entry<?>> list : List.of(DynamicRegistries.getDynamicRegistries(), RegistryLoader.SYNCED_REGISTRIES)) {
            for (RegistryLoader.Entry<?> entry : list) {
                if (Sweep.ours(entry.key().getValue())) dynamic += roundTripRegistry(entry, registries, json, findings);
            }
        }
        Sweep.atLeast(context, "stacks", stacks.size(), 150);
        Sweep.atLeast(context, "entries of our dynamic registries", dynamic, 5);
        Sweep.report(context, "Every stack and registry entry of ours reads back what it wrote", findings, Map.of());
        context.complete();
    }

    /**
     * Every tag the mod names in code has something in it, and so does every repair material.
     *
     * <p>The 1.21.11 port named {@code #hempdustry:hemp_armor_repair} as the outfit's repair material
     * and no provider ever wrote it, so no anvil repaired the outfit and nothing logged, until
     * 2.0.3. A tag the code asks about and nobody fills behaves exactly like a feature that is off.
     */
    public static void everyTagWeNameHasSomethingInIt(TestContext context) {
        DynamicRegistryManager registries = context.getWorld().getRegistryManager();
        Map<String, String> findings = new TreeMap<>();
        int tags = 0;
        for (Class<?> holder : ModTags.class.getDeclaredClasses()) {
            for (Field field : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !TagKey.class.isAssignableFrom(field.getType())) continue;
                TagKey<?> tag = tagIn(field);
                tags++;
                if (size(registries, tag) == 0) {
                    findings.put("#" + tag.id(), "ModTags." + holder.getSimpleName() + "." + field.getName() + " resolves to nothing");
                }
            }
        }
        for (Item item : Registries.ITEM) {
            RepairableComponent repair = item.getComponents().get(DataComponentTypes.REPAIRABLE);
            if (Sweep.ours(Registries.ITEM.getId(item)) && repair != null && repair.items().size() == 0) {
                findings.put(Registries.ITEM.getId(item).toString(), "repairs with " + repair.items() + ", which holds nothing");
            }
        }
        Sweep.atLeast(context, "tags in ModTags", tags, 20);
        Sweep.report(context, "Every tag the mod names has something in it", findings, Map.of());
        context.complete();
    }

    /**
     * Every item of ours that can be enchanted takes some enchantment, and every one that wears out
     * takes Unbreaking and Mending.
     *
     * <p>The Redstone Vaporizer had an enchantability of 5 and was in no {@code #enchantable/*} tag,
     * so Unbreaking and Mending refused it: an enchantable item no enchantment accepts.
     */
    public static void everyEnchantableItemTakesAnEnchantment(TestContext context) {
        Registry<Enchantment> enchantments = context.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Enchantment unbreaking = enchantments.getValueOrThrow(Enchantments.UNBREAKING);
        Enchantment mending = enchantments.getValueOrThrow(Enchantments.MENDING);
        Map<String, String> findings = new TreeMap<>();
        int checked = 0;
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            ItemStack stack = new ItemStack(item);
            if (!Sweep.ours(id) || !stack.contains(DataComponentTypes.ENCHANTABLE)) continue;
            checked++;
            if (stack.isDamageable() && !(unbreaking.isAcceptableItem(stack) && mending.isAcceptableItem(stack))) {
                findings.put(id.toString(), "wears out, but Unbreaking "
                        + (unbreaking.isAcceptableItem(stack) ? "takes" : "refuses") + " it and Mending "
                        + (mending.isAcceptableItem(stack) ? "takes" : "refuses") + " it");
            } else if (enchantments.stream().noneMatch(enchantment -> enchantment.isAcceptableItem(stack))) {
                findings.put(id.toString(), "is enchantable, and no enchantment accepts it");
            }
        }
        Sweep.atLeast(context, "enchantable items", checked, 10);
        Sweep.report(context, "Every enchantable item of ours takes an enchantment", findings, Map.of());
        context.complete();
    }

    /**
     * Every sound of ours is mono, every file {@code sounds.json} names is there, every sound file is
     * named, and every sound event the mod registers has an entry.
     *
     * <p>Minecraft spatializes mono sounds only: {@code bonghit.ogg} and {@code smoking.ogg} were
     * stereo, so every player in range heard each hit at full volume as if it were their own, until
     * 2.0.3. The channel count is read from the Ogg Vorbis identification header.
     */
    public static void everySoundIsMonoAndOnDisk(TestContext context) {
        Map<String, java.nio.file.Path> files = Sweep.modFiles();
        String prefix = "assets/hempdustry/sounds/";
        JsonObject sounds = Sweep.json(files.get("assets/hempdustry/sounds.json")).getAsJsonObject();
        Map<String, String> findings = new TreeMap<>();
        Map<String, Boolean> named = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> event : sounds.entrySet()) {
            for (JsonElement sound : event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                JsonObject spec = sound.isJsonObject() ? sound.getAsJsonObject() : null;
                if (spec != null && "event".equals(spec.has("type") ? spec.get("type").getAsString() : "file")) continue;
                Identifier file = Identifier.of(spec != null ? spec.get("name").getAsString() : sound.getAsString());
                if (Sweep.ours(file)) named.put(prefix + file.getPath() + ".ogg", true);
            }
        }
        for (String file : named.keySet()) {
            if (!files.containsKey(file)) {
                findings.put(file, "sounds.json names it and it is not there");
                continue;
            }
            int channels = channels(Sweep.bytes(files.get(file)));
            if (channels != 1) findings.put(file, "has " + channels + " channels, so it has no position");
        }
        for (String file : files.keySet()) {
            if (file.startsWith(prefix) && file.endsWith(".ogg") && !named.containsKey(file)) {
                findings.put(file, "is shipped and no sound event plays it");
            }
        }
        for (Identifier event : Registries.SOUND_EVENT.getIds()) {
            if (Sweep.ours(event) && !sounds.has(event.getPath())) {
                findings.put(event.toString(), "is registered and sounds.json has no entry for it, so it plays nothing");
            }
        }
        Sweep.atLeast(context, "sound files", named.size(), 5);
        Sweep.report(context, "Every sound of ours is mono and on disk", findings, Map.of());
        context.complete();
    }

    // ----- recipes -----

    private record Sample(RecipeEntry<?> recipe, RecipeInput input) {
        String describe() {
            List<String> items = new ArrayList<>();
            for (int i = 0; i < input.size(); i++) {
                if (!input.getStackInSlot(i).isEmpty()) items.add(Registries.ITEM.getId(input.getStackInSlot(i).getItem()).toString());
            }
            return items.toString();
        }
    }

    /**
     * An input every recipe of ours accepts, built from its own ingredients — with {@code everyChoice},
     * one more for each other item a slot's ingredient takes. A recipe the sweep cannot build an input
     * for is a finding, so a new kind of recipe fails here until the sweep is taught it.
     */
    private static List<Sample> samples(ServerWorld world, boolean everyChoice, Map<String, String> findings) {
        List<Sample> samples = new ArrayList<>();
        for (RecipeEntry<?> entry : world.getServer().getRecipeManager().values()) {
            Identifier id = entry.id().getValue();
            if (!Sweep.ours(id)) continue;
            Recipe<?> recipe = entry.value();
            List<List<ItemStack>> slots = new ArrayList<>();
            if (recipe instanceof ShapedRecipe shaped) {
                for (Optional<Ingredient> ingredient : shaped.getIngredients()) {
                    slots.add(ingredient.map(SweepGameTest::choices).orElse(List.of(ItemStack.EMPTY)));
                }
                grids(entry, shaped.getWidth(), shaped.getHeight(), slots, everyChoice, samples, findings);
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                for (Ingredient ingredient : shapeless.getIngredientPlacement().getIngredients()) slots.add(choices(ingredient));
                grids(entry, 3, 3, slots, everyChoice, samples, findings);
            } else if (recipe instanceof PackingRecipe) {
                // A special recipe lists no ingredients; the rows the recipe viewers draw are every load it packs.
                for (ViewerRecipes.Packing row : ViewerRecipes.packing(world.getRegistryManager())) {
                    grids(entry, 3, 3, new ArrayList<>(row.inputs()), everyChoice, samples, findings);
                }
            } else if (recipe instanceof SingleStackRecipe || recipe instanceof DecarboxylatingRecipe
                    || recipe instanceof PressingRecipe || recipe instanceof InfusingRecipe) {
                for (Ingredient ingredient : recipe.getIngredientPlacement().getIngredients()) {
                    List<ItemStack> choices = choices(ingredient);
                    if (choices.isEmpty()) findings.putIfAbsent(id.toString(), "has an ingredient nothing satisfies");
                    for (ItemStack choice : everyChoice ? choices : choices.subList(0, Math.min(1, choices.size()))) {
                        samples.add(new Sample(entry, new SingleStackRecipeInput(choice)));
                    }
                    if (!everyChoice) break;
                }
            } else {
                findings.put(id.toString(), recipe.getClass().getSimpleName()
                        + " is a kind of recipe the sweep cannot build an input for: teach samples() its shape");
            }
        }
        return samples;
    }

    private static List<ItemStack> choices(Ingredient ingredient) {
        return ingredient.getMatchingItems().limit(CHOICES).map(ItemStack::new).toList();
    }

    /** The grid made of each slot's first choice, and with {@code everyChoice} a grid per other choice. */
    private static void grids(RecipeEntry<?> entry, int width, int height, List<List<ItemStack>> slots,
                              boolean everyChoice, List<Sample> into, Map<String, String> findings) {
        while (slots.size() < width * height) slots.add(List.of(ItemStack.EMPTY));
        List<ItemStack> first = new ArrayList<>();
        for (List<ItemStack> slot : slots) {
            if (slot.isEmpty()) {
                findings.putIfAbsent(entry.id().getValue().toString(), "has an ingredient nothing satisfies");
                return;
            }
            first.add(slot.getFirst());
        }
        into.add(new Sample(entry, CraftingRecipeInput.create(width, height, first)));
        if (!everyChoice) return;
        for (int i = 0; i < slots.size(); i++) {
            for (ItemStack other : slots.get(i).subList(1, slots.get(i).size())) {
                List<ItemStack> grid = new ArrayList<>(first);
                grid.set(i, other);
                into.add(new Sample(entry, CraftingRecipeInput.create(width, height, grid)));
            }
        }
    }

    /** The same input with every stackable item doubled, or null if nothing in it stacks. */
    private static RecipeInput stacked(RecipeInput input) {
        if (input instanceof SingleStackRecipeInput single) {
            return single.item().getMaxCount() > 1 ? new SingleStackRecipeInput(single.item().copyWithCount(2)) : null;
        }
        CraftingRecipeInput grid = (CraftingRecipeInput) input;
        boolean any = false;
        List<ItemStack> doubled = new ArrayList<>();
        for (ItemStack stack : grid.getStacks()) {
            boolean grows = !stack.isEmpty() && stack.getMaxCount() > 1;
            any |= grows;
            doubled.add(grows ? stack.copyWithCount(2) : stack);
        }
        return any ? CraftingRecipeInput.create(grid.getWidth(), grid.getHeight(), doubled) : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean matches(Recipe<?> recipe, RecipeInput input, ServerWorld world) {
        return ((Recipe) recipe).matches(input, world);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack craft(Recipe<?> recipe, RecipeInput input, ServerWorld world) {
        return ((Recipe) recipe).craft(input, world.getRegistryManager());
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!ItemStack.areEqual(a.get(i), b.get(i))) return false;
        return true;
    }

    // ----- blocks -----

    /** Every block of ours whose default state has a block entity, the ones that reuse vanilla's included. */
    private static List<Block> blocksWithEntities() {
        List<Block> blocks = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            if (Sweep.ours(Registries.BLOCK.getId(block)) && block.getDefaultState().hasBlockEntity()) blocks.add(block);
        }
        return blocks;
    }

    /** Every stack lying on the ground around {@code pos}, picked up so the next block starts clean. */
    private static List<ItemStack> take(ServerWorld world, BlockPos pos) {
        List<ItemStack> found = new ArrayList<>();
        for (ItemEntity drop : world.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(3), entity -> true)) {
            found.add(drop.getStack());
            drop.discard();
        }
        return found;
    }

    // ----- codecs -----

    /** What is wrong with {@code value} after an encode and a decode through {@code codec}, or null. */
    private static <T, F> String roundTrip(Codec<T> codec, DynamicOps<F> ops, T value, java.util.function.BiPredicate<T, T> same) {
        DataResult<F> encoded = codec.encodeStart(ops, value);
        if (encoded.isError()) return "does not encode: " + encoded.error().orElseThrow().message();
        DataResult<T> decoded = codec.parse(ops, encoded.getOrThrow());
        if (decoded.isError()) return "does not decode what it wrote: " + decoded.error().orElseThrow().message();
        return same.test(value, decoded.getOrThrow()) ? null : "comes back different: " + encoded.getOrThrow();
    }

    /** Round-trips every entry of one of our dynamic registries through its codec; returns how many. */
    private static <T> int roundTripRegistry(RegistryLoader.Entry<T> entry, DynamicRegistryManager registries,
                                             DynamicOps<JsonElement> json, Map<String, String> findings) {
        Registry<T> registry = registries.getOrThrow(entry.key());
        Codec<T> codec = entry.elementCodec();
        for (Map.Entry<RegistryKey<T>, T> element : registry.getEntrySet()) {
            // Compared as what they encode to: an entry need not have a value-based equals.
            DataResult<JsonElement> first = codec.encodeStart(json, element.getValue());
            String problem = first.isError() ? "does not encode: " + first.error().orElseThrow().message()
                    : codec.parse(json, first.getOrThrow()).flatMap(back -> codec.encodeStart(json, back))
                    .mapOrElse(again -> again.equals(first.getOrThrow()) ? null : "comes back as " + again,
                            error -> "does not decode what it wrote: " + error.message());
            if (problem != null) findings.put(element.getKey().getValue() + " (" + entry.key().getValue() + ")", problem);
        }
        return registry.getEntrySet().size();
    }

    private static TagKey<?> tagIn(Field field) {
        try {
            field.setAccessible(true);
            return (TagKey<?>) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> int size(DynamicRegistryManager registries, TagKey<T> tag) {
        return registries.getOrThrow(tag.registryRef()).getOptional(tag).map(list -> list.size()).orElse(0);
    }

    private static String describe(ItemStack stack) {
        return stack.getCount() + " " + Registries.ITEM.getId(stack.getItem()) + stack.getComponentChanges();
    }

    /** The channel count in an Ogg Vorbis file's identification header, or -1 if there is none. */
    private static int channels(byte[] ogg) {
        for (int i = 0; i + 11 < ogg.length && i < 512; i++) {
            if (ogg[i] == 1 && ogg[i + 1] == 'v' && ogg[i + 2] == 'o' && ogg[i + 3] == 'r' && ogg[i + 4] == 'b'
                    && ogg[i + 5] == 'i' && ogg[i + 6] == 's') {
                return ogg[i + 11];
            }
        }
        return -1;
    }
}

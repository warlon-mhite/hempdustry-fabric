package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.block.custom.HempGrowth;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.DecarboxylatingRecipe;
import com.warlonmhite.hempdustry.recipe.InfusingRecipe;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.recipe.PressingRecipe;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** Where the state sweep works, clear of the test's own structure block. */
    private static final BlockPos FUZZ_AT = new BlockPos(4, 1, 4);

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
    public static void noTwoRecipesTakeTheSameIngredients(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Map<String, String> findings = new TreeMap<>();
        List<Sample> samples = samples(world, true, findings);
        Map<RecipeType<?>, List<RecipeHolder<?>>> byType = new HashMap<>();
        for (RecipeHolder<?> entry : world.getServer().getRecipeManager().getRecipes()) {
            byType.computeIfAbsent(entry.value().getType(), type -> new ArrayList<>()).add(entry);
        }

        for (Sample sample : samples) {
            RecipeType<?> type = sample.recipe().value().getType();
            // A stonecutter offers every recipe its input fits, by design.
            if (type == RecipeType.STONECUTTING) continue;
            boolean itself = false;
            List<String> others = new ArrayList<>();
            for (RecipeHolder<?> candidate : byType.get(type)) {
                if (matches(candidate.value(), sample.input(), world)) {
                    if (candidate == sample.recipe()) itself = true;
                    else others.add(candidate.id().identifier().toString());
                }
            }
            String id = sample.recipe().id().identifier().toString();
            if (!itself) {
                findings.putIfAbsent(id, "does not match its own ingredients: " + sample.describe());
            } else if (!others.isEmpty()) {
                findings.putIfAbsent(id, sample.describe() + " also makes " + others);
            }
        }
        Sweep.atLeast(context, "recipe inputs", samples.size(), 200);
        Sweep.report(context, "Every recipe of ours is the only one its ingredients make", findings, Map.of());
        context.succeed();
    }

    /**
     * No recipe of ours makes anything different when a slot holds more than one item — a craft takes
     * one from every occupied slot, whatever the stack size.
     *
     * <p>{@code PackingRecipe} summed {@code getCount()}: three buds stacked in one slot packed a
     * level III bowl for one bud, and a Crafter could repeat it for ever (2.0.0 to 2.0.2).
     */
    public static void noRecipeReadsAStackSize(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Map<String, String> findings = new TreeMap<>();
        int checked = 0;
        for (Sample sample : samples(world, false, new HashMap<>())) {
            RecipeInput stacked = stacked(sample.input());
            if (stacked == null) continue;
            checked++;
            Recipe<?> recipe = sample.recipe().value();
            String id = sample.recipe().id().identifier().toString();
            if (!matches(recipe, stacked, world)) {
                findings.putIfAbsent(id, "refuses " + sample.describe() + " once a slot holds two");
                continue;
            }
            ItemStack one = craft(recipe, sample.input(), world);
            ItemStack two = craft(recipe, stacked, world);
            if (!ItemStack.matches(one, two)) {
                findings.putIfAbsent(id, "makes " + describe(one) + " from " + sample.describe()
                        + " but " + describe(two) + " when a slot holds two");
            } else if (recipe instanceof CraftingRecipe crafting && !sameStacks(
                    crafting.getRemainingItems((CraftingInput) sample.input()),
                    crafting.getRemainingItems((CraftingInput) stacked))) {
                findings.putIfAbsent(id, "hands back different leftovers when a slot holds two");
            }
        }
        Sweep.atLeast(context, "recipes with a stackable ingredient", checked, 100);
        Sweep.report(context, "No recipe of ours reads a stack size", findings, Map.of());
        context.succeed();
    }

    /**
     * Every block of ours that holds items drops all of them when it is broken.
     *
     * <p>Since 1.21.5 a block entity is gone before its block's {@code affectNeighborsAfterRemoval} runs, so
     * every machine that spilled from the block found nothing: a broken simmering Infuser lost its batch for
     * the whole 1.21.11 line, until 2.0.3. A diamond named after its slot goes in every slot, and
     * every one must come out.
     */
    public static void everyContainerSpillsWhenBroken(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(AT);
        Map<String, String> findings = new TreeMap<>();
        int containers = 0;
        for (Block block : blocksWithEntities()) {
            world.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (!(world.getBlockEntity(pos) instanceof Container inventory)) {
                world.removeBlock(pos, false);
                take(world, pos);
                continue;
            }
            containers++;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack marker = new ItemStack(Items.DIAMOND);
                marker.set(DataComponents.CUSTOM_NAME, Component.literal("slot " + slot));
                inventory.setItem(slot, marker);
            }
            world.destroyBlock(pos, true);
            List<ItemStack> dropped = take(world, pos);
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                Component name = Component.literal("slot " + slot);
                if (dropped.stream().noneMatch(drop -> drop.is(Items.DIAMOND) && name.equals(drop.get(DataComponents.CUSTOM_NAME)))) {
                    findings.put(BuiltInRegistries.BLOCK.getKey(block) + " slot " + slot, "broken, it did not drop what was in this slot");
                }
            }
        }
        Sweep.atLeast(context, "blocks that hold items", containers, 3);
        Sweep.report(context, "Every block of ours that holds items drops them when broken", findings, Map.of(
                "hempdustry:infuser slot 3", "the output slot only ever holds a preview of the batch, which is "
                        + "not an item yet; a broken tub hands back the batch's hemp instead (discardPreview)"));
        context.succeed();
    }

    /**
     * Every block entity of ours reads back exactly what it wrote, and a container of ours keeps the
     * name an anvil gave it — on its screen, through a save, and in the block it drops.
     *
     * <p>A field written and never read comes back empty from every save: the machine reloads without
     * it and nothing is logged. The name is {@code a_named_machine_keeps_its_name} held to every
     * container rather than to the Decarboxylator alone.
     */
    public static void everyBlockEntitySurvivesASave(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        RegistryAccess registries = world.registryAccess();
        BlockPos pos = context.absolutePos(AT);
        Map<String, String> findings = new TreeMap<>();
        int checked = 0;
        for (Block block : blocksWithEntities()) {
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            BlockState state = block.defaultBlockState();
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity == null) {
                findings.put(id, "placed, it has no block entity");
                continue;
            }
            checked++;
            if (entity instanceof Container inventory) {
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) inventory.setItem(slot, new ItemStack(Items.DIAMOND, slot + 1));
            }
            ItemStack named = new ItemStack(block.asItem());
            if (!named.isEmpty()) {
                named.set(DataComponents.CUSTOM_NAME, Component.literal("Swept"));
                entity.applyComponentsFromItemStack(named);
            }
            CompoundTag saved = entity.saveWithFullMetadata(registries);
            BlockEntity loaded = BlockEntity.loadStatic(pos, state, saved, registries);
            if (loaded == null) {
                findings.put(id, "does not load from its own save: " + saved);
            } else if (!saved.equals(loaded.saveWithFullMetadata(registries))) {
                findings.put(id, "saved " + saved + " and, loaded, saves " + loaded.saveWithFullMetadata(registries));
            } else if (entity instanceof Container && Sweep.ours(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()))
                    && Block.getDrops(state, world, pos, loaded).stream().noneMatch(
                            drop -> drop.is(block.asItem()) && Component.literal("Swept").equals(drop.get(DataComponents.CUSTOM_NAME)))) {
                findings.put(id, "loses its anvil name through a save and a break");
            }
            world.removeBlock(pos, false);
            take(world, pos);
        }
        Sweep.atLeast(context, "blocks with a block entity", checked, 5);
        Sweep.report(context, "Every block entity of ours survives a save and a load", findings, Map.of());
        context.succeed();
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
    public static void everyStackSurvivesASaveAndTheWire(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        RegistryAccess registries = world.registryAccess();
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.buildContents(new CreativeModeTab.ItemDisplayParameters(
                world.enabledFeatures(), true, registries));
        List<ItemStack> stacks = new ArrayList<>(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayItems());
        stacks.addAll(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabDisplayItems());
        for (Sample sample : samples(world, false, new HashMap<>())) {
            stacks.add(craft(sample.recipe().value(), sample.input(), world));
        }

        Map<String, String> findings = new TreeMap<>();
        DynamicOps<Tag> nbt = registries.createSerializationContext(NbtOps.INSTANCE);
        DynamicOps<JsonElement> json = registries.createSerializationContext(JsonOps.INSTANCE);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            String nbtProblem = roundTrip(ItemStack.CODEC, nbt, stack, ItemStack::matches);
            String jsonProblem = roundTrip(ItemStack.CODEC, json, stack, ItemStack::matches);
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            ItemStack sent = ItemStack.STREAM_CODEC.decode(buffer);
            buffer.release();
            if (nbtProblem != null) findings.putIfAbsent(id, describe(stack) + " in a save: " + nbtProblem);
            else if (jsonProblem != null) findings.putIfAbsent(id, describe(stack) + " as JSON: " + jsonProblem);
            else if (!ItemStack.matches(stack, sent)) findings.putIfAbsent(id, describe(stack) + " arrives as " + describe(sent));
        }

        // Every dynamic registry with the codec its files are read with (Fabric's list: vanilla's never
        // holds a mod's), then every synced one with the codec it is sent to a client with.
        int dynamic = 0;
        for (List<RegistryDataLoader.RegistryData<?>> list : List.of(TestPlatform.INSTANCE.dynamicRegistries(), RegistryDataLoader.SYNCHRONIZED_REGISTRIES)) {
            for (RegistryDataLoader.RegistryData<?> entry : list) {
                if (Sweep.ours(entry.key().identifier())) dynamic += roundTripRegistry(entry, registries, json, findings);
            }
        }
        Sweep.atLeast(context, "stacks", stacks.size(), 150);
        Sweep.atLeast(context, "entries of our dynamic registries", dynamic, 5);
        Sweep.report(context, "Every stack and registry entry of ours reads back what it wrote", findings, Map.of());
        context.succeed();
    }

    /**
     * Every tag the mod names in code has something in it, and so does every repair material.
     *
     * <p>The 1.21.11 port named {@code #hempdustry:hemp_armor_repair} as the outfit's repair material
     * and no provider ever wrote it, so no anvil repaired the outfit and nothing logged, until
     * 2.0.3. A tag the code asks about and nobody fills behaves exactly like a feature that is off.
     */
    public static void everyTagWeNameHasSomethingInIt(GameTestHelper context) {
        RegistryAccess registries = context.getLevel().registryAccess();
        Map<String, String> findings = new TreeMap<>();
        int tags = 0;
        for (Class<?> holder : ModTags.class.getDeclaredClasses()) {
            for (Field field : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !TagKey.class.isAssignableFrom(field.getType())) continue;
                TagKey<?> tag = tagIn(field);
                tags++;
                if (size(registries, tag) == 0) {
                    findings.put("#" + tag.location(), "ModTags." + holder.getSimpleName() + "." + field.getName() + " resolves to nothing");
                }
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            Repairable repair = item.components().get(DataComponents.REPAIRABLE);
            if (Sweep.ours(BuiltInRegistries.ITEM.getKey(item)) && repair != null && repair.items().size() == 0) {
                findings.put(BuiltInRegistries.ITEM.getKey(item).toString(), "repairs with " + repair.items() + ", which holds nothing");
            }
        }
        Sweep.atLeast(context, "tags in ModTags", tags, 20);
        Sweep.report(context, "Every tag the mod names has something in it", findings, Map.of());
        context.succeed();
    }

    /**
     * Every item of ours that can be enchanted takes some enchantment, and every one that wears out
     * takes Unbreaking and Mending.
     *
     * <p>The Redstone Vaporizer had an enchantability of 5 and was in no {@code #enchantable/*} tag,
     * so Unbreaking and Mending refused it: an enchantable item no enchantment accepts.
     */
    public static void everyEnchantableItemTakesAnEnchantment(GameTestHelper context) {
        Registry<Enchantment> enchantments = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Enchantment unbreaking = enchantments.getValueOrThrow(Enchantments.UNBREAKING);
        Enchantment mending = enchantments.getValueOrThrow(Enchantments.MENDING);
        Map<String, String> findings = new TreeMap<>();
        int checked = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = new ItemStack(item);
            if (!Sweep.ours(id) || !stack.has(DataComponents.ENCHANTABLE)) continue;
            checked++;
            if (stack.isDamageableItem() && !(unbreaking.canEnchant(stack) && mending.canEnchant(stack))) {
                findings.put(id.toString(), "wears out, but Unbreaking "
                        + (unbreaking.canEnchant(stack) ? "takes" : "refuses") + " it and Mending "
                        + (mending.canEnchant(stack) ? "takes" : "refuses") + " it");
            } else if (enchantments.stream().noneMatch(enchantment -> enchantment.canEnchant(stack))) {
                findings.put(id.toString(), "is enchantable, and no enchantment accepts it");
            }
        }
        Sweep.atLeast(context, "enchantable items", checked, 10);
        Sweep.report(context, "Every enchantable item of ours takes an enchantment", findings, Map.of());
        context.succeed();
    }

    /**
     * Every sound of ours is mono, every file {@code sounds.json} names is there, every sound file is
     * named, and every sound event the mod registers has an entry.
     *
     * <p>Minecraft spatializes mono sounds only: {@code bonghit.ogg} and {@code smoking.ogg} were
     * stereo, so every player in range heard each hit at full volume as if it were their own, until
     * 2.0.3. The channel count is read from the Ogg Vorbis identification header.
     */
    public static void everySoundIsMonoAndOnDisk(GameTestHelper context) {
        Map<String, java.nio.file.Path> files = Sweep.modFiles();
        String prefix = "assets/hempdustry/sounds/";
        JsonObject sounds = Sweep.json(files.get("assets/hempdustry/sounds.json")).getAsJsonObject();
        Map<String, String> findings = new TreeMap<>();
        Map<String, Boolean> named = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> event : sounds.entrySet()) {
            for (JsonElement sound : event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                JsonObject spec = sound.isJsonObject() ? sound.getAsJsonObject() : null;
                if (spec != null && "event".equals(spec.has("type") ? spec.get("type").getAsString() : "file")) continue;
                Identifier file = Identifier.parse(spec != null ? spec.get("name").getAsString() : sound.getAsString());
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
        for (Identifier event : BuiltInRegistries.SOUND_EVENT.keySet()) {
            if (Sweep.ours(event) && !sounds.has(event.getPath())) {
                findings.put(event.toString(), "is registered and sounds.json has no entry for it, so it plays nothing");
            }
        }
        Sweep.atLeast(context, "sound files", named.size(), 5);
        Sweep.report(context, "Every sound of ours is mono and on disk", findings, Map.of());
        context.succeed();
    }

    /**
     * Every state of every block of ours survives what the world does to a block — a random tick, a
     * scheduled tick, a neighbour update, its shapes, its comparator, pick-block, its loot by hand,
     * with a pickaxe and with shears — and a break, without throwing.
     *
     * <p>An exception in a block's tick takes the server down with it, and one in its loot, shape or
     * pick-block code reaches a player the first time they meet that state. The crops alone have
     * hundreds of states, most of which no test of their own ever builds.
     */
    public static void everyBlockStateSurvivesATickAndABreak(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(FUZZ_AT);
        RandomSource random = RandomSource.create(20261008L);
        List<ItemStack> tools = List.of(ItemStack.EMPTY, new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.SHEARS));
        Map<String, String> findings = new TreeMap<>();
        int states = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!Sweep.ours(id)) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                states++;
                try {
                    world.setBlock(pos, state, Block.UPDATE_CLIENTS);
                    if (world.getBlockState(pos).isRandomlyTicking()) world.getBlockState(pos).randomTick(world, pos, random);
                    world.getBlockState(pos).tick(world, pos, random);
                    world.getBlockState(pos).handleNeighborChanged(world, pos, Blocks.STONE, null, false);
                    BlockState now = world.getBlockState(pos);
                    now.getShape(world, pos);
                    now.getCollisionShape(world, pos);
                    if (now.hasAnalogOutputSignal()) now.getAnalogOutputSignal(world, pos, Direction.NORTH);
                    now.getCloneItemStack(world, pos, true);
                    for (ItemStack tool : tools) Block.getDrops(now, world, pos, world.getBlockEntity(pos), null, tool);
                    world.destroyBlock(pos, true);
                } catch (RuntimeException | LinkageError | AssertionError e) {
                    findings.putIfAbsent(id.toString(), state + " threw " + e);
                } finally {
                    for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1))) {
                        world.setBlock(near, Blocks.AIR.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                    }
                    take(world, pos);
                }
            }
        }
        Sweep.atLeast(context, "block states", states, 500);
        Sweep.report(context, "Every state of every block of ours survives a tick and a break", findings, Map.of());
        context.succeed();
    }

    /**
     * Every config setting at the bottom of its range, every multiplier a hair above zero, and every
     * setting at the top of its range still give numbers that make sense — effects that last, odds
     * that are odds, machine times in order — and a setting turned down never makes anything
     * stronger than the default does.
     *
     * <p>A {@code greenOutChanceMultiplier} just above 0 made the odds so long that, narrowed to an
     * int, they wrapped negative, and {@code max(1, …)} turned that into a green-out on every hit
     * (until 2.0.3). "At least one" was true of the broken number too, so the test asks which way it
     * moved.
     */
    public static void everyConfigExtremeKeepsTheNumbersSane(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        JsonObject defaults = HempdustryConfig.CODEC.encodeStart(JsonOps.INSTANCE, HempdustryConfig.DEFAULT)
                .getOrThrow().getAsJsonObject();
        Numbers normal = Numbers.now(world);
        Map<String, String> findings = new TreeMap<>();
        for (Extreme extreme : Extreme.values()) {
            ConfigGameTest.withConfig(extreme.over(defaults).toString(), () -> {
                Numbers at = Numbers.now(world);
                String where = "every setting " + extreme.description;
                for (MobEffectInstance effect : at.effects()) {
                    if (effect.getDuration() < 1 || effect.getAmplifier() < 0 || effect.getAmplifier() > 255) {
                        findings.putIfAbsent(where, effect + " is not an effect a player can have");
                    }
                }
                boolean down = extreme != Extreme.TOP;
                checkWay(findings, where, "a 900-tick effect's length", normal.duration(), at.duration(), down);
                checkWay(findings, where, "a 100-tick cooldown", normal.cooldown(), at.cooldown(), down);
                checkWay(findings, where, "a crop's growth odds", normal.growth(), at.growth(), down);
                checkWay(findings, where, "an edible's onset", normal.onset(), at.onset(), down);
                // Green-out odds are one-in-n: turned down, n grows (or 0, never); turned up, n shrinks.
                boolean longer = at.greenOutOneIn() == 0 || at.greenOutOneIn() >= normal.greenOutOneIn();
                if (down != longer || at.greenOutOneIn() < 0) {
                    findings.putIfAbsent(where + " green-out", "one-in-" + normal.greenOutOneIn() + " became one-in-" + at.greenOutOneIn());
                }
                if (at.infuserMin() < 1 || at.infuserFull() <= at.infuserMin()) {
                    findings.putIfAbsent(where + " infuser", "the batch's times are " + at.infuserMin() + " and " + at.infuserFull());
                }
            });
        }
        Sweep.atLeast(context, "effects measured", normal.effects().size(), 50);
        Sweep.report(context, "Every config extreme keeps the numbers sane", findings, Map.of());
        context.succeed();
    }

    /**
     * Rules the mod is built on, written down beside the code, held as checks.
     * <ul>
     *   <li><b>Heat activates; raw plant does nothing.</b> No raw bud, leaf or seed can be eaten or
     *   drunk at all: every edible goes through the Decarboxylator.</li>
     *   <li><b>A {@code Feature.FLOWER} patch is a bone-meal recipe</b> — bone-mealed grass grows any
     *   flower feature of its biome, so a wild plant placed by one was free seeds wherever it spawned.</li>
     *   <li><b>No two-block plant may be held by an enderman</b>: lifting one half orphans the other,
     *   which pops off and rolls the loot.</li>
     *   <li><b>No boolean property of ours defaults to true</b>: one left out of
     *   {@code registerDefaultState} does, and two days of seeds were planted pre-trimmed that way.</li>
     * </ul>
     */
    public static void theWrittenRulesHold(GameTestHelper context) {
        RegistryAccess registries = context.getLevel().registryAccess();
        Map<String, String> findings = new TreeMap<>();
        for (TagKey<Item> raw : List.of(ModTags.Conventional.HEMP_CROPS, ModTags.Items.HEMP_SEEDS)) {
            for (Holder<Item> item : registries.lookupOrThrow(Registries.ITEM).getOrThrow(raw)) {
                if (item.value().components().has(DataComponents.CONSUMABLE)) {
                    findings.put("heat activates: " + item.getRegisteredName(), "is raw plant and can be eaten or drunk");
                }
            }
        }
        registries.lookupOrThrow(Registries.CONFIGURED_FEATURE).entrySet().forEach(entry -> {
            if (Sweep.ours(entry.getKey().identifier())
                    && entry.getValue().getFeatures().anyMatch(feature -> feature.feature() == Feature.FLOWER)) {
                findings.put("flower feature: " + entry.getKey().identifier(), "is or holds a Feature.FLOWER — use RANDOM_PATCH");
            }
        });
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!Sweep.ours(id)) continue;
            BlockState state = block.defaultBlockState();
            boolean twoHigh = state.getProperties().stream().anyMatch(property -> property.getName().equals("half")
                    || property.getName().equals("segment"));
            if (twoHigh && state.is(BlockTags.ENDERMAN_HOLDABLE)) {
                findings.put("enderman: " + id, "is a plant of more than one block, in #minecraft:enderman_holdable");
            }
            for (Property<?> property : state.getProperties()) {
                if (property instanceof BooleanProperty flag && state.getValue(flag)) {
                    findings.put("default false: " + id + " " + flag.getName(), "defaults to true");
                }
            }
        }
        Sweep.report(context, "The rules written down beside the code hold", findings, Map.of(
                "default false: hempdustry:hemp_bricks_wall up", "vanilla's WallBlock: a wall on its own stands as a post"));
        context.succeed();
    }

    /**
     * Every item of ours can be had in survival, starting from nothing a player did not find: what
     * chests, mobs, fishing, brushing and vanilla's own blocks drop (with what we add to them), and
     * what worldgen places; then anything a recipe makes from what can be had, and anything one of
     * our blocks drops once its item can be had and placed. A block dropping itself is no source, so
     * a block whose recipe is gone is a finding. What only code hands out is listed with the code.
     */
    public static void everyItemCanBeHadInSurvival(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        DynamicOps<JsonElement> json = world.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        Map<String, String> byCode = Map.of(
                "hempdustry:retted_hemp_stem", "a water cauldron rets hemp stems (ModCauldronBehaviors)",
                "hempdustry:washed_decarboxylated_hemp", "a water cauldron washes decarboxylated hemp (ModCauldronBehaviors)",
                "hempdustry:charas", "shears rubbing a ripe plant drop it (Defoliation)",
                "hempdustry:hempcrete_block", "hempcrete powder hardens into it in water (CustomConcreteBlock)");
        Set<String> had = new HashSet<>(byCode.keySet());
        Map<Block, Set<String>> ourBlockDrops = new HashMap<>();
        world.getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE)
                .listElements().forEach(table -> {
                    Set<String> drops = ids(LootTable.DIRECT_CODEC.encodeStart(json, table.value()).getOrThrow().toString());
                    Identifier id = table.key().identifier();
                    Block block = Sweep.ours(id) && id.getPath().startsWith("blocks/")
                            ? BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(7))).orElse(null)
                            : null;
                    if (block == null) had.addAll(drops);
                    else ourBlockDrops.put(block, drops);
                });
        // A block worldgen places can be broken where it stands.
        world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).entrySet().forEach(feature -> {
            if (!Sweep.ours(feature.getKey().identifier())) return;
            for (String placed : ids(ConfiguredFeature.DIRECT_CODEC.encodeStart(json, feature.getValue()).getOrThrow().toString())) {
                BuiltInRegistries.BLOCK.getOptional(Identifier.parse(placed)).map(ourBlockDrops::get).ifPresent(had::addAll);
            }
        });
        List<Sample> samples = samples(world, true, new HashMap<>());
        for (boolean grew = true; grew; ) {
            grew = false;
            for (Map.Entry<Block, Set<String>> block : ourBlockDrops.entrySet()) {
                if (had.contains(BuiltInRegistries.ITEM.getKey(block.getKey().asItem()).toString())) grew |= had.addAll(block.getValue());
            }
            for (Sample sample : samples) {
                boolean supplied = true;
                for (int i = 0; i < sample.input().size(); i++) {
                    Identifier needed = BuiltInRegistries.ITEM.getKey(sample.input().getItem(i).getItem());
                    supplied &= sample.input().getItem(i).isEmpty() || !Sweep.ours(needed) || had.contains(needed.toString());
                }
                if (supplied) {
                    grew |= had.add(BuiltInRegistries.ITEM.getKey(craft(sample.recipe().value(), sample.input(), world).getItem()).toString());
                }
            }
        }
        Map<String, String> findings = new TreeMap<>();
        int items = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!Sweep.ours(id)) continue;
            items++;
            if (!had.contains(id.toString()) || byCode.containsKey(id.toString())) {
                findings.put(id.toString(), "nothing a player can find or make leads to it");
            }
        }
        Sweep.atLeast(context, "items", items, 100);
        Sweep.report(context, "Every item of ours can be had in survival", findings, byCode);
        context.succeed();
    }

    /** Every id of ours named anywhere in {@code text}. */
    private static Set<String> ids(String text) {
        Set<String> found = new HashSet<>();
        Matcher named = Pattern.compile("hempdustry:[a-z0-9_/.]+").matcher(text);
        while (named.find()) found.add(named.group());
        return found;
    }

    // ----- recipes -----

    private record Sample(RecipeHolder<?> recipe, RecipeInput input) {
        String describe() {
            List<String> items = new ArrayList<>();
            for (int i = 0; i < input.size(); i++) {
                if (!input.getItem(i).isEmpty()) items.add(BuiltInRegistries.ITEM.getKey(input.getItem(i).getItem()).toString());
            }
            return items.toString();
        }
    }

    /**
     * An input every recipe of ours accepts, built from its own ingredients — with {@code everyChoice},
     * one more for each other item a slot's ingredient takes. A recipe the sweep cannot build an input
     * for is a finding, so a new kind of recipe fails here until the sweep is taught it.
     */
    private static List<Sample> samples(ServerLevel world, boolean everyChoice, Map<String, String> findings) {
        List<Sample> samples = new ArrayList<>();
        for (RecipeHolder<?> entry : world.getServer().getRecipeManager().getRecipes()) {
            Identifier id = entry.id().identifier();
            if (!Sweep.ours(id)) continue;
            Recipe<?> recipe = entry.value();
            List<List<ItemStack>> slots = new ArrayList<>();
            if (recipe instanceof ShapedRecipe shaped) {
                for (Optional<Ingredient> ingredient : shaped.getIngredients()) {
                    slots.add(ingredient.map(SweepGameTest::choices).orElse(List.of(ItemStack.EMPTY)));
                }
                grids(entry, shaped.getWidth(), shaped.getHeight(), slots, everyChoice, samples, findings);
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                for (Ingredient ingredient : shapeless.placementInfo().ingredients()) slots.add(choices(ingredient));
                grids(entry, 3, 3, slots, everyChoice, samples, findings);
            } else if (recipe instanceof PackingRecipe) {
                // A special recipe lists no ingredients; the rows the recipe viewers draw are every load it packs.
                for (ViewerRecipes.Packing row : ViewerRecipes.packing(world.registryAccess())) {
                    grids(entry, 3, 3, new ArrayList<>(row.inputs()), everyChoice, samples, findings);
                }
            } else if (recipe instanceof SingleItemRecipe || recipe instanceof DecarboxylatingRecipe
                    || recipe instanceof PressingRecipe || recipe instanceof InfusingRecipe) {
                for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
                    List<ItemStack> choices = choices(ingredient);
                    if (choices.isEmpty()) findings.putIfAbsent(id.toString(), "has an ingredient nothing satisfies");
                    for (ItemStack choice : everyChoice ? choices : choices.subList(0, Math.min(1, choices.size()))) {
                        samples.add(new Sample(entry, new SingleRecipeInput(choice)));
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
        return ingredient.items().limit(CHOICES).map(ItemStack::new).toList();
    }

    /** The grid made of each slot's first choice, and with {@code everyChoice} a grid per other choice. */
    private static void grids(RecipeHolder<?> entry, int width, int height, List<List<ItemStack>> slots,
                              boolean everyChoice, List<Sample> into, Map<String, String> findings) {
        while (slots.size() < width * height) slots.add(List.of(ItemStack.EMPTY));
        List<ItemStack> first = new ArrayList<>();
        for (List<ItemStack> slot : slots) {
            if (slot.isEmpty()) {
                findings.putIfAbsent(entry.id().identifier().toString(), "has an ingredient nothing satisfies");
                return;
            }
            first.add(slot.getFirst());
        }
        into.add(new Sample(entry, CraftingInput.of(width, height, first)));
        if (!everyChoice) return;
        for (int i = 0; i < slots.size(); i++) {
            for (ItemStack other : slots.get(i).subList(1, slots.get(i).size())) {
                List<ItemStack> grid = new ArrayList<>(first);
                grid.set(i, other);
                into.add(new Sample(entry, CraftingInput.of(width, height, grid)));
            }
        }
    }

    /** The same input with every stackable item doubled, or null if nothing in it stacks. */
    private static RecipeInput stacked(RecipeInput input) {
        if (input instanceof SingleRecipeInput single) {
            return single.item().getMaxStackSize() > 1 ? new SingleRecipeInput(single.item().copyWithCount(2)) : null;
        }
        CraftingInput grid = (CraftingInput) input;
        boolean any = false;
        List<ItemStack> doubled = new ArrayList<>();
        for (ItemStack stack : grid.items()) {
            boolean grows = !stack.isEmpty() && stack.getMaxStackSize() > 1;
            any |= grows;
            doubled.add(grows ? stack.copyWithCount(2) : stack);
        }
        return any ? CraftingInput.of(grid.width(), grid.height(), doubled) : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean matches(Recipe<?> recipe, RecipeInput input, ServerLevel world) {
        return ((Recipe) recipe).matches(input, world);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack craft(Recipe<?> recipe, RecipeInput input, ServerLevel world) {
        return ((Recipe) recipe).assemble(input, world.registryAccess());
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!ItemStack.matches(a.get(i), b.get(i))) return false;
        return true;
    }

    // ----- config -----

    /** Where {@link #everyConfigExtremeKeepsTheNumbersSane} puts every setting. */
    private enum Extreme {
        // The sentinels are what the log gate in build.gradle lets through as a clamp warning.
        BOTTOM("at the bottom of its range", -1_000_000_000, -1.0E9),
        HAIR("a hair above zero", null, 1.0E-9),
        TOP("at the top of its range", 1_000_000_000, 1.0E9);

        private final String description;
        private final Integer whole;
        private final double fraction;

        Extreme(String description, Integer whole, double fraction) {
            this.description = description;
            this.whole = whole;
            this.fraction = fraction;
        }

        /** The default config with every number replaced; whole-number settings keep theirs at {@link #HAIR}. */
        JsonElement over(JsonElement json) {
            if (json.isJsonObject()) {
                JsonObject out = new JsonObject();
                json.getAsJsonObject().entrySet().forEach(e -> out.add(e.getKey(), over(e.getValue())));
                return out;
            }
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) return json;
            if (json.getAsNumber() instanceof Integer) return whole == null ? json : new JsonPrimitive(whole);
            return new JsonPrimitive(fraction);
        }
    }

    /** The numbers the settings feed, read from the config the game is running. */
    private record Numbers(List<MobEffectInstance> effects, int duration, int cooldown, double growth,
                           int onset, int greenOutOneIn, int infuserMin, int infuserFull) {
        static Numbers now(ServerLevel world) {
            List<MobEffectInstance> effects = new ArrayList<>();
            java.util.Random random = new java.util.Random(20261008L);
            for (Holder.Reference<Strain> strain : Strain.all(world.registryAccess())) {
                for (int dose = 1; dose <= 3; dose++) {
                    SmokeContents load = SmokeContents.of(strain, dose);
                    effects.addAll(EffectPolicy.filter(load.effects(900, false, random), load.buffBonus()));
                }
            }
            for (int tier = 1; tier <= EdibleEffects.MAX_TIER; tier++) {
                for (Quality quality : Quality.values()) {
                    EdibleEffects.bundle(EdibleBundle.BUILT_IN, tier, quality, 0).forEach(dose -> effects.add(dose.effect()));
                }
            }
            return new Numbers(effects, EffectPolicy.duration(900), EffectPolicy.cooldown(100),
                    HempGrowth.chance(25.0F, 3.0F, HempdustryConfig.get().world().cropGrowthMultiplier()),
                    EffectPolicy.onset(600, 200, 1200), EffectPolicy.greenOutChanceOneIn(20),
                    InfuserBlockEntity.minTime(), InfuserBlockEntity.fullTime());
        }
    }

    /** A setting turned down may weaken what it feeds, or leave it; never strengthen it — and up, the reverse. */
    private static void checkWay(Map<String, String> findings, String where, String what, double normal, double at, boolean down) {
        if (at < 0 || (down ? at > normal : at < normal)) {
            findings.putIfAbsent(where + " " + what, "was " + normal + " at the defaults and is " + at);
        }
    }

    // ----- blocks -----

    /** Every block of ours whose default state has a block entity, the ones that reuse vanilla's included. */
    private static List<Block> blocksWithEntities() {
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (Sweep.ours(BuiltInRegistries.BLOCK.getKey(block)) && block.defaultBlockState().hasBlockEntity()) blocks.add(block);
        }
        return blocks;
    }

    /** Every stack lying on the ground around {@code pos}, picked up so the next block starts clean. */
    private static List<ItemStack> take(ServerLevel world, BlockPos pos) {
        List<ItemStack> found = new ArrayList<>();
        for (ItemEntity drop : world.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3), entity -> true)) {
            found.add(drop.getItem());
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
    private static <T> int roundTripRegistry(RegistryDataLoader.RegistryData<T> entry, RegistryAccess registries,
                                             DynamicOps<JsonElement> json, Map<String, String> findings) {
        Registry<T> registry = registries.lookupOrThrow(entry.key());
        Codec<T> codec = entry.elementCodec();
        for (Map.Entry<ResourceKey<T>, T> element : registry.entrySet()) {
            // Compared as what they encode to: an entry need not have a value-based equals.
            DataResult<JsonElement> first = codec.encodeStart(json, element.getValue());
            String problem = first.isError() ? "does not encode: " + first.error().orElseThrow().message()
                    : codec.parse(json, first.getOrThrow()).flatMap(back -> codec.encodeStart(json, back))
                    .mapOrElse(again -> again.equals(first.getOrThrow()) ? null : "comes back as " + again,
                            error -> "does not decode what it wrote: " + error.message());
            if (problem != null) findings.put(element.getKey().identifier() + " (" + entry.key().identifier() + ")", problem);
        }
        return registry.entrySet().size();
    }

    private static TagKey<?> tagIn(Field field) {
        try {
            field.setAccessible(true);
            return (TagKey<?>) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> int size(RegistryAccess registries, TagKey<T> tag) {
        return registries.lookupOrThrow(tag.registry()).get(tag).map(list -> list.size()).orElse(0);
    }

    private static String describe(ItemStack stack) {
        return stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()) + stack.getComponentsPatch();
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

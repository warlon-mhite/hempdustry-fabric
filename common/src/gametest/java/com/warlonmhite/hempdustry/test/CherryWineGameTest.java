package com.warlonmhite.hempdustry.test;

import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModConfiguredFeatures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cherry Wine: Purple Kush's body on farmland, a wild flower of Purple Kush's kind in cherry groves, a
 * smoke of CBD only, seeds in the chests but not the grass, and a joke advancement that says CBD
 * fixes nothing. Each guard fails quietly: a seed that went down on grass, a wild flower dropping the
 * wrong thing, a Hunger or a high from a CBD bud, cherry seeds in every meadow, or an advancement
 * that never fires would all look like working content.
 */
public final class CherryWineGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos CROP = SOIL.above();

    /** Seeds take farmland and a bed only, as Purple Kush's do. */
    public static void cherryWinePlantsOnFarmland(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos soil = context.absolutePos(SOIL);
        BlockPos crop = context.absolutePos(CROP);
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                context.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
            }
        }
        context.setBlock(SOIL.east(2), Blocks.WATER);

        // Planting through the real click: farmland and both beds take a seed; grass and dirt refuse
        // it without eating it.
        ServerPlayer player = Players.survival(context);
        ItemStack seeds = new ItemStack(ModItems.CHERRY_WINE_SEEDS, 8);
        for (Block ok : new Block[]{Blocks.FARMLAND, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(SOIL, ok);
            int before = seeds.getCount();
            plant(player, world, soil, seeds);
            context.assertTrue(world.getBlockState(crop).is(ModBlocks.CHERRY_WINE_CROP), "a seed would not go down on " + ok);
            context.assertValueEqual(before - 1, seeds.getCount(), "seeds left after planting on " + ok);
            context.setBlock(CROP, Blocks.AIR);
        }
        for (Block no : new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL}) {
            context.setBlock(SOIL, no);
            int before = seeds.getCount();
            plant(player, world, soil, seeds);
            context.assertTrue(!world.getBlockState(crop).is(ModBlocks.CHERRY_WINE_CROP), "a seed went down on " + no);
            context.assertValueEqual(before, seeds.getCount(), "seeds left after " + no + " refused one");
        }

        // The crop stands where Purple Kush's does, and nowhere else: the wild plant is a flower.
        BlockState seedling = ModBlocks.CHERRY_WINE_CROP.defaultBlockState();
        for (Block ok : new Block[]{Blocks.FARMLAND, ModBlocks.GROW_POT}) {
            context.setBlock(SOIL, ok);
            context.assertTrue(seedling.canSurvive(world, crop), "Cherry Wine cannot stand on " + ok);
        }
        for (Block no : new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE, Blocks.SAND}) {
            context.setBlock(SOIL, no);
            context.assertTrue(!seedling.canSurvive(world, crop), "Cherry Wine can stand on " + no);
        }
        context.succeed();
    }

    /** A CBD-only smoke: Regeneration and Slowness at level I for 50 ticks a mid bud, no high, no Hunger. */
    public static void cherryWineSmokesCbdOnly(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> cherryWine = strain(world, ModStrains.CHERRY_WINE);
        context.assertTrue(cherryWine.value().chemotype() == Chemotype.CBD_DOMINANT, "Cherry Wine is not CBD-dominant");
        SmokeContents one = SmokeContents.of(cherryWine, 1);
        context.assertValueEqual(0, one.thcDose(), "a Cherry Wine bud's THC dose");

        ServerPlayer smoker = Players.survival(context);
        smoke(smoker, world, cherryWine);
        for (Holder<MobEffect> effect : List.of(MobEffects.REGENERATION, MobEffects.SLOWNESS)) {
            MobEffectInstance got = smoker.getEffect(effect);
            context.assertTrue(got != null, "a Cherry Wine pipe gave no " + effect.getRegisteredName());
            context.assertValueEqual(0, got.getAmplifier(), "a Cherry Wine pipe's " + effect.getRegisteredName() + " level");
            context.assertValueEqual(50, got.getDuration(), "a Cherry Wine pipe's " + effect.getRegisteredName() + " ticks");
        }
        context.assertTrue(!smoker.hasEffect(MobEffects.HUNGER), "Cherry Wine gave the munchies");
        context.assertTrue(!smoker.hasEffect(MobEffects.WEAKNESS), "Cherry Wine greened a smoker out");

        // A bong of three: three buds' CBD, 150 ticks, the cap; still level I.
        List<MobEffectInstance> three = SmokeContents.of(cherryWine, 3).effects(DeviceType.PIPE.builtInStats().durationTicks());
        MobEffectInstance regeneration = three.stream().filter(e -> e.is(MobEffects.REGENERATION)).findFirst().orElse(null);
        context.assertTrue(regeneration != null && regeneration.getDuration() == 150 && regeneration.getAmplifier() == 0,
                "a Cherry Wine load of three's Regeneration: " + regeneration);
        context.succeed();
    }

    /** Its seeds share the general chests and the village houses, and never come out of tall grass. */
    public static void cherryWineSeedsSkipTheGrass(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        String seeds = "hempdustry:cherry_wine_seeds";
        expect(context, world, Blocks.TALL_GRASS.getLootTable().orElseThrow(), seeds, false, "hempdustry:indica_seeds");
        expect(context, world, Blocks.LARGE_FERN.getLootTable().orElseThrow(), seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.SIMPLE_DUNGEON, seeds, true, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.SHIPWRECK_SUPPLY, seeds, true, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.VILLAGE_PLAINS_HOUSE, seeds, true, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.VILLAGE_TAIGA_HOUSE, seeds, true, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.DESERT_PYRAMID, seeds, false, "hempdustry:beldia_seeds");
        context.succeed();
    }

    /**
     * The wild plant is a flower of Purple Kush's kind, in a cherry grove only: it stands on grass,
     * never stone; shears or Silk Touch lift it, anything else breaks it into seeds.
     */
    public static void wildCherryWineIsAFlower(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        // The biome is the tag's to say (vanilla's BiomeFilter asks it, and refuses an unregistered
        // copy of the feature, so the patch below starts after it).
        var biomes = world.registryAccess().lookupOrThrow(Registries.BIOME);
        context.assertTrue(biomes.getOrThrow(Biomes.CHERRY_GROVE).is(ModTags.Biomes.CHERRY_WINE_GEN),
                "#hempdustry:cherry_wine_gen does not hold the cherry grove");
        for (ResourceKey<Biome> not : List.of(Biomes.PLAINS, Biomes.MEADOW, Biomes.FLOWER_FOREST)) {
            context.assertTrue(!biomes.getOrThrow(not).is(ModTags.Biomes.CHERRY_WINE_GEN),
                    "#hempdustry:cherry_wine_gen holds " + not.identifier());
        }
        Holder<Strain> cherryWine = strain(world, ModStrains.CHERRY_WINE);
        context.assertTrue(cherryWine.value().wildFlower().filter(f -> f == ModBlocks.CHERRY_WINE_FLOWER).isPresent(),
                "Cherry Wine's wild flower is " + cherryWine.value().wildFlower());
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlock(new BlockPos(x, 1, z), x < 5 ? Blocks.GRASS_BLOCK : Blocks.STONE);
                context.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }
        // The wild feature picks a small or a big patch; one flower is what either patch places once it
        // has scattered to a spot, and the patch is the whole feature, from its tries on.
        ConfiguredFeature<?, ?> patch = world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModConfiguredFeatures.CHERRY_WINE_KEY).value();
        ConfiguredFeature<?, ?> small = ((RandomFeatureConfiguration) patch.config()).defaultFeature.value().feature().value();
        PlacedFeature plant = ((RandomPatchConfiguration) small.config()).feature().value();
        RandomSource random = RandomSource.create(7L);
        var generator = world.getChunkSource().getGenerator();
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(6, 2, 3))),
                "the wild Cherry Wine feature accepted stone");
        context.assertTrue(plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 3))),
                "the wild Cherry Wine feature refused grass");
        context.setBlock(new BlockPos(2, 2, 3), Blocks.AIR);
        for (int i = 0; i < 20; i++) {
            patch.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 4)));
        }
        context.assertTrue(count(context) > 0, "twenty Cherry Wine patches in a cherry grove placed nothing");
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                if (context.getBlockState(new BlockPos(x, 2, z)).is(ModBlocks.CHERRY_WINE_FLOWER)) {
                    context.assertTrue(context.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK),
                            "wild Cherry Wine grew off the grass at x " + x + " z " + z);
                }
                context.assertTrue(!context.getBlockState(new BlockPos(x, 2, z)).is(ModBlocks.CHERRY_WINE_CROP),
                        "the wild patch placed a crop at x " + x + " z " + z);
            }
        }

        // Its drops, as Wild Purple Kush's: seeds by hand, the flower itself with shears.
        LootTable flower = world.getServer().reloadableRegistries().getLootTable(ModBlocks.CHERRY_WINE_FLOWER.getLootTable().orElseThrow());
        for (ItemStack tool : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.SHEARS)}) {
            LootParams params = new LootParams.Builder(world)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(new BlockPos(2, 2, 3))))
                    .withParameter(LootContextParams.BLOCK_STATE, ModBlocks.CHERRY_WINE_FLOWER.defaultBlockState())
                    .withParameter(LootContextParams.TOOL, tool)
                    .create(LootContextParamSets.BLOCK);
            List<ItemStack> drops = flower.getRandomItems(params, 11L);
            boolean shears = !tool.isEmpty();
            context.assertTrue(!drops.isEmpty() && drops.stream().allMatch(d -> d.is(shears
                            ? ModBlocks.CHERRY_WINE_FLOWER.asItem() : ModItems.CHERRY_WINE_SEEDS)),
                    "Wild Cherry Wine broken " + (shears ? "with shears" : "by hand") + " dropped " + drops);
        }
        context.succeed();
    }

    /**
     * "It Doesn't Work Like That": a Cherry Wine hit, or a CBD-dominant edible kicking in, while the
     * green-out's Nausea is on you. Purple Kush while nauseous, Cherry Wine while well, and a THC
     * edible while nauseous grant nothing.
     */
    public static void itDoesntWorkLikeThat(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        AdvancementHolder joke = world.getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath("hempdustry", "it_doesnt_work_like_that"));
        context.assertTrue(joke != null, "hempdustry:it_doesnt_work_like_that is not loaded");
        context.assertTrue(joke.value().display().map(DisplayInfo::isHidden).orElse(false), "the joke is not hidden");
        context.assertTrue(joke.value().parent().equals(java.util.Optional.of(Identifier.fromNamespaceAndPath("hempdustry", "first_contact"))),
                "the joke does not hang under First Contact");
        Holder<Strain> cherryWine = strain(world, ModStrains.CHERRY_WINE);

        ServerPlayer greenedOut = Players.survival(context);
        greenedOut.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 400, 0));
        smoke(greenedOut, world, cherryWine);
        context.assertTrue(greenedOut.getAdvancements().getOrStartProgress(joke).isDone(),
                "a Cherry Wine hit while greened out did not grant the joke");
        context.assertTrue(greenedOut.hasEffect(MobEffects.NAUSEA), "Cherry Wine took the Nausea away");

        ServerPlayer well = Players.survival(context);
        smoke(well, world, cherryWine);
        context.assertTrue(!well.getAdvancements().getOrStartProgress(joke).isDone(), "a Cherry Wine hit while well granted the joke");
        ServerPlayer kush = Players.survival(context);
        kush.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 400, 0));
        smoke(kush, world, strain(world, ModStrains.INDICA));
        context.assertTrue(!kush.getAdvancements().getOrStartProgress(joke).isDone(), "a Purple Kush hit while greened out granted the joke");

        // The edible road: eaten for real, then its wait shortened so the test need not sit out a come-up.
        ServerPlayer cbd = Players.survival(context);
        ServerPlayer thc = Players.survival(context);
        for (ServerPlayer eater : new ServerPlayer[]{cbd, thc}) {
            eater.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 2000, 0));
        }
        EdibleEffects.consume(cbd, 0, 2, Quality.PERFECT);
        EdibleEffects.consume(thc, 2, 0, Quality.PERFECT);
        for (ServerPlayer eater : new ServerPlayer[]{cbd, thc}) {
            List<EdibleScheduler.Pending> waiting = EdibleScheduler.pending(eater);
            context.assertValueEqual(1L, waiting.stream().filter(EdibleScheduler.Pending::onset).count(),
                    "doses marked as the edible kicking in");
            List<EdibleScheduler.Pending> soon = new ArrayList<>();
            int least = waiting.stream().mapToInt(EdibleScheduler.Pending::ticksLeft).min().orElse(0);
            for (EdibleScheduler.Pending pending : waiting) {
                soon.add(new EdibleScheduler.Pending(pending.ticksLeft() - least + 3, pending.effect(), pending.onset(),
                        pending.cbdDominant()));
            }
            Services.PLAYER_DATA.setPendingEdibles(eater, List.copyOf(soon));
        }
        context.assertTrue(!cbd.getAdvancements().getOrStartProgress(joke).isDone(), "a CBD edible granted the joke on the swallow");
        context.runAtTickTime(8, () -> {
            context.assertTrue(cbd.getAdvancements().getOrStartProgress(joke).isDone(),
                    "a CBD edible kicking in while greened out did not grant the joke");
            context.assertTrue(!thc.getAdvancements().getOrStartProgress(joke).isDone(),
                    "a THC edible kicking in while greened out granted the joke");
            context.succeed();
        });
    }

    // ----- helpers -----

    private static Holder<Strain> strain(ServerLevel world, ResourceKey<Strain> key) {
        return world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static int count(GameTestHelper context) {
        int placed = 0;
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                if (context.getBlockState(new BlockPos(x, 2, z)).is(ModBlocks.CHERRY_WINE_FLOWER)) {
                    placed++;
                }
            }
        }
        return placed;
    }

    private static void plant(ServerPlayer player, ServerLevel world, BlockPos soil, ItemStack seeds) {
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        player.gameMode.useItemOn(player, world, seeds, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false));
    }

    private static void smoke(ServerPlayer player, ServerLevel world, Holder<Strain> strain) {
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        pipe.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, 1));
        pipe.set(ModComponents.CHARGES, DeviceType.PIPE.builtInBowl().hits());
        player.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        player.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(ModItems.WOODEN_PIPE));
        player.gameMode.useItem(player, world, pipe, InteractionHand.MAIN_HAND);
    }

    private static com.google.gson.JsonElement encode(ServerLevel world, ResourceKey<LootTable> key) {
        LootTable table = world.getServer().reloadableRegistries().getLootTable(key);
        return LootTable.DIRECT_CODEC.encodeStart(world.registryAccess().createSerializationContext(JsonOps.INSTANCE), table).getOrThrow();
    }

    /** The encoded table names {@code item}, or does not, beside a control that it must. */
    private static void expect(GameTestHelper context, ServerLevel world, ResourceKey<LootTable> key, String item,
                               boolean present, String control) {
        String json = encode(world, key).toString();
        context.assertTrue(json.contains("\"" + item + "\"") == present,
                key.identifier() + (present ? " does not name " : " names ") + item);
        context.assertTrue(json.contains("\"" + control + "\""), key.identifier() + " does not name its control " + control);
    }
}

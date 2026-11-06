package com.warlonmhite.hempdustry.test;

import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.CrimsonWeedCropBlock;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.HempGrowth;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModConfiguredFeatures;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Crimson Weed: the plant that hangs. Its root is the UPPER half, on the canopy, and the tip dangles
 * below — so every rule the crops share is asked here the other way up, and the one mistake the flip
 * invites, a ripe plant paying out twice or not at all, is asked through each way a plant comes down.
 */
public final class CrimsonWeedGameTest {
    /** The canopy block; the root hangs under it, and the tip under that, over a floor at y 1. */
    private static final BlockPos CANOPY = new BlockPos(2, 5, 2);
    private static final BlockPos ROOT = CANOPY.below();
    private static final BlockPos TIP = ROOT.below();
    private static final int ROLLS = 40;
    private static final long SEED_SOURCE = 0xC812A50FL;

    public static void crimsonHangsFromTheCanopy(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerLevel nether = world.getServer().getLevel(Level.NETHER);
        context.assertTrue(nether != null, "this server has no Nether, so this proves nothing");
        CrimsonWeedCropBlock weed = (CrimsonWeedCropBlock) ModBlocks.CRIMSON_CROP;
        BlockPos root = context.absolutePos(ROOT);
        BlockState seedling = weed.defaultBlockState();
        context.assertTrue(seedling.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.UPPER,
                "a Crimson Weed seed plants as " + seedling.getValue(IndicaCropBlock.HALF) + ", not as the hanging root");

        // The ceiling: a nether wart block, and nothing else.
        context.setBlock(CANOPY, Blocks.NETHER_WART_BLOCK);
        context.assertTrue(seedling.canSurvive(world, root), "Crimson Weed cannot hang from a nether wart block");
        for (Block ceiling : new Block[]{Blocks.WARPED_WART_BLOCK, Blocks.NETHERRACK, Blocks.CRIMSON_NYLIUM,
                Blocks.SHROOMLIGHT, Blocks.FARMLAND, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(CANOPY, ceiling);
            context.assertTrue(!seedling.canSurvive(world, root), "Crimson Weed can hang from " + ceiling);
        }
        // Never a floor: a canopy block under it, or a Grow Pot, holds nothing up.
        context.setBlock(CANOPY, Blocks.AIR);
        for (Block floor : new Block[]{Blocks.NETHER_WART_BLOCK, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY, Blocks.FARMLAND}) {
            context.setBlock(TIP, floor);
            context.assertTrue(!seedling.canSurvive(world, root), "Crimson Weed stands on " + floor);
        }
        context.setBlock(TIP, Blocks.AIR);

        // Planted the way a player plants it: seeds on the underside of the canopy.
        context.setBlock(CANOPY, Blocks.NETHER_WART_BLOCK);
        ServerPlayer player = Players.survival(context);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CRIMSON_SEEDS));
        BlockPos canopy = context.absolutePos(CANOPY);
        player.gameMode.useItemOn(player, world, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atBottomCenterOf(canopy), Direction.DOWN, canopy, false));
        context.assertTrue(world.getBlockState(root) == seedling,
                "seeds on the underside of a nether wart block planted " + world.getBlockState(root));
        context.setBlock(ROOT, Blocks.AIR);

        // Speed: 1-in-5 where water boils away, never anywhere else. Nothing crowds it.
        float here = HempGrowth.chance(weed.growthResistance(world, root), weed.fieldMoisture(world, root), 1.0);
        BlockPos far = new BlockPos(0, 64, 0);
        float there = HempGrowth.chance(weed.growthResistance(nether, far), weed.fieldMoisture(nether, far), 1.0);
        context.assertTrue(Math.abs(there - 0.2F) < 1e-6, "Crimson Weed grows at " + there + " a tick in the Nether, expected 1-in-5");
        context.assertTrue(weed.fieldMoisture(world, root) == 0.0F, "Crimson Weed grows at " + here + " a tick in the overworld");
        context.setBlock(ROOT, seedling);
        for (int i = 0; i < 2000; i++) {
            world.getBlockState(root).randomTick(world, root, world.getRandom());
        }
        context.assertTrue(world.getBlockState(root).getValue(IndicaCropBlock.AGE) == 0,
                "Crimson Weed grew in the overworld, to age " + world.getBlockState(root).getValue(IndicaCropBlock.AGE));

        // Bone meal: refused, and not eaten.
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
        context.assertTrue(!BoneMealItem.growCrop(meal, world, root), "Crimson Weed took bone meal");
        context.assertTrue(meal.getCount() == 4, "Crimson Weed ate the bone meal it refused");

        // It grows DOWN: grown on to ripe, the root stays on the canopy and the tip lands under it,
        // with nothing above the canopy touched. A lit Grow Lamp beside it leaves no light record.
        context.setBlock(CANOPY.east(), Blocks.NETHER_WART_BLOCK);
        context.setBlock(ROOT.east(), ModBlocks.GROW_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        grow(world, root, IndicaCropBlock.MAX_AGE);
        BlockState ripe = world.getBlockState(root);
        BlockState tip = context.getBlockState(TIP);
        context.assertTrue(ripe.is(weed) && ripe.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.UPPER
                && ripe.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE, "the root did not ripen in place: " + ripe);
        context.assertTrue(tip.is(weed) && tip.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.LOWER
                && tip.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE, "no ripe tip hangs under the root: " + tip);
        context.assertTrue(context.getBlockState(CANOPY).is(Blocks.NETHER_WART_BLOCK) && context.getBlockState(CANOPY.above()).isAir(),
                "Crimson Weed grew up through its canopy");
        context.assertTrue(ripe.getValue(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "Crimson Weed kept a light record: " + ripe.getValue(GrowLight.PROPERTY));
        context.assertTrue(ripe.getShape(world, root).max(Direction.Axis.Y) == 1.0
                        && tip.getShape(world, context.absolutePos(TIP)).max(Direction.Axis.Y) == 1.0,
                "the plant's outline does not hang from the top of its block");
        context.setBlock(ROOT.east(), Blocks.AIR);

        // Charas: a Nether plant's 1-in-2; the other crops keep theirs.
        context.assertTrue(Defoliation.charasOneIn(ripe) == 2, "Crimson Weed rubs at 1-in-" + Defoliation.charasOneIn(ripe));
        context.assertTrue(Defoliation.charasOneIn(ModBlocks.INDICA_CROP.defaultBlockState()) == Defoliation.CHARAS_CHANCE_ONE_IN,
                "Purple Kush lost its own charas odds");

        // The tip is expendable: knocked off by anything but a player, the root keeps its age, and
        // the next tick hangs a new tip.
        world.destroyBlock(context.absolutePos(TIP), false);
        context.assertTrue(world.getBlockState(root) == ripe, "losing its tip cost the root " + world.getBlockState(root));
        world.getBlockState(root).randomTick(world, root, world.getRandom());
        context.assertTrue(context.getBlockState(TIP).is(weed), "a ripe root did not hang a new tip");

        // No room below, no tip: over lava, over a block, the root stops one stage short of needing one.
        for (Block under : new Block[]{Blocks.LAVA, Blocks.NETHERRACK}) {
            context.setBlock(ROOT, Blocks.AIR);
            context.setBlock(TIP, under);
            context.setBlock(ROOT, seedling);
            grow(world, root, IndicaCropBlock.MAX_AGE);
            int age = world.getBlockState(root).getValue(IndicaCropBlock.AGE);
            context.assertTrue(age == IndicaCropBlock.DOUBLE_BLOCK_AGE - 1,
                    "with " + under + " below, Crimson Weed grew to age " + age);
            context.assertTrue(context.getBlockState(TIP).is(under), "Crimson Weed grew its tip into " + under);
        }
        context.setBlock(TIP, Blocks.AIR);
        context.succeed();
    }

    /**
     * A ripe plant comes down three ways, and each pays one harvest — one stack of seeds — never two
     * and never none: the canopy mined from under it, which pops both halves on its own; a player
     * breaking the root; a player breaking the tip.
     */
    public static void breakingTheAnchorPaysOnce(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = Players.survival(context);

        hangRipe(context);
        world.destroyBlock(context.absolutePos(CANOPY), false);
        context.assertTrue(context.getBlockState(ROOT).isAir() && context.getBlockState(TIP).isAir(),
                "the plant still hangs from a canopy that is gone");
        context.assertValueEqual(1, seedStacks(context), "seed stacks from mining the canopy over a ripe plant");
        clearDrops(context);

        hangRipe(context);
        Players.breakAs(context, player, ROOT);
        context.assertTrue(context.getBlockState(TIP).isAir(), "breaking the root left the tip hanging");
        context.assertValueEqual(1, seedStacks(context), "seed stacks from a player breaking the root");
        clearDrops(context);

        hangRipe(context);
        Players.breakAs(context, player, TIP);
        context.assertTrue(context.getBlockState(ROOT).isAir(), "breaking the tip left the root hanging");
        context.assertValueEqual(1, seedStacks(context), "seed stacks from a player breaking the tip");
        clearDrops(context);
        context.succeed();
    }

    public static void crimsonHarvestAndItsHoglins(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(ROOT));

        // On matched seeds, a ripe Crimson Weed pays Warped Kush's harvest with a bud more and a leaf
        // fewer: every pool draws the same randoms, so the difference is the base counts alone.
        LootTable weed = table(world, ModBlocks.CRIMSON_CROP);
        LootTable kush = table(world, ModBlocks.WARPED_KUSH_CROP);
        Random seeds = new Random(SEED_SOURCE);
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            int[] c = harvest(weed, world, origin, ripe(ModBlocks.CRIMSON_CROP, DoubleBlockHalf.UPPER), seed,
                    ModItems.CRIMSON_BUDS, ModItems.CRIMSON_SEEDS);
            int[] k = harvest(kush, world, origin, ripe(ModBlocks.WARPED_KUSH_CROP, DoubleBlockHalf.LOWER), seed,
                    ModItems.WARPED_KUSH_BUDS, ModItems.WARPED_KUSH_SEEDS);
            context.assertTrue(c[0] == k[0] + 1 && c[1] == k[1] - 1 && c[2] == k[2] && c[3] == k[3],
                    "Crimson Weed paid buds/leaves/seeds/stems " + c[0] + "/" + c[1] + "/" + c[2] + "/" + c[3]
                            + " against Warped Kush's " + k[0] + "/" + k[1] + "/" + k[2] + "/" + k[3]
                            + " — expected a bud more and a leaf fewer");
            // The tip pays nothing, ripe or not: the root is the plant.
            int[] t = harvest(weed, world, origin, ripe(ModBlocks.CRIMSON_CROP, DoubleBlockHalf.LOWER), seed,
                    ModItems.CRIMSON_BUDS, ModItems.CRIMSON_SEEDS);
            context.assertTrue(t[0] + t[1] + t[2] + t[3] == 0, "a ripe Crimson Weed tip paid " + java.util.Arrays.toString(t));
        }

        // The hoglins breed on the seeds, through the tag vanilla reads; nothing else wants it.
        Hoglin hoglin = EntityType.HOGLIN.create(world, EntitySpawnReason.COMMAND);
        context.assertTrue(hoglin != null && hoglin.isFood(new ItemStack(ModItems.CRIMSON_SEEDS)),
                "a hoglin will not breed on Crimson Weed seeds");
        context.assertTrue(hoglin.isFood(new ItemStack(Items.CRIMSON_FUNGUS)), "a hoglin no longer eats crimson fungus");
        context.assertTrue(!hoglin.isFood(new ItemStack(ModItems.CRIMSON_BUDS)), "a hoglin eats the buds — breeding burns a smokeable");
        Strider strider = EntityType.STRIDER.create(world, EntitySpawnReason.COMMAND);
        context.assertTrue(strider != null && !strider.isFood(new ItemStack(ModItems.CRIMSON_SEEDS)), "a strider eats Crimson Weed seeds");
        context.assertTrue(!new ItemStack(ModItems.CRIMSON_BUDS).is(ItemTags.PIGLIN_LOVED), "piglins covet Crimson Weed buds");
        context.assertTrue(!ripe(ModBlocks.CRIMSON_CROP, DoubleBlockHalf.UPPER).is(BlockTags.HOGLIN_REPELLENTS),
                "hoglins keep away from Crimson Weed");
        context.assertTrue(!ripe(ModBlocks.CRIMSON_CROP, DoubleBlockHalf.UPPER).is(BlockTags.ENDERMAN_HOLDABLE),
                "an enderman can lift half a Crimson Weed");
        context.assertTrue(!ripe(ModBlocks.CRIMSON_CROP, DoubleBlockHalf.UPPER).is(BlockTags.BEE_GROWABLES),
                "bees grow Crimson Weed");
        context.assertTrue(new ItemStack(ModItems.CRIMSON_SEEDS).is(ModTags.Items.HEMP_SEEDS), "Crimson Weed seeds are not hemp seeds");
        context.succeed();
    }

    /** Where the seeds turn up: the hoglin stables, and none of the places that are Warped Kush's or the overworld's. */
    public static void crimsonStaysInTheCrimsonForest(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        String seeds = "hempdustry:crimson_seeds";
        String buds = "hempdustry:crimson_buds";
        expect(context, world, Blocks.TALL_GRASS.getLootTable().orElseThrow(), seeds, false, "hempdustry:sativa_seeds");
        expect(context, world, BuiltInLootTables.SIMPLE_DUNGEON, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.SHIPWRECK_SUPPLY, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.NETHER_BRIDGE, seeds, false, "hempdustry:warped_kush_seeds");
        expect(context, world, BuiltInLootTables.BASTION_OTHER, seeds, false, "hempdustry:warped_kush_seeds");
        expect(context, world, BuiltInLootTables.BASTION_TREASURE, buds, false, "hempdustry:warped_kush_buds");
        expect(context, world, BuiltInLootTables.BASTION_TREASURE, seeds, false, "hempdustry:warped_kush_buds");
        expect(context, world, BuiltInLootTables.PIGLIN_BARTERING, seeds, false, "hempdustry:warped_kush_seeds");
        expect(context, world, BuiltInLootTables.BASTION_HOGLIN_STABLE, seeds, true, "minecraft:crimson_fungus");
        expect(context, world, BuiltInLootTables.BASTION_HOGLIN_STABLE, "hempdustry:warped_kush_seeds", false, null);
        context.succeed();
    }

    /** The wild plant is the crop, generated ripe and whole, hanging under the canopy only. */
    public static void wildCrimsonGrowsRipe(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        // A ceiling at y 5: nether wart block over x < 5, warped wart block beyond; air under it.
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                context.setBlock(new BlockPos(x, 1, z), Blocks.NETHERRACK);
                for (int y = 2; y < 5; y++) {
                    context.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
                context.setBlock(new BlockPos(x, 5, z), x < 5 ? Blocks.NETHER_WART_BLOCK : Blocks.WARPED_WART_BLOCK);
            }
        }
        var feature = world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModConfiguredFeatures.CRIMSON_KEY).value();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7L);

        // The canopy filter, asked directly: a live world would pop a plant off the warped wart block,
        // chunk generation would not.
        var plant = ((RandomPatchConfiguration) feature.config()).feature().value();
        var generator = world.getChunkSource().getGenerator();
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(7, 4, 8))),
                "the wild Crimson Weed feature accepted a spot under a warped wart block");
        context.setBlock(new BlockPos(1, 3, 8), Blocks.NETHERRACK);
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(1, 4, 8))),
                "the wild Crimson Weed feature hung a plant with no room for its tip");
        context.setBlock(new BlockPos(1, 3, 8), Blocks.AIR);
        context.assertTrue(plant.place(world, generator, random, context.absolutePos(new BlockPos(1, 4, 8))),
                "the wild Crimson Weed feature refused a clear spot under a nether wart block");
        context.setBlock(new BlockPos(1, 4, 8), Blocks.AIR);
        context.setBlock(new BlockPos(1, 3, 8), Blocks.AIR);

        for (int i = 0; i < 20; i++) {
            feature.place(world, generator, random, context.absolutePos(new BlockPos(4, 4, 4)));
        }
        int placed = 0;
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                for (int y = 2; y < 5; y++) {
                    BlockState state = context.getBlockState(new BlockPos(x, y, z));
                    if (!state.is(ModBlocks.CRIMSON_CROP) || state.getValue(IndicaCropBlock.HALF) != DoubleBlockHalf.UPPER) {
                        continue;
                    }
                    placed++;
                    BlockState tip = context.getBlockState(new BlockPos(x, y - 1, z));
                    context.assertTrue(x < 5 && y == 4, "wild Crimson Weed hung from " + context.getBlockState(new BlockPos(x, y + 1, z)));
                    context.assertTrue(state.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE,
                            "wild Crimson Weed was placed unripe: " + state);
                    context.assertTrue(tip.is(ModBlocks.CRIMSON_CROP) && tip.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.LOWER
                            && tip.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE, "wild Crimson Weed has no ripe tip: " + tip);
                }
            }
        }
        context.assertTrue(placed > 0, "twenty Crimson Weed patches under nether wart placed nothing");
        context.succeed();
    }

    /** Strength and the red eyes; and debuffs=false takes the red eyes, which vanilla files as neutral. */
    public static void crimsonSmokesStrengthAndGivesYouAway(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Strain crimson = world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.CRIMSON).value();
        Strain lemon = world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.SATIVA).value();
        List<MobEffectInstance> hit = crimson.effects(3, 700, false, 0, new Random(1));
        context.assertTrue(has(hit, MobEffects.STRENGTH) && has(hit, MobEffects.GLOWING) && has(hit, MobEffects.HUNGER)
                && hit.size() == 3, "a Crimson Weed hit gave " + hit);
        context.assertTrue(level(EffectPolicy.filter(hit), MobEffects.STRENGTH) == 1,
                "a dose of three reached Strength " + (level(EffectPolicy.filter(hit), MobEffects.STRENGTH) + 1) + " past the buff cap");
        context.assertTrue(level(hit, MobEffects.GLOWING) == 0, "Glowing scaled with the dose");
        context.assertTrue(has(EffectPolicy.filter(hit), MobEffects.GLOWING), "Glowing was taken away with debuffs on");
        ConfigGameTest.withConfig("{ \"effects\": { \"debuffs\": false } }", () -> {
            List<MobEffectInstance> kept = EffectPolicy.filter(hit);
            context.assertTrue(has(kept, MobEffects.STRENGTH), "debuffs=false took Strength");
            context.assertTrue(!has(kept, MobEffects.GLOWING), "debuffs=false left Crimson Weed's Glowing on");
            List<MobEffectInstance> haze = EffectPolicy.filter(lemon.effects(1, 700, false, 0, new Random(1)));
            context.assertTrue(!has(haze, MobEffects.WEAKNESS) && has(haze, MobEffects.SPEED),
                    "the control failed: debuffs=false on Lemon Haze gave " + haze);
        });
        context.succeed();
    }

    // ----- helpers -----

    /** Grows the plant whose root is at {@code root} one bone-meal step at a time, which refusal does not gate. */
    private static void grow(ServerLevel world, BlockPos root, int steps) {
        for (int i = 0; i < steps; i++) {
            BlockState state = world.getBlockState(root);
            ((CropBlock) state.getBlock()).growCrops(world, root, state);
        }
    }

    /** A ripe, untrimmed plant hanging from a fresh canopy at {@link #CANOPY}. */
    private static void hangRipe(GameTestHelper context) {
        context.setBlock(CANOPY, Blocks.NETHER_WART_BLOCK);
        context.setBlock(ROOT, ripe(ModBlocks.CRIMSON_CROP, DoubleBlockHalf.UPPER));
        context.setBlock(TIP, ripe(ModBlocks.CRIMSON_CROP, DoubleBlockHalf.LOWER));
    }

    /**
     * The seed stacks lying around the plant, counted on the tick they dropped: a harvest pops its
     * seeds as one stack, and item entities only merge on a later tick, so two harvests are two.
     */
    private static int seedStacks(GameTestHelper context) {
        AABB box = new AABB(context.absolutePos(TIP)).inflate(3);
        return context.getLevel().getEntitiesOfClass(ItemEntity.class, box,
                item -> item.getItem().is(ModItems.CRIMSON_SEEDS)).size();
    }

    private static void clearDrops(GameTestHelper context) {
        AABB box = new AABB(context.absolutePos(TIP)).inflate(3);
        context.getLevel().getEntitiesOfClass(ItemEntity.class, box, item -> true).forEach(ItemEntity::discard);
    }

    private static BlockState ripe(Block crop, DoubleBlockHalf half) {
        return Defoliation.unworked(crop.defaultBlockState())
                .setValue(IndicaCropBlock.HALF, half)
                .setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
    }

    private static boolean has(List<MobEffectInstance> effects, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effects.stream().anyMatch(e -> e.getEffect().equals(effect));
    }

    private static int level(List<MobEffectInstance> effects, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effects.stream().filter(e -> e.getEffect().equals(effect)).mapToInt(MobEffectInstance::getAmplifier).max().orElse(-1);
    }

    private static LootTable table(ServerLevel world, Block block) {
        return world.getServer().reloadableRegistries().getLootTable(block.getLootTable().orElseThrow());
    }

    /** {buds, leaves, seeds, stems} from one roll. */
    private static int[] harvest(LootTable table, ServerLevel world, Vec3 origin, BlockState state, long seed,
                                 Item buds, Item seeds) {
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);
        int[] counts = new int[4];
        for (ItemStack stack : table.getRandomItems(params, seed)) {
            if (stack.is(buds)) {
                counts[0] += stack.getCount();
            } else if (stack.is(ModItems.HEMP_LEAF)) {
                counts[1] += stack.getCount();
            } else if (stack.is(seeds)) {
                counts[2] += stack.getCount();
            } else if (stack.is(ModItems.HEMP_STEM)) {
                counts[3] += stack.getCount();
            }
        }
        return counts;
    }

    /** As in WarpedKushGameTest: the encoded table names {@code item}, or does not, beside a control that it must. */
    private static void expect(GameTestHelper context, ServerLevel world, ResourceKey<LootTable> key, String item,
                               boolean present, String control) {
        LootTable table = world.getServer().reloadableRegistries().getLootTable(key);
        String json = LootTable.DIRECT_CODEC.encodeStart(world.registryAccess().createSerializationContext(JsonOps.INSTANCE), table)
                .getOrThrow().toString();
        context.assertTrue(json.contains("\"" + item + "\"") == present,
                key.identifier() + (present ? " does not name " : " names ") + item);
        if (control != null) {
            context.assertTrue(json.contains("\"" + control + "\""), key.identifier() + " does not name its control " + control);
        }
    }
}

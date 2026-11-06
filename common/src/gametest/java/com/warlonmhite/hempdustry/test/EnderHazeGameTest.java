package com.warlonmhite.hempdustry.test;

import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.EnderHazeCropBlock;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.HydroTrayBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.effect.ModEffects;
import com.warlonmhite.hempdustry.entity.ai.SeekHempCropGoal;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModConfiguredFeatures;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ender Haze: five tall on end stone, slow everywhere but a working Hydro Tray, and a plant the
 * endermen keep — they loiter in it, they wake when it is harvested, and they do not see a player
 * who smoked it.
 */
public final class EnderHazeGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 0, 2);
    private static final BlockPos ROOT = SOIL.above();
    private static final int ROLLS = 40;
    private static final long SEED_SOURCE = 0xE7DE4A2EL;
    /** The pipe's exhale: {@code Smoking.EXHALE_DELAY_TICKS}, with no sound delay. */
    private static final int EXHALE_TICKS = 38;

    /** The ground, the light, the speed, bone meal, and the two kit exceptions. */
    public static void enderHazeGrowsOnEndStone(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        EnderHazeCropBlock haze = (EnderHazeCropBlock) ModBlocks.ENDER_HAZE_CROP;
        BlockPos root = context.absolutePos(ROOT);
        BlockState seedling = haze.defaultBlockState();

        // End stone, or a Hydro Tray. Never soil.
        context.setBlock(SOIL, Blocks.END_STONE);
        context.assertTrue(seedling.canSurvive(world, root), "Ender Haze cannot grow on end stone");
        context.setBlock(SOIL, ModBlocks.HYDRO_TRAY);
        context.assertTrue(seedling.canSurvive(world, root), "Ender Haze cannot grow in a Hydro Tray");
        for (Block floor : new Block[]{Blocks.FARMLAND, ModBlocks.GROW_POT, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN,
                Blocks.DIRT, Blocks.END_STONE_BRICKS, Blocks.WARPED_NYLIUM}) {
            context.setBlock(SOIL, floor);
            context.assertTrue(!seedling.canSurvive(world, root), "Ender Haze grows on " + floor);
        }

        // The Grow Pot turns the seed down, and the player keeps it.
        context.setBlock(SOIL, ModBlocks.GROW_POT);
        ServerPlayer player = Players.survival(context);
        ItemStack seeds = new ItemStack(ModItems.ENDER_HAZE_SEEDS);
        Players.clickLikeAClient(context, player, SOIL, seeds);
        context.assertTrue(context.getBlockState(ROOT).isAir(), "a Grow Pot took an Ender Haze seed: " + context.getBlockState(ROOT));
        context.assertTrue(player.getMainHandItem().is(ModItems.ENDER_HAZE_SEEDS) && player.getMainHandItem().getCount() == 1,
                "the refused seed was used up");

        // The odds: 1-in-8 a random tick on end stone, packed or alone. A roll just under 1/8 grows
        // the plant and one just over does not.
        context.setBlock(SOIL, Blocks.END_STONE);
        context.setBlock(ROOT, seedling);
        context.assertTrue(grows(world, root, 0.124F), "a roll under 1-in-8 did not grow Ender Haze on end stone");
        context.setBlock(ROOT, seedling);
        context.assertTrue(!grows(world, root, 0.126F), "a roll over 1-in-8 grew Ender Haze on end stone");
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                context.setBlock(new BlockPos(x, 0, z), Blocks.END_STONE);
                if (x != 2 || z != 2) {
                    context.setBlock(new BlockPos(x, 1, z), seedling);
                }
            }
        }
        context.setBlock(ROOT, seedling);
        context.assertTrue(grows(world, root, 0.124F), "packed Ender Haze is crowded below 1-in-8");
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                context.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
            }
        }

        // A lit Grow Lamp over a young plant moves no odds — Lemon Haze under the same lamp, the
        // control, does get its head start.
        context.setBlock(ROOT.above(EnderHazeCropBlock.HEIGHT + 1),
                ModBlocks.GROW_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        context.setBlock(ROOT, seedling.setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP));
        context.assertTrue(!grows(world, root, 0.13F), "a Grow Lamp sped a young Ender Haze up");
        context.setBlock(SOIL, Blocks.FARMLAND);
        BlockState lemon = ModBlocks.SATIVA_CROP.defaultBlockState();
        context.setBlock(ROOT, lemon.setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP));
        float lemonOdds = 1.0F / (int) (35.0F / ((SativaCropBlock) ModBlocks.SATIVA_CROP).fieldMoisture(world, root) + 1);
        context.assertTrue(grows(world, root, lemonOdds * 1.05F),
                "the control failed: the Grow Lamp gave a young Lemon Haze no head start");
        context.setBlock(ROOT.above(EnderHazeCropBlock.HEIGHT + 1), Blocks.AIR);

        // A working tray doubles it; plain water, or the pump off, is the outdoor rate.
        BlockState tray = ModBlocks.HYDRO_TRAY.defaultBlockState().setValue(HydroTrayBlock.LEVEL, 3);
        // The pump runs on redstone: a tray placed with no signal turns its pump off.
        context.setBlock(SOIL.east(), Blocks.REDSTONE_BLOCK);
        context.setBlock(SOIL, tray.setValue(HydroTrayBlock.FED, true).setValue(HydroTrayBlock.POWERED, true));
        context.setBlock(ROOT, seedling);
        context.assertTrue(grows(world, root, 0.249F), "a working Hydro Tray did not double Ender Haze");
        context.setBlock(ROOT, seedling);
        context.assertTrue(!grows(world, root, 0.251F), "a working Hydro Tray more than doubled Ender Haze");
        context.setBlock(SOIL.east(), Blocks.END_STONE);
        for (BlockState still : new BlockState[]{tray, tray.setValue(HydroTrayBlock.FED, true)}) {
            context.setBlock(SOIL, still);
            context.setBlock(ROOT, seedling);
            context.assertTrue(!grows(world, root, 0.126F), "a tray with " + still + " sped Ender Haze up");
        }

        // Bone meal: taken, and it grows the plant.
        context.setBlock(SOIL, Blocks.END_STONE);
        context.setBlock(ROOT, seedling);
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
        context.assertTrue(BoneMealItem.growCrop(meal, world, root), "Ender Haze refused bone meal");
        context.assertTrue(meal.getCount() == 3, "bone meal on Ender Haze was not used");

        // An ordinary plant's charas odds, and a light record like any overworld strain's.
        context.assertTrue(Defoliation.charasOneIn(ripe(0)) == Defoliation.CHARAS_CHANCE_ONE_IN,
                "Ender Haze rubs at 1-in-" + Defoliation.charasOneIn(ripe(0)));
        context.succeed();
    }

    /** The shape by age, the headroom cap at every height, and one harvest from any segment. */
    public static void enderHazeClimbsToFive(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos root = context.absolutePos(ROOT);
        int[] heights = {1, 1, 1, 2, 3, 4, 5, 5};
        context.setBlock(SOIL, Blocks.END_STONE);
        context.setBlock(ROOT, ModBlocks.ENDER_HAZE_CROP);
        for (int age = 0; age <= EnderHazeCropBlock.MAX_AGE; age++) {
            context.assertTrue(world.getBlockState(root).getValue(EnderHazeCropBlock.AGE) == age, "the plant did not reach age " + age);
            context.assertValueEqual(heights[age], standing(context), "blocks of Ender Haze at age " + age);
            for (int segment = 0; segment < heights[age]; segment++) {
                BlockState part = context.getBlockState(ROOT.above(segment));
                context.assertTrue(part.getValue(EnderHazeCropBlock.SEGMENT) == segment
                        && part.getValue(EnderHazeCropBlock.AGE) == age, "segment " + segment + " at age " + age + " is " + part);
            }
            grow(world, root, 1);
        }

        // A roof r blocks over the root stops the plant one stage short of needing that block, and
        // it grows on once the roof is gone.
        int[] stopsAt = {2, 3, 4, 5};
        for (int roof = 1; roof <= 4; roof++) {
            clear(context);
            context.setBlock(ROOT, ModBlocks.ENDER_HAZE_CROP);
            context.setBlock(ROOT.above(roof), Blocks.STONE);
            grow(world, root, 10);
            int age = world.getBlockState(root).getValue(EnderHazeCropBlock.AGE);
            context.assertTrue(age == stopsAt[roof - 1], "under a roof " + roof + " up, Ender Haze grew to age " + age);
            context.assertTrue(context.getBlockState(ROOT.above(roof)).is(Blocks.STONE), "Ender Haze grew into its roof");
            ItemStack meal = new ItemStack(Items.BONE_MEAL);
            context.assertTrue(!BoneMealItem.growCrop(meal, world, root) && meal.getCount() == 1,
                    "a capped Ender Haze took bone meal under a roof " + roof + " up");
            context.setBlock(ROOT.above(roof), Blocks.AIR);
            grow(world, root, 10);
            context.assertTrue(world.getBlockState(root).getValue(EnderHazeCropBlock.AGE) == EnderHazeCropBlock.MAX_AGE
                    && standing(context) == EnderHazeCropBlock.HEIGHT, "Ender Haze did not finish once its roof " + roof + " up was gone");
        }

        // Breaking any of the five pays the plant once — one stack of seeds — and takes all of it.
        ServerPlayer player = Players.survival(context);
        for (int segment = 0; segment < EnderHazeCropBlock.HEIGHT; segment++) {
            clear(context);
            standRipe(context);
            Players.breakAs(context, player, ROOT.above(segment));
            context.assertValueEqual(0, standing(context), "blocks left after breaking segment " + segment);
            context.assertValueEqual(1, seedStacks(context), "seed stacks from breaking segment " + segment);
            clearDrops(context);
        }
        context.succeed();
    }

    /** Water keeps in the End, so the tray works there — the mirror of the Nether's test. */
    public static void aTrayHoldsWaterInTheEnd(GameTestHelper context) {
        ServerLevel end = context.getLevel().getServer().getLevel(Level.END);
        ServerLevel nether = context.getLevel().getServer().getLevel(Level.NETHER);
        context.assertTrue(end != null && nether != null, "this server has no End or no Nether, so this proves nothing");
        BlockPos pos = new BlockPos(100, 60, 0);
        context.assertTrue(!HydroTrayBlock.boilsAway(end, pos), "water boils away in the End");
        context.assertTrue(HydroTrayBlock.boilsAway(nether, new BlockPos(0, 64, 0)), "the control failed: water keeps in the Nether");
        BlockState watered = ModBlocks.HYDRO_TRAY.defaultBlockState().setValue(HydroTrayBlock.LEVEL, 3);
        context.assertTrue(((HydroTrayBlock) ModBlocks.HYDRO_TRAY).isValidBonemealTarget(end, pos, watered, net.minecraft.world.level.block.BonemealSource.INTERACTION),
                "a watered tray cannot be fed in the End");
        context.succeed();
    }

    /**
     * A real pipe hit: Speed, Averted Gaze and Hunger with it, Levitation only on the exhale and for a
     * tenth of the hit, never on a player who died in the window. Levitation scales with the dose,
     * Averted Gaze does not, and debuffs=false takes Levitation and leaves the rest.
     */
    public static void enderHazeLiftsYou(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> haze = world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.ENDER_HAZE);
        int length = DeviceType.PIPE.builtInStats().durationTicks();

        List<MobEffectInstance> three = haze.value().effects(3, 700);
        context.assertTrue(level(three, MobEffects.LEVITATION) == 2, "a dose of three gave Levitation " + (level(three, MobEffects.LEVITATION) + 1));
        context.assertTrue(level(three, ModEffects.AVERTED_GAZE) == 0, "Averted Gaze scaled with the dose");
        context.assertTrue(has(EffectPolicy.filter(three), MobEffects.LEVITATION), "Levitation was taken away with debuffs on");
        ConfigGameTest.withConfig("{ \"effects\": { \"debuffs\": false } }", () -> {
            List<MobEffectInstance> kept = EffectPolicy.filter(three);
            context.assertTrue(!has(kept, MobEffects.LEVITATION), "debuffs=false left Ender Haze's Levitation on");
            context.assertTrue(has(kept, MobEffects.SPEED) && has(kept, ModEffects.AVERTED_GAZE),
                    "debuffs=false took more than the tax: " + kept);
        });

        ServerPlayer smoker = Players.survival(context);
        smoke(smoker, world, haze);
        MobEffectInstance gaze = smoker.getEffect(ModEffects.AVERTED_GAZE);
        context.assertTrue(gaze != null && gaze.getDuration() == length, "the hit did not give " + length + " ticks of Averted Gaze: " + gaze);
        context.assertTrue(smoker.hasEffect(MobEffects.SPEED) && smoker.hasEffect(MobEffects.HUNGER), "the hit did not give Speed and Hunger");
        context.assertTrue(!smoker.hasEffect(MobEffects.LEVITATION), "Levitation landed with the hit, not the exhale");

        ServerPlayer dies = Players.survival(context);
        smoke(dies, world, haze);
        dies.kill(world);
        ServerPlayer respawned = world.getServer().getPlayerList().respawn(dies, false, Entity.RemovalReason.KILLED);

        context.runAtTickTime(EXHALE_TICKS + 5, () -> {
            MobEffectInstance lift = smoker.getEffect(MobEffects.LEVITATION);
            int expected = Math.round(length * 0.1F);
            context.assertTrue(lift != null, "no Levitation on the exhale");
            context.assertTrue(lift.getDuration() <= expected && lift.getDuration() > expected - 10,
                    "the exhale's Levitation lasts " + lift.getDuration() + ", expected a tenth of the hit (" + expected + ")");
            context.assertTrue(!respawned.hasEffect(MobEffects.LEVITATION), "a player who died before the exhale came back floating");
            context.succeed();
        });
    }

    /**
     * The enderman's own stare test: a bare player meeting its eyes is staring, one carrying Averted
     * Gaze or a carved pumpkin (the control) is not, and the effect wearing off restores the stare.
     * And the mixin gave every enderman the loiter goal, which wants Ender Haze and nothing else.
     */
    public static void avertedGazeBlindsAnEnderman(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        // spawn, NOT spawnWithNoFreeWill: that strips every goal off the mob.
        Enderman enderman = context.spawn(EntityTypes.ENDERMAN, new BlockPos(5, 1, 5));
        enderman.setNoAi(true);
        ServerPlayer player = Players.survival(context);
        Vec3 stand = Vec3.atBottomCenterOf(context.absolutePos(new BlockPos(1, 1, 5)));
        player.snapTo(stand.x, stand.y, stand.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, enderman.getEyePosition());

        context.assertTrue(staredBy(enderman, player), "the control failed: a bare player meeting its eyes is not staring");
        player.addEffect(new MobEffectInstance(ModEffects.AVERTED_GAZE, 40));
        context.assertTrue(!staredBy(enderman, player), "an enderman sees a player under Averted Gaze");
        player.removeEffect(ModEffects.AVERTED_GAZE);
        context.assertTrue(staredBy(enderman, player), "the stare did not come back once Averted Gaze was gone");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        context.assertTrue(!staredBy(enderman, player), "the control failed: a carved pumpkin does not hide a player");

        WrappedGoal loiter = goals(enderman).getAvailableGoals().stream()
                .filter(goal -> goal.getGoal() instanceof SeekHempCropGoal).findFirst().orElse(null);
        context.assertTrue(loiter != null && loiter.getPriority() == 6,
                "an enderman has no loiter goal at priority 6: " + loiter);

        // What it seeks: grown, open-sky Ender Haze, and never a hemp field.
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlock(new BlockPos(x, 0, z), Blocks.END_STONE);
            }
        }
        context.setBlock(new BlockPos(2, 0, 2), Blocks.FARMLAND);
        context.setBlock(new BlockPos(2, 1, 2), ModBlocks.INDICA_CROP.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7));
        context.setBlock(new BlockPos(2, 2, 2), ModBlocks.INDICA_CROP.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7)
                .setValue(com.warlonmhite.hempdustry.block.custom.IndicaCropBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        openSky(context);
        java.util.function.Predicate<BlockState> haze = state -> state.is(ModBlocks.ENDER_HAZE_CROP);
        context.assertTrue(SeekHempCropGoal.findCover(enderman, 3, 256, haze) == null,
                "an enderman took a Purple Kush field for Ender Haze");
        context.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
        context.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        context.setBlock(new BlockPos(2, 0, 2), Blocks.END_STONE);
        BlockPos at = new BlockPos(5, 1, 3);
        for (int segment = 0; segment < EnderHazeCropBlock.HEIGHT; segment++) {
            context.setBlock(at.above(segment), ripe(segment));
        }
        BlockPos cover = SeekHempCropGoal.findCover(enderman, 3, 256, haze);
        context.assertTrue(context.absolutePos(at).equals(cover), "an enderman beside a ripe Ender Haze found " + cover);
        context.succeed();
    }

    /**
     * Breaking a plant turns the endermen in range on the player, farmed exactly as wild (it is one
     * block); one out of range stays calm; a disguised player — Averted Gaze, a pumpkin — wakes none,
     * and neither does anyone with the off switch thrown.
     */
    public static void harvestingEnderHazeWakesTheNeighbours(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = Players.survival(context);
        context.setBlock(SOIL, Blocks.END_STONE);

        Enderman near = calm(context, new BlockPos(6, 1, 6));
        Enderman far = calm(context, new BlockPos(2, 1, 2 + 24));
        standRipe(context);
        Players.breakAs(context, player, ROOT.above(2));
        context.assertTrue(near.isAngryAt(player, world) && near.getTarget() == player, "an enderman 6 blocks off slept through the harvest");
        context.assertTrue(!far.isAngryAt(player, world), "an enderman 24 blocks off was woken by the harvest");
        long angryUntil = near.getPersistentAngerEndTime();
        standRipe(context);
        Players.breakAs(context, player, ROOT);
        context.assertTrue(near.getPersistentAngerEndTime() == angryUntil, "an enderman already angry was angered again");
        near.discard();
        far.discard();
        clearDrops(context);

        Enderman calm = calm(context, new BlockPos(6, 1, 6));
        player.addEffect(new MobEffectInstance(ModEffects.AVERTED_GAZE, 200));
        standRipe(context);
        Players.breakAs(context, player, ROOT);
        context.assertTrue(!calm.isAngryAt(player, world), "a player under Averted Gaze woke the endermen");
        player.removeEffect(ModEffects.AVERTED_GAZE);

        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
        standRipe(context);
        Players.breakAs(context, player, ROOT);
        context.assertTrue(!calm.isAngryAt(player, world), "a player in a carved pumpkin woke the endermen");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        ConfigGameTest.withConfig("{ \"world\": { \"creepersSeekHemp\": false } }", () -> {
            standRipe(context);
            Players.breakAs(context, player, ROOT);
            context.assertTrue(!calm.isAngryAt(player, world), "the off switch did not stop the harvest waking the endermen");
        });
        standRipe(context);
        Players.breakAs(context, player, ROOT);
        context.assertTrue(calm.isAngryAt(player, world), "the control failed: a bare player's harvest woke nobody");
        clearDrops(context);
        context.succeed();
    }

    /** Lemon Haze's harvest with stems traded for leaves, on matched seeds; only the root pays. */
    public static void enderHazeHarvest(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(ROOT));
        LootTable haze = table(world, ModBlocks.ENDER_HAZE_CROP);
        LootTable lemon = table(world, ModBlocks.SATIVA_CROP);
        BlockState lemonRipe = Defoliation.unworked(ModBlocks.SATIVA_CROP.defaultBlockState())
                .setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                .setValue(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE);
        Random seeds = new Random(SEED_SOURCE);
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            int[] e = harvest(haze, world, origin, ripe(0), seed, ModItems.ENDER_HAZE_BUDS, ModItems.ENDER_HAZE_SEEDS);
            int[] l = harvest(lemon, world, origin, lemonRipe, seed, ModItems.SATIVA_BUDS, ModItems.SATIVA_SEEDS);
            context.assertTrue(e[0] == l[0] && e[1] == l[1] + 1 && e[2] == l[2] && e[3] == l[3] - 2,
                    "Ender Haze paid buds/leaves/seeds/stems " + e[0] + "/" + e[1] + "/" + e[2] + "/" + e[3]
                            + " against Lemon Haze's " + l[0] + "/" + l[1] + "/" + l[2] + "/" + l[3]
                            + " — expected a leaf more and two stems fewer");
            for (int segment = 1; segment < EnderHazeCropBlock.HEIGHT; segment++) {
                int[] t = harvest(haze, world, origin, ripe(segment), seed, ModItems.ENDER_HAZE_BUDS, ModItems.ENDER_HAZE_SEEDS);
                context.assertTrue(t[0] + t[1] + t[2] + t[3] == 0, "segment " + segment + " of a ripe Ender Haze paid " + java.util.Arrays.toString(t));
            }
        }
        BlockState ripe = ripe(0);
        context.assertTrue(!ripe.is(BlockTags.ENDERMAN_HOLDABLE), "an enderman can lift a fifth of an Ender Haze");
        context.assertTrue(!ripe.is(BlockTags.BEE_GROWABLES) && !ripe.is(BlockTags.MAINTAINS_FARMLAND),
                "Ender Haze is in a farmland crop's tags");
        context.assertTrue(ripe.is(ModTags.Blocks.HEMP_CROPS), "Ender Haze is not a hemp crop");
        context.assertTrue(new ItemStack(ModItems.ENDER_HAZE_SEEDS).is(ModTags.Items.HEMP_SEEDS), "Ender Haze seeds are not hemp seeds");
        context.succeed();
    }

    /** Where the seeds turn up: the end cities (and their ship), never grass or the overworld's chests. */
    public static void enderHazeStaysInTheEnd(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        String seeds = "hempdustry:ender_haze_seeds";
        expect(context, world, Blocks.TALL_GRASS.getLootTable().orElseThrow(), seeds, false, "hempdustry:sativa_seeds");
        expect(context, world, BuiltInLootTables.SIMPLE_DUNGEON, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.SHIPWRECK_SUPPLY, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.NETHER_BRIDGE, seeds, false, "hempdustry:warped_kush_seeds");
        expect(context, world, BuiltInLootTables.END_CITY_TREASURE, seeds, true, "minecraft:beetroot_seeds");
        expect(context, world, BuiltInLootTables.END_CITY_TREASURE, "hempdustry:indica_seeds", false, null);
        context.succeed();
    }

    /** The wild plant is the crop, generated ripe and five tall, on end stone only. */
    public static void wildEnderHazeGrowsRipe(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlock(new BlockPos(x, 0, z), x < 5 ? Blocks.END_STONE : Blocks.PURPUR_BLOCK);
            }
        }
        var feature = wildPatch(world, com.warlonmhite.hempdustry.world.ModPlacedFeatures.ENDER_HAZE_PLACED_KEY, -1);
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7L);
        var plant = wildPatch(world, com.warlonmhite.hempdustry.world.ModPlacedFeatures.ENDER_HAZE_PLACED_KEY, 1);
        var generator = world.getChunkSource().getGenerator();
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(6, 1, 6))),
                "the wild Ender Haze feature accepted purpur");
        context.setBlock(new BlockPos(1, 4, 6), Blocks.END_STONE);
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(1, 1, 6))),
                "the wild Ender Haze feature placed a plant with no room for its five blocks");
        context.setBlock(new BlockPos(1, 4, 6), Blocks.AIR);
        context.assertTrue(plant.place(world, generator, random, context.absolutePos(new BlockPos(1, 1, 6))),
                "the wild Ender Haze feature refused a clear spot on end stone");
        for (int y = 1; y <= EnderHazeCropBlock.HEIGHT; y++) {
            context.setBlock(new BlockPos(1, y, 6), Blocks.AIR);
        }

        for (int i = 0; i < 20; i++) {
            feature.place(world, generator, random, context.absolutePos(new BlockPos(3, 1, 3)));
        }
        int placed = 0;
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                BlockState state = context.getBlockState(new BlockPos(x, 1, z));
                if (!state.is(ModBlocks.ENDER_HAZE_CROP)) {
                    continue;
                }
                placed++;
                context.assertTrue(x < 5, "wild Ender Haze grew on " + context.getBlockState(new BlockPos(x, 0, z)));
                for (int segment = 0; segment < EnderHazeCropBlock.HEIGHT; segment++) {
                    BlockState part = context.getBlockState(new BlockPos(x, 1 + segment, z));
                    context.assertTrue(part.is(ModBlocks.ENDER_HAZE_CROP) && part.getValue(EnderHazeCropBlock.SEGMENT) == segment
                            && part.getValue(EnderHazeCropBlock.AGE) == EnderHazeCropBlock.MAX_AGE,
                            "wild Ender Haze at " + x + "," + z + " is not ripe and whole: segment " + segment + " is " + part);
                }
            }
        }
        context.assertTrue(placed > 0, "twenty Ender Haze patches on end stone placed nothing");
        context.succeed();
    }

    // ----- helpers -----

    /**
     * The enderman's own stare test, {@code Enderman#isBeingStaredBy} — package-private, so asked
     * by reflection under the name both loaders' development runs use.
     */
    private static boolean staredBy(Enderman enderman, Player player) {
        try {
            Method method = Enderman.class.getDeclaredMethod("isBeingStaredBy", Player.class);
            method.setAccessible(true);
            return (boolean) method.invoke(enderman, player);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot ask an enderman whether it is stared at", e);
        }
    }

    /** A mob's goal selector, which is protected. */
    private static GoalSelector goals(Mob mob) {
        try {
            Field field = Mob.class.getDeclaredField("goalSelector");
            field.setAccessible(true);
            return (GoalSelector) field.get(mob);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read a mob's goals", e);
        }
    }

    /** A random source whose every float is {@code roll}: the growth roll decided by the test. */
    private static final class FixedRoll extends LegacyRandomSource {
        private final float roll;

        FixedRoll(float roll) {
            super(0L);
            this.roll = roll;
        }

        @Override
        public float nextFloat() {
            return this.roll;
        }
    }

    /** Whether one random tick, with every roll at {@code roll}, ages the plant at {@code root}. */
    private static boolean grows(ServerLevel world, BlockPos root, float roll) {
        BlockState before = world.getBlockState(root);
        int age = before.getValue(BlockStateProperties.AGE_7);
        before.randomTick(world, root, new FixedRoll(roll));
        return world.getBlockState(root).getValue(BlockStateProperties.AGE_7) > age;
    }

    /** Grows the plant whose root is at {@code root} one bone-meal step at a time. */
    private static void grow(ServerLevel world, BlockPos root, int steps) {
        for (int i = 0; i < steps; i++) {
            BlockState state = world.getBlockState(root);
            if (state.getBlock() instanceof CropBlock crop) {
                crop.growCrops(world, root, state);
            }
        }
    }

    /**
     * The test runner roofs every test box with barriers, and the loiter goal wants open sky: lift the
     * roof over this box, above the height of a grown plant.
     */
    private static void openSky(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                BlockPos column = context.absolutePos(new BlockPos(x, 0, z));
                int top = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
                for (int y = column.getY() + EnderHazeCropBlock.HEIGHT + 1; y < top; y++) {
                    world.setBlockAndUpdate(new BlockPos(column.getX(), y, column.getZ()), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /** How many blocks of Ender Haze stand in the column over the soil. */
    private static int standing(GameTestHelper context) {
        int count = 0;
        for (int y = 0; y < 7; y++) {
            if (context.getBlockState(ROOT.above(y)).is(ModBlocks.ENDER_HAZE_CROP)) {
                count++;
            }
        }
        return count;
    }

    private static void clear(GameTestHelper context) {
        for (int y = 0; y < 7; y++) {
            context.setBlock(ROOT.above(y), Blocks.AIR);
        }
        context.setBlock(SOIL, Blocks.END_STONE);
    }

    /** A ripe, untrimmed plant, all five segments, on end stone at {@link #SOIL}. */
    private static void standRipe(GameTestHelper context) {
        context.setBlock(SOIL, Blocks.END_STONE);
        for (int segment = 0; segment < EnderHazeCropBlock.HEIGHT; segment++) {
            context.setBlock(ROOT.above(segment), ripe(segment));
        }
    }

    private static BlockState ripe(int segment) {
        return Defoliation.unworked(ModBlocks.ENDER_HAZE_CROP.defaultBlockState())
                .setValue(EnderHazeCropBlock.SEGMENT, segment)
                .setValue(EnderHazeCropBlock.AGE, EnderHazeCropBlock.MAX_AGE);
    }

    /** An enderman that stands still and is angry at nobody. */
    private static Enderman calm(GameTestHelper context, BlockPos at) {
        Enderman enderman = context.spawn(EntityTypes.ENDERMAN, at);
        enderman.setNoAi(true);
        return enderman;
    }

    private static int seedStacks(GameTestHelper context) {
        AABB box = new AABB(context.absolutePos(ROOT)).inflate(4, 7, 4);
        return context.getLevel().getEntitiesOfClass(ItemEntity.class, box,
                item -> item.getItem().is(ModItems.ENDER_HAZE_SEEDS)).size();
    }

    private static void clearDrops(GameTestHelper context) {
        AABB box = new AABB(context.absolutePos(ROOT)).inflate(4, 7, 4);
        context.getLevel().getEntitiesOfClass(ItemEntity.class, box, item -> true).forEach(ItemEntity::discard);
    }

    private static void smoke(ServerPlayer player, ServerLevel world, Holder<Strain> strain) {
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        pipe.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, 1));
        pipe.set(ModComponents.CHARGES, DeviceType.PIPE.builtInBowl().hits());
        player.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        player.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(ModItems.WOODEN_PIPE));
        player.gameMode.useItem(player, world, pipe, InteractionHand.MAIN_HAND);
    }

    private static boolean has(List<MobEffectInstance> effects, Holder<MobEffect> effect) {
        return effects.stream().anyMatch(e -> e.getEffect().equals(effect));
    }

    private static int level(List<MobEffectInstance> effects, Holder<MobEffect> effect) {
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

    /** The encoded table names {@code item}, or does not, beside a control that it must. */
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

    /**
     * A wild patch as 26.1 writes it: the placed feature from {@code from} placement steps before its
     * scatter on. {@code -1} keeps the tries, so it is the patch from its centre; {@code +1} starts
     * after the scatter, so it is one plant and the filters it must pass.
     */
    private static net.minecraft.world.level.levelgen.placement.PlacedFeature wildPatch(ServerLevel world,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> key, int from) {
        var placed = world.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(key).value();
        int scatter = 0;
        while (!(placed.placement().get(scatter) instanceof net.minecraft.world.level.levelgen.placement.OffsetPlacement)) {
            scatter++;
        }
        return new net.minecraft.world.level.levelgen.placement.PlacedFeature(placed.feature(),
                placed.placement().subList(scatter + from, placed.placement().size()));
    }
}

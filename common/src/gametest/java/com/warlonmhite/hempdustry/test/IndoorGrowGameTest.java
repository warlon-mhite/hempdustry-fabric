package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.GrowPotBlock;
import com.warlonmhite.hempdustry.block.custom.HydroTrayBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Grow Pot and the light ladder. Every one of these fails silently in play: a record that
 * reads the lamp at harvest looks exactly like one that kept it, and a pot that never spends its
 * soil looks like a pot that is working.
 */
public final class IndoorGrowGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    /** Purple Kush stands two tall, so the first block a light can hang in is two above the LOWER. */
    private static final BlockPos LIGHT = CROP.above(2);

    /** Enough seeded rolls that the ambient bonus both paying and not paying is certain. */
    private static final int ROLLS = 40;
    private static final long SEED_SOURCE = 0x5EED1A4DL;

    // ----- the record -----

    public static void thePlantKeepsItsLightRecord(GameTestHelper context) {
        plant(context, litGrowLamp());
        grow(context, IndicaCropBlock.MAX_AGE);
        context.assertTrue(record(context) == GrowLight.GROW_LAMP,
                "a plant bone-mealed to ripeness under a lit Grow Lamp recorded " + record(context));

        // The path carryOver guards. A growth step re-derives the record, so it survives a broken
        // carryOver untouched; the random tick that re-sprouts a missing top half at the SAME age
        // does not. (Watched green against a carryOver that dropped the record until this was added.)
        // The lamp goes too: that same-age reconcile must not read the light, or a ripe plant would
        // be stressed for a lamp taken away after it had finished growing.
        ServerLevel tickWorld = context.getLevel();
        BlockPos ripe = context.absolutePos(CROP);
        context.setBlock(LIGHT, Blocks.AIR);
        context.setBlock(CROP.above(), Blocks.AIR);
        tickWorld.getBlockState(ripe).randomTick(tickWorld, ripe, tickWorld.getRandom());
        context.assertTrue(context.getBlockState(CROP.above()).is(ModBlocks.INDICA_CROP),
                "the random tick did not re-sprout the missing top half, so this probe proves nothing");
        context.assertTrue(record(context) == GrowLight.GROW_LAMP, "re-sprouting a ripe plant's top half"
                + " with its lamp gone changed its light record to " + record(context)
                + " — carryOver has to copy it, and a same-age reconcile must not read the light");

        // The cheat the record exists to stop: a lamp moved over a plant that grew without one.
        plant(context, Blocks.AIR.defaultBlockState());
        grow(context, IndicaCropBlock.MAX_AGE);
        context.setBlock(LIGHT, litGrowLamp());
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        for (int i = 0; i < 20; i++) {
            world.getBlockState(pos).randomTick(world, pos, world.getRandom());
        }
        context.assertTrue(record(context) == GrowLight.NATURAL,
                "a lamp moved over a ripe plant changed its record to " + record(context));

        plant(context, litGrowLamp());
        grow(context, GrowLight.FLOWERING_AGE);
        context.setBlock(LIGHT, Blocks.AIR);
        grow(context, 1);
        context.assertTrue(record(context) == GrowLight.STRESSED,
                "a plant that lost its lamp while flowering recorded " + record(context) + ", not STRESSED");
        // The upper half carries a copy of the record, because whatever draws a block reads only
        // that block. A stale copy is put right by the next random tick; the plant is ripe first so
        // the tick cannot grow it, leaving only the shape check to notice.
        context.assertTrue(context.getBlockState(CROP.above()).getValue(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "a stressed plant's upper half reads " + context.getBlockState(CROP.above()).getValue(GrowLight.PROPERTY));
        grow(context, IndicaCropBlock.MAX_AGE);
        context.setBlock(CROP.above(), context.getBlockState(CROP.above()).setValue(GrowLight.PROPERTY, GrowLight.NATURAL));
        world.getBlockState(pos).randomTick(world, pos, world.getRandom());
        context.assertTrue(context.getBlockState(CROP.above()).getValue(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "a random tick left a stale upper half reading " + context.getBlockState(CROP.above()).getValue(GrowLight.PROPERTY));

        plant(context, litGrowLamp());
        grow(context, 2);
        context.setBlock(LIGHT, Blocks.AIR);
        grow(context, 1);
        context.assertTrue(record(context) == GrowLight.NATURAL,
                "a plant that lost its lamp while young recorded " + record(context) + ", not NATURAL");

        plant(context, litGrowLamp());
        grow(context, 3);
        context.setBlock(LIGHT, Blocks.GLOWSTONE);
        grow(context, 1);
        context.setBlock(LIGHT, litGrowLamp());
        grow(context, 1);
        context.assertTrue(record(context) == GrowLight.AMBIENT, "a Grow Lamp swapped for glowstone"
                + " and back recorded " + record(context) + " — the weakest light it grew under must win");

        plant(context, Blocks.AIR.defaultBlockState());
        grow(context, 1);
        context.setBlock(LIGHT, litGrowLamp());
        grow(context, IndicaCropBlock.MAX_AGE - 1);
        context.assertTrue(record(context) == GrowLight.NATURAL,
                "a plant that sprouted with no lamp earned " + record(context) + " by getting one later");

        // The Grow Lamp's leaf share, read off the plant's record: two leaves a trim, not one.
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        int lampLeaves = trimLeaves(context, player, GrowLight.GROW_LAMP);
        int plainLeaves = trimLeaves(context, player, GrowLight.LAMP);
        context.assertTrue(lampLeaves == 2, "trimming a Grow Lamp plant dropped " + lampLeaves + " leaves, not 2");
        context.assertTrue(plainLeaves == 1, "trimming a lamp plant dropped " + plainLeaves + " leaves, not 1");

        // Lemon Haze stands three tall, so both the MIDDLE and the UPPER carry a copy — in a column of
        // its own, out of reach of anything above.
        BlockPos haze = CROP.east(3);
        BlockPos hazePos = context.absolutePos(haze);
        context.setBlock(haze.below(), moistFarmland());
        context.setBlock(haze, ModBlocks.SATIVA_CROP.defaultBlockState());
        context.setBlock(haze.above(4), Blocks.REDSTONE_BLOCK);
        context.setBlock(haze.above(3), litGrowLamp());
        grow(context, haze, GrowLight.FLOWERING_AGE);
        context.setBlock(haze.above(3), Blocks.AIR);
        grow(context, haze, SativaCropBlock.MAX_AGE);
        context.assertTrue(context.getBlockState(haze).getValue(GrowLight.PROPERTY) == GrowLight.STRESSED
                        && context.getBlockState(haze.above(2)).is(ModBlocks.SATIVA_CROP),
                "Lemon Haze did not grow three tall and stressed, so this probe proves nothing");
        for (int y = 1; y <= 2; y++) {
            GrowLight copy = context.getBlockState(haze.above(y)).getValue(GrowLight.PROPERTY);
            context.assertTrue(copy == GrowLight.STRESSED, "a stressed Lemon Haze reads " + copy + " " + y + " up");
        }
        // Both copies stale, as on a plant grown before the copy existed: one right copy alone would
        // trip a shape check that had stopped comparing the record, and hide it.
        for (int y = 1; y <= 2; y++) {
            context.setBlock(haze.above(y), context.getBlockState(haze.above(y)).setValue(GrowLight.PROPERTY, GrowLight.NATURAL));
        }
        world.getBlockState(hazePos).randomTick(world, hazePos, world.getRandom());
        for (int y = 1; y <= 2; y++) {
            GrowLight copy = context.getBlockState(haze.above(y)).getValue(GrowLight.PROPERTY);
            context.assertTrue(copy == GrowLight.STRESSED, "a random tick left Lemon Haze reading " + copy + " " + y + " up");
        }
        context.succeed();
    }

    // ----- the lamp itself -----

    /**
     * A powered Grow Lamp lights, and lights the world: the lamp's own LIT state, its luminance, and
     * the block light actually reaching the block under it once the light engine has caught up.
     * Reported in play as "powered, but emits no light".
     */
    public static void growLampLightsWhenPowered(GameTestHelper context) {
        BlockPos lamp = new BlockPos(1, 3, 1);
        context.assertTrue(ModBlocks.GROW_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true).getLightEmission() == 15,
                "a lit Grow Lamp's state reports luminance "
                        + ModBlocks.GROW_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true).getLightEmission() + ", not 15");
        context.setBlock(lamp.above(), Blocks.STONE);
        context.setBlock(lamp, ModBlocks.GROW_LAMP);
        context.setBlock(lamp.east(), Blocks.REDSTONE_BLOCK);
        context.runAfterDelay(10, () -> {
            context.assertTrue(context.getBlockState(lamp).getValue(BlockStateProperties.LIT), "a Grow Lamp beside a redstone block is not lit");
            int light = context.getLevel().getBrightness(LightLayer.BLOCK, context.absolutePos(lamp.below()));
            context.assertTrue(light >= 14, "a lit Grow Lamp lights the block under it to " + light + ", not 14");
            // It hangs: take the ceiling away and it comes down, as a hanging lantern does.
            context.setBlock(lamp.above(), Blocks.AIR);
            context.assertTrue(context.getBlockState(lamp).isAir(), "a Grow Lamp stayed up after its ceiling was removed");
            context.succeed();
        });
    }

    // ----- what counts as a light -----

    public static void lightIsReadOffTheBlock(GameTestHelper context) {
        classify(context, litGrowLamp(), GrowLight.GROW_LAMP, "a lit Grow Lamp");
        classify(context, ModBlocks.GROW_LAMP.defaultBlockState(), GrowLight.NATURAL, "an unlit Grow Lamp");
        classify(context, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true), GrowLight.LAMP, "a lit redstone lamp");
        classify(context, Blocks.REDSTONE_LAMP.defaultBlockState(), GrowLight.NATURAL, "an unlit redstone lamp");
        classify(context, Blocks.COPPER_BULB.defaultBlockState().setValue(BlockStateProperties.LIT, true), GrowLight.LAMP, "a lit copper bulb");
        classify(context, Blocks.OXIDIZED_COPPER_BULB.defaultBlockState().setValue(BlockStateProperties.LIT, true),
                GrowLight.NATURAL, "a lit oxidised copper bulb (light 4)");
        classify(context, Blocks.GLOWSTONE.defaultBlockState(), GrowLight.AMBIENT, "glowstone");
        classify(context, Blocks.SEA_LANTERN.defaultBlockState(), GrowLight.AMBIENT, "a sea lantern");
        classify(context, Blocks.LANTERN.defaultBlockState(), GrowLight.AMBIENT, "a lantern");
        classify(context, Blocks.TORCH.defaultBlockState(), GrowLight.NATURAL, "a torch (light 14)");
        classify(context, Blocks.CAMPFIRE.defaultBlockState(), GrowLight.NATURAL, "a lit campfire");
        classify(context, Blocks.FIRE.defaultBlockState(), GrowLight.NATURAL, "fire");
        classify(context, Blocks.LAVA.defaultBlockState(), GrowLight.NATURAL, "lava");

        ServerLevel world = context.getLevel();
        BlockPos plant = context.absolutePos(CROP);
        context.setBlock(LIGHT.offset(1, 0, 1), litGrowLamp());
        reach(context, world, plant, GrowLight.GROW_LAMP, "a Grow Lamp over the next plant along");
        context.setBlock(LIGHT.offset(1, 0, 1), Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.NATURAL, "glowstone over the next plant along");
        context.setBlock(LIGHT.offset(1, 0, 1), Blocks.AIR);

        context.setBlock(LIGHT, Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.AMBIENT, "glowstone straight overhead");
        context.setBlock(LIGHT, Blocks.STONE);
        context.setBlock(LIGHT.above(), Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.NATURAL, "glowstone behind a block of stone");
        context.setBlock(LIGHT, Blocks.GLASS);
        reach(context, world, plant, GrowLight.AMBIENT, "glowstone behind glass");
        context.setBlock(LIGHT, Blocks.AIR);
        context.setBlock(LIGHT.above(), Blocks.AIR);
        context.setBlock(LIGHT.above(2), Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.AMBIENT, "glowstone three blocks above the plant's top");
        context.setBlock(LIGHT.above(2), Blocks.AIR);
        context.setBlock(LIGHT.above(3), Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.NATURAL, "glowstone four blocks above the plant's top");
        context.succeed();
    }

    // ----- the pot -----

    public static void growPotFeedsAndIsSpent(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos soil = context.absolutePos(SOIL);
        context.setBlock(SOIL, ModBlocks.GROW_POT);
        context.assertTrue(fertility(context) == 0, "a freshly placed Grow Pot is not empty");

        ItemStack meal = new ItemStack(Items.BONE_MEAL, 8);
        for (int i = 0; i < GrowPotBlock.MAX_FERTILITY; i++) {
            context.assertTrue(BoneMealItem.growCrop(meal, world, soil),
                    "a Grow Pot at fertility " + i + " refused bone meal");
        }
        context.assertTrue(fertility(context) == GrowPotBlock.MAX_FERTILITY,
                "three bone meal left the pot at fertility " + fertility(context));
        context.assertTrue(!BoneMealItem.growCrop(meal, world, soil), "a full Grow Pot took bone meal");
        context.assertTrue(meal.getCount() == 8 - GrowPotBlock.MAX_FERTILITY,
                "a full Grow Pot ate the bone meal it refused");
        GrowPotBlock pot = (GrowPotBlock) ModBlocks.GROW_POT;
        context.setBlock(SOIL, context.getBlockState(SOIL).setValue(GrowPotBlock.WATERED, true));
        context.assertTrue(pot.moisture(context.getBlockState(SOIL)) == GrowPotBlock.WET_MOISTURE
                        && pot.speed(context.getBlockState(SOIL)) == GrowPotBlock.FERTILE_SPEED,
                "a fed, watered pot does not read as fertile soil");

        // Hemp, and only hemp. A glowstone beside the plant, so the light check in canSurvive
        // passes at any time of day.
        context.setBlock(CROP.east(), Blocks.GLOWSTONE);
        BlockPos crop = context.absolutePos(CROP);
        context.assertTrue(ModBlocks.INDICA_CROP.defaultBlockState().canSurvive(world, crop),
                "Purple Kush cannot be planted in a Grow Pot");
        context.assertTrue(ModBlocks.SATIVA_CROP.defaultBlockState().canSurvive(world, crop),
                "Lemon Haze cannot be planted in a Grow Pot");
        context.assertTrue(!Blocks.WHEAT.defaultBlockState().canSurvive(world, crop),
                "wheat can be planted in a Grow Pot — it is meant for hemp");

        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState());
        grow(context, IndicaCropBlock.MAX_AGE);
        context.assertTrue(fertility(context) == GrowPotBlock.MAX_FERTILITY - 1,
                "a plant ripening in a pot left it at fertility " + fertility(context) + " — one is spent per plant");
        context.assertTrue(!context.getBlockState(SOIL).getValue(GrowPotBlock.WATERED),
                "a plant ripening in a pot left it watered — it drinks the water");

        context.setBlock(SOIL, ModBlocks.GROW_POT.defaultBlockState());
        context.assertTrue(pot.moisture(context.getBlockState(SOIL)) == GrowPotBlock.DRY_MOISTURE
                        && pot.speed(context.getBlockState(SOIL)) == 1.0F,
                "a dry, spent pot still reads as good soil");
        context.succeed();
    }

    // ----- the tray -----

    /**
     * Water, nutrients and the pump — all three, or the tray is just a wet bed. And neglect only
     * ever costs time: vanilla never kills a planted crop by drying out, so neither may this.
     */
    public static void hydroTrayRunsOnItsPump(GameTestHelper context) {
        HydroTrayBlock tray = (HydroTrayBlock) ModBlocks.HYDRO_TRAY;
        BlockState solutionPumped = tray.defaultBlockState()
                .setValue(HydroTrayBlock.LEVEL, 3).setValue(HydroTrayBlock.FED, true).setValue(HydroTrayBlock.POWERED, true);
        BlockState solutionStill = solutionPumped.setValue(HydroTrayBlock.POWERED, false);
        BlockState plainWater = solutionPumped.setValue(HydroTrayBlock.FED, false);
        BlockState dry = tray.defaultBlockState().setValue(HydroTrayBlock.POWERED, true);

        context.assertTrue(tray.speed(solutionPumped) == HydroTrayBlock.PUMPED_SPEED,
                "a fed, pumped tray runs at " + tray.speed(solutionPumped) + "x, not " + HydroTrayBlock.PUMPED_SPEED + "x");
        context.assertTrue(tray.speed(solutionStill) == 1.0F,
                "an unpowered tray still runs at " + tray.speed(solutionStill) + "x — the pump is what buys the speed");
        context.assertTrue(tray.speed(plainWater) == 1.0F,
                "plain water runs at " + tray.speed(plainWater) + "x — nutrients are what make it a solution");
        context.assertTrue(tray.moisture(plainWater) == HydroTrayBlock.WET_MOISTURE,
                "a watered tray reads moisture " + tray.moisture(plainWater) + ", not " + HydroTrayBlock.WET_MOISTURE);
        context.assertTrue(tray.moisture(dry) == HydroTrayBlock.DRY_MOISTURE && tray.speed(dry) == 1.0F,
                "a dry tray is not reading as a dry bed");

        // Neglect costs time, never the plant.
        context.setBlock(SOIL, ModBlocks.HYDRO_TRAY);
        context.setBlock(CROP.above(), Blocks.AIR);
        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState());
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        for (int i = 0; i < 200; i++) {
            world.getBlockState(pos).randomTick(world, pos, world.getRandom());
        }
        context.assertTrue(context.getBlockState(CROP).is(ModBlocks.INDICA_CROP),
                "a plant left in a dry tray died — neglect must only slow a crop down");
        context.succeed();
    }

    /**
     * A water bucket fills the reservoir and hands back the empty — and, the bug this was written
     * for, <b>never lets vanilla place a water block in the plant's space</b>.
     */
    public static void aWaterBucketFillsATray(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(SOIL);
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        context.setBlock(SOIL, ModBlocks.HYDRO_TRAY);
        context.setBlock(CROP, Blocks.AIR);

        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        context.assertTrue(world.getBlockState(pos).useItemOn(bucket, world, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "a Hydro Tray refused a water bucket");
        context.assertTrue(level(context) == HydroTrayBlock.MAX_LEVEL,
                "a bucket of water left the tray at level " + level(context));
        context.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET),
                "filling a tray did not hand back an empty bucket");
        context.assertTrue(context.getBlockState(CROP).isAir(),
                "filling a tray placed water above it — the plant's own space");

        // A full tray takes no more — and must still accept the click, or the client goes on to the
        // bucket's own use and pours a water block into the plant's space (it fell through until
        // 2026-09-24, and a real client poured). It keeps the bucket full.
        ItemStack second = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, second);
        context.assertTrue(world.getBlockState(pos).useItemOn(second, world, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "a full tray let a water bucket click through — the client would pour it into the plant's space");
        context.assertTrue(second.is(Items.WATER_BUCKET) && level(context) == HydroTrayBlock.MAX_LEVEL,
                "a full tray emptied the bucket anyway");
        context.succeed();
    }

    /** Nutrients need water to go into, one charge is drunk per plant, and the last of it takes the feed with it. */
    public static void hydroTraySpendsALevelPerPlant(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos soil = context.absolutePos(SOIL);
        context.setBlock(SOIL, ModBlocks.HYDRO_TRAY);
        context.setBlock(CROP.above(), Blocks.AIR);
        context.setBlock(CROP, Blocks.AIR);

        ItemStack meal = new ItemStack(Items.BONE_MEAL, 8);
        context.assertTrue(!BoneMealItem.growCrop(meal, world, soil),
                "an empty tray took fertiliser — nutrients need water to go into");

        context.assertTrue(HydroTrayBlock.fill(world, soil, world.getBlockState(soil)), "a dry tray refused to fill");
        context.assertTrue(BoneMealItem.growCrop(meal, world, soil), "a watered tray refused fertiliser");
        context.assertTrue(fed(context), "fertiliser did not mix into the water");
        context.assertTrue(!BoneMealItem.growCrop(meal, world, soil), "a fed tray took fertiliser twice");
        context.assertTrue(meal.getCount() == 7, "feeding a tray took " + (8 - meal.getCount()) + " fertiliser, not 1");

        context.setBlock(CROP.east(), Blocks.GLOWSTONE);
        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState());
        grow(context, IndicaCropBlock.MAX_AGE);
        context.assertTrue(level(context) == HydroTrayBlock.MAX_LEVEL - 1,
                "a plant ripening in a tray left it at level " + level(context) + " — one charge is drunk per plant");
        context.assertTrue(fed(context), "one plant drained the nutrients; they last as long as the water does");

        // Drain the rest: the nutrients go with the last of the water.
        HydroTrayBlock tray = (HydroTrayBlock) ModBlocks.HYDRO_TRAY;
        for (int i = 0; i < 2; i++) {
            tray.spend(world, soil, world.getBlockState(soil));
        }
        context.assertTrue(level(context) == 0 && !fed(context),
                "an empty tray is still fed — the nutrients drain with the last of the water");
        context.succeed();
    }

    /**
     * Both beds are fed from <b>{@code #c:fertilizers}</b>, the convention tag, so a modpack's own
     * fertiliser works with neither side knowing about the other. Anything else must fall through to
     * {@code super} rather than a bare PASS, which would swallow the click.
     */
    public static void taggedFertiliserFeedsBothBeds(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(SOIL);
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        for (Block bed : new Block[]{ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(SOIL, bed);
            if (bed == ModBlocks.HYDRO_TRAY) {
                HydroTrayBlock.fill(world, pos, world.getBlockState(pos));
            }
            ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
            player.setItemInHand(InteractionHand.MAIN_HAND, meal);
            context.assertTrue(world.getBlockState(pos).useItemOn(meal, world, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                    bed.getDescriptionId() + " refused a fertiliser from the hand");
            context.assertTrue(meal.getCount() == 3,
                    bed.getDescriptionId() + " took " + (4 - meal.getCount()) + " fertiliser for one charge");
            context.assertTrue(charge(context, bed) == 1,
                    bed.getDescriptionId() + " did not take the feed");

            ItemStack stone = new ItemStack(Items.STONE);
            player.setItemInHand(InteractionHand.MAIN_HAND, stone);
            context.assertTrue(!world.getBlockState(pos).useItemOn(stone, world, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                    bed.getDescriptionId() + " accepted a block of stone as fertiliser");
            context.assertTrue(stone.getCount() == 1, bed.getDescriptionId() + " ate a block of stone");
        }
        context.succeed();
    }

    /** No hydroponics where water boils: the Nether gets soil or nothing. */
    public static void aTrayHoldsNothingInTheNether(GameTestHelper context) {
        ServerLevel overworld = context.getLevel();
        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        context.assertTrue(nether != null, "this server has no Nether, so this proves nothing");
        BlockPos pos = context.absolutePos(SOIL);
        BlockPos netherPos = new BlockPos(0, 64, 0);
        context.assertTrue(!HydroTrayBlock.boilsAway(overworld, pos), "solution boils away in the overworld");
        context.assertTrue(HydroTrayBlock.boilsAway(nether, netherPos), "solution keeps in the Nether — water does not");

        // A watered tray, so the test is about the dimension and not about being empty.
        BlockState watered = ModBlocks.HYDRO_TRAY.defaultBlockState().setValue(HydroTrayBlock.LEVEL, 3);
        HydroTrayBlock tray = (HydroTrayBlock) ModBlocks.HYDRO_TRAY;
        context.assertTrue(tray.isValidBonemealTarget(overworld, pos, watered), "a watered tray cannot be fed in the overworld");
        context.assertTrue(!tray.isValidBonemealTarget(nether, netherPos, watered), "a tray in the Nether can still be fed");

        nether.setBlockAndUpdate(netherPos, ModBlocks.HYDRO_TRAY.defaultBlockState());
        context.assertTrue(!HydroTrayBlock.fill(nether, netherPos, nether.getBlockState(netherPos)),
                "a tray in the Nether filled with water");
        context.succeed();
    }

    // ----- the harvest -----

    /**
     * The ladder against the <b>real</b> loot tables, on matched seeds — the light pools draw no
     * randoms except the ambient chance, so a plant's other drops come out identical and each tier
     * moves exactly what it says. The ambient bonus is asserted as a roll (it both pays and does
     * not), never as a rate.
     */
    public static void lightAndPotChangeTheHarvest(GameTestHelper context) {
        ladder(context, ModBlocks.INDICA_CROP.defaultBlockState()
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE), ModItems.INDICA_BUDS, ModItems.INDICA_SEEDS);
        ladder(context, ModBlocks.SATIVA_CROP.defaultBlockState()
                .setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                .setValue(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE), ModItems.SATIVA_BUDS, ModItems.SATIVA_SEEDS);
        context.succeed();
    }

    private static void ladder(GameTestHelper context, BlockState mature, Item buds, Item seeds) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(CROP));
        LootTable table = world.getServer().reloadableRegistries()
                .getLootTable(mature.getBlock().getLootTable().orElseThrow());
        String crop = mature.getBlock().getDescriptionId();
        BlockState natural = Defoliation.unworked(mature);
        java.util.Random seedSource = new java.util.Random(SEED_SOURCE);
        int ambientPaid = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seedSource.nextLong();
            context.setBlock(SOIL, Blocks.FARMLAND);
            int[] base = harvest(table, world, origin, natural, seed, buds, seeds);
            expect(context, harvest(table, world, origin, natural.setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP), seed, buds, seeds),
                    base, 2, 1, 0, 0, crop + " under a Grow Lamp");
            expect(context, harvest(table, world, origin, natural.setValue(GrowLight.PROPERTY, GrowLight.LAMP), seed, buds, seeds),
                    base, 1, 1, 0, 0, crop + " under a lamp");
            expect(context, harvest(table, world, origin, natural.setValue(GrowLight.PROPERTY, GrowLight.STRESSED), seed, buds, seeds),
                    base, -1, 0, 2, 0, crop + " stressed");
            int extra = harvest(table, world, origin, natural.setValue(GrowLight.PROPERTY, GrowLight.AMBIENT), seed, buds, seeds)[0] - base[0];
            context.assertTrue(extra == 0 || extra == 1, crop + " under glowstone: " + extra + " extra buds, expected 0 or 1");
            ambientPaid += extra;

            context.setBlock(SOIL, ModBlocks.GROW_POT);
            expect(context, harvest(table, world, origin, natural, seed, buds, seeds),
                    base, 0, 0, 0, -1, crop + " in a Grow Pot");
            // Root-bound is root-bound: a tray's net pot costs the same stem as the pot's soil.
            context.setBlock(SOIL, ModBlocks.HYDRO_TRAY);
            expect(context, harvest(table, world, origin, natural, seed, buds, seeds),
                    base, 0, 0, 0, -1, crop + " in a Hydro Tray");
        }
        context.assertTrue(ambientPaid > 0 && ambientPaid < ROLLS, crop + " under glowstone paid its bonus "
                + ambientPaid + " times in " + ROLLS + " — it has to be a roll, not a certainty either way");
    }

    private static void expect(GameTestHelper context, int[] got, int[] base,
                               int buds, int leaves, int seeds, int stems, String what) {
        int[] delta = {buds, leaves, seeds, stems};
        String[] names = {"buds", "leaves", "seeds", "stems"};
        for (int i = 0; i < delta.length; i++) {
            context.assertTrue(got[i] == base[i] + delta[i], what + ": " + got[i] + " " + names[i]
                    + " against " + base[i] + " for a plain plant, expected " + (delta[i] >= 0 ? "+" : "") + delta[i]);
        }
    }

    /** {buds, leaves, seeds, stems} from one roll of a crop's loot table on a given seed. */
    private static int[] harvest(LootTable table, ServerLevel world, Vec3 origin, BlockState state,
                                 long seed, Item buds, Item seeds) {
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);
        int[] counts = new int[4];
        for (ItemStack stack : table.getRandomItems(params, seed)) {
            // A bud that went to schwag is still a bud the plant grew; SchwagGameTest owns the split.
            if (stack.is(buds) || stack.is(ModItems.SCHWAG)) {
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

    // ----- helpers -----

    private static BlockState moistFarmland() {
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
    }

    private static BlockState litGrowLamp() {
        return ModBlocks.GROW_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true);
    }

    /**
     * A seedling on farmland with {@code light} hung over it, and a redstone block keeping a lamp lit.
     * The farmland is moist: bone-dry farmland can stress a flowering plant on its own, and this is
     * about the light alone.
     */
    private static void plant(GameTestHelper context, BlockState light) {
        context.setBlock(SOIL, moistFarmland());
        context.setBlock(CROP.above(), Blocks.AIR);
        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState());
        context.setBlock(LIGHT, light);
        context.setBlock(LIGHT.above(), Blocks.REDSTONE_BLOCK);
    }

    /** {@code steps} growth steps through growCrops — bone meal's path, without its 1-in-3 miss. */
    private static void grow(GameTestHelper context, int steps) {
        grow(context, CROP, steps);
    }

    private static void grow(GameTestHelper context, BlockPos crop, int steps) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(crop);
        for (int i = 0; i < steps; i++) {
            BlockState state = world.getBlockState(pos);
            ((CropBlock) state.getBlock()).growCrops(world, pos, state);
        }
    }

    private static GrowLight record(GameTestHelper context) {
        return context.getBlockState(CROP).getValue(GrowLight.PROPERTY);
    }

    private static int level(GameTestHelper context) {
        return context.getBlockState(SOIL).getValue(HydroTrayBlock.LEVEL);
    }

    private static boolean fed(GameTestHelper context) {
        return context.getBlockState(SOIL).getValue(HydroTrayBlock.FED);
    }

    /** Whatever the bed counts in: the pot's fertility, the tray's solution. */
    private static int charge(GameTestHelper context, Block bed) {
        BlockState state = context.getBlockState(SOIL);
        return bed == ModBlocks.HYDRO_TRAY
                ? (state.getValue(HydroTrayBlock.FED) ? 1 : 0)
                : state.getValue(GrowPotBlock.FERTILITY);
    }

    private static int fertility(GameTestHelper context) {
        return context.getBlockState(SOIL).getValue(GrowPotBlock.FERTILITY);
    }

    private static void classify(GameTestHelper context, BlockState state, GrowLight expected, String what) {
        GrowLight actual = GrowLight.of(state);
        context.assertTrue(actual == expected, what + " counts as " + actual + ", expected " + expected);
    }

    private static void reach(GameTestHelper context, ServerLevel world, BlockPos plant, GrowLight expected, String what) {
        GrowLight actual = GrowLight.over(world, plant, 2);
        context.assertTrue(actual == expected, what + " reads as " + actual + ", expected " + expected);
    }

    /** Shears one early trim off a fresh age-3 plant with {@code light} recorded, and counts the leaves. */
    private static int trimLeaves(GameTestHelper context, ServerPlayer player, GrowLight light) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        AABB area = AABB.encapsulatingFullBlocks(pos.offset(-3, -2, -3), pos.offset(3, 3, 3));
        world.getEntitiesOfClass(ItemEntity.class, area, e -> true).forEach(Entity::discard);
        context.setBlock(SOIL, Blocks.FARMLAND);
        context.setBlock(CROP.above(), Blocks.AIR);
        context.setBlock(CROP, Defoliation.unworked(ModBlocks.INDICA_CROP.defaultBlockState())
                .setValue(IndicaCropBlock.AGE, Defoliation.EARLY_MIN_AGE)
                .setValue(GrowLight.PROPERTY, light));
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        world.getBlockState(pos).useItemOn(shears, world, player, InteractionHand.MAIN_HAND, hit);
        int leaves = 0;
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, area, e -> true)) {
            if (item.getItem().is(ModItems.HEMP_LEAF)) {
                leaves += item.getItem().getCount();
            }
        }
        return leaves;
    }
}

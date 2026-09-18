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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.LightType;

/**
 * The Grow Pot and the light ladder. Every one of these fails silently in play: a record that
 * reads the lamp at harvest looks exactly like one that kept it, and a pot that never spends its
 * soil looks like a pot that is working.
 */
public final class IndoorGrowGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.up();
    /** Purple Kush stands two tall, so the first block a light can hang in is two above the LOWER. */
    private static final BlockPos LIGHT = CROP.up(2);

    /** Enough seeded rolls that the ambient bonus both paying and not paying is certain. */
    private static final int ROLLS = 40;
    private static final long SEED_SOURCE = 0x5EED1A4DL;

    // ----- the record -----

    public static void thePlantKeepsItsLightRecord(TestContext context) {
        plant(context, litGrowLamp());
        grow(context, IndicaCropBlock.MAX_AGE);
        context.assertTrue(record(context) == GrowLight.GROW_LAMP,
                "a plant bone-mealed to ripeness under a lit Grow Lamp recorded " + record(context));

        // The path carryOver guards. A growth step re-derives the record, so it survives a broken
        // carryOver untouched; the random tick that re-sprouts a missing top half at the SAME age
        // does not. (Watched green against a carryOver that dropped the record until this was added.)
        ServerWorld tickWorld = context.getWorld();
        BlockPos ripe = context.getAbsolutePos(CROP);
        context.setBlockState(CROP.up(), Blocks.AIR);
        tickWorld.getBlockState(ripe).randomTick(tickWorld, ripe, tickWorld.getRandom());
        context.assertTrue(context.getBlockState(CROP.up()).isOf(ModBlocks.INDICA_CROP),
                "the random tick did not re-sprout the missing top half, so this probe proves nothing");
        context.assertTrue(record(context) == GrowLight.GROW_LAMP, "re-sprouting a ripe plant's top half"
                + " wiped its light record to " + record(context) + " — carryOver has to copy it");

        // The cheat the record exists to stop: a lamp moved over a plant that grew without one.
        plant(context, Blocks.AIR.getDefaultState());
        grow(context, IndicaCropBlock.MAX_AGE);
        context.setBlockState(LIGHT, litGrowLamp());
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(CROP);
        for (int i = 0; i < 20; i++) {
            world.getBlockState(pos).randomTick(world, pos, world.getRandom());
        }
        context.assertTrue(record(context) == GrowLight.NATURAL,
                "a lamp moved over a ripe plant changed its record to " + record(context));

        plant(context, litGrowLamp());
        grow(context, GrowLight.FLOWERING_AGE);
        context.setBlockState(LIGHT, Blocks.AIR);
        grow(context, 1);
        context.assertTrue(record(context) == GrowLight.STRESSED,
                "a plant that lost its lamp while flowering recorded " + record(context) + ", not STRESSED");

        plant(context, litGrowLamp());
        grow(context, 2);
        context.setBlockState(LIGHT, Blocks.AIR);
        grow(context, 1);
        context.assertTrue(record(context) == GrowLight.NATURAL,
                "a plant that lost its lamp while young recorded " + record(context) + ", not NATURAL");

        plant(context, litGrowLamp());
        grow(context, 3);
        context.setBlockState(LIGHT, Blocks.GLOWSTONE);
        grow(context, 1);
        context.setBlockState(LIGHT, litGrowLamp());
        grow(context, 1);
        context.assertTrue(record(context) == GrowLight.AMBIENT, "a Grow Lamp swapped for glowstone"
                + " and back recorded " + record(context) + " — the weakest light it grew under must win");

        plant(context, Blocks.AIR.getDefaultState());
        grow(context, 1);
        context.setBlockState(LIGHT, litGrowLamp());
        grow(context, IndicaCropBlock.MAX_AGE - 1);
        context.assertTrue(record(context) == GrowLight.NATURAL,
                "a plant that sprouted with no lamp earned " + record(context) + " by getting one later");

        // The Grow Lamp's leaf share, read off the plant's record: two leaves a trim, not one.
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        int lampLeaves = trimLeaves(context, player, GrowLight.GROW_LAMP);
        int plainLeaves = trimLeaves(context, player, GrowLight.LAMP);
        context.assertTrue(lampLeaves == 2, "trimming a Grow Lamp plant dropped " + lampLeaves + " leaves, not 2");
        context.assertTrue(plainLeaves == 1, "trimming a lamp plant dropped " + plainLeaves + " leaves, not 1");
        context.complete();
    }

    // ----- the lamp itself -----

    /**
     * A powered Grow Lamp lights, and lights the world: the lamp's own LIT state, its luminance, and
     * the block light actually reaching the block under it once the light engine has caught up.
     * Reported in play as "powered, but emits no light".
     */
    public static void growLampLightsWhenPowered(TestContext context) {
        BlockPos lamp = new BlockPos(1, 3, 1);
        context.assertTrue(ModBlocks.GROW_LAMP.getDefaultState().with(Properties.LIT, true).getLuminance() == 15,
                "a lit Grow Lamp's state reports luminance "
                        + ModBlocks.GROW_LAMP.getDefaultState().with(Properties.LIT, true).getLuminance() + ", not 15");
        context.setBlockState(lamp.up(), Blocks.STONE);
        context.setBlockState(lamp, ModBlocks.GROW_LAMP);
        context.setBlockState(lamp.east(), Blocks.REDSTONE_BLOCK);
        context.waitAndRun(10, () -> {
            context.assertTrue(context.getBlockState(lamp).get(Properties.LIT), "a Grow Lamp beside a redstone block is not lit");
            int light = context.getWorld().getLightLevel(LightType.BLOCK, context.getAbsolutePos(lamp.down()));
            context.assertTrue(light >= 14, "a lit Grow Lamp lights the block under it to " + light + ", not 14");
            // It hangs: take the ceiling away and it comes down, as a hanging lantern does.
            context.setBlockState(lamp.up(), Blocks.AIR);
            context.assertTrue(context.getBlockState(lamp).isAir(), "a Grow Lamp stayed up after its ceiling was removed");
            context.complete();
        });
    }

    // ----- what counts as a light -----

    public static void lightIsReadOffTheBlock(TestContext context) {
        classify(context, litGrowLamp(), GrowLight.GROW_LAMP, "a lit Grow Lamp");
        classify(context, ModBlocks.GROW_LAMP.getDefaultState(), GrowLight.NATURAL, "an unlit Grow Lamp");
        classify(context, Blocks.REDSTONE_LAMP.getDefaultState().with(Properties.LIT, true), GrowLight.LAMP, "a lit redstone lamp");
        classify(context, Blocks.REDSTONE_LAMP.getDefaultState(), GrowLight.NATURAL, "an unlit redstone lamp");
        classify(context, Blocks.COPPER_BULB.getDefaultState().with(Properties.LIT, true), GrowLight.LAMP, "a lit copper bulb");
        classify(context, Blocks.OXIDIZED_COPPER_BULB.getDefaultState().with(Properties.LIT, true),
                GrowLight.NATURAL, "a lit oxidised copper bulb (light 4)");
        classify(context, Blocks.GLOWSTONE.getDefaultState(), GrowLight.AMBIENT, "glowstone");
        classify(context, Blocks.SEA_LANTERN.getDefaultState(), GrowLight.AMBIENT, "a sea lantern");
        classify(context, Blocks.LANTERN.getDefaultState(), GrowLight.AMBIENT, "a lantern");
        classify(context, Blocks.TORCH.getDefaultState(), GrowLight.NATURAL, "a torch (light 14)");
        classify(context, Blocks.CAMPFIRE.getDefaultState(), GrowLight.NATURAL, "a lit campfire");
        classify(context, Blocks.FIRE.getDefaultState(), GrowLight.NATURAL, "fire");
        classify(context, Blocks.LAVA.getDefaultState(), GrowLight.NATURAL, "lava");

        ServerWorld world = context.getWorld();
        BlockPos plant = context.getAbsolutePos(CROP);
        context.setBlockState(LIGHT.add(1, 0, 1), litGrowLamp());
        reach(context, world, plant, GrowLight.GROW_LAMP, "a Grow Lamp over the next plant along");
        context.setBlockState(LIGHT.add(1, 0, 1), Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.NATURAL, "glowstone over the next plant along");
        context.setBlockState(LIGHT.add(1, 0, 1), Blocks.AIR);

        context.setBlockState(LIGHT, Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.AMBIENT, "glowstone straight overhead");
        context.setBlockState(LIGHT, Blocks.STONE);
        context.setBlockState(LIGHT.up(), Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.NATURAL, "glowstone behind a block of stone");
        context.setBlockState(LIGHT, Blocks.GLASS);
        reach(context, world, plant, GrowLight.AMBIENT, "glowstone behind glass");
        context.setBlockState(LIGHT, Blocks.AIR);
        context.setBlockState(LIGHT.up(), Blocks.AIR);
        context.setBlockState(LIGHT.up(2), Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.AMBIENT, "glowstone three blocks above the plant's top");
        context.setBlockState(LIGHT.up(2), Blocks.AIR);
        context.setBlockState(LIGHT.up(3), Blocks.GLOWSTONE);
        reach(context, world, plant, GrowLight.NATURAL, "glowstone four blocks above the plant's top");
        context.complete();
    }

    // ----- the pot -----

    public static void growPotFeedsAndIsSpent(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos soil = context.getAbsolutePos(SOIL);
        context.setBlockState(SOIL, ModBlocks.GROW_POT);
        context.assertTrue(fertility(context) == 0, "a freshly placed Grow Pot is not empty");

        ItemStack meal = new ItemStack(Items.BONE_MEAL, 8);
        for (int i = 0; i < GrowPotBlock.MAX_FERTILITY; i++) {
            context.assertTrue(BoneMealItem.useOnFertilizable(meal, world, soil),
                    "a Grow Pot at fertility " + i + " refused bone meal");
        }
        context.assertTrue(fertility(context) == GrowPotBlock.MAX_FERTILITY,
                "three bone meal left the pot at fertility " + fertility(context));
        context.assertTrue(!BoneMealItem.useOnFertilizable(meal, world, soil), "a full Grow Pot took bone meal");
        context.assertTrue(meal.getCount() == 8 - GrowPotBlock.MAX_FERTILITY,
                "a full Grow Pot ate the bone meal it refused");
        GrowPotBlock pot = (GrowPotBlock) ModBlocks.GROW_POT;
        context.assertTrue(pot.moisture(context.getBlockState(SOIL)) == GrowPotBlock.FERTILE_MOISTURE
                        && pot.speed(context.getBlockState(SOIL)) == GrowPotBlock.FERTILE_SPEED,
                "a fertile pot does not read as fertile soil");

        // Hemp, and only hemp. A glowstone beside the plant, so the light check in canPlaceAt
        // passes at any time of day.
        context.setBlockState(CROP.east(), Blocks.GLOWSTONE);
        BlockPos crop = context.getAbsolutePos(CROP);
        context.assertTrue(ModBlocks.INDICA_CROP.getDefaultState().canPlaceAt(world, crop),
                "Purple Kush cannot be planted in a Grow Pot");
        context.assertTrue(ModBlocks.SATIVA_CROP.getDefaultState().canPlaceAt(world, crop),
                "Lemon Haze cannot be planted in a Grow Pot");
        context.assertTrue(!Blocks.WHEAT.getDefaultState().canPlaceAt(world, crop),
                "wheat can be planted in a Grow Pot — it is meant for hemp");

        context.setBlockState(CROP, ModBlocks.INDICA_CROP.getDefaultState());
        grow(context, IndicaCropBlock.MAX_AGE);
        context.assertTrue(fertility(context) == GrowPotBlock.MAX_FERTILITY - 1,
                "a plant ripening in a pot left it at fertility " + fertility(context) + " — one is spent per plant");

        context.setBlockState(SOIL, ModBlocks.GROW_POT.getDefaultState());
        context.assertTrue(pot.moisture(context.getBlockState(SOIL)) == GrowPotBlock.SPENT_MOISTURE
                        && pot.speed(context.getBlockState(SOIL)) == 1.0F,
                "a spent pot still reads as fertile soil");
        context.complete();
    }

    // ----- the tray -----

    /**
     * Water, nutrients and the pump — all three, or the tray is just a wet bed. And neglect only
     * ever costs time: vanilla never kills a planted crop by drying out, so neither may this.
     */
    public static void hydroTrayRunsOnItsPump(TestContext context) {
        HydroTrayBlock tray = (HydroTrayBlock) ModBlocks.HYDRO_TRAY;
        BlockState solutionPumped = tray.getDefaultState()
                .with(HydroTrayBlock.LEVEL, 3).with(HydroTrayBlock.FED, true).with(HydroTrayBlock.POWERED, true);
        BlockState solutionStill = solutionPumped.with(HydroTrayBlock.POWERED, false);
        BlockState plainWater = solutionPumped.with(HydroTrayBlock.FED, false);
        BlockState dry = tray.getDefaultState().with(HydroTrayBlock.POWERED, true);

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
        context.setBlockState(SOIL, ModBlocks.HYDRO_TRAY);
        context.setBlockState(CROP.up(), Blocks.AIR);
        context.setBlockState(CROP, ModBlocks.INDICA_CROP.getDefaultState());
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(CROP);
        for (int i = 0; i < 200; i++) {
            world.getBlockState(pos).randomTick(world, pos, world.getRandom());
        }
        context.assertTrue(context.getBlockState(CROP).isOf(ModBlocks.INDICA_CROP),
                "a plant left in a dry tray died — neglect must only slow a crop down");
        context.complete();
    }

    /**
     * A water bucket fills the reservoir and hands back the empty — and, the bug this was written
     * for, <b>never lets vanilla place a water block in the plant's space</b>.
     */
    public static void aWaterBucketFillsATray(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(SOIL);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        context.setBlockState(SOIL, ModBlocks.HYDRO_TRAY);
        context.setBlockState(CROP, Blocks.AIR);

        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        player.setStackInHand(Hand.MAIN_HAND, bucket);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        context.assertTrue(world.getBlockState(pos).onUseWithItem(bucket, world, player, Hand.MAIN_HAND, hit).isAccepted(),
                "a Hydro Tray refused a water bucket");
        context.assertTrue(level(context) == HydroTrayBlock.MAX_LEVEL,
                "a bucket of water left the tray at level " + level(context));
        context.assertTrue(player.getStackInHand(Hand.MAIN_HAND).isOf(Items.BUCKET),
                "filling a tray did not hand back an empty bucket");
        context.assertTrue(context.getBlockState(CROP).isAir(),
                "filling a tray placed water above it — the plant's own space");

        // A full tray takes no more, and says so by falling through rather than eating the bucket.
        ItemStack second = new ItemStack(Items.WATER_BUCKET);
        player.setStackInHand(Hand.MAIN_HAND, second);
        context.assertTrue(!world.getBlockState(pos).onUseWithItem(second, world, player, Hand.MAIN_HAND, hit).isAccepted(),
                "a full tray accepted another bucket");
        context.assertTrue(second.isOf(Items.WATER_BUCKET), "a full tray emptied the bucket anyway");
        context.complete();
    }

    /** Nutrients need water to go into, one charge is drunk per plant, and the last of it takes the feed with it. */
    public static void hydroTraySpendsALevelPerPlant(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos soil = context.getAbsolutePos(SOIL);
        context.setBlockState(SOIL, ModBlocks.HYDRO_TRAY);
        context.setBlockState(CROP.up(), Blocks.AIR);
        context.setBlockState(CROP, Blocks.AIR);

        ItemStack meal = new ItemStack(Items.BONE_MEAL, 8);
        context.assertTrue(!BoneMealItem.useOnFertilizable(meal, world, soil),
                "an empty tray took fertiliser — nutrients need water to go into");

        context.assertTrue(HydroTrayBlock.fill(world, soil, world.getBlockState(soil)), "a dry tray refused to fill");
        context.assertTrue(BoneMealItem.useOnFertilizable(meal, world, soil), "a watered tray refused fertiliser");
        context.assertTrue(fed(context), "fertiliser did not mix into the water");
        context.assertTrue(!BoneMealItem.useOnFertilizable(meal, world, soil), "a fed tray took fertiliser twice");
        context.assertTrue(meal.getCount() == 7, "feeding a tray took " + (8 - meal.getCount()) + " fertiliser, not 1");

        context.setBlockState(CROP.east(), Blocks.GLOWSTONE);
        context.setBlockState(CROP, ModBlocks.INDICA_CROP.getDefaultState());
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
        context.complete();
    }

    /**
     * Both beds are fed from <b>{@code #c:fertilizers}</b>, the convention tag, so a modpack's own
     * fertiliser works with neither side knowing about the other. Anything else must fall through to
     * {@code super} rather than a bare PASS, which would swallow the click.
     */
    public static void taggedFertiliserFeedsBothBeds(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(SOIL);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);

        for (Block bed : new Block[]{ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlockState(SOIL, bed);
            if (bed == ModBlocks.HYDRO_TRAY) {
                HydroTrayBlock.fill(world, pos, world.getBlockState(pos));
            }
            ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
            player.setStackInHand(Hand.MAIN_HAND, meal);
            context.assertTrue(world.getBlockState(pos).onUseWithItem(meal, world, player, Hand.MAIN_HAND, hit).isAccepted(),
                    bed.getTranslationKey() + " refused a fertiliser from the hand");
            context.assertTrue(meal.getCount() == 3,
                    bed.getTranslationKey() + " took " + (4 - meal.getCount()) + " fertiliser for one charge");
            context.assertTrue(charge(context, bed) == 1,
                    bed.getTranslationKey() + " did not take the feed");

            ItemStack stone = new ItemStack(Items.STONE);
            player.setStackInHand(Hand.MAIN_HAND, stone);
            context.assertTrue(!world.getBlockState(pos).onUseWithItem(stone, world, player, Hand.MAIN_HAND, hit).isAccepted(),
                    bed.getTranslationKey() + " accepted a block of stone as fertiliser");
            context.assertTrue(stone.getCount() == 1, bed.getTranslationKey() + " ate a block of stone");
        }
        context.complete();
    }

    /** No hydroponics where water boils: the Nether gets soil or nothing. */
    public static void aTrayHoldsNothingInTheNether(TestContext context) {
        ServerWorld overworld = context.getWorld();
        ServerWorld nether = overworld.getServer().getWorld(World.NETHER);
        context.assertTrue(nether != null, "this server has no Nether, so this proves nothing");
        BlockPos pos = context.getAbsolutePos(SOIL);
        BlockPos netherPos = new BlockPos(0, 64, 0);
        context.assertTrue(!HydroTrayBlock.boilsAway(overworld, pos), "solution boils away in the overworld");
        context.assertTrue(HydroTrayBlock.boilsAway(nether, netherPos), "solution keeps in the Nether — water does not");

        // A watered tray, so the test is about the dimension and not about being empty.
        BlockState watered = ModBlocks.HYDRO_TRAY.getDefaultState().with(HydroTrayBlock.LEVEL, 3);
        HydroTrayBlock tray = (HydroTrayBlock) ModBlocks.HYDRO_TRAY;
        context.assertTrue(tray.isFertilizable(overworld, pos, watered), "a watered tray cannot be fed in the overworld");
        context.assertTrue(!tray.isFertilizable(nether, netherPos, watered), "a tray in the Nether can still be fed");

        nether.setBlockState(netherPos, ModBlocks.HYDRO_TRAY.getDefaultState());
        context.assertTrue(!HydroTrayBlock.fill(nether, netherPos, nether.getBlockState(netherPos)),
                "a tray in the Nether filled with water");
        context.complete();
    }

    // ----- the harvest -----

    /**
     * The ladder against the <b>real</b> loot tables, on matched seeds — the light pools draw no
     * randoms except the ambient chance, so a plant's other drops come out identical and each tier
     * moves exactly what it says. The ambient bonus is asserted as a roll (it both pays and does
     * not), never as a rate.
     */
    public static void lightAndPotChangeTheHarvest(TestContext context) {
        ladder(context, ModBlocks.INDICA_CROP.getDefaultState()
                .with(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .with(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE), ModItems.INDICA_BUDS, ModItems.INDICA_SEEDS);
        ladder(context, ModBlocks.SATIVA_CROP.getDefaultState()
                .with(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                .with(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE), ModItems.SATIVA_BUDS, ModItems.SATIVA_SEEDS);
        context.complete();
    }

    private static void ladder(TestContext context, BlockState mature, Item buds, Item seeds) {
        ServerWorld world = context.getWorld();
        Vec3d origin = Vec3d.ofCenter(context.getAbsolutePos(CROP));
        LootTable table = world.getServer().getReloadableRegistries()
                .getLootTable(mature.getBlock().getLootTableKey().orElseThrow());
        String crop = mature.getBlock().getTranslationKey();
        BlockState natural = Defoliation.unworked(mature);
        java.util.Random seedSource = new java.util.Random(SEED_SOURCE);
        int ambientPaid = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seedSource.nextLong();
            context.setBlockState(SOIL, Blocks.FARMLAND);
            int[] base = harvest(table, world, origin, natural, seed, buds, seeds);
            expect(context, harvest(table, world, origin, natural.with(GrowLight.PROPERTY, GrowLight.GROW_LAMP), seed, buds, seeds),
                    base, 2, 1, 0, 0, crop + " under a Grow Lamp");
            expect(context, harvest(table, world, origin, natural.with(GrowLight.PROPERTY, GrowLight.LAMP), seed, buds, seeds),
                    base, 1, 1, 0, 0, crop + " under a lamp");
            expect(context, harvest(table, world, origin, natural.with(GrowLight.PROPERTY, GrowLight.STRESSED), seed, buds, seeds),
                    base, -1, 0, 2, 0, crop + " stressed");
            int extra = harvest(table, world, origin, natural.with(GrowLight.PROPERTY, GrowLight.AMBIENT), seed, buds, seeds)[0] - base[0];
            context.assertTrue(extra == 0 || extra == 1, crop + " under glowstone: " + extra + " extra buds, expected 0 or 1");
            ambientPaid += extra;

            context.setBlockState(SOIL, ModBlocks.GROW_POT);
            expect(context, harvest(table, world, origin, natural, seed, buds, seeds),
                    base, 0, 0, 0, -1, crop + " in a Grow Pot");
            // Root-bound is root-bound: a tray's net pot costs the same stem as the pot's soil.
            context.setBlockState(SOIL, ModBlocks.HYDRO_TRAY);
            expect(context, harvest(table, world, origin, natural, seed, buds, seeds),
                    base, 0, 0, 0, -1, crop + " in a Hydro Tray");
        }
        context.assertTrue(ambientPaid > 0 && ambientPaid < ROLLS, crop + " under glowstone paid its bonus "
                + ambientPaid + " times in " + ROLLS + " — it has to be a roll, not a certainty either way");
    }

    private static void expect(TestContext context, int[] got, int[] base,
                               int buds, int leaves, int seeds, int stems, String what) {
        int[] delta = {buds, leaves, seeds, stems};
        String[] names = {"buds", "leaves", "seeds", "stems"};
        for (int i = 0; i < delta.length; i++) {
            context.assertTrue(got[i] == base[i] + delta[i], what + ": " + got[i] + " " + names[i]
                    + " against " + base[i] + " for a plain plant, expected " + (delta[i] >= 0 ? "+" : "") + delta[i]);
        }
    }

    /** {buds, leaves, seeds, stems} from one roll of a crop's loot table on a given seed. */
    private static int[] harvest(LootTable table, ServerWorld world, Vec3d origin, BlockState state,
                                 long seed, Item buds, Item seeds) {
        LootWorldContext params = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, origin)
                .add(LootContextParameters.BLOCK_STATE, state)
                .add(LootContextParameters.TOOL, ItemStack.EMPTY)
                .build(LootContextTypes.BLOCK);
        int[] counts = new int[4];
        for (ItemStack stack : table.generateLoot(params, seed)) {
            if (stack.isOf(buds)) {
                counts[0] += stack.getCount();
            } else if (stack.isOf(ModItems.HEMP_LEAF)) {
                counts[1] += stack.getCount();
            } else if (stack.isOf(seeds)) {
                counts[2] += stack.getCount();
            } else if (stack.isOf(ModItems.HEMP_STEM)) {
                counts[3] += stack.getCount();
            }
        }
        return counts;
    }

    // ----- helpers -----

    private static BlockState litGrowLamp() {
        return ModBlocks.GROW_LAMP.getDefaultState().with(Properties.LIT, true);
    }

    /** A seedling on farmland with {@code light} hung over it, and a redstone block keeping a lamp lit. */
    private static void plant(TestContext context, BlockState light) {
        context.setBlockState(SOIL, Blocks.FARMLAND);
        context.setBlockState(CROP.up(), Blocks.AIR);
        context.setBlockState(CROP, ModBlocks.INDICA_CROP.getDefaultState());
        context.setBlockState(LIGHT, light);
        context.setBlockState(LIGHT.up(), Blocks.REDSTONE_BLOCK);
    }

    /** {@code steps} growth steps through applyGrowth — bone meal's path, without its 1-in-3 miss. */
    private static void grow(TestContext context, int steps) {
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(CROP);
        for (int i = 0; i < steps; i++) {
            BlockState state = world.getBlockState(pos);
            ((CropBlock) state.getBlock()).applyGrowth(world, pos, state);
        }
    }

    private static GrowLight record(TestContext context) {
        return context.getBlockState(CROP).get(GrowLight.PROPERTY);
    }

    private static int level(TestContext context) {
        return context.getBlockState(SOIL).get(HydroTrayBlock.LEVEL);
    }

    private static boolean fed(TestContext context) {
        return context.getBlockState(SOIL).get(HydroTrayBlock.FED);
    }

    /** Whatever the bed counts in: the pot's fertility, the tray's solution. */
    private static int charge(TestContext context, Block bed) {
        BlockState state = context.getBlockState(SOIL);
        return bed == ModBlocks.HYDRO_TRAY
                ? (state.get(HydroTrayBlock.FED) ? 1 : 0)
                : state.get(GrowPotBlock.FERTILITY);
    }

    private static int fertility(TestContext context) {
        return context.getBlockState(SOIL).get(GrowPotBlock.FERTILITY);
    }

    private static void classify(TestContext context, BlockState state, GrowLight expected, String what) {
        GrowLight actual = GrowLight.of(state);
        context.assertTrue(actual == expected, what + " counts as " + actual + ", expected " + expected);
    }

    private static void reach(TestContext context, ServerWorld world, BlockPos plant, GrowLight expected, String what) {
        GrowLight actual = GrowLight.over(world, plant, 2);
        context.assertTrue(actual == expected, what + " reads as " + actual + ", expected " + expected);
    }

    /** Shears one early trim off a fresh age-3 plant with {@code light} recorded, and counts the leaves. */
    private static int trimLeaves(TestContext context, ServerPlayerEntity player, GrowLight light) {
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(CROP);
        Box area = Box.enclosing(pos.add(-3, -2, -3), pos.add(3, 3, 3));
        world.getEntitiesByClass(ItemEntity.class, area, e -> true).forEach(Entity::discard);
        context.setBlockState(SOIL, Blocks.FARMLAND);
        context.setBlockState(CROP.up(), Blocks.AIR);
        context.setBlockState(CROP, Defoliation.unworked(ModBlocks.INDICA_CROP.getDefaultState())
                .with(IndicaCropBlock.AGE, Defoliation.EARLY_MIN_AGE)
                .with(GrowLight.PROPERTY, light));
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setStackInHand(Hand.MAIN_HAND, shears);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        world.getBlockState(pos).onUseWithItem(shears, world, player, Hand.MAIN_HAND, hit);
        int leaves = 0;
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, area, e -> true)) {
            if (item.getStack().isOf(ModItems.HEMP_LEAF)) {
                leaves += item.getStack().getCount();
            }
        }
        return leaves;
    }
}

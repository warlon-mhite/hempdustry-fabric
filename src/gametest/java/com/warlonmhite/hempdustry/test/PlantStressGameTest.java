package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.GrowPotBlock;
import com.warlonmhite.hempdustry.block.custom.HydroTrayBlock;
import com.warlonmhite.hempdustry.block.custom.PlantStress;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.List;

/**
 * Stress beyond the light, and the Grow Pot's water. Both fail silently in play: a roll that never
 * fires looks exactly like a player who was lucky, and a pot that never dries looks like one that
 * was watered.
 *
 * <p>The odds are asserted as numbers, never measured — a measured rate would be measuring the RNG.
 * The wiring is driven with a random that decides every roll: {@link #HIT} lands under any odds above
 * zero and {@link #MISS} under none, so each step either certainly stresses or certainly does not.
 */
public final class PlantStressGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.up();
    /** Where a light over a Purple Kush hangs: two above its LOWER. */
    private static final BlockPos LIGHT = CROP.up(2);

    private static final Random HIT = decided(0.0F);
    private static final Random MISS = decided(0.999F);

    // ----- the causes -----

    public static void stressHasMoreCausesThanLight(TestContext context) {
        // The agreed numbers.
        context.assertTrue(PlantStress.OVERFED == 1.0F / 9 && PlantStress.OVERWATERED == 1.0F / 4
                        && PlantStress.DRY_BED == 1.0F / 3 && PlantStress.STAGNANT == 1.0F / 6
                        && PlantStress.DRY_FIELD == 1.0F / 20,
                "the stress odds are not 1 in 9 overfed, 1 in 4 overwatered, 1 in 3 dry bed, 1 in 6 stagnant, 1 in 20 dry field");

        BlockState dryField = Blocks.FARMLAND.getDefaultState();
        BlockState pot = ModBlocks.GROW_POT.getDefaultState();
        BlockState tray = ModBlocks.HYDRO_TRAY.getDefaultState();
        odds(context, dryField, PlantStress.DRY_FIELD, "bone-dry farmland");
        odds(context, moist(), 0.0F, "moist farmland");
        odds(context, dryField.with(FarmlandBlock.MOISTURE, 1), 0.0F, "farmland drying out but not yet dry");
        odds(context, Blocks.SAND.getDefaultState(), 0.0F, "sand");
        odds(context, pot, PlantStress.DRY_BED, "a dry Grow Pot");
        odds(context, pot.with(GrowPotBlock.WATERED, true), 0.0F, "a watered Grow Pot");
        odds(context, tray.with(HydroTrayBlock.POWERED, true), PlantStress.DRY_BED, "a dry tray, pump running");
        odds(context, tray.with(HydroTrayBlock.LEVEL, 3), PlantStress.STAGNANT, "a watered tray with its pump off");
        odds(context, tray.with(HydroTrayBlock.LEVEL, 3).with(HydroTrayBlock.FED, true), PlantStress.STAGNANT,
                "a fed tray with its pump off");
        odds(context, tray.with(HydroTrayBlock.LEVEL, 3).with(HydroTrayBlock.POWERED, true), 0.0F,
                "a watered tray with its pump running");

        // A field plant can be stressed now — but only flowering, only bone dry, and only on a hit.
        Block indica = ModBlocks.INDICA_CROP;
        expect(context, step(context, indica, dryField, 3, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "a plain field plant stepping into flowering on bone-dry farmland");
        context.assertTrue(context.getBlockState(CROP.up()).get(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "a plant stressed by its ground is drawn healthy at the top — the upper half reads "
                        + context.getBlockState(CROP.up()).get(GrowLight.PROPERTY));
        expect(context, step(context, indica, dryField, 2, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "a young plant stepping on bone-dry farmland");
        expect(context, step(context, indica, moist(), 3, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "a flowering plant on moist farmland");
        expect(context, step(context, indica, dryField, 3, GrowLight.NATURAL, MISS), GrowLight.NATURAL,
                "a missed roll on bone-dry farmland");

        // The beds.
        BlockState wetPot = pot.with(GrowPotBlock.WATERED, true);
        BlockState pumped = tray.with(HydroTrayBlock.LEVEL, 3).with(HydroTrayBlock.POWERED, true);
        expect(context, step(context, indica, pot, 4, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "a flowering plant in a dry Grow Pot");
        expect(context, step(context, indica, wetPot, 4, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "a flowering plant in a watered Grow Pot");
        expect(context, step(context, indica, tray.with(HydroTrayBlock.LEVEL, 3), 4, GrowLight.NATURAL, HIT),
                GrowLight.STRESSED, "a flowering plant over still water, pump off");
        expect(context, step(context, indica, pumped, 4, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "a flowering plant over a running pump");

        // Overfeeding: bone meal on a plant already flowering, in a bed that already has food in it.
        BlockState fedPot = wetPot.with(GrowPotBlock.FERTILITY, 3);
        expect(context, step(context, indica, fedPot, 4, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "bone meal on a flowering plant in a fed pot");
        expect(context, step(context, indica, fedPot, 4, GrowLight.NATURAL, MISS), GrowLight.NATURAL,
                "a missed overfeeding roll");
        expect(context, step(context, indica, fedPot, 3, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "bone meal on a plant not yet flowering, in a fed pot");
        expect(context, step(context, indica, pumped.with(HydroTrayBlock.FED, true), 4, GrowLight.NATURAL, HIT),
                GrowLight.STRESSED, "bone meal on a flowering plant in a fed tray");

        // Stress takes the light's record with it: a Grow Lamp plant burnt by its bed harvests as stressed.
        context.setBlockState(LIGHT, ModBlocks.GROW_LAMP.getDefaultState().with(Properties.LIT, true));
        context.setBlockState(LIGHT.up(), Blocks.REDSTONE_BLOCK);
        expect(context, step(context, indica, pot, 4, GrowLight.GROW_LAMP, MISS), GrowLight.GROW_LAMP,
                "a Grow Lamp plant in a dry pot, on a missed roll");
        expect(context, step(context, indica, pot, 4, GrowLight.GROW_LAMP, HIT), GrowLight.STRESSED,
                "a Grow Lamp plant in a dry pot, on a hit");
        context.setBlockState(LIGHT, Blocks.AIR);
        context.setBlockState(LIGHT.up(), Blocks.AIR);

        // Lemon Haze takes the same step, and every segment carries the record.
        expect(context, step(context, ModBlocks.SATIVA_CROP, pot, 5, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "a flowering Lemon Haze in a dry pot");
        for (int y = 1; y <= 2; y++) {
            BlockState segment = context.getBlockState(CROP.up(y));
            context.assertTrue(segment.isOf(ModBlocks.SATIVA_CROP) && segment.get(GrowLight.PROPERTY) == GrowLight.STRESSED,
                    "a stressed Lemon Haze reads " + segment + " " + y + " up");
        }

        // Beldía is a strain like any other in a bed.
        expect(context, step(context, ModBlocks.BELDIA_CROP, pot, 4, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "a flowering Beldía in a dry pot");
        context.complete();
    }

    /**
     * A bee's step is a growth step: it reads the light and rolls like any other. Before, the bee
     * mixin only carried the record across, so a bee could see a plant through a dry flowering.
     */
    public static void aBeeStepIsAGrowthStep(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(CROP);
        CropBlock indica = (CropBlock) ModBlocks.INDICA_CROP;

        plant(context, ModBlocks.INDICA_CROP, Blocks.FARMLAND.getDefaultState(), 4, GrowLight.NATURAL);
        BlockState current = world.getBlockState(pos);
        BlockState hit = PlantStress.beeStep(world, pos, current, indica.withAge(5), HIT);
        context.assertTrue(hit.get(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "a bee growing a flowering plant on bone-dry farmland could not stress it: " + hit.get(GrowLight.PROPERTY));
        BlockState miss = PlantStress.beeStep(world, pos, current, indica.withAge(5), MISS);
        context.assertTrue(miss.get(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "a bee's missed roll stressed the plant anyway");

        // The light is read on a bee's step too: a lamp plant a bee grows with no lamp over it is stressed.
        plant(context, ModBlocks.INDICA_CROP, moist(), 4, GrowLight.LAMP);
        BlockState lamp = PlantStress.beeStep(world, pos, world.getBlockState(pos), indica.withAge(5), MISS);
        context.assertTrue(lamp.get(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "a bee grew a lamp plant with no lamp over it and it kept " + lamp.get(GrowLight.PROPERTY));

        // Anything that is not one of ours comes back exactly as the bee wrote it.
        BlockState wheat = Blocks.WHEAT.getDefaultState();
        context.assertTrue(PlantStress.beeStep(world, pos, wheat, ((CropBlock) Blocks.WHEAT).withAge(1), HIT)
                        == ((CropBlock) Blocks.WHEAT).withAge(1), "a bee's step on wheat was changed");
        context.complete();
    }

    // ----- the pot's water -----

    public static void aPotTakesOneBottle(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos soil = context.getAbsolutePos(SOIL);
        GrowPotBlock pot = (GrowPotBlock) ModBlocks.GROW_POT;
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        BlockHitResult onPot = new BlockHitResult(Vec3d.ofCenter(soil), Direction.UP, soil, false);

        // Water decides the moisture; food only works in water.
        BlockState fresh = pot.getDefaultState();
        context.assertTrue(!fresh.get(GrowPotBlock.WATERED) && fresh.get(GrowPotBlock.FERTILITY) == 0,
                "a fresh Grow Pot is not dry and empty");
        BlockState fedDry = fresh.with(GrowPotBlock.FERTILITY, 3);
        context.assertTrue(pot.moisture(fedDry) == GrowPotBlock.DRY_MOISTURE && pot.speed(fedDry) == 1.0F,
                "a fed but dry pot grows like soil with water in it");
        BlockState fedWet = fedDry.with(GrowPotBlock.WATERED, true);
        context.assertTrue(pot.moisture(fedWet) == GrowPotBlock.WET_MOISTURE && pot.speed(fedWet) == GrowPotBlock.FERTILE_SPEED,
                "a fed, watered pot is not the best soil there is");
        BlockState plainWet = fresh.with(GrowPotBlock.WATERED, true);
        context.assertTrue(pot.moisture(plainWet) == GrowPotBlock.WET_MOISTURE && pot.speed(plainWet) == 1.0F,
                "a watered pot with no food is not an ordinary wet field");

        // A bottle from the hand waters it and hands back the glass.
        context.setBlockState(SOIL, fresh);
        ItemStack bottle = waterBottle();
        player.setStackInHand(Hand.MAIN_HAND, bottle);
        context.assertTrue(world.getBlockState(soil).onUseWithItem(bottle, world, player, Hand.MAIN_HAND, onPot).isAccepted(),
                "a Grow Pot refused a water bottle");
        context.assertTrue(context.getBlockState(SOIL).get(GrowPotBlock.WATERED), "a bottle did not water the pot");
        context.assertTrue(player.getStackInHand(Hand.MAIN_HAND).isOf(Items.GLASS_BOTTLE),
                "watering a pot did not hand back the glass bottle");

        // A bucket is refused by accepting the click and doing nothing. Only an accepted result stops
        // the client going on to the bucket's own use, which pours a water block into the plant's
        // space: a PASS does not, and neither does a FAIL (it was FAIL first, and a real client poured).
        context.setBlockState(SOIL, fresh);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        player.setStackInHand(Hand.MAIN_HAND, bucket);
        ActionResult refused = world.getBlockState(soil).onUseWithItem(bucket, world, player, Hand.MAIN_HAND, onPot);
        context.assertTrue(refused.isAccepted(), "a Grow Pot answered a water bucket with " + refused
                + " — not accepted, so the client goes on to pour it");
        context.assertTrue(bucket.isOf(Items.WATER_BUCKET) && !context.getBlockState(SOIL).get(GrowPotBlock.WATERED),
                "a refused bucket was used anyway");

        // A second bottle into a wet pot is overwatering: a flowering plant may be stressed, and is
        // drawn so at once; a young one never is; and the water is poured either way.
        BlockState wet = fresh.with(GrowPotBlock.WATERED, true);
        plant(context, ModBlocks.INDICA_CROP, wet, 5, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), HIT);
        context.assertTrue(context.getBlockState(CROP).get(GrowLight.PROPERTY) == GrowLight.STRESSED
                        && context.getBlockState(CROP.up()).get(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "overwatering a flowering plant did not stress both its halves");
        plant(context, ModBlocks.INDICA_CROP, wet, 5, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), MISS);
        context.assertTrue(context.getBlockState(CROP).get(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "a missed overwatering roll stressed the plant");
        plant(context, ModBlocks.INDICA_CROP, wet, 3, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), HIT);
        context.assertTrue(context.getBlockState(CROP).get(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "overwatering stressed a plant that was not yet flowering");
        plant(context, ModBlocks.INDICA_CROP, wet, 7, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), HIT);
        context.assertTrue(context.getBlockState(CROP).get(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "overwatering stressed a ripe plant, which has finished growing");
        plant(context, ModBlocks.INDICA_CROP, fresh, 5, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), HIT);
        context.assertTrue(context.getBlockState(SOIL).get(GrowPotBlock.WATERED)
                        && context.getBlockState(CROP).get(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "watering a dry pot was treated as overwatering");

        // A bottle used on the plant waters its pot — Lemon Haze clicked at the very top.
        plant(context, ModBlocks.SATIVA_CROP, fresh, 6, GrowLight.NATURAL);
        BlockPos top = context.getAbsolutePos(CROP.up(2));
        context.assertTrue(world.getBlockState(top).isOf(ModBlocks.SATIVA_CROP),
                "Lemon Haze did not stand three tall, so this probe proves nothing");
        ItemStack onPlant = waterBottle();
        player.setStackInHand(Hand.MAIN_HAND, onPlant);
        BlockHitResult atTop = new BlockHitResult(Vec3d.ofCenter(top), Direction.UP, top, false);
        context.assertTrue(world.getBlockState(top).onUseWithItem(onPlant, world, player, Hand.MAIN_HAND, atTop).isAccepted()
                        && context.getBlockState(SOIL).get(GrowPotBlock.WATERED),
                "a bottle used on the top of a potted plant did not water its pot");

        // The plant drinks when it ripens, and eats with it.
        plant(context, ModBlocks.INDICA_CROP, fedWet, 6, GrowLight.NATURAL);
        BlockPos crop = context.getAbsolutePos(CROP);
        ((CropBlock) ModBlocks.INDICA_CROP).grow(world, MISS, crop, world.getBlockState(crop));
        BlockState after = context.getBlockState(SOIL);
        context.assertTrue(!after.get(GrowPotBlock.WATERED) && after.get(GrowPotBlock.FERTILITY) == 2,
                "a plant ripening in a pot left it " + after + " — it drinks the water and eats one charge");

        // Picked up, a pot keeps its food and not its water.
        ItemStack dropped = potDrop(world, soil, fedWet);
        BlockStateComponent kept = dropped.get(DataComponentTypes.BLOCK_STATE);
        context.assertTrue(kept != null && "3".equals(kept.properties().get("fertility"))
                        && !kept.properties().containsKey("watered"),
                "a watered pot at fertility 3 dropped as " + dropped + " with " + kept);

        // No evaporation for a bottle: vanilla makes mud with one in the Nether, so a pot there waters.
        ServerWorld nether = world.getServer().getWorld(World.NETHER);
        context.assertTrue(nether != null, "this server has no Nether, so this proves nothing");
        BlockPos netherPos = new BlockPos(0, 64, 0);
        nether.setBlockState(netherPos, pot.getDefaultState());
        GrowPotBlock.water(nether, netherPos, nether.getBlockState(netherPos), MISS);
        boolean netherWatered = nether.getBlockState(netherPos).get(GrowPotBlock.WATERED);
        nether.setBlockState(netherPos, Blocks.AIR.getDefaultState());
        context.assertTrue(netherWatered, "a bottle did not water a pot in the Nether");
        context.complete();
    }

    private static final BlockPos DISPENSER = new BlockPos(0, 1, 1);
    private static final BlockPos PULSE = new BlockPos(0, 2, 1);

    /** A dispenser's water bottle waters a pot and keeps the glass; anything else still gets vanilla's mud. */
    public static void aDispenserWatersAPot(TestContext context) {
        context.setBlockState(SOIL, ModBlocks.GROW_POT);
        context.setBlockState(DISPENSER, Blocks.DISPENSER.getDefaultState().with(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = context.getBlockEntity(DISPENSER, DispenserBlockEntity.class);
        dispenser.setStack(0, waterBottle());
        context.putAndRemoveRedstoneBlock(PULSE, 1);

        context.runAtTick(10, () -> {
            context.assertTrue(context.getBlockState(SOIL).get(GrowPotBlock.WATERED), "the dispenser did not water the pot");
            context.assertTrue(dispenser.getStack(0).isOf(Items.GLASS_BOTTLE),
                    "the dispenser did not keep the glass bottle: slot 0 holds " + dispenser.getStack(0));
            context.setBlockState(SOIL, Blocks.DIRT);
            dispenser.setStack(0, waterBottle());
            context.putAndRemoveRedstoneBlock(PULSE, 1);
        });

        context.runAtTick(20, () -> {
            context.assertTrue(context.getBlockState(SOIL).isOf(Blocks.MUD),
                    "a dispensed water bottle no longer turns dirt to mud — vanilla's behaviour was replaced, not wrapped");
            context.complete();
        });
    }

    // ----- helpers -----

    /** A random whose every float is {@code value}, so a roll's outcome is decided in advance. */
    private static Random decided(float value) {
        return new LocalRandom(0L) {
            @Override
            public float nextFloat() {
                return value;
            }
        };
    }

    private static BlockState moist() {
        return Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
    }

    private static ItemStack waterBottle() {
        return PotionContentsComponent.createStack(Items.POTION, Potions.WATER);
    }

    private static void odds(TestContext context, BlockState floor, float expected, String what) {
        float actual = PlantStress.ofGround(floor);
        context.assertTrue(actual == expected, what + " stresses at " + actual + " a step, expected " + expected);
    }

    private static void expect(TestContext context, GrowLight actual, GrowLight expected, String what) {
        context.assertTrue(actual == expected, what + " left the plant " + actual + ", expected " + expected);
    }

    /** A plant of {@code crop} at {@code age} on {@code soil}, with its segments grown in by a same-age reconcile. */
    private static void plant(TestContext context, Block crop, BlockState soil, int age, GrowLight record) {
        // Only the old plant's segments: a light hung above the plant stays where it is.
        for (int y = 2; y >= 1; y--) {
            if (context.getBlockState(CROP.up(y)).getBlock() instanceof CropBlock) {
                context.setBlockState(CROP.up(y), Blocks.AIR);
            }
        }
        // A tray's pump follows real redstone, so a running one needs a signal beside it.
        boolean pumped = soil.getBlock() instanceof HydroTrayBlock && soil.get(HydroTrayBlock.POWERED);
        context.setBlockState(SOIL.north(), pumped ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
        context.setBlockState(SOIL, soil);
        context.setBlockState(CROP, Defoliation.unworked(crop.getDefaultState())
                .with(Properties.AGE_7, age).with(GrowLight.PROPERTY, record));
        // A random tick reconciles the missing segments; MISS keeps it from growing the plant too.
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(CROP);
        world.getBlockState(pos).randomTick(world, pos, MISS);
    }

    /** One bone meal step on a fresh plant, with its roll decided, and the record it leaves. */
    private static GrowLight step(TestContext context, Block crop, BlockState soil, int age, GrowLight record,
                                  Random roll) {
        plant(context, crop, soil, age, record);
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(CROP);
        ((CropBlock) crop).grow(world, roll, pos, world.getBlockState(pos));
        context.assertTrue(context.getBlockState(CROP).get(Properties.AGE_7) == age + 1,
                "the step did not grow the plant, so this probe proves nothing");
        return context.getBlockState(CROP).get(GrowLight.PROPERTY);
    }

    private static ItemStack potDrop(ServerWorld world, BlockPos pos, BlockState state) {
        LootTable table = world.getServer().getReloadableRegistries()
                .getLootTable(ModBlocks.GROW_POT.getLootTableKey().orElseThrow());
        LootWorldContext params = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
                .add(LootContextParameters.BLOCK_STATE, state)
                .add(LootContextParameters.TOOL, ItemStack.EMPTY)
                .build(LootContextTypes.BLOCK);
        List<ItemStack> drops = table.generateLoot(params, 0L);
        return drops.isEmpty() ? ItemStack.EMPTY : drops.getFirst();
    }
}

package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.GrowPotBlock;
import com.warlonmhite.hempdustry.block.custom.HydroTrayBlock;
import com.warlonmhite.hempdustry.block.custom.PlantStress;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Stress beyond the light, and the Grow Pot's water. Both fail silently in play: a roll that never
 * fires looks exactly like a player who was lucky, and a pot that never dries looks like one that
 * was watered.
 *
 * <p>The odds are asserted as numbers, never measured — a measured rate would be measuring the RNG.
 * The wiring is driven with a random that decides every roll: {@link #HIT} lands under any odds above
 * zero and {@link #MISS} under none, so each step either certainly stresses or certainly does not.
 */
public final class PlantStressGameTest implements FabricGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    /** Where a light over a Purple Kush hangs: two above its LOWER. */
    private static final BlockPos LIGHT = CROP.above(2);

    private static final RandomSource HIT = decided(0.0F);
    private static final RandomSource MISS = decided(0.999F);

    // ----- the causes -----

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void stressHasMoreCausesThanLight(GameTestHelper context) {
        // The agreed numbers.
        context.assertTrue(PlantStress.OVERFED == 1.0F / 9 && PlantStress.OVERWATERED == 1.0F / 4
                        && PlantStress.DRY_BED == 1.0F / 3 && PlantStress.STAGNANT == 1.0F / 6
                        && PlantStress.DRY_FIELD == 1.0F / 20,
                "the stress odds are not 1 in 9 overfed, 1 in 4 overwatered, 1 in 3 dry bed, 1 in 6 stagnant, 1 in 20 dry field");

        BlockState dryField = Blocks.FARMLAND.defaultBlockState();
        BlockState pot = ModBlocks.GROW_POT.defaultBlockState();
        BlockState tray = ModBlocks.HYDRO_TRAY.defaultBlockState();
        odds(context, dryField, PlantStress.DRY_FIELD, "bone-dry farmland");
        odds(context, moist(), 0.0F, "moist farmland");
        odds(context, dryField.setValue(FarmBlock.MOISTURE, 1), 0.0F, "farmland drying out but not yet dry");
        odds(context, Blocks.SAND.defaultBlockState(), 0.0F, "sand");
        odds(context, pot, PlantStress.DRY_BED, "a dry Grow Pot");
        odds(context, pot.setValue(GrowPotBlock.WATERED, true), 0.0F, "a watered Grow Pot");
        odds(context, tray.setValue(HydroTrayBlock.POWERED, true), PlantStress.DRY_BED, "a dry tray, pump running");
        odds(context, tray.setValue(HydroTrayBlock.LEVEL, 3), PlantStress.STAGNANT, "a watered tray with its pump off");
        odds(context, tray.setValue(HydroTrayBlock.LEVEL, 3).setValue(HydroTrayBlock.FED, true), PlantStress.STAGNANT,
                "a fed tray with its pump off");
        odds(context, tray.setValue(HydroTrayBlock.LEVEL, 3).setValue(HydroTrayBlock.POWERED, true), 0.0F,
                "a watered tray with its pump running");

        // A field plant can be stressed now — but only flowering, only bone dry, and only on a hit.
        Block indica = ModBlocks.INDICA_CROP;
        expect(context, step(context, indica, dryField, 3, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "a plain field plant stepping into flowering on bone-dry farmland");
        context.assertTrue(context.getBlockState(CROP.above()).getValue(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "a plant stressed by its ground is drawn healthy at the top — the upper half reads "
                        + context.getBlockState(CROP.above()).getValue(GrowLight.PROPERTY));
        expect(context, step(context, indica, dryField, 2, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "a young plant stepping on bone-dry farmland");
        expect(context, step(context, indica, moist(), 3, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "a flowering plant on moist farmland");
        expect(context, step(context, indica, dryField, 3, GrowLight.NATURAL, MISS), GrowLight.NATURAL,
                "a missed roll on bone-dry farmland");

        // The beds.
        BlockState wetPot = pot.setValue(GrowPotBlock.WATERED, true);
        BlockState pumped = tray.setValue(HydroTrayBlock.LEVEL, 3).setValue(HydroTrayBlock.POWERED, true);
        expect(context, step(context, indica, pot, 4, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "a flowering plant in a dry Grow Pot");
        expect(context, step(context, indica, wetPot, 4, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "a flowering plant in a watered Grow Pot");
        expect(context, step(context, indica, tray.setValue(HydroTrayBlock.LEVEL, 3), 4, GrowLight.NATURAL, HIT),
                GrowLight.STRESSED, "a flowering plant over still water, pump off");
        expect(context, step(context, indica, pumped, 4, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "a flowering plant over a running pump");

        // Overfeeding: bone meal on a plant already flowering, in a bed that already has food in it.
        BlockState fedPot = wetPot.setValue(GrowPotBlock.FERTILITY, 3);
        expect(context, step(context, indica, fedPot, 4, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "bone meal on a flowering plant in a fed pot");
        expect(context, step(context, indica, fedPot, 4, GrowLight.NATURAL, MISS), GrowLight.NATURAL,
                "a missed overfeeding roll");
        expect(context, step(context, indica, fedPot, 3, GrowLight.NATURAL, HIT), GrowLight.NATURAL,
                "bone meal on a plant not yet flowering, in a fed pot");
        expect(context, step(context, indica, pumped.setValue(HydroTrayBlock.FED, true), 4, GrowLight.NATURAL, HIT),
                GrowLight.STRESSED, "bone meal on a flowering plant in a fed tray");

        // Stress takes the light's record with it: a Grow Lamp plant burnt by its bed harvests as stressed.
        context.setBlock(LIGHT, ModBlocks.GROW_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        context.setBlock(LIGHT.above(), Blocks.REDSTONE_BLOCK);
        expect(context, step(context, indica, pot, 4, GrowLight.GROW_LAMP, MISS), GrowLight.GROW_LAMP,
                "a Grow Lamp plant in a dry pot, on a missed roll");
        expect(context, step(context, indica, pot, 4, GrowLight.GROW_LAMP, HIT), GrowLight.STRESSED,
                "a Grow Lamp plant in a dry pot, on a hit");
        context.setBlock(LIGHT, Blocks.AIR);
        context.setBlock(LIGHT.above(), Blocks.AIR);

        // Lemon Haze takes the same step, and every segment carries the record.
        expect(context, step(context, ModBlocks.SATIVA_CROP, pot, 5, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "a flowering Lemon Haze in a dry pot");
        for (int y = 1; y <= 2; y++) {
            BlockState segment = context.getBlockState(CROP.above(y));
            context.assertTrue(segment.is(ModBlocks.SATIVA_CROP) && segment.getValue(GrowLight.PROPERTY) == GrowLight.STRESSED,
                    "a stressed Lemon Haze reads " + segment + " " + y + " up");
        }

        // Beldía is a strain like any other in a bed.
        expect(context, step(context, ModBlocks.BELDIA_CROP, pot, 4, GrowLight.NATURAL, HIT), GrowLight.STRESSED,
                "a flowering Beldía in a dry pot");
        context.succeed();
    }

    /**
     * A bee's step is a growth step: it reads the light and rolls like any other. Before, the bee
     * mixin only carried the record across, so a bee could see a plant through a dry flowering.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aBeeStepIsAGrowthStep(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        CropBlock indica = (CropBlock) ModBlocks.INDICA_CROP;

        plant(context, ModBlocks.INDICA_CROP, Blocks.FARMLAND.defaultBlockState(), 4, GrowLight.NATURAL);
        BlockState current = world.getBlockState(pos);
        BlockState hit = PlantStress.beeStep(world, pos, current, indica.getStateForAge(5), HIT);
        context.assertTrue(hit.getValue(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "a bee growing a flowering plant on bone-dry farmland could not stress it: " + hit.getValue(GrowLight.PROPERTY));
        BlockState miss = PlantStress.beeStep(world, pos, current, indica.getStateForAge(5), MISS);
        context.assertTrue(miss.getValue(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "a bee's missed roll stressed the plant anyway");

        // The light is read on a bee's step too: a lamp plant a bee grows with no lamp over it is stressed.
        plant(context, ModBlocks.INDICA_CROP, moist(), 4, GrowLight.LAMP);
        BlockState lamp = PlantStress.beeStep(world, pos, world.getBlockState(pos), indica.getStateForAge(5), MISS);
        context.assertTrue(lamp.getValue(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "a bee grew a lamp plant with no lamp over it and it kept " + lamp.getValue(GrowLight.PROPERTY));

        // Anything that is not one of ours comes back exactly as the bee wrote it.
        BlockState wheat = Blocks.WHEAT.defaultBlockState();
        context.assertTrue(PlantStress.beeStep(world, pos, wheat, ((CropBlock) Blocks.WHEAT).getStateForAge(1), HIT)
                        == ((CropBlock) Blocks.WHEAT).getStateForAge(1), "a bee's step on wheat was changed");
        context.succeed();
    }

    // ----- the pot's water -----

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aPotTakesOneBottle(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos soil = context.absolutePos(SOIL);
        GrowPotBlock pot = (GrowPotBlock) ModBlocks.GROW_POT;
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockHitResult onPot = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);

        // Water decides the moisture; food only works in water.
        BlockState fresh = pot.defaultBlockState();
        context.assertTrue(!fresh.getValue(GrowPotBlock.WATERED) && fresh.getValue(GrowPotBlock.FERTILITY) == 0,
                "a fresh Grow Pot is not dry and empty");
        BlockState fedDry = fresh.setValue(GrowPotBlock.FERTILITY, 3);
        context.assertTrue(pot.moisture(fedDry) == GrowPotBlock.DRY_MOISTURE && pot.speed(fedDry) == 1.0F,
                "a fed but dry pot grows like soil with water in it");
        BlockState fedWet = fedDry.setValue(GrowPotBlock.WATERED, true);
        context.assertTrue(pot.moisture(fedWet) == GrowPotBlock.WET_MOISTURE && pot.speed(fedWet) == GrowPotBlock.FERTILE_SPEED,
                "a fed, watered pot is not the best soil there is");
        BlockState plainWet = fresh.setValue(GrowPotBlock.WATERED, true);
        context.assertTrue(pot.moisture(plainWet) == GrowPotBlock.WET_MOISTURE && pot.speed(plainWet) == 1.0F,
                "a watered pot with no food is not an ordinary wet field");

        // A bottle from the hand waters it and hands back the glass.
        context.setBlock(SOIL, fresh);
        ItemStack bottle = waterBottle();
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        context.assertTrue(world.getBlockState(soil).useItemOn(bottle, world, player, InteractionHand.MAIN_HAND, onPot).consumesAction(),
                "a Grow Pot refused a water bottle");
        context.assertTrue(context.getBlockState(SOIL).getValue(GrowPotBlock.WATERED), "a bottle did not water the pot");
        context.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.GLASS_BOTTLE),
                "watering a pot did not hand back the glass bottle");

        // A bucket is refused by accepting the click and doing nothing. Only an accepted result stops
        // the client going on to the bucket's own use, which pours a water block into the plant's
        // space: a PASS does not, and neither does a FAIL (it was FAIL first, and a real client poured).
        context.setBlock(SOIL, fresh);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, bucket);
        ItemInteractionResult refused = world.getBlockState(soil).useItemOn(bucket, world, player, InteractionHand.MAIN_HAND, onPot);
        context.assertTrue(refused.consumesAction(), "a Grow Pot answered a water bucket with " + refused
                + " — not accepted, so the client goes on to pour it");
        context.assertTrue(bucket.is(Items.WATER_BUCKET) && !context.getBlockState(SOIL).getValue(GrowPotBlock.WATERED),
                "a refused bucket was used anyway");

        // A second bottle into a wet pot is overwatering: a flowering plant may be stressed, and is
        // drawn so at once; a young one never is; and the water is poured either way.
        BlockState wet = fresh.setValue(GrowPotBlock.WATERED, true);
        plant(context, ModBlocks.INDICA_CROP, wet, 5, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), HIT);
        context.assertTrue(context.getBlockState(CROP).getValue(GrowLight.PROPERTY) == GrowLight.STRESSED
                        && context.getBlockState(CROP.above()).getValue(GrowLight.PROPERTY) == GrowLight.STRESSED,
                "overwatering a flowering plant did not stress both its halves");
        plant(context, ModBlocks.INDICA_CROP, wet, 5, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), MISS);
        context.assertTrue(context.getBlockState(CROP).getValue(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "a missed overwatering roll stressed the plant");
        plant(context, ModBlocks.INDICA_CROP, wet, 3, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), HIT);
        context.assertTrue(context.getBlockState(CROP).getValue(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "overwatering stressed a plant that was not yet flowering");
        plant(context, ModBlocks.INDICA_CROP, wet, 7, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), HIT);
        context.assertTrue(context.getBlockState(CROP).getValue(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "overwatering stressed a ripe plant, which has finished growing");
        plant(context, ModBlocks.INDICA_CROP, fresh, 5, GrowLight.NATURAL);
        GrowPotBlock.water(world, soil, world.getBlockState(soil), HIT);
        context.assertTrue(context.getBlockState(SOIL).getValue(GrowPotBlock.WATERED)
                        && context.getBlockState(CROP).getValue(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "watering a dry pot was treated as overwatering");

        // A bottle used on the plant waters its pot — Lemon Haze clicked at the very top.
        plant(context, ModBlocks.SATIVA_CROP, fresh, 6, GrowLight.NATURAL);
        BlockPos top = context.absolutePos(CROP.above(2));
        context.assertTrue(world.getBlockState(top).is(ModBlocks.SATIVA_CROP),
                "Lemon Haze did not stand three tall, so this probe proves nothing");
        ItemStack onPlant = waterBottle();
        player.setItemInHand(InteractionHand.MAIN_HAND, onPlant);
        BlockHitResult atTop = new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false);
        context.assertTrue(world.getBlockState(top).useItemOn(onPlant, world, player, InteractionHand.MAIN_HAND, atTop).consumesAction()
                        && context.getBlockState(SOIL).getValue(GrowPotBlock.WATERED),
                "a bottle used on the top of a potted plant did not water its pot");

        // The plant drinks when it ripens, and eats with it.
        plant(context, ModBlocks.INDICA_CROP, fedWet, 6, GrowLight.NATURAL);
        BlockPos crop = context.absolutePos(CROP);
        ((CropBlock) ModBlocks.INDICA_CROP).performBonemeal(world, MISS, crop, world.getBlockState(crop));
        BlockState after = context.getBlockState(SOIL);
        context.assertTrue(!after.getValue(GrowPotBlock.WATERED) && after.getValue(GrowPotBlock.FERTILITY) == 2,
                "a plant ripening in a pot left it " + after + " — it drinks the water and eats one charge");

        // Picked up, a pot keeps its food and not its water.
        ItemStack dropped = potDrop(world, soil, fedWet);
        BlockItemStateProperties kept = dropped.get(DataComponents.BLOCK_STATE);
        context.assertTrue(kept != null && "3".equals(kept.properties().get("fertility"))
                        && !kept.properties().containsKey("watered"),
                "a watered pot at fertility 3 dropped as " + dropped + " with " + kept);

        // No evaporation for a bottle: vanilla makes mud with one in the Nether, so a pot there waters.
        ServerLevel nether = world.getServer().getLevel(Level.NETHER);
        context.assertTrue(nether != null, "this server has no Nether, so this proves nothing");
        BlockPos netherPos = new BlockPos(0, 64, 0);
        nether.setBlockAndUpdate(netherPos, pot.defaultBlockState());
        GrowPotBlock.water(nether, netherPos, nether.getBlockState(netherPos), MISS);
        boolean netherWatered = nether.getBlockState(netherPos).getValue(GrowPotBlock.WATERED);
        nether.setBlockAndUpdate(netherPos, Blocks.AIR.defaultBlockState());
        context.assertTrue(netherWatered, "a bottle did not water a pot in the Nether");
        context.succeed();
    }

    private static final BlockPos DISPENSER = new BlockPos(0, 1, 1);
    private static final BlockPos PULSE = new BlockPos(0, 2, 1);

    /** A dispenser's water bottle waters a pot and keeps the glass; anything else still gets vanilla's mud. */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aDispenserWatersAPot(GameTestHelper context) {
        context.setBlock(SOIL, ModBlocks.GROW_POT);
        context.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = context.<DispenserBlockEntity>getBlockEntity(DISPENSER);
        dispenser.setItem(0, waterBottle());
        context.pulseRedstone(PULSE, 1);

        context.runAtTickTime(10, () -> {
            context.assertTrue(context.getBlockState(SOIL).getValue(GrowPotBlock.WATERED), "the dispenser did not water the pot");
            context.assertTrue(dispenser.getItem(0).is(Items.GLASS_BOTTLE),
                    "the dispenser did not keep the glass bottle: slot 0 holds " + dispenser.getItem(0));
            context.setBlock(SOIL, Blocks.DIRT);
            dispenser.setItem(0, waterBottle());
            context.pulseRedstone(PULSE, 1);
        });

        context.runAtTickTime(20, () -> {
            context.assertTrue(context.getBlockState(SOIL).is(Blocks.MUD),
                    "a dispensed water bottle no longer turns dirt to mud — vanilla's behaviour was replaced, not wrapped");
            context.succeed();
        });
    }

    // ----- helpers -----

    /** A random whose every float is {@code value}, so a roll's outcome is decided in advance. */
    private static RandomSource decided(float value) {
        return new SingleThreadedRandomSource(0L) {
            @Override
            public float nextFloat() {
                return value;
            }
        };
    }

    private static BlockState moist() {
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
    }

    private static ItemStack waterBottle() {
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    private static void odds(GameTestHelper context, BlockState floor, float expected, String what) {
        float actual = PlantStress.ofGround(floor);
        context.assertTrue(actual == expected, what + " stresses at " + actual + " a step, expected " + expected);
    }

    private static void expect(GameTestHelper context, GrowLight actual, GrowLight expected, String what) {
        context.assertTrue(actual == expected, what + " left the plant " + actual + ", expected " + expected);
    }

    /** A plant of {@code crop} at {@code age} on {@code soil}, with its segments grown in by a same-age reconcile. */
    private static void plant(GameTestHelper context, Block crop, BlockState soil, int age, GrowLight record) {
        // Only the old plant's segments: a light hung above the plant stays where it is.
        for (int y = 2; y >= 1; y--) {
            if (context.getBlockState(CROP.above(y)).getBlock() instanceof CropBlock) {
                context.setBlock(CROP.above(y), Blocks.AIR);
            }
        }
        // A tray's pump follows real redstone, so a running one needs a signal beside it.
        boolean pumped = soil.getBlock() instanceof HydroTrayBlock && soil.getValue(HydroTrayBlock.POWERED);
        context.setBlock(SOIL.north(), pumped ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
        context.setBlock(SOIL, soil);
        context.setBlock(CROP, Defoliation.unworked(crop.defaultBlockState())
                .setValue(BlockStateProperties.AGE_7, age).setValue(GrowLight.PROPERTY, record));
        // A random tick reconciles the missing segments; MISS keeps it from growing the plant too.
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        world.getBlockState(pos).randomTick(world, pos, MISS);
    }

    /** One bone meal step on a fresh plant, with its roll decided, and the record it leaves. */
    private static GrowLight step(GameTestHelper context, Block crop, BlockState soil, int age, GrowLight record,
                                  RandomSource roll) {
        plant(context, crop, soil, age, record);
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        ((CropBlock) crop).performBonemeal(world, roll, pos, world.getBlockState(pos));
        context.assertTrue(context.getBlockState(CROP).getValue(BlockStateProperties.AGE_7) == age + 1,
                "the step did not grow the plant, so this probe proves nothing");
        return context.getBlockState(CROP).getValue(GrowLight.PROPERTY);
    }

    private static ItemStack potDrop(ServerLevel world, BlockPos pos, BlockState state) {
        LootTable table = world.getServer().reloadableRegistries()
                .getLootTable(ModBlocks.GROW_POT.getLootTable());
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);
        List<ItemStack> drops = table.getRandomItems(params, 0L);
        return drops.isEmpty() ? ItemStack.EMPTY : drops.getFirst();
    }
}

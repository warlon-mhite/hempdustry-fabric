package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.GrowPotBlock;
import com.warlonmhite.hempdustry.block.custom.HydroTrayBlock;
import com.warlonmhite.hempdustry.block.custom.PlantStress;
import com.warlonmhite.hempdustry.block.custom.StressCause;
import com.warlonmhite.hempdustry.strain.Resistance;
import com.warlonmhite.hempdustry.strain.ResistanceGenes;
import com.warlonmhite.hempdustry.strain.StressPhenomenon;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FarmlandBlock;
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
public final class PlantStressGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    /** Where a light over a Purple Kush hangs: two above its LOWER. */
    private static final BlockPos LIGHT = CROP.above(2);

    private static final RandomSource HIT = decided(0.0F);
    private static final RandomSource MISS = decided(0.999F);

    // ----- the causes -----

    public static void stressHasMoreCausesThanLight(GameTestHelper context) {
        // The agreed numbers.
        context.assertTrue(PlantStress.OVERFED == 1.0F / 9 && PlantStress.OVERWATERED == 1.0F / 4
                        && PlantStress.DRY_BED == 1.0F / 3 && PlantStress.STAGNANT == 1.0F / 6
                        && PlantStress.DRY_FIELD == 1.0F / 20,
                "the stress odds are not 1 in 9 overfed, 1 in 4 overwatered, 1 in 3 dry bed, 1 in 6 stagnant, 1 in 20 dry field");

        BlockState dryField = Blocks.FARMLAND.defaultBlockState();
        BlockState pot = ModBlocks.GROW_POT.defaultBlockState();
        BlockState tray = ModBlocks.HYDRO_TRAY.defaultBlockState();
        odds(context, dryField, StressCause.dryGround(PlantStress.DRY_FIELD), "bone-dry farmland");
        odds(context, moist(), StressCause.NONE, "moist farmland");
        odds(context, dryField.setValue(FarmlandBlock.MOISTURE, 1), StressCause.NONE, "farmland drying out but not yet dry");
        odds(context, Blocks.SAND.defaultBlockState(), StressCause.NONE, "sand");
        odds(context, pot, StressCause.dryGround(PlantStress.DRY_BED), "a dry Grow Pot");
        odds(context, pot.setValue(GrowPotBlock.WATERED, true), StressCause.NONE, "a watered Grow Pot");
        odds(context, tray.setValue(HydroTrayBlock.POWERED, true), StressCause.dryGround(PlantStress.DRY_BED), "a dry tray, pump running");
        odds(context, tray.setValue(HydroTrayBlock.LEVEL, 3), StressCause.overwatering(PlantStress.STAGNANT), "a watered tray with its pump off");
        odds(context, tray.setValue(HydroTrayBlock.LEVEL, 3).setValue(HydroTrayBlock.FED, true), StressCause.overwatering(PlantStress.STAGNANT),
                "a fed tray with its pump off");
        odds(context, tray.setValue(HydroTrayBlock.LEVEL, 3).setValue(HydroTrayBlock.POWERED, true), StressCause.NONE,
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
     * A plant's resistances scale every cause by its own phenomenon: a dry bed by its dry-ground step, a dead
     * pump, overfeeding and a bottle by its overwatering step, and with two causes the worse after each is scaled.
     * A lost light stresses a weak plant a stage early and a resistant one only now and then.
     */
    public static void aPlantsResistanceScalesItsStress(GameTestHelper context) {
        Block indica = ModBlocks.INDICA_CROP;
        BlockState pot = ModBlocks.GROW_POT.defaultBlockState();
        ResistanceGenes neutral = ResistanceGenes.NEUTRAL;

        // A dry pot, 1 in 3 at neutral: very weak 2 in 3, very resistant 1 in 12.
        ResistanceGenes dryWeak = neutral.with(StressPhenomenon.DRY_GROUND, Resistance.VERY_WEAK);
        ResistanceGenes dryHardy = neutral.with(StressPhenomenon.DRY_GROUND, Resistance.VERY_RESISTANT);
        expect(context, step(context, indica, pot, 4, GrowLight.NATURAL, decided(0.6F), dryWeak), GrowLight.STRESSED,
                "a plant very weak to dry ground in a dry pot, at 0.6");
        expect(context, step(context, indica, pot, 4, GrowLight.NATURAL, decided(0.6F), neutral), GrowLight.NATURAL,
                "a neutral plant in a dry pot, at 0.6");
        expect(context, step(context, indica, pot, 4, GrowLight.NATURAL, decided(0.08F), dryHardy), GrowLight.STRESSED,
                "a plant very resistant to dry ground in a dry pot, at 0.08");
        expect(context, step(context, indica, pot, 4, GrowLight.NATURAL, decided(0.09F), dryHardy), GrowLight.NATURAL,
                "a plant very resistant to dry ground in a dry pot, at 0.09");
        // A dry pot is dry ground, not wet roots.
        expect(context, step(context, indica, pot, 4, GrowLight.NATURAL, decided(0.6F),
                        neutral.with(StressPhenomenon.OVERWATERING, Resistance.VERY_WEAK)), GrowLight.NATURAL,
                "a plant very weak to wet roots in a dry pot, at 0.6");
        // Still water with the pump off, 1 in 6: wet roots, so its overwatering step, not its dry-ground one.
        BlockState stagnant = ModBlocks.HYDRO_TRAY.defaultBlockState().setValue(HydroTrayBlock.LEVEL, 3);
        expect(context, step(context, indica, stagnant, 4, GrowLight.NATURAL, decided(0.3F),
                        neutral.with(StressPhenomenon.OVERWATERING, Resistance.VERY_WEAK)), GrowLight.STRESSED,
                "a plant very weak to wet roots over still water, at 0.3");
        expect(context, step(context, indica, stagnant, 4, GrowLight.NATURAL, decided(0.3F), dryWeak), GrowLight.NATURAL,
                "a plant very weak to dry ground over still water, at 0.3");
        // Two causes, each scaled by its own step: a dry fed pot and bone meal, dry 1 in 3 and overfed 1 in 9.
        // Very resistant to dry ground (1 in 12) and very weak to overfeeding (2 in 9): the worse is 2 in 9.
        BlockState dryFed = pot.setValue(GrowPotBlock.FERTILITY, 3);
        expect(context, step(context, indica, dryFed, 4, GrowLight.NATURAL, decided(0.2F),
                        dryHardy.with(StressPhenomenon.OVERWATERING, Resistance.VERY_WEAK)), GrowLight.STRESSED,
                "bone meal on a plant hardy to dry ground but very weak to overfeeding, in a dry fed pot, at 0.2");
        expect(context, step(context, indica, dryFed, 4, GrowLight.NATURAL, decided(0.2F),
                        dryHardy.with(StressPhenomenon.OVERWATERING, Resistance.VERY_RESISTANT)), GrowLight.NATURAL,
                "bone meal on a plant very resistant to both, in a dry fed pot, at 0.2");

        // A bottle into a wet pot, 1 in 4: very resistant 1 in 16, very weak 1 in 2; a roll of 0.3 stresses only the weak.
        ServerLevel world = context.getLevel();
        BlockPos soil = context.absolutePos(SOIL);
        BlockState wet = pot.setValue(GrowPotBlock.WATERED, true);
        for (Resistance step : new Resistance[]{Resistance.VERY_RESISTANT, Resistance.NEUTRAL, Resistance.VERY_WEAK}) {
            plant(context, indica, wet, 4, GrowLight.NATURAL);
            Genes.set(context, CROP, neutral.with(StressPhenomenon.OVERWATERING, step));
            GrowPotBlock.water(world, soil, world.getBlockState(soil), decided(0.3F));
            GrowLight expected = step == Resistance.VERY_WEAK ? GrowLight.STRESSED : GrowLight.NATURAL;
            expect(context, context.getBlockState(CROP).getValue(GrowLight.PROPERTY), expected,
                    "a bottle into a wet pot on a plant " + step + " to wet roots, at 0.3");
        }
        plant(context, indica, wet, 4, GrowLight.NATURAL);
        Genes.set(context, CROP, neutral.with(StressPhenomenon.OVERWATERING, Resistance.VERY_RESISTANT));
        GrowPotBlock.water(world, soil, world.getBlockState(soil), decided(0.06F));
        expect(context, context.getBlockState(CROP).getValue(GrowLight.PROPERTY), GrowLight.STRESSED, "a bottle into a wet pot on a very resistant plant, at 0.06");

        // A lost light: no lamp hangs over these plants, so a record of one is a lamp taken away.
        BlockState moist = moist();
        for (Resistance step : Resistance.values()) {
            GrowLight young = step(context, indica, moist, 2, GrowLight.GROW_LAMP, MISS,
                    neutral.with(StressPhenomenon.LIGHT, step));
            GrowLight expected = step.compareTo(Resistance.NEUTRAL) < 0 ? GrowLight.STRESSED : GrowLight.NATURAL;
            expect(context, young, expected, "a plant " + step + " to light losing its lamp stepping from age 2 to 3");
        }
        float[][] ignores = {{0.49F, 0.51F}, {0.74F, 0.76F}};
        Resistance[] hardy = {Resistance.RESISTANT, Resistance.VERY_RESISTANT};
        for (int i = 0; i < 2; i++) {
            ResistanceGenes genes = neutral.with(StressPhenomenon.LIGHT, hardy[i]);
            expect(context, step(context, indica, moist, 4, GrowLight.GROW_LAMP, decided(ignores[i][0]), genes),
                    GrowLight.NATURAL, "a plant " + hardy[i] + " to light losing its lamp while flowering, at " + ignores[i][0]);
            expect(context, step(context, indica, moist, 4, GrowLight.GROW_LAMP, decided(ignores[i][1]), genes),
                    GrowLight.STRESSED, "a plant " + hardy[i] + " to light losing its lamp while flowering, at " + ignores[i][1]);
        }
        expect(context, step(context, indica, moist, 4, GrowLight.GROW_LAMP, HIT, neutral), GrowLight.STRESSED,
                "a neutral plant losing its lamp while flowering, on any roll");
        // A bee's step reads the same genes.
        plant(context, indica, moist, 4, GrowLight.GROW_LAMP);
        Genes.set(context, CROP, neutral.with(StressPhenomenon.LIGHT, Resistance.VERY_RESISTANT));
        BlockPos crop = context.absolutePos(CROP);
        BlockState current = world.getBlockState(crop);
        BlockState bee = PlantStress.beeStep(world, crop, current, ((CropBlock) indica).getStateForAge(5), decided(0.7F));
        expect(context, bee.getValue(GrowLight.PROPERTY), GrowLight.NATURAL,
                "a bee's step on a plant very resistant to light, losing its lamp, at 0.7");
        context.succeed();
    }

    /**
     * A bee's step is a growth step: it reads the light and rolls like any other. Before, the bee
     * mixin only carried the record across, so a bee could see a plant through a dry flowering.
     */
    public static void aBeeStepIsAGrowthStep(GameTestHelper context) {
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

    public static void aPotTakesOneBottle(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos soil = context.absolutePos(SOIL);
        GrowPotBlock pot = (GrowPotBlock) ModBlocks.GROW_POT;
        ServerPlayer player = Players.survival(context);
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
        InteractionResult refused = world.getBlockState(soil).useItemOn(bucket, world, player, InteractionHand.MAIN_HAND, onPot);
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
        ((CropBlock) ModBlocks.INDICA_CROP).performBonemeal(world, MISS, crop, world.getBlockState(crop), BonemealSource.INTERACTION);
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
    public static void aDispenserWatersAPot(GameTestHelper context) {
        context.setBlock(SOIL, ModBlocks.GROW_POT);
        context.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = context.getBlockEntity(DISPENSER, DispenserBlockEntity.class);
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
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
    }

    private static ItemStack waterBottle() {
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    private static void odds(GameTestHelper context, BlockState floor, StressCause expected, String what) {
        StressCause actual = PlantStress.ofGround(floor);
        context.assertTrue(actual.equals(expected), what + " stresses with " + actual + " a step, expected " + expected);
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
        Genes.neutral(context, CROP);
        // A random tick reconciles the missing segments; MISS keeps it from growing the plant too.
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        world.getBlockState(pos).randomTick(world, pos, MISS);
    }

    /** One bone meal step on a fresh neutral plant, with its roll decided, and the record it leaves. */
    private static GrowLight step(GameTestHelper context, Block crop, BlockState soil, int age, GrowLight record,
                                  RandomSource roll) {
        return step(context, crop, soil, age, record, roll, ResistanceGenes.NEUTRAL);
    }

    /** One bone meal step on a fresh plant with {@code genes}, with its roll decided, and the record it leaves. */
    private static GrowLight step(GameTestHelper context, Block crop, BlockState soil, int age, GrowLight record,
                                  RandomSource roll, ResistanceGenes genes) {
        plant(context, crop, soil, age, record);
        Genes.set(context, CROP, genes);
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        ((CropBlock) crop).performBonemeal(world, roll, pos, world.getBlockState(pos), BonemealSource.INTERACTION);
        context.assertTrue(context.getBlockState(CROP).getValue(BlockStateProperties.AGE_7) == age + 1,
                "the step did not grow the plant, so this probe proves nothing");
        return context.getBlockState(CROP).getValue(GrowLight.PROPERTY);
    }

    private static ItemStack potDrop(ServerLevel world, BlockPos pos, BlockState state) {
        LootTable table = world.getServer().reloadableRegistries()
                .getLootTable(ModBlocks.GROW_POT.getLootTable().orElseThrow());
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);
        List<ItemStack> drops = table.getRandomItems(params, 0L);
        return drops.isEmpty() ? ItemStack.EMPTY : drops.getFirst();
    }
}

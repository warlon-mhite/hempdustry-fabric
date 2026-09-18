package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.custom.HempGrowth;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * The growth roll's odds, asserted as numbers rather than by growing plants and counting — a rate
 * measured off random ticks is measuring the RNG, and the thing that broke here was arithmetic.
 *
 * <p>Each row is one the old code got wrong: the multiplier sat inside vanilla's
 * {@code floor(resistance / moisture)} and the cast ate it. Moisture 10 is a lone plant on fully
 * watered farmland; 25 and 35 are Purple Kush and Lemon Haze.
 */
public final class CropGrowthGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void growthMultiplierScalesTheOdds(TestContext context) {
        // Unchanged at 1.0: vanilla's own odds, floor and all.
        expect(context, 25.0F, 10.0F, 1.0, 1.0F / 3.0F);
        expect(context, 35.0F, 10.0F, 1.0, 1.0F / 4.0F);
        // The old code: 1.2 did nothing to Purple Kush, and 2.0 was only 1.5.
        expect(context, 25.0F, 10.0F, 1.2, 0.4F);
        expect(context, 25.0F, 10.0F, 2.0, 2.0F / 3.0F);
        // The old code: 1.2 overshot Lemon Haze to a third faster.
        expect(context, 35.0F, 10.0F, 1.2, 0.3F);
        // A crowded field halves the moisture; the multiplier still scales whatever odds are left.
        expect(context, 25.0F, 5.0F, 2.0, 2.0F / 6.0F);
        context.complete();
    }

    private static void expect(TestContext context, float resistance, float moisture, double multiplier, float odds) {
        float actual = HempGrowth.chance(resistance, moisture, multiplier);
        context.assertTrue(Math.abs(actual - odds) < 1.0E-6F, "resistance " + resistance + ", moisture "
                + moisture + ", multiplier " + multiplier + ": odds " + actual + ", expected " + odds);
    }
}

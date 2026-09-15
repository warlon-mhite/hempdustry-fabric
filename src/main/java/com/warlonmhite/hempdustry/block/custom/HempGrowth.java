package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.config.HempdustryConfig;
import net.minecraft.util.math.random.Random;

/**
 * The random-tick growth roll both hemp crops share.
 *
 * <p>Vanilla's {@code CropBlock#randomTick} grows a crop with odds of
 * {@code 1 / (floor(resistance / moisture) + 1)}. The {@code floor} is harmless on its own, but
 * <b>a speed multiplier must not go inside it</b>: that is where {@code cropGrowthMultiplier} used
 * to sit, and the cast ate it. At full moisture (10) Purple Kush's 25 / 10 / 1.2 still floors to 2,
 * so 1.2 did nothing; 1.5 and 2.0 both floored to 1, so they grew at the same speed; and Lemon Haze
 * went the other way, 1.2 growing it a third faster. So the base odds are vanilla's exactly and the
 * multiplier scales the <em>odds</em>, which is the only place a multiplier means what it says.
 *
 * <p>At a multiplier of 1.0 this is the same probability as before, so default worlds grow exactly
 * as they did.
 */
public final class HempGrowth {

    private HempGrowth() {
    }

    /**
     * The odds that one random tick advances the plant, before any cap: above 1.0 means every tick.
     */
    public static float chance(float resistance, float moisture, double multiplier) {
        return (float) (multiplier / ((int) (resistance / moisture) + 1));
    }

    /** Rolls one random tick's growth, at the configured speed. */
    static boolean rolls(Random random, float resistance, float moisture) {
        return random.nextFloat() < chance(resistance, moisture, HempdustryConfig.get().world().cropGrowthMultiplier());
    }
}

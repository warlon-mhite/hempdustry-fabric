package com.warlonmhite.hempdustry.effect;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

/**
 * The mod's own status effects. An effect's id is something a save keeps (on every entity that has
 * it, and in a datapack strain that names it), so once an id here has shipped it stays.
 */
public class ModEffects {

    /**
     * Ender Haze's signature: endermen do not see you. The End's own sky colour, as the plant is —
     * {@code minecraft:visual/sky_light_color} in the End's dimension type.
     */
    public static final Holder<MobEffect> AVERTED_GAZE = register("averted_gaze", new AvertedGazeEffect(0xE580FF));

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name), effect);
    }

    public static void registerEffects() {
        Hempdustry.LOGGER.info("Registering Effects for " + Hempdustry.MOD_ID);
    }
}

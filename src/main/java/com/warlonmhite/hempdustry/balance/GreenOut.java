package com.warlonmhite.hempdustry.balance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * What smoking too much costs, as data: {@code data/<namespace>/hempdustry/green_out/default.json}.
 *
 * <p>{@code one_in_by_dose} is the odds per hit as 1-in-N, dose 1 first, 0 for never; the last entry
 * holds for every dose above it. From {@code full_from_dose} a green-out is the full one, and the
 * caller locks every smokeable out for {@code lockout_ticks}; below it, it is the spins. The two
 * effect lists are written as vanilla writes a potion's custom effects. What a green-out
 * <em>does</em> stays code: it replaces the hit's effects, and the full one ends every buff the mod
 * grants and empties the saturation.
 */
public record GreenOut(List<Integer> oneInByDose, int fullFromDose, List<MobEffectInstance> spins,
                       List<MobEffectInstance> full, int lockoutTicks) {

    public static final ResourceKey<Registry<GreenOut>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "green_out"));

    public static final ResourceKey<GreenOut> DEFAULT =
            ResourceKey.create(REGISTRY_KEY, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "default"));

    public static final Codec<GreenOut> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.listOf(1, Integer.MAX_VALUE).fieldOf("one_in_by_dose").forGetter(GreenOut::oneInByDose),
            ExtraCodecs.POSITIVE_INT.fieldOf("full_from_dose").forGetter(GreenOut::fullFromDose),
            MobEffectInstance.CODEC.listOf().fieldOf("spins").forGetter(GreenOut::spins),
            MobEffectInstance.CODEC.listOf().fieldOf("full").forGetter(GreenOut::full),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("lockout_ticks").forGetter(GreenOut::lockoutTicks)
    ).apply(instance, GreenOut::new));

    /**
     * The 2.0.1 green-out. <b>Dose 1 can never green you out</b>, so the cheap everyday hit carries
     * no tail risk and a big one is a real decision; dose 2 is 1 in 12, dose 3 1 in 4. The spins
     * hold you down for 15 s with a wobble that outlasts them, felt after you can move again; the
     * full one, from dose 3 — past the default buff cap, the dose taken for a longer high — lasts
     * 30 s and locks smoking out for a minute.
     */
    public static final GreenOut BUILT_IN = new GreenOut(
            List.of(0, 12, 4),
            3,
            List.of(new MobEffectInstance(MobEffects.NAUSEA, 400, 0),
                    new MobEffectInstance(MobEffects.SLOWNESS, 300, 1),
                    new MobEffectInstance(MobEffects.WEAKNESS, 300, 1),
                    new MobEffectInstance(MobEffects.MINING_FATIGUE, 300, 1)),
            List.of(new MobEffectInstance(MobEffects.NAUSEA, 700, 0),
                    new MobEffectInstance(MobEffects.SLOWNESS, 600, 1),
                    new MobEffectInstance(MobEffects.WEAKNESS, 600, 1),
                    new MobEffectInstance(MobEffects.MINING_FATIGUE, 600, 1)),
            1200);

    public static void bootstrap(BootstrapContext<GreenOut> registerable) {
        registerable.register(DEFAULT, BUILT_IN);
    }

    /** The green-out in this world. */
    public static GreenOut of(HolderLookup.Provider registries) {
        return ModBalance.entry(registries, DEFAULT, BUILT_IN);
    }

    /** Odds of greening out at {@code dose}, as 1-in-N; 0 is never. */
    public int oneIn(int dose) {
        return dose <= 0 ? 0 : oneInByDose.get(Math.min(dose, oneInByDose.size()) - 1);
    }
}

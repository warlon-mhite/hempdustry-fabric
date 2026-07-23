package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A cannabis strain: its buds, and the effects it applies when smoked. Effects are data — a list of
 * {@link SmokeEffect} plus a light/heavy duration — so the {@link Potency} of the device decides each
 * effect's amplifier and how long it lasts. Adding a strain is one row here; no logic changes.
 *
 * <p>Display names ("Purple Kush", "Lemon Haze") live in the lang file; the ids stay
 * {@code indica}/{@code sativa} to match the rest of the codebase.
 */
public enum Strain {
    INDICA("indica", () -> ModItems.INDICA_BUDS,
            List.of(
                    new SmokeEffect(StatusEffects.RESISTANCE, 0, 1),      // Resistance I (light) / II (heavy)
                    new SmokeEffect(StatusEffects.HUNGER, 0, 0),          // Hunger I regardless of device
                    new SmokeEffect(StatusEffects.MINING_FATIGUE, 0, 1)), // Mining Fatigue I (light) / II (heavy)
            700, 1000), // 35s spliff/pipe, 50s bong
    // Lemon Haze isn't active yet; effects are a placeholder until it's finished (roadmap: energetic — Speed/Haste).
    SATIVA("sativa", () -> ModItems.SATIVA_BUDS, List.of(), 700, 1000);

    /**
     * Strains with a full smoking chain (spliff + packable devices). Only Purple Kush is live for
     * now — add {@code SATIVA} here to activate Lemon Haze end-to-end.
     */
    public static final List<Strain> ACTIVE = List.of(INDICA);

    private final String id;
    private final Supplier<Item> buds;
    private final List<SmokeEffect> smokeEffects;
    private final int lightDurationTicks;
    private final int heavyDurationTicks;

    Strain(String id, Supplier<Item> buds, List<SmokeEffect> smokeEffects, int lightDurationTicks, int heavyDurationTicks) {
        this.id = id;
        this.buds = buds;
        this.smokeEffects = smokeEffects;
        this.lightDurationTicks = lightDurationTicks;
        this.heavyDurationTicks = heavyDurationTicks;
    }

    public String id() {
        return id;
    }

    public Item buds() {
        return buds.get();
    }

    /** Fresh status-effect instances for one hit at the given potency (empty if the strain has none). */
    public List<StatusEffectInstance> effects(Potency potency) {
        boolean heavy = potency == Potency.HEAVY;
        int duration = heavy ? heavyDurationTicks : lightDurationTicks;
        List<StatusEffectInstance> out = new ArrayList<>(smokeEffects.size());
        for (SmokeEffect effect : smokeEffects) {
            out.add(new StatusEffectInstance(effect.effect(), duration, heavy ? effect.heavyAmplifier() : effect.lightAmplifier()));
        }
        return out;
    }

    /** The active strain whose buds are {@code item}, or {@code null} if none. */
    public static Strain fromBuds(Item item) {
        for (Strain strain : ACTIVE) {
            if (strain.buds() == item) {
                return strain;
            }
        }
        return null;
    }

    /** One effect a strain grants when smoked, with its amplifier at light vs heavy potency. */
    public record SmokeEffect(RegistryEntry<StatusEffect> effect, int lightAmplifier, int heavyAmplifier) {
    }
}

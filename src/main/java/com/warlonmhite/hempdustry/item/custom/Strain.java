package com.warlonmhite.hempdustry.item.custom;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.item.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.StringIdentifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A cannabis strain: its buds, and the effects it applies when smoked.
 *
 * <h2>Effects are data, and dose is the amplifier</h2>
 *
 * A strain owns <em>what</em> it does; the device owns <em>how long</em> it does it, and the
 * <em>dose</em> (how many buds went in) owns how strongly. See CLAUDE.md §5b D10 for the full
 * design. In short:
 *
 * <pre>
 *   amplifier = baseAmplifier + (dose - 1)      // for effects that scale
 *   duration  = the device's own duration       // never touched by dose
 * </pre>
 *
 * <p><b>Duration deliberately does not shrink as the amplifier rises</b>, which is where this
 * departs from vanilla's glowstone rule. That rule works for potions because a potion is
 * single-signed — all buff or all debuff. A strain is a <em>bundle</em> of both (Purple Kush is
 * Resistance <i>and</i> Mining Fatigue), so halving the duration would shorten the penalty too and
 * dosing would partly reward itself. The brake is instead that <b>dose amplifies the debuff as
 * well</b>: Purple Kush at dose 3 is unkillable and cannot mine; Lemon Haze at dose 3 is fast and
 * cannot punch.
 *
 * <p>Effects flagged {@code scales = false} sit out of that — Hunger is flat at level I however much
 * you smoke, for the same reason it is identical across strains: the munchies don't care.
 *
 * <p>Display names ("Purple Kush", "Lemon Haze") live in the lang files under
 * {@code hempdustry.strain.<id>}; the ids stay {@code indica}/{@code sativa} to match the rest of
 * the codebase. Note the folk sativa/indica <em>effect</em> split is genre furniture, not botany —
 * see CLAUDE.md.
 */
public enum Strain implements StringIdentifiable {
    // Purple Kush — the body high: hard to hurt, hard to get anything done.
    INDICA("indica", () -> ModItems.INDICA_SEEDS, () -> ModItems.INDICA_BUDS, 0x8E6FB5,
            List.of(
                    new SmokeEffect(StatusEffects.RESISTANCE, 0, true),
                    new SmokeEffect(StatusEffects.HUNGER, 0, false),
                    new SmokeEffect(StatusEffects.MINING_FATIGUE, 0, true))),
    // Lemon Haze — the head high, and a deliberate mirror of Purple Kush: where indica buffs
    // defence and taxes mining, sativa buffs movement and mining and taxes melee damage. Hunger
    // is in both because the munchies don't care which strain you smoked.
    SATIVA("sativa", () -> ModItems.SATIVA_SEEDS, () -> ModItems.SATIVA_BUDS, 0xC7D14A,
            List.of(
                    new SmokeEffect(StatusEffects.SPEED, 0, true),
                    new SmokeEffect(StatusEffects.HASTE, 0, true),
                    new SmokeEffect(StatusEffects.HUNGER, 0, false),
                    new SmokeEffect(StatusEffects.WEAKNESS, 0, true)));

    /**
     * Strains with a full smoking chain. Everything per-strain that is pure plumbing — creative-tab
     * stacks, the packing recipe, spliff recipes, {@code fromBuds} lookup, the seed loot pools — is
     * driven off this list, so a third strain is one row plus its own content.
     */
    public static final List<Strain> ACTIVE = List.of(INDICA, SATIVA);

    public static final Codec<Strain> CODEC = StringIdentifiable.createCodec(Strain::values);
    /** Ordinal-based: this only travels over the wire, never to disk (the codec above does disk). */
    public static final PacketCodec<ByteBuf, Strain> PACKET_CODEC =
            PacketCodecs.VAR_INT.xmap(ordinal -> values()[ordinal], Strain::ordinal);

    private final String id;
    private final Supplier<Item> seeds;
    private final Supplier<Item> buds;
    private final int color;
    private final List<SmokeEffect> smokeEffects;

    Strain(String id, Supplier<Item> seeds, Supplier<Item> buds, int color, List<SmokeEffect> smokeEffects) {
        this.id = id;
        this.seeds = seeds;
        this.buds = buds;
        this.color = color;
        this.smokeEffects = smokeEffects;
    }

    public String id() {
        return id;
    }

    /** The seed item that plants this strain's crop. */
    public Item seeds() {
        return seeds.get();
    }

    public Item buds() {
        return buds.get();
    }

    /** Packed-device / spliff tint, the way a potion tints its liquid layer. Blended for mixes. */
    public int color() {
        return color;
    }

    /** e.g. {@code hempdustry.strain.indica} — "Purple Kush". */
    public String getTranslationKey() {
        return "hempdustry.strain." + id;
    }

    /**
     * Fresh status-effect instances for one hit of this strain at {@code dose}, lasting
     * {@code durationTicks}. Dose 1 reproduces the old {@code LIGHT} numbers exactly and dose 2 the
     * old {@code HEAVY} ones, so this rework changed no shipped effect value.
     */
    public List<StatusEffectInstance> effects(int dose, int durationTicks) {
        List<StatusEffectInstance> out = new ArrayList<>(smokeEffects.size());
        for (SmokeEffect effect : smokeEffects) {
            int amplifier = effect.baseAmplifier() + (effect.scales() ? Math.max(0, dose - 1) : 0);
            out.add(new StatusEffectInstance(effect.effect(), durationTicks, amplifier));
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

    @Override
    public String asString() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * One effect a strain grants when smoked.
     *
     * @param baseAmplifier amplifier at dose 1 (0 = level I)
     * @param scales        whether dose raises it; {@code false} pins it at {@code baseAmplifier}
     */
    public record SmokeEffect(RegistryEntry<StatusEffect> effect, int baseAmplifier, boolean scales) {
    }
}

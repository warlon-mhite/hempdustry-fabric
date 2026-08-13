package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A cannabis strain: its buds, its flower, its colour, and the effects it applies when smoked.
 *
 * <h2>This is a datapack registry, not an enum</h2>
 *
 * Strains live in {@code data/<namespace>/strain/<id>.json} and load into the synced dynamic registry
 * {@link #REGISTRY_KEY}. A server owner rebalances what a strain does by shipping a datapack and
 * running {@code /reload}; no config file, no restart, and <b>the definitions reach clients for
 * free</b> because the registry is registered with {@code DynamicRegistries.registerSynced}.
 *
 * <p>This is the shape vanilla moved enchantments to in 1.21, and the one this mod already used for
 * its paintings and jukebox songs. See {@code roadmap.md} D14 for the decision and its costs.
 *
 * <p><b>It buys editing, not turnkey new strains.</b> A genuinely new strain still needs seeds, buds,
 * a crop block, textures, models and recipes — all code and assets, and the spliff and packing
 * recipes are datagen'd per strain. A datapack can add an entry that reuses existing items; it
 * cannot conjure one from nothing. {@link ModStrains#BUILT_IN} is the list the mod ships art for.
 *
 * <h2>Effects are data, and dose is the amplifier</h2>
 *
 * A strain owns <em>what</em> it does; the device owns <em>how long</em>, and the <em>dose</em> (how
 * many buds went in) owns how strongly:
 *
 * <pre>
 *   amplifier = baseAmplifier + (dose - 1)      // for effects that scale
 *   duration  = the device's own duration       // never touched by dose
 * </pre>
 *
 * <p><b>Duration deliberately does not shrink as the amplifier rises</b>, which is where this departs
 * from vanilla's glowstone rule. That rule works for potions because a potion is single-signed — all
 * buff or all debuff. A strain is a <em>bundle</em> of both (Purple Kush is Resistance <i>and</i>
 * Mining Fatigue), so halving the duration would shorten the penalty too and dosing would partly
 * reward itself. The brake is instead that <b>dose amplifies the debuff as well</b>.
 *
 * <p>Effects flagged {@code scales = false} sit out of that — Hunger is flat at level I however much
 * you smoke, for the same reason it is identical across strains: the munchies don't care.
 *
 * <h2>{@code model_index} is why this is safe for the art</h2>
 *
 * Per-strain art is model overrides keyed on the {@code hempdustry:strain} item property, which used
 * to be the enum's {@code ordinal() + 1}. <b>Registry iteration order is not guaranteed stable</b>,
 * so an ordinal-shaped index would eventually show one strain's texture on another's spliff — a
 * client-side failure that is visual rather than loud. The index therefore lives <em>in the data</em>
 * and is read straight off the entry. A strain with no index, or one the client has no override for,
 * falls back to the base model rather than borrowing someone else's art.
 *
 * <p>Display names live in the lang files under the {@code translation_key} each entry names; the
 * folk sativa/indica <em>effect</em> split is genre furniture, not botany — see CLAUDE.md.
 *
 * @param translationKey lang key for the display name, e.g. {@code hempdustry.strain.indica}
 * @param color          packed device / spliff tint, the way a potion tints its liquid layer
 * @param modelIndex     stable art index; {@code 0} means "no art of its own"
 * @param seeds          the seed item that plants this strain's crop
 * @param buds           the bud item that packs into a spliff, pipe or bong
 * @param flower         the wild flower that drops this strain's seeds
 * @param smokeEffects   what one hit applies, before dose scaling
 */
public record Strain(String translationKey, int color, int modelIndex,
                     Item seeds, Item buds, Block flower, List<SmokeEffect> smokeEffects) {

    /** The dynamic registry itself. Entries load from {@code data/<namespace>/strain/<id>.json}. */
    public static final RegistryKey<Registry<Strain>> REGISTRY_KEY =
            RegistryKey.ofRegistry(Identifier.of(Hempdustry.MOD_ID, "strain"));

    public static final Codec<Strain> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(Strain::translationKey),
            Codec.INT.fieldOf("color").forGetter(Strain::color),
            Codec.INT.optionalFieldOf("model_index", 0).forGetter(Strain::modelIndex),
            Registries.ITEM.getCodec().fieldOf("seeds").forGetter(Strain::seeds),
            Registries.ITEM.getCodec().fieldOf("buds").forGetter(Strain::buds),
            Registries.BLOCK.getCodec().fieldOf("flower").forGetter(Strain::flower),
            SmokeEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Strain::smokeEffects)
    ).apply(instance, Strain::new));

    /**
     * How a <em>reference</em> to a strain is written — in the {@code smoke_contents} component, and
     * anywhere else that points at one. {@link RegistryFixedCodec} is what vanilla uses for the same
     * job in {@code JukeboxPlayableComponent}, and it refuses inline entries, so a stack always
     * carries an id rather than a copy of the definition.
     */
    public static final Codec<RegistryEntry<Strain>> ENTRY_CODEC = RegistryFixedCodec.of(REGISTRY_KEY);

    public static final PacketCodec<RegistryByteBuf, RegistryEntry<Strain>> ENTRY_PACKET_CODEC =
            PacketCodecs.registryEntry(REGISTRY_KEY);

    /** The strain registry as loaded for this world. */
    public static RegistryWrapper.Impl<Strain> registry(RegistryWrapper.WrapperLookup registries) {
        return registries.getWrapperOrThrow(REGISTRY_KEY);
    }

    /**
     * Every loaded strain, in a <b>deterministic</b> order: by {@link #modelIndex} first, then by id.
     * Registry iteration order is not guaranteed, and this list drives the creative tab and the seed
     * loot pools, so an unstable order would shuffle the tab between launches.
     */
    public static List<RegistryEntry.Reference<Strain>> all(RegistryWrapper.WrapperLookup registries) {
        List<RegistryEntry.Reference<Strain>> out = new ArrayList<>(registry(registries).streamEntries().toList());
        out.sort(Comparator.comparingInt((RegistryEntry.Reference<Strain> entry) -> entry.value().modelIndex())
                .thenComparing(entry -> entry.registryKey().getValue()));
        return out;
    }

    /**
     * The strain whose buds are {@code item}, if any is loaded.
     *
     * <p>Deliberately <b>not</b> routed through {@link #all}: a lookup by identity has no use for a
     * defined order, and paying for one here is not free. This runs from
     * {@code PackingRecipe.matches}, which the server calls on <em>every</em> crafting-grid change
     * for every player, once per non-device stack in the grid — so going through {@code all} meant
     * up to nine registry copies and nine sorts per click, all to answer a question that is a scan.
     * Ordering matters where the result is <em>shown</em> (the creative tab, the seed loot pools);
     * it never matters here.
     */
    public static Optional<RegistryEntry.Reference<Strain>> fromBuds(RegistryWrapper.WrapperLookup registries, Item item) {
        return registry(registries).streamEntries()
                .filter(entry -> entry.value().buds() == item)
                .findFirst();
    }

    /**
     * Fresh status-effect instances for one hit of this strain at {@code dose}, lasting
     * {@code durationTicks}. A strain with no effects — including one a datapack has emptied
     * deliberately — simply applies nothing, which is also the fallback for a strain the world no
     * longer defines.
     */
    public List<StatusEffectInstance> effects(int dose, int durationTicks) {
        List<StatusEffectInstance> out = new ArrayList<>(smokeEffects.size());
        for (SmokeEffect effect : smokeEffects) {
            int amplifier = effect.baseAmplifier() + (effect.scales() ? Math.max(0, dose - 1) : 0);
            out.add(new StatusEffectInstance(effect.effect(), durationTicks, amplifier));
        }
        return out;
    }

    /**
     * One effect a strain grants when smoked.
     *
     * <p>Both numbers are optional in JSON so a datapack entry can be as short as
     * {@code {"effect": "minecraft:speed"}} — level I, dose-scaling, which is what every shipped
     * effect but Hunger is.
     *
     * @param baseAmplifier amplifier at dose 1 (0 = level I)
     * @param scales        whether dose raises it; {@code false} pins it at {@code baseAmplifier}
     */
    public record SmokeEffect(RegistryEntry<StatusEffect> effect, int baseAmplifier, boolean scales) {
        public static final Codec<SmokeEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StatusEffect.ENTRY_CODEC.fieldOf("effect").forGetter(SmokeEffect::effect),
                Codec.INT.optionalFieldOf("base_amplifier", 0).forGetter(SmokeEffect::baseAmplifier),
                Codec.BOOL.optionalFieldOf("scales", true).forGetter(SmokeEffect::scales)
        ).apply(instance, SmokeEffect::new));
    }
}

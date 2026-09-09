package com.warlonmhite.hempdustry.strain;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.Strain.SmokeEffect;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The strains this mod ships, and the registration of the registry itself.
 *
 * <h2>Two lists, and the difference matters</h2>
 *
 * <ul>
 *   <li>{@link #BUILT_IN} — the strains the mod ships <b>art and recipes</b> for. Code-side, ordered,
 *       and read by datagen: the spliff models and their override indices, the per-strain recipes,
 *       the item tags. A datapack cannot extend it, because a datapack cannot add a texture.</li>
 *   <li><b>The registry</b> — every strain <em>loaded</em>, whatever a datapack has done to it. Read
 *       at runtime by the creative tab, the loot pools, packing and smoking. This is the one that
 *       decides behaviour.</li>
 * </ul>
 *
 * The two are the same at the moment, and they are meant to drift: editing Purple Kush's effects
 * changes the registry and not this file.
 *
 * <h2>Model indices are assigned here, once</h2>
 *
 * {@link #modelIndex} is a strain's position in {@link #BUILT_IN} plus one, and that number is
 * written into the entry the bootstrap builds. It is <b>not</b> a registry ordinal, which would not
 * be stable — see {@link Strain} for what that would break. Both the datagen'd model overrides and
 * the client's item property read this same number, so there is one source for it.
 *
 * <h2>Who may use which index</h2>
 *
 * {@code model_index} is a number in shared data with no namespace, so two addons picking the same
 * one is a real possibility and its symptom is one strain wearing another's art — a silent, visual
 * failure. The range is therefore split, and {@link #validateModelIndices} logs anything that
 * breaks the split or collides outright, so it fails in the log instead of only on screen:
 *
 * <ul>
 *   <li><b>0</b> — "no bespoke art of my own", and the <b>normal</b> value. Such a strain takes the
 *       shared look and is told apart by its {@code color}. Anything a datapack adds should use
 *       this, since a datapack cannot ship a texture.</li>
 *   <li><b>{@value #RESERVED_MODEL_INDEX_MIN}–{@value #RESERVED_MODEL_INDEX_MAX}</b> — reserved for
 *       Hempdustry's own strains, which is where {@link #BUILT_IN} sits.</li>
 *   <li><b>{@value #THIRD_PARTY_MODEL_INDEX_MIN} and up</b> — for another mod shipping its own art
 *       and its own resource-pack overrides.</li>
 * </ul>
 */
public class ModStrains {

    public static final RegistryKey<Strain> INDICA = key("indica");
    public static final RegistryKey<Strain> SATIVA = key("sativa");
    /** Not a plant: the Dry Sifter's product, strainless by construction. No seeds, no flower. */
    public static final RegistryKey<Strain> HASHISH = key("hashish");
    /** Not a plant either: resin rubbed off a living plant while trimming it. */
    public static final RegistryKey<Strain> CHARAS = key("charas");

    /**
     * The strains the mod ships art and recipes for. <b>Append only, and never reorder</b> —
     * {@link #modelIndex} is a position in this list, so moving an entry silently repaints every
     * stack in every existing world with somebody else's art. That is the exact failure
     * {@code model_index} exists to prevent, and it fails visually rather than loudly.
     *
     * <p>Not all of them are plants. {@code HASHISH} has no seeds and no flower; see {@link Strain}.
     */
    public static final List<RegistryKey<Strain>> BUILT_IN = List.of(INDICA, SATIVA, HASHISH, CHARAS);

    /** First {@code model_index} this mod claims for its own art. */
    public static final int RESERVED_MODEL_INDEX_MIN = 1;
    /** Last {@code model_index} this mod claims. Generous on purpose — it costs nothing to reserve. */
    public static final int RESERVED_MODEL_INDEX_MAX = 99;
    /** First {@code model_index} another mod may use for art of its own. */
    public static final int THIRD_PARTY_MODEL_INDEX_MIN = 100;

    /**
     * Registers the registry itself. <b>Synced</b>, so a datapack's strain definitions reach every
     * client without a packet of our own; the tinting and the naming both read the loaded entry.
     *
     * <p>No {@code SKIP_WHEN_EMPTY}: the mod's own strains are always present, and a client without
     * the registry is a client without the mod.
     */
    public static void registerStrains() {
        DynamicRegistries.registerSynced(Strain.REGISTRY_KEY, Strain.CODEC);
        // Checked once at startup and again on every /reload, because a datapack is exactly where a
        // clashing index comes from and a reload is when it would arrive.
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                validateModelIndices(server.getRegistryManager()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                validateModelIndices(server.getRegistryManager());
            }
        });
        Hempdustry.LOGGER.info("Registering Strains for " + Hempdustry.MOD_ID);
    }

    /**
     * Complains about {@code model_index} values that will misbehave, so a problem whose only other
     * symptom is <em>the wrong picture on an item</em> also shows up somewhere greppable.
     *
     * <p>Two things are worth a warning. A <b>collision</b> is the serious one: model overrides match
     * on {@code >=}, so two strains sharing an index means whichever override is listed last wins for
     * both, and one strain silently wears the other's art. A <b>strain from another namespace sitting
     * in this mod's reserved range</b> is the near miss — harmless until Hempdustry ships a strain
     * with that index, at which point it becomes a collision in someone else's build.
     *
     * <p>Warnings only, never a hard failure: a wrong texture is not worth refusing to load a world
     * over, and the server owner may not be the person who can fix the datapack.
     */
    private static void validateModelIndices(RegistryWrapper.WrapperLookup registries) {
        Map<Integer, Identifier> claimed = new HashMap<>();
        for (RegistryEntry.Reference<Strain> entry : Strain.all(registries)) {
            int index = entry.value().modelIndex();
            if (index == 0) {
                continue; // "no art of my own" — shared by design, never a clash
            }
            Identifier id = entry.registryKey().getValue();
            Identifier previous = claimed.putIfAbsent(index, id);
            if (previous != null) {
                Hempdustry.LOGGER.warn(
                        "Strains {} and {} both declare model_index {} — one will wear the other's art. "
                                + "Give one of them a different index, or 0 to take the shared look.",
                        previous, id, index);
            } else if (!Hempdustry.MOD_ID.equals(id.getNamespace())
                    && index >= RESERVED_MODEL_INDEX_MIN && index <= RESERVED_MODEL_INDEX_MAX) {
                Hempdustry.LOGGER.warn(
                        "Strain {} declares model_index {}, which is inside Hempdustry's reserved range "
                                + "{}-{}. Use {} or above for a strain with art of its own, or 0 to take "
                                + "the shared look.",
                        id, index, RESERVED_MODEL_INDEX_MIN, RESERVED_MODEL_INDEX_MAX,
                        THIRD_PARTY_MODEL_INDEX_MIN);
            }
        }
    }

    /** The art index a built-in strain gets. Datagen and the bootstrap both read this. */
    public static int modelIndex(RegistryKey<Strain> key) {
        int index = BUILT_IN.indexOf(key);
        return index < 0 ? 0 : index + RESERVED_MODEL_INDEX_MIN;
    }

    /** The registry id's path — {@code indica} — which is what texture and recipe names are built on. */
    public static String id(RegistryKey<Strain> key) {
        return key.getValue().getPath();
    }

    public static void bootstrap(Registerable<Strain> context) {
        // Purple Kush — the body high: hard to hurt, hard to get anything done.
        context.register(INDICA, new Strain("hempdustry.strain.indica", 0x8E6FB5, modelIndex(INDICA),
                Optional.of(ModItems.INDICA_SEEDS), ModItems.INDICA_BUDS,
                Optional.of(ModBlocks.INDICA_FLOWER), 1.0F,
                List.of(
                        new SmokeEffect(StatusEffects.RESISTANCE, 0, true),
                        new SmokeEffect(StatusEffects.HUNGER, 0, false),
                        new SmokeEffect(StatusEffects.MINING_FATIGUE, 0, true))));

        // Lemon Haze — the head high, and a deliberate mirror of Purple Kush: where indica buffs
        // defence and taxes mining, sativa buffs movement and mining and taxes melee damage. Hunger
        // is in both because the munchies don't care which strain you smoked.
        context.register(SATIVA, new Strain("hempdustry.strain.sativa", 0xC7D14A, modelIndex(SATIVA),
                Optional.of(ModItems.SATIVA_SEEDS), ModItems.SATIVA_BUDS,
                Optional.of(ModBlocks.SATIVA_FLOWER), 1.0F,
                List.of(
                        new SmokeEffect(StatusEffects.SPEED, 0, true),
                        new SmokeEffect(StatusEffects.HASTE, 0, true),
                        new SmokeEffect(StatusEffects.HUNGER, 0, false),
                        new SmokeEffect(StatusEffects.WEAKNESS, 0, true))));

        // Hashish -- strainless by construction. Sifting keeps the trichome heads and throws the
        // plant away, and a trichome head is a trichome head whichever plant grew it. Its identity
        // is the product's, not the cultivar's, which is how hash is actually named and sold.
        //
        // Resistance + Slowness + Hunger is the hash-family body: heavy, sedating, hard to bother,
        // hard to hurry, hungry. Where the two plants share only Hunger, the hash entries share
        // three effects and differ by one signature -- a family, against two individuals.
        //
        // Night Vision is this one's signature and it is sourced. Moroccan kif is *sifted resin*,
        // which is exactly what this block makes, and Russo et al. (2004) measured dark adaptation
        // and scotopic sensitivity in Rif-mountain kif smokers after the local fishermen's own
        // reports, with a double-blinded graduated-THC arm alongside the field study. Small n and a
        // case study rather than a trial -- but real, peer-reviewed, and about this exact product.
        //
        // Neither Night Vision nor Slowness scales: Night Vision has no meaningful amplifier in
        // vanilla (level II is identical to level I), so flat is forced there rather than chosen.
        // Resistance is the one scaling effect, which is what stops dosing hash being a pure
        // downside -- but one piece is usually the right answer, which is a genuinely different
        // dose curve from the plants' and is also how hash is used.
        context.register(HASHISH, new Strain("hempdustry.strain.hashish", 0x6B4A2F, modelIndex(HASHISH),
                Optional.empty(), ModItems.HASHISH, Optional.empty(), 1.0F,
                List.of(
                        new SmokeEffect(StatusEffects.NIGHT_VISION, 0, false),
                        new SmokeEffect(StatusEffects.RESISTANCE, 0, true),
                        new SmokeEffect(StatusEffects.SLOWNESS, 0, false),
                        new SmokeEffect(StatusEffects.HUNGER, 0, false))));

        // Charas -- the same hash body as hashish, and the family's second signature.
        //
        // SLOW FALLING, and the deciding argument is a rarity one rather than a flavour one. Every
        // hash signature has to be matched against the vanilla source of the same effect:
        //
        //   Night Vision  golden carrot     trivial to get       <- hashish, made in bulk from waste
        //   Slow Falling  phantom membrane  three nights awake   <- charas, ~4 plants, by hand, never automatable
        //
        // The scarcity of the mod item matches the scarcity of the vanilla alternative it competes
        // with, which is what stops either being a shortcut past a gate vanilla already charges for.
        // Charas is not "rare hashish"; it is the cheap route to the effect vanilla makes you stay
        // awake for, and that is a niche it keeps for ever rather than a stage it passes through.
        // The heavy, floaty body stone made mechanical is the flavour that agrees with it.
        //
        // Flat, like every signature in the family: Night Vision has no meaningful amplifier in
        // vanilla, so hashish's is forced flat, and this one matches rather than diverging.
        //
        // Near-black, because that is what resin taken off a living plant and never dried actually
        // looks like -- oxidised as it forms. Colour follows process here and means nothing else:
        // a gold bar and a black bar can test the same, and no colour in this mod claims a number.
        context.register(CHARAS, new Strain("hempdustry.strain.charas", 0x2B2118, modelIndex(CHARAS),
                Optional.empty(), ModItems.CHARAS, Optional.empty(), 1.0F,
                List.of(
                        new SmokeEffect(StatusEffects.SLOW_FALLING, 0, false),
                        new SmokeEffect(StatusEffects.RESISTANCE, 0, true),
                        new SmokeEffect(StatusEffects.SLOWNESS, 0, false),
                        new SmokeEffect(StatusEffects.HUNGER, 0, false))));
    }

    private static RegistryKey<Strain> key(String name) {
        return RegistryKey.of(Strain.REGISTRY_KEY, Identifier.of(Hempdustry.MOD_ID, name));
    }
}

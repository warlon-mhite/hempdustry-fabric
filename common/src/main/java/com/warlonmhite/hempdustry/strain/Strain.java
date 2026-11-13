package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.Hempdustry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.codec.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * A smokeable material and what it does: what packs into a device, its colour, and the effects one
 * hit applies. For a plant strain that is buds, seeds and a wild flower; for hashish it is the
 * product alone.
 *
 * <h2>This is a datapack registry, not an enum</h2>
 *
 * Strains live in {@code data/<namespace>/hempdustry/strain/<id>.json} and load into the synced
 * dynamic registry {@link #REGISTRY_KEY}. A server owner rebalances what a strain does by shipping a
 * datapack and restarting — a dynamic registry is read once, when the world opens, and
 * {@code /reload} does not touch it — with no config file, and <b>the definitions reach clients for
 * free</b> because the registry is registered with {@code DynamicRegistries.registerSynced}.
 *
 * <p><b>That path has {@code hempdustry} in it twice for the mod's own strains, and that is correct.</b>
 * {@code RegistryDataLoader} builds a dynamic registry's directory from the registry id: a vanilla
 * registry like {@code minecraft:worldgen/biome} gets {@code worldgen/biome}, but any registry
 * outside the {@code minecraft} namespace gets {@code <namespace>/<path>}. So the folder is
 * {@code hempdustry/strain}, and it sits under whichever pack namespace owns the file — giving
 * {@code data/hempdustry/hempdustry/strain/indica.json} for ours and
 * {@code data/yourpack/hempdustry/strain/whatever.json} for a third party's. A file at
 * {@code data/<namespace>/strain/} is simply never read, and <b>nothing logs a complaint</b> — the
 * strain just silently does not exist, which is a miserable thing to debug.
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
 * <p>Effects flagged {@code scales = false} sit out of that. Hunger used to be one, flat at level I
 * however much you smoked; it scales since 2.0.1, because a flat Hunger I cost a bong of three about
 * one food point, all of it saturation, and a fed player never saw it.
 *
 * <p>The amplifier here is what the strain asks for. {@code EffectPolicy} then stops a buff at
 * {@code maxBuffLevel} and leaves a cost alone, so a dose of three is II of the good part and III of
 * the price.
 *
 * <h2>{@code model_index} is why this is safe for the art</h2>
 *
 * Per-strain art is model overrides keyed on the {@code hempdustry:strain} item property, which used
 * to be the enum's {@code ordinal() + 1}. <b>Registry iteration order is not guaranteed stable</b>,
 * so an ordinal-shaped index would eventually show one strain's texture on another's spliff — a
 * client-side failure that is visual rather than loud. The index therefore lives <em>in the data</em>
 * and is read straight off the entry.
 *
 * <p><b>{@code 0} is the normal value and means "no art of my own"</b>: such a strain takes the
 * shared look and is told apart by its {@link #color}. A non-zero index says "I ship bespoke art at
 * this slot", which is the rarer case and the one {@link ModStrains} splits into reserved ranges.
 * <em>Whether</em> a device is loaded at all is a separate property, {@code hempdustry:packed} —
 * these were once the same number and that was a bug.
 *
 * <p>Display names live in the lang files under the {@code translation_key} each entry names; the
 * folk sativa/indica <em>effect</em> split is genre furniture, not botany: genetic studies find no
 * consistent difference between what is sold under the two labels.
 *
 * <h2>This codec is a public contract</h2>
 *
 * Every field below is read out of JSON that <b>someone else may have written</b> — a server owner's
 * datapack, another mod's data, a pack shipped by a third party. Once 2.0.0 is out, that shape is
 * frozen in the same way a registry id is:
 *
 * <ul>
 *   <li><b>Any field added later must be {@code optionalFieldOf} with a default.</b> A new required
 *       field silently invalidates every strain JSON already in the wild, and the failure surfaces as
 *       a world that will not load rather than as anything naming the mod that changed.</li>
 *   <li><b>No field may be removed or renamed</b>, and none may narrow what it accepts. Widening is
 *       fine.</li>
 *   <li>The same applies to {@link SmokeEffect}, which is nested inside this one.</li>
 * </ul>
 *
 * The same promise covers every id, tag and data shape a released build had, and the game tests
 * fail on any of them going missing.
 *
 * <h2>A strain is a smokeable material, not necessarily a plant</h2>
 *
 * <b>{@code seeds} and {@code flower} are optional, and {@code buds} is not.</b> Every entry names
 * the item that packs into a spliff, pipe or bong; only the ones that <em>grew on a plant</em> have
 * a seed to sow and a wild flower to find. Hashish, charas and filtered hashish are the strainless
 * ones: sifting keeps the trichome heads and throws the plant away, and a trichome head is a
 * trichome head whichever plant grew it. Their identity is the product's, which is how hash is
 * actually named and sold — by region and method, never by cultivar.
 *
 * <p><b>{@code wildFlower().isPresent()} is therefore the predicate for "this grew on a plant"</b>, and
 * it is the one every guard in the mod keys on: the creative tab's seed and flower runs, the grass
 * seed pools, {@code #hempdustry:siftable/flower}, and the plain spliff recipes. One predicate, and
 * anything hash-shaped added later is covered by it for free.
 *
 * <p>The two fields are {@code seedItem} and {@code wildFlower} in code, though their JSON keys are
 * still {@code seeds} and {@code flower}: 2.0 shipped {@code seeds()} and {@code flower()} returning
 * a plain item and block, and an addon compiled against 2.0 links against exactly those. They are
 * kept below as deprecated bridges, beside 2.0's constructor and {@link #effects(int, int)}.
 *
 * @param translationKey  lang key for the display name, e.g. {@code hempdustry.strain.indica}
 * @param color           packed device / spliff tint, the way a potion tints its liquid layer
 * @param modelIndex      stable art index; {@code 0} means "no art of its own"
 * @param seedItem        the seed item that plants this strain's crop, if it is a plant at all
 * @param buds            the item that packs into a spliff, pipe or bong. Every strain has one
 * @param wildFlower      the wild flower that drops this strain's seeds, if it is a plant at all. Warped
 *                        Kush's is its own crop block, which the world generates ripe
 * @param greenOutFactor  divides the green-out odds; {@code 2.0} halves the risk. Purity buys
 *                        smoothness, never power — see {@code hashish.md} §4
 * @param dosePerItem     how much dose <em>one</em> of {@link #buds} is worth when packed. Almost
 *                        always {@code 1}: a bud is a bud. Rosin is {@code 3}, because a dab is not
 *                        a step on a ladder — one piece <em>is</em> the bowl. See
 *                        {@code PackingRecipe}, where the device's {@code maxDose} is what then
 *                        makes a concentrate bong-only without a rule having to say so
 * @param smokeEffects    what one hit applies, before dose scaling
 * @param coughFactor     widens the device's cough odds the way {@code greenOutFactor} widens the
 *                        green-out's: {@code 2.0} halves them, {@code 0.5} doubles them. Beldía's
 *                        harsh leafy smoke is the {@code 0.5}. The cough is a sound, so this is
 *                        flavour rather than balance
 * @param chemotype       what the plant makes, THC, CBD, both or neither (see {@link Chemotype}). A
 *                        bud's and a seed's chemotype is always their strain's; everything else from
 *                        the plant is stamped with it as it drops. THC-dominant when the file names none
 * @param terpene         the terpene its smell is known for (see {@link Terpene}), if any: its name on
 *                        the bud's and the seed's tooltip, and the colour of its smoke and its plant's
 *                        aroma. A cosmetic: it changes nothing the strain does
 * @param smokeColor      the colour its smoke takes, RGB, when it is not its terpene's: schwag has no
 *                        terpene and smokes the colour of stale hay
 * @param potency         a plant strain's potency sheet (see {@link Potency}): a mid bud's worth at the
 *                        oven and the odds of each band a plant rolls. {@link Potency#DEFAULT} when the
 *                        file names none; read only for the buds of a plant
 * @param jarColour       the colour of its buds' pistils in a Stash Jar, RGB, when it is not the strain's own
 *                        {@code color}: a strain whose colour would vanish on a green bud can name another
 * @param plant           what a plant of the strain rolls at planting beyond its band (see {@link Plant}): its
 *                        quality sheet and its resistance sheet, written in the file's {@code plant} object.
 *                        {@link Plant#DEFAULT} when the file names none; read only for a plant
 */
public record Strain(String translationKey, int color, int modelIndex,
                     Optional<Item> seedItem, Item buds, Optional<Block> wildFlower,
                     float greenOutFactor, int dosePerItem, List<SmokeEffect> smokeEffects,
                     float coughFactor, Chemotype chemotype, Optional<ResourceKey<Terpene>> terpene,
                     Optional<Integer> smokeColor, Potency potency, Optional<Integer> jarColour,
                     Plant plant) {

    /** A strain with the default plant sheets: the shape every strain had before {@code plant}. */
    public Strain(String translationKey, int color, int modelIndex,
                  Optional<Item> seedItem, Item buds, Optional<Block> wildFlower,
                  float greenOutFactor, int dosePerItem, List<SmokeEffect> smokeEffects,
                  float coughFactor, Chemotype chemotype, Optional<ResourceKey<Terpene>> terpene,
                  Optional<Integer> smokeColor, Potency potency, Optional<Integer> jarColour) {
        this(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem, smokeEffects,
                coughFactor, chemotype, terpene, smokeColor, potency, jarColour, Plant.DEFAULT);
    }

    /**
     * What a plant of a strain rolls at planting beyond its potency band, kept in the strain file's {@code plant}
     * object: {@code "plant": {"quality": {...}, "resistances": {...}}}. Both keys are optional.
     *
     * @param quality     the base luck of its harvests and the odds of each quality gene (see {@link GrowQuality})
     * @param resistances the odds of each resistance step, per phenomenon (see {@link Resistances})
     */
    public record Plant(GrowQuality quality, Resistances resistances) {

        /** The default quality sheet and neutral on every phenomenon: a strain that names no {@code plant}. */
        public static final Plant DEFAULT = new Plant(GrowQuality.DEFAULT, Resistances.DEFAULT);

        public static final Codec<Plant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GrowQuality.CODEC.optionalFieldOf("quality", GrowQuality.DEFAULT).forGetter(Plant::quality),
                Resistances.CODEC.optionalFieldOf("resistances", Resistances.DEFAULT).forGetter(Plant::resistances)
        ).apply(instance, Plant::new));

        /** The {@code plant} object as a strain file carries it. */
        static final MapCodec<Plant> FIELD = CODEC.optionalFieldOf("plant", DEFAULT);
    }

    /** A plant strain's quality sheet: the base luck of its harvests and the odds of each quality gene a plant rolls. */
    public GrowQuality quality() {
        return plant.quality();
    }

    /** A plant strain's resistance sheet: the odds of each resistance step a plant rolls, per phenomenon. */
    public Resistances resistances() {
        return plant.resistances();
    }

    /** A strain with no jar colour of its own: the shape every strain had before {@code jar_colour}. */
    public Strain(String translationKey, int color, int modelIndex,
                  Optional<Item> seedItem, Item buds, Optional<Block> wildFlower,
                  float greenOutFactor, int dosePerItem, List<SmokeEffect> smokeEffects,
                  float coughFactor, Chemotype chemotype, Optional<ResourceKey<Terpene>> terpene,
                  Optional<Integer> smokeColor, Potency potency) {
        this(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem, smokeEffects,
                coughFactor, chemotype, terpene, smokeColor, potency, Optional.empty());
    }

    /** A strain with the default potency sheet. */
    public Strain(String translationKey, int color, int modelIndex,
                  Optional<Item> seedItem, Item buds, Optional<Block> wildFlower,
                  float greenOutFactor, int dosePerItem, List<SmokeEffect> smokeEffects,
                  float coughFactor, Chemotype chemotype, Optional<ResourceKey<Terpene>> terpene,
                  Optional<Integer> smokeColor) {
        this(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem, smokeEffects,
                coughFactor, chemotype, terpene, smokeColor, Potency.DEFAULT);
    }

    /** A strain with no terpene and the plain smoke. */
    public Strain(String translationKey, int color, int modelIndex,
                  Optional<Item> seedItem, Item buds, Optional<Block> wildFlower,
                  float greenOutFactor, int dosePerItem, List<SmokeEffect> smokeEffects,
                  float coughFactor, Chemotype chemotype) {
        this(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem, smokeEffects,
                coughFactor, chemotype, Optional.empty(), Optional.empty());
    }

    /** Every strain written before {@code chemotype} existed is THC-dominant, as 2.1 had them all. */
    public Strain(String translationKey, int color, int modelIndex,
                  Optional<Item> seedItem, Item buds, Optional<Block> wildFlower,
                  float greenOutFactor, int dosePerItem, List<SmokeEffect> smokeEffects,
                  float coughFactor) {
        this(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem, smokeEffects,
                coughFactor, Chemotype.THC_DOMINANT);
    }

    /** Every strain written before {@code cough_factor} existed coughs like the device does. */
    public Strain(String translationKey, int color, int modelIndex,
                  Optional<Item> seedItem, Item buds, Optional<Block> wildFlower,
                  float greenOutFactor, int dosePerItem, List<SmokeEffect> smokeEffects) {
        this(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem, smokeEffects, 1.0F);
    }

    /**
     * 2.0's constructor: a plant strain, smoothing and coughing like its device, one dose a bud.
     *
     * @deprecated kept so an addon built against 2.0 still links; use the canonical constructor
     */
    @Deprecated
    public Strain(String translationKey, int color, int modelIndex,
                  Item seeds, Item buds, Block flower, List<SmokeEffect> smokeEffects) {
        this(translationKey, color, modelIndex, Optional.ofNullable(seeds), buds, Optional.ofNullable(flower),
                1.0F, 1, smokeEffects);
    }

    /**
     * 2.0's accessor. Air for a strain that never grew on a plant, which 2.0 never had.
     *
     * @deprecated kept so an addon built against 2.0 still links; use {@link #seedItem()}
     */
    @Deprecated
    public Item seeds() {
        return seedItem.orElse(Items.AIR);
    }

    /**
     * 2.0's accessor. Air for a strain that never grew on a plant, which 2.0 never had.
     *
     * @deprecated kept so an addon built against 2.0 still links; use {@link #wildFlower()}
     */
    @Deprecated
    public Block flower() {
        return wildFlower.orElse(Blocks.AIR);
    }

    /** This strain with {@code terpene} as its signature. */
    public Strain withTerpene(ResourceKey<Terpene> terpene) {
        return new Strain(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem,
                smokeEffects, coughFactor, chemotype, Optional.of(terpene), smokeColor, potency, jarColour, plant);
    }

    /** This strain smoking {@code rgb} rather than its terpene's colour. */
    public Strain withSmokeColor(int rgb) {
        return new Strain(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem,
                smokeEffects, coughFactor, chemotype, terpene, Optional.of(rgb), potency, jarColour, plant);
    }

    /** This strain with {@code potency} as its sheet. */
    public Strain withPotency(Potency potency) {
        return new Strain(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem,
                smokeEffects, coughFactor, chemotype, terpene, smokeColor, potency, jarColour, plant);
    }

    /** This strain's pistils in a Stash Jar tinted {@code rgb} rather than its own colour. */
    public Strain withJarColour(int rgb) {
        return new Strain(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem,
                smokeEffects, coughFactor, chemotype, terpene, smokeColor, potency, Optional.of(rgb), plant);
    }

    /** This strain with {@code quality} as its quality sheet. */
    public Strain withQuality(GrowQuality quality) {
        return new Strain(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem,
                smokeEffects, coughFactor, chemotype, terpene, smokeColor, potency, jarColour,
                new Plant(quality, plant.resistances()));
    }

    /** This strain with {@code resistances} as its resistance sheet. */
    public Strain withResistances(Resistances resistances) {
        return new Strain(translationKey, color, modelIndex, seedItem, buds, wildFlower, greenOutFactor, dosePerItem,
                smokeEffects, coughFactor, chemotype, terpene, smokeColor, potency, jarColour,
                new Plant(plant.quality(), resistances));
    }

    /** The colour of its buds' pistils in a Stash Jar, RGB: its {@code jar_colour}, else its own {@link #color}. */
    public int jarColourOrDefault() {
        return jarColour.orElse(color);
    }

    /** The terpene the world loaded under this strain's {@link #terpene} key, if both exist. */
    public Optional<Terpene> loadedTerpene(HolderLookup.Provider registries) {
        return terpene.flatMap(key -> Terpene.resolve(registries, key));
    }

    /** The colour its smoke takes: its own smoke colour, else its terpene's, else none (plain smoke). */
    public Optional<Integer> smokeTint(HolderLookup.Provider registries) {
        return smokeColor.or(() -> loadedTerpene(registries).map(Terpene::color));
    }

    /** The dynamic registry itself. Entries load from {@code data/<namespace>/hempdustry/strain/<id>.json}. */
    public static final ResourceKey<Registry<Strain>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "strain"));

    public static final Codec<Strain> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(Strain::translationKey),
            Codec.INT.fieldOf("color").forGetter(Strain::color),
            Codec.INT.optionalFieldOf("model_index", 0).forGetter(Strain::modelIndex),
            // seeds and flower are optional because a hash entry has neither -- see the class
            // javadoc. Widening a required field to optional is the one schema change compat.md
            // permits: every strain JSON already written keeps parsing unchanged.
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("seeds").forGetter(Strain::seedItem),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("buds").forGetter(Strain::buds),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("flower").forGetter(Strain::wildFlower),
            Codec.FLOAT.optionalFieldOf("green_out_factor", 1.0F).forGetter(Strain::greenOutFactor),
            Codec.INT.optionalFieldOf("dose_per_item", 1).forGetter(Strain::dosePerItem),
            SmokeEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Strain::smokeEffects),
            Codec.FLOAT.optionalFieldOf("cough_factor", 1.0F).forGetter(Strain::coughFactor),
            Chemotype.CODEC.optionalFieldOf("chemotype", Chemotype.THC_DOMINANT).forGetter(Strain::chemotype),
            // By key, looked up when used: a strain naming a terpene the world does not define still
            // loads, and smokes plain.
            ResourceKey.codec(Terpene.REGISTRY_KEY).optionalFieldOf("terpene").forGetter(Strain::terpene),
            Codec.INT.optionalFieldOf("smoke_color").forGetter(Strain::smokeColor),
            Potency.CODEC.optionalFieldOf("potency", Potency.DEFAULT).forGetter(Strain::potency),
            Codec.INT.optionalFieldOf("jar_colour").forGetter(Strain::jarColour),
            // The sixteenth field, the most one RecordCodecBuilder group takes. What a plant rolls at planting
            // lives in the file's own "plant" object, and a field added later goes inside one of the objects.
            Plant.FIELD.forGetter(Strain::plant)
    ).apply(instance, Strain::new));

    /**
     * How a <em>reference</em> to a strain is written — in the {@code smoke_contents} component, and
     * anywhere else that points at one. {@link RegistryFixedCodec} is what vanilla uses for the same
     * job in {@code JukeboxPlayable}, and it refuses inline entries, so a stack always
     * carries an id rather than a copy of the definition.
     */
    public static final Codec<Holder<Strain>> ENTRY_CODEC = RegistryFixedCodec.create(REGISTRY_KEY);

    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Strain>> ENTRY_PACKET_CODEC =
            ByteBufCodecs.holderRegistry(REGISTRY_KEY);

    /** The strain registry as loaded for this world. */
    public static HolderLookup.RegistryLookup<Strain> registry(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(REGISTRY_KEY);
    }

    /**
     * The strain {@code block} is a plant of: the one whose seeds plant it, or whose wild flower it is.
     * Empty for a plant no strain names (Industrial Hemp is fibre, not a smoking strain).
     *
     * <p>When a datapack strain names the same seeds or flower as one of the mod's, the plant stays the
     * mod's own, then the lowest id, as {@link #fromBuds} rules: a datapack's {@code aaa:kush} reusing
     * Purple Kush's seeds must not hand every Purple Kush plant its odds, while the plant's loot still
     * stamps by its own table.
     */
    public static Optional<Holder.Reference<Strain>> ofPlant(Block block, HolderLookup.Provider registries) {
        return registry(registries).listElements().filter(strain -> {
            Strain value = strain.value();
            return value.seedItem().filter(seed -> seed instanceof BlockItem blockItem
                    && blockItem.getBlock() == block).isPresent()
                    || value.wildFlower().filter(flower -> flower == block).isPresent();
        }).min(Comparator.comparing((Holder.Reference<Strain> entry) -> !entry.key().identifier().getNamespace().equals(Hempdustry.MOD_ID))
                .thenComparing(entry -> entry.key().identifier()));
    }

    /**
     * Every loaded strain, in a <b>deterministic</b> order: by {@link #modelIndex} first, then by id.
     * Registry iteration order is not guaranteed, and this list drives the creative tab and the seed
     * loot pools, so an unstable order would shuffle the tab between launches.
     */
    public static List<Holder.Reference<Strain>> all(HolderLookup.Provider registries) {
        return sorted(registry(registries));
    }

    /**
     * {@link #all}, from the registries a loot table is loaded with. Since 26.3 a loader may hand those
     * over as bare getters (NeoForge's loot table event does): the strain registry, loaded before any
     * loot table, is a full lookup underneath, and a getter that cannot list is refused rather than
     * read as no strains.
     */
    public static List<Holder.Reference<Strain>> all(HolderGetter.Provider registries) {
        if (registries instanceof HolderLookup.Provider lookup) {
            return all(lookup);
        }
        if (registries.lookupOrThrow(REGISTRY_KEY) instanceof HolderLookup<Strain> strains) {
            return sorted(strains);
        }
        throw new IllegalStateException("the strain registry cannot be listed from " + registries);
    }

    private static List<Holder.Reference<Strain>> sorted(HolderLookup<Strain> strains) {
        List<Holder.Reference<Strain>> out = new ArrayList<>(strains.listElements().toList());
        out.sort(Comparator.comparingInt((Holder.Reference<Strain> entry) -> entry.value().modelIndex())
                .thenComparing(entry -> entry.key().identifier()));
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
     * Ordering matters where the result is <em>shown</em> (the creative tab, the seed loot pools).
     *
     * <p>It matters here only when two strains name the same buds, which a datapack may do, and then
     * a dynamic registry's order is no answer: the same buds could pack as either strain from one
     * world load to the next. The mod's own strain wins, then the lowest id — one pass with
     * {@code min}, still no copy and no sort.
     */
    public static Optional<Holder.Reference<Strain>> fromBuds(HolderLookup.Provider registries, Item item) {
        return registry(registries).listElements()
                .filter(entry -> entry.value().buds() == item)
                .min(Comparator.comparing((Holder.Reference<Strain> entry) -> !entry.key().identifier().getNamespace().equals(Hempdustry.MOD_ID))
                        .thenComparing(entry -> entry.key().identifier()));
    }

    /**
     * Fresh status-effect instances for one hit of this strain at {@code dose}: the ones that land
     * on the hit ({@code onExhale} false) or the ones held back to the exhale ({@code onExhale} true).
     * Each lasts {@code durationTicks} times its own {@link SmokeEffect#durationFactor}, and lands
     * only if its {@link SmokeEffect#chance} comes up on {@code random}. A strain with no effects —
     * including one a datapack has emptied deliberately — simply applies nothing.
     *
     * <p>A strain the world no longer defines never reaches this method at all: the reference is
     * dropped while the {@code smoke_contents} component decodes, leaving the device unpacked. See
     * {@code SmokeContents.ENTRIES_CODEC} for why that has to happen there rather than here.
     */
    public List<MobEffectInstance> effects(int dose, int durationTicks, boolean onExhale, RandomGenerator random) {
        return effects(dose, durationTicks, onExhale, 0, random);
    }

    /**
     * 2.0's question: everything one hit at {@code dose} applies, the hit's effects and the exhale's
     * together, and every effect with a {@link SmokeEffect#chance} taken as landing (a chance of
     * zero still never does). 2.0 had neither timing nor chance, so this is the whole list it meant.
     *
     * @deprecated kept so an addon built against 2.0 still links; use
     *             {@link #effects(int, int, boolean, RandomGenerator)}
     */
    @Deprecated
    public List<MobEffectInstance> effects(int dose, int durationTicks) {
        // A generator whose every draw is zero: nextFloat() is 0.0, under any chance above zero.
        RandomGenerator lands = () -> 0L;
        List<MobEffectInstance> out = new ArrayList<>(effects(dose, durationTicks, false, lands));
        out.addAll(effects(dose, durationTicks, true, lands));
        return out;
    }

    /**
     * As above, with every scaling <em>buff</em> raised by {@code buffBonus} levels on top of the
     * dose: how a bud loaded with a concentrate reaches past the cap. Costs are not raised, because
     * the concentrate is potency, not a bigger dose of the plant.
     */
    public List<MobEffectInstance> effects(int dose, int durationTicks, boolean onExhale, int buffBonus,
                                              RandomGenerator random) {
        // The strain's own chemotype: a balanced plant's bong of three is a THC dose of one however
        // it is asked. A strainless entry (hash) is THC-dominant unless its file says otherwise.
        return effects(dose, chemotype, durationTicks, onExhale, buffBonus, random);
    }

    /**
     * As above, for {@code units} of material of {@code chemotype}: each effect reads what its
     * {@link EffectLabel} says moves it. A {@link EffectLabel#THC THC} effect scales with the THC dose
     * ({@link Chemotype#thcDose}) and is not there at all without THC; a {@link EffectLabel#SMOKE
     * SMOKE} effect scales with the units burned; a {@link EffectLabel#STRAIN STRAIN} effect is flat;
     * a {@link EffectLabel#CBD CBD} effect is flat, lasts by the CBD smoked
     * ({@code SmokeContents#cbdTicks}) and is not there without CBD. For THC-dominant material the THC dose is the unit count, so every effect
     * comes out exactly as it did before chemotypes existed.
     */
    public List<MobEffectInstance> effects(int units, Chemotype chemotype, int durationTicks, boolean onExhale,
                                              int buffBonus, RandomGenerator random) {
        return effects(units, chemotype.thcDose(units), chemotype, durationTicks, onExhale, buffBonus, random);
    }

    /**
     * As above, with the THC dose given rather than read off the units: a bowl weighs a bud's THC by
     * its potency band as well as its chemotype (see {@code SmokeContents.Entry#thcDose}), so a pipe of
     * two extra-high buds is a dose of three. Every label lasts {@code durationTicks} but CBD's, which
     * takes the smoked-CBD rule at the units' worth of CBD.
     */
    public List<MobEffectInstance> effects(int units, int thcDose, Chemotype chemotype, int durationTicks,
                                              boolean onExhale, int buffBonus, RandomGenerator random) {
        return effects(new Burn(units, thcDose, chemotype, durationTicks, durationTicks,
                        SmokeContents.cbdTicks(units * (double) chemotype.cbdHalves() / Chemotype.FULL_WEIGHT)),
                onExhale, buffBonus, random);
    }

    /**
     * One entry of a bowl as its effects read it: how many units burn, the THC dose that sets a THC
     * effect's level, the chemotype, and how long each kind of effect lasts — {@code thcTicks} for a
     * THC-labelled one (the exact dose buys time, {@code SmokeContents#thcTicks}), {@code ticks} for a
     * strain- or smoke-labelled one (a bud past the buff cap buys time), {@code cbdTicks} for a
     * CBD-labelled one ({@code SmokeContents#cbdTicks}).
     */
    public record Burn(int units, int thcDose, Chemotype chemotype, int thcTicks, int ticks, int cbdTicks) {
    }

    /** Fresh status-effect instances for one hit of {@code burn}, each label at its own level and length. */
    public List<MobEffectInstance> effects(Burn burn, boolean onExhale, int buffBonus, RandomGenerator random) {
        int units = burn.units();
        int thcDose = burn.thcDose();
        Chemotype chemotype = burn.chemotype();
        List<MobEffectInstance> out = new ArrayList<>(smokeEffects.size());
        for (SmokeEffect effect : smokeEffects) {
            if (effect.onExhale() != onExhale) {
                continue;
            }
            EffectLabel label = effect.label();
            // Before the chance draw: an effect the chemotype rules out never takes a random number,
            // so a THC-dominant load draws exactly what it always did.
            // Cannabinoid-free material has none of the strain's own effects either, a hash's or a
            // scorched bud's: only what any smoke does (schwag's Poison) is left.
            if ((label == EffectLabel.THC && thcDose == 0) || (label == EffectLabel.CBD && chemotype.cbdHalves() == 0)
                    || (label == EffectLabel.STRAIN && chemotype == Chemotype.CANNABINOID_FREE)) {
                continue;
            }
            // A certain effect draws nothing, so every strain written before chance existed takes
            // exactly the random numbers it always did.
            if (effect.chance() < 1.0F && random.nextFloat() >= effect.chance()) {
                continue;
            }
            int level = switch (label) {
                case THC -> thcDose;
                case SMOKE -> units;
                case STRAIN, CBD -> 1;
            };
            int amplifier = effect.baseAmplifier() + Math.max(0, level - 1);
            if (label.scales() && effect.effect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                amplifier += buffBonus;
            }
            amplifier = Math.min(effect.maxAmplifier(), amplifier);
            int ticks = switch (label) {
                case THC -> burn.thcTicks();
                case CBD -> burn.cbdTicks();
                case STRAIN, SMOKE -> burn.ticks();
            };
            // At least a tick: a datapack's 0 or negative factor is a very short effect, never one
            // that silently does not exist.
            int duration = Math.max(1, Math.round(ticks * effect.durationFactor()));
            out.add(new MobEffectInstance(effect.effect(), duration, amplifier));
        }
        return out;
    }

    /**
     * One effect a strain grants when smoked.
     *
     * <p>Everything but the effect is optional in JSON so a datapack entry can be as short as
     * {@code {"effect": "minecraft:speed"}} — level I, dose-scaling, lasting the device's whole
     * duration from the hit, which is what almost every shipped effect is.
     *
     * @param baseAmplifier  amplifier at dose 1 (0 = level I)
     * @param scales         whether dose raises it; {@code false} pins it at {@code baseAmplifier}
     * @param durationFactor this effect's share of the device's duration. {@code 1.0} for nearly
     *                       everything; Beldía's Blindness is {@code 0.25}, because a downside that
     *                       lasts as long as Invisibility cancels it rather than pricing it
     * @param onExhale       whether it lands with the exhale puff ({@code Smoking}'s exhale delay)
     *                       rather than on the hit. Streaming eyes come when the smoke comes out
     * @param chance         the odds it lands at all on a given hit, rolled per hit. {@code 1.0} for
     *                       nearly everything; schwag's Poison is {@code 0.6}, the poisonous potato's
     *                       own odds. Vanilla's {@code apply_effects} consume effect calls the same
     *                       number {@code probability}
     * @param maxAmplifier   the highest amplifier dose may scale it to. Unbounded in practice (255,
     *                       vanilla's own ceiling) for nearly everything; schwag's Poison stops at
     *                       {@code 1}, level II, because survival vanilla never goes past Poison II
     *                       (the strong potion, the pufferfish) and level III takes a full-health
     *                       player to half a heart in seven seconds
     * @param declaredLabel  what the effect comes from, and so what makes it stronger (see
     *                       {@link EffectLabel}); when a file names none, {@link #label} reads it off
     *                       {@code scales}
     */
    public record SmokeEffect(Holder<MobEffect> effect, int baseAmplifier, boolean scales,
                              float durationFactor, boolean onExhale, float chance, int maxAmplifier,
                              Optional<EffectLabel> declaredLabel) {
        /** Vanilla's highest amplifier, and the default: no cap a dose could reach. */
        public static final int NO_CAP = 255;

        public static final Codec<SmokeEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                MobEffect.CODEC.fieldOf("effect").forGetter(SmokeEffect::effect),
                Codec.INT.optionalFieldOf("base_amplifier", 0).forGetter(SmokeEffect::baseAmplifier),
                Codec.BOOL.optionalFieldOf("scales", true).forGetter(SmokeEffect::scales),
                Codec.FLOAT.optionalFieldOf("duration_factor", 1.0F).forGetter(SmokeEffect::durationFactor),
                Codec.BOOL.optionalFieldOf("on_exhale", false).forGetter(SmokeEffect::onExhale),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(SmokeEffect::chance),
                Codec.intRange(0, NO_CAP).optionalFieldOf("max_amplifier", NO_CAP).forGetter(SmokeEffect::maxAmplifier),
                EffectLabel.CODEC.optionalFieldOf("label").forGetter(SmokeEffect::declaredLabel)
        ).apply(instance, SmokeEffect::new));

        /** Every effect written before {@code label} existed: the label its {@code scales} stood for. */
        public SmokeEffect(Holder<MobEffect> effect, int baseAmplifier, boolean scales,
                           float durationFactor, boolean onExhale, float chance, int maxAmplifier) {
            this(effect, baseAmplifier, scales, durationFactor, onExhale, chance, maxAmplifier, Optional.empty());
        }

        /** An effect with its label named, its {@code scales} flag kept in step for older readers. */
        public SmokeEffect(Holder<MobEffect> effect, int baseAmplifier, EffectLabel label,
                           float durationFactor, boolean onExhale, float chance, int maxAmplifier) {
            this(effect, baseAmplifier, label.scales(), durationFactor, onExhale, chance, maxAmplifier,
                    Optional.of(label));
        }

        /** What the effect comes from: the label the file names, or the one {@code scales} stood for. */
        public EffectLabel label() {
            return declaredLabel.orElseGet(() -> EffectLabel.fromScales(scales));
        }

        /** The whole duration, from the hit — every effect written before the last four fields existed. */
        public SmokeEffect(Holder<MobEffect> effect, int baseAmplifier, boolean scales) {
            this(effect, baseAmplifier, scales, 1.0F, false);
        }

        /** Certain to land and uncapped — every effect written before {@code chance} existed. */
        public SmokeEffect(Holder<MobEffect> effect, int baseAmplifier, boolean scales,
                           float durationFactor, boolean onExhale) {
            this(effect, baseAmplifier, scales, durationFactor, onExhale, 1.0F, NO_CAP);
        }
    }
}

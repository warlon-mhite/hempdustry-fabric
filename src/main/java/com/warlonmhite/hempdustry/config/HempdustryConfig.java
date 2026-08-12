package com.warlonmhite.hempdustry.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The server's balance knobs — {@code config/hempdustry.json}.
 *
 * <h2>What is here, and what deliberately is not</h2>
 *
 * This file is for <b>numbers and switches</b>. Three kinds of thing are kept out of it on purpose
 * (see {@code roadmap.md} D14 for the full argument):
 *
 * <ul>
 *   <li><b>Anything a datapack already does.</b> Recipes, tags, advancements, worldgen placement and
 *       loot <em>tables</em> are datapack territory, and a knob here would fight them. <b>Strain
 *       effects are a datapack registry</b> — {@code data/<ns>/strain/<id>.json} — not a config
 *       section.</li>
 *   <li><b>Anything that would contradict the mod's own premise.</b> There is no switch that gives a
 *       raw bud an effect: <em>heat activates, raw plant does nothing</em>. Nor one that lets dose 1
 *       green you out, or shrinks duration as the amplifier rises.</li>
 *   <li><b>Strain display names.</b> They resolve client-side from a lang key; a server sending a
 *       literal string would override all eight locales with one language. A server resource pack is
 *       the vanilla answer, and it works today.</li>
 * </ul>
 *
 * <h2>Server-side only, and that is not an accident</h2>
 *
 * Nothing here has to reach a client. Effects, cooldowns, growth and loot are all applied
 * server-side, and <b>the Infuser's timings ride to the screen on its existing
 * {@code PropertyDelegate}</b> rather than needing a config packet — the GUI reads the values the
 * block is actually using, so a client with a different file (or no file) still draws a truthful bar.
 * There is no sync code in this mod, and this file is why there does not need to be.
 *
 * <h2>Reading it</h2>
 *
 * Missing fields fall back to the default rather than failing, so a half-written file still loads and
 * an upgrade that adds a knob needs no migration. Values outside their sane range are <b>clamped and
 * logged</b>, not rejected — a server should never fail to boot over a config typo. A file that is not
 * valid JSON at all logs the parse error and runs on defaults.
 *
 * <p>{@code /hempdustry reload} re-reads it in place.
 */
public record HempdustryConfig(Effects effects, World world, Infuser infuser, Loot loot) {

    public static final HempdustryConfig DEFAULT = new HempdustryConfig(
            Effects.DEFAULT, World.DEFAULT, Infuser.DEFAULT, Loot.DEFAULT);

    public static final Codec<HempdustryConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Effects.CODEC.fieldOf("effects").forGetter(HempdustryConfig::effects),
            World.CODEC.fieldOf("world").forGetter(HempdustryConfig::world),
            Infuser.CODEC.fieldOf("infuser").forGetter(HempdustryConfig::infuser),
            Loot.CODEC.fieldOf("loot").forGetter(HempdustryConfig::loot)
    ).apply(instance, HempdustryConfig::new));

    private static volatile HempdustryConfig instance = DEFAULT;

    /**
     * The loaded config. Never {@code null}: before {@link #load()} runs, and after a failed load,
     * this is {@link #DEFAULT}, so every caller can read it without a guard.
     */
    public static HempdustryConfig get() {
        return instance;
    }

    /**
     * Reads the file, writing a complete, commented one back out afterwards. Safe to call again —
     * that is what {@code /hempdustry reload} does.
     *
     * <h2>Why the codec's fields are required, and missing ones still work</h2>
     *
     * The obvious shape — {@code optionalFieldOf(name, default)} everywhere — has a trap:
     * <b>DFU omits any field whose value equals its default when encoding</b>, so a freshly generated
     * config writes out as {@code &#123;&#125;} and an admin never sees a knob they could turn. The
     * fields are therefore required, and tolerance is provided at the other end: whatever the file
     * contains is <b>merged over the defaults</b> before decoding, so a missing section, a missing
     * field, or a config written by an older version all still load. Comments and any other unknown
     * keys are ignored by the decoder and simply rewritten.
     */
    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(Hempdustry.MOD_ID + ".json");
        HempdustryConfig loaded = DEFAULT;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement json = new GsonBuilder().create().fromJson(reader, JsonElement.class);
                JsonObject merged = merge(defaultJson(), json);
                loaded = CODEC.parse(JsonOps.INSTANCE, merged).resultOrPartial(error ->
                        Hempdustry.LOGGER.error("Bad value in {}: {} — running on defaults", path, error)
                ).orElse(DEFAULT);
            } catch (IOException | RuntimeException e) {
                Hempdustry.LOGGER.error("Could not read {} — running on defaults", path, e);
                loaded = DEFAULT;
            }
        }
        instance = loaded.clamped();
        write(path, instance);
    }

    /** The defaults as JSON, which is the base every file is read on top of. */
    private static JsonObject defaultJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, DEFAULT).getOrThrow().getAsJsonObject();
    }

    /**
     * {@code overrides} laid over {@code base}, one level into each section. Anything the file does
     * not mention keeps its default, which is what lets a knob be added in a later version without a
     * migration step.
     */
    private static JsonObject merge(JsonObject base, JsonElement overrides) {
        if (!(overrides instanceof JsonObject object)) {
            return base;
        }
        JsonObject out = base.deepCopy();
        for (var entry : object.entrySet()) {
            JsonElement existing = out.get(entry.getKey());
            if (existing instanceof JsonObject nested && entry.getValue() instanceof JsonObject override) {
                out.add(entry.getKey(), merge(nested, override));
            } else {
                out.add(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    /** Every value pulled into its sane range, complaining about any it had to move. */
    private HempdustryConfig clamped() {
        return new HempdustryConfig(effects.clamped(), world.clamped(), infuser.clamped(), loot.clamped());
    }

    /**
     * Writes the config back out, with a {@code _comment} line in each section.
     *
     * <p>Rewriting on every load is what keeps the file complete: a knob added in a later version
     * appears in the file at its default rather than staying invisible until someone reads the source.
     * Unknown fields are ignored by the codec, so the comments survive the round trip.
     */
    private static void write(Path path, HempdustryConfig config) {
        try {
            JsonElement encoded = CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow();
            JsonObject root = encoded.getAsJsonObject();
            comment(root, "effects", "enabled=false is 'industrial hemp only': no drug effects anywhere. "
                    + "maxLevel caps every effect the mod applies. Multipliers are 0.05-10; "
                    + "greenOutChanceMultiplier 0 disables green-outs, as does greenOut=false.");
            comment(root, "world", "cropGrowthMultiplier and machineSpeedMultiplier are speeds: 2.0 is twice as fast. "
                    + "machineSpeed drives the Decarboxylator; the Infuser has its own section. "
                    + "Whether bees pollinate hemp is the #minecraft:bee_growables tag, not a setting here.");
            comment(root, "infuser", "Ticks. minTime is the earliest a batch can be pulled, fullTime a full simmer. "
                    + "20 ticks = 1 second. Defaults are 5 and 15 minutes.");
            comment(root, "loot", "The mod's additions to vanilla loot tables: hemp seeds in grass and chests, "
                    + "music discs, shipwreck fibre. enabled=false removes all of them.");
            Files.createDirectories(path.getParent());
            // disableHtmlEscaping, or every "=" and "'" in the comments is written as \u003d and
            // \u0027 — valid JSON, unreadable prose.
            Files.writeString(path,
                    new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root) + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            Hempdustry.LOGGER.error("Could not write {}", path, e);
        }
    }

    /**
     * Puts a section's comment first and its keys in alphabetical order.
     *
     * <p>Sorting matters more than it looks: a codec encodes into an unordered map, so without this
     * the knobs come out shuffled — and differently between runs — which makes a diff of two servers'
     * configs unreadable. Alphabetical also matches what the datagen'd JSON in {@code src/main/generated}
     * does, so the whole project's JSON reads the same way. {@code _comment} leads because {@code _}
     * sorts before every lower-case letter.
     */
    private static void comment(JsonObject root, String section, String text) {
        if (root.get(section) instanceof JsonObject object) {
            JsonObject commented = new JsonObject();
            commented.addProperty("_comment", text);
            object.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> commented.add(entry.getKey(), entry.getValue()));
            root.add(section, commented);
        }
    }

    private static double clampDouble(String name, double value, double min, double max) {
        double clamped = MathHelper.clamp(value, min, max);
        if (clamped != value) {
            Hempdustry.LOGGER.warn("Config {} was {}, clamped to {}", name, value, clamped);
        }
        return clamped;
    }

    private static int clampInt(String name, int value, int min, int max) {
        int clamped = MathHelper.clamp(value, min, max);
        if (clamped != value) {
            Hempdustry.LOGGER.warn("Config {} was {}, clamped to {}", name, value, clamped);
        }
        return clamped;
    }

    /**
     * What smoking and edibles do to a player.
     *
     * @param enabled                  master switch; {@code false} is "industrial hemp only"
     * @param maxLevel                 highest level any mod effect may reach — the PvP knob
     * @param durationMultiplier       scales every effect's duration, smoking and edibles alike
     * @param cooldownMultiplier       scales the per-device use cooldowns
     * @param greenOutChanceMultiplier scales green-out odds; {@code 0} disables them
     * @param onsetMultiplier          scales an edible's 30 s–3 min come-up window
     * @param greenOut                 whether a bad hit can replace its effects entirely
     * @param nausea                   whether nausea is ever applied — an accessibility switch
     * @param munchies                 whether Hunger is applied
     * @param debuffs                  whether harmful effects are applied at all
     */
    public record Effects(boolean enabled, int maxLevel, double durationMultiplier,
                          double cooldownMultiplier, double greenOutChanceMultiplier,
                          double onsetMultiplier, boolean greenOut, boolean nausea,
                          boolean munchies, boolean debuffs) {

        public static final Effects DEFAULT =
                new Effects(true, 4, 1.0, 1.0, 1.0, 1.0, true, true, true, true);

        public static final Codec<Effects> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("enabled").forGetter(Effects::enabled),
                Codec.INT.fieldOf("maxLevel").forGetter(Effects::maxLevel),
                Codec.DOUBLE.fieldOf("durationMultiplier").forGetter(Effects::durationMultiplier),
                Codec.DOUBLE.fieldOf("cooldownMultiplier").forGetter(Effects::cooldownMultiplier),
                Codec.DOUBLE.fieldOf("greenOutChanceMultiplier").forGetter(Effects::greenOutChanceMultiplier),
                Codec.DOUBLE.fieldOf("onsetMultiplier").forGetter(Effects::onsetMultiplier),
                Codec.BOOL.fieldOf("greenOut").forGetter(Effects::greenOut),
                Codec.BOOL.fieldOf("nausea").forGetter(Effects::nausea),
                Codec.BOOL.fieldOf("munchies").forGetter(Effects::munchies),
                Codec.BOOL.fieldOf("debuffs").forGetter(Effects::debuffs)
        ).apply(instance, Effects::new));

        Effects clamped() {
            return new Effects(enabled,
                    clampInt("effects.maxLevel", maxLevel, 1, 10),
                    clampDouble("effects.durationMultiplier", durationMultiplier, 0.05, 10.0),
                    clampDouble("effects.cooldownMultiplier", cooldownMultiplier, 0.0, 10.0),
                    clampDouble("effects.greenOutChanceMultiplier", greenOutChanceMultiplier, 0.0, 100.0),
                    clampDouble("effects.onsetMultiplier", onsetMultiplier, 0.05, 10.0),
                    greenOut, nausea, munchies, debuffs);
        }
    }

    /**
     * <p><b>Bee pollination is deliberately not here.</b> Whether bees advance hemp is decided by
     * membership of {@code #minecraft:bee_growables} — a tag, which a datapack already controls
     * completely. A config switch for it would be the mod fighting the datapack, which is the first
     * of the three filters at the top of this file.
     *
     * @param cropGrowthMultiplier     growth speed for both crops; 2.0 grows twice as fast
     * @param machineSpeedMultiplier   the Decarboxylator's speed (the Infuser has its own section)
     */
    public record World(double cropGrowthMultiplier, double machineSpeedMultiplier) {

        public static final World DEFAULT = new World(1.0, 1.0);

        public static final Codec<World> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("cropGrowthMultiplier").forGetter(World::cropGrowthMultiplier),
                Codec.DOUBLE.fieldOf("machineSpeedMultiplier").forGetter(World::machineSpeedMultiplier)
        ).apply(instance, World::new));

        World clamped() {
            return new World(
                    clampDouble("world.cropGrowthMultiplier", cropGrowthMultiplier, 0.05, 20.0),
                    clampDouble("world.machineSpeedMultiplier", machineSpeedMultiplier, 0.05, 20.0));
        }
    }

    /**
     * The simmer's two clocks, in ticks.
     *
     * <p>They are absolute rather than a multiplier because the Quality maths reads both, and because
     * {@code fullTime} must stay above {@code minTime} — a relationship that is easier to state and to
     * validate on the numbers themselves. <b>The screen reads these off the block's property
     * delegate</b>, so the bar and its marks stay honest whatever they are set to.
     */
    public record Infuser(int minTimeTicks, int fullTimeTicks) {

        /**
         * Seeded from the machine's own constants rather than repeating the numbers, so the default
         * and the documented behaviour cannot drift apart. Both are compile-time {@code static final
         * int}s, so this inlines and loads no class — which matters, because this record is built
         * while {@link #load()} runs and that is the first thing {@code onInitialize} does.
         */
        public static final Infuser DEFAULT =
                new Infuser(InfuserBlockEntity.MIN_TIME, InfuserBlockEntity.FULL_TIME);

        public static final Codec<Infuser> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("minTimeTicks").forGetter(Infuser::minTimeTicks),
                Codec.INT.fieldOf("fullTimeTicks").forGetter(Infuser::fullTimeTicks)
        ).apply(instance, Infuser::new));

        Infuser clamped() {
            int min = clampInt("infuser.minTimeTicks", minTimeTicks, 20, 1_728_000);
            int full = clampInt("infuser.fullTimeTicks", fullTimeTicks, min + 20, 1_728_000);
            return new Infuser(min, full);
        }
    }

    /**
     * @param enabled          whether the mod adds anything to vanilla loot tables at all
     * @param chanceMultiplier scales every injected chance; the results are still clamped to 0–1
     */
    public record Loot(boolean enabled, double chanceMultiplier) {

        public static final Loot DEFAULT = new Loot(true, 1.0);

        public static final Codec<Loot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("enabled").forGetter(Loot::enabled),
                Codec.DOUBLE.fieldOf("chanceMultiplier").forGetter(Loot::chanceMultiplier)
        ).apply(instance, Loot::new));

        Loot clamped() {
            return new Loot(enabled, clampDouble("loot.chanceMultiplier", chanceMultiplier, 0.0, 10.0));
        }
    }
}

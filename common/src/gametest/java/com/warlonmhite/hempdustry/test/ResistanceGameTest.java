package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndustrialHempCropBlock;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.GrowQuality;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Resistance;
import com.warlonmhite.hempdustry.strain.ResistanceGenes;
import com.warlonmhite.hempdustry.strain.ResistanceOdds;
import com.warlonmhite.hempdustry.strain.Resistances;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.strain.StressPhenomenon;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Resistances: the five steps and what each does to a cause, the shared spread that turns a strain's most likely
 * step into odds, how a strain file writes them, every plant's agreed profile, and the record that rolls them at
 * planting. What a step does to each cause in play is held beside the causes ({@code PlantStressGameTest},
 * {@code MildewGameTest}).
 */
public final class ResistanceGameTest {
    private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos ROOT = FLOOR.above();

    /** Dry ground, overwatering, light, mildew: the agreed profiles (cannabinoids.md §5, #189). */
    private static final Map<ResourceKey<Strain>, Resistance[]> PROFILES = new LinkedHashMap<>();

    static {
        Resistance n = Resistance.NEUTRAL;
        Resistance r = Resistance.RESISTANT;
        Resistance vr = Resistance.VERY_RESISTANT;
        Resistance w = Resistance.WEAK;
        Resistance vw = Resistance.VERY_WEAK;
        PROFILES.put(ModStrains.INDICA, new Resistance[]{n, r, n, w});
        PROFILES.put(ModStrains.SATIVA, new Resistance[]{r, w, n, r});
        PROFILES.put(ModStrains.BELDIA, new Resistance[]{vr, w, n, r});
        PROFILES.put(ModStrains.WARPED_KUSH, new Resistance[]{r, n, vr, n});
        PROFILES.put(ModStrains.CRIMSON, new Resistance[]{r, n, vr, w});
        PROFILES.put(ModStrains.ENDER_HAZE, new Resistance[]{n, w, r, r});
        PROFILES.put(ModStrains.RUDERALIS, new Resistance[]{r, n, vr, n});
        PROFILES.put(ModStrains.CHERRY_WINE, new Resistance[]{n, r, n, w});
        PROFILES.put(ModStrains.WHITE_WIDOW, new Resistance[]{n, n, vw, n});
        PROFILES.put(ModStrains.DUD_WEED, new Resistance[]{vr, vr, r, vr});
    }

    private ResistanceGameTest() {
    }

    /**
     * The steps' factors and light rules, the five rows of the shared spread as agreed (with #190's end-step rule),
     * a resistant plant's light roll taking one draw and every other plant's none, the rolls landing on the odds,
     * and the strain file's two ways of writing a phenomenon.
     */
    public static void theResistanceSpreadIsTheAgreedTable(GameTestHelper context) {
        float[] factors = {2.0F, 1.5F, 1.0F, 0.5F, 0.25F};
        int[] lossAges = {3, 3, 4, 4, 4};
        for (Resistance step : Resistance.values()) {
            context.assertValueEqual(step.factor(), factors[step.ordinal()], step + "'s factor");
            context.assertValueEqual(step.lightLossAge(), lossAges[step.ordinal()], step + "'s light loss age");
            context.assertValueEqual(step.score(), step.ordinal() - 2, step + "'s place on the hardiness scale");
        }
        context.assertValueEqual(Resistance.VERY_WEAK.apply(1.0F / 3), 2.0F / 3, "a very weak plant in a dry bed");
        context.assertValueEqual(Resistance.VERY_RESISTANT.apply(1.0F / 3), 1.0F / 12, "a very resistant plant in a dry bed");
        context.assertValueEqual(Resistance.VERY_WEAK.apply(0.75F), 1.0F, "a doubled chance past certain");

        int[][] rows = {{60, 25, 15, 0, 0}, {19, 62, 12, 7, 0}, {7, 12, 62, 12, 7}, {0, 7, 12, 62, 19}, {0, 0, 15, 25, 60}};
        for (Resistance step : Resistance.values()) {
            int[] row = rows[step.ordinal()];
            ResistanceOdds expected = new ResistanceOdds(row[0], row[1], row[2], row[3], row[4]);
            context.assertValueEqual(ResistanceOdds.spread(step), expected, "the spread around " + step);
            context.assertValueEqual(ResistanceOdds.spread(step).mostLikelyIfSpread().orElse(null), step,
                    "the spread around " + step + " read back");
        }
        context.assertTrue(ResistanceOdds.ALWAYS_NEUTRAL.mostLikelyIfSpread().isEmpty(),
                "an always-neutral phenomenon reads as the spread around neutral, so it would write as one");

        // A lost light: a resistant plant ignores it 1 time in 2, a very resistant one 3 in 4; one draw for those, none for the rest.
        context.assertTrue(Resistance.RESISTANT.ignoresLostLight(decided(0.49F))
                        && !Resistance.RESISTANT.ignoresLostLight(decided(0.51F))
                        && Resistance.VERY_RESISTANT.ignoresLostLight(decided(0.74F))
                        && !Resistance.VERY_RESISTANT.ignoresLostLight(decided(0.76F))
                        && !Resistance.NEUTRAL.ignoresLostLight(decided(0.0F))
                        && !Resistance.WEAK.ignoresLostLight(decided(0.0F)),
                "a lost light is ignored at other odds than resistant 1 in 2, very resistant 3 in 4, nobody else");
        for (Resistance step : Resistance.values()) {
            long seed = new Random(20261113L + step.ordinal()).nextLong();
            RandomSource rolled = RandomSource.create(seed);
            RandomSource counted = RandomSource.create(seed);
            step.ignoresLostLight(rolled);
            if (step.compareTo(Resistance.RESISTANT) >= 0) {
                counted.nextFloat();
            }
            context.assertValueEqual(rolled.nextLong(), counted.nextLong(),
                    "the draw after " + step + "'s light roll: it took other than "
                            + (step.compareTo(Resistance.RESISTANT) >= 0 ? "one draw" : "no draw"));
        }

        // Rolls land on their odds.
        for (Resistance centre : Resistance.values()) {
            ResistanceOdds odds = ResistanceOdds.spread(centre);
            RandomSource random = RandomSource.create(new Random(20261113L * 31 + centre.ordinal()).nextLong());
            int rolls = 8000;
            Map<Resistance, Integer> seen = new EnumMap<>(Resistance.class);
            for (int roll = 0; roll < rolls; roll++) {
                seen.merge(odds.roll(random), 1, Integer::sum);
            }
            for (Resistance step : Resistance.values()) {
                double expected = odds.weight(step) / 100.0;
                double share = (double) seen.getOrDefault(step, 0) / rolls;
                context.assertTrue(Math.abs(share - expected) < 0.02, "odds " + odds + " rolled " + step + " "
                        + share + " of the time, expected " + expected);
            }
        }
        context.assertValueEqual(new ResistanceOdds(0, 0, 0, 0, 0).roll(decided(0.0F)), Resistance.NEUTRAL,
                "odds that are all zero");

        // The file: a step, or five weights; a step's spread writes back as the step.
        context.assertValueEqual(parse(ResistanceOdds.CODEC, "\"resistant\""), ResistanceOdds.spread(Resistance.RESISTANT),
                "a phenomenon written as its most likely step");
        context.assertValueEqual(parse(ResistanceOdds.CODEC, "{\"weak\": 5, \"resistant\": 95}"),
                new ResistanceOdds(0, 5, 0, 95, 0), "a phenomenon written as weights");
        context.assertValueEqual(encode(ResistanceOdds.CODEC, ResistanceOdds.spread(Resistance.WEAK)).toString(),
                "\"weak\"", "a spread written back");
        context.assertTrue(encode(ResistanceOdds.CODEC, new ResistanceOdds(0, 5, 0, 95, 0)).isJsonObject(),
                "odds of their own written back as a step");

        // A strain file: no plant object is the old plant; a named phenomenon spreads, an unnamed one stays neutral.
        Strain bare = parse(Strain.CODEC, "{\"translation_key\":\"x\",\"color\":1,\"buds\":\"minecraft:wheat\"}");
        context.assertValueEqual(bare.plant(), Strain.Plant.DEFAULT, "a strain file naming no plant");
        context.assertValueEqual(bare.resistances(), Resistances.DEFAULT, "a strain file naming no resistances");
        Strain touchy = parse(Strain.CODEC, "{\"translation_key\":\"x\",\"color\":1,\"buds\":\"minecraft:wheat\","
                + "\"plant\":{\"resistances\":{\"light\":\"very_weak\"}}}");
        context.assertValueEqual(touchy.resistances(), new Resistances(ResistanceOdds.ALWAYS_NEUTRAL,
                        ResistanceOdds.ALWAYS_NEUTRAL, ResistanceOdds.spread(Resistance.VERY_WEAK), ResistanceOdds.ALWAYS_NEUTRAL),
                "a strain file naming only its light");
        context.assertValueEqual(touchy.quality(), GrowQuality.DEFAULT, "a plant object naming no quality");
        context.succeed();
    }

    /**
     * Every plant carries its agreed profile, Industrial Hemp included (its own sheet, since it is no strain), every
     * profile is written as most likely steps, and every strain smoked for its effects keeps the hardiness rule:
     * a score of −3 to +3, at most one very resistant and one very weak step.
     */
    public static void everyStrainCarriesItsResistanceProfile(GameTestHelper context) {
        HolderLookup.Provider registries = context.getLevel().registryAccess();
        for (Map.Entry<ResourceKey<Strain>, Resistance[]> profile : PROFILES.entrySet()) {
            Resistance[] steps = profile.getValue();
            Strain strain = Strain.registry(registries).getOrThrow(profile.getKey()).value();
            context.assertValueEqual(strain.resistances(), Resistances.of(steps[0], steps[1], steps[2], steps[3]),
                    profile.getKey().identifier() + "'s resistance profile");
            int score = 0;
            int veryResistant = 0;
            int veryWeak = 0;
            for (Resistance step : steps) {
                score += step.score();
                veryResistant += step == Resistance.VERY_RESISTANT ? 1 : 0;
                veryWeak += step == Resistance.VERY_WEAK ? 1 : 0;
            }
            if (strain.chemotype() != Chemotype.CANNABINOID_FREE) {
                context.assertTrue(score >= -3 && score <= 3 && veryResistant <= 1 && veryWeak <= 1,
                        profile.getKey().identifier() + " breaks the hardiness rule: score " + score + ", "
                                + veryResistant + " very resistant, " + veryWeak + " very weak");
            }
        }
        context.assertValueEqual(IndustrialHempCropBlock.RESISTANCES, Resistances.of(Resistance.RESISTANT,
                Resistance.RESISTANT, Resistance.RESISTANT, Resistance.NEUTRAL), "Industrial Hemp's resistance profile");
        context.succeed();
    }

    /**
     * A plant rolls its four resistances at planting and keeps them through a save; set back to age 0 it rolls them
     * again; a record from before resistances rolls them when it joins the world; Industrial Hemp keeps a record of
     * resistances and no band or gene.
     */
    public static void aPlantRollsItsResistancesAtPlanting(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 2), Block.UPDATE_CLIENTS);
        PlantRecordBlockEntity record = record(world, pos);
        context.assertTrue(record.rolledResistances().isPresent(), "a plant was planted with no resistances rolled");

        ResistanceGenes hardy = ResistanceGenes.all(Resistance.VERY_RESISTANT).with(StressPhenomenon.LIGHT, Resistance.WEAK);
        record.setResistances(hardy);
        CompoundTag saved = record.saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(pos, world.getBlockState(pos), saved, registries);
        context.assertTrue(loaded instanceof PlantRecordBlockEntity plant && hardy.equals(plant.rolledResistances().orElse(null)),
                "a plant's resistances saved as " + saved + " and loaded as " + loaded);

        // A record saved before resistances existed rolls them as it joins the world, and keeps its band.
        CompoundTag old = saved.copy();
        old.remove("resistances");
        PlantRecordBlockEntity before = (PlantRecordBlockEntity) BlockEntity.loadStatic(pos, world.getBlockState(pos), old, registries);
        context.assertTrue(before != null && before.rolledResistances().isEmpty(), "a record with no resistances loaded with some");
        PotencyBand band = before.rolledBand().orElseThrow();
        before.setLevel(world);
        context.assertTrue(before.rolledResistances().isPresent() && before.rolledBand().orElse(null) == band,
                "a record from before resistances, joining the world, rolled " + before.rolledResistances()
                        + " and kept band " + before.rolledBand() + " (was " + band + ")");

        // Set back to age 0, it is a new plant.
        boolean kept = true;
        for (int i = 0; i < 40 && kept; i++) {
            world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 3), Block.UPDATE_CLIENTS);
            record(world, pos).setResistances(hardy);
            world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 0), Block.UPDATE_CLIENTS);
            kept = hardy.equals(record(world, pos).rolledResistances().orElse(null));
        }
        context.assertTrue(!kept, "forty plants set back to age 0 kept their resistances: the replant rolls none");

        // Industrial Hemp: a record, its own resistances, and no band or gene.
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        world.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        world.setBlock(pos, ripe(ModBlocks.INDUSTRIAL_HEMP_CROP, 1), Block.UPDATE_CLIENTS);
        context.assertTrue(world.getBlockEntity(pos) instanceof PlantRecordBlockEntity, "Industrial Hemp keeps no record");
        PlantRecordBlockEntity fibre = record(world, pos);
        context.assertTrue(fibre.rolledResistances().isPresent(), "Industrial Hemp rolled no resistances");
        context.assertTrue(fibre.rolledBand().isEmpty() && fibre.rolledGene().isEmpty() && fibre.band() == PotencyBand.MID,
                "Industrial Hemp rolled band " + fibre.rolledBand() + " and gene " + fibre.rolledGene()
                        + "; it grows no bud, so it rolls neither and reads mid");
        int resistant = 0;
        for (int i = 0; i < 200; i++) {
            fibre.replant();
            resistant += fibre.resistances().dryGround() == Resistance.RESISTANT ? 1 : 0;
        }
        context.assertTrue(resistant > 90 && resistant < 160, "Industrial Hemp rolled resistant to dry ground "
                + resistant + " times in 200, against its sheet's 62 in 100");
        context.assertTrue(fibre.rolledBand().isEmpty() && fibre.rolledGene().isEmpty(),
                "a replanted Industrial Hemp rolled a band or a gene");
        context.succeed();
    }

    private static <T> T parse(com.mojang.serialization.Codec<T> codec, String json) {
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static <T> JsonElement encode(com.mojang.serialization.Codec<T> codec, T value) {
        return codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
    }

    private static net.minecraft.world.level.block.state.BlockState ripe(Block crop, int age) {
        return ((CropBlock) crop).getStateForAge(age);
    }

    private static PlantRecordBlockEntity record(ServerLevel world, BlockPos pos) {
        return (PlantRecordBlockEntity) world.getBlockEntity(pos);
    }

    /** A random whose every float is {@code value}. */
    static RandomSource decided(float value) {
        return new net.minecraft.world.level.levelgen.SingleThreadedRandomSource(0L) {
            @Override
            public float nextFloat() {
                return value;
            }
        };
    }
}

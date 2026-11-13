package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.IndustrialHempCropBlock;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.component.StashContents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.ResinSheet;
import com.warlonmhite.hempdustry.strain.ResinStep;
import com.warlonmhite.hempdustry.strain.ResinTexture;
import com.warlonmhite.hempdustry.strain.ResistanceGenes;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Resin: the five steps and the three textures and what each pays at the sieve and the rub, every strain's
 * sheet, the record that rolls a step at planting, the harvest that hands it to the buds, the Sifting Box and
 * the rub reading it, the tooltip naming it, and the Stash Jar blending it like tier and band.
 */
public final class ResinGameTest {
    private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos ROOT = FLOOR.above();
    private static final BlockPos BOX = new BlockPos(4, 2, 4);

    /** Very low / low / average / rich / very rich, and the texture: the agreed sheets (cannabinoids.md §5b, #106, #109). */
    private static final Map<ResourceKey<Strain>, ResinSheet> SHEETS = new LinkedHashMap<>();

    static {
        SHEETS.put(ModStrains.BELDIA, new ResinSheet(0, 0, 0, 20, 80, ResinTexture.DRY));
        SHEETS.put(ModStrains.INDICA, new ResinSheet(0, 5, 25, 50, 20, ResinTexture.EVEN));
        SHEETS.put(ModStrains.SATIVA, new ResinSheet(10, 40, 40, 10, 0, ResinTexture.EVEN));
        SHEETS.put(ModStrains.WARPED_KUSH, new ResinSheet(0, 0, 10, 30, 60, ResinTexture.STICKY));
        SHEETS.put(ModStrains.CRIMSON, new ResinSheet(0, 0, 20, 50, 30, ResinTexture.STICKY));
        SHEETS.put(ModStrains.ENDER_HAZE, new ResinSheet(5, 25, 50, 15, 5, ResinTexture.EVEN));
        SHEETS.put(ModStrains.RUDERALIS, new ResinSheet(25, 50, 25, 0, 0, ResinTexture.DRY));
        SHEETS.put(ModStrains.CHERRY_WINE, new ResinSheet(0, 10, 30, 45, 15, ResinTexture.EVEN));
        SHEETS.put(ModStrains.WHITE_WIDOW, new ResinSheet(0, 0, 15, 50, 35, ResinTexture.STICKY));
        SHEETS.put(ModStrains.DUD_WEED, new ResinSheet(80, 12, 8, 0, 0, ResinTexture.EVEN));
    }

    private ResinGameTest() {
    }

    /**
     * Every step on every texture at the sieve and the rub, as agreed: a low step costs in full on both routes, a
     * rich one's bonus goes where the texture sends it. The strain file's {@code resin} object, the rolls landing on
     * their odds, and what a bud with no step reads and what stamping writes.
     */
    public static void theResinStepsAreTheAgreedTable(GameTestHelper context) {
        float[] sieve = {0.5F, 0.75F, 1.0F};
        float[] rub = {1.0F / 8, 1.0F / 6, 1.0F / 4};
        Map<ResinTexture, float[]> richSieve = Map.of(ResinTexture.DRY, new float[]{1.5F, 2.0F},
                ResinTexture.EVEN, new float[]{1.25F, 1.5F}, ResinTexture.STICKY, new float[]{1.25F, 1.5F});
        Map<ResinTexture, float[]> richRub = Map.of(ResinTexture.DRY, new float[]{1.0F / 4, 1.0F / 4},
                ResinTexture.EVEN, new float[]{7.0F / 24, 3.0F / 8}, ResinTexture.STICKY, new float[]{1.0F / 3, 1.0F / 2});
        for (ResinTexture texture : ResinTexture.values()) {
            for (ResinStep step : ResinStep.values()) {
                int i = step.ordinal();
                float expectedSieve = i < 3 ? sieve[i] : richSieve.get(texture)[i - 3];
                float expectedRub = i < 3 ? rub[i] : richRub.get(texture)[i - 3];
                context.assertTrue(Math.abs(step.sieveFactor(texture) - expectedSieve) < 1e-6F,
                        step + " " + texture + " sieves ×" + step.sieveFactor(texture) + ", not ×" + expectedSieve);
                context.assertTrue(Math.abs(step.rubChance(texture) - expectedRub) < 1e-6F,
                        step + " " + texture + " rubs at " + step.rubChance(texture) + ", not " + expectedRub);
            }
        }
        for (ResinStep step : ResinStep.values()) {
            context.assertValueEqual(step.offset(), step.ordinal() - 2, step + "'s place around average");
            context.assertValueEqual(ResinStep.ofOffset(step.offset()), step, step + " read back from its offset");
        }
        context.assertValueEqual(ResinStep.ofOffset(-7), ResinStep.VERY_LOW, "an offset below the steps");
        context.assertValueEqual(ResinStep.ofOffset(9), ResinStep.VERY_RICH, "an offset above the steps");

        // The file.
        Strain bare = parse(Strain.CODEC, "{\"translation_key\":\"x\",\"color\":1,\"buds\":\"minecraft:wheat\"}");
        context.assertValueEqual(bare.resin(), ResinSheet.DEFAULT, "a strain file naming no resin");
        Strain sifted = parse(Strain.CODEC, "{\"translation_key\":\"x\",\"color\":1,\"buds\":\"minecraft:wheat\","
                + "\"plant\":{\"resin\":{\"rich\":20,\"very_rich\":80,\"texture\":\"dry\"}}}");
        context.assertValueEqual(sifted.resin(), new ResinSheet(0, 0, 0, 20, 80, ResinTexture.DRY), "a strain file naming its resin");
        context.assertValueEqual(parse(ResinSheet.CODEC, "{\"texture\":\"sticky\"}"),
                new ResinSheet(0, 0, 0, 0, 0, ResinTexture.STICKY), "a resin naming only its texture");
        context.assertValueEqual(parse(ResinSheet.CODEC, "{}").texture(), ResinTexture.EVEN, "a resin naming no texture");

        // Rolls land on their odds; all-zero odds are average and take no draw.
        ResinSheet beldia = SHEETS.get(ModStrains.BELDIA);
        RandomSource random = RandomSource.create(new Random(20261113L).nextLong());
        Map<ResinStep, Integer> seen = new EnumMap<>(ResinStep.class);
        int rolls = 8000;
        for (int roll = 0; roll < rolls; roll++) {
            seen.merge(beldia.roll(random), 1, Integer::sum);
        }
        for (ResinStep step : ResinStep.values()) {
            double share = (double) seen.getOrDefault(step, 0) / rolls;
            context.assertTrue(Math.abs(share - beldia.weight(step) / 100.0) < 0.02,
                    "Beldía rolled " + step + " " + share + " of the time, against its sheet's " + beldia.weight(step));
        }
        long seed = new Random(20261114L).nextLong();
        RandomSource rolled = RandomSource.create(seed);
        RandomSource counted = RandomSource.create(seed);
        context.assertValueEqual(new ResinSheet(0, 0, 0, 0, 0, ResinTexture.DRY).roll(rolled), ResinStep.AVERAGE, "odds that are all zero");
        context.assertValueEqual(rolled.nextLong(), counted.nextLong(), "odds that are all zero took a draw");

        // A bud with no step reads what it sieved as before resin; that step writes nothing.
        ItemStack kush = new ItemStack(ModItems.INDICA_BUDS);
        ItemStack beldiaBud = new ItemStack(ModItems.BELDIA_BUDS);
        context.assertValueEqual(ResinStep.of(kush), ResinStep.AVERAGE, "a Purple Kush bud with no step");
        context.assertValueEqual(ResinStep.of(beldiaBud), ResinStep.VERY_RICH, "a Beldía bud with no step (two levels a bud)");
        context.assertTrue(ResinStep.stamp(kush.copy(), ResinStep.AVERAGE).getComponentsPatch().isEmpty()
                        && ResinStep.stamp(beldiaBud.copy(), ResinStep.VERY_RICH).getComponentsPatch().isEmpty(),
                "an average Purple Kush bud or a very rich Beldía bud was written a step: it no longer stacks with an old one");
        context.assertValueEqual(ResinStep.stamp(kush.copy(), ResinStep.VERY_RICH).get(ModComponents.RESIN_STEP), ResinStep.VERY_RICH,
                "a very rich Purple Kush bud's step");
        context.assertValueEqual(ResinStep.stamp(beldiaBud.copy(), ResinStep.AVERAGE).get(ModComponents.RESIN_STEP), ResinStep.AVERAGE,
                "an average Beldía bud's step");
        context.assertTrue(ResinStep.stamp(ResinStep.stamp(kush.copy(), ResinStep.LOW), ResinStep.AVERAGE).getComponentsPatch().isEmpty(),
                "a low bud stamped back to average keeps a patch");
        context.succeed();
    }

    /** Every plant carries its agreed resin, Industrial Hemp included (its own sheet, since it is no strain). */
    public static void everyStrainCarriesItsResinSheet(GameTestHelper context) {
        HolderLookup.Provider registries = context.getLevel().registryAccess();
        for (Map.Entry<ResourceKey<Strain>, ResinSheet> sheet : SHEETS.entrySet()) {
            Strain strain = Strain.registry(registries).getOrThrow(sheet.getKey()).value();
            context.assertValueEqual(strain.resin(), sheet.getValue(), sheet.getKey().identifier() + "'s resin");
        }
        context.assertValueEqual(IndustrialHempCropBlock.RESIN, new ResinSheet(92, 5, 3, 0, 0, ResinTexture.EVEN),
                "Industrial Hemp's resin");
        context.succeed();
    }

    /**
     * A plant rolls its resin at planting and keeps it through a save; set back to age 0 it rolls again; a record
     * from before resin rolls it when it joins the world, keeping what it had; Industrial Hemp rolls its own.
     */
    public static void aPlantRollsItsResinAtPlanting(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 2), Block.UPDATE_CLIENTS);
        PlantRecordBlockEntity record = record(world, pos);
        context.assertTrue(record.rolledResin().isPresent(), "a plant was planted with no resin rolled");

        record.setResin(ResinStep.VERY_LOW);
        CompoundTag saved = record.saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(pos, world.getBlockState(pos), saved, registries);
        context.assertTrue(loaded instanceof PlantRecordBlockEntity plant && plant.rolledResin().orElse(null) == ResinStep.VERY_LOW,
                "a plant's resin saved as " + saved + " and loaded as " + loaded);

        CompoundTag old = saved.copy();
        old.remove("resin");
        PlantRecordBlockEntity before = (PlantRecordBlockEntity) BlockEntity.loadStatic(pos, world.getBlockState(pos), old, registries);
        context.assertTrue(before != null && before.rolledResin().isEmpty(), "a record with no resin loaded with some");
        PotencyBand band = before.rolledBand().orElseThrow();
        ResistanceGenes genes = before.rolledResistances().orElseThrow();
        before.setLevel(world);
        context.assertTrue(before.rolledResin().isPresent() && before.rolledBand().orElse(null) == band
                        && genes.equals(before.rolledResistances().orElse(null)),
                "a record from before resin, joining the world, rolled " + before.rolledResin() + " and kept band "
                        + before.rolledBand() + " (was " + band + ") and resistances " + before.rolledResistances());

        boolean kept = true;
        for (int i = 0; i < 40 && kept; i++) {
            world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 3), Block.UPDATE_CLIENTS);
            record(world, pos).setResin(ResinStep.VERY_LOW);
            world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 0), Block.UPDATE_CLIENTS);
            kept = record(world, pos).rolledResin().orElse(null) == ResinStep.VERY_LOW;
        }
        context.assertTrue(!kept, "forty plants set back to age 0 kept their resin: the replant rolls none");

        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        world.setBlock(pos, ((net.minecraft.world.level.block.CropBlock) ModBlocks.INDUSTRIAL_HEMP_CROP).getStateForAge(1),
                Block.UPDATE_CLIENTS);
        PlantRecordBlockEntity fibre = record(world, pos);
        context.assertTrue(fibre.rolledResin().isPresent(), "Industrial Hemp rolled no resin");
        int veryLow = 0;
        for (int i = 0; i < 200; i++) {
            fibre.replant();
            veryLow += fibre.resin() == ResinStep.VERY_LOW ? 1 : 0;
            context.assertTrue(fibre.resin().compareTo(ResinStep.AVERAGE) <= 0, "Industrial Hemp rolled " + fibre.resin());
        }
        context.assertTrue(veryLow > 170 && veryLow < 197, "Industrial Hemp rolled very low " + veryLow
                + " times in 200, against its sheet's 92 in 100");
        context.succeed();
    }

    /**
     * Every bud of a harvest carries its plant's step, leaves none; a harvest with no record writes nothing; the
     * tooltip names the step between the band and the tier, says nothing for average, and nothing on a
     * cannabinoid-free bud.
     */
    public static void aHarvestHandsItsResinToTheBuds(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (Block crop : new Block[]{ModBlocks.INDICA_CROP, ModBlocks.BELDIA_CROP}) {
            Item buds = crop == ModBlocks.INDICA_CROP ? ModItems.INDICA_BUDS : ModItems.BELDIA_BUDS;
            BlockState state = ripe(crop, 7);
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            for (ResinStep step : ResinStep.values()) {
                record(world, pos).setResin(step);
                boolean any = false;
                for (ItemStack drop : drops(world, pos, state, world.getBlockEntity(pos), step.ordinal() + 7L)) {
                    if (drop.is(buds)) {
                        any = true;
                        context.assertValueEqual(ResinStep.of(drop), step, "a bud of a " + step + " " + buds);
                        context.assertValueEqual(drop.has(ModComponents.RESIN_STEP), step != ResinStep.legacy(drop),
                                "whether a " + step + " " + buds + " carries its step written");
                    } else {
                        context.assertTrue(!drop.has(ModComponents.RESIN_STEP), drop + " off a plant carries a resin step");
                    }
                }
                context.assertTrue(any, "a ripe " + crop + " dropped no buds");
            }
            for (ItemStack drop : drops(world, pos, state, null, 3L)) {
                context.assertTrue(!drop.has(ModComponents.RESIN_STEP), drop + " from a harvest with no record carries a resin step");
            }
        }

        // The tooltip.
        String potency = Component.translatable("hempdustry.potency.mid").getString();
        String dank = Component.translatable("hempdustry.tier.dank").getString();
        for (ResinStep step : ResinStep.values()) {
            List<String> lines = lines(world, ResinStep.stamp(new ItemStack(ModItems.INDICA_BUDS), step));
            String line = Component.translatable(step.getTranslationKey()).getString();
            if (step == ResinStep.AVERAGE) {
                context.assertTrue(!lines.contains(line), "an average bud's tooltip names its resin: " + lines);
            } else {
                int at = lines.indexOf(line);
                context.assertTrue(at > lines.indexOf(potency) && lines.indexOf(potency) > 0 && at < lines.indexOf(dank),
                        "a " + step + " bud's tooltip " + lines + " does not read potency, resin, tier");
            }
        }
        context.assertTrue(lines(world, new ItemStack(ModItems.BELDIA_BUDS))
                        .contains(Component.translatable(ResinStep.VERY_RICH.getTranslationKey()).getString()),
                "a Beldía bud with no step, which sieves as a very rich one, does not say so");
        List<String> dud = lines(world, ResinStep.stamp(new ItemStack(ModItems.DUD_WEED_BUDS), ResinStep.VERY_LOW));
        context.assertTrue(!dud.contains(Component.translatable(ResinStep.VERY_LOW.getTranslationKey()).getString()),
                "a cannabinoid-free bud names its resin: " + dud);
        context.succeed();
    }

    /**
     * A bud's step moves its rate at the Sifting Box by its strain's texture; a bud with none sieves as before
     * resin (Beldía two levels); leaves and kief carry none. Played through the box: an average Beldía bud moves
     * one level, an old one two.
     */
    public static void aBudsResinMovesTheSieve(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        Map<ItemStack, Float> expected = new LinkedHashMap<>();
        expected.put(new ItemStack(ModItems.INDICA_BUDS), 1.0F);
        expected.put(new ItemStack(ModItems.BELDIA_BUDS), 2.0F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.INDICA_BUDS), ResinStep.VERY_LOW), 0.5F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.INDICA_BUDS), ResinStep.LOW), 0.75F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.INDICA_BUDS), ResinStep.RICH), 1.25F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.INDICA_BUDS), ResinStep.VERY_RICH), 1.5F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.BELDIA_BUDS), ResinStep.AVERAGE), 1.0F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.BELDIA_BUDS), ResinStep.RICH), 1.5F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.BELDIA_BUDS), ResinStep.VERY_LOW), 0.5F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.WARPED_KUSH_BUDS), ResinStep.VERY_RICH), 1.5F);
        expected.put(ResinStep.stamp(new ItemStack(ModItems.RUDERALIS_BUDS), ResinStep.LOW), 0.75F);
        expected.put(new ItemStack(ModItems.HEMP_LEAF), SiftingBoxBlock.TRIM_CHANCE);
        expected.put(new ItemStack(ModItems.KIEF), SiftingBoxBlock.KIEF_CHANCE);
        for (Map.Entry<ItemStack, Float> entry : expected.entrySet()) {
            float chance = SiftingBoxBlock.sieveChance(entry.getKey(), registries);
            context.assertTrue(Math.abs(chance - entry.getValue()) < 1e-6F, entry.getKey() + " "
                    + ResinStep.written(entry.getKey()) + " sieves at " + chance + ", not " + entry.getValue());
        }

        // In the box: one average Beldía bud is one level; one with no step is two.
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState());
        Players.click(context, Players.survival(context), BOX,
                ResinStep.stamp(new ItemStack(ModItems.BELDIA_BUDS, 4), ResinStep.AVERAGE));
        context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), 1,
                "an average Beldía bud's level on the screen");
        Players.click(context, Players.survival(context), BOX, new ItemStack(ModItems.BELDIA_BUDS, 4));
        context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), 3,
                "an old Beldía bud's two levels on the screen");
        context.succeed();
    }

    /**
     * A plant's step and its strain's texture set the rub, halved under a lamp or stressed; a plant with no record
     * rubs at the old odds. Played: three hundred rubs of very low Purple Kush against three hundred of very rich
     * White Widow (sticky, 1 in 2).
     */
    public static void aPlantsResinMovesTheRub(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);

        context.assertTrue(Math.abs(Defoliation.charasChance(world, context.absolutePos(BOX), ModBlocks.INDICA_CROP.defaultBlockState())
                - 1.0F / Defoliation.CHARAS_CHANCE_ONE_IN) < 1e-6F, "a plant with no record does not rub at 1 in 4");
        context.assertTrue(Math.abs(Defoliation.charasChance(world, context.absolutePos(BOX), ModBlocks.WARPED_KUSH_CROP.defaultBlockState())
                - 1.0F / Defoliation.CHARAS_CHANCE_ONE_IN_WARPED) < 1e-6F, "a Warped Kush with no record does not rub at 1 in 2");

        Map<Block, ResinTexture> textures = Map.of(ModBlocks.INDICA_CROP, ResinTexture.EVEN,
                ModBlocks.BELDIA_CROP, ResinTexture.DRY, ModBlocks.WHITE_WIDOW_CROP, ResinTexture.STICKY);
        for (Map.Entry<Block, ResinTexture> crop : textures.entrySet()) {
            BlockState state = ripe(crop.getKey(), 7);
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
            for (ResinStep step : ResinStep.values()) {
                record(world, pos).setResin(step);
                float sun = Defoliation.charasChance(world, pos, state);
                float lamp = Defoliation.charasChance(world, pos, state.setValue(GrowLight.PROPERTY, GrowLight.LAMP));
                float stressed = Defoliation.charasChance(world, pos, state.setValue(GrowLight.PROPERTY, GrowLight.STRESSED));
                float expected = step.rubChance(crop.getValue());
                context.assertTrue(Math.abs(sun - expected) < 1e-6F && Math.abs(lamp - expected / 2) < 1e-6F
                                && Math.abs(stressed - expected / 2) < 1e-6F,
                        "a " + step + " " + crop.getKey() + " rubs at " + sun + " / lamp " + lamp + " / stressed " + stressed
                                + ", not " + expected + " and half of it");
            }
        }

        int poor = rubs(context, world, pos, ModBlocks.INDICA_CROP, ResinStep.VERY_LOW, 300);
        int rich = rubs(context, world, pos, ModBlocks.WHITE_WIDOW_CROP, ResinStep.VERY_RICH, 300);
        context.assertTrue(poor >= 15 && poor <= 65, "very low Purple Kush paid " + poor + " charas in 300 rubs, against 1 in 8");
        context.assertTrue(rich >= 115 && rich <= 185, "very rich White Widow paid " + rich + " charas in 300 rubs, against 1 in 2");
        context.succeed();
    }

    /**
     * The Stash Jar blends resin as it blends tier and band: exact sums, best out first on all three, at most four
     * stacks, order-free; an old bud counts as the step it sieves as, and comes back out stacking with old ones;
     * an entry saved without its resin sum reads that too.
     */
    public static void aJarBlendsResin(GameTestHelper context) {
        // 1 very rich + 1 very low: two average buds, written as nothing.
        List<ItemStack> parts = blend(List.of(kush(1, BudTier.DANK, PotencyBand.MID, ResinStep.VERY_RICH),
                kush(1, BudTier.DANK, PotencyBand.MID, ResinStep.VERY_LOW))).parts();
        context.assertTrue(parts.size() == 1 && parts.getFirst().getCount() == 2 && parts.getFirst().getComponentsPatch().isEmpty(),
                "very rich + very low blended to " + describe(parts) + ", not two plain buds");

        // Four buds, three axes, four stacks, best first.
        List<ItemStack> in = new ArrayList<>(List.of(
                kush(1, BudTier.TOP_SHELF, PotencyBand.HIGH, ResinStep.RICH),
                kush(1, BudTier.DANK, PotencyBand.HIGH, ResinStep.RICH),
                kush(1, BudTier.DANK, PotencyBand.MID, ResinStep.RICH),
                kush(1, BudTier.DANK, PotencyBand.MID, ResinStep.AVERAGE)));
        List<ItemStack> expected = List.copyOf(in);
        Collections.shuffle(in, new Random(20261113L));
        parts = blend(in).parts();
        boolean same = parts.size() == expected.size();
        for (int i = 0; same && i < parts.size(); i++) {
            same = ItemStack.matches(parts.get(i), expected.get(i));
        }
        context.assertTrue(same, "four buds on three axes laid out as " + describe(parts) + ", not " + describe(expected));

        // 3 rich + 1 very low: sum 1 over 4, one rich and three average, nothing made or lost.
        parts = blend(List.of(kush(3, BudTier.DANK, PotencyBand.MID, ResinStep.RICH),
                kush(1, BudTier.DANK, PotencyBand.MID, ResinStep.VERY_LOW))).parts();
        context.assertTrue(parts.size() == 2 && ResinStep.of(parts.get(0)) == ResinStep.RICH && parts.get(0).getCount() == 1
                        && ResinStep.of(parts.get(1)) == ResinStep.AVERAGE && parts.get(1).getCount() == 3,
                "3 rich + 1 very low blended to " + describe(parts));

        // Old Beldía buds read very rich and come back plain; an average one levels them down.
        parts = blend(List.of(new ItemStack(ModItems.BELDIA_BUDS, 3))).parts();
        context.assertTrue(parts.size() == 1 && ItemStack.matches(parts.getFirst(), new ItemStack(ModItems.BELDIA_BUDS, 3)),
                "three old Beldía buds came out as " + describe(parts));
        parts = blend(List.of(new ItemStack(ModItems.BELDIA_BUDS),
                ResinStep.stamp(new ItemStack(ModItems.BELDIA_BUDS), ResinStep.AVERAGE))).parts();
        context.assertTrue(parts.size() == 1 && parts.getFirst().getCount() == 2 && ResinStep.of(parts.getFirst()) == ResinStep.RICH,
                "an old Beldía bud and an average one blended to " + describe(parts) + ", not two rich ones");

        // Saved without a resin sum (a jar from before resin): the old step, per bud.
        DynamicOps<JsonElement> ops = context.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        StashContents beldia = blend(List.of(new ItemStack(ModItems.BELDIA_BUDS, 5), new ItemStack(ModItems.INDICA_BUDS, 2)));
        JsonElement saved = StashContents.CODEC.encodeStart(ops, beldia).getOrThrow();
        for (JsonElement entry : saved.getAsJsonArray()) {
            context.assertTrue(((JsonObject) entry).remove("resin_sum") != null, "an entry saved no resin sum: " + entry);
        }
        StashContents read = StashContents.CODEC.parse(ops, saved).getOrThrow();
        context.assertValueEqual(read, beldia, "a jar saved without resin sums");
        context.succeed();
    }

    private static int rubs(GameTestHelper context, ServerLevel world, BlockPos pos, Block crop, ResinStep step, int count) {
        ServerPlayer player = Players.survival(context);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        AABB area = AABB.encapsulatingFullBlocks(pos.offset(-4, -2, -4), pos.offset(4, 3, 4));
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, area, e -> true)) {
            item.discard();
        }
        world.setBlock(pos, ripe(crop, Defoliation.RUB_MAX_AGE), Block.UPDATE_CLIENTS);
        for (int i = 0; i < count; i++) {
            // The same block, unrubbed again: its record (and the step pinned on it) stays.
            world.setBlock(pos, ripe(crop, Defoliation.RUB_MAX_AGE), Block.UPDATE_CLIENTS);
            record(world, pos).setResin(step);
            ItemStack shears = new ItemStack(Items.SHEARS);
            player.setItemInHand(InteractionHand.MAIN_HAND, shears);
            world.getBlockState(pos).useItemOn(shears, world, player, InteractionHand.MAIN_HAND, hit);
        }
        int charas = 0;
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, area, e -> true)) {
            if (item.getItem().is(ModItems.CHARAS)) {
                charas += item.getItem().getCount();
            }
            item.discard();
        }
        return charas;
    }

    private static ItemStack kush(int count, BudTier tier, PotencyBand band, ResinStep resin) {
        return ResinStep.stamp(PotencyBand.stamp(BudTier.stamp(new ItemStack(ModItems.INDICA_BUDS, count), tier), band), resin);
    }

    private static StashContents blend(List<ItemStack> stacks) {
        StashContents.Mutable stash = StashContents.EMPTY.mutable();
        for (ItemStack stack : stacks) {
            stash.insert(stack.copy(), Integer.MAX_VALUE);
        }
        return stash.toImmutable();
    }

    private static String describe(List<ItemStack> stacks) {
        return stacks.stream().map(stack -> stack.getCount() + " " + BudTier.of(stack) + "/" + PotencyBand.of(stack)
                + "/" + ResinStep.of(stack) + (stack.has(ModComponents.RESIN_STEP) ? "" : " (unwritten)")).toList().toString();
    }

    private static List<String> lines(ServerLevel world, ItemStack stack) {
        List<String> lines = new ArrayList<>();
        for (Component line : stack.getTooltipLines(Item.TooltipContext.of(world), null, TooltipFlag.NORMAL)) {
            lines.add(line.getString());
        }
        return lines;
    }

    private static <T> T parse(com.mojang.serialization.Codec<T> codec, String json) {
        return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static BlockState ripe(Block crop, int age) {
        return Defoliation.unworked(crop.defaultBlockState())
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(IndicaCropBlock.AGE, age);
    }

    private static PlantRecordBlockEntity record(ServerLevel world, BlockPos pos) {
        return (PlantRecordBlockEntity) world.getBlockEntity(pos);
    }

    private static List<ItemStack> drops(ServerLevel world, BlockPos pos, BlockState state, BlockEntity entity, long seed) {
        LootTable table = world.getServer().reloadableRegistries().getLootTable(state.getBlock().getLootTable().orElseThrow());
        LootParams.Builder params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity);
        return table.getRandomItems(params.create(LootContextParamSets.BLOCK), seed);
    }
}

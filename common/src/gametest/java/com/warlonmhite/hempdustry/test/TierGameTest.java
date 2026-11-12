package com.warlonmhite.hempdustry.test;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.ChemotypeBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.GrowLuck;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.Mildew;
import com.warlonmhite.hempdustry.block.custom.PlantStress;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.custom.TierBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.SiftingBoxBlockEntity;
import com.warlonmhite.hempdustry.block.custom.stash.StashPieces;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.component.StashContents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.GrowQuality;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.QualityGene;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Quality and bud tiers: a hidden gene rolled at planting, an open-sky record kept like the light, one
 * tier roll per harvest in vanilla's loot-luck grammar, the tier on the bud with its rarity, what it buys
 * in a bowl (each entry its own), the kief and hash that carry their batch's, and nothing in butter.
 * Absent is Dank everywhere, so nothing 2.1 made changes.
 */
public final class TierGameTest {

    private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos ROOT = FLOOR.above();
    private static final BlockPos BOX = new BlockPos(5, 1, 5);
    private static final BlockPos BAR = new BlockPos(5, 1, 2);

    /** Every strain's quality sheet (base luck, then poor / average / good / elite), as agreed. */
    private static final Map<ResourceKey<Strain>, GrowQuality> SHEETS = new LinkedHashMap<>();

    static {
        SHEETS.put(ModStrains.INDICA, new GrowQuality(0, 20, 50, 25, 5));
        SHEETS.put(ModStrains.SATIVA, new GrowQuality(0, 25, 50, 20, 5));
        SHEETS.put(ModStrains.BELDIA, new GrowQuality(-1, 25, 50, 20, 5));
        SHEETS.put(ModStrains.WARPED_KUSH, new GrowQuality(2, 15, 50, 25, 10));
        SHEETS.put(ModStrains.CRIMSON, new GrowQuality(2, 15, 50, 25, 10));
        SHEETS.put(ModStrains.ENDER_HAZE, new GrowQuality(0, 15, 45, 30, 10));
        SHEETS.put(ModStrains.RUDERALIS, new GrowQuality(0, 30, 50, 18, 2));
        SHEETS.put(ModStrains.CHERRY_WINE, new GrowQuality(0, 20, 50, 25, 5));
        SHEETS.put(ModStrains.WHITE_WIDOW, new GrowQuality(0, 18, 48, 28, 6));
        SHEETS.put(ModStrains.DUD_WEED, new GrowQuality(0, 30, 50, 18, 2));
    }

    /** The spec's odds table in whole percent, Mids / Dank / Top Shelf / Epic, at L −1 to 8. */
    private static final int[][] TABLE = {
            {69, 31, 0, 0}, {63, 37, 0, 0}, {53, 41, 6, 0}, {42, 45, 13, 0}, {32, 49, 19, 0},
            {21, 53, 25, 2}, {10, 56, 30, 4}, {0, 58, 36, 6}, {0, 56, 37, 7}, {0, 54, 38, 8}};

    /**
     * The tier table is vanilla's: each tier weighs max(⌊w + q × L⌋, 0), which gives the spec's odds at
     * every L from −1 to 8, and a roll draws those odds — exactly one draw, whatever it decides.
     */
    public static void theTierTableIsVanillasLuckGrammar(GameTestHelper context) {
        for (int i = 0; i < TABLE.length; i++) {
            int luck = i - 1;
            int total = 0;
            for (BudTier tier : BudTier.values()) {
                total += tier.weightAt(luck);
            }
            for (BudTier tier : BudTier.values()) {
                int percent = Math.round(100.0F * tier.weightAt(luck) / total);
                context.assertValueEqual(percent, TABLE[i][tier.ordinal()], tier + "'s share at L " + luck);
            }
        }
        context.assertValueEqual(BudTier.EPIC.weightAt(3), 0, "Epic's weight at L 3: it appears only from L 4");
        context.assertTrue(BudTier.EPIC.weightAt(4) > 0, "Epic's weight at L 4");
        context.assertValueEqual(BudTier.TOP_SHELF.weightAt(0.5F), 3, "Top Shelf's weight at a fractional L 0.5");

        // Rolled at several L, on seeds drawn from a seeded java.util.Random: within 1.5 % of the table.
        Random seeds = new Random(20261112L);
        for (int luck : new int[]{-1, 0, 2, 4, 5, 7}) {
            RandomSource random = RandomSource.create(seeds.nextLong());
            int rolls = 20000;
            int[] seen = new int[BudTier.values().length];
            for (int roll = 0; roll < rolls; roll++) {
                seen[BudTier.roll(luck, random).ordinal()]++;
            }
            int total = 0;
            for (BudTier tier : BudTier.values()) {
                total += tier.weightAt(luck);
            }
            for (BudTier tier : BudTier.values()) {
                double expected = (double) tier.weightAt(luck) / total;
                double share = (double) seen[tier.ordinal()] / rolls;
                context.assertTrue(tier.weightAt(luck) == 0 ? seen[tier.ordinal()] == 0 : Math.abs(share - expected) < 0.015,
                        tier + " came up " + share + " of the time at L " + luck + ", expected " + expected);
            }
        }

        // One draw a roll, even where only one tier can come up (L -20: Mids alone; L 20: no Mids).
        for (float luck : new float[]{-20.0F, 0.0F, 3.0F, 20.0F}) {
            long seed = seeds.nextLong();
            RandomSource rolled = RandomSource.create(seed);
            RandomSource counted = RandomSource.create(seed);
            BudTier.roll(luck, rolled);
            counted.nextInt(1000);
            context.assertValueEqual(rolled.nextLong(), counted.nextLong(), "the draw after a roll at L " + luck
                    + ": a roll took other than one draw");
        }
        context.succeed();
    }

    /**
     * Every strain carries the agreed quality sheet; a plant rolls its gene at planting from those odds and
     * keeps it; a strain file without {@code quality} reads the default; and the ceilings without player
     * luck hold — Purple Kush 7, Beldía 6, the Nether pair 6, Ruderalis 2, so never Epic.
     */
    public static void aPlantRollsItsGeneAndKnowsItsCeiling(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        for (Map.Entry<ResourceKey<Strain>, GrowQuality> sheet : SHEETS.entrySet()) {
            context.assertValueEqual(Strain.registry(registries).getOrThrow(sheet.getKey()).value().quality(), sheet.getValue(),
                    sheet.getKey().identifier() + "'s quality sheet");
        }
        for (GrowQuality quality : SHEETS.values()) {
            int total = quality.poor() + quality.average() + quality.good() + quality.elite();
            RandomSource random = RandomSource.create(new Random(20261112L + quality.hashCode()).nextLong());
            int rolls = 8000;
            Map<QualityGene, Integer> seen = new EnumMap<>(QualityGene.class);
            for (int roll = 0; roll < rolls; roll++) {
                seen.merge(quality.roll(random), 1, Integer::sum);
            }
            for (QualityGene gene : QualityGene.values()) {
                double expected = (double) quality.weight(gene) / total;
                double share = (double) seen.getOrDefault(gene, 0) / rolls;
                context.assertTrue(Math.abs(share - expected) < 0.02,
                        "a sheet " + quality + " rolled " + gene + " " + share + " of the time, expected " + expected);
            }
        }
        Strain bare = Strain.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"translation_key\":\"x\",\"color\":1,\"buds\":\"minecraft:wheat\"}")).getOrThrow();
        context.assertValueEqual(bare.quality(), GrowQuality.DEFAULT, "a strain file naming no quality");

        // Planted, a root has its gene; it lives through a save; set back to age 0, the plant rolls again.
        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 2), Block.UPDATE_CLIENTS);
        PlantRecordBlockEntity record = record(world, pos);
        context.assertTrue(record.rolledGene().isPresent(), "a plant was planted with no gene rolled");
        record.setGene(QualityGene.ELITE);
        record.setOpenSky(true);
        var saved = record.saveWithFullMetadata(registries);
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, world.getBlockState(pos), saved, registries);
        context.assertTrue(loaded instanceof PlantRecordBlockEntity plant && plant.rolledGene().orElse(null) == QualityGene.ELITE
                        && plant.openSky(), "an elite sun-grown plant saved as " + saved + " and loaded as " + loaded);
        boolean rolledElite = true;
        for (int i = 0; i < 40 && rolledElite; i++) {
            world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 3), Block.UPDATE_CLIENTS);
            record(world, pos).setGene(QualityGene.ELITE);
            world.setBlock(pos, ((IndicaCropBlock) ModBlocks.INDICA_CROP).getStateForAge(0), Block.UPDATE_CLIENTS);
            rolledElite = record(world, pos).rolledGene().orElse(null) == QualityGene.ELITE;
        }
        context.assertTrue(!rolledElite, "forty plants set back to age 0 kept an elite gene: the replant rolls no gene");
        context.assertTrue(record(world, pos).skyRecord().isEmpty(), "a replanted plant kept its old sky record");

        // The ceilings with no player luck: the best gene and all the care a plant can take.
        BlockState kush = ripe(ModBlocks.INDICA_CROP, IndicaCropBlock.MAX_AGE)
                .setValue(Defoliation.TRIMMED_EARLY, true).setValue(Defoliation.TRIMMED_LATE, true)
                .setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP);
        context.assertValueEqual(ceiling(ModStrains.INDICA, registries, kush, true), 7, "Purple Kush's ceiling");
        BlockState beldia = ripe(ModBlocks.BELDIA_CROP, IndicaCropBlock.MAX_AGE)
                .setValue(Defoliation.TRIMMED_EARLY, true).setValue(Defoliation.TRIMMED_LATE, true)
                .setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP);
        context.assertValueEqual(ceiling(ModStrains.BELDIA, registries, beldia, true), 6, "Beldía's ceiling");
        // The Nether pair keep no light record: their state's record stays the sun's whatever hangs over it.
        BlockState warped = ripe(ModBlocks.WARPED_KUSH_CROP, IndicaCropBlock.MAX_AGE)
                .setValue(Defoliation.TRIMMED_EARLY, true).setValue(Defoliation.TRIMMED_LATE, true);
        context.assertValueEqual(ceiling(ModStrains.WARPED_KUSH, registries, warped, false), 6, "Warped Kush's ceiling");
        BlockState ruderalis = ModBlocks.RUDERALIS_CROP.defaultBlockState();
        int ruderalisCeiling = ceiling(ModStrains.RUDERALIS, registries, ruderalis, false);
        context.assertValueEqual(ruderalisCeiling, 2, "Ruderalis's ceiling");
        context.assertValueEqual(BudTier.EPIC.weightAt(ruderalisCeiling), 0, "Epic's weight at Ruderalis's ceiling");
        context.assertValueEqual(BudTier.EPIC.weightAt(ruderalisCeiling + 1), 0,
                "Epic's weight at Ruderalis's ceiling with its own Luck smoke");
        context.succeed();
    }

    /** A strain's best L without player luck: base, an elite gene, and the care {@code best} shows. */
    private static int ceiling(ResourceKey<Strain> strain, HolderLookup.Provider registries, BlockState best, boolean tray) {
        return Strain.registry(registries).getOrThrow(strain).value().quality().baseLuck() + QualityGene.ELITE.luck()
                + GrowLuck.care(best, false, tray);
    }

    /**
     * Grow luck adds up as the spec has it: base, gene, each trim, the best light only (open sky one, never
     * on top of a lamp), a Hydro Tray, and the harvesting player's luck — Luck II two more, Bad Luck one less,
     * nobody nothing.
     */
    public static void growLuckAddsUp(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockState ripe = ripe(ModBlocks.INDICA_CROP, IndicaCropBlock.MAX_AGE);
        world.setBlock(pos, ripe, Block.UPDATE_CLIENTS);
        PlantRecordBlockEntity record = record(world, pos);
        record.setGene(QualityGene.GOOD);
        record.setOpenSky(false);

        context.assertValueEqual(GrowLuck.of(record, ripe, world, null), 1.0F, "a good plant in the shade, untrimmed");
        BlockState trimmed = ripe.setValue(Defoliation.TRIMMED_EARLY, true);
        context.assertValueEqual(GrowLuck.of(record, trimmed, world, null), 2.0F, "one trim window used");
        trimmed = trimmed.setValue(Defoliation.TRIMMED_LATE, true);
        context.assertValueEqual(GrowLuck.of(record, trimmed, world, null), 3.0F, "both trim windows used");
        record.setOpenSky(true);
        context.assertValueEqual(GrowLuck.of(record, trimmed, world, null), 4.0F, "both trims under open sky");
        context.assertValueEqual(GrowLuck.of(record, trimmed.setValue(GrowLight.PROPERTY, GrowLight.AMBIENT), world, null), 4.0F,
                "under open sky with a glowstone record: the sun counts, glowstone is nothing");
        context.assertValueEqual(GrowLuck.of(record, trimmed.setValue(GrowLight.PROPERTY, GrowLight.LAMP), world, null), 4.0F,
                "a lamp record under open sky: one light only");
        context.assertValueEqual(GrowLuck.of(record, trimmed.setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP), world, null), 5.0F,
                "a Grow Lamp record under open sky: the Grow Lamp's two, never three");
        record.setOpenSky(false);
        context.assertValueEqual(GrowLuck.of(record, trimmed.setValue(GrowLight.PROPERTY, GrowLight.LAMP), world, null), 4.0F,
                "a lamp record in the shade");
        world.setBlock(pos.below(), ModBlocks.HYDRO_TRAY.defaultBlockState(), Block.UPDATE_CLIENTS);
        context.assertValueEqual(GrowLuck.of(record, trimmed, world, null), 4.0F, "both trims on a Hydro Tray");

        ServerPlayer player = Players.survival(context);
        context.assertValueEqual(GrowLuck.of(record, trimmed, world, player), 4.0F, "a player with no luck");
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 1));
        context.assertValueEqual(GrowLuck.of(record, trimmed, world, player), 6.0F, "a player under Luck II");
        player.removeEffect(MobEffects.LUCK);
        player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 600, 0));
        context.assertValueEqual(GrowLuck.of(record, trimmed, world, player), 3.0F, "a player under Bad Luck");
        Entity notAPlayer = net.minecraft.world.entity.EntityTypes.PIG.create(world, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        context.assertValueEqual(GrowLuck.of(record, trimmed, world, notAPlayer), 4.0F, "a harvester that is no player");
        context.succeed();
    }

    /**
     * The open-sky record is read like the light: the sky at the full-grown top on the first step, the
     * weaker of before and now after; glass passes the sky, leaves stop it, and a lost sun never comes back
     * nor stresses the plant. A plant that keeps no light record never takes it.
     */
    public static void theOpenSkyIsReadLikeTheLight(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(ROOT);
        BlockPos top = pos.above();
        // The structure's barrier roof is at y 8 over every column: open this one.
        context.setBlock(new BlockPos(ROOT.getX(), 8, ROOT.getZ()), Blocks.AIR);
        // Wet farmland: a bone-dry field could stress the flowering steps below for a reason of its own.
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE, 7),
                Block.UPDATE_CLIENTS);
        world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 0), Block.UPDATE_CLIENTS);
        // Light is updated off the tick that changed it, and a busy batch of tests (or a slow machine) holds the
        // light engine back: every check waits for the sky it needs rather than for a fixed number of ticks.
        context.startSequence()
                .thenWaitUntil(() -> context.assertTrue(world.canSeeSky(top), "the opened column reads no sky at the plant's top"))
                .thenExecute(() -> {
                    step(world, pos, 0);
                    context.assertValueEqual(record(world, pos).skyRecord().orElse(null), Boolean.TRUE, "a seedling's first step under open sky");
                    world.setBlock(top.above(), Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenWaitUntil(() -> context.assertTrue(world.canSeeSky(top), "a glass roof stopped the sky"))
                .thenExecute(() -> {
                    step(world, pos, 1);
                    context.assertValueEqual(record(world, pos).skyRecord().orElse(null), Boolean.TRUE, "a step under a glass roof");
                    world.setBlock(top.above(), Blocks.OAK_LEAVES.defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenWaitUntil(() -> context.assertTrue(!world.canSeeSky(top), "leaves over the plant let the full sky through"))
                .thenExecute(() -> {
                    GrowLight light = step(world, pos, 4);
                    context.assertValueEqual(record(world, pos).skyRecord().orElse(null), Boolean.FALSE, "a step under leaves");
                    context.assertTrue(light != GrowLight.STRESSED, "losing the sun stressed a flowering plant");
                    world.setBlock(top.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenWaitUntil(() -> context.assertTrue(world.canSeeSky(top), "the sky did not come back when the leaves went"))
                .thenExecute(() -> {
                    step(world, pos, 5);
                    context.assertValueEqual(record(world, pos).skyRecord().orElse(null), Boolean.FALSE,
                            "the sun back after a step in the shade: the weaker of before and now");
                    // A first step reads the sky now, whatever came before.
                    step(world, pos, 0);
                    context.assertValueEqual(record(world, pos).skyRecord().orElse(null), Boolean.TRUE, "a first step under the sun");
                    // A Warped Kush keeps no light record: a bee's step under the same sky leaves it none.
                    world.setBlock(pos.below(), Blocks.SOUL_SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
                    BlockState young = ripe(ModBlocks.WARPED_KUSH_CROP, 1);
                    world.setBlock(pos, young, Block.UPDATE_CLIENTS);
                    PlantStress.beeStep(world, pos, young, ripe(ModBlocks.WARPED_KUSH_CROP, 2), world.getRandom());
                    context.assertTrue(record(world, pos).skyRecord().isEmpty(),
                            "a Warped Kush, which keeps no light record, took the sky");
                })
                .thenSucceed();
    }

    /** One growth step of the Purple Kush at {@code pos} from {@code fromAge}, as every growth route takes it. */
    private static GrowLight step(ServerLevel world, BlockPos pos, int fromAge) {
        return PlantStress.afterStep(world, pos, GrowLight.NATURAL, fromAge, fromAge + 1, 2, 0.0F,
                RandomSource.create(1L), true, false);
    }

    /**
     * One roll per harvest: every bud of a harvest, Fortune's extras and the lamp's included, carries the
     * same tier; the tier takes no draw of the loot's (the same seed drops the same items with a record or
     * without); a stressed or mildewed plant gives Mids and a sulfured one rolls; no record, Mids.
     */
    public static void oneTierRollPerHarvest(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockState ripe = ripe(ModBlocks.INDICA_CROP, IndicaCropBlock.MAX_AGE)
                .setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP);
        world.setBlock(pos, ripe, Block.UPDATE_CLIENTS);
        PlantRecordBlockEntity record = record(world, pos);
        record.setGene(QualityGene.ELITE);
        ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
        hoe.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);

        Random seeds = new Random(20261112L);
        Map<BudTier, Integer> tiers = new EnumMap<>(BudTier.class);
        for (int roll = 0; roll < 60; roll++) {
            long seed = seeds.nextLong();
            List<ItemStack> plain = drops(world, pos, ripe, null, hoe, null, seed);
            List<ItemStack> tiered = drops(world, pos, ripe, record, hoe, null, seed);
            context.assertValueEqual(tiered.size(), plain.size(), "a recorded harvest's stacks against a recordless one's");
            BudTier harvest = null;
            int buds = 0;
            for (int i = 0; i < tiered.size(); i++) {
                ItemStack got = tiered.get(i);
                context.assertTrue(got.getItem() == plain.get(i).getItem() && got.getCount() == plain.get(i).getCount(),
                        "a recorded harvest dropped " + tiered + " where the same seed drops " + plain + " without one");
                if (got.is(ModItems.INDICA_BUDS)) {
                    buds += got.getCount();
                    context.assertTrue(harvest == null || BudTier.of(got) == harvest,
                            "one harvest's buds carry two tiers: " + tiered);
                    harvest = BudTier.of(got);
                    context.assertValueEqual(BudTier.of(plain.get(i)), BudTier.MIDS, "a recordless harvest's bud tier");
                } else {
                    context.assertTrue(!got.has(ModComponents.BUD_TIER), "a tier on " + got + ", which is no bud");
                }
            }
            context.assertTrue(buds >= 4, "a Grow Lamp harvest at Fortune III gave " + buds + " buds");
            tiers.merge(harvest, 1, Integer::sum);
        }
        // L 4 (elite, a Grow Lamp): Mids 21, Dank 53, Top Shelf 25, Epic 2 in a hundred. Sixty harvests see
        // more than one tier, and never only Mids.
        context.assertTrue(tiers.size() >= 2 && tiers.getOrDefault(BudTier.DANK, 0) > 0, "sixty harvests at L 4 gave " + tiers);

        // Stressed or mildewed: Mids, however lucky; sulfured rolls like a healthy plant.
        for (BlockState sick : new BlockState[]{ripe.setValue(GrowLight.PROPERTY, GrowLight.STRESSED),
                ripe.setValue(Mildew.PROPERTY, Mildew.MILDEWED)}) {
            world.setBlock(pos, sick, Block.UPDATE_CLIENTS);
            record(world, pos).setGene(QualityGene.ELITE);
            ServerPlayer lucky = Players.survival(context);
            lucky.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 9));
            for (int roll = 0; roll < 10; roll++) {
                for (ItemStack got : drops(world, pos, sick, record(world, pos), hoe, lucky, seeds.nextLong())) {
                    context.assertTrue(!got.is(ModItems.INDICA_BUDS) || BudTier.of(got) == BudTier.MIDS,
                            "a sick plant, " + sick + ", harvested with Luck X gave " + got + " at " + BudTier.of(got));
                }
            }
        }
        context.assertTrue(!GrowLuck.givesMids(ripe.setValue(Mildew.PROPERTY, Mildew.SULFURED)),
                "a sulfured plant skips the roll");
        context.succeed();
    }

    /**
     * What a tier buys when smoked: each entry's tier scales its own effects (Mids ¾, Epic 1½, after the
     * band's stretch, CBD on its own clock), the load's primary entry sets the cough, the Nausea and the
     * green-out odds, and the weakest bud sets an entry's tier when packing or rolling.
     */
    public static void aTierBuysSmoothnessAndLength(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        Holder<Strain> kush = strain(registries, ModStrains.INDICA);
        Holder<Strain> haze = strain(registries, ModStrains.SATIVA);
        Holder<Strain> beldia = strain(registries, ModStrains.BELDIA);
        Holder<Strain> cherry = strain(registries, ModStrains.CHERRY_WINE);
        Holder<Strain> hashish = strain(registries, ModStrains.HASHISH);
        int device = 1000;
        float[] factor = {0.75F, 1.0F, 1.25F, 1.5F};
        for (BudTier tier : BudTier.values()) {
            float f = factor[tier.ordinal()];
            context.assertValueEqual(duration(tiered(kush, 1, tier), device, MobEffects.HUNGER), Math.round(device * f),
                    "a " + tier + " Purple Kush bud's Hunger");
            context.assertValueEqual(duration(tiered(beldia, 1, tier), device, MobEffects.INVISIBILITY), Math.round(device * f),
                    "a " + tier + " Beldía bud's Invisibility, a Strain-labelled effect");
            context.assertValueEqual(duration(tiered(cherry, 1, tier), device, MobEffects.REGENERATION),
                    duration(SmokeContents.of(cherry, 1), device, MobEffects.REGENERATION),
                    "a " + tier + " Cherry Wine bud's Regeneration, a CBD-labelled effect on its own clock");
        }
        // Extra-high and Epic: the band's ×2 cap, then the tier's ×1.5 — three times the device.
        SmokeContents strongest = SmokeContents.of(kush, 1, Chemotype.THC_DOMINANT, 20).withTiers(
                java.util.Optional.of(BudTier.EPIC), java.util.Optional.empty());
        context.assertValueEqual(duration(strongest, device, MobEffects.HUNGER), 3 * device,
                "an Epic bud far past the band's cap");

        // Each entry its own: an Epic Lemon Haze bud with Mids hash.
        SmokeContents spliff = new SmokeContents(List.of(
                new SmokeContents.Entry(haze, 1).withTier(BudTier.EPIC),
                new SmokeContents.Entry(hashish, 1).withTier(BudTier.MIDS)));
        context.assertValueEqual(duration(spliff, device, MobEffects.SPEED), 1500, "the Epic bud's Speed in a hash spliff");
        context.assertValueEqual(duration(spliff, device, MobEffects.NIGHT_VISION), 750, "the Mids hash's Night Vision in it");

        // The odds a hit rolls once follow the primary entry (the plant, in a spliff of one and one).
        int[] cough = {2, 3, 6, 0};
        int[] nausea = {5, 7, 10, 0};
        int[] greenOut = {4, 5, 8, 16};
        for (BudTier tier : BudTier.values()) {
            SmokeContents load = tiered(kush, 1, tier);
            context.assertValueEqual(Smoking.coughOneIn(3, load), cough[tier.ordinal()], "a bong's cough, 1 in N, at " + tier);
            context.assertValueEqual(Smoking.nauseaOneIn(5, load), nausea[tier.ordinal()], "a bong's Nausea, 1 in N, at " + tier);
            context.assertValueEqual(Smoking.greenOutOneIn(4, load), greenOut[tier.ordinal()], "a full green-out, 1 in N, at " + tier);
        }
        context.assertValueEqual(Smoking.coughOneIn(3, spliff), 0, "an Epic bud's spliff with Mids hash: the bud decides, no cough");
        context.assertValueEqual(Smoking.greenOutOneIn(0, tiered(kush, 1, BudTier.MIDS)), 0, "a dose that never greens out");

        // Schwag smokes outside the tiers: 2.1's duration and odds, whatever its entry says.
        Holder<Strain> schwag = strain(registries, ModStrains.SCHWAG);
        SmokeContents spoiled = SmokeContents.of(schwag, 1);
        context.assertValueEqual(Smoking.nauseaOneIn(5, spoiled), 5, "a schwag bong's Nausea, 1 in N, as 2.1");
        for (BudTier tier : BudTier.values()) {
            context.assertValueEqual(Smoking.greenOutOneIn(4, tiered(schwag, 1, tier)), Smoking.greenOutOneIn(4, spoiled),
                    "a schwag load's green-out, 1 in N, with its entry at " + tier);
            context.assertValueEqual(Smoking.coughOneIn(3, tiered(schwag, 1, tier)), Smoking.coughOneIn(3, spoiled),
                    "a schwag bong's cough, 1 in N, with its entry at " + tier);
            context.assertValueEqual(Smoking.nauseaOneIn(5, tiered(schwag, 1, tier)), 5,
                    "a schwag load's Nausea, 1 in N, with its entry at " + tier);
        }
        context.assertValueEqual(duration(tiered(schwag, 1, BudTier.EPIC), device, MobEffects.POISON),
                duration(spoiled, device, MobEffects.POISON), "schwag's Poison, whatever tier its entry names");

        // Packing and rolling: the weakest bud of the entry, the weakest hash of the other.
        ItemStack packed = PackingRecipe.INSTANCE.assemble(CraftingInput.of(3, 1, List.of(new ItemStack(ModItems.BONG),
                bud(ModItems.INDICA_BUDS, BudTier.EPIC), bud(ModItems.INDICA_BUDS, BudTier.MIDS))), registries);
        context.assertValueEqual(packed.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).entries().getFirst().tier(),
                BudTier.MIDS, "a bong packed with an Epic bud and a Mids one");
        ItemStack kiefPipe = PackingRecipe.INSTANCE.assemble(CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.WOODEN_PIPE),
                BudTier.stamp(new ItemStack(ModItems.HASHISH), BudTier.TOP_SHELF))), registries);
        context.assertValueEqual(kiefPipe.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).entries().getFirst().tier(),
                BudTier.TOP_SHELF, "a pipe of Top Shelf hashish");
        ItemStack moonRock = craft(world, CraftingInput.of(3, 1, List.of(bud(ModItems.INDICA_BUDS, BudTier.EPIC),
                BudTier.stamp(new ItemStack(ModItems.ROSIN), BudTier.EPIC), BudTier.stamp(new ItemStack(ModItems.HASHISH), BudTier.TOP_SHELF))));
        SmokeContents rock = moonRock.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        context.assertTrue(moonRock.is(ModItems.MOON_ROCK) && rock.entries().size() == 2
                        && rock.entries().get(0).tier() == BudTier.EPIC && rock.entries().get(1).tier() == BudTier.TOP_SHELF,
                "a moon rock of an Epic bud, Epic rosin and Top Shelf hash carries " + rock);
        context.assertTrue(!moonRock.has(ModComponents.BUD_TIER) && moonRock.getRarity() == Rarity.COMMON,
                "a moon rock carries a tier of its own: " + moonRock);

        // Written down, a Dank load is what 2.1 wrote; a tiered one reads back.
        Tag dank = SmokeContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
                SmokeContents.of(kush, 2)).getOrThrow();
        context.assertTrue(!dank.toString().contains("tier"), "a Dank load wrote its tier: " + dank);
        Tag epic = SmokeContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), strongest).getOrThrow();
        context.assertValueEqual(SmokeContents.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), epic).getOrThrow(),
                strongest, "an Epic load read back");
        context.succeed();
    }

    /**
     * The tier is on the bud with its rarity: Dank writes nothing, so an untiered bud is Dank and stacks
     * with a Dank one; Top Shelf is rare (aqua), Epic epic (light purple); the tooltip names it after the
     * potency and before the terpene. At the oven a tier is nothing: butter never knows.
     */
    public static void theTierIsOnTheBudAndNotInButter(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ItemStack plain = new ItemStack(ModItems.INDICA_BUDS);
        context.assertValueEqual(BudTier.of(plain), BudTier.DANK, "an untiered bud's tier");
        ItemStack dank = bud(ModItems.INDICA_BUDS, BudTier.DANK);
        context.assertTrue(ItemStack.isSameItemSameComponents(plain, dank) && dank.getComponentsPatch().isEmpty(),
                "an explicit Dank bud does not stack with an old one: " + dank.getComponentsPatch());
        context.assertTrue(ItemStack.isSameItemSameComponents(plain,
                        BudTier.stamp(bud(ModItems.INDICA_BUDS, BudTier.EPIC), BudTier.DANK))
                        && BudTier.stamp(bud(ModItems.INDICA_BUDS, BudTier.EPIC), BudTier.DANK).getComponentsPatch().isEmpty(),
                "an Epic bud stamped back to Dank keeps a patch");
        context.assertValueEqual(bud(ModItems.INDICA_BUDS, BudTier.MIDS).getRarity(), Rarity.COMMON, "a Mids bud's rarity");
        context.assertValueEqual(bud(ModItems.INDICA_BUDS, BudTier.TOP_SHELF).getRarity(), Rarity.RARE, "a Top Shelf bud's rarity");
        context.assertValueEqual(bud(ModItems.INDICA_BUDS, BudTier.EPIC).getRarity(), Rarity.EPIC, "an Epic bud's rarity");
        context.assertValueEqual(bud(ModItems.INDICA_BUDS, BudTier.TOP_SHELF).getStyledHoverName().getStyle().getColor().getValue(),
                0x55FFFF, "a Top Shelf bud's name colour");
        context.assertValueEqual(bud(ModItems.INDICA_BUDS, BudTier.EPIC).getStyledHoverName().getStyle().getColor().getValue(),
                0xFF55FF, "an Epic bud's name colour");
        context.assertTrue(!ItemStack.isSameItemSameComponents(bud(ModItems.INDICA_BUDS, BudTier.MIDS), plain),
                "a Mids bud stacks with a Dank one");

        for (BudTier tier : BudTier.values()) {
            List<String> lines = new ArrayList<>();
            for (Component line : bud(ModItems.INDICA_BUDS, tier).getTooltipLines(Item.TooltipContext.of(world), null, TooltipFlag.NORMAL)) {
                lines.add(line.getString());
            }
            String tierLine = Component.translatable(tier.getTranslationKey()).getString();
            String potency = Component.translatable("hempdustry.potency.mid").getString();
            String terpene = Component.translatable("hempdustry.terpene.linalool").getString();
            int at = lines.indexOf(tierLine);
            context.assertTrue(at > lines.indexOf(potency) && lines.indexOf(potency) > 0 && at < lines.indexOf(terpene),
                    "a " + tier + " bud's tooltip " + lines + " does not read potency, tier, terpene");
        }
        List<String> kief = new ArrayList<>();
        for (Component line : BudTier.stamp(new ItemStack(ModItems.KIEF), BudTier.TOP_SHELF)
                .getTooltipLines(Item.TooltipContext.of(world), null, TooltipFlag.NORMAL)) {
            kief.add(line.getString());
        }
        context.assertTrue(kief.contains(Component.translatable("hempdustry.tier.top_shelf").getString()),
                "Top Shelf kief's tooltip names no tier: " + kief);

        // The oven: an Epic bud bakes into exactly what a Dank one does.
        for (BudTier tier : BudTier.values()) {
            ItemStack baked = DecarboxylatorBlockEntity.resultFor(world, bud(ModItems.INDICA_BUDS, tier));
            context.assertTrue(ItemStack.matches(baked, DecarboxylatorBlockEntity.resultFor(world, plain)),
                    "a " + tier + " bud baked into " + baked + ", not what an untiered one does");
        }
        // The furnace too: an Epic bud chars into the scorched hemp an untiered one does.
        net.minecraft.world.item.crafting.SingleRecipeInput charred =
                new net.minecraft.world.item.crafting.SingleRecipeInput(bud(ModItems.INDICA_BUDS, BudTier.EPIC));
        ItemStack scorched = world.recipeAccess().getRecipeFor(RecipeType.SMELTING, charred, world)
                .map(entry -> entry.value().assemble(charred)).orElse(ItemStack.EMPTY);
        context.assertTrue(scorched.is(ModItems.SCORCHED_HEMP) && scorched.getComponentsPatch().isEmpty(),
                "an Epic bud charred into " + scorched + ", not the scorched hemp an untiered one gives");
        // A jar of mixed tiers: one strain's buds, whatever their tier, are one share of the stash.
        StashContents.Mutable mixed = StashContents.EMPTY.mutable();
        mixed.insert(bud(ModItems.INDICA_BUDS, BudTier.EPIC).copyWithCount(10), 64);
        mixed.insert(bud(ModItems.INDICA_BUDS, BudTier.MIDS).copyWithCount(10), 64);
        mixed.insert(new ItemStack(ModItems.INDICA_BUDS, 10), 64);
        StashContents stash = mixed.toImmutable();
        StashPieces.Colours pieces = StashPieces.colours(stash, 64, world.registryAccess());
        int colour = pieces.pistils()[0];
        for (int i = 0; i < pieces.pistils().length; i++) {
            context.assertValueEqual(pieces.pistils()[i], colour, "a jar of one strain's buds on three tiers, piece " + i);
        }
        context.succeed();
    }

    /**
     * Kief and hash carry their batch's tier: the Sifting Box averages what goes in by the levels each
     * adds (a leaf as Mids), the Press keeps it, a bar takes nine of one tier and gives it back, and an
     * untiered piece reads Dank.
     */
    public static void kiefAndHashCarryTheirBatchesTier(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        context.assertValueEqual(SiftingBoxBlock.averageTier(3 * 3 + 3 * 1, 6), BudTier.TOP_SHELF,
                "three Epic levels and three Dank ones");
        context.assertValueEqual(SiftingBoxBlock.averageTier(3 + 2, 2), BudTier.TOP_SHELF,
                "an Epic level and a Top Shelf one: a half rounds down");
        context.assertValueEqual(SiftingBoxBlock.averageTier(6 * 3, 7), BudTier.EPIC, "six Epic levels and a Mids one");
        context.assertValueEqual(SiftingBoxBlock.tierOf(new ItemStack(ModItems.HEMP_LEAF)), BudTier.MIDS, "a leaf going in");
        context.assertValueEqual(SiftingBoxBlock.tierOf(new ItemStack(ModItems.KIEF)), BudTier.DANK, "an untiered kief going in");

        // Three Epic and four Mids are Dank (9 over 7 levels) in either order: the box keeps a sum, which
        // the order the pieces come in cannot move. Seven Epic buds sift Epic kief.
        ServerPlayer player = Players.survival(context);
        for (boolean epicFirst : new boolean[]{true, false}) {
            context.setBlock(BOX, ModBlocks.SIFTING_BOX);
            for (int i = 0; i < 7; i++) {
                boolean epic = epicFirst ? i < 3 : i >= 4;
                Players.click(context, player, BOX, bud(ModItems.INDICA_BUDS, epic ? BudTier.EPIC : BudTier.MIDS));
            }
            context.assertValueEqual(boxTier(context, BOX), BudTier.DANK,
                    "three Epic buds and four Mids, " + (epicFirst ? "the Epic first" : "the Mids first"));
        }
        context.setBlock(BOX, ModBlocks.SIFTING_BOX);
        for (int i = 0; i < 7; i++) {
            Players.click(context, player, BOX, bud(ModItems.INDICA_BUDS, BudTier.EPIC));
        }
        context.assertValueEqual(boxTier(context, BOX), BudTier.EPIC, "a batch of seven Epic buds");
        // A ready box broken hands its batch over with the tier, and the box itself plain.
        BlockPos spare = context.absolutePos(BAR.east());
        world.setBlock(spare, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL),
                Block.UPDATE_CLIENTS);
        SiftingBoxBlockEntity.of(world, spare).add(Chemotype.THC_DOMINANT, BudTier.TOP_SHELF, SiftingBoxBlock.FULL_LEVEL);
        List<ItemStack> fromBox = Block.getDrops(world.getBlockState(spare), world, spare, world.getBlockEntity(spare));
        context.assertTrue(fromBox.stream().anyMatch(stack -> stack.is(ModItems.KIEF) && BudTier.of(stack) == BudTier.TOP_SHELF)
                        && fromBox.stream().filter(stack -> stack.is(ModBlocks.SIFTING_BOX.asItem()))
                        .allMatch(stack -> stack.getComponentsPatch().isEmpty()),
                "a ready Top Shelf box broke into " + fromBox);
        world.setBlock(spare, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

        // The yield takes it, once the batch has settled.
        context.runAfterDelay(30, () -> {
            Players.click(context, player, BOX, ItemStack.EMPTY);
            List<ItemStack> out = new ArrayList<>();
            for (var item : world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(context.absolutePos(BOX)).inflate(2))) {
                out.add(item.getItem().copy());
                item.discard();
            }
            context.assertTrue(out.size() == 1 && out.getFirst().is(ModItems.KIEF) && BudTier.of(out.getFirst()) == BudTier.EPIC,
                    "the batch gave " + out);

            // The Press keeps the tier.
            ItemStack pressed = HempPressBlockEntity.resultFor(world, BudTier.stamp(new ItemStack(ModItems.KIEF), BudTier.TOP_SHELF));
            context.assertTrue(pressed.is(ModItems.HASHISH) && BudTier.of(pressed) == BudTier.TOP_SHELF,
                    "Top Shelf kief pressed into " + pressed);
            ItemStack rosin = HempPressBlockEntity.resultFor(world, BudTier.stamp(new ItemStack(ModItems.FILTERED_HASHISH), BudTier.EPIC));
            context.assertTrue(rosin.is(ModItems.ROSIN) && BudTier.of(rosin) == BudTier.EPIC
                            && rosin.getRarity() == Rarity.EPIC, "Epic filtered hashish pressed into " + rosin);

            // A bar takes nine of one tier, keeps it placed and cut, and refuses two.
            List<ItemStack> nine = new ArrayList<>(Collections.nCopies(9, BudTier.stamp(new ItemStack(ModItems.HASHISH), BudTier.TOP_SHELF)));
            ItemStack bar = craft(world, CraftingInput.of(3, 3, nine));
            context.assertTrue(bar.is(ModBlocks.HASHISH_BAR.asItem()) && TierBlocks.ofBlockItem(bar) == BudTier.TOP_SHELF
                    && bar.getRarity() == Rarity.RARE, "nine Top Shelf hashish made " + bar);
            nine.set(4, new ItemStack(ModItems.HASHISH));
            context.assertTrue(craft(world, CraftingInput.of(3, 3, nine)).isEmpty(), "eight Top Shelf hashish and a Dank one made a bar");
            List<ItemStack> plainNine = new ArrayList<>(Collections.nCopies(9, new ItemStack(ModItems.HASHISH)));
            ItemStack plainBar = craft(world, CraftingInput.of(3, 3, plainNine));
            context.assertTrue(plainBar.is(ModBlocks.HASHISH_BAR.asItem()) && plainBar.getComponentsPatch().isEmpty(),
                    "nine plain hashish made a bar unlike 2.1's: " + plainBar.getComponentsPatch());

            BlockPos at = context.absolutePos(BAR);
            world.setBlock(at, ModBlocks.HASHISH_BAR.defaultBlockState().setValue(TierBlocks.TIER, BudTier.TOP_SHELF)
                    .setValue(ChemotypeBlocks.CHEMOTYPE, Chemotype.THC_DOMINANT), Block.UPDATE_CLIENTS);
            List<ItemStack> broken = Block.getDrops(world.getBlockState(at), world, at, null);
            context.assertTrue(broken.size() == 1 && TierBlocks.ofBlockItem(broken.getFirst()) == BudTier.TOP_SHELF,
                    "a placed Top Shelf bar broke into " + broken);
            Players.click(context, player, BAR, new ItemStack(Items.IRON_SWORD));
            List<ItemStack> cut = new ArrayList<>();
            for (var item : world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(at).inflate(2))) {
                cut.add(item.getItem().copy());
                item.discard();
            }
            context.assertTrue(cut.size() == 1 && cut.getFirst().is(ModItems.HASHISH) && BudTier.of(cut.getFirst()) == BudTier.TOP_SHELF,
                    "a cut off a Top Shelf bar gave " + cut);
            context.succeed();
        });
    }

    /**
     * A box a 2.1 world left part-filled has levels but no record of them, and they sift what 2.1 sifted,
     * THC-dominant and Dank: five such levels and two Epic buds are Top Shelf (11 over 7), never the
     * Mids a missing sum would read; a ready one breaks into plain kief, the 2.1 stack.
     */
    public static void aBoxFromA21WorldSiftsAsItDidThere(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = Players.survival(context);
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.LEVEL, 5));
        context.assertValueEqual(boxTier(context, BOX), BudTier.DANK, "five levels a 2.1 world left");
        Players.click(context, player, BOX, bud(ModItems.INDICA_BUDS, BudTier.EPIC));
        Players.click(context, player, BOX, bud(ModItems.INDICA_BUDS, BudTier.EPIC));
        BlockPos box = context.absolutePos(BOX);
        context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), SiftingBoxBlock.FULL_LEVEL,
                "two buds on five levels");
        context.assertValueEqual(boxTier(context, BOX), BudTier.TOP_SHELF, "five 2.1 levels and two Epic buds");
        context.assertValueEqual(SiftingBoxBlock.chemotypeOf(world, box, context.getBlockState(BOX)), Chemotype.THC_DOMINANT,
                "five 2.1 levels and two Purple Kush buds");

        BlockPos spare = context.absolutePos(BAR.east());
        world.setBlock(spare, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL),
                Block.UPDATE_CLIENTS);
        List<ItemStack> fromBox = Block.getDrops(world.getBlockState(spare), world, spare, world.getBlockEntity(spare));
        context.assertTrue(fromBox.stream().anyMatch(stack -> stack.is(ModItems.KIEF) && stack.getComponentsPatch().isEmpty()),
                "a ready 2.1 box broke into " + fromBox);
        context.succeed();
    }

    // ----- helpers -----

    private static BudTier boxTier(GameTestHelper context, BlockPos box) {
        return SiftingBoxBlock.tierOf(context.getLevel(), context.absolutePos(box), context.getBlockState(box));
    }

    private static Holder<Strain> strain(HolderLookup.Provider registries, ResourceKey<Strain> key) {
        return Strain.registry(registries).getOrThrow(key);
    }

    private static SmokeContents tiered(Holder<Strain> strain, int count, BudTier tier) {
        return SmokeContents.of(strain, count).withTiers(java.util.Optional.of(tier), java.util.Optional.of(tier));
    }

    private static ItemStack bud(Item item, BudTier tier) {
        return BudTier.stamp(new ItemStack(item), tier);
    }

    /** How long {@code effect} lasts on a hit of {@code contents} from a device of {@code device} ticks. */
    private static int duration(SmokeContents contents, int device, Holder<MobEffect> effect) {
        // A generator whose every draw is zero: every chance lands.
        java.util.random.RandomGenerator lands = () -> 0L;
        List<MobEffectInstance> all = new ArrayList<>(contents.effects(device, false, lands));
        all.addAll(contents.effects(device, true, lands));
        return all.stream().filter(instance -> instance.getEffect().equals(effect)).mapToInt(MobEffectInstance::getDuration)
                .max().orElse(-1);
    }

    private static BlockState ripe(Block crop, int age) {
        return ((net.minecraft.world.level.block.CropBlock) crop).getStateForAge(age);
    }

    private static PlantRecordBlockEntity record(ServerLevel world, BlockPos pos) {
        return (PlantRecordBlockEntity) world.getBlockEntity(pos);
    }

    private static List<ItemStack> drops(ServerLevel world, BlockPos pos, BlockState state,
                                         net.minecraft.world.level.block.entity.BlockEntity entity, ItemStack tool,
                                         Entity breaker, long seed) {
        LootTable table = world.getServer().reloadableRegistries().getLootTable(state.getBlock().getLootTable().orElseThrow());
        LootParams.Builder params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, breaker)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity);
        return table.getRandomItems(params.create(LootContextParamSets.BLOCK), seed);
    }

    private static ItemStack craft(ServerLevel world, CraftingInput input) {
        return world.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, world)
                .map(entry -> entry.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }
}

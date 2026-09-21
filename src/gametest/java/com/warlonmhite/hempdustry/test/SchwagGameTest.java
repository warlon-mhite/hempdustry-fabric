package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Schwag: a ripe plant's bud gone wrong, the poisonous potato moved into the bud system.
 *
 * <p>Every rule here fails in the quiet direction. A conversion that turned into an extra drop is
 * free buds; odds that ignore Fortune or stress look like odds; a poison that always lands, or never
 * does, is just a strain. So the odds are asserted as <b>numbers</b>, over thousands of rolls on
 * seeds drawn from a seeded {@link Random} (consecutive seeds leave the first draws correlated).
 */
public final class SchwagGameTest {
    private static final BlockPos CROP = new BlockPos(1, 2, 1);
    private static final long SEED_SOURCE = 0x5C4A6L;
    private static final int PLANTS = 2000;

    /**
     * A healthy ripe plant loses <b>one</b> bud to schwag one plant in fifty, never more than one, and
     * never at all under any level of Fortune — on all three crops' real tables. An unripe plant and
     * an upper half never give any.
     */
    public static void healthyPlantsSpoilOneBudInFifty(TestContext context) {
        ServerWorld world = context.getWorld();
        for (BlockState ripe : ripeLowers()) {
            String crop = ripe.getBlock().getTranslationKey();
            Random seeds = new Random(SEED_SOURCE);
            int spoiled = 0;
            int fortuneSpoiled = 0;
            for (int i = 0; i < PLANTS; i++) {
                long seed = seeds.nextLong();
                int[] got = harvest(world, context, ripe, seed, 0);
                context.assertTrue(got[1] <= 1, crop + ": a healthy plant gave " + got[1]
                        + " schwag — it risks one bad bud, not a spoiled harvest");
                spoiled += got[1];
                fortuneSpoiled += harvest(world, context, ripe, seed, 1)[1];
            }
            // 2% of 2000 is 40, with a standard deviation near 6.3.
            context.assertTrue(spoiled >= 20 && spoiled <= 65, crop + ": " + spoiled + " of " + PLANTS
                    + " healthy plants gave schwag, expected about 40 (2%)");
            context.assertEquals(fortuneSpoiled, 0, crop + ": Fortune I left " + fortuneSpoiled
                    + " schwag on healthy plants — any Fortune clears them");
        }

        BlockState indica = ripeLowers().get(0);
        Random seeds = new Random(SEED_SOURCE);
        for (int i = 0; i < 200; i++) {
            long seed = seeds.nextLong();
            context.assertEquals(harvest(world, context, indica.with(IndicaCropBlock.AGE, 0), seed, 0)[1], 0,
                    "an unripe plant gave schwag");
            int[] upper = harvest(world, context, indica.with(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER), seed, 0);
            context.assertTrue(upper[0] == 0 && upper[1] == 0,
                    "the upper half of a plant paid out — a two-block plant would pay twice");
        }
        context.complete();
    }

    /**
     * Every hemp crop's loot table passes vanilla's own validation. Its {@code alternatives} check
     * reads only the chain's own children's conditions, so a spoiling wrapper whose conditions sat on
     * the bud inside it made every later alternative "Unreachable entry!" — three warnings at each
     * world load, and nothing any harvest could show.
     */
    public static void cropLootTablesValidateClean(TestContext context) {
        ServerWorld world = context.getWorld();
        for (BlockState ripe : ripeLowers()) {
            LootTable table = world.getServer().getReloadableRegistries()
                    .getLootTable(ripe.getBlock().getLootTableKey().orElseThrow());
            ErrorReporter.Impl errors = new ErrorReporter.Impl();
            table.validate(new LootTableReporter(errors, LootContextTypes.BLOCK));
            context.assertTrue(errors.isEmpty(), ripe.getBlock().getTranslationKey()
                    + "'s loot table does not validate: " + errors.getErrorsAsString());
        }
        context.complete();
    }

    /**
     * A stressed plant loses each bud on its own: 25%, 20%, 15%, 10% by Fortune level. And it is a
     * <b>conversion</b>: on every seed, a stressed plant's buds and schwag add up to exactly one fewer
     * than the healthy plant's buds and schwag — the stressed bud it never grew, and nothing more.
     */
    public static void stressedBudsSpoilOneByOne(TestContext context) {
        ServerWorld world = context.getWorld();
        float[] expected = {0.25F, 0.20F, 0.15F, 0.10F};
        for (BlockState ripe : ripeLowers()) {
            String crop = ripe.getBlock().getTranslationKey();
            BlockState stressed = ripe.with(GrowLight.PROPERTY, GrowLight.STRESSED);
            for (int level = 0; level < expected.length; level++) {
                Random seeds = new Random(SEED_SOURCE);
                int buds = 0;
                int schwag = 0;
                for (int i = 0; i < PLANTS; i++) {
                    long seed = seeds.nextLong();
                    int[] sick = harvest(world, context, stressed, seed, level);
                    int[] well = harvest(world, context, ripe, seed, level);
                    context.assertEquals(sick[0] + sick[1], well[0] + well[1] - 1, crop + " at Fortune "
                            + level + ": a stressed plant's buds and schwag came to " + (sick[0] + sick[1])
                            + " against a healthy one's " + (well[0] + well[1]) + " — schwag has to replace a bud");
                    buds += sick[0];
                    schwag += sick[1];
                }
                float share = schwag / (float) (buds + schwag);
                context.assertTrue(Math.abs(share - expected[level]) < 0.035F, crop + " at Fortune " + level
                        + ": " + schwag + " of " + (buds + schwag) + " stressed buds spoiled (" + share
                        + "), expected " + expected[level]);
            }
        }
        context.complete();
    }

    /**
     * One hit of schwag: Poison on about 60% of hits, the potato's odds, for a seventh of the
     * device's duration (100 ticks from a pipe), scaling with the dose but never past Poison II —
     * and Hunger always, scaling with the dose as every strain's does.
     */
    public static void schwagSmokesAGamble(TestContext context) {
        Strain schwag = strain(context.getWorld()).value();
        Random random = new Random(SEED_SOURCE);
        int poisoned = 0;
        int hits = 2000;
        for (int i = 0; i < hits; i++) {
            List<StatusEffectInstance> effects = schwag.effects(1, 700, false, random);
            StatusEffectInstance poison = find(effects, StatusEffects.POISON);
            if (poison != null) {
                poisoned++;
                context.assertEquals(poison.getDuration(), 100, "schwag's Poison from a pipe lasts "
                        + poison.getDuration() + " ticks, not the potato's 100");
                context.assertEquals(poison.getAmplifier(), 0, "one schwag poisons above level I");
            }
            context.assertTrue(find(effects, StatusEffects.HUNGER) != null, "a hit of schwag skipped Hunger");
        }
        // 60% of 2000 is 1200, with a standard deviation near 22.
        context.assertTrue(poisoned >= 1100 && poisoned <= 1300, poisoned + " of " + hits
                + " hits poisoned, expected about 1200 (60%)");

        StatusEffectInstance bong = null;
        for (int i = 0; i < 50 && bong == null; i++) {
            bong = find(schwag.effects(3, 1000, false, random), StatusEffects.POISON);
        }
        context.assertTrue(bong != null && bong.getAmplifier() == 1,
                "a bong of three schwag does not poison at level II — the dose has to scale it, and "
                        + "max_amplifier has to stop it at vanilla's ceiling");
        context.assertEquals(2, find(schwag.effects(3, 1000, false, random), StatusEffects.HUNGER).getAmplifier(),
                "a bong of three schwag is Hunger III, as every strain's is");
        context.assertTrue(schwag.effects(1, 700, true, random).isEmpty(), "schwag holds an effect back to the exhale");
        context.assertEquals(schwag.greenOutFactor(), 0.0F, "schwag can green you out");
        context.complete();
    }

    /**
     * What schwag is worth outside the bowl: half a bud in the oven, one scorched hemp in a furnace
     * like any bud, a spliff on its own at every dose, a bud's rate in a composter — and nothing
     * at the screen. Plus the shipwreck chest, measured off the loaded table.
     */
    public static void schwagIsHalfABud(TestContext context) {
        ServerWorld world = context.getWorld();
        ItemStack oven = world.getRecipeManager()
                .getFirstMatch(ModRecipes.DECARBOXYLATING_TYPE, new SingleStackRecipeInput(new ItemStack(ModItems.SCHWAG)), world)
                .map(entry -> entry.value().craft(new SingleStackRecipeInput(new ItemStack(ModItems.SCHWAG)),
                        world.getRegistryManager()))
                .orElse(ItemStack.EMPTY);
        context.assertTrue(oven.isOf(ModItems.DECARBOXYLATED_HEMP) && oven.getCount() == 2,
                "the oven made " + oven + " of a schwag, not 2 decarboxylated hemp");

        ItemStack furnace = world.getRecipeManager()
                .getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(new ItemStack(ModItems.SCHWAG)), world)
                .map(entry -> entry.value().craft(new SingleStackRecipeInput(new ItemStack(ModItems.SCHWAG)),
                        world.getRegistryManager()))
                .orElse(ItemStack.EMPTY);
        context.assertTrue(furnace.isOf(ModItems.SCORCHED_HEMP) && furnace.getCount() == 1,
                "a furnace made " + furnace + " of a schwag, not one scorched hemp");

        RegistryEntry<Strain> schwag = strain(world);
        for (int dose = 1; dose <= ModItems.SPLIFF_MAX_DOSE; dose++) {
            List<ItemStack> slots = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                slots.add(i < dose ? new ItemStack(ModItems.SCHWAG) : i >= 3 && i < 3 + dose
                        ? new ItemStack(Items.PAPER) : ItemStack.EMPTY);
            }
            CraftingRecipeInput grid = CraftingRecipeInput.create(3, 3, slots);
            int wanted = dose;
            ItemStack spliff = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, grid, world)
                    .map(entry -> entry.value().craft(grid, world.getRegistryManager()))
                    .orElse(ItemStack.EMPTY);
            SmokeContents contents = spliff.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
            context.assertTrue(spliff.isOf(ModItems.SPLIFF) && contents.dose() == wanted
                            && contents.primaryStrain() != null && contents.primaryStrain().matchesKey(ModStrains.SCHWAG),
                    dose + " schwag over " + dose + " paper did not roll a dose-" + dose + " schwag spliff");
        }
        context.assertTrue(ModStrains.isPlantMatter(schwag), "schwag is not plant matter");
        context.assertTrue(schwag.value().flower().isEmpty(), "schwag has a plant of its own");

        context.assertEquals(ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(ModItems.SCHWAG),
                ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(ModItems.INDICA_BUDS),
                "schwag does not compost at a bud's rate");
        ItemStack stack = new ItemStack(ModItems.SCHWAG);
        context.assertFalse(stack.isIn(ModTags.Items.SIFTABLE_FLOWER) || stack.isIn(ModTags.Items.SIFTABLE_RESINOUS)
                || stack.isIn(ModTags.Items.SIFTABLE_TRIM), "schwag goes through the screen");

        LootTable wreck = world.getServer().getReloadableRegistries().getLootTable(LootTables.SHIPWRECK_SUPPLY_CHEST);
        LootWorldContext params = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(context.getAbsolutePos(CROP)))
                .build(LootContextTypes.CHEST);
        Random seeds = new Random(SEED_SOURCE);
        int chests = 1000;
        int withSchwag = 0;
        for (int i = 0; i < chests; i++) {
            if (wreck.generateLoot(params, seeds.nextLong()).stream().anyMatch(s -> s.isOf(ModItems.SCHWAG))) {
                withSchwag++;
            }
        }
        // 20% of 1000 is 200, with a standard deviation near 13.
        context.assertTrue(withSchwag >= 140 && withSchwag <= 260, withSchwag + " of " + chests
                + " shipwreck supply chests held schwag, expected about 200 (20%)");
        context.complete();
    }

    // ----- helpers -----

    /** A ripe, unworked, naturally lit LOWER of each crop, in the order indica, Beldía, sativa. */
    private static List<BlockState> ripeLowers() {
        return List.of(
                Defoliation.unworked(ModBlocks.INDICA_CROP.getDefaultState()
                        .with(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                        .with(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE)),
                Defoliation.unworked(ModBlocks.BELDIA_CROP.getDefaultState()
                        .with(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                        .with(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE)),
                Defoliation.unworked(ModBlocks.SATIVA_CROP.getDefaultState()
                        .with(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                        .with(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE)));
    }

    /** {buds, schwag} from one roll of the plant's own table, harvested with Fortune {@code level}. */
    private static int[] harvest(ServerWorld world, TestContext context, BlockState state, long seed, int level) {
        LootTable table = world.getServer().getReloadableRegistries()
                .getLootTable(state.getBlock().getLootTableKey().orElseThrow());
        ItemStack tool = new ItemStack(Items.DIAMOND_HOE);
        if (level > 0) {
            RegistryEntry<Enchantment> fortune = world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
            tool.addEnchantment(fortune, level);
        }
        LootWorldContext params = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(context.getAbsolutePos(CROP)))
                .add(LootContextParameters.BLOCK_STATE, state)
                .add(LootContextParameters.TOOL, tool)
                .build(LootContextTypes.BLOCK);
        Item buds = state.isOf(ModBlocks.INDICA_CROP) ? ModItems.INDICA_BUDS
                : state.isOf(ModBlocks.BELDIA_CROP) ? ModItems.BELDIA_BUDS : ModItems.SATIVA_BUDS;
        int[] counts = new int[2];
        for (ItemStack stack : table.generateLoot(params, seed)) {
            if (stack.isOf(buds)) {
                counts[0] += stack.getCount();
            } else if (stack.isOf(ModItems.SCHWAG)) {
                counts[1] += stack.getCount();
            }
        }
        return counts;
    }

    private static RegistryEntry<Strain> strain(ServerWorld world) {
        return world.getRegistryManager().getOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.SCHWAG);
    }

    private static StatusEffectInstance find(List<StatusEffectInstance> effects,
                                             RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        for (StatusEffectInstance instance : effects) {
            if (instance.getEffectType().equals(effect)) {
                return instance;
            }
        }
        return null;
    }
}

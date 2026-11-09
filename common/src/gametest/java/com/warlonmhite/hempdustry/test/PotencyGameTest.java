package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.EnderHazeCropBlock;
import com.warlonmhite.hempdustry.block.custom.HempLeavesBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Potency;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Potency bands: each plant rolls one from its strain's odds and keeps it on its root's record; its
 * buds carry it, its leaves a high mark; the oven pays it as a count and the bowl weighs it as THC; a
 * craft never mixes two. Absent is mid everywhere, so nothing 2.1 made changes.
 */
public final class PotencyGameTest {

    private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos ROOT = FLOOR.above();

    /** The bands every strain the mod ships rolls (low / mid / high / extra-high), all at base 4. */
    private static final Map<ResourceKey<Strain>, Potency> SHEETS = new LinkedHashMap<>();

    static {
        SHEETS.put(ModStrains.INDICA, Potency.odds(25, 50, 25, 0));
        SHEETS.put(ModStrains.SATIVA, Potency.odds(25, 50, 25, 0));
        SHEETS.put(ModStrains.BELDIA, Potency.odds(30, 50, 20, 0));
        SHEETS.put(ModStrains.WARPED_KUSH, Potency.odds(20, 50, 25, 5));
        SHEETS.put(ModStrains.CRIMSON, Potency.odds(20, 50, 25, 5));
        SHEETS.put(ModStrains.ENDER_HAZE, Potency.odds(28, 50, 20, 2));
        SHEETS.put(ModStrains.RUDERALIS, Potency.odds(50, 40, 10, 0));
    }

    /**
     * Every crop that grows buds keeps a record on its root and nowhere else; it rolls its band as it is
     * planted, or as it joins a world without one; the band lives through growth and a save, and a root a
     * harvester sets back to age 0 is a new plant that rolls again.
     */
    public static void aPlantKeepsItsRecordOnItsRoot(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Map<Block, Predicate<BlockState>> roots = new LinkedHashMap<>();
        Predicate<BlockState> lower = state -> state.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.LOWER;
        roots.put(ModBlocks.INDICA_CROP, lower);
        roots.put(ModBlocks.BELDIA_CROP, lower);
        roots.put(ModBlocks.WARPED_KUSH_CROP, lower);
        roots.put(ModBlocks.CRIMSON_CROP, state -> state.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.UPPER);
        roots.put(ModBlocks.SATIVA_CROP, state -> state.getValue(SativaCropBlock.SEGMENT) == TriplePlantSegment.LOWER);
        roots.put(ModBlocks.ENDER_HAZE_CROP, state -> state.getValue(EnderHazeCropBlock.SEGMENT) == 0);
        roots.put(ModBlocks.RUDERALIS_CROP, state -> true);

        // Every plant strain's crop is among them: a strain added later is held to the same rule.
        Map<String, String> findings = new TreeMap<>();
        int strains = 0;
        for (Holder.Reference<Strain> strain : Strain.all(world.registryAccess())) {
            if (strain.value().wildFlower().isEmpty()) {
                continue;
            }
            strains++;
            Block crop = strain.value().seedItem().map(seed -> ((BlockItem) seed).getBlock()).orElseThrow();
            if (!roots.containsKey(crop)) {
                findings.put(strain.key().identifier().toString(), BuiltInRegistries.BLOCK.getKey(crop) + " keeps no record");
            }
        }
        Sweep.atLeast(context, "plant strains", strains, 7);
        for (Map.Entry<Block, Predicate<BlockState>> crop : roots.entrySet()) {
            String id = BuiltInRegistries.BLOCK.getKey(crop.getKey()).toString();
            for (BlockState state : crop.getKey().getStateDefinition().getPossibleStates()) {
                BlockEntity entity = ((EntityBlock) crop.getKey()).newBlockEntity(BlockPos.ZERO, state);
                boolean root = crop.getValue().test(state);
                if (root != entity instanceof PlantRecordBlockEntity) {
                    findings.put(id, (root ? "a root has no record: " : "a block that is not the root has one: ") + state);
                    break;
                }
            }
            if (!ModBlockEntities.PLANT_RECORD.isValid(crop.getKey().defaultBlockState())) {
                findings.put(id, "the record's type does not take this crop");
            }
        }
        Sweep.report(context, "Every crop that grows buds keeps a record on its root", findings, Map.of());
        context.assertTrue(!(ModBlocks.INDUSTRIAL_HEMP_CROP instanceof EntityBlock provider)
                        || provider.newBlockEntity(BlockPos.ZERO, ModBlocks.INDUSTRIAL_HEMP_CROP.defaultBlockState()) == null,
                "Industrial Hemp keeps a record, and grows no bud to give it to");

        // Planted, every crop's root has rolled its band already; a record that joins a world without one
        // (a plant sown before records existed, a generated one going live) rolls as it joins.
        BlockPos pos = context.absolutePos(ROOT);
        for (Map.Entry<Block, Predicate<BlockState>> crop : roots.entrySet()) {
            world.setBlock(pos, crop.getKey().defaultBlockState(), Block.UPDATE_CLIENTS);
            context.assertTrue(record(context, pos).rolledBand().isPresent(),
                    BuiltInRegistries.BLOCK.getKey(crop.getKey()) + " was planted with no band rolled");
        }
        PlantRecordBlockEntity joining = (PlantRecordBlockEntity) ((EntityBlock) ModBlocks.INDICA_CROP)
                .newBlockEntity(pos, ModBlocks.INDICA_CROP.defaultBlockState());
        context.assertTrue(joining.rolledBand().isEmpty(), "a record rolled before it joined any world");
        joining.setLevel(world);
        context.assertTrue(joining.rolledBand().isPresent(), "a record joined a world without rolling a band");

        // Grown a step, the band stays; set back to age 0 in place, the plant rolls again.
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 2), Block.UPDATE_CLIENTS);
        record(context, pos).setBand(PotencyBand.HIGH);
        world.setBlock(pos, ripe(ModBlocks.INDICA_CROP, 3), Block.UPDATE_CLIENTS);
        context.assertValueEqual(PotencyBand.HIGH, record(context, pos).rolledBand().orElse(null),
                "a growing plant's band");

        CompoundTag saved = record(context, pos).saveWithFullMetadata(world.registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(pos, world.getBlockState(pos), saved, world.registryAccess());
        context.assertTrue(loaded instanceof PlantRecordBlockEntity plant && plant.rolledBand().orElse(null) == PotencyBand.HIGH,
                "a high plant saved as " + saved + " and loaded as " + loaded);

        // Extra-high, which Purple Kush never rolls: only a new roll can take it away.
        record(context, pos).setBand(PotencyBand.EXTRA_HIGH);
        world.setBlock(pos, ((IndicaCropBlock) ModBlocks.INDICA_CROP).getStateForAge(0), Block.UPDATE_CLIENTS);
        PotencyBand replanted = record(context, pos).rolledBand().orElse(null);
        context.assertTrue(replanted != null && replanted != PotencyBand.EXTRA_HIGH,
                "a plant a harvester set back to age 0 is " + replanted + ": it kept its old band, or rolled none");
        context.succeed();
    }

    /** Each strain's sheet is the agreed one, and a plant rolls its band at those odds. */
    public static void aPlantRollsItsBandFromItsStrainsOdds(GameTestHelper context) {
        HolderLookup.Provider registries = context.getLevel().registryAccess();
        for (Map.Entry<ResourceKey<Strain>, Potency> sheet : SHEETS.entrySet()) {
            Potency potency = Strain.registry(registries).getOrThrow(sheet.getKey()).value().potency();
            context.assertValueEqual(sheet.getValue(), potency, sheet.getKey().identifier() + "'s potency sheet");
        }
        for (Holder.Reference<Strain> strain : Strain.all(registries)) {
            if (strain.value().wildFlower().isEmpty()) {
                continue;
            }
            Potency potency = strain.value().potency();
            int total = potency.low() + potency.mid() + potency.high() + potency.extraHigh();
            int rolls = 8000;
            int[] counts = new int[PotencyBand.values().length];
            RandomSource random = RandomSource.create(20261109L);
            for (int roll = 0; roll < rolls; roll++) {
                counts[potency.roll(random).ordinal()]++;
            }
            for (PotencyBand band : PotencyBand.values()) {
                double expected = (double) potency.weight(band) / total;
                double seen = (double) counts[band.ordinal()] / rolls;
                context.assertTrue(potency.weight(band) == 0 ? counts[band.ordinal()] == 0 : Math.abs(seen - expected) < 0.02,
                        strain.key().identifier() + " rolled " + band + " " + seen + " of the time, expected " + expected);
            }
        }
        context.succeed();
    }

    /**
     * A harvest hands the plant's band to its buds and its high mark to its leaves, takes no draw of the
     * loot's own (the same seed drops the same things with a record or without), and a plant broken by
     * its tip or trimmed while it grows reads the same record.
     */
    public static void theHarvestCarriesThePlantsBand(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockState ripe = ripe(ModBlocks.INDICA_CROP, IndicaCropBlock.MAX_AGE);
        world.setBlock(pos, ripe, Block.UPDATE_CLIENTS);
        PlantRecordBlockEntity record = record(context, pos);

        Random seeds = new Random(20261109L);
        for (int roll = 0; roll < 20; roll++) {
            long seed = seeds.nextLong();
            List<ItemStack> plain = drops(world, pos, ripe, null, seed);
            for (PotencyBand band : PotencyBand.values()) {
                record.setBand(band);
                List<ItemStack> banded = drops(world, pos, ripe, record, seed);
                context.assertValueEqual(plain.size(), banded.size(), "a " + band + " plant's stacks against a plain one's");
                for (int i = 0; i < banded.size(); i++) {
                    ItemStack got = banded.get(i);
                    context.assertTrue(got.getItem() == plain.get(i).getItem() && got.getCount() == plain.get(i).getCount(),
                            "a " + band + " plant dropped " + banded + " where the same seed drops " + plain);
                    PotencyBand expected = got.is(ModItems.INDICA_BUDS) ? (band == PotencyBand.MID ? null : band)
                            : got.is(ModItems.HEMP_LEAF) && band.marksLeaf() ? PotencyBand.HIGH : null;
                    context.assertTrue(java.util.Objects.equals(got.get(ModComponents.POTENCY_BAND), expected),
                            "the band on " + got + " from a " + band + " plant: expected " + expected);
                }
            }
        }

        // Broken by its top half, the plant's buds still carry its band.
        world.setBlock(pos.above(), ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
        record.setBand(PotencyBand.EXTRA_HIGH);
        ServerPlayer player = Players.survival(context);
        context.assertTrue(Players.breakAs(context, player, ROOT.above()), "the top half did not break");
        List<ItemStack> picked = pickUp(world, pos);
        context.assertTrue(picked.stream().anyMatch(stack -> stack.is(ModItems.INDICA_BUDS))
                        && picked.stream().filter(stack -> stack.is(ModItems.INDICA_BUDS))
                        .allMatch(stack -> PotencyBand.of(stack) == PotencyBand.EXTRA_HIGH),
                "an extra-high plant broken by its top half dropped " + picked);

        // A trim of a high plant hands over a marked leaf; of a low one, a plain leaf.
        for (PotencyBand band : new PotencyBand[]{PotencyBand.HIGH, PotencyBand.LOW}) {
            BlockState growing = ripe(ModBlocks.INDICA_CROP, Defoliation.EARLY_MIN_AGE);
            world.setBlock(pos, growing, Block.UPDATE_CLIENTS);
            record(context, pos).setBand(band);
            ItemStack shears = new ItemStack(Items.SHEARS);
            player.setItemInHand(InteractionHand.MAIN_HAND, shears);
            Defoliation.tryCut(world, pos, growing, Defoliation.EARLY_MIN_AGE, shears, player, InteractionHand.MAIN_HAND);
            List<ItemStack> leaves = pickUp(world, pos);
            context.assertTrue(leaves.size() == 1 && leaves.getFirst().is(ModItems.HEMP_LEAF)
                            && PotencyBand.of(leaves.getFirst()) == (band.marksLeaf() ? PotencyBand.HIGH : PotencyBand.MID),
                    "trimming a " + band + " plant gave " + leaves);
        }
        context.succeed();
    }

    /**
     * The band is a count at the oven (a leaf's mark too) and a weight in the bowl; a craft carries one
     * band and refuses two; a mid bud and an unmarked leaf block write nothing, so they stack with 2.1's.
     */
    public static void theBandMovesTheOvenTheBowlAndTheCraft(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        int[] baked = {3, 4, 5, 6};
        for (PotencyBand band : PotencyBand.values()) {
            ItemStack bud = PotencyBand.stamp(new ItemStack(ModItems.INDICA_BUDS), band);
            context.assertValueEqual(baked[band.ordinal()], DecarboxylatorBlockEntity.resultFor(world, bud).getCount(),
                    "a " + band + " Purple Kush bud's decarboxylated hemp");
        }
        context.assertValueEqual(4, DecarboxylatorBlockEntity.resultFor(world, new ItemStack(ModItems.RUDERALIS_BUDS)).getCount(),
                "a mid Ruderalis bud's decarboxylated hemp");
        context.assertValueEqual(1, DecarboxylatorBlockEntity.resultFor(world, new ItemStack(ModItems.HEMP_LEAF)).getCount(),
                "a leaf's decarboxylated hemp");
        context.assertValueEqual(2, DecarboxylatorBlockEntity.resultFor(world, high(ModItems.HEMP_LEAF, 1)).getCount(),
                "a high-potency leaf's decarboxylated hemp");

        Holder<Strain> kush = Strain.registry(registries).getOrThrow(ModStrains.INDICA);
        Holder<Strain> beldia = Strain.registry(registries).getOrThrow(ModStrains.BELDIA);
        context.assertValueEqual(3, SmokeContents.of(kush, 2, com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT,
                PotencyBand.EXTRA_HIGH).thcDose(), "an extra-high pipe of two's THC dose, a full green-out");
        context.assertValueEqual(2, SmokeContents.of(kush, 3, com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT,
                PotencyBand.LOW).thcDose(), "a low bong of three's THC dose");
        context.assertValueEqual(1, SmokeContents.of(kush, 1, com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT,
                PotencyBand.LOW).thcDose(), "a single low bud's THC dose, which never drops below one");
        context.assertValueEqual(3, SmokeContents.of(kush, 3).thcDose(), "a mid bong of three's THC dose");
        context.assertValueEqual(1, SmokeContents.of(beldia, 2, com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT,
                PotencyBand.EXTRA_HIGH).thcDose(), "an extra-high balanced pipe of two's THC dose");
        context.assertValueEqual(2, hunger(SmokeContents.of(kush, 2, com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT,
                PotencyBand.EXTRA_HIGH)), "an extra-high pipe of two's Hunger amplifier");
        context.assertValueEqual(1, hunger(SmokeContents.of(kush, 2)), "a mid pipe of two's Hunger amplifier");
        // An entry's dose has no cap of its own: a bong of three Extra-High Warped Kush is dose 4. The load's
        // dose, the green-out, stops at 3, a buff at maxBuffLevel, and only a cost feels the rest -- Hunger IV,
        // the price of the strongest bowl, as vanilla's Turtle Master pays for its Resistance with Slowness IV.
        SmokeContents strongest = SmokeContents.of(Strain.registry(registries).getOrThrow(ModStrains.WARPED_KUSH), 3,
                com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT, PotencyBand.EXTRA_HIGH);
        context.assertValueEqual(3, strongest.thcDose(), "an extra-high Warped Kush bong of three's green-out dose");
        context.assertValueEqual(3, hunger(strongest), "an extra-high Warped Kush bong of three's Hunger amplifier");
        context.assertValueEqual(1, amplifier(strongest, MobEffects.JUMP_BOOST),
                "an extra-high Warped Kush bong of three's Jump Boost amplifier, a buff held at II");

        // Written to a stack, a mid load is what 2.1 wrote; a high one keeps its band.
        Tag mid = SmokeContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
                SmokeContents.of(kush, 2)).getOrThrow();
        context.assertTrue(!mid.toString().contains("potency"), "a mid load wrote its band: " + mid);
        SmokeContents highLoad = SmokeContents.of(kush, 2, com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT, PotencyBand.HIGH);
        Tag high = SmokeContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), highLoad).getOrThrow();
        context.assertValueEqual(highLoad,
                SmokeContents.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), high).getOrThrow(), "a high load read back");

        // Packing and spliffs: buds of different bands load together, at their average.
        ItemStack packed = new PackingRecipe(CraftingBookCategory.MISC).assemble(CraftingInput.of(3, 1, List.of(new ItemStack(ModItems.BONG),
                high(ModItems.INDICA_BUDS, 1), high(ModItems.INDICA_BUDS, 1))), registries);
        context.assertValueEqual(highLoad, packed.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY),
                "a bong packed with two high buds");
        SmokeContents mixed = new PackingRecipe(CraftingBookCategory.MISC).assemble(CraftingInput.of(4, 1, List.of(new ItemStack(ModItems.BONG),
                low(), low(), new ItemStack(ModItems.INDICA_BUDS))), registries)
                .getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        context.assertValueEqual(SmokeContents.of(kush, 3, com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT, -2), mixed,
                "a bong packed with two low buds and a mid one");
        // 3 + 3 + 4 decarboxylated hemp is 10 of a mid bong's 12: a dose of two, where three mid buds are three.
        context.assertValueEqual(2, mixed.thcDose(), "two low buds and a mid one's THC dose");
        ItemStack spliff = craft(world, CraftingInput.of(2, 2, List.of(high(ModItems.INDICA_BUDS, 1), high(ModItems.INDICA_BUDS, 1),
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER))));
        context.assertValueEqual(highLoad, spliff.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY),
                "a spliff rolled from two high buds");
        ItemStack mixedSpliff = craft(world, CraftingInput.of(2, 2, List.of(high(ModItems.INDICA_BUDS, 1),
                new ItemStack(ModItems.INDICA_BUDS), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER))));
        context.assertValueEqual(SmokeContents.of(kush, 2, com.warlonmhite.hempdustry.strain.Chemotype.THC_DOMINANT, 1),
                mixedSpliff.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY),
                "a spliff rolled from a high and a mid bud");

        // Nine marked leaves make a marked block and come back out marked; eight and a plain one, nothing.
        List<ItemStack> nine = new ArrayList<>(Collections.nCopies(9, high(ModItems.HEMP_LEAF, 1)));
        ItemStack block = craft(world, CraftingInput.of(3, 3, nine));
        context.assertTrue(block.is(ModBlocks.HEMP_LEAVES.asItem()) && HempLeavesBlock.markOfBlockItem(block) == PotencyBand.HIGH,
                "nine high-potency leaves made " + block);
        ItemStack leaves = craft(world, CraftingInput.of(1, 1, List.of(block)));
        context.assertTrue(leaves.getCount() == 9 && PotencyBand.of(leaves) == PotencyBand.HIGH, "a marked block unpacked into " + leaves);
        nine.set(4, new ItemStack(ModItems.HEMP_LEAF));
        context.assertTrue(craft(world, CraftingInput.of(3, 3, nine)).isEmpty(), "eight marked leaves and a plain one made a block");

        BlockPos pos = context.absolutePos(ROOT);
        world.setBlock(pos, ModBlocks.HEMP_LEAVES.defaultBlockState().setValue(HempLeavesBlock.HIGH_POTENCY, true), Block.UPDATE_CLIENTS);
        List<ItemStack> marked = Block.getDrops(world.getBlockState(pos), world, pos, null);
        context.assertTrue(marked.size() == 1 && HempLeavesBlock.markOfBlockItem(marked.getFirst()) == PotencyBand.HIGH,
                "a placed marked block broke into " + marked);
        world.setBlock(pos, ModBlocks.HEMP_LEAVES.defaultBlockState(), Block.UPDATE_CLIENTS);
        List<ItemStack> plain = Block.getDrops(world.getBlockState(pos), world, pos, null);
        context.assertTrue(plain.size() == 1 && ItemStack.isSameItemSameComponents(plain.getFirst(), new ItemStack(ModBlocks.HEMP_LEAVES)),
                "a placed plain block broke into " + plain + ", which no longer stacks with 2.1's");
        context.assertTrue(!PotencyBand.stamp(new ItemStack(ModItems.INDICA_BUDS), PotencyBand.MID).has(ModComponents.POTENCY_BAND)
                        && !new ItemStack(ModBlocks.HEMP_LEAVES).has(DataComponents.BLOCK_STATE),
                "a mid bud or a plain block item carries a value 2.1's do not");
        context.succeed();
    }

    private static ItemStack high(Item item, int count) {
        return PotencyBand.stamp(new ItemStack(item, count), PotencyBand.HIGH);
    }

    private static ItemStack low() {
        return PotencyBand.stamp(new ItemStack(ModItems.INDICA_BUDS), PotencyBand.LOW);
    }

    private static int hunger(SmokeContents contents) {
        return amplifier(contents, MobEffects.HUNGER);
    }

    private static int amplifier(SmokeContents contents, Holder<MobEffect> wanted) {
        for (MobEffectInstance effect : contents.effects(700, false, () -> 0L)) {
            if (effect.getEffect() == wanted) {
                return effect.getAmplifier();
            }
        }
        return -1;
    }

    private static PlantRecordBlockEntity record(GameTestHelper context, BlockPos absolute) {
        BlockEntity entity = context.getLevel().getBlockEntity(absolute);
        context.assertTrue(entity instanceof PlantRecordBlockEntity, "no plant record at " + absolute + ": " + entity);
        return (PlantRecordBlockEntity) entity;
    }

    private static BlockState ripe(Block crop, int age) {
        return Defoliation.unworked(crop.defaultBlockState())
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(IndicaCropBlock.AGE, age);
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

    private static ItemStack craft(ServerLevel world, CraftingInput input) {
        return world.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, world)
                .map(entry -> entry.value().assemble(input, world.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    /** Every item lying around {@code pos}, taken out of the world. */
    private static List<ItemStack> pickUp(ServerLevel world, BlockPos pos) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))) {
            out.add(item.getItem().copy());
            item.discard();
        }
        return out;
    }
}

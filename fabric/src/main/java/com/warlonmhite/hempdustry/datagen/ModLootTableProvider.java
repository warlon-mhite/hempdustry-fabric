package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.GrowPotBlock;
import com.warlonmhite.hempdustry.block.custom.HydroTrayBlock;
import com.warlonmhite.hempdustry.block.custom.HashishBarBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.Mildew;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.loot.ModLootEntryTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ModLootTableProvider extends FabricBlockLootSubProvider {
    /**
     * Breaking a hashish bar hands back what is still in it: the whole bar if it is uncut, and the
     * pieces that have not been cut off yet if it is not.
     *
     * <p>Vanilla's cake drops nothing at all, which would be punishing here for no reason — the bar
     * is a stash, and a misplaced pickaxe should not delete a week of sifting. Nothing dupes either:
     * the counts are exactly what is left after the cuts already taken.
     *
     * <p>One pool per cut count, each with a mutually exclusive condition, rather than one pool of
     * alternatives. Same output, and each row reads as its own line.
     *
     * <p>Parameterised because the two bars want the identical table with a different piece in it —
     * which is the same reason they are the same block class with one method overridden.
     */
    /**
     * The box, and — when a batch is ready to take — the batch, as a full composter drops its bone
     * meal. Without it, breaking a ready box threw away nine pieces. What the batch is follows from
     * the state alone, exactly as {@code SiftingBoxBlock#collect} reads it: kief on the screen makes
     * filtered kief, a wash makes bubble hash, and a dry sift of plant matter makes kief. A box that is
     * still filling drops only itself, as a composter below full does.
     */
    private LootTable.Builder siftingBoxDrops() {
        return createSingleItemTable(ModBlocks.SIFTING_BOX).withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(siftingBox(StatePropertiesPredicate.Builder.properties()
                        .hasProperty(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL)))
                .add(batch(ModItems.FILTERED_KIEF, StatePropertiesPredicate.Builder.properties()
                        .hasProperty(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.KIEF))
                        .otherwise(batch(ModItems.BUBBLE_HASH, StatePropertiesPredicate.Builder.properties()
                                .hasProperty(SiftingBoxBlock.FILLED, true)))
                        .otherwise(LootItem.lootTableItem(ModItems.KIEF)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(SiftingBoxBlock.YIELD))))));
    }

    private static LootItemBlockStatePropertyCondition.Builder siftingBox(StatePropertiesPredicate.Builder state) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.SIFTING_BOX).setProperties(state);
    }

    private static LootPoolSingletonContainer.Builder<?> batch(Item item, StatePropertiesPredicate.Builder when) {
        return LootItem.lootTableItem(item)
                .when(siftingBox(when))
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(SiftingBoxBlock.YIELD)));
    }

    private LootTable.Builder hashishBarDrops(Block bar, Item piece) {
        LootTable.Builder table = LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(cutsExactly(bar, 0))
                .add(LootItem.lootTableItem(bar)));
        for (int cuts = 1; cuts <= 4; cuts++) {
            table.withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .when(cutsExactly(bar, cuts))
                    .add(LootItem.lootTableItem(piece).apply(SetItemCountFunction.setCount(
                            ConstantValue.exactly(HashishBarBlock.remaining(cuts))))));
        }
        return table;
    }

    /**
     * The condition names the block explicitly, so the two bars cannot share one by accident —
     * {@code block_state_property} checks the block as well as the state, and a table pointing at
     * the wrong one would simply never fire and drop nothing at all.
     */
    private static LootItemCondition.Builder cutsExactly(Block bar, int cuts) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(bar)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(HashishBarBlock.CUTS, cuts));
    }

    /**
     * Buds dropped by an untrimmed mature plant, and the Fortune curve on them — <b>deliberately
     * identical for every strain</b>, which is why they're constants here and not per-crop
     * parameters. A strain is told apart by what its buds <em>do</em> when smoked and by the
     * leaf/stem split below, never by how many buds it hands over.
     */
    private static final int BUDS_AT_ZERO_CUTS = 2;
    private static final float BUDS_FORTUNE_CHANCE = 0.20F;

    /**
     * The light ladder's buds (see {@code GrowLight}). The Grow Lamp's +2 is twice Fortune III's
     * average +1, which is only fair because the lamp is Nether-gated through its glowstone. A
     * STRESSED plant loses one bud and seeds itself.
     */
    private static final int GROW_LAMP_BUDS = 2;
    private static final int LAMP_BUDS = 1;
    private static final float AMBIENT_BUD_CHANCE = 0.5F;
    private static final int STRESSED_SEEDS = 2;

    /**
     * Schwag, by Fortune level: the odds a <b>healthy</b> ripe plant loses one bud to it — the
     * poisonous potato's 2%, once per plant — and that <b>each</b> bud of a stressed one does.
     *
     * <p>Fortune is a careful harvest that picks the seedy buds out: any level clears a healthy plant
     * entirely, and it only softens a stressed one. Stress is the thing to avoid; Fortune limits the
     * damage and never makes it safe.
     */
    private static final Float[] HEALTHY_SCHWAG_CHANCES = {0.02F, 0.0F};
    private static final Float[] STRESSED_SCHWAG_CHANCES = {0.25F, 0.20F, 0.15F, 0.10F};
    /**
     * Mildew costs what stress does to the buds and nothing else: no bud short and no seeds, since
     * mildew does not make a plant hermaphrodite. A plant reached by the sulfur gas while it
     * flowered spoils less. Fortune softens both the way it softens stress. See {@code Mildew}.
     */
    private static final Float[] MILDEWED_SCHWAG_CHANCES = STRESSED_SCHWAG_CHANCES;
    private static final Float[] SULFURED_SCHWAG_CHANCES = {0.10F, 0.08F, 0.06F, 0.04F};

    /** The flags that move the buds, one bud each: the two trims, and only those. */
    private static final BooleanProperty[] TRIMS = {Defoliation.TRIMMED_EARLY, Defoliation.TRIMMED_LATE};
    /**
     * The flags that each took one leaf off the growing plant, which the harvest then takes back:
     * both trims <b>and the rub</b> (see {@link Defoliation}). Leave one out and that leaf is a free
     * leaf per plant.
     */
    private static final BooleanProperty[] LEAVES_TAKEN =
            {Defoliation.TRIMMED_EARLY, Defoliation.TRIMMED_LATE, Defoliation.RUBBED};

    public ModLootTableProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        dropSelf(ModBlocks.HEMP_BRICKS_BLOCK);
        add(ModBlocks.HEMP_BRICKS_SLAB,createSlabItemTable(ModBlocks.HEMP_BRICKS_SLAB));
        dropSelf(ModBlocks.HEMP_BRICKS_STAIRS);
        dropSelf(ModBlocks.HEMP_BRICKS_WALL);
        dropSelf(ModBlocks.HEMP_PLANKS);
        add(ModBlocks.HEMP_PLANKS_SLAB, createSlabItemTable(ModBlocks.HEMP_PLANKS_SLAB));
        dropSelf(ModBlocks.HEMP_PLANKS_STAIRS);
        dropSelf(ModBlocks.HEMP_PLANKS_BUTTON);
        add(ModBlocks.HEMP_PLANKS_DOOR, createDoorTable((ModBlocks.HEMP_PLANKS_DOOR)));
        dropSelf(ModBlocks.HEMP_PLANKS_FENCE);
        // Missing until 2.0.2: a block with no table of its own drops nothing at all.
        dropSelf(ModBlocks.HEMP_PLANKS_TRAPDOOR);
        dropSelf(ModBlocks.HEMP_PLANKS_FENCE_GATE);
        dropSelf(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE);
        dropSelf(ModBlocks.HEMP_PLANKS_SHELF);
        dropSelf(ModBlocks.HEMP_PARQUET);
        dropSelf(ModBlocks.HEMP_PARQUET_STAIRS);
        add(ModBlocks.HEMP_PARQUET_SLAB, createSlabItemTable(ModBlocks.HEMP_PARQUET_SLAB));
        dropSelf(ModBlocks.HEMPCRETE_BLOCK);
        dropSelf(ModBlocks.HEMPCRETE_POWDER_BLOCK);
        dropSelf(ModBlocks.HEMP_BALE);
        dropSelf(ModBlocks.HEMP_LEAVES);
        dropSelf(ModBlocks.HEMP_WOOL);
        dropSelf(ModBlocks.HEMP_CARPET);
        // Two halves, one bed: vanilla's beds drop from the head half only, and so does this one.
        add(ModBlocks.HEMP_BED, createSinglePropConditionTable(ModBlocks.HEMP_BED, BedBlock.PART, BedPart.HEAD));
        // Empty, exactly like vanilla's cake: you cannot pick a cake back up once it is placed.
        // Vanilla still ships a loot table file for it rather than omitting one, so we do too.
        add(ModBlocks.SPACE_CAKE, LootTable.lootTable());
        // Each machine's block entity scatters its own contents when the block goes, so the loot
        // table only has to hand back the machine itself. Vanilla's furnace drop: the machine,
        // carrying the name an anvil gave it (NamedMachineBlockEntity keeps it while the machine
        // stands).
        add(ModBlocks.DECARBOXYLATOR, createNameableBlockEntityTable(ModBlocks.DECARBOXYLATOR));
        add(ModBlocks.INFUSER, createNameableBlockEntityTable(ModBlocks.INFUSER));
        add(ModBlocks.SIFTING_BOX, siftingBoxDrops());
        add(ModBlocks.HEMP_PRESS, createNameableBlockEntityTable(ModBlocks.HEMP_PRESS));
        dropSelf(ModBlocks.GROW_LAMP);
        add(ModBlocks.GROW_POT, growPotDrops());
        // The tray drops empty — you drain a reservoir before you move it — so it needs no
        // copy_state, unlike the pot, which keeps its soil.
        dropSelf(ModBlocks.HYDRO_TRAY);
        // A placed bong is the device itself, set down: it drops that device back, carrying every
        // component the block entity kept (durability, enchantments, a name). Explosions still
        // break it the way they break a flower pot, because it is glass.
        for (Block bong : ModBlocks.DEVICE_BLOCKS.values()) {
            add(bong, LootTable.lootTable().withPool(applyExplosionCondition(bong, LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(bong.asItem())
                            .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY))))));
        }
        dropSelf(ModBlocks.CHARAS_BALL);
        add(ModBlocks.HASHISH_BAR, hashishBarDrops(ModBlocks.HASHISH_BAR, ModItems.HASHISH));
        add(ModBlocks.FILTERED_HASHISH_BAR,
                hashishBarDrops(ModBlocks.FILTERED_HASHISH_BAR, ModItems.FILTERED_HASHISH));

        // Wall sign / wall hanging sign share the standing block's loot table (see ModBlocks#dropsLike),
        // so they must not get their own dropSelf call here.
        dropSelf(ModBlocks.HEMP_PLANKS_SIGN);
        dropSelf(ModBlocks.HEMP_PLANKS_HANGING_SIGN);

        add(ModBlocks.INDICA_CROP, indicaCropDrops());
        add(ModBlocks.BELDIA_CROP, beldiaCropDrops());
        add(ModBlocks.INDICA_FLOWER, indicaFlowerDrops());

        add(ModBlocks.SATIVA_CROP, sativaCropDrops());
        add(ModBlocks.SATIVA_FLOWER, sativaFlowerDrops());
        // The pot and the plant back, as every vanilla potted plant gives. Copying a potted allium's
        // settings does not bring its table along -- a block reads the table under its own id -- so
        // both pots dropped nothing until 2.0.2.
        dropPottedContents(ModBlocks.POTTED_INDICA_FLOWER);
        dropPottedContents(ModBlocks.POTTED_SATIVA_FLOWER);
    }

    /**
     * Wild indica flower: shears or Silk Touch lift the flower itself intact (for potting/decor,
     * same as leaves); any other tool breaks it down into seeds with the same Fortune scaling as
     * the crop's seeds (base 1 + binomial(fortuneLevel + 3, 0.40)).
     */
    private LootTable.Builder indicaFlowerDrops() {
        Holder<Enchantment> fortune = this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        return this.createSilkTouchOrShearsDispatchTable(ModBlocks.INDICA_FLOWER,
                this.applyExplosionDecay(ModBlocks.INDICA_FLOWER,
                        LootItem.lootTableItem(ModItems.INDICA_SEEDS)
                                .apply(ApplyBonusCount.addBonusBinomialDistributionCount(fortune, 0.40F, 3))));
    }

    /**
     * Loot for the two-tall indica crop. Purple Kush is the compact, bushy strain: <b>two leaves
     * more and two stems fewer</b> than Lemon Haze at every cut count. Buds are identical between
     * the strains — see {@link #BUDS_AT_ZERO_CUTS} — and so are seeds.
     */
    private LootTable.Builder indicaCropDrops() {
        return this.hempCropDrops(ModBlocks.INDICA_CROP,
                () -> StatePropertiesPredicate.Builder.properties().hasProperty(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER),
                IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE,
                ModItems.INDICA_BUDS, ModItems.INDICA_SEEDS,
                5, 2);
    }

    /**
     * The harvest table shared by both hemp crops.
     *
     * <p>Only the LOWER segment yields anything, so a plant is harvested exactly once no matter
     * which of its blocks is broken (see each crop's {@code onBreak}); an immature LOWER returns a
     * single seed. A mature LOWER pays out buds, seeds, hemp stem and hemp leaf, each with the
     * usual base + {@code binomial(fortuneLevel + extra, probability)} bonus.
     *
     * <p><b>Cut history moves buds and leaves in opposite directions</b> (see {@link Defoliation}).
     * Buds go up by one per <b>trim</b> ({@link #TRIMS}); leaves go down by one per leaf the player
     * already took off the plant — both trims <b>and the rub</b> ({@link #LEAVES_TAKEN}). A plant
     * trimmed twice and rubbed finishes with {@link #BUDS_AT_ZERO_CUTS} + 2 buds and
     * {@code leavesAtZeroCuts - 3} leaves, an untouched one with the base of each. The rub moves no
     * buds: the sugar leaf it takes is not defoliation. Stems are <b>flat</b> — stalk is structural,
     * and pruning foliage has no business changing how much of it there is.
     *
     * <p><b>Buds and seeds are strain-independent</b> and stem/leaf counts are the only per-crop
     * numbers, which is why only those two are parameters. Keep it that way: a strain should be
     * told apart by its effects and its leaf/stem character, not by handing over more of the same
     * premium drop.
     *
     * <p>Note that the leaves a player takes during the two cuts and the rub exactly replace the
     * leaves they give up at harvest, so a plant yields the same number of leaves either way and
     * defoliating is a straight gain in buds. That is intended: it is what the real practice does
     * (you keep the trimmed fan leaves <em>and</em> the flowers do better), and the price is paid in
     * shear durability and in having to come back at the right moments instead of planting and
     * forgetting. <b>Hence {@code leavesAtZeroCuts} must be at least three</b> — one per leaf that
     * can be taken. Below that the base would go negative and silently eat the Fortune bonus, so it
     * throws instead. Lemon Haze sits exactly on it: worked all three ways, its base is zero and
     * only Fortune's bonus remains.
     */
    private LootTable.Builder hempCropDrops(Block crop, Supplier<StatePropertiesPredicate.Builder> lowerPredicate,
                                            IntegerProperty ageProperty, int maxAge,
                                            Item buds, Item seeds,
                                            int leavesAtZeroCuts, int stemCount) {
        if (leavesAtZeroCuts < LEAVES_TAKEN.length) {
            throw new IllegalArgumentException(crop + " has " + leavesAtZeroCuts + " leaves at harvest,"
                    + " fewer than the " + LEAVES_TAKEN.length + " a player can take off it while it grows");
        }
        HolderLookup.RegistryLookup<Enchantment> enchantments = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);

        LootItemCondition.Builder isLower = LootItemBlockStatePropertyCondition.hasBlockStateProperties(crop)
                .setProperties(lowerPredicate.get());
        LootItemCondition.Builder isMatureLower = LootItemBlockStatePropertyCondition.hasBlockStateProperties(crop)
                .setProperties(lowerPredicate.get().hasProperty(ageProperty, maxAge));

        // The three cut buckets, which drive the buds. "One cut" is the only one that needs an OR,
        // because it is reached by two different paths: the player caught the early window, or only
        // the late one.
        LootItemCondition.Builder zeroCuts = workedBucket(crop, lowerPredicate, ageProperty, maxAge, 0, TRIMS);
        LootItemCondition.Builder oneCut = workedBucket(crop, lowerPredicate, ageProperty, maxAge, 1, TRIMS);
        LootItemCondition.Builder twoCuts = workedBucket(crop, lowerPredicate, ageProperty, maxAge, 2, TRIMS);
        LootItemCondition.Builder stressed = grewUnder(crop, lowerPredicate, ageProperty, maxAge, GrowLight.STRESSED);
        LootItemCondition.Builder mildewed = carries(crop, lowerPredicate, ageProperty, maxAge, Mildew.MILDEWED);
        LootItemCondition.Builder sulfured = carries(crop, lowerPredicate, ageProperty, maxAge, Mildew.SULFURED);
        LootItemCondition.Builder inABed = LocationCheck.checkLocation(
                LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block()
                        .of(this.registries.lookupOrThrow(Registries.BLOCK),
                                ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY)),
                new BlockPos(0, -1, 0));

        return this.applyExplosionDecay(crop,
                LootTable.lootTable()
                        // Buds when mature — more of them the more the plant was trimmed. Falls
                        // through to a single seed when the plant isn't mature yet.
                        .withPool(LootPool.lootPool()
                                .when(isLower)
                                // A STRESSED plant first: one bud short of whatever its trims earned,
                                // and every bud it keeps may have gone to schwag. Every other ripe
                                // plant risks the poisonous potato's one bad item, which any Fortune
                                // clears. See SCHWAG_*_CHANCES. Mildew and sulfur come after stress, so
                                // a plant with both takes the worse odds, never the sum.
                                .add(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS - 1, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(zeroCuts, stressed), fortune, STRESSED_SCHWAG_CHANCES)
                                        .otherwise(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(oneCut, stressed), fortune, STRESSED_SCHWAG_CHANCES))
                                        .otherwise(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS + 1, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(twoCuts, stressed), fortune, STRESSED_SCHWAG_CHANCES))
                                        .otherwise(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(zeroCuts, mildewed), fortune, MILDEWED_SCHWAG_CHANCES))
                                        .otherwise(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS + 1, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(oneCut, mildewed), fortune, MILDEWED_SCHWAG_CHANCES))
                                        .otherwise(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS + 2, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(twoCuts, mildewed), fortune, MILDEWED_SCHWAG_CHANCES))
                                        .otherwise(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(zeroCuts, sulfured), fortune, SULFURED_SCHWAG_CHANCES))
                                        .otherwise(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS + 1, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(oneCut, sulfured), fortune, SULFURED_SCHWAG_CHANCES))
                                        .otherwise(spoiledBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS + 2, fortune, BUDS_FORTUNE_CHANCE, 2), AllOfCondition.allOf(twoCuts, sulfured), fortune, SULFURED_SCHWAG_CHANCES))
                                        .otherwise(healthyBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS, fortune, BUDS_FORTUNE_CHANCE, 2), zeroCuts, fortune))
                                        .otherwise(healthyBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS + 1, fortune, BUDS_FORTUNE_CHANCE, 2), oneCut, fortune))
                                        .otherwise(healthyBuds(scaledCount(buds, BUDS_AT_ZERO_CUTS + 2, fortune, BUDS_FORTUNE_CHANCE, 2), twoCuts, fortune))
                                        .otherwise(LootItem.lootTableItem(seeds))))
                        // Hemp leaf when mature — one fewer for every leaf already taken off the
                        // plant while it grew: both trims AND the rub, so four buckets, not three.
                        .withPool(LootPool.lootPool()
                                .when(isMatureLower)
                                .add(scaledEntry(ModItems.HEMP_LEAF, leavesAtZeroCuts,
                                                workedBucket(crop, lowerPredicate, ageProperty, maxAge, 0, LEAVES_TAKEN), fortune, 0.30F, 3)
                                        .otherwise(scaledEntry(ModItems.HEMP_LEAF, leavesAtZeroCuts - 1,
                                                workedBucket(crop, lowerPredicate, ageProperty, maxAge, 1, LEAVES_TAKEN), fortune, 0.30F, 3))
                                        .otherwise(scaledEntry(ModItems.HEMP_LEAF, leavesAtZeroCuts - 2,
                                                workedBucket(crop, lowerPredicate, ageProperty, maxAge, 2, LEAVES_TAKEN), fortune, 0.30F, 3))
                                        .otherwise(scaledEntry(ModItems.HEMP_LEAF, leavesAtZeroCuts - 3,
                                                workedBucket(crop, lowerPredicate, ageProperty, maxAge, 3, LEAVES_TAKEN), fortune, 0.30F, 3))))
                        // The light it grew under: +2 buds under a Grow Lamp, +1 under any lit lamp, and
                        // an even chance of +1 under any other light-15 block. See GrowLight.
                        .withPool(LootPool.lootPool()
                                .when(isMatureLower)
                                .add(fixedEntry(buds, GROW_LAMP_BUDS, grewUnder(crop, lowerPredicate, ageProperty, maxAge, GrowLight.GROW_LAMP))
                                        .otherwise(fixedEntry(buds, LAMP_BUDS, grewUnder(crop, lowerPredicate, ageProperty, maxAge, GrowLight.LAMP)))
                                        .otherwise(fixedEntry(buds, 1, grewUnder(crop, lowerPredicate, ageProperty, maxAge, GrowLight.AMBIENT))
                                                .when(LootItemRandomChanceCondition.randomChance(AMBIENT_BUD_CHANCE)))))
                        // A leaf for any lit lamp. The Grow Lamp's larger share comes during growth
                        // instead, a second leaf on each trim (Defoliation#tryCut).
                        .withPool(LootPool.lootPool()
                                .when(isMatureLower)
                                .when(AnyOfCondition.anyOf(
                                        grewUnder(crop, lowerPredicate, ageProperty, maxAge, GrowLight.GROW_LAMP),
                                        grewUnder(crop, lowerPredicate, ageProperty, maxAge, GrowLight.LAMP)))
                                .add(LootItem.lootTableItem(ModItems.HEMP_LEAF)))
                        // A STRESSED plant went hermaphrodite and seeded its own buds.
                        .withPool(LootPool.lootPool()
                                .when(stressed)
                                .add(LootItem.lootTableItem(seeds)
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly((float) STRESSED_SEEDS)))))
                        // Seeds when mature. Identical across strains, so replanting either costs the same.
                        .withPool(LootPool.lootPool()
                                .when(isMatureLower)
                                .add(LootItem.lootTableItem(seeds)
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(3.0F)))
                                        .apply(ApplyBonusCount.addBonusBinomialDistributionCount(fortune, 0.40F, 3))))
                        // Hemp stem when mature. Unaffected by trimming — but one short in any bed of
                        // ours: a plant with its roots boxed in (a pot's soil, a tray's net pot) grows a
                        // thinner stalk, and fibre hemp is a field crop. Read off the block underneath,
                        // so it needs no state of its own.
                        .withPool(LootPool.lootPool()
                                .when(isMatureLower)
                                .add(LootItem.lootTableItem(ModItems.HEMP_STEM)
                                        .when(inABed)
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly((float) (stemCount - 1))))
                                        .apply(ApplyBonusCount.addBonusBinomialDistributionCount(fortune, 0.30F, 3))
                                        .otherwise(LootItem.lootTableItem(ModItems.HEMP_STEM)
                                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly((float) stemCount)))
                                                .apply(ApplyBonusCount.addBonusBinomialDistributionCount(fortune, 0.30F, 3))))));
    }

    /**
     * "This plant is a mature LOWER and exactly {@code count} of these {@code flags} are set" — an OR
     * over every combination that gets there, so a bucket reached by several paths is still one
     * condition. A bucket with only one path comes back bare rather than as a one-term OR.
     *
     * <p>Every bucket names <b>every</b> flag in the list, so the buckets for one list are mutually
     * exclusive and cover all of its combinations: a plant cannot match two and fall through none.
     */
    private static LootItemCondition.Builder workedBucket(Block crop, Supplier<StatePropertiesPredicate.Builder> lowerPredicate,
                                                      IntegerProperty ageProperty, int maxAge,
                                                      int count, BooleanProperty[] flags) {
        List<LootItemCondition.Builder> paths = new ArrayList<>();
        for (int set = 0; set < 1 << flags.length; set++) {
            if (Integer.bitCount(set) != count) {
                continue;
            }
            StatePropertiesPredicate.Builder state = lowerPredicate.get().hasProperty(ageProperty, maxAge);
            for (int i = 0; i < flags.length; i++) {
                state.hasProperty(flags[i], (set & 1 << i) != 0);
            }
            paths.add(LootItemBlockStatePropertyCondition.hasBlockStateProperties(crop).setProperties(state));
        }
        return paths.size() == 1 ? paths.get(0)
                : AnyOfCondition.anyOf(paths.toArray(LootItemCondition.Builder[]::new));
    }

    /** "This plant is a mature LOWER that grew under {@code light}." */
    private static LootItemCondition.Builder grewUnder(Block crop, Supplier<StatePropertiesPredicate.Builder> lowerPredicate,
                                                   IntegerProperty ageProperty, int maxAge, GrowLight light) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(crop)
                .setProperties(lowerPredicate.get().hasProperty(ageProperty, maxAge).hasProperty(GrowLight.PROPERTY, light));
    }

    /** "This plant is a mature LOWER that carries {@code mildew}." */
    private static LootItemCondition.Builder carries(Block crop, Supplier<StatePropertiesPredicate.Builder> lowerPredicate,
                                                 IntegerProperty ageProperty, int maxAge, Mildew mildew) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(crop)
                .setProperties(lowerPredicate.get().hasProperty(ageProperty, maxAge).hasProperty(Mildew.PROPERTY, mildew));
    }

    /**
     * A healthy ripe plant's buds: one of them turns to schwag {@link #HEALTHY_SCHWAG_CHANCES} of the
     * time. Only the main bud pool is wrapped, and exactly one of its alternatives fires, so that is
     * the odds per plant; the light ladder's bonus buds are never spoiled.
     */
    private static LootPoolEntryContainer.Builder<?> healthyBuds(LootPoolSingletonContainer.Builder<?> buds, LootItemCondition.Builder bucket,
                                                       Holder<Enchantment> fortune) {
        return ModLootEntryTypes.SpoilingEntry.builder(buds, ModItems.SCHWAG)
                .when(bucket)
                .enchantment(fortune)
                .oneItem(HEALTHY_SCHWAG_CHANCES);
    }

    /**
     * A stressed, mildewed or sulfured plant's buds: each one turns to schwag at {@code chances}.
     *
     * <p>Both wrappers carry the bucket's condition <b>themselves</b>, and the buds inside carry none:
     * they sit in an {@code alternatives} chain, and vanilla's validation reads only the conditions of
     * the chain's own children. With them on the buds, every later alternative was reported as an
     * "Unreachable entry!" — three warnings at every world load, though the drops were right.
     */
    private static LootPoolEntryContainer.Builder<?> spoiledBuds(LootPoolSingletonContainer.Builder<?> buds, LootItemCondition.Builder bucket,
                                                       Holder<Enchantment> fortune, Float[] chances) {
        return ModLootEntryTypes.SpoilingEntry.builder(buds, ModItems.SCHWAG)
                .when(bucket)
                .enchantment(fortune)
                .eachItem(chances);
    }

    /** A flat bonus: no Fortune, which already scales the plant's own drops. */
    private static LootPoolSingletonContainer.Builder<?> fixedEntry(Item item, int count, LootItemCondition.Builder condition) {
        return LootItem.lootTableItem(item)
                .when(condition)
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly((float) count)));
    }

    /**
     * The pot drops itself, carrying its fertility when it has any. A spent pot drops plain, so it
     * stacks with a freshly crafted one — both are empty.
     */
    private LootTable.Builder growPotDrops() {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(ModBlocks.GROW_POT)
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.GROW_POT)
                                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(GrowPotBlock.FERTILITY, 0)))
                        .otherwise(LootItem.lootTableItem(ModBlocks.GROW_POT)
                                .apply(CopyBlockState.copyState(ModBlocks.GROW_POT).copy(GrowPotBlock.FERTILITY)))));
    }

    /** A fixed-count drop gated on one cut bucket, with the usual Fortune bonus on top. */
    private static LootPoolSingletonContainer.Builder<?> scaledEntry(Item item, int count, LootItemCondition.Builder bucket,
                                                    Holder<Enchantment> fortune,
                                                    float fortuneChance, int fortuneExtra) {
        return scaledCount(item, count, fortune, fortuneChance, fortuneExtra).when(bucket);
    }

    /** The same drop with no condition of its own, for a wrapper that carries it (the buds). */
    private static LootPoolSingletonContainer.Builder<?> scaledCount(Item item, int count, Holder<Enchantment> fortune,
                                                    float fortuneChance, int fortuneExtra) {
        return LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly((float) count)))
                .apply(ApplyBonusCount.addBonusBinomialDistributionCount(fortune, fortuneChance, fortuneExtra));
    }

    /**
     * Wild Lemon Haze: the same deal as the wild Purple Kush above — shears or Silk Touch lift the
     * flower itself for potting, anything else breaks it down into that strain's seeds.
     *
     * <h2>The {@code half=lower} guard is the whole difference from indica's</h2>
     *
     * This flower is <b>two blocks tall</b>, and a two-block plant rolls its loot table <em>twice</em>
     * per break: once for the half the player hit, once as the orphaned half pops off in the
     * neighbour update. Without a guard the player gets two flowers, or two rolls of the seed
     * bonus, from one plant — silently, and in the generous direction (`crops.md`, *the tall-plant
     * loot double-roll*). Pinning the pool to the lower half means exactly one of the two rolls can
     * ever pay out, whichever half was struck. It is the same fix vanilla's {@code rose_bush} table
     * uses, and the same one {@code hempCropDrops} applies through {@code isLower}.
     */
    private LootTable.Builder sativaFlowerDrops() {
        Holder<Enchantment> fortune = this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        LootItemCondition.Builder isLower = LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.SATIVA_FLOWER)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(isLower)
                .add(LootItem.lootTableItem(ModBlocks.SATIVA_FLOWER)
                        .when(this.hasShears().or(this.hasSilkTouch()))
                        .otherwise(this.applyExplosionDecay(ModBlocks.SATIVA_FLOWER,
                                LootItem.lootTableItem(ModItems.SATIVA_SEEDS)
                                        .apply(ApplyBonusCount.addBonusBinomialDistributionCount(fortune, 0.40F, 3))))));
    }

    /**
     * Beldía: Purple Kush's five leaves and <b>one stem</b> — a short, leafy plant grown for its
     * resin, so it sits at the bottom of the stem list (Purple Kush 2, Lemon Haze 4). Eleven items
     * where the two older strains hand over twelve; the missing stem pays for buds that sift double.
     * A flat count, so no condition beyond the shared table's; in a bed it is one short like any
     * plant, which here is none. It is also its own wild plant, so a ripe one found on a desert
     * riverbank pays exactly this.
     */
    private LootTable.Builder beldiaCropDrops() {
        return this.hempCropDrops(ModBlocks.BELDIA_CROP,
                () -> StatePropertiesPredicate.Builder.properties().hasProperty(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER),
                IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE,
                ModItems.BELDIA_BUDS, ModItems.BELDIA_SEEDS,
                5, 1);
    }

    /**
     * Loot for the three-tall sativa crop. Lemon Haze is the tall, lanky strain: <b>two stems more
     * and two leaves fewer</b> than Purple Kush at every cut count — a plant that is mostly stalk
     * should read as mostly stalk in the drops.
     *
     * <p>Buds and seeds are identical to indica's. Lemon Haze is worth growing for what its buds
     * <em>do</em> (Speed/Haste against Purple Kush's Resistance) and for the stem yield, not for a
     * bigger pile of the same thing — see {@link #BUDS_AT_ZERO_CUTS}. Note the two strains end up
     * on the <b>same total item count</b> in every row; they differ in composition, not quantity.
     */
    private LootTable.Builder sativaCropDrops() {
        return this.hempCropDrops(ModBlocks.SATIVA_CROP,
                () -> StatePropertiesPredicate.Builder.properties().hasProperty(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER),
                SativaCropBlock.AGE, SativaCropBlock.MAX_AGE,
                ModItems.SATIVA_BUDS, ModItems.SATIVA_SEEDS,
                3, 4);
    }
}

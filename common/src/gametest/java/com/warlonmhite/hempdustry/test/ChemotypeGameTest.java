package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.ChemotypeBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Chemotype travels with the matter: no route may turn CBD matter into THC matter, nothing 2.1 made
 * changes, and the THC in a bowl or a butter is weighed by what it is. 2.2 ships no CBD-dominant
 * strain yet, so CBD matter is stamped by hand here, as a datapack strain or a future one would drop it;
 * Beldía is the balanced plant, Industrial Hemp the CBD-dominant one.
 */
public final class ChemotypeGameTest {

    private static final BlockPos BOX = new BlockPos(1, 1, 1);
    private static final BlockPos TUB = new BlockPos(3, 1, 1);

    private static ItemStack stamped(Item item, int count, Chemotype chemotype) {
        return Chemotype.stamp(new ItemStack(item, count), chemotype);
    }

    /** The furnace, the oven, the press and the cauldron each keep what went in. */
    public static void everyRouteKeepsTheChemotype(GameTestHelper context) {
        ServerLevel world = context.getLevel();

        ItemStack scorchedLeaf = smelt(world, stamped(ModItems.HEMP_LEAF, 1, Chemotype.CBD_DOMINANT));
        context.assertTrue(scorchedLeaf.is(ModItems.SCORCHED_HEMP) && Chemotype.of(scorchedLeaf) == Chemotype.CBD_DOMINANT,
                "a CBD leaf charred into " + scorchedLeaf + ", not CBD scorched hemp");
        ItemStack scorchedBud = smelt(world, new ItemStack(ModItems.BELDIA_BUDS));
        context.assertTrue(Chemotype.of(scorchedBud) == Chemotype.BALANCED,
                "a Beldía bud charred into " + Chemotype.of(scorchedBud) + " scorched hemp, not balanced");
        ItemStack scorchedKush = smelt(world, new ItemStack(ModItems.INDICA_BUDS));
        context.assertTrue(!scorchedKush.has(ModComponents.CHEMOTYPE),
                "a Purple Kush bud charred into stamped scorched hemp, which no longer stacks with 2.1's");

        ItemStack baked = DecarboxylatorBlockEntity.resultFor(world, new ItemStack(ModItems.BELDIA_BUDS));
        context.assertTrue(baked.is(ModItems.DECARBOXYLATED_HEMP) && Chemotype.of(baked) == Chemotype.BALANCED
                        && baked.getCount() == DecarboxylatorBlockEntity.BUDS_OUTPUT,
                "a Beldía bud baked into " + baked + ", not four balanced decarboxylated hemp");
        ItemStack bakedLeaf = DecarboxylatorBlockEntity.resultFor(world, stamped(ModItems.HEMP_LEAF, 1, Chemotype.CBD_DOMINANT));
        context.assertTrue(Chemotype.of(bakedLeaf) == Chemotype.CBD_DOMINANT,
                "a CBD leaf baked into " + bakedLeaf);
        for (Chemotype neither : new Chemotype[]{Chemotype.CBG_DOMINANT, Chemotype.CANNABINOID_FREE}) {
            context.assertTrue(DecarboxylatorBlockEntity.resultFor(world, stamped(ModItems.HEMP_LEAF, 1, neither)).isEmpty(),
                    "the oven took a " + neither + " leaf, which would make butter that does nothing");
            context.assertTrue(!InfuserBlockEntity.isHemp(world, stamped(ModItems.SCORCHED_HEMP, 1, neither)),
                    "the Infuser took " + neither + " scorched hemp, the second door into an edible");
        }

        ItemStack pressed = HempPressBlockEntity.resultFor(world, stamped(ModItems.KIEF, 1, Chemotype.CBD_DOMINANT));
        context.assertTrue(pressed.is(ModItems.HASHISH) && Chemotype.of(pressed) == Chemotype.CBD_DOMINANT,
                "CBD kief pressed into " + pressed);

        BlockPos cauldron = context.absolutePos(BOX);
        world.setBlockAndUpdate(cauldron, net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        ItemStack hemp = stamped(ModItems.DECARBOXYLATED_HEMP, 4, Chemotype.CBD_DOMINANT);
        player.setItemInHand(InteractionHand.MAIN_HAND, hemp);
        world.getBlockState(cauldron).useItemOn(hemp, world, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false));
        ItemStack washed = find(player, ModItems.WASHED_DECARBOXYLATED_HEMP);
        context.assertTrue(washed.getCount() == 4 && Chemotype.of(washed) == Chemotype.CBD_DOMINANT,
                "washing CBD decarboxylated hemp gave " + washed);
        context.succeed();
    }

    /** Nine of one kind make a block of it and come back out as they went in; nine of two kinds make nothing. */
    public static void nineLeavesMakeABlockOfTheirKind(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        List<ItemStack> nine = new ArrayList<>(Collections.nCopies(9, stamped(ModItems.HEMP_LEAF, 1, Chemotype.CBD_DOMINANT)));
        ItemStack block = craft(world, CraftingInput.of(3, 3, nine));
        context.assertTrue(block.is(ModBlocks.HEMP_LEAVES.asItem())
                        && ChemotypeBlocks.ofBlockItem(block) == Chemotype.CBD_DOMINANT,
                "nine CBD leaves made " + block + ", not a CBD Block of Hemp Leaves");

        nine.set(4, new ItemStack(ModItems.HEMP_LEAF));
        context.assertTrue(craft(world, CraftingInput.of(3, 3, nine)).isEmpty(),
                "eight CBD leaves and a THC one made a block: the CBD would come back out THC");

        ItemStack leaves = craft(world, CraftingInput.of(1, 1, List.of(block)));
        context.assertTrue(leaves.is(ModItems.HEMP_LEAF) && leaves.getCount() == 9
                        && Chemotype.of(leaves) == Chemotype.CBD_DOMINANT,
                "a CBD block unpacked into " + leaves);

        // Placed and broken, the block keeps it too.
        BlockPos pos = context.absolutePos(BOX);
        world.setBlockAndUpdate(pos, ModBlocks.HEMP_LEAVES.defaultBlockState()
                .setValue(ChemotypeBlocks.CHEMOTYPE, Chemotype.CBD_DOMINANT));
        List<ItemStack> drops = Block.getDrops(world.getBlockState(pos), world, pos, null);
        context.assertTrue(drops.size() == 1 && ChemotypeBlocks.ofBlockItem(drops.getFirst()) == Chemotype.CBD_DOMINANT,
                "a placed CBD block broke into " + drops);
        // THC-dominant is never written, so the block item 2.1 made and the one made now are one stack.
        List<ItemStack> thc = new ArrayList<>(Collections.nCopies(9, new ItemStack(ModItems.HEMP_LEAF)));
        ItemStack plain = craft(world, CraftingInput.of(3, 3, thc));
        context.assertTrue(ItemStack.isSameItemSameComponents(plain, new ItemStack(ModBlocks.HEMP_LEAVES)),
                "nine THC leaves made a stamped block, which no longer stacks with 2.1's: " + plain);
        context.succeed();
    }

    /** A crop's leaves and schwag drop stamped with its plant's chemotype; a bud never is. */
    public static void cropsDropTheirChemotype(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(BOX));
        Random seeds = new Random(20261109L);
        boolean sawBalancedLeaf = false;
        boolean sawCbdLeaf = false;
        for (int roll = 0; roll < 20; roll++) {
            long seed = seeds.nextLong();
            for (ItemStack stack : drops(world, origin, ripe(ModBlocks.BELDIA_CROP), seed)) {
                if (stack.is(ModItems.HEMP_LEAF) || stack.is(ModItems.SCHWAG)) {
                    context.assertTrue(Chemotype.of(stack) == Chemotype.BALANCED, "a ripe Beldía dropped " + stack
                            + " stamped " + Chemotype.of(stack) + ", not balanced");
                    sawBalancedLeaf |= stack.is(ModItems.HEMP_LEAF);
                }
                context.assertTrue(!stack.is(ModItems.BELDIA_BUDS) || !stack.has(ModComponents.CHEMOTYPE),
                        "a Beldía bud dropped with a stamp: a bud is its strain");
            }
            for (ItemStack stack : drops(world, origin, ripe(ModBlocks.INDICA_CROP), seed)) {
                context.assertTrue(!stack.has(ModComponents.CHEMOTYPE),
                        "a ripe Purple Kush dropped stamped " + stack + ", which no longer stacks with 2.1's");
            }
            BlockState hemp = ModBlocks.INDUSTRIAL_HEMP_CROP.defaultBlockState()
                    .setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                    .setValue(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE);
            for (ItemStack stack : drops(world, origin, hemp, seed)) {
                if (stack.is(ModItems.HEMP_LEAF) || stack.is(ModItems.SCHWAG)) {
                    context.assertTrue(Chemotype.of(stack) == Chemotype.CBD_DOMINANT,
                            "Industrial Hemp dropped " + stack + " stamped " + Chemotype.of(stack));
                    sawCbdLeaf |= stack.is(ModItems.HEMP_LEAF);
                }
            }
        }
        context.assertTrue(sawBalancedLeaf && sawCbdLeaf, "twenty harvests never dropped a leaf to check");
        context.succeed();
    }

    /** A bowl's THC is its units weighed by chemotype: a balanced bong of three is a THC dose of one. */
    public static void aBowlsThcIsWeighedByChemotype(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> beldia = strain(world, ModStrains.BELDIA);
        Holder<Strain> kush = strain(world, ModStrains.INDICA);
        Holder<Strain> schwag = strain(world, ModStrains.SCHWAG);

        context.assertValueEqual(3, SmokeContents.of(kush, 3).thcDose(), "a Purple Kush bong of three's THC dose");
        context.assertValueEqual(1, SmokeContents.of(beldia, 1).thcDose(),
                "a single balanced bud's THC dose, which never drops below one");
        context.assertValueEqual(1, SmokeContents.of(beldia, 3).thcDose(), "a balanced bong of three's THC dose");
        // A 2.1 load naming Beldía carries no stamp, and still reads balanced: a bud is its strain.
        context.assertValueEqual(1, SmokeContents.of(beldia, 2, Chemotype.THC_DOMINANT).thcDose(),
                "a Beldía load read its stamp instead of its strain");

        SmokeContents cbdSchwag = SmokeContents.of(schwag, 2, Chemotype.CBD_DOMINANT);
        context.assertValueEqual(0, cbdSchwag.thcDose(), "CBD schwag's THC dose");
        List<Holder<MobEffect>> effects = new ArrayList<>();
        for (MobEffectInstance effect : cbdSchwag.effects(700, false, () -> 0L)) {
            effects.add(effect.getEffect());
        }
        context.assertTrue(effects.contains(MobEffects.POISON) && !effects.contains(MobEffects.HUNGER),
                "CBD schwag gave " + effects + ": the smoke's Poison and none of the THC's Hunger");
        context.succeed();
    }

    /** CBD butter has no strength and gives only what answers to CBD; a mixed sieve takes the dosage. */
    public static void cbdButterAndAMixedSieve(GameTestHelper context) {
        ServerLevel world = context.getLevel();

        context.setBlock(TUB, ModBlocks.INFUSER);
        InfuserBlockEntity infuser = context.getBlockEntity(TUB, InfuserBlockEntity.class);
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Progress", InfuserBlockEntity.fullTime());
        nbt.putBoolean("HaveMilk", true);
        nbt.putInt("BatchUnwashed", 8);
        CompoundTag cbdCounts = new CompoundTag();
        cbdCounts.putInt("cbd_dominant", 8);
        nbt.put("BatchUnwashedChemotypes", cbdCounts);
        infuser.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), nbt));
        context.assertValueEqual(0, infuser.strength(), "eight CBD hemp's THC sum");
        context.assertValueEqual(8, infuser.cbdStrength(), "eight CBD hemp's CBD sum");
        context.assertTrue(infuser.chemotype() == Chemotype.CBD_DOMINANT, "eight CBD hemp made " + infuser.chemotype() + " butter");

        List<Holder<MobEffect>> cbd = new ArrayList<>();
        for (EdibleEffects.Dose dose : EdibleEffects.bundle(EdibleBundle.BUILT_IN, 0, 2, Quality.STANDARD, 0)) {
            cbd.add(dose.effect().getEffect());
            context.assertValueEqual(0, dose.effect().getAmplifier(), "a CBD edible's " + dose.effect() + " level");
        }
        context.assertValueEqual(List.of(MobEffects.SLOWNESS, MobEffects.REGENERATION), cbd,
                "a CBD edible's effects, which are Slowness and Regeneration and no high");
        // A THC edible is what it was.
        context.assertValueEqual(EdibleEffects.bundle(EdibleBundle.BUILT_IN, 3, Quality.CLEAN, 0).size(),
                EdibleEffects.bundle(EdibleBundle.BUILT_IN, 3, 0, Quality.CLEAN, 0).size(), "a THC edible's bundle");

        // Three levels of CBD plant, then four Purple Kush buds to fill it: about half each, balanced.
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.LEVEL, 3)
                .setValue(SiftingBoxBlock.THC_SHARE, 0).setValue(ChemotypeBlocks.CHEMOTYPE, Chemotype.CBD_DOMINANT));
        BlockPos box = context.absolutePos(BOX);
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(box), Direction.UP, box, false);
        ItemStack buds = new ItemStack(ModItems.INDICA_BUDS, 4);
        for (int i = 0; i < 4; i++) {
            world.getBlockState(box).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        }
        context.assertTrue(buds.isEmpty(), "the screen refused Purple Kush on a CBD batch: " + buds);
        context.assertTrue(ChemotypeBlocks.of(world.getBlockState(box)) == Chemotype.BALANCED,
                "half CBD and half THC sifted " + ChemotypeBlocks.of(world.getBlockState(box)) + ", not balanced");
        context.runAtTickTime(40, () -> {
            context.assertValueEqual(SiftingBoxBlock.READY_LEVEL, world.getBlockState(box).getValue(SiftingBoxBlock.LEVEL),
                    "the full screen did not settle");
            world.getBlockState(box).useWithoutItem(world, player, hit);
            List<ItemEntity> kief = world.getEntitiesOfClass(ItemEntity.class, new AABB(box).inflate(2));
            context.assertTrue(kief.size() == 1 && Chemotype.of(kief.getFirst().getItem()) == Chemotype.BALANCED,
                    "the balanced batch yielded " + kief.stream().map(ItemEntity::getItem).toList());
            context.succeed();
        });
    }

    private static Holder<Strain> strain(ServerLevel world, net.minecraft.resources.ResourceKey<Strain> key) {
        return world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static ItemStack smelt(ServerLevel world, ItemStack input) {
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return world.recipeAccess().getRecipeFor(RecipeType.SMELTING, recipeInput, world)
                .map(entry -> entry.value().assemble(recipeInput, world.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack craft(ServerLevel world, CraftingInput input) {
        return world.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, world)
                .map(entry -> entry.value().assemble(input, world.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack find(Player player, Item item) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static BlockState ripe(Block crop) {
        return Defoliation.unworked(crop.defaultBlockState())
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
    }

    private static List<ItemStack> drops(ServerLevel world, Vec3 origin, BlockState state, long seed) {
        LootTable table = world.getServer().reloadableRegistries()
                .getLootTable(state.getBlock().getLootTable().orElseThrow());
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);
        return table.getRandomItems(params, seed);
    }
}

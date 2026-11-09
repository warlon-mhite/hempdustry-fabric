package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.ChemotypeBlocks;
import com.warlonmhite.hempdustry.block.custom.HempLeavesBlock;
import com.warlonmhite.hempdustry.block.custom.SpaceCakeBlock;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.recipe.StampedSmeltingRecipe;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Strain;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Stamped matter in the corners: a default named explicitly stacks with a plain item, a stamped block
 * picks as it is, bhang turns away what the oven does, every bud scorches by a recipe whose own result
 * is right, a Space Cake carries both tiers its butter gave, and the oven's comparator reads what a 2.1
 * oven would.
 */
public final class StampedMatterGameTest {

    private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos SPOT = FLOOR.above();

    /**
     * A stack naming the default chemotype or band ({@code /give}, a datapack's {@code set_components},
     * another mod's copy) stacks with a plain one, and every way of writing the default writes nothing.
     */
    public static void aDefaultNeverCostsASlot(GameTestHelper context) {
        HolderLookup.Provider registries = context.getLevel().registryAccess();
        // Through the codec, as /give and every save read a stack.
        for (Item item : List.of(ModItems.HEMP_LEAF, ModItems.SCHWAG, ModItems.DECARBOXYLATED_HEMP, ModItems.SCORCHED_HEMP,
                ModItems.HASHISH, ModItems.ROSIN, ModItems.CANNABUTTER, ModItems.SPACE_COOKIE)) {
            ItemStack named = ItemStack.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE),
                    JsonParser.parseString("{\"id\": \"" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)
                            + "\", \"components\": {\"hempdustry:chemotype\": \"thc_dominant\"}}")).getOrThrow();
            context.assertTrue(ItemStack.isSameItemSameComponents(named, new ItemStack(item)),
                    "a " + item + " naming THC-dominant does not stack with a plain one: " + named.getComponentsPatch());
        }
        for (Item item : List.of(ModItems.HEMP_LEAF, ModItems.INDICA_BUDS, ModItems.ENDER_HAZE_BUDS)) {
            ItemStack named = ItemStack.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE),
                    JsonParser.parseString("{\"id\": \"" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)
                            + "\", \"components\": {\"hempdustry:potency_band\": \"mid\"}}")).getOrThrow();
            context.assertTrue(ItemStack.isSameItemSameComponents(named, new ItemStack(item)),
                    "a " + item + " naming mid does not stack with a plain one: " + named.getComponentsPatch());
        }
        // A plain set, as another mod's code would write it.
        ItemStack set = new ItemStack(ModItems.KIEF);
        set.set(ModComponents.CHEMOTYPE, Chemotype.THC_DOMINANT);
        context.assertTrue(set.getComponentsPatch().isEmpty(), "kief set to THC-dominant carries " + set.getComponentsPatch());

        // Stamped one way and back: nothing left behind, not even a removal.
        ItemStack leaf = PotencyBand.stamp(Chemotype.stamp(new ItemStack(ModItems.HEMP_LEAF), Chemotype.CBD_DOMINANT), PotencyBand.HIGH);
        Chemotype.stamp(leaf, Chemotype.THC_DOMINANT);
        PotencyBand.stamp(leaf, PotencyBand.MID);
        context.assertTrue(leaf.getComponentsPatch().isEmpty(), "a leaf stamped and unstamped carries " + leaf.getComponentsPatch());
        ItemStack bud = PotencyBand.stamp(PotencyBand.stamp(new ItemStack(ModItems.INDICA_BUDS), PotencyBand.HIGH), PotencyBand.MID);
        context.assertTrue(bud.getComponentsPatch().isEmpty(), "a bud banded and unbanded carries " + bud.getComponentsPatch());
        // An item without the band on its prototype still writes nothing for mid.
        ItemStack decarb = PotencyBand.stamp(new ItemStack(ModItems.DECARBOXYLATED_HEMP), PotencyBand.MID);
        context.assertTrue(decarb.getComponentsPatch().isEmpty(), "mid on decarboxylated hemp wrote " + decarb.getComponentsPatch());
        // A bud and a seed never carry a chemotype: their strain is their chemotype.
        context.assertTrue(!new ItemStack(ModItems.BELDIA_BUDS).has(ModComponents.CHEMOTYPE)
                        && !new ItemStack(ModItems.BELDIA_SEEDS).has(ModComponents.CHEMOTYPE),
                "a Beldía bud or seed carries a chemotype of its own");
        context.assertTrue(Chemotype.ofMatter(new ItemStack(ModItems.BELDIA_BUDS), registries) == Chemotype.BALANCED,
                "a Beldía bud no longer reads its strain");

        // A bad packet reads a value rather than throwing.
        ByteBuf wire = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(wire, 99);
        ByteBufCodecs.VAR_INT.encode(wire, 99);
        context.assertTrue(Chemotype.PACKET_CODEC.decode(wire) == Chemotype.THC_DOMINANT, "an unknown chemotype id");
        context.assertTrue(PotencyBand.PACKET_CODEC.decode(wire) == PotencyBand.LOW, "an unknown band id");
        context.succeed();
    }

    /** A creative pick-block hands back the block of the chemotype and mark it is. */
    public static void aStampedBlockPicksAsItIs(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(SPOT);
        pick(context, ModBlocks.HEMP_LEAVES.defaultBlockState().setValue(ChemotypeBlocks.CHEMOTYPE, Chemotype.CBD_DOMINANT)
                .setValue(HempLeavesBlock.HIGH_POTENCY, true), Chemotype.CBD_DOMINANT, PotencyBand.HIGH);
        pick(context, ModBlocks.HASHISH_BAR.defaultBlockState().setValue(ChemotypeBlocks.CHEMOTYPE, Chemotype.BALANCED),
                Chemotype.BALANCED, PotencyBand.MID);
        pick(context, ModBlocks.CHARAS_BALL.defaultBlockState().setValue(ChemotypeBlocks.CHEMOTYPE, Chemotype.CBD_DOMINANT),
                Chemotype.CBD_DOMINANT, PotencyBand.MID);
        // A plain block picks the stack 2.1 picked.
        world.setBlock(pos, ModBlocks.HEMP_LEAVES.defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack plain = world.getBlockState(pos).getCloneItemStack(world, pos, true);
        context.assertTrue(ItemStack.isSameItemSameComponents(plain, new ItemStack(ModBlocks.HEMP_LEAVES)),
                "a plain Block of Hemp Leaves picked " + plain.getComponentsPatch());
        context.succeed();
    }

    private static void pick(GameTestHelper context, BlockState state, Chemotype chemotype, PotencyBand mark) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(SPOT);
        world.setBlock(pos, state, Block.UPDATE_CLIENTS);
        ItemStack picked = world.getBlockState(pos).getCloneItemStack(world, pos, true);
        context.assertTrue(picked.is(state.getBlock().asItem()) && ChemotypeBlocks.ofBlockItem(picked) == chemotype
                        && HempLeavesBlock.markOfBlockItem(picked) == mark,
                state + " picked " + picked + " " + picked.getComponentsPatch());
    }

    /** Bhang, the third door into an edible, turns away the two chemotypes the oven and the Infuser do. */
    public static void bhangRefusesHempWithoutThcOrCbd(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        for (Chemotype chemotype : Chemotype.values()) {
            ItemStack hemp = Chemotype.stamp(new ItemStack(ModItems.DECARBOXYLATED_HEMP), chemotype);
            CraftingInput grid = CraftingInput.of(2, 2, List.of(hemp, hemp.copy(), new ItemStack(Items.MILK_BUCKET),
                    new ItemStack(Items.SUGAR)));
            ItemStack bhang = world.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid, world)
                    .map(entry -> entry.value().assemble(grid)).orElse(ItemStack.EMPTY);
            context.assertTrue(bhang.is(ModItems.BHANG_BUCKET) == chemotype.infusable(),
                    "two " + chemotype + " decarboxylated hemp and milk made " + bhang);
        }
        context.succeed();
    }

    /**
     * Every bud chars by a recipe of its own whose written result already carries its strain's
     * chemotype, so a machine that copies the result instead of assembling it makes the same thing.
     */
    public static void everyBudScorchesByItsOwnRecipe(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        int seen = 0;
        for (var strain : Strain.all(world.registryAccess())) {
            if (strain.value().wildFlower().isEmpty()) {
                continue;
            }
            ItemStack bud = new ItemStack(strain.value().buds());
            SingleRecipeInput input = new SingleRecipeInput(bud);
            RecipeHolder<?> recipe = world.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, world).orElse(null);
            context.assertTrue(recipe != null && recipe.value() instanceof StampedSmeltingRecipe,
                    "no stamped smelting recipe for " + bud + ": " + recipe);
            StampedSmeltingRecipe smelting = (StampedSmeltingRecipe) recipe.value();
            context.assertTrue(smelting.input().items().count() == 1,
                    recipe.id().identifier() + " takes more than its own bud, so its result cannot say its chemotype");
            ItemStack copied = smelting.template().create();
            context.assertTrue(copied.is(ModItems.SCORCHED_HEMP) && Chemotype.of(copied) == strain.value().chemotype(),
                    recipe.id().identifier() + "'s own result is " + Chemotype.of(copied) + " scorched hemp, the bud is "
                            + strain.value().chemotype());
            context.assertTrue(ItemStack.isSameItemSameComponents(copied, smelting.assemble(input)),
                    recipe.id().identifier() + "'s assembled result differs from its own");
            seen++;
        }
        context.assertTrue(seen >= 7, "saw only " + seen + " bud strains");
        context.succeed();
    }

    /**
     * One butter, one answer: a cake keeps both tiers its butter gave, and a bite queues what a cookie
     * of the same butter does.
     */
    public static void theSpaceCakeCarriesBothTiers(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        context.setBlock(FLOOR, net.minecraft.world.level.block.Blocks.STONE);
        int[][] tiers = {{2, 0}, {1, 3}, {0, 2}, {3, 3}};
        for (int[] pair : tiers) {
            ServerPlayer player = Players.survival(context);
            ItemStack cake = new ItemStack(ModBlocks.SPACE_CAKE);
            if (pair[0] > 0) {
                cake.set(ModComponents.POTENCY, pair[0]);
            }
            if (pair[1] > 0) {
                cake.set(ModComponents.CBD_POTENCY, pair[1]);
            }
            cake.set(ModComponents.QUALITY, Quality.CLEAN);
            BlockPos floor = context.absolutePos(FLOOR);
            BlockPos pos = floor.above();
            world.removeBlock(pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, cake);
            cake.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
            BlockState placed = world.getBlockState(pos);
            context.assertTrue(placed.is(ModBlocks.SPACE_CAKE) && placed.getValue(SpaceCakeBlock.POTENCY) == pair[0]
                            && placed.getValue(SpaceCakeBlock.CBD_POTENCY) == pair[1],
                    "a cake of THC " + pair[0] + " and CBD " + pair[1] + " placed as " + placed);
            player.getFoodData().setFoodLevel(10);
            placed.useWithoutItem(world, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            List<MobEffectInstance> bitten = new ArrayList<>();
            for (EdibleScheduler.Pending pending : EdibleScheduler.pending(player)) {
                bitten.add(pending.effect());
            }
            List<MobEffectInstance> cookie = new ArrayList<>();
            for (EdibleEffects.Dose dose : EdibleEffects.bundle(EdibleBundle.of(world.registryAccess()), pair[0], pair[1], Quality.CLEAN, 0)) {
                cookie.add(dose.effect());
            }
            // Through the policy every edible's doses pass on their way to the queue (the buff cap).
            cookie = EffectPolicy.filterKeepingDuration(cookie);
            context.assertValueEqual(describe(bitten), describe(cookie),
                    "a bite of a cake of THC " + pair[0] + " and CBD " + pair[1] + " against a cookie of the same butter");
        }
        context.succeed();
    }

    private static List<String> describe(List<MobEffectInstance> effects) {
        List<String> out = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            out.add(effect.getEffect().getRegisteredName() + " " + effect.getAmplifier() + " " + effect.getDuration());
        }
        out.sort(String::compareTo);
        return out;
    }

    /**
     * The oven's comparator reads a 2.1-shaped oven: the fuel, three trays and the three outputs pooled
     * as one slot, through vanilla's formula, so every load a 2.1 oven held gives 2.1's signal.
     */
    public static void theOvensComparatorReadsA21Oven(GameTestHelper context) {
        context.setBlock(SPOT, ModBlocks.DECARBOXYLATOR);
        DecarboxylatorBlockEntity oven = context.getBlockEntity(SPOT, DecarboxylatorBlockEntity.class);
        int[][] loads = {
                // fuel, tray 1, tray 2, tray 3, output 1, output 2, output 3
                {0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 32, 0, 0},
                {0, 0, 0, 0, 64, 0, 0},
                {0, 0, 0, 0, 0, 0, 1},
                {16, 8, 8, 8, 20, 20, 20},
                {0, 0, 0, 0, 64, 64, 64},
                {64, 64, 64, 64, 64, 64, 64},
        };
        int[] expected = {0, 2, 3, 1, -1, 3, 15};
        for (int i = 0; i < loads.length; i++) {
            int[] load = loads[i];
            oven.setItem(DecarboxylatorBlockEntity.FUEL_SLOT, count(Items.COAL, load[0]));
            for (int tray = 0; tray < DecarboxylatorBlockEntity.TRAY_COUNT; tray++) {
                oven.setItem(DecarboxylatorBlockEntity.FIRST_TRAY_SLOT + tray, count(ModItems.HEMP_LEAF, load[1 + tray]));
                oven.setItem(DecarboxylatorBlockEntity.OUTPUT_SLOTS[tray], count(ModItems.DECARBOXYLATED_HEMP, load[4 + tray]));
            }
            // What a 2.1 oven would hold: the same fuel and trays, the outputs in its one output slot.
            Container old = new SimpleContainer(5);
            old.setItem(0, count(Items.COAL, load[0]));
            for (int tray = 0; tray < 3; tray++) {
                old.setItem(1 + tray, count(ModItems.HEMP_LEAF, load[1 + tray]));
            }
            old.setItem(4, count(ModItems.DECARBOXYLATED_HEMP, Math.min(64, load[4] + load[5] + load[6])));
            int signal = context.getLevel().getBlockState(context.absolutePos(SPOT))
                    .getAnalogOutputSignal(context.getLevel(), context.absolutePos(SPOT), Direction.NORTH);
            context.assertValueEqual(signal, AbstractContainerMenu.getRedstoneSignalFromContainer(old),
                    "the comparator on load " + i + " against a 2.1 oven holding it");
            if (expected[i] >= 0) {
                context.assertValueEqual(signal, expected[i], "the comparator on load " + i);
            }
        }
        context.succeed();
    }

    private static ItemStack count(Item item, int count) {
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private StampedMatterGameTest() {
    }
}

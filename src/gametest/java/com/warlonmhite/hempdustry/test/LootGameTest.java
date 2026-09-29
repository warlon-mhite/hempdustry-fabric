package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * What hemp's blocks give back when broken.
 */
public final class LootGameTest {
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    /**
     * A Sifting Box broken with a batch ready gives the batch back, as a full composter gives its bone
     * meal — which batch following the box's state exactly as a click would — and a box still filling
     * gives only itself.
     */
    public static void aReadyBoxGivesItsBatch(TestContext context) {
        BlockState ready = ModBlocks.SIFTING_BOX.getDefaultState().with(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL);
        assertDrops(context, ready.with(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.PLANT).with(SiftingBoxBlock.FILLED, false),
                ModItems.KIEF, "a ready dry sift");
        assertDrops(context, ready.with(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.PLANT).with(SiftingBoxBlock.FILLED, true),
                ModItems.BUBBLE_HASH, "a ready wash");
        assertDrops(context, ready.with(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.KIEF),
                ModItems.FILTERED_KIEF, "a ready re-sift");
        List<ItemStack> filling = Block.getDroppedStacks(
                ready.with(SiftingBoxBlock.LEVEL, SiftingBoxBlock.FULL_LEVEL), context.getWorld(), context.getAbsolutePos(AT), null);
        context.assertTrue(filling.size() == 1 && filling.getFirst().isOf(ModBlocks.SIFTING_BOX.asItem()),
                "a box still filling dropped " + filling + " instead of only itself");
        context.complete();
    }

    /**
     * A Decarboxylator placed from an item an anvil renamed keeps the name: on its screen, through a
     * save and a load, and back onto the item when broken — vanilla's furnace, in all three.
     */
    public static void aNamedMachineKeepsItsName(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(AT);
        context.setBlockState(AT, ModBlocks.DECARBOXYLATOR);
        ItemStack named = new ItemStack(ModBlocks.DECARBOXYLATOR);
        named.set(DataComponentTypes.CUSTOM_NAME, Text.literal("The Oven"));
        DecarboxylatorBlockEntity machine = context.getBlockEntity(AT, DecarboxylatorBlockEntity.class);
        machine.readComponents(named);

        context.assertTrue(machine.getDisplayName().getString().equals("The Oven"),
                "a renamed Decarboxylator's screen is titled " + machine.getDisplayName().getString());
        BlockEntity reloaded = BlockEntity.createFromNbt(pos, machine.getCachedState(),
                machine.createNbtWithIdentifyingData(world.getRegistryManager()), world.getRegistryManager());
        context.assertTrue(reloaded instanceof DecarboxylatorBlockEntity loaded
                        && loaded.getDisplayName().getString().equals("The Oven"),
                "the Decarboxylator's name did not survive a save and a load");
        List<ItemStack> drops = Block.getDroppedStacks(machine.getCachedState(), world, pos, machine);
        context.assertTrue(drops.size() == 1 && Text.literal("The Oven").equals(drops.getFirst().get(DataComponentTypes.CUSTOM_NAME)),
                "a broken renamed Decarboxylator dropped " + drops + " without its name");
        context.complete();
    }

    // ----- helpers -----

    private static void assertDrops(TestContext context, BlockState state, Item batch, String description) {
        List<ItemStack> drops = Block.getDroppedStacks(state, context.getWorld(), context.getAbsolutePos(AT), null);
        boolean box = drops.stream().anyMatch(s -> s.isOf(ModBlocks.SIFTING_BOX.asItem()));
        int pieces = drops.stream().filter(s -> s.isOf(batch)).mapToInt(ItemStack::getCount).sum();
        context.assertTrue(box && pieces == SiftingBoxBlock.YIELD && drops.size() == 2,
                description + " broken dropped " + drops + ", expected the box and " + SiftingBoxBlock.YIELD + " " + batch);
    }
}

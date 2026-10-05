package com.warlonmhite.hempdustry.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.storage.NbtReadView;
import net.minecraft.test.TestContext;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockPos;

/**
 * A broken Infuser hands back the hemp in its slots <em>and</em> the hemp already drawn into its
 * batch, and never the uncollected cannabutter preview.
 *
 * <p>Since 1.21.5 the world removes a block entity before it calls the block's
 * {@code onStateReplaced}, so a spill written there finds nothing. Until 2.0.3 that is where the
 * Infuser's was: vanilla still scattered the slots on its own, preview included, and the batch —
 * up to 24 hemp — was simply gone.
 */
public final class InfuserBreakGameTest {

    private static final BlockPos SIMMERING = new BlockPos(1, 1, 1);
    private static final BlockPos READY = new BlockPos(3, 1, 1);

    public static void aBrokenInfuserSpillsItsBatch(TestContext context) {
        InfuserBlockEntity simmering = place(context, SIMMERING, InfuserBlockEntity.minTime() / 2);
        simmering.setStack(InfuserBlockEntity.FIRST_HEMP_SLOT, new ItemStack(ModItems.DECARBOXYLATED_HEMP, 3));
        context.getWorld().breakBlock(context.getAbsolutePos(SIMMERING), false);
        context.assertEquals(8, count(context, ModItems.DECARBOXYLATED_HEMP),
                "the hemp spilled by a simmering tub with 3 in its slot and 5 in its batch");

        InfuserBlockEntity ready = place(context, READY, InfuserBlockEntity.minTime());
        ready.setStack(InfuserBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.CANNABUTTER));
        context.getWorld().breakBlock(context.getAbsolutePos(READY), false);
        context.assertEquals(0, count(context, ModItems.CANNABUTTER), "the cannabutter a ready tub dropped");
        context.assertEquals(13, count(context, ModItems.DECARBOXYLATED_HEMP),
                "the hemp on the ground once the ready tub, 5 in its batch, was broken too");
        context.complete();
    }

    /** A tub with milk in and five unwashed hemp absorbed, as a save would hold it. */
    private static InfuserBlockEntity place(TestContext context, BlockPos pos, int progress) {
        context.setBlockState(pos, ModBlocks.INFUSER);
        InfuserBlockEntity infuser = context.getBlockEntity(pos, InfuserBlockEntity.class);
        try {
            infuser.read(NbtReadView.create(ErrorReporter.EMPTY, context.getWorld().getRegistryManager(),
                    StringNbtReader.readCompound("{Progress:" + progress + ",HaveMilk:1b,BatchUnwashed:5}")));
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
        return infuser;
    }

    private static int count(TestContext context, Item item) {
        int total = 0;
        for (ItemEntity entity : context.getEntities(EntityType.ITEM)) {
            if (entity.getStack().isOf(item)) {
                total += entity.getStack().getCount();
            }
        }
        return total;
    }
}

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
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;

/**
 * A broken Infuser hands back the hemp in its slots <em>and</em> the hemp already drawn into its
 * batch, and never the uncollected cannabutter preview.
 *
 * <p>From 1.21.5 the world removes a block entity before it calls the block's
 * {@code onStateReplaced}, so a spill written there finds nothing: on the 1.21.11 build, until
 * 2.0.3, the batch — up to 24 hemp — was simply gone. On this version the block entity is still
 * there when the block spills, so that bug never happened here; the batch and the preview are held
 * to the same rule all the same.
 */
public final class InfuserBreakGameTest implements FabricGameTest {

    private static final BlockPos SIMMERING = new BlockPos(1, 1, 1);
    private static final BlockPos READY = new BlockPos(3, 1, 1);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void aBrokenInfuserSpillsItsBatch(TestContext context) {
        InfuserBlockEntity simmering = place(context, SIMMERING, InfuserBlockEntity.minTime() / 2);
        simmering.setStack(InfuserBlockEntity.FIRST_HEMP_SLOT, new ItemStack(ModItems.DECARBOXYLATED_HEMP, 3));
        context.getWorld().breakBlock(context.getAbsolutePos(SIMMERING), false);
        context.assertEquals(count(context, ModItems.DECARBOXYLATED_HEMP), 8,
                "the hemp spilled by a simmering tub with 3 in its slot and 5 in its batch");

        InfuserBlockEntity ready = place(context, READY, InfuserBlockEntity.minTime());
        ready.setStack(InfuserBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.CANNABUTTER));
        context.getWorld().breakBlock(context.getAbsolutePos(READY), false);
        context.assertEquals(count(context, ModItems.CANNABUTTER), 0, "the cannabutter a ready tub dropped");
        context.assertEquals(count(context, ModItems.DECARBOXYLATED_HEMP), 13,
                "the hemp on the ground once the ready tub, 5 in its batch, was broken too");
        context.complete();
    }

    /** A tub with milk in and five unwashed hemp absorbed, as a save would hold it. */
    private static InfuserBlockEntity place(TestContext context, BlockPos pos, int progress) {
        context.setBlockState(pos, ModBlocks.INFUSER);
        InfuserBlockEntity infuser = context.<InfuserBlockEntity>getBlockEntity(pos);
        try {
            infuser.read(StringNbtReader.parse("{Progress:" + progress + ",HaveMilk:1b,BatchUnwashed:5}"),
                    context.getWorld().getRegistryManager());
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

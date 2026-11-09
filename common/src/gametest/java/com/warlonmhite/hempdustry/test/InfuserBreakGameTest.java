package com.warlonmhite.hempdustry.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.Chemotype;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * A broken Infuser hands back the hemp in its slots <em>and</em> the hemp already drawn into its
 * batch, and never the uncollected cannabutter preview.
 *
 * <p>Since 1.21.5 the world removes a block entity before it calls the block's
 * {@code affectNeighborsAfterRemoval}, so a spill written there finds nothing. Until 2.0.3 that is where the
 * Infuser's was: vanilla still scattered the slots on its own, preview included, and the batch —
 * up to 24 hemp — was simply gone.
 */
public final class InfuserBreakGameTest {

    private static final BlockPos SIMMERING = new BlockPos(1, 1, 1);
    private static final BlockPos READY = new BlockPos(3, 1, 1);
    private static final BlockPos MIXED = new BlockPos(1, 2, 3);

    public static void aBrokenInfuserSpillsItsBatch(GameTestHelper context) {
        InfuserBlockEntity simmering = place(context, SIMMERING, InfuserBlockEntity.minTime() / 2);
        simmering.setItem(InfuserBlockEntity.FIRST_HEMP_SLOT, new ItemStack(ModItems.DECARBOXYLATED_HEMP, 3));
        context.getLevel().destroyBlock(context.absolutePos(SIMMERING), false);
        context.assertValueEqual(8, count(context, ModItems.DECARBOXYLATED_HEMP),
                "the hemp spilled by a simmering tub with 3 in its slot and 5 in its batch");

        InfuserBlockEntity ready = place(context, READY, InfuserBlockEntity.minTime());
        ready.setItem(InfuserBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.CANNABUTTER));
        context.getLevel().destroyBlock(context.absolutePos(READY), false);
        context.assertValueEqual(0, count(context, ModItems.CANNABUTTER), "the cannabutter a ready tub dropped");
        context.assertValueEqual(13, count(context, ModItems.DECARBOXYLATED_HEMP),
                "the hemp on the ground once the ready tub, 5 in its batch, was broken too");
        context.succeed();
    }

    /**
     * A mixed batch spills each chemotype as it went in, through a save and a load. The batch decides
     * the butter by its dosage; a spill is the ingredients handed back, so the dosage has no say in
     * it. Stamping everything with the batch's chemotype turned CBD and balanced hemp into THC hemp
     * for the price of a milk bucket.
     */
    public static void aMixedBatchSpillsEachChemotypeAsItWentIn(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        context.setBlock(MIXED.below(), Blocks.CAMPFIRE);
        InfuserBlockEntity tub = place(context, MIXED, "{Progress:0,HaveMilk:1b}");
        int[] slots = InfuserBlockEntity.HEMP_SLOTS;
        tub.setItem(slots[0], new ItemStack(ModItems.DECARBOXYLATED_HEMP, 1));
        tub.setItem(slots[1], Chemotype.stamp(new ItemStack(ModItems.DECARBOXYLATED_HEMP, 2), Chemotype.CBD_DOMINANT));
        tub.setItem(slots[2], Chemotype.stamp(new ItemStack(ModItems.DECARBOXYLATED_HEMP, 2), Chemotype.BALANCED));
        tub.setItem(slots[3], Chemotype.stamp(new ItemStack(ModItems.WASHED_DECARBOXYLATED_HEMP, 1), Chemotype.BALANCED));
        BlockPos pos = context.absolutePos(MIXED);
        for (int tick = 0; tick < InfuserBlockEntity.absorbInterval() * 6; tick++) {
            tub.tick(world, pos, tub.getBlockState());
        }
        for (int slot : slots) {
            context.assertTrue(tub.getItem(slot).isEmpty(), "the tub left " + tub.getItem(slot) + " in slot " + slot);
        }
        context.assertTrue(tub.chemotype() == Chemotype.BALANCED, "the mixed batch is " + tub.chemotype());

        BlockEntity reloaded = BlockEntity.loadStatic(pos, tub.getBlockState(),
                tub.saveWithFullMetadata(world.registryAccess()), world.registryAccess());
        world.setBlockEntity(reloaded);
        world.destroyBlock(pos, false);
        Map<String, Integer> spilled = new TreeMap<>();
        for (ItemEntity entity : context.getEntities(EntityTypes.ITEM)) {
            ItemStack stack = entity.getItem();
            spilled.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath() + " " + Chemotype.of(stack),
                    stack.getCount(), Integer::sum);
        }
        context.assertValueEqual(spilled, Map.of(
                        "decarboxylated_hemp thc_dominant", 1,
                        "decarboxylated_hemp cbd_dominant", 2,
                        "decarboxylated_hemp balanced", 2,
                        "washed_decarboxylated_hemp balanced", 1),
                "what the broken tub spilled, by item and chemotype");
        context.succeed();
    }

    /** A tub with milk in and five unwashed hemp absorbed, as a save would hold it. */
    static InfuserBlockEntity place(GameTestHelper context, BlockPos pos, int progress) {
        return place(context, pos, "{Progress:" + progress + ",HaveMilk:1b,BatchUnwashed:5}");
    }

    /** A tub read in from {@code snbt}, as a save would hold it. */
    static InfuserBlockEntity place(GameTestHelper context, BlockPos pos, String snbt) {
        context.setBlock(pos, ModBlocks.INFUSER);
        InfuserBlockEntity infuser = context.getBlockEntity(pos, InfuserBlockEntity.class);
        try {
            infuser.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, context.getLevel().registryAccess(),
                    TagParser.parseCompoundFully(snbt)));
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
        return infuser;
    }

    private static int count(GameTestHelper context, Item item) {
        int total = 0;
        for (ItemEntity entity : context.getEntities(EntityTypes.ITEM)) {
            if (entity.getItem().is(item)) {
                total += entity.getItem().getCount();
            }
        }
        return total;
    }
}

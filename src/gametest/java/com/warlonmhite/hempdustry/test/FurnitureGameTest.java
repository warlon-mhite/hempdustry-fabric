package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Optional;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;

/**
 * The hemp bed and the parquet. Each thing tested here is a registration that fails without a
 * sound: a villager that never claims the bed, a fireproof set that quietly burns in a furnace.
 *
 * <p>The shelf and the harness of the 1.21.11 line are not here: vanilla has neither on 1.21.1.
 */
public final class FurnitureGameTest implements FabricGameTest {

    /**
     * A villager's home is a point of interest, and vanilla builds the home point from its own
     * sixteen bed heads. A hemp bed nobody added is a bed no villager will ever sleep in, with
     * nothing in any log. So the bed is placed for real and the world's own point-of-interest
     * record is asked — which is what a villager's bed sensor reads — a few ticks later, because
     * the world files a new point on the next server task, not inside {@code setBlockState}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void aHempBedIsAVillagersHome(TestContext context) {
        BlockPos head = new BlockPos(1, 1, 1);
        BlockPos foot = head.south();
        BlockState headState = ModBlocks.HEMP_BED.getDefaultState()
                .with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD);
        context.setBlockState(foot, headState.with(BedBlock.PART, BedPart.FOOT));
        context.setBlockState(head, headState);

        // Throws if the head has no block entity of ours, which is the one the bed is drawn from.
        context.<HempBedBlockEntity>getBlockEntity(head);
        context.assertTrue(headState.isIn(BlockTags.BEDS),
                "the hemp bed is not in #minecraft:beds, which a villager's sleep task checks");

        context.waitAndRun(5, () -> {
            var storage = context.getWorld().getPointOfInterestStorage();
            Optional<RegistryEntry<PointOfInterestType>> atHead = storage.getType(context.getAbsolutePos(head));
            context.assertTrue(atHead.isPresent() && atHead.get().matchesKey(PointOfInterestTypes.HOME),
                    "the hemp bed's head is not a villager's home point (found " + atHead + ")");
            context.assertTrue(storage.getType(context.getAbsolutePos(foot)).isEmpty(),
                    "the hemp bed's foot is a point of interest too — vanilla files a bed by its head only");
            context.complete();
        });
    }

    /**
     * The bed is cloth and burns, at hemp wool's rate; the parquet is the fireproof plank set and
     * burns neither in a fire nor in a furnace. An oak slab is asked too, so this cannot pass
     * against a fuel table that has simply lost every wooden block.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void theBedBurnsTheWoodDoesNot(TestContext context) {
        var fire = FlammableBlockRegistry.getDefaultInstance();
        var bed = fire.get(ModBlocks.HEMP_BED);
        var wool = fire.get(ModBlocks.HEMP_WOOL);
        context.assertTrue(bed != null && bed.getBurnChance() > 0 && bed.getSpreadChance() > 0,
                "the hemp bed does not burn — it is made of hemp cloth, the one hemp material that does");
        context.assertTrue(bed.equals(wool),
                "the hemp bed burns at " + bed.getBurnChance() + "/" + bed.getSpreadChance()
                        + " rather than at the hemp wool it is made of");

        for (Block wood : new Block[] {ModBlocks.HEMP_PARQUET,
                ModBlocks.HEMP_PARQUET_STAIRS, ModBlocks.HEMP_PARQUET_SLAB}) {
            var entry = fire.get(wood);
            context.assertTrue(entry == null || (entry.getBurnChance() == 0 && entry.getSpreadChance() == 0),
                    wood + " catches fire, and the hemp plank set is fireproof");
            context.assertFalse(AbstractFurnaceBlockEntity.canUseAsFuel(new ItemStack(wood)),
                    wood + " burns in a furnace, and the hemp plank set is fireproof");
        }
        context.assertTrue(AbstractFurnaceBlockEntity.canUseAsFuel(new ItemStack(Blocks.OAK_SLAB)),
                "an oak slab is not fuel either, so the check above proves nothing");
        context.complete();
    }
}

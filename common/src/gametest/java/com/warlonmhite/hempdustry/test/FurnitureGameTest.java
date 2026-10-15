package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.Optional;

/**
 * The hemp bed and the parquet. Each thing tested here is a registration that fails without a
 * sound: a villager that never claims the bed, a fireproof set that quietly burns in a furnace.
 *
 * <p>The shelf and the harness of the 1.21.11 line are not here: vanilla has neither on 1.21.1.
 */
public final class FurnitureGameTest {

    /**
     * A villager's home is a point of interest, and vanilla builds the home point from its own
     * sixteen bed heads. A hemp bed nobody added is a bed no villager will ever sleep in, with
     * nothing in any log. So the bed is placed for real and the world's own point-of-interest
     * record is asked — which is what a villager's bed sensor reads — a few ticks later, because
     * the world files a new point on the next server task, not inside {@code setBlock}.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aHempBedIsAVillagersHome(GameTestHelper context) {
        BlockPos head = new BlockPos(1, 1, 1);
        BlockPos foot = head.south();
        BlockState headState = ModBlocks.HEMP_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD);
        context.setBlock(foot, headState.setValue(BedBlock.PART, BedPart.FOOT));
        context.setBlock(head, headState);

        // Throws if the head has no block entity of ours, which is the one the bed is drawn from.
        context.<HempBedBlockEntity>getBlockEntity(head);
        context.assertTrue(headState.is(BlockTags.BEDS),
                "the hemp bed is not in #minecraft:beds, which a villager's sleep task checks");

        context.runAfterDelay(5, () -> {
            var storage = context.getLevel().getPoiManager();
            Optional<Holder<PoiType>> atHead = storage.getType(context.absolutePos(head));
            context.assertTrue(atHead.isPresent() && atHead.get().is(PoiTypes.HOME),
                    "the hemp bed's head is not a villager's home point (found " + atHead + ")");
            context.assertTrue(storage.getType(context.absolutePos(foot)).isEmpty(),
                    "the hemp bed's foot is a point of interest too — vanilla files a bed by its head only");
            context.succeed();
        });
    }

    /**
     * The bed is cloth and burns, at hemp wool's rate; the parquet is the fireproof plank set and
     * burns neither in a fire nor in a furnace. An oak slab is asked too, so this cannot pass
     * against a fuel table that has simply lost every wooden block.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void theBedBurnsTheWoodDoesNot(GameTestHelper context) {
        var bed = TestPlatform.INSTANCE.flammability(ModBlocks.HEMP_BED);
        var wool = TestPlatform.INSTANCE.flammability(ModBlocks.HEMP_WOOL);
        context.assertTrue(bed.igniteOdds() > 0 && bed.burnOdds() > 0,
                "the hemp bed does not burn — it is made of hemp cloth, the one hemp material that does");
        context.assertTrue(bed.equals(wool),
                "the hemp bed burns at " + bed.igniteOdds() + "/" + bed.burnOdds()
                        + " rather than at the hemp wool it is made of");

        for (Block wood : new Block[] {ModBlocks.HEMP_PARQUET,
                ModBlocks.HEMP_PARQUET_STAIRS, ModBlocks.HEMP_PARQUET_SLAB}) {
            var entry = TestPlatform.INSTANCE.flammability(wood);
            context.assertTrue(entry.igniteOdds() == 0 && entry.burnOdds() == 0,
                    wood + " catches fire, and the hemp plank set is fireproof");
            context.assertFalse(AbstractFurnaceBlockEntity.isFuel(new ItemStack(wood)),
                    wood + " burns in a furnace, and the hemp plank set is fireproof");
        }
        context.assertTrue(AbstractFurnaceBlockEntity.isFuel(new ItemStack(Blocks.OAK_SLAB)),
                "an oak slab is not fuel either, so the check above proves nothing");
        context.succeed();
    }
}

package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import java.util.Optional;

/**
 * The hemp bed, the shelf, the parquet and the harness. Each thing tested here is a registration that fails
 * without a sound: a villager that never claims the bed, a shelf that crashes the moment it is
 * placed, a fireproof set that quietly burns in a furnace.
 */
public final class FurnitureGameTest {

    /**
     * A villager's home is a point of interest, and vanilla builds the home point from its own
     * sixteen bed heads. A hemp bed nobody added is a bed no villager will ever sleep in, with
     * nothing in any log. So the bed is placed for real and the world's own point-of-interest
     * record is asked — which is what a villager's bed sensor reads — a few ticks later, because
     * the world files a new point on the next server task, not inside {@code setBlock}.
     */
    public static void aHempBedIsAVillagersHome(GameTestHelper context) {
        BlockPos head = new BlockPos(1, 1, 1);
        BlockPos foot = head.south();
        BlockState headState = ModBlocks.HEMP_BED.defaultBlockState()
                .setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD);
        context.setBlock(foot, headState.setValue(BedBlock.PART, BedPart.FOOT));
        context.setBlock(head, headState);

        // Throws if the head has no block entity of ours, which is the one the bed is drawn from.
        context.getBlockEntity(head, HempBedBlockEntity.class);
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
     * The bed is cloth and burns, at hemp wool's rate; the shelf and the parquet are the fireproof
     * plank set and burn neither in a fire nor in a furnace. The furnace half is the one that can
     * slip: the shelf sits in {@code #minecraft:wooden_shelves}, which vanilla makes fuel, and only
     * {@code #minecraft:non_flammable_wood} takes it out again. An oak shelf is asked too, so this
     * cannot pass against a fuel table that has simply lost every shelf.
     */
    public static void theBedBurnsTheWoodDoesNot(GameTestHelper context) {
        var bed = TestPlatform.INSTANCE.flammability(ModBlocks.HEMP_BED);
        var wool = TestPlatform.INSTANCE.flammability(ModBlocks.HEMP_WOOL);
        context.assertTrue(bed.igniteOdds() > 0 && bed.burnOdds() > 0,
                "the hemp bed does not burn — it is made of hemp cloth, the one hemp material that does");
        context.assertTrue(bed.equals(wool),
                "the hemp bed burns at " + bed.igniteOdds() + "/" + bed.burnOdds()
                        + " rather than at the hemp wool it is made of");

        for (Block wood : new Block[] {ModBlocks.HEMP_PLANKS_SHELF, ModBlocks.HEMP_PARQUET,
                ModBlocks.HEMP_PARQUET_STAIRS, ModBlocks.HEMP_PARQUET_SLAB,
                ModBlocks.HEMP_PLANKS, ModBlocks.HEMP_PLANKS_STAIRS, ModBlocks.HEMP_PLANKS_SLAB,
                ModBlocks.HEMP_PLANKS_FENCE, ModBlocks.HEMP_PLANKS_FENCE_GATE, ModBlocks.HEMP_PLANKS_DOOR,
                ModBlocks.HEMP_PLANKS_TRAPDOOR, ModBlocks.HEMP_PLANKS_BUTTON, ModBlocks.HEMP_PLANKS_PRESSURE_PLATE}) {
            var entry = TestPlatform.INSTANCE.flammability(wood);
            context.assertTrue(entry.igniteOdds() == 0 && entry.burnOdds() == 0,
                    wood + " catches fire, and the hemp plank set is fireproof");
            context.assertFalse(context.getLevel().fuelValues().isFuel(new ItemStack(wood)),
                    wood + " burns in a furnace, and the hemp plank set is fireproof");
        }
        context.assertTrue(context.getLevel().fuelValues().isFuel(new ItemStack(Blocks.OAK_SHELF)),
                "an oak shelf is not fuel either, so the check above proves nothing");
        context.succeed();
    }

    /**
     * Hempcrete burns no longer than the stems it is made from, and its powder does not burn at all.
     *
     * <p>Four stems make two Hempcrete, and each burned 200 ticks against a stem's 50, so crafting a
     * stem into Hempcrete quadrupled its fuel, and a Block of Hempcrete Powder at 1800 ticks made
     * the mod's best fuel out of a building material. Real hempcrete is shiv bound in lime, rated
     * fire-resistant, and vanilla's concrete powder is no fuel either.
     */
    public static void hempcreteBurnsNoLongerThanItsStems(GameTestHelper context) {
        int stem = context.getLevel().fuelValues().burnDuration(new ItemStack(ModItems.HEMP_STEM));
        int hempcrete = context.getLevel().fuelValues().burnDuration(new ItemStack(ModItems.HEMPCRETE));
        context.assertTrue(stem > 0, "a hemp stem is no fuel, so the check below proves nothing");
        context.assertTrue(2 * hempcrete <= 4 * stem,
                "two Hempcrete burn " + 2 * hempcrete + " ticks, and the four stems they are made of " + 4 * stem);
        context.assertTrue(context.getLevel().fuelValues().burnDuration(new ItemStack(ModBlocks.HEMPCRETE_POWDER_BLOCK)) == 0,
                "a Block of Hempcrete Powder burns in a furnace");
        context.succeed();
    }

    /**
     * The Hemp Harness goes on a happy ghast and nothing else, and draws its ropes. Two separate
     * registrations, both silent when missing: the equip data decides who may wear it (a harness
     * without it is a plain item a ghast ignores), and {@code #minecraft:harnesses} is what the
     * client asks before drawing the ropes. A llama is asked too, so this cannot pass against equip
     * data that lets anything wear it.
     */
    public static void aHempHarnessFitsAHappyGhast(GameTestHelper context) {
        ItemStack harness = new ItemStack(ModItems.HEMP_HARNESS);
        HappyGhast ghast = context.spawn(EntityType.HAPPY_GHAST, new BlockPos(2, 2, 2));
        context.assertTrue(ghast.isEquippableInSlot(harness, EquipmentSlot.BODY),
                "a happy ghast will not wear the Hemp Harness");
        context.assertTrue(harness.is(ItemTags.HARNESSES),
                "the Hemp Harness is not in #minecraft:harnesses, so a ghast wearing it draws no ropes");
        var llama = context.spawn(EntityType.LLAMA, new BlockPos(5, 1, 5));
        llama.setTamed(true);
        context.assertFalse(llama.isEquippableInSlot(harness, EquipmentSlot.BODY),
                "a llama will wear the Hemp Harness, so its equip data lets in more than a ghast");
        context.succeed();
    }

    /**
     * The shelf is vanilla's {@code ShelfBlock} and makes vanilla's shelf block entity, whose type
     * lists the blocks it may stand in. A shelf left off that list does not fail quietly: the block
     * entity refuses to exist and the world throws the moment one is placed.
     */
    public static void aHempShelfIsAVanillaShelf(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlock(pos, ModBlocks.HEMP_PLANKS_SHELF);
        // Throws if the shelf stands without vanilla's shelf block entity.
        context.getBlockEntity(pos, ShelfBlockEntity.class);
        context.succeed();
    }
}

package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ShelfBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

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
     * the world files a new point on the next server task, not inside {@code setBlockState}.
     */
    public static void aHempBedIsAVillagersHome(TestContext context) {
        BlockPos head = new BlockPos(1, 1, 1);
        BlockPos foot = head.south();
        BlockState headState = ModBlocks.HEMP_BED.getDefaultState()
                .with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD);
        context.setBlockState(foot, headState.with(BedBlock.PART, BedPart.FOOT));
        context.setBlockState(head, headState);

        // Throws if the head has no block entity of ours, which is the one the bed is drawn from.
        context.getBlockEntity(head, HempBedBlockEntity.class);
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
     * The bed is cloth and burns, at hemp wool's rate; the shelf and the parquet are the fireproof
     * plank set and burn neither in a fire nor in a furnace. The furnace half is the one that can
     * slip: the shelf sits in {@code #minecraft:wooden_shelves}, which vanilla makes fuel, and only
     * {@code #minecraft:non_flammable_wood} takes it out again. An oak shelf is asked too, so this
     * cannot pass against a fuel table that has simply lost every shelf.
     */
    public static void theBedBurnsTheWoodDoesNot(TestContext context) {
        var fire = FlammableBlockRegistry.getDefaultInstance();
        var bed = fire.get(ModBlocks.HEMP_BED);
        var wool = fire.get(ModBlocks.HEMP_WOOL);
        context.assertTrue(bed != null && bed.getBurnChance() > 0 && bed.getSpreadChance() > 0,
                "the hemp bed does not burn — it is made of hemp cloth, the one hemp material that does");
        context.assertTrue(bed.equals(wool),
                "the hemp bed burns at " + bed.getBurnChance() + "/" + bed.getSpreadChance()
                        + " rather than at the hemp wool it is made of");

        for (Block wood : new Block[] {ModBlocks.HEMP_PLANKS_SHELF, ModBlocks.HEMP_PARQUET,
                ModBlocks.HEMP_PARQUET_STAIRS, ModBlocks.HEMP_PARQUET_SLAB}) {
            var entry = fire.get(wood);
            context.assertTrue(entry == null || (entry.getBurnChance() == 0 && entry.getSpreadChance() == 0),
                    wood + " catches fire, and the hemp plank set is fireproof");
            context.assertFalse(context.getWorld().getFuelRegistry().isFuel(new ItemStack(wood)),
                    wood + " burns in a furnace, and the hemp plank set is fireproof");
        }
        context.assertTrue(context.getWorld().getFuelRegistry().isFuel(new ItemStack(Blocks.OAK_SHELF)),
                "an oak shelf is not fuel either, so the check above proves nothing");
        context.complete();
    }

    /**
     * The Hemp Harness goes on a happy ghast and nothing else, and draws its ropes. Two separate
     * registrations, both silent when missing: the equip data decides who may wear it (a harness
     * without it is a plain item a ghast ignores), and {@code #minecraft:harnesses} is what the
     * client asks before drawing the ropes. A llama is asked too, so this cannot pass against equip
     * data that lets anything wear it.
     */
    public static void aHempHarnessFitsAHappyGhast(TestContext context) {
        ItemStack harness = new ItemStack(ModItems.HEMP_HARNESS);
        HappyGhastEntity ghast = context.spawnEntity(EntityType.HAPPY_GHAST, new BlockPos(2, 2, 2));
        context.assertTrue(ghast.canEquip(harness, EquipmentSlot.BODY),
                "a happy ghast will not wear the Hemp Harness");
        context.assertTrue(harness.isIn(ItemTags.HARNESSES),
                "the Hemp Harness is not in #minecraft:harnesses, so a ghast wearing it draws no ropes");
        var llama = context.spawnEntity(EntityType.LLAMA, new BlockPos(5, 1, 5));
        llama.setTame(true);
        context.assertFalse(llama.canEquip(harness, EquipmentSlot.BODY),
                "a llama will wear the Hemp Harness, so its equip data lets in more than a ghast");
        context.complete();
    }

    /**
     * The shelf is vanilla's {@code ShelfBlock} and makes vanilla's shelf block entity, whose type
     * lists the blocks it may stand in. A shelf left off that list does not fail quietly: the block
     * entity refuses to exist and the world throws the moment one is placed.
     */
    public static void aHempShelfIsAVanillaShelf(TestContext context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        context.setBlockState(pos, ModBlocks.HEMP_PLANKS_SHELF);
        // Throws if the shelf stands without vanilla's shelf block entity.
        context.getBlockEntity(pos, ShelfBlockEntity.class);
        context.complete();
    }
}

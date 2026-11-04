package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the device a placed bong was. It has to give back everything the item carried — durability,
 * enchantments, repair cost, a name from the anvil, the load in the bowl — so it keeps the item's
 * whole tag rather than a field per value: a value added to devices later rides through too. The bong
 * block hands it back to the item it drops ({@code BongBlock#getDrops}).
 */
public class BongBlockEntity extends BlockEntity {
    private static final String ITEM_TAG = "ItemTag";

    @Nullable
    private CompoundTag itemTag;

    public BongBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BONG, pos, state);
    }

    /** Keeps what the placed item carried. */
    public void takeFrom(ItemStack stack) {
        itemTag = stack.getTag() == null ? null : stack.getTag().copy();
        setChanged();
    }

    /** Gives it back to the item the block drops. */
    public void giveTo(ItemStack stack) {
        if (itemTag != null) {
            stack.setTag(itemTag.copy());
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemTag = nbt.contains(ITEM_TAG, Tag.TAG_COMPOUND) ? nbt.getCompound(ITEM_TAG) : null;
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (itemTag != null) {
            nbt.put(ITEM_TAG, itemTag.copy());
        }
    }
}

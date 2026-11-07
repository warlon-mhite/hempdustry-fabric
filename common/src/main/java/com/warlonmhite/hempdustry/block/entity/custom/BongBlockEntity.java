package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.util.Stacks;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds the device a placed bong was. It has to give back everything the item carried — durability,
 * enchantments, repair cost, a name from the anvil, the load in the bowl — so it keeps the item
 * itself rather than a field per value: a value added to devices later rides through too. The bong
 * block hands it back to the item it drops ({@code BongBlock#getDrops}).
 *
 * <p>It is kept as a whole item stack, not as the bare tag, because a stack is what Minecraft's data
 * fixer knows how to carry to a newer version: the data fixer's table names this field as an item
 * stack ({@code Schema1460Mixin}), so a world moved to 1.21.1 arrives with the device in that
 * version's own shape, ready for the block entity there to take.
 */
public class BongBlockEntity extends BlockEntity {
    private static final String ITEM = "Item";

    private ItemStack item = ItemStack.EMPTY;

    public BongBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BONG, pos, state);
    }

    /** Keeps what the placed item carried. */
    public void takeFrom(ItemStack stack) {
        item = Stacks.copyWithCount(stack, 1);
        setChanged();
    }

    /** Gives it back to the item the block drops. */
    public void giveTo(ItemStack stack) {
        if (item.hasTag()) {
            stack.setTag(item.getTag().copy());
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        item = nbt.contains(ITEM, Tag.TAG_COMPOUND) ? ItemStack.of(nbt.getCompound(ITEM)) : ItemStack.EMPTY;
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (!item.isEmpty()) {
            nbt.put(ITEM, item.save(new CompoundTag()));
        }
    }
}

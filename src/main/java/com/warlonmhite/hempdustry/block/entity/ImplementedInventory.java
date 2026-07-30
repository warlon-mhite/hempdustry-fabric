package com.warlonmhite.hempdustry.block.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link SidedInventory} built entirely from default methods on top of one {@link DefaultedList}
 * of stacks, so a block entity only has to supply the list.
 *
 * <p>This is the well-known community helper (originally by Juuz, CC0) that the Fabric docs and
 * every tutorial use; it is vendored here rather than pulled in because Fabric API doesn't ship it.
 * The sided defaults are permissive — override {@link #getAvailableSlots}, {@link #canInsert} and
 * {@link #canExtract} to actually restrict hopper access, which
 * {@link com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity} does.
 */
@FunctionalInterface
public interface ImplementedInventory extends SidedInventory {
    /** The backing list. Must return the same instance every call. */
    DefaultedList<ItemStack> getItems();

    static ImplementedInventory of(DefaultedList<ItemStack> items) {
        return () -> items;
    }

    static ImplementedInventory ofSize(int size) {
        return of(DefaultedList.ofSize(size, ItemStack.EMPTY));
    }

    // ----- SidedInventory -----

    @Override
    default int[] getAvailableSlots(Direction side) {
        int[] result = new int[getItems().size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    @Override
    default boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    default boolean canExtract(int slot, ItemStack stack, Direction side) {
        return true;
    }

    // ----- Inventory -----

    @Override
    default int size() {
        return getItems().size();
    }

    @Override
    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            if (!getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack removeStack(int slot, int count) {
        ItemStack result = Inventories.splitStack(getItems(), slot, count);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    default ItemStack removeStack(int slot) {
        return Inventories.removeStack(getItems(), slot);
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
    }

    @Override
    default void clear() {
        getItems().clear();
    }

    @Override
    default void markDirty() {
        // A BlockEntity subclass inherits its own concrete markDirty(), which wins over this.
    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    /** Convenience for {@link Inventory#getMaxCountPerStack()} callers that want the stack's own cap. */
    static int maxCountFor(Inventory inventory, ItemStack stack) {
        return Math.min(inventory.getMaxCountPerStack(), stack.getMaxCount());
    }
}

package com.warlonmhite.hempdustry.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link WorldlyContainer} built entirely from default methods on top of one {@link NonNullList}
 * of stacks, so a block entity only has to supply the list.
 *
 * <p>This is the well-known community helper (originally by Juuz, CC0) that the Fabric docs and
 * every tutorial use; it is vendored here rather than pulled in because Fabric API doesn't ship it.
 * The sided defaults are permissive — override {@link #getSlotsForFace}, {@link #canPlaceItemThroughFace} and
 * {@link #canTakeItemThroughFace} to actually restrict hopper access, which
 * {@link com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity} does.
 */
@FunctionalInterface
public interface ImplementedInventory extends WorldlyContainer {
    /** The backing list. Must return the same instance every call. */
    NonNullList<ItemStack> getItems();

    static ImplementedInventory of(NonNullList<ItemStack> items) {
        return () -> items;
    }

    static ImplementedInventory ofSize(int size) {
        return of(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    // ----- WorldlyContainer -----

    @Override
    default int[] getSlotsForFace(Direction side) {
        int[] result = new int[getItems().size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    @Override
    default boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return true;
    }

    @Override
    default boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    // ----- Container -----

    @Override
    default int getContainerSize() {
        return getItems().size();
    }

    @Override
    default boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack getItem(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(getItems(), slot, count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(getItems(), slot);
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
    }

    @Override
    default void clearContent() {
        getItems().clear();
    }

    @Override
    default void setChanged() {
        // A BlockEntity subclass inherits its own concrete setChanged(), which wins over this.
    }

    @Override
    default boolean stillValid(Player player) {
        return true;
    }

    /** Convenience for {@link Container#getMaxStackSize()} callers that want the stack's own cap. */
    static int maxCountFor(Container inventory, ItemStack stack) {
        return Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
    }
}

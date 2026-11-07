package com.warlonmhite.hempdustry.test;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * A crafting grid with nothing around it, what 1.21's {@code CraftingInput.of} makes: before 1.21 a
 * crafting recipe matches a crafting container, and this is one no menu shows.
 */
final class CraftingInput extends CraftingContainer {
    private static final AbstractContainerMenu NO_MENU = new AbstractContainerMenu(null, -1) {
        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    };

    private CraftingInput(int width, int height) {
        super(NO_MENU, width, height);
    }

    static CraftingInput of(int width, int height, List<ItemStack> stacks) {
        CraftingInput grid = new CraftingInput(width, height);
        for (int i = 0; i < stacks.size(); i++) {
            grid.setItem(i, stacks.get(i));
        }
        return grid;
    }

    int ingredientCount() {
        int count = 0;
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }
}

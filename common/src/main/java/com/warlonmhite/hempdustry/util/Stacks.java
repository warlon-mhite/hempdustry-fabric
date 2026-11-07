package com.warlonmhite.hempdustry.util;

import net.minecraft.world.item.ItemStack;

/** Small {@code ItemStack} helpers the newer lines' vanilla has and 1.18.2's does not. */
public final class Stacks {

    private Stacks() {
    }

    /** A copy of {@code stack} holding {@code count} items, as 1.20's {@code ItemStack#copyWithCount}. */
    public static ItemStack copyWithCount(ItemStack stack, int count) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }
}

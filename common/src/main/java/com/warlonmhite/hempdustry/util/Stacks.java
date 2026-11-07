package com.warlonmhite.hempdustry.util;

import net.minecraft.world.item.ItemStack;

/** Small {@code ItemStack} helpers the newer lines' vanilla has and 1.16.5's does not. */
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

    /** Whether two stacks hold the same item with the same tag, as 1.20's {@code isSameItemSameTags}. */
    public static boolean isSameItemSameTags(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && ItemStack.tagMatches(a, b);
    }
}

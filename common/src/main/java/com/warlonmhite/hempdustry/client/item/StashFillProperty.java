package com.warlonmhite.hempdustry.client.item;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.custom.stash.StashFill;
import com.warlonmhite.hempdustry.item.custom.StashJarItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * {@code hempdustry:stash_fill} — how full a Stash Jar is, as the placed block's {@code fill} state
 * reads it: 0 empty, 1 low, 2 mid, 3 full, against that jar's own capacity. The jars' client item
 * definitions dispatch on it, so a jar looks as full in the hand as it does on a shelf.
 */
public record StashFillProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<StashFillProperty> CODEC = MapCodec.unit(new StashFillProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel world, ItemOwner context, int seed) {
        if (!(stack.getItem() instanceof StashJarItem jar)) {
            return 0.0F;
        }
        return StashFill.of(StashJarItem.contentsOf(stack).count(), jar.capacity()).ordinal();
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return CODEC;
    }
}

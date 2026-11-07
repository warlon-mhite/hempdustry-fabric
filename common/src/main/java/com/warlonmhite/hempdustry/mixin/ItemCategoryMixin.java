package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.item.ModItemGroups;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Lets the mod put its items in its tab once they are all registered ({@code ModItemGroups.assignTab}):
 * a 1.18.2 item takes its tab from its properties as it is built, into a final field.
 */
@Mixin(Item.class)
public abstract class ItemCategoryMixin implements ModItemGroups.ItemCategoryAccess {

    @Shadow
    @Final
    @Mutable
    protected CreativeModeTab category;

    @Override
    public void hempdustry$setCategory(CreativeModeTab tab) {
        this.category = tab;
    }
}

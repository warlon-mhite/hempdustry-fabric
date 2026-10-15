package com.warlonmhite.hempdustry.neoforge.client;

import com.warlonmhite.hempdustry.item.ModArmorMaterials;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * An undyed hemp shirt or pair of harem pants, worn, in unbleached hemp rather than leather's brown:
 * NeoForge's armour layer asks the item for its colour when it has no dye, where Fabric's is the
 * vanilla call {@code ArmorFeatureRendererMixin} changes. A dyed one keeps its own colour.
 */
final class UndyedHempExtensions implements IClientItemExtensions {
    @Override
    public int getDefaultDyeColor(ItemStack stack) {
        return FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, ModArmorMaterials.HEMP_UNDYED_COLOR));
    }
}

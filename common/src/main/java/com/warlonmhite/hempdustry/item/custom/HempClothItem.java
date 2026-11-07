package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.item.ModArmorMaterials;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * The shirt and the harem pants: dyeable like leather, and undyed the colour of unbleached hemp rather
 * than leather's brown, which is what 1.20.1's {@code DyeableLeatherItem} hands back for any armour
 * that has not been dyed.
 */
public class HempClothItem extends DyeableArmorItem {
    public HempClothItem(ArmorMaterial material, EquipmentSlot slot, Properties properties) {
        super(material, slot, properties);
    }

    @Override
    public int getColor(ItemStack stack) {
        CompoundTag display = stack.getTagElement("display");
        return display != null && display.contains("color", 99)
                ? display.getInt("color")
                : ModArmorMaterials.HEMP_UNDYED_COLOR & 0xFFFFFF;
    }
}

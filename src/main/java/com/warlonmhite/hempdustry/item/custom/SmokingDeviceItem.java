package com.warlonmhite.hempdustry.item.custom;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

/**
 * An <em>empty</em> smoking device (wooden pipe / bong). It's damageable, repairable (with the
 * material it's crafted from) and enchantable. Pack it with a strain's buds in a crafting grid to
 * get the matching {@link PackedSmokingDeviceItem} — see {@code PackingRecipe}, which copies this
 * item's durability and enchantments onto the packed result so nothing is lost across the states.
 *
 * <p>Repairing is intentionally only possible in this empty state (the packed variants have no
 * repair hook).
 */
public class SmokingDeviceItem extends Item {
    private final DeviceType device;

    public SmokingDeviceItem(DeviceType device, Settings settings) {
        super(settings);
        this.device = device;
    }

    public DeviceType device() {
        return device;
    }

    @Override
    public int getEnchantability() {
        return device.enchantability();
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        // Vanilla-style material repair: pipe = its build material (planks), bong = glass.
        return switch (device) {
            case PIPE -> ingredient.isIn(ItemTags.PLANKS);
            case BONG -> ingredient.isOf(Items.GLASS);
        };
    }
}

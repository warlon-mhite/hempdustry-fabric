package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import java.util.EnumMap;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The hemp outfit's material. On 1.20.1 an armour material is a plain object rather than a registry
 * entry, and its worn texture is read off its name: {@code hempdustry:textures/models/armor/hemp_fiber_layer_*}
 * for the grey cloth the dye tints, {@code …_overlay} for the drawn layer on top, untinted.
 */
public enum ModArmorMaterials implements ArmorMaterial {
    HEMP_FIBER;

    /** The material every piece of the outfit is made of. */
    public static final ArmorMaterial HEMP_ARMOR_MATERIAL = HEMP_FIBER;

    /**
     * The colour an undyed shirt or pair of harem pants is drawn in: unbleached hemp, the one tint
     * that best gives back the shirt as it was drawn before it took dye. The dyeable textures are
     * grey and this tints them, as leather's brown tints leather's. On 1.20.1 vanilla asks the item for
     * the colour, worn and in the inventory alike, so the outfit's own item answers it
     * ({@link com.warlonmhite.hempdustry.item.custom.HempClothItem}).
     */
    public static final int HEMP_UNDYED_COLOR = 0xFFFFDFB7;

    private static final EnumMap<EquipmentSlot, Integer> DEFENSE = Util.make(new EnumMap<>(EquipmentSlot.class), map -> {
        map.put(EquipmentSlot.FEET, 1);
        map.put(EquipmentSlot.LEGS, 1);
        map.put(EquipmentSlot.CHEST, 2);
        map.put(EquipmentSlot.HEAD, 1);
    });

    /** Vanilla's durability per slot, which every material multiplies ({@code ArmorMaterials}). */
    private static final EnumMap<EquipmentSlot, Integer> HEALTH_PER_SLOT = Util.make(new EnumMap<>(EquipmentSlot.class), map -> {
        map.put(EquipmentSlot.FEET, 13);
        map.put(EquipmentSlot.LEGS, 15);
        map.put(EquipmentSlot.CHEST, 16);
        map.put(EquipmentSlot.HEAD, 11);
    });

    /**
     * Leather's: hemp is the hard-wearing cloth (sailcloth, canvas), so it wears as long as leather
     * does, and only leather's protection stays out of reach. It was 3 until 2.1.
     */
    private static final int DURABILITY_MULTIPLIER = 5;

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return HEALTH_PER_SLOT.get(slot) * DURABILITY_MULTIPLIER;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return DEFENSE.get(slot);
    }

    @Override
    public int getEnchantmentValue() {
        return 20;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.HEMP_FIBER);
    }

    @Override
    public String getName() {
        return Hempdustry.MOD_ID + ":hemp_fiber";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}

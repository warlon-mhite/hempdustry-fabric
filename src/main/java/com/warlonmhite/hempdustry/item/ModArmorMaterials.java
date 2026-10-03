package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;

public class ModArmorMaterials {

    /**
     * Since 1.21.4 an armour material is a plain record rather than a registry entry, and the
     * texture layers it used to name directly live in an <b>equipment asset</b> — a JSON at
     * {@code assets/hempdustry/equipment/hemp_fiber.json} pointing at
     * {@code textures/entity/equipment/humanoid[_leggings]/hemp_fiber.png}. The key below is what
     * ties the item to that file; the old {@code textures/models/armor/*_layer_N.png} pair is gone.
     */
    private static final RegistryKey<? extends Registry<EquipmentAsset>> EQUIPMENT_ASSET_REGISTRY =
            RegistryKey.ofRegistry(Identifier.ofVanilla("equipment_asset"));
    public static final RegistryKey<EquipmentAsset> HEMP_EQUIPMENT_ASSET =
            RegistryKey.of(EQUIPMENT_ASSET_REGISTRY, Identifier.of(Hempdustry.MOD_ID, "hemp_fiber"));

    // Durability 5 is leather's: hemp is the hard-wearing cloth (sailcloth, canvas), so it wears as
    // long as leather does, and only leather's protection stays out of reach. It was 3 until 2.1,
    // the base the old ArmorItem.Type.getMaxDamage(3) calls carried. Repair is a tag, not an Ingredient.
    public static final ArmorMaterial HEMP_ARMOR_MATERIAL = new ArmorMaterial(5,
            Util.make(new EnumMap<>(EquipmentType.class), map -> {
                map.put(EquipmentType.BOOTS, 1);
                map.put(EquipmentType.LEGGINGS, 1);
                map.put(EquipmentType.CHESTPLATE, 2);
                map.put(EquipmentType.HELMET, 1);
                map.put(EquipmentType.BODY, 2);
            }), 20, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0, 0,
            ModTags.Items.HEMP_ARMOR_REPAIR, HEMP_EQUIPMENT_ASSET);

    /**
     * The colour an undyed shirt or pair of harem pants is drawn in: unbleached hemp, the one tint
     * that best gives back the shirt as it was drawn before it took dye. The dyeable textures are
     * grey and this tints them, as leather's brown tints leather's. The equipment asset
     * ({@code equipment/hemp_fiber.json}) spells the same number as its {@code color_when_undyed}.
     */
    public static final int HEMP_UNDYED_COLOR = 0xFFFFDFB7;
}

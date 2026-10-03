package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class ModArmorMaterials {
    public static final RegistryEntry<ArmorMaterial> HEMP_ARMOR_MATERIAL = registerArmorMaterial("hemp_fiber",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 1);
                map.put(ArmorItem.Type.LEGGINGS, 1);
                map.put(ArmorItem.Type.CHESTPLATE, 2);
                map.put(ArmorItem.Type.HELMET, 1);
                map.put(ArmorItem.Type.BODY, 2);
            }), 20, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, () -> Ingredient.ofItems(ModItems.HEMP_FIBER),
                    // The grey cloth first, tinted by the dye (or HEMP_UNDYED_COLOR), and the drawn
                    // layer -- the name a resource pack already redraws -- untinted on top, so a pack
                    // that redrew the whole garment still shows its own art.
                    List.of(new ArmorMaterial.Layer(Identifier.of(Hempdustry.MOD_ID, "hemp_fiber_dyeable"), "", true),
                            new ArmorMaterial.Layer(Identifier.of(Hempdustry.MOD_ID, "hemp_fiber"), "", false)), 0, 0));

    /**
     * The colour an undyed shirt or pair of harem pants is drawn in: unbleached hemp, the one tint
     * that best gives back the shirt as it was drawn before it took dye. The dyeable textures are
     * grey and this tints them, as leather's brown tints leather's. Vanilla hard-codes that brown for
     * every dyeable armour layer, so the worn garments take this one through
     * {@code ArmorFeatureRendererMixin}, and the icons through their colour provider.
     */
    public static final int HEMP_UNDYED_COLOR = 0xFFFFDFB7;

    public static RegistryEntry<ArmorMaterial> registerArmorMaterial(String name, Supplier<ArmorMaterial> material) {
        return Registry.registerReference(Registries.ARMOR_MATERIAL, Identifier.of(Hempdustry.MOD_ID, name), material.get());
    }
}

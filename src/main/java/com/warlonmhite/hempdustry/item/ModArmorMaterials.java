package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class ModArmorMaterials {
    public static final Holder<ArmorMaterial> HEMP_ARMOR_MATERIAL = registerArmorMaterial("hemp_fiber",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 1);
                map.put(ArmorItem.Type.LEGGINGS, 1);
                map.put(ArmorItem.Type.CHESTPLATE, 2);
                map.put(ArmorItem.Type.HELMET, 1);
                map.put(ArmorItem.Type.BODY, 2);
            }), 20, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ModItems.HEMP_FIBER),
                    // The grey cloth first, tinted by the dye (or HEMP_UNDYED_COLOR), and the drawn
                    // layer -- the name a resource pack already redraws -- untinted on top, so a pack
                    // that redrew the whole garment still shows its own art.
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp_fiber_dyeable"), "", true),
                            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp_fiber"), "", false)), 0, 0));

    /**
     * The colour an undyed shirt or pair of harem pants is drawn in: unbleached hemp, the one tint
     * that best gives back the shirt as it was drawn before it took dye. The dyeable textures are
     * grey and this tints them, as leather's brown tints leather's. Vanilla hard-codes that brown for
     * every dyeable armour layer, so the worn garments take this one through
     * {@code ArmorFeatureRendererMixin}, and the icons through their colour provider.
     */
    public static final int HEMP_UNDYED_COLOR = 0xFFFFDFB7;

    public static Holder<ArmorMaterial> registerArmorMaterial(String name, Supplier<ArmorMaterial> material) {
        return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, name), material.get());
    }
}

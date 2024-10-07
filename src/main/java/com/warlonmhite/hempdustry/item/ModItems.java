package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.custom.IndicaSpliffItem;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    public static final Item INDICA_SEEDS = registerItem("indica_seeds", new AliasedBlockItem(ModBlocks.INDICA_CROP, new Item.Settings()));
    public static final Item INDICA_BUDS = registerItem("indica_buds", new Item(new Item.Settings()));
    public static final Item SATIVA_BUDS = registerItem("sativa_buds", new Item(new Item.Settings()));
    public static final Item HEMP_STEM = registerItem("hemp_stem", new Item(new Item.Settings()));

    public static final Item HEMP_FLOUR = registerItem("hemp_flour", new Item(new Item.Settings()));
    public static final Item HEMP_FIBER = registerItem("hemp_fiber", new Item(new Item.Settings()));
    public static final Item HEMPCRETE = registerItem("hempcrete", new Item(new Item.Settings()));
    public static final Item HEMP_BRICK = registerItem("hemp_brick", new Item(new Item.Settings()));
    public static final Item CANNABUTTER = registerItem("cannabutter", new Item(new Item.Settings()));


    public static final Item INDICA_SPLIFF = registerItem("indica_spliff", new IndicaSpliffItem(new Item.Settings().rarity(Rarity.COMMON).maxCount(16)));
    public static final Item PIPE = registerItem("pipe", new Item(new Item.Settings().rarity(Rarity.COMMON).maxCount(1)));
    public static final Item GLASS_BONG = registerItem("glass_bong", new Item(new Item.Settings().rarity(Rarity.COMMON).maxCount(1)));

    public static final Item HEMP_BEANNIE = registerItem("hemp_beannie", new ArmorItem(ModArmorMaterials.HEMP_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings()
            .maxDamage(ArmorItem.Type.HELMET.getMaxDamage(3))));
    public static final Item HEMP_SHIRT = registerItem("hemp_shirt", new ArmorItem(ModArmorMaterials.HEMP_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings()
            .maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(3))));
    public static final Item HEMP_HAREM_PANTS = registerItem("hemp_harem_pants", new ArmorItem(ModArmorMaterials.HEMP_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings()
            .maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(3))));
    public static final Item FLIP_FLOPS = registerItem("flip_flops", new ArmorItem(ModArmorMaterials.HEMP_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings()
            .maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(3))));

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name), item);
    }
    public static void registerModItems(){
        Hempdustry.LOGGER.info("Registering Mod Items for " + Hempdustry.MOD_ID);
    }
}

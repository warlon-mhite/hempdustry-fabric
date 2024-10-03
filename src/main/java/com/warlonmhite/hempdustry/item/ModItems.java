package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item INDICA_SEEDS = registerItem("indica_seeds", new Item(new Item.Settings()));
    public static final Item INDICA_BUDS = registerItem("indica_buds", new Item(new Item.Settings()));
    public static final Item SATIVA_BUDS = registerItem("sativa_buds", new Item(new Item.Settings()));
    public static final Item HEMP_STEM = registerItem("hemp_stem", new Item(new Item.Settings()));

    public static final Item HEMP_FIBER = registerItem("hemp_fiber", new Item(new Item.Settings()));
    public static final Item HEMPCRETE = registerItem("hempcrete", new Item(new Item.Settings()));
    public static final Item HEMP_BRICK = registerItem("hemp_brick", new Item(new Item.Settings()));
    public static final Item CANNABUTTER = registerItem("cannabutter", new Item(new Item.Settings()));

    public static final Item PIPE = registerItem("pipe", new Item(new Item.Settings()));
    public static final Item PACKED_PIPE = registerItem("packed_pipe", new Item(new Item.Settings()));

    public static final Item HEMP_BEANNIE = registerItem("hemp_beannie", new Item(new Item.Settings()));
    public static final Item HEMP_SHIRT = registerItem("hemp_shirt", new Item(new Item.Settings()));
    public static final Item HEMP_HAREM_PANTS = registerItem("hemp_harem_pants", new Item(new Item.Settings()));
    public static final Item FLIP_FLOPS = registerItem("flip_flops", new Item(new Item.Settings()));
    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name), item);
    }
    public static void registerModItems(){
        Hempdustry.LOGGER.info("Registering Mod Items for " + Hempdustry.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
            entries.add(INDICA_SEEDS);
            entries.add(INDICA_BUDS);
            entries.add(SATIVA_BUDS);
            entries.add(HEMP_STEM);

            entries.add(HEMP_FIBER);
            entries.add(HEMPCRETE);
            entries.add(HEMP_BRICK);
            entries.add(CANNABUTTER);

            entries.add(PIPE);
            entries.add(PACKED_PIPE);

            entries.add(HEMP_BEANNIE);
            entries.add(HEMP_SHIRT);
            entries.add(HEMP_HAREM_PANTS);
            entries.add(FLIP_FLOPS);
        });
    }
}

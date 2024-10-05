package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup HEMPDUSTRY_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(Hempdustry.MOD_ID, "hempdustry_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.SATIVA_BUDS))
                    .displayName(Text.translatable("itemgroup.hempdustry.sativa_buds"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.INDICA_SEEDS);
                        entries.add(ModItems.INDICA_BUDS);
                        entries.add(ModItems.SATIVA_BUDS);
                        entries.add(ModItems.HEMP_STEM);

                        entries.add(ModItems.HEMP_FLOUR);
                        entries.add(ModItems.HEMP_FIBER);
                        entries.add(ModItems.HEMPCRETE);
                        entries.add(ModItems.HEMP_BRICK);
                        entries.add(ModItems.CANNABUTTER);

                        entries.add(ModItems.INDICA_SPLIFF);
                        entries.add(ModItems.PIPE);

                        entries.add(ModItems.HEMP_BEANNIE);
                        entries.add(ModItems.HEMP_SHIRT);
                        entries.add(ModItems.HEMP_HAREM_PANTS);
                        entries.add(ModItems.FLIP_FLOPS);


                        entries.add(ModBlocks.HEMP_BRICKS_BLOCK);
                        entries.add(ModBlocks.HEMP_BRICKS_SLAB);
                        entries.add(ModBlocks.HEMP_BRICKS_STAIRS);
                        entries.add(ModBlocks.HEMP_BRICKS_WALL);

                        entries.add(ModBlocks.HEMP_PLANKS);
                        entries.add(ModBlocks.HEMP_PLANKS_SLAB);
                        entries.add(ModBlocks.HEMP_PLANKS_STAIRS);
                        entries.add(ModBlocks.HEMP_PLANKS_BUTTON);
                        entries.add(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE);
                        entries.add(ModBlocks.HEMP_PLANKS_FENCE);
                        entries.add(ModBlocks.HEMP_PLANKS_FENCE_GATE);
                        entries.add(ModBlocks.HEMP_PLANKS_DOOR);
                        entries.add(ModBlocks.HEMP_PLANKS_TRAPDOOR);

                        entries.add(ModBlocks.HEMPCRETE_POWDER_BLOCK);
                        entries.add(ModBlocks.HEMPCRETE_BLOCK);
                        entries.add(ModBlocks.HEMP_BALE);
                    }).build());

    public static void registerItemGroups(){
        Hempdustry.LOGGER.info("Registering Item Groups for "+ Hempdustry.MOD_ID);
    }
}

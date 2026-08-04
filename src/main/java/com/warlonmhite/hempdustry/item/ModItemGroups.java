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
                        entries.add(ModItems.SATIVA_SEEDS);
                        entries.add(ModItems.SATIVA_BUDS);
                        entries.add(ModItems.HEMP_STEM);
                        entries.add(ModItems.HEMP_LEAF);
                        entries.add(ModItems.DECARBOXYLATED_HEMP);
                        entries.add(ModItems.WASHED_DECARBOXYLATED_HEMP);
                        entries.add(ModBlocks.DECARBOXYLATOR);
                        entries.add(ModBlocks.INFUSER);

                        entries.add(ModItems.HEMP_FLOUR);
                        entries.add(ModItems.HEMP_FIBER);
                        entries.add(ModItems.HEMP_CANVAS);
                        entries.add(ModItems.HEMPCRETE);
                        entries.add(ModItems.HEMP_BRICK);
                        entries.add(ModItems.CANNABUTTER);
                        entries.add(ModItems.TOASTED_HEMP_SEEDS);
                        entries.add(ModItems.HEMP_FLAPJACK);
                        entries.add(ModItems.HEMP_MILK_BUCKET);
                        entries.add(ModItems.SIEMIENIOTKA);

                        entries.add(ModItems.CANNABUTTER_TOAST);
                        entries.add(ModItems.SPACE_COOKIE);
                        entries.add(ModItems.SPACE_BROWNIE);
                        entries.add(ModItems.BHANG_BUCKET);
                        entries.add(ModItems.DAWAMESK);
                        entries.add(ModBlocks.SPACE_CAKE);

                        // The empty devices as items, then one *stack* per strain per dose for
                        // everything loaded — the same way vanilla lists forty-odd potions off a
                        // single registered item. JEI/EMI build their item lists from creative tabs,
                        // so this is also what keeps every strain discoverable in a modpack.
                        entries.add(ModItems.WOODEN_PIPE);
                        entries.add(ModItems.BONG);
                        ModItems.loadedSmokeables().forEach(entries::add);

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
                        entries.add(ModItems.HEMP_PLANKS_SIGN);
                        entries.add(ModItems.HEMP_PLANKS_HANGING_SIGN);
                        entries.add(ModItems.HEMP_BOAT);
                        entries.add(ModItems.HEMP_CHEST_BOAT);

                        entries.add(ModBlocks.HEMPCRETE_POWDER_BLOCK);
                        entries.add(ModBlocks.HEMPCRETE_BLOCK);
                        entries.add(ModBlocks.HEMP_BALE);
                        entries.add(ModBlocks.HEMP_WOOL);
                        entries.add(ModBlocks.HEMP_CARPET);

                        entries.add(ModBlocks.INDICA_FLOWER);
                        entries.add(ModBlocks.SATIVA_FLOWER);

                        entries.add(ModItems.MUSIC_DISC_GANJA);
                    }).build());

    public static void registerItemGroups(){
        Hempdustry.LOGGER.info("Registering Item Groups for "+ Hempdustry.MOD_ID);
    }
}

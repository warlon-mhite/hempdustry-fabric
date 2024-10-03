package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MossBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block HEMP_BRICKS_BLOCK = registerBlock("hemp_bricks_block",
            new Block(AbstractBlock.Settings.create().strength(4f).sounds(BlockSoundGroup.WOOD)));
    public static final Block HEMP_PLANKS = registerBlock("hemp_planks",
            new Block(AbstractBlock.Settings.create().strength(4f).sounds(BlockSoundGroup.WOOD)));
    public static final Block HEMPCRETE_POWDER_BLOCK = registerBlock("hempcrete_powder_block",
            new Block(AbstractBlock.Settings.create().strength(4f).sounds(BlockSoundGroup.SAND)));
    public static final Block HEMPCRETE_BLOCK = registerBlock("hempcrete_block",
            new Block(AbstractBlock.Settings.create().strength(4f).sounds(BlockSoundGroup.STONE)));

    private static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(Hempdustry.MOD_ID, name), block);
    }
    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        Hempdustry.LOGGER.info("Registering Mod Blocks for " + Hempdustry.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(ModBlocks.HEMP_BRICKS_BLOCK);
            entries.add(ModBlocks.HEMP_PLANKS);
            entries.add(ModBlocks.HEMPCRETE_POWDER_BLOCK);
            entries.add(ModBlocks.HEMPCRETE_BLOCK);
        });
    }
}

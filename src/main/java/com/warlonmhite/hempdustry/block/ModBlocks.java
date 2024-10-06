package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;


public class ModBlocks {

    public static final Block HEMP_BRICKS_BLOCK = registerBlock("hemp_bricks_block",
            new Block(AbstractBlock.Settings.create().strength(2.0F, 10.0F).sounds(BlockSoundGroup.WOOD)));

    public static final Block HEMP_BRICKS_STAIRS = registerBlock("hemp_bricks_stairs",
            new StairsBlock(ModBlocks.HEMP_BRICKS_BLOCK.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD)));
    public static final Block HEMP_BRICKS_SLAB = registerBlock("hemp_bricks_slab",
            new SlabBlock(AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD)));

    public static final Block HEMP_BRICKS_WALL = registerBlock("hemp_bricks_wall",
            new WallBlock(AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD)));


    public static final Block HEMP_PLANKS = registerBlock("hemp_planks",
            new Block(AbstractBlock.Settings.create().strength(2.0F, 3.0F).sounds(BlockSoundGroup.WOOD)));

    public static final Block HEMP_PLANKS_STAIRS = registerBlock("hemp_planks_stairs",
            new StairsBlock(ModBlocks.HEMP_PLANKS.getDefaultState(),
                    AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD)));
    public static final Block HEMP_PLANKS_SLAB = registerBlock("hemp_planks_slab",
            new SlabBlock(AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD)));

    public static final Block HEMP_PLANKS_BUTTON = registerBlock("hemp_planks_button",
            new ButtonBlock(BlockSetType.OAK, 2, AbstractBlock.Settings.create().strength(2f).noCollision()));
    public static final Block HEMP_PLANKS_PRESSURE_PLATE = registerBlock("hemp_planks_pressure_plate",
            new PressurePlateBlock(BlockSetType.OAK, AbstractBlock.Settings.create().strength(2f).noCollision()));

    public static final Block HEMP_PLANKS_FENCE = registerBlock("hemp_planks_fence",
            new FenceBlock(AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD)));
    public static final Block HEMP_PLANKS_FENCE_GATE = registerBlock("hemp_planks_fence_gate",
            new FenceGateBlock(WoodType.OAK, AbstractBlock.Settings.create().strength(2f)));


    public static final Block HEMP_PLANKS_DOOR = registerBlock("hemp_planks_door",
            new DoorBlock(BlockSetType.OAK, AbstractBlock.Settings.create().strength(2f).nonOpaque()));
    public static final Block HEMP_PLANKS_TRAPDOOR = registerBlock("hemp_planks_trapdoor",
            new TrapdoorBlock(BlockSetType.OAK, AbstractBlock.Settings.create().strength(2f).nonOpaque()));

    public static final Block HEMPCRETE_BLOCK = registerBlock("hempcrete_block",
            new Block(AbstractBlock.Settings.create().strength(1.8F).sounds(BlockSoundGroup.STONE)));
    public static final FallingBlock HEMPCRETE_POWDER_BLOCK = (FallingBlock) registerBlock("hempcrete_powder_block",
            new CustomConcreteBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.SAND)));


    public static final Block HEMP_BALE = registerBlock("hemp_bale",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.HAY_BLOCK).strength(0.5f).sounds(BlockSoundGroup.GRASS)));


    public static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(Hempdustry.MOD_ID, name), block);
    }
    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        Hempdustry.LOGGER.info("Registering Mod Blocks for " + Hempdustry.MOD_ID);

    }
}

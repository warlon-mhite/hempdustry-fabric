package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.custom.CustomConcreteBlock;
import com.warlonmhite.hempdustry.block.custom.DecarboxylatorBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.InfuserBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaFlower;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaFlower;
import com.warlonmhite.hempdustry.block.custom.SpaceCakeBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;


public class ModBlocks {

    public static final BlockSetType HEMP_BLOCK_SET_TYPE = BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
            .register(Identifier.of(Hempdustry.MOD_ID, "hemp"));
    public static final WoodType HEMP_WOOD_TYPE = WoodTypeBuilder.copyOf(WoodType.OAK)
            .register(Identifier.of(Hempdustry.MOD_ID, "hemp"), HEMP_BLOCK_SET_TYPE);

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

    // Signs place their own item specially (SignItem/HangingSignItem reference both the standing and wall
    // block), so these are registered without the usual auto BlockItem.
    public static final Block HEMP_PLANKS_SIGN = registerBlockWithoutItem("hemp_planks_sign",
            new SignBlock(HEMP_WOOD_TYPE, AbstractBlock.Settings.create().strength(1.0F).sounds(BlockSoundGroup.WOOD).noCollision()));
    public static final Block HEMP_PLANKS_WALL_SIGN = registerBlockWithoutItem("hemp_planks_wall_sign",
            new WallSignBlock(HEMP_WOOD_TYPE, AbstractBlock.Settings.create().strength(1.0F).sounds(BlockSoundGroup.WOOD).noCollision().dropsLike(HEMP_PLANKS_SIGN)));

    public static final Block HEMP_PLANKS_HANGING_SIGN = registerBlockWithoutItem("hemp_planks_hanging_sign",
            new HangingSignBlock(HEMP_WOOD_TYPE, AbstractBlock.Settings.create().strength(1.0F).sounds(BlockSoundGroup.HANGING_SIGN).noCollision()));
    public static final Block HEMP_PLANKS_WALL_HANGING_SIGN = registerBlockWithoutItem("hemp_planks_wall_hanging_sign",
            new WallHangingSignBlock(HEMP_WOOD_TYPE, AbstractBlock.Settings.create().strength(1.0F).sounds(BlockSoundGroup.HANGING_SIGN).noCollision().dropsLike(HEMP_PLANKS_HANGING_SIGN)));

    public static final Block HEMPCRETE_BLOCK = registerBlock("hempcrete_block",
            new Block(AbstractBlock.Settings.create().strength(1.8F).sounds(BlockSoundGroup.STONE)));
    public static final FallingBlock HEMPCRETE_POWDER_BLOCK = (FallingBlock) registerBlock("hempcrete_powder_block",
            new CustomConcreteBlock(AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.SAND)));


    public static final Block HEMP_BALE = registerBlock("hemp_bale",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.HAY_BLOCK).strength(0.5f).sounds(BlockSoundGroup.GRASS)));


    /**
     * Hemp cloth in bulk — the bale, not a lighter fabric than canvas. Vanilla wool's settings
     * exactly (0.8 hardness, wool sounds, guitar under a note block), because it <em>is</em> a wool
     * block in everything but the dye colour.
     * <p>
     * Deliberately <b>not</b> in {@code #minecraft:wool}: that tag's only effects are the painting
     * recipe (which belongs to this block explicitly, not by tag — see ModRecipeProvider), the
     * vibration-damping tags, and a 100-tick fuel entry. The latter two are granted directly, so
     * joining the tag would buy nothing and cost the recipe. See CLAUDE.md's <i>cloth chain</i> note.
     * <p>
     * It burns, at vanilla wool's 30/60 — a deliberate departure from the fireproof hemp plank set
     * and hempcrete. Cloth burns.
     */
    public static final Block HEMP_WOOL = registerBlock("hemp_wool",
            new Block(AbstractBlock.Settings.create()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .instrument(NoteBlockInstrument.GUITAR)
                    .strength(0.8F)
                    .sounds(BlockSoundGroup.WOOL)
                    .burnable()));

    /**
     * Hemp carpet. A plain {@link CarpetBlock}, not vanilla's {@code DyedCarpetBlock} — the dyed
     * subclass exists only to report a {@code DyeColor} to llama decoration, and ours has no dye
     * colour to report (moss carpet is plain for the same reason).
     * <p>
     * It shares the wool block's texture, exactly as every vanilla carpet shares its wool's.
     */
    public static final Block HEMP_CARPET = registerBlock("hemp_carpet",
            new CarpetBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.1F)
                    .sounds(BlockSoundGroup.WOOL)
                    .burnable()));


    public static final Block INDICA_CROP = registerBlock("indica_crop",
            new IndicaCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT)));

    public static final Block INDICA_FLOWER = registerBlock("indica_flower",
            new IndicaFlower(StatusEffects.MINING_FATIGUE, 1, AbstractBlock.Settings.copy(Blocks.ALLIUM)));
    public static final Block POTTED_INDICA_FLOWER = registerBlock("potted_indica_flower",
            new FlowerPotBlock(INDICA_FLOWER, AbstractBlock.Settings.copy(Blocks.POTTED_ALLIUM)));


    public static final Block SATIVA_CROP = registerBlock("sativa_crop",
            new SativaCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT)));

    // Wild Lemon Haze. SativaFlower widens the ground it accepts to sand/terracotta so it can
    // actually grow in badlands (see that class). Suspicious stew effect is Haste, matching what
    // the strain does when smoked.
    public static final Block SATIVA_FLOWER = registerBlock("sativa_flower",
            new SativaFlower(StatusEffects.HASTE, 1, AbstractBlock.Settings.copy(Blocks.DANDELION)));
    public static final Block POTTED_SATIVA_FLOWER = registerBlock("potted_sativa_flower",
            new FlowerPotBlock(SATIVA_FLOWER, AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION)));


    /**
     * The Decarboxylator, the mod's first machine. Furnace-grade stone (3.5 hardness / 3.5 blast
     * resistance, pickaxe-mined) and, like the hemp wood set, deliberately never registered as
     * flammable — an oven that catches fire reads as a bug, not a feature.
     */
    public static final Block DECARBOXYLATOR = registerBlock("decarboxylator",
            new DecarboxylatorBlock(AbstractBlock.Settings.create()
                    .strength(3.5F, 3.5F)
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE)
                    .luminance(state -> state.get(DecarboxylatorBlock.LIT) ? 13 : 0)));


    /**
     * The Infuser. Same furnace-grade stone as the Decarboxylator, and deliberately <b>not</b>
     * light-emitting: its glow has to come from whatever is heating it from below, or the
     * heat-from-below mechanic would look self-powered.
     */
    public static final Block INFUSER = registerBlock("infuser",
            new InfuserBlock(AbstractBlock.Settings.create()
                    .strength(3.5F, 3.5F)
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE)));


    /**
     * Space Cake — vanilla's cake, baked with cannabutter. Copies {@code Blocks.CAKE}'s settings
     * wholesale (0.5 hardness, wool sounds, no occlusion) so it behaves identically to the block
     * players already know; the {@code maxCount(1)} on its item is vanilla's cake too.
     */
    public static final Block SPACE_CAKE = registerBlock("space_cake",
            new SpaceCakeBlock(AbstractBlock.Settings.copy(Blocks.CAKE)),
            new Item.Settings().maxCount(1));


    public static Block registerBlock(String name, Block block){
        return registerBlock(name, block, new Item.Settings());
    }
    /** Same, but with explicit item settings — for blocks whose item isn't a plain 64-stack. */
    public static Block registerBlock(String name, Block block, Item.Settings itemSettings){
        registerBlockItem(name, block, itemSettings);
        return Registry.register(Registries.BLOCK, Identifier.of(Hempdustry.MOD_ID, name), block);
    }
    private static void registerBlockItem(String name, Block block, Item.Settings itemSettings) {
        Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name),
                new BlockItem(block, itemSettings));
    }
    private static Block registerBlockWithoutItem(String name, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(Hempdustry.MOD_ID, name), block);
    }

    public static void registerModBlocks() {
        Hempdustry.LOGGER.info("Registering Mod Blocks for " + Hempdustry.MOD_ID);

    }
}

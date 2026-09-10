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
import com.warlonmhite.hempdustry.item.custom.EdibleBlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.BiFunction;


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
     * Nine hemp leaves baled into a block, and back again. Vanilla's leaves settings to the pixel —
     * it is in {@code #minecraft:leaves}, which is the <em>only</em> way to get shears' 15× mining
     * rule (that rule names the tag, not a block list — verified in {@code ShearsItem}), and carries
     * {@code #minecraft:mineable/hoe} and {@code #sword_efficient} in for free.
     * <p>
     * Deliberately <b>not</b> a {@link LeavesBlock}: that class exists to decay, and this block is
     * crafted rather than grown, so there is no log for a {@code distance} to count from. Everything
     * that reads that property guards on {@code state.contains(DISTANCE)} first, so a plain
     * {@link Block} sits in the tag safely.
     * <p>
     * The tag's one real cost is {@code #replaceable_by_trees} — a sapling grown next to a wall of
     * these will eat it. That is exactly what happens to a wall of oak leaves, so it stays.
     * <p>
     * Untinted, unlike every vanilla leaf: the texture carries hemp's own green rather than the
     * biome's, which also keeps it out of the tint-pairing trap in {@code crops.md}.
     * <p>
     * A piston breaks it rather than pushing it, as vanilla's {@code createLeavesBlock} sets for
     * every leaf. That line was missed on the way in, and a leaf block that a piston shoves like
     * stone reads as a bug. {@code ticksRandomly} is the one vanilla setting left out: it only
     * drives decay.
     * <p>
     * The four predicates are spelled out rather than borrowed from {@code Blocks}: vanilla's
     * {@code never} and {@code canSpawnOnLeaves} helpers are private on this line. The spawning one
     * is vanilla's own rule verbatim — ocelots and parrots, nothing else.
     */
    public static final Block HEMP_LEAVES = registerBlock("hemp_leaves",
            new Block(AbstractBlock.Settings.create()
                    .mapColor(MapColor.DARK_GREEN)
                    .strength(0.2F)
                    .sounds(BlockSoundGroup.GRASS)
                    .nonOpaque()
                    .burnable()
                    .allowsSpawning((state, world, pos, type) ->
                            type == EntityType.OCELOT || type == EntityType.PARROT)
                    .suffocates((state, world, pos) -> false)
                    .blockVision((state, world, pos) -> false)
                    .solidBlock((state, world, pos) -> false)
                    .pistonBehavior(PistonBehavior.DESTROY)));


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


    // NO BLOCK ITEM, on both crops, exactly as vanilla WHEAT has none: the seeds are the item, and
    // ModItems.INDICA_SEEDS / SATIVA_SEEDS are the BlockItems that plant these. A BlockItem here
    // would be a registry entry no player can obtain except with /give, with no model to render it
    // (CropBlock#getPickStack already hands back getSeedsItem, so middle-click is unaffected).
    public static final Block INDICA_CROP = registerBlockWithoutItem("indica_crop",
            new IndicaCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT)));

    // Wild Purple Kush. One block, and it takes a suspicious-stew effect because FlowerBlock's
    // constructor demands one — IT CAN NEVER APPLY IT. Both the crafting recipe
    // (SuspiciousStewRecipe) and the brown mooshroom gate on the *item* tag
    // #minecraft:small_flowers, which this deliberately does not join — a stew brewed from a raw
    // flower would hand out a status effect with no heat anywhere in the chain, and "heat
    // activates, raw plant does nothing" is the rule the whole Decarboxylator exists to enforce
    // (CLAUDE.md §4).
    //
    // So the argument below is structurally required and functionally dead. It is kept meaningful
    // rather than arbitrary — Mining Fatigue is what the strain does when smoked — purely so that
    // the day the rule is revisited, the answer is already written down. SATIVA_FLOWER no longer
    // has the problem at all: a TallPlantBlock never asks for a stew effect.
    // Note the *block* tags in ModBlockTagProvider are a separate question and are joined.
    public static final Block INDICA_FLOWER = registerBlock("indica_flower",
            new IndicaFlower(StatusEffects.MINING_FATIGUE, 1, AbstractBlock.Settings.copy(Blocks.ALLIUM)));
    public static final Block POTTED_INDICA_FLOWER = registerBlock("potted_indica_flower",
            new FlowerPotBlock(INDICA_FLOWER, AbstractBlock.Settings.copy(Blocks.POTTED_ALLIUM)));


    // No block item, for the reason on INDICA_CROP above.
    public static final Block SATIVA_CROP = registerBlockWithoutItem("sativa_crop",
            new SativaCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT)));

    // Wild Lemon Haze — TWO BLOCKS TALL, unlike its Purple Kush counterpart. Both wild flowers wear
    // their strain's mid-growth crop art: indica stage 3, one tile; sativa stage 5, drawn across a
    // bottom and a top, because Lemon Haze is the lanky strain. Settings therefore copy ROSE_BUSH,
    // vanilla's own two-tall flower, rather than DANDELION.
    //
    // SativaFlower widens the ground it accepts to sand/terracotta so it can actually grow in
    // badlands, and extends TallPlantBlock rather than TallFlowerBlock so bone meal does NOT
    // duplicate it — see that class for both.
    public static final Block SATIVA_FLOWER = registerBlock("sativa_flower",
            new SativaFlower(AbstractBlock.Settings.copy(Blocks.ROSE_BUSH)));
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
     *
     * <p><b>{@code nonOpaque()} is load-bearing, not tidiness.</b> The model is a shaped tub whose
     * waist is inset a pixel and whose top is an open pot. A block that reports itself opaque has
     * its neighbours cull the faces they press against it — so an opaque Infuser would leave a 1px
     * see-through band all the way round the waist and a hole where the pot is. It also lets light
     * into the pot, which is what vanilla's cauldron does for the same reason.
     */
    public static final Block INFUSER = registerBlock("infuser",
            new InfuserBlock(AbstractBlock.Settings.create()
                    .strength(3.5F, 3.5F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.STONE)));


    /**
     * Space Cake — vanilla's cake, baked with cannabutter. Copies {@code Blocks.CAKE}'s settings
     * wholesale (0.5 hardness, wool sounds, no occlusion) so it behaves identically to the block
     * players already know; the {@code maxCount(1)} on its item is vanilla's cake too.
     */
    public static final Block SPACE_CAKE = registerBlockWithItem("space_cake",
            new SpaceCakeBlock(AbstractBlock.Settings.copy(Blocks.CAKE)),
            (block, settings) -> new EdibleBlockItem(block, settings),
            new Item.Settings().maxCount(1));


    public static Block registerBlock(String name, Block block){
        return registerBlock(name, block, new Item.Settings());
    }
    /** Same, but with explicit item settings — for blocks whose item isn't a plain 64-stack. */
    public static Block registerBlock(String name, Block block, Item.Settings itemSettings){
        return registerBlockWithItem(name, block, BlockItem::new, itemSettings);
    }
    /** Same again, but with a custom BlockItem — the Space Cake needs one for its dose tooltip. */
    public static Block registerBlockWithItem(String name, Block block,
                                              BiFunction<Block, Item.Settings, BlockItem> itemFactory,
                                              Item.Settings itemSettings) {
        Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name),
                itemFactory.apply(block, itemSettings));
        return Registry.register(Registries.BLOCK, Identifier.of(Hempdustry.MOD_ID, name), block);
    }
    private static Block registerBlockWithoutItem(String name, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(Hempdustry.MOD_ID, name), block);
    }

    public static void registerModBlocks() {
        Hempdustry.LOGGER.info("Registering Mod Blocks for " + Hempdustry.MOD_ID);

    }
}

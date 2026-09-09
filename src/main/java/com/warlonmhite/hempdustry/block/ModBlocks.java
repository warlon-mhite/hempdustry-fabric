package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.custom.CustomConcreteBlock;
import com.warlonmhite.hempdustry.block.custom.DecarboxylatorBlock;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.custom.FilteredHashishBarBlock;
import com.warlonmhite.hempdustry.block.custom.HashishBarBlock;
import com.warlonmhite.hempdustry.block.custom.HempPressBlock;
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
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.BiFunction;
import java.util.function.Function;


public class ModBlocks {

    public static final BlockSetType HEMP_BLOCK_SET_TYPE = BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
            .register(Identifier.of(Hempdustry.MOD_ID, "hemp"));
    public static final WoodType HEMP_WOOD_TYPE = WoodTypeBuilder.copyOf(WoodType.OAK)
            .register(Identifier.of(Hempdustry.MOD_ID, "hemp"), HEMP_BLOCK_SET_TYPE);

    public static final Block HEMP_BRICKS_BLOCK = registerBlock("hemp_bricks_block", Block::new,
            AbstractBlock.Settings.create().strength(2.0F, 10.0F).sounds(BlockSoundGroup.WOOD));

    public static final Block HEMP_BRICKS_STAIRS = registerBlock("hemp_bricks_stairs", settings -> new StairsBlock(ModBlocks.HEMP_BRICKS_BLOCK.getDefaultState(), settings),
            AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD));
    public static final Block HEMP_BRICKS_SLAB = registerBlock("hemp_bricks_slab", SlabBlock::new,
            AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD));

    public static final Block HEMP_BRICKS_WALL = registerBlock("hemp_bricks_wall", WallBlock::new,
            AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD));


    public static final Block HEMP_PLANKS = registerBlock("hemp_planks", Block::new,
            AbstractBlock.Settings.create().strength(2.0F, 3.0F).sounds(BlockSoundGroup.WOOD));

    public static final Block HEMP_PLANKS_STAIRS = registerBlock("hemp_planks_stairs", settings -> new StairsBlock(ModBlocks.HEMP_PLANKS.getDefaultState(), settings),
            AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD));
    public static final Block HEMP_PLANKS_SLAB = registerBlock("hemp_planks_slab", SlabBlock::new,
            AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD));

    public static final Block HEMP_PLANKS_BUTTON = registerBlock("hemp_planks_button", settings -> new ButtonBlock(BlockSetType.OAK, 2, settings),
            AbstractBlock.Settings.create().strength(2f).noCollision());
    public static final Block HEMP_PLANKS_PRESSURE_PLATE = registerBlock("hemp_planks_pressure_plate", settings -> new PressurePlateBlock(BlockSetType.OAK, settings),
            AbstractBlock.Settings.create().strength(2f).noCollision());

    public static final Block HEMP_PLANKS_FENCE = registerBlock("hemp_planks_fence", FenceBlock::new,
            AbstractBlock.Settings.create().strength(2f).sounds(BlockSoundGroup.WOOD));
    public static final Block HEMP_PLANKS_FENCE_GATE = registerBlock("hemp_planks_fence_gate", settings -> new FenceGateBlock(WoodType.OAK, settings),
            AbstractBlock.Settings.create().strength(2f));


    public static final Block HEMP_PLANKS_DOOR = registerBlock("hemp_planks_door", settings -> new DoorBlock(BlockSetType.OAK, settings),
            AbstractBlock.Settings.create().strength(2f).nonOpaque());
    public static final Block HEMP_PLANKS_TRAPDOOR = registerBlock("hemp_planks_trapdoor", settings -> new TrapdoorBlock(BlockSetType.OAK, settings),
            AbstractBlock.Settings.create().strength(2f).nonOpaque());

    // Signs place their own item specially (SignItem/HangingSignItem reference both the standing and wall
    // block), so these are registered without the usual auto BlockItem.
    public static final Block HEMP_PLANKS_SIGN = registerBlockWithoutItem("hemp_planks_sign", settings -> new SignBlock(HEMP_WOOD_TYPE, settings),
            AbstractBlock.Settings.create().strength(1.0F).sounds(BlockSoundGroup.WOOD).noCollision());
    public static final Block HEMP_PLANKS_WALL_SIGN = registerBlockWithoutItem("hemp_planks_wall_sign", settings -> new WallSignBlock(HEMP_WOOD_TYPE, settings),
            AbstractBlock.Settings.create().strength(1.0F).sounds(BlockSoundGroup.WOOD).noCollision().lootTable(HEMP_PLANKS_SIGN.getLootTableKey()));

    public static final Block HEMP_PLANKS_HANGING_SIGN = registerBlockWithoutItem("hemp_planks_hanging_sign", settings -> new HangingSignBlock(HEMP_WOOD_TYPE, settings),
            AbstractBlock.Settings.create().strength(1.0F).sounds(BlockSoundGroup.HANGING_SIGN).noCollision());
    public static final Block HEMP_PLANKS_WALL_HANGING_SIGN = registerBlockWithoutItem("hemp_planks_wall_hanging_sign", settings -> new WallHangingSignBlock(HEMP_WOOD_TYPE, settings),
            AbstractBlock.Settings.create().strength(1.0F).sounds(BlockSoundGroup.HANGING_SIGN).noCollision().lootTable(HEMP_PLANKS_HANGING_SIGN.getLootTableKey()));

    public static final Block HEMPCRETE_BLOCK = registerBlock("hempcrete_block", Block::new,
            AbstractBlock.Settings.create().strength(1.8F).sounds(BlockSoundGroup.STONE));
    public static final FallingBlock HEMPCRETE_POWDER_BLOCK = (FallingBlock) registerBlock("hempcrete_powder_block", CustomConcreteBlock::new,
            AbstractBlock.Settings.create().strength(0.5f).sounds(BlockSoundGroup.SAND));


    public static final Block HEMP_BALE = registerBlock("hemp_bale", PillarBlock::new,
            AbstractBlock.Settings.copy(Blocks.HAY_BLOCK).strength(0.5f).sounds(BlockSoundGroup.GRASS));


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
    public static final Block HEMP_WOOL = registerBlock("hemp_wool", Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .instrument(NoteBlockInstrument.GUITAR)
                    .strength(0.8F)
                    .sounds(BlockSoundGroup.WOOL)
                    .burnable());

    /**
     * Hemp carpet. A plain {@link CarpetBlock}, not vanilla's {@code DyedCarpetBlock} — the dyed
     * subclass exists only to report a {@code DyeColor} to llama decoration, and ours has no dye
     * colour to report (moss carpet is plain for the same reason).
     * <p>
     * It shares the wool block's texture, exactly as every vanilla carpet shares its wool's.
     */
    public static final Block HEMP_CARPET = registerBlock("hemp_carpet", CarpetBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.1F)
                    .sounds(BlockSoundGroup.WOOL)
                    .burnable());


    // NO BLOCK ITEM, on both crops, exactly as vanilla WHEAT has none: the seeds are the item, and
    // ModItems.INDICA_SEEDS / SATIVA_SEEDS are the BlockItems that plant these. A BlockItem here
    // would be a registry entry no player can obtain except with /give, with nothing to render it
    // (CropBlock#getPickStack already hands back getSeedsItem, so middle-click is unaffected).
    public static final Block INDICA_CROP = registerBlockWithoutItem("indica_crop", IndicaCropBlock::new,
            AbstractBlock.Settings.copy(Blocks.WHEAT));

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
    public static final Block INDICA_FLOWER = registerBlock("indica_flower", settings -> new IndicaFlower(StatusEffects.MINING_FATIGUE, 1, settings),
            AbstractBlock.Settings.copy(Blocks.ALLIUM));
    public static final Block POTTED_INDICA_FLOWER = registerBlock("potted_indica_flower", settings -> new FlowerPotBlock(INDICA_FLOWER, settings),
            AbstractBlock.Settings.copy(Blocks.POTTED_ALLIUM));


    // No block item, for the reason on INDICA_CROP above.
    public static final Block SATIVA_CROP = registerBlockWithoutItem("sativa_crop", SativaCropBlock::new,
            AbstractBlock.Settings.copy(Blocks.WHEAT));

    // Wild Lemon Haze — TWO BLOCKS TALL, unlike its Purple Kush counterpart. Both wild flowers wear
    // their strain's mid-growth crop art: indica stage 3, one tile; sativa stage 5, drawn across a
    // bottom and a top, because Lemon Haze is the lanky strain. Settings therefore copy ROSE_BUSH,
    // vanilla's own two-tall flower, rather than DANDELION.
    //
    // SativaFlower widens the ground it accepts to sand/terracotta so it can actually grow in
    // badlands, and extends TallPlantBlock rather than TallFlowerBlock so bone meal does NOT
    // duplicate it — see that class for both.
    public static final Block SATIVA_FLOWER = registerBlock("sativa_flower", SativaFlower::new,
            AbstractBlock.Settings.copy(Blocks.ROSE_BUSH));
    public static final Block POTTED_SATIVA_FLOWER = registerBlock("potted_sativa_flower", settings -> new FlowerPotBlock(SATIVA_FLOWER, settings),
            AbstractBlock.Settings.copy(Blocks.POTTED_DANDELION));


    /**
     * The Decarboxylator, the mod's first machine. Furnace-grade stone (3.5 hardness / 3.5 blast
     * resistance, pickaxe-mined) and, like the hemp wood set, deliberately never registered as
     * flammable — an oven that catches fire reads as a bug, not a feature.
     */
    public static final Block DECARBOXYLATOR = registerBlock("decarboxylator", DecarboxylatorBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.5F, 3.5F)
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE)
                    .luminance(state -> state.get(DecarboxylatorBlock.LIT) ? 13 : 0));


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
    public static final Block INFUSER = registerBlock("infuser", InfuserBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.5F, 3.5F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.STONE));


    /**
     * The Dry Sifter. A screened wooden box, so it is wood rather than the two machines' stone —
     * it is a sieve on a frame, not a furnace, and it should read as something a farmer built.
     *
     * <p><b>{@code nonOpaque()} for the same reason the Infuser needs it:</b> the model is an open
     * box with a hollow middle, and an opaque block has its neighbours cull the faces they press
     * against it, which would show straight through the walls. Vanilla's composter is
     * {@code notSolid} for exactly this.
     *
     * <p>Deliberately <b>not</b> registered as flammable, even though it is wooden: it is a work
     * block a player leaves standing next to a field, and losing one to a stray lightning strike
     * or a lava bucket would read as a bug rather than as a consequence. The composter is not
     * flammable either.
     */
    public static final Block SIFTING_BOX = registerBlock("sifting_box", SiftingBoxBlock::new,
            AbstractBlock.Settings.create()
                    .strength(0.6F)
                    .nonOpaque()
                    .sounds(BlockSoundGroup.WOOD));


    /**
     * The pressed slab of hashish, and the hash family's storage block. Cut with a blade, nine
     * pieces to a bar — see {@link HashishBarBlock}.
     *
     * <p>{@code BlockSoundGroup.HONEY} is the reuse: sticky, soft, resinous, and a sound group that
     * already exists. {@code nonOpaque} because the bar is 4 px tall and must not cull whatever it
     * sits next to. {@code PistonBehavior.DESTROY} because a partially-cut bar carries state a
     * piston cannot move, which is what vanilla does to cake for the same reason.
     *
     * <p><b>Deliberately not in {@code #minecraft:enderman_holdable}</b>, same reasoning as the
     * two-block plant (CLAUDE.md §5): an enderman would put the bar back with its cut count reset.
     *
     * <p><b>And deliberately not in {@code #c:storage_blocks}</b>, despite being a 9↔9 block — this
     * is the one worth saying out loud. That convention tag means "nine of an item, and you can get
     * them back out", so another mod generating uncrafting recipes from it would mint exactly the
     * shapeless unpack this block exists to refuse. A bar unpacks <em>by being cut</em>, with a blade
     * and a durability cost; a tag that quietly undoes that is worse than no tag at all.
     */
    /**
     * Heated from below, and emitting no light of its own — the heat is the neighbour's, and a block
     * that glowed would be claiming otherwise. Stone-hard and tool-gated like the other two machines.
     */
    public static final Block HEMP_PRESS = registerBlock("hemp_press", HempPressBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.5F, 3.5F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.STONE));

    public static final Block HASHISH_BAR = registerBlock("hashish_bar", HashishBarBlock::new,
            AbstractBlock.Settings.create()
                    .strength(0.5F)
                    .sounds(BlockSoundGroup.HONEY)
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY));

    /**
     * The blonde bar — the Dry Sifter's resin pass, pressed. Settings copied from
     * {@link #HASHISH_BAR} because it <em>is</em> that block in a different colour; see
     * {@link FilteredHashishBarBlock} for why the symmetry is deliberate.
     */
    public static final Block FILTERED_HASHISH_BAR = registerBlock("filtered_hashish_bar",
            FilteredHashishBarBlock::new,
            AbstractBlock.Settings.create()
                    .strength(0.5F)
                    .sounds(BlockSoundGroup.HONEY)
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY));

    /**
     * Space Cake — vanilla's cake, baked with cannabutter. Copies {@code Blocks.CAKE}'s settings
     * wholesale (0.5 hardness, wool sounds, no occlusion) so it behaves identically to the block
     * players already know; the {@code maxCount(1)} on its item is vanilla's cake too.
     */
    /**
     * Nine charas, rolled together. <b>A ball and not a bar, and that is the whole point.</b>
     *
     * <p>{@code hashish.md} lands the family's three colours on three <em>forms</em> — a pinch, a
     * brown pressed bar, a blonde pressed bar — and a fourth flat slab would collapse that to three
     * colours on two forms. Charas is <b>rubbed off a living plant and rolled between the palms</b>,
     * never pressed, which is why it has no bar and why the shape it is actually sold in is a ball.
     *
     * <p>So it is a plain storage block: 9 ↔ 1 in the grid, both directions, <b>no blade and no
     * {@code CUTS}</b>. You unroll it by hand. That also keeps the rule the Press introduced exactly
     * true — <b>charas never goes through the Press</b> — while giving the rarest thing in the family
     * somewhere to sit on a shelf.
     *
     * <p>Honey sound group, like the two bars: sticky, soft, resinous.
     */
    public static final Block CHARAS_BALL = registerBlock("charas_ball", Block::new,
            AbstractBlock.Settings.create()
                    .strength(0.5F)
                    .sounds(BlockSoundGroup.HONEY)
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY));

    public static final Block SPACE_CAKE = registerBlockWithItem("space_cake", SpaceCakeBlock::new,
            AbstractBlock.Settings.copy(Blocks.CAKE),
            EdibleBlockItem::new,
            new Item.Settings().maxCount(1));


    public static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> factory,
                                      AbstractBlock.Settings settings) {
        return registerBlock(name, factory, settings, new Item.Settings());
    }
    /** Same, but with explicit item settings — for blocks whose item isn't a plain 64-stack. */
    public static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> factory,
                                      AbstractBlock.Settings settings, Item.Settings itemSettings) {
        return registerBlockWithItem(name, factory, settings, BlockItem::new, itemSettings);
    }
    /** Same again, but with a custom BlockItem — the Space Cake needs one for its dose tooltip. */
    public static Block registerBlockWithItem(String name, Function<AbstractBlock.Settings, Block> factory,
                                              AbstractBlock.Settings settings,
                                              BiFunction<Block, Item.Settings, BlockItem> itemFactory,
                                              Item.Settings itemSettings) {
        Block block = registerBlockWithoutItem(name, factory, settings);
        // useBlockPrefixedTranslationKey is not tidiness: since 1.21.2 a BlockItem's translation key
        // defaults to "item.<ns>.<path>" like any other item, so without this every block in the mod
        // renders as the raw key "item.hempdustry.decarboxylator" in hand and in the creative tab.
        // The lang files key on "block.hempdustry.*", which is what vanilla's own block items use.
        Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name),
                itemFactory.apply(block, itemSettings
                        .useBlockPrefixedTranslationKey()
                        .registryKey(itemKey(name))));
        return block;
    }
    // Since 1.21.2 a block has to know its own id before it is constructed: Settings carries the
    // RegistryKey and AbstractBlock reads it in the constructor (that is where the default loot
    // table id comes from). Hence the factory — the settings cannot be finished by the caller.
    private static Block registerBlockWithoutItem(String name, Function<AbstractBlock.Settings, Block> factory,
                                                  AbstractBlock.Settings settings) {
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Hempdustry.MOD_ID, name));
        return Registry.register(Registries.BLOCK, key, factory.apply(settings.registryKey(key)));
    }

    private static RegistryKey<Item> itemKey(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Hempdustry.MOD_ID, name));
    }

    public static void registerModBlocks() {
        Hempdustry.LOGGER.info("Registering Mod Blocks for " + Hempdustry.MOD_ID);

    }
}

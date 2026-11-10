package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.custom.BeldiaCropBlock;
import com.warlonmhite.hempdustry.block.custom.BongBlock;
import com.warlonmhite.hempdustry.block.custom.CharasBallBlock;
import com.warlonmhite.hempdustry.block.custom.CustomConcreteBlock;
import com.warlonmhite.hempdustry.block.custom.DecarboxylatorBlock;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.custom.FilteredHashishBarBlock;
import com.warlonmhite.hempdustry.block.custom.HashishBarBlock;
import com.warlonmhite.hempdustry.block.custom.GrowLampBlock;
import com.warlonmhite.hempdustry.block.custom.GrowPotBlock;
import com.warlonmhite.hempdustry.block.custom.HempBedBlock;
import com.warlonmhite.hempdustry.block.custom.HempPressBlock;
import com.warlonmhite.hempdustry.block.custom.HydroTrayBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.InfuserBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaFlower;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaFlower;
import com.warlonmhite.hempdustry.block.custom.SpaceCakeBlock;
import com.warlonmhite.hempdustry.item.custom.EdibleBlockItem;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;


public class ModBlocks {

    /** Vanilla's leaves rule, which vanilla keeps private: only ocelots and parrots spawn on leaves. */
    private static boolean ocelotOrParrot(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> type) {
        return type == EntityType.OCELOT || type == EntityType.PARROT;
    }

    /** Vanilla's "no" for a block's state, which vanilla keeps private: leaves never suffocate, block the view or conduct redstone. */
    private static boolean never(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    /** Vanilla's lamp light, which vanilla keeps private: {@code light} while lit, dark otherwise. */
    private static ToIntFunction<BlockState> litBlockEmission(int light) {
        return state -> state.getValue(BlockStateProperties.LIT) ? light : 0;
    }

    // Oak's sounds and behaviour under hemp's name. Registered through the loader: vanilla keeps
    // both registrations private.
    public static final BlockSetType HEMP_BLOCK_SET_TYPE = Services.REGISTRATION.registerBlockSetType(
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp"), BlockSetType.OAK);
    public static final WoodType HEMP_WOOD_TYPE = Services.REGISTRATION.registerWoodType(
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp"), WoodType.OAK, HEMP_BLOCK_SET_TYPE);

    public static final Block HEMP_BRICKS_BLOCK = registerBlock("hemp_bricks_block", Block::new,
            BlockBehaviour.Properties.of().strength(2.0F, 10.0F).sound(SoundType.WOOD));

    public static final Block HEMP_BRICKS_STAIRS = registerBlock("hemp_bricks_stairs", settings -> new StairBlock(ModBlocks.HEMP_BRICKS_BLOCK.defaultBlockState(), settings),
            BlockBehaviour.Properties.ofLegacyCopy(HEMP_BRICKS_BLOCK));
    public static final Block HEMP_BRICKS_SLAB = registerBlock("hemp_bricks_slab", SlabBlock::new,
            BlockBehaviour.Properties.ofLegacyCopy(HEMP_BRICKS_BLOCK));

    public static final Block HEMP_BRICKS_WALL = registerBlock("hemp_bricks_wall", WallBlock::new,
            BlockBehaviour.Properties.ofLegacyCopy(HEMP_BRICKS_BLOCK));


    public static final Block HEMP_PLANKS = registerBlock("hemp_planks", Block::new,
            BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD));

    public static final Block HEMP_PLANKS_STAIRS = registerBlock("hemp_planks_stairs", settings -> new StairBlock(ModBlocks.HEMP_PLANKS.defaultBlockState(), settings),
            BlockBehaviour.Properties.ofLegacyCopy(HEMP_PLANKS));
    public static final Block HEMP_PLANKS_SLAB = registerBlock("hemp_planks_slab", SlabBlock::new,
            BlockBehaviour.Properties.ofLegacyCopy(HEMP_PLANKS));

    public static final Block HEMP_PLANKS_BUTTON = registerBlock("hemp_planks_button", settings -> new ButtonBlock(BlockSetType.OAK, 30, settings),
            BlockBehaviour.Properties.of().noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY));
    public static final Block HEMP_PLANKS_PRESSURE_PLATE = registerBlock("hemp_planks_pressure_plate", settings -> new PressurePlateBlock(BlockSetType.OAK, settings),
            BlockBehaviour.Properties.of().forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(0.5F).pushReaction(PushReaction.DESTROY));

    public static final Block HEMP_PLANKS_FENCE = registerBlock("hemp_planks_fence", FenceBlock::new,
            BlockBehaviour.Properties.ofLegacyCopy(HEMP_PLANKS));
    public static final Block HEMP_PLANKS_FENCE_GATE = registerBlock("hemp_planks_fence_gate", settings -> new FenceGateBlock(WoodType.OAK, settings),
            BlockBehaviour.Properties.of().forceSolidOn().instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F));


    public static final Block HEMP_PLANKS_DOOR = registerBlock("hemp_planks_door", settings -> new DoorBlock(BlockSetType.OAK, settings),
            BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASS).strength(3.0F).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final Block HEMP_PLANKS_TRAPDOOR = registerBlock("hemp_planks_trapdoor", settings -> new TrapDoorBlock(BlockSetType.OAK, settings),
            BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASS).strength(3.0F).noOcclusion());

    // Signs place their own item specially (SignItem/HangingSignItem reference both the standing and wall
    // block), so these are registered without the usual auto BlockItem.
    public static final Block HEMP_PLANKS_SIGN = registerBlockWithoutItem("hemp_planks_sign", settings -> new StandingSignBlock(HEMP_WOOD_TYPE, settings),
            BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD).noCollision());
    public static final Block HEMP_PLANKS_WALL_SIGN = registerBlockWithoutItem("hemp_planks_wall_sign", settings -> new WallSignBlock(HEMP_WOOD_TYPE, settings),
            BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.WOOD).noCollision().overrideLootTable(HEMP_PLANKS_SIGN.getLootTable()));

    public static final Block HEMP_PLANKS_HANGING_SIGN = registerBlockWithoutItem("hemp_planks_hanging_sign", settings -> new CeilingHangingSignBlock(HEMP_WOOD_TYPE, settings),
            BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.HANGING_SIGN).noCollision());
    public static final Block HEMP_PLANKS_WALL_HANGING_SIGN = registerBlockWithoutItem("hemp_planks_wall_hanging_sign", settings -> new WallHangingSignBlock(HEMP_WOOD_TYPE, settings),
            BlockBehaviour.Properties.of().strength(1.0F).sound(SoundType.HANGING_SIGN).noCollision().overrideLootTable(HEMP_PLANKS_HANGING_SIGN.getLootTable()));

    /**
     * The shelf every vanilla wood got in 1.21.9: vanilla's own {@link ShelfBlock}, with vanilla's
     * settings for it (bass under a note block, the shelf sound group, planks' 2.0/3.0) — except
     * {@code burnable()}, which the rest of this fireproof set leaves out too. <b>1.21.11 only</b>:
     * the class does not exist before 1.21.9, so a backport of this set to the 1.21.1 line leaves the
     * shelf behind rather than copying it.
     */
    public static final Block HEMP_PLANKS_SHELF = registerBlock("hemp_planks_shelf", ShelfBlock::new,
            BlockBehaviour.Properties.of()
                    .instrument(NoteBlockInstrument.BASS)
                    .sound(SoundType.SHELF)
                    .strength(2.0F, 3.0F));

    /**
     * Hemp laid as a floor — the plank set's decorative block, the way bamboo mosaic is bamboo's.
     * Same wood as the planks, so the same settings; the stairs and slab copy the parquet, as
     * vanilla's copy the block they are cut from.
     */
    public static final Block HEMP_PARQUET = registerBlock("hemp_parquet", Block::new,
            BlockBehaviour.Properties.ofFullCopy(HEMP_PLANKS));
    public static final Block HEMP_PARQUET_STAIRS = registerBlock("hemp_parquet_stairs",
            settings -> new StairBlock(ModBlocks.HEMP_PARQUET.defaultBlockState(), settings),
            BlockBehaviour.Properties.ofFullCopy(HEMP_PARQUET));
    public static final Block HEMP_PARQUET_SLAB = registerBlock("hemp_parquet_slab", SlabBlock::new,
            BlockBehaviour.Properties.ofFullCopy(HEMP_PARQUET));

    public static final Block HEMPCRETE_BLOCK = registerBlock("hempcrete_block", Block::new,
            BlockBehaviour.Properties.of().strength(1.8F).sound(SoundType.STONE));
    public static final FallingBlock HEMPCRETE_POWDER_BLOCK = (FallingBlock) registerBlock("hempcrete_powder_block", CustomConcreteBlock::new,
            BlockBehaviour.Properties.of().strength(0.5f).sound(SoundType.SAND));


    public static final Block HEMP_BALE = registerBlock("hemp_bale", RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).strength(0.5f).sound(SoundType.GRASS));

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
     * A piston breaks it rather than pushing it, as vanilla's {@code leavesProperties} sets for
     * every leaf. That line was missed on the way in, and a leaf block that a piston shoves like
     * stone reads as a bug. {@code randomTicks} is the one vanilla setting left out: it only
     * drives decay.
     */
    public static final Block HEMP_LEAVES = registerBlock("hemp_leaves", Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .ignitedByLava()
                    .isValidSpawn(ModBlocks::ocelotOrParrot)
                    .isSuffocating(ModBlocks::never)
                    .isViewBlocking(ModBlocks::never)
                    .isRedstoneConductor(ModBlocks::never)
                    .pushReaction(PushReaction.DESTROY));


    /**
     * Hemp cloth in bulk — the bale, not a lighter fabric than canvas. Vanilla wool's settings
     * exactly (0.8 hardness, wool sounds, guitar under a note block), because it <em>is</em> a wool
     * block in everything but the dye colour.
     * <p>
     * Deliberately <b>not</b> in {@code #minecraft:wool}: that tag's only effects are the painting
     * recipe (which belongs to this block explicitly, not by tag — see ModRecipeProvider), the
     * vibration-damping tags, and a 100-tick fuel entry. The latter two are granted directly, so
     * joining the tag would buy nothing and cost the recipe.
     * <p>
     * It burns, at 60/60, catching faster than vanilla wool's 30/60 — a deliberate departure from
     * the fireproof hemp plank set and hempcrete. Cloth burns.
     */
    public static final Block HEMP_WOOL = registerBlock("hemp_wool", Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .instrument(NoteBlockInstrument.GUITAR)
                    .strength(0.8F)
                    .sound(SoundType.WOOL)
                    .ignitedByLava());

    /**
     * Hemp carpet. A plain {@link CarpetBlock}, not vanilla's {@code WoolCarpetBlock}, which only
     * carries a {@code DyeColor} ours does not have (moss carpet is plain for the same reason).
     * <p>
     * It shares the wool block's texture, exactly as every vanilla carpet shares its wool's.
     * <p>
     * <b>A llama wears it because of the item's equip data, not a tag.</b> On 1.21.11 a carpet goes
     * on a llama through the {@code equippable} component vanilla gives each of its sixteen
     * ({@code Equippable.llamaSwag}), and {@code #minecraft:wool_carpets} decides nothing
     * about it — 1.21.1 still asked the tag, which is why this went unnoticed through the port. This is that call, call for call, with our own equipment asset where
     * vanilla looks one up by colour: {@code equipment/hemp_carpet.json}, drawn from
     * {@code entity/equipment/llama_body/hemp.png}.
     */
    public static final Block HEMP_CARPET = registerBlock("hemp_carpet", CarpetBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.1F)
                    .sound(SoundType.WOOL)
                    .ignitedByLava(),
            new Item.Properties().component(DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.BODY)
                            .setEquipSound(SoundEvents.LLAMA_SWAG)
                            .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID,
                                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp_carpet")))
                            .setAllowedEntities(EntityType.LLAMA, EntityType.TRADER_LLAMA)
                            .setCanBeSheared(true)
                            .setShearingSound(SoundEvents.LLAMA_CARPET_UNEQUIP)
                            .build()));

    /**
     * The bed hemp wool makes — see {@link HempBedBlock}. Vanilla's bed settings to the letter
     * (0.2 hardness, wood sounds, a piston breaks it), with hemp wool's map colour on the blanket
     * half where vanilla puts the dye's. It also <em>burns</em>, which no vanilla bed does: it is
     * registered flammable at hemp wool's own rate, because it is a bed made of cloth that burns.
     */
    public static final Block HEMP_BED = registerBlockWithItem("hemp_bed", HempBedBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(state -> state.getValue(BedBlock.PART) == BedPart.FOOT
                            ? MapColor.TERRACOTTA_WHITE : MapColor.WOOL)
                    .sound(SoundType.WOOD)
                    .strength(0.2F)
                    .noOcclusion()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY),
            BedItem::new,
            new Item.Properties().stacksTo(1));


    // NO BLOCK ITEM, on both crops, exactly as vanilla WHEAT has none: the seeds are the item, and
    // ModItems.INDICA_SEEDS / SATIVA_SEEDS are the BlockItems that plant these. A BlockItem here
    // would be a registry entry no player can obtain except with /give, with nothing to render it
    // (CropBlock#getCloneItemStack already hands back getBaseSeedId, so middle-click is unaffected).
    public static final Block INDICA_CROP = registerBlockWithoutItem("indica_crop", IndicaCropBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));

    // Wild Purple Kush. One block, and it takes a suspicious-stew effect because FlowerBlock's
    // constructor demands one — IT CAN NEVER APPLY IT. Both the crafting recipe
    // (SuspiciousStewRecipe) and the brown mooshroom gate on the *item* tag
    // #minecraft:small_flowers, which this deliberately does not join — a stew brewed from a raw
    // flower would hand out a status effect with no heat anywhere in the chain, and "heat
    // activates, raw plant does nothing" is the rule the whole Decarboxylator exists to enforce.
    //
    // So the argument below is structurally required and functionally dead. It is kept meaningful
    // rather than arbitrary — Mining Fatigue is what the strain does when smoked — purely so that
    // the day the rule is revisited, the answer is already written down. SATIVA_FLOWER no longer
    // has the problem at all: a DoublePlantBlock never asks for a stew effect.
    // Note the *block* tags in ModBlockTagProvider are a separate question and are joined.
    public static final Block INDICA_FLOWER = registerBlock("indica_flower", settings -> new IndicaFlower(MobEffects.MINING_FATIGUE, 1, settings),
            BlockBehaviour.Properties.ofFullCopy(Blocks.ALLIUM));
    // No block item on either pot, as vanilla's potted plants have none: the flower is the item.
    public static final Block POTTED_INDICA_FLOWER = registerBlockWithoutItem("potted_indica_flower", settings -> new FlowerPotBlock(INDICA_FLOWER, settings),
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ALLIUM));


    // No block item, for the reason on INDICA_CROP above.
    public static final Block SATIVA_CROP = registerBlockWithoutItem("sativa_crop", SativaCropBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));

    // Wild Lemon Haze — TWO BLOCKS TALL, unlike its Purple Kush counterpart. Both wild flowers wear
    // their strain's mid-growth crop art: indica stage 3, one tile; sativa stage 5, drawn across a
    // bottom and a top, because Lemon Haze is the lanky strain. Properties therefore copy ROSE_BUSH,
    // vanilla's own two-tall flower, rather than DANDELION.
    //
    // SativaFlower widens the ground it accepts to sand/terracotta so it can actually grow in
    // badlands, and extends DoublePlantBlock rather than TallFlowerBlock so bone meal does NOT
    // duplicate it — see that class for both.
    public static final Block SATIVA_FLOWER = registerBlock("sativa_flower", SativaFlower::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.ROSE_BUSH));
    public static final Block POTTED_SATIVA_FLOWER = registerBlockWithoutItem("potted_sativa_flower", settings -> new FlowerPotBlock(SATIVA_FLOWER, settings),
            BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION));


    // No block item, for the reason on INDICA_CROP above. It is also Beldía's wild plant: the world
    // generates this very block, ripe, on desert riverbanks -- there is no separate wild flower.
    public static final Block BELDIA_CROP = registerBlockWithoutItem("beldia_crop", BeldiaCropBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));


    /**
     * The Decarboxylator, the mod's first machine. Furnace-grade stone (3.5 hardness / 3.5 blast
     * resistance, pickaxe-mined) and, like the hemp wood set, deliberately never registered as
     * flammable — an oven that catches fire reads as a bug, not a feature.
     */
    public static final Block DECARBOXYLATOR = registerBlock("decarboxylator", DecarboxylatorBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5F, 3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(DecarboxylatorBlock.LIT) ? 13 : 0));


    /**
     * The Infuser. Same furnace-grade stone as the Decarboxylator, and deliberately <b>not</b>
     * light-emitting: its glow has to come from whatever is heating it from below, or the
     * heat-from-below mechanic would look self-powered.
     *
     * <p><b>{@code noOcclusion()} is load-bearing, not tidiness.</b> The model is a shaped tub whose
     * waist is inset a pixel and whose top is an open pot. A block that reports itself opaque has
     * its neighbours cull the faces they press against it — so an opaque Infuser would leave a 1px
     * see-through band all the way round the waist and a hole where the pot is. It also lets light
     * into the pot, which is what vanilla's cauldron does for the same reason.
     */
    public static final Block INFUSER = registerBlock("infuser", InfuserBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5F, 3.5F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.STONE));


    /**
     * The Sifting Box. A screened wooden box, so it is wood rather than the two machines' stone —
     * it is a sieve on a frame, not a furnace, and it should read as something a farmer built.
     *
     * <p><b>{@code noOcclusion()} for the same reason the Infuser needs it:</b> the model is an open
     * box with a hollow middle, and an opaque block has its neighbours cull the faces they press
     * against it, which would show straight through the walls. Vanilla's composter is
     * {@code forceSolidOff} for exactly this.
     *
     * <p>Deliberately <b>not</b> registered as flammable, even though it is wooden: it is a work
     * block a player leaves standing next to a field, and losing one to a stray lightning strike
     * or a lava bucket would read as a bug rather than as a consequence. The composter is not
     * flammable either.
     */
    public static final Block SIFTING_BOX = registerBlock("sifting_box", SiftingBoxBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(0.6F)
                    .noOcclusion()
                    .sound(SoundType.WOOD));


    /**
     * Heated from below, and emitting no light of its own — the heat is the neighbour's, and a block
     * that glowed would be claiming otherwise. Stone-hard and tool-gated like the other two machines.
     */
    public static final Block HEMP_PRESS = registerBlock("hemp_press", HempPressBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5F, 3.5F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.STONE));

    /**
     * The Grow Lamp — an LED panel hung on copper chains; see {@link GrowLampBlock}. Copper-bulb
     * hardness and sounds, for the housing. {@code noOcclusion()} because it is a thin panel and two
     * chains, not a cube: an opaque block would cull the faces its neighbours press against it.
     */
    public static final Block GROW_LAMP = registerBlock("grow_lamp", GrowLampBlock::new,
            BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .lightLevel(ModBlocks.litBlockEmission(15))
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER_BULB));

    /**
     * The Grow Pot — see {@link GrowPotBlock}. {@code noOcclusion()} because the soil sits below the
     * rim: an opaque block would light that recessed face from inside itself, which is black.
     */
    public static final Block GROW_POT = registerBlock("grow_pot", GrowPotBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_YELLOW)
                    .strength(1.25F)
                    .noOcclusion()
                    .sound(SoundType.DECORATED_POT));

    /**
     * The Hydro Tray — see {@link HydroTrayBlock}. Copper, like the plumbing it is: the mod's
     * electrical metal, and the same hardness as the Grow Lamp's housing. {@code noOcclusion()}
     * because the solution sits below the lip, and an opaque block would light that recessed face
     * from inside itself.
     */
    public static final Block HYDRO_TRAY = registerBlock("hydro_tray", HydroTrayBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.COPPER));

    /**
     * The pressed slab of hashish, and the hash family's storage block. Cut with a blade, nine
     * pieces to a bar — see {@link HashishBarBlock}.
     *
     * <p>{@code SoundType.HONEY_BLOCK} is the reuse: sticky, soft, resinous, and a sound group that
     * already exists. {@code noOcclusion} because the bar is 4 px tall and must not cull whatever it
     * sits next to. {@code PushReaction.DESTROY} because a partially-cut bar carries state a
     * piston cannot move, which is what vanilla does to cake for the same reason.
     *
     * <p><b>Deliberately not in {@code #minecraft:enderman_holdable}</b>, same reasoning as the
     * two-block plant: an enderman would put the bar back with its cut count reset.
     *
     * <p><b>And deliberately not in {@code #c:storage_blocks}</b>, despite being a 9↔9 block — this
     * is the one worth saying out loud. That convention tag means "nine of an item, and you can get
     * them back out", so another mod generating uncrafting recipes from it would mint exactly the
     * shapeless unpack this block exists to refuse. A bar unpacks <em>by being cut</em>, with a blade
     * and a durability cost; a tag that quietly undoes that is worse than no tag at all.
     */
    public static final Block HASHISH_BAR = registerBlock("hashish_bar", HashishBarBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.HONEY_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    /**
     * The blonde bar — nine filtered hashish, crafted together. Properties copied from
     * {@link #HASHISH_BAR} because it <em>is</em> that block in a different colour; see
     * {@link FilteredHashishBarBlock} for why the symmetry is deliberate.
     */
    public static final Block FILTERED_HASHISH_BAR = registerBlock("filtered_hashish_bar",
            FilteredHashishBarBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.HONEY_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

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
    public static final Block CHARAS_BALL = registerBlock("charas_ball", CharasBallBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.HONEY_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    /**
     * Space Cake — vanilla's cake, baked with cannabutter. Copies {@code Blocks.CAKE}'s settings
     * wholesale (0.5 hardness, wool sounds, no occlusion) so it behaves identically to the block
     * players already know; the {@code maxCount(1)} on its item is vanilla's cake too.
     */
    public static final Block SPACE_CAKE = registerBlockWithItem("space_cake", SpaceCakeBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CAKE),
            EdibleBlockItem::new,
            new Item.Properties().stacksTo(1));

    /**
     * Vanilla's creative-tab colour order. {@code CreativeModeTabs} spells it out once per colour family
     * and keeps no list of it, so this is that order written down once. Here rather than in
     * {@code ModItems} because the bong blocks below need it, and this class must never reach into
     * that one during static initialisation — {@code ModItems} reads blocks from here.
     */
    public static final List<DyeColor> DYE_ORDER = List.of(
            DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK, DyeColor.BROWN,
            DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN,
            DyeColor.CYAN, DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA,
            DyeColor.PINK);

    /**
     * What a device stands as when it is put down, keyed by the device item's id — which the block
     * shares, as a candle's does. Only bongs today, one per glass: clear, tinted, then
     * {@link #DYE_ORDER}. A pipe or vaporizer has no entry, which is the whole of "it does not
     * place". Blocks without items: the device <em>is</em> the item, see {@code SmokingDeviceItem}.
     *
     * <p>The block's name is the item's key, so a placed Red Bong needs no {@code block.*} line in
     * eight locales to say so in a tooltip mod or a {@code /setblock} reply.
     */
    public static final Map<String, Block> DEVICE_BLOCKS = registerBongBlocks();

    private static Map<String, Block> registerBongBlocks() {
        List<String> names = new ArrayList<>(List.of("bong", "tinted_bong"));
        DYE_ORDER.forEach(color -> names.add(color.getName() + "_bong"));
        Map<String, Block> blocks = new LinkedHashMap<>();
        for (String name : names) {
            blocks.put(name, registerBlockWithoutItem(name, BongBlock::new, BlockBehaviour.Properties.of()
                    .instabreak()
                    .noOcclusion()
                    .sound(SoundType.GLASS)
                    .pushReaction(PushReaction.DESTROY)
                    .overrideDescription("item." + Hempdustry.MOD_ID + "." + name)));
        }
        return Collections.unmodifiableMap(blocks);
    }


    public static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory,
                                      BlockBehaviour.Properties settings) {
        return registerBlock(name, factory, settings, new Item.Properties());
    }
    /** Same, but with explicit item settings — for blocks whose item isn't a plain 64-stack. */
    public static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory,
                                      BlockBehaviour.Properties settings, Item.Properties itemSettings) {
        return registerBlockWithItem(name, factory, settings, BlockItem::new, itemSettings);
    }
    /** Same again, but with a custom BlockItem — the Space Cake needs one for its dose tooltip. */
    public static Block registerBlockWithItem(String name, Function<BlockBehaviour.Properties, Block> factory,
                                              BlockBehaviour.Properties settings,
                                              BiFunction<Block, Item.Properties, BlockItem> itemFactory,
                                              Item.Properties itemSettings) {
        Block block = registerBlockWithoutItem(name, factory, settings);
        // useBlockDescriptionPrefix is not tidiness: since 1.21.2 a BlockItem's translation key
        // defaults to "item.<ns>.<path>" like any other item, so without this every block in the mod
        // renders as the raw key "item.hempdustry.decarboxylator" in hand and in the creative tab.
        // The lang files key on "block.hempdustry.*", which is what vanilla's own block items use.
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name),
                itemFactory.apply(block, itemSettings
                        .useBlockDescriptionPrefix()
                        .setId(itemKey(name))));
        return block;
    }
    // Since 1.21.2 a block has to know its own id before it is constructed: Properties carries the
    // ResourceKey and BlockBehaviour reads it in the constructor (that is where the default loot
    // table id comes from). Hence the factory — the settings cannot be finished by the caller.
    private static Block registerBlockWithoutItem(String name, Function<BlockBehaviour.Properties, Block> factory,
                                                  BlockBehaviour.Properties settings) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(settings.setId(key)));
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name));
    }

    public static void registerModBlocks() {
        Hempdustry.LOGGER.info("Registering Mod Blocks for " + Hempdustry.MOD_ID);

    }
}

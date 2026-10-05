package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A planter for hemp, any strain. It stands in for farmland — never trampled, and a plant in it never
 * competes with its neighbours for roots: vanilla halves a crop's moisture when the same crop stands
 * beside it, and a pot's plant has soil of its own.
 *
 * <p><b>Soil holds food well and water badly</b>, which is how a pot differs from the Hydro Tray:
 *
 * <ul>
 *   <li><b>Water</b> ({@link #WATERED}) is one bottle, by hand or from a dispenser, and a plant that
 *       ripens drinks it. So a pot is watered every plant, the chore a hydroponic reservoir is built to
 *       save. A dry pot grows like dry farmland whatever it has been fed, and stresses a flowering
 *       plant ({@link PlantStress#DRY_BED}). A bottle into a pot that is still wet is overwatering —
 *       the roots sit in water and rot ({@link PlantStress#OVERWATERED}). A bucket is refused: it is
 *       the tray's tool, and letting it through would put a water block in the plant's space.</li>
 *   <li><b>Food</b> ({@link #FERTILITY}, 0–{@value #MAX_FERTILITY}) is bone meal, one a plant, and a
 *       watered pot with food in it grows its plant {@value #FERTILE_SPEED}× faster than the best
 *       field. Hemp is a nitrogen-hungry plant of manured ground, so the pot runs on the composter; a
 *       full pot refuses bone meal rather than eating it, the way a full composter refuses.</li>
 * </ul>
 *
 * <p>The soil shows both the way a player already reads them: vanilla's own dry and moist farmland
 * for the water, and compost crumbs on top, thinning as the plants eat it, for the food. Rain never
 * waters a pot — nothing indoors is rained on, and the tray ignores it too.
 *
 * <p>The fertility is kept when the pot is picked up (the loot table copies it, as a beehive keeps its
 * honey); the water is not, and a freshly crafted pot starts dry and empty.
 */
public class GrowPotBlock extends Block implements GrowMedium {
    public static final MapCodec<GrowPotBlock> CODEC = createCodec(GrowPotBlock::new);

    public static final int MAX_FERTILITY = 3;
    public static final IntProperty FERTILITY = IntProperty.of("fertility", 0, MAX_FERTILITY);
    public static final BooleanProperty WATERED = BooleanProperty.of("watered");

    /** Watered: vanilla's best, a lone plant on moist farmland ringed by moist farmland. */
    public static final float WET_MOISTURE = 10.0F;
    /** Dry: dry farmland ringed by dry farmland. */
    public static final float DRY_MOISTURE = 4.0F;
    public static final float FERTILE_SPEED = 1.5F;

    /** A body inset a pixel under a full-width lip, as the model draws it. */
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 13.0D, 15.0D),
            Block.createCuboidShape(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D));

    public GrowPotBlock(Settings settings) {
        super(settings);
        // WATERED spelled out: a BooleanProperty left out of the default state defaults to true.
        this.setDefaultState(this.stateManager.getDefaultState().with(FERTILITY, 0).with(WATERED, false));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    public static boolean isPot(BlockState state) {
        return state.getBlock() instanceof GrowPotBlock;
    }

    public static boolean isFertile(BlockState state) {
        return isPot(state) && state.get(FERTILITY) > 0;
    }

    @Override
    public float moisture(BlockState state) {
        return state.get(WATERED) ? WET_MOISTURE : DRY_MOISTURE;
    }

    /** Food only works in water: a dry pot gives nothing, however well it has been fed. */
    @Override
    public float speed(BlockState state) {
        return state.get(WATERED) && isFertile(state) ? FERTILE_SPEED : 1.0F;
    }

    /** The plant that ripens eats one charge of food and drinks the water. */
    @Override
    public void spend(World world, BlockPos pos, BlockState state) {
        BlockState spent = state.with(FERTILITY, Math.max(0, state.get(FERTILITY) - 1)).with(WATERED, false);
        if (spent != state) {
            world.setBlockState(pos, spent, Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public boolean fed(BlockState state) {
        return isFertile(state);
    }

    @Override
    public float stress(BlockState state) {
        return state.get(WATERED) ? 0.0F : PlantStress.DRY_BED;
    }

    // ----- water -----

    public static boolean isWaterBottle(ItemStack stack) {
        return stack.isOf(Items.POTION) && stack.getOrDefault(DataComponentTypes.POTION_CONTENTS,
                PotionContentsComponent.DEFAULT).matches(Potions.WATER);
    }

    /**
     * Pours one bottle into the pot at {@code pos}: a dry pot is watered, and a wet one is overwatered,
     * which may stress a flowering plant in it. Either way the water is poured. No check for the Nether:
     * vanilla's own bottle makes mud there, by hand and from a dispenser, with no evaporation — only a
     * bucket's water boils away.
     */
    public static void water(World world, BlockPos pos, BlockState state, Random random) {
        if (state.get(WATERED)) {
            PlantStress.roll(world, pos.up(), PlantStress.OVERWATERED, random);
        } else {
            world.setBlockState(pos, state.with(WATERED, true), Block.NOTIFY_LISTENERS);
        }
        // Vanilla's feedback for a bottle poured onto dirt (PotionItem#useOnBlock).
        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
        if (world instanceof ServerWorld server) {
            for (int i = 0; i < 5; i++) {
                server.spawnParticles(ParticleTypes.SPLASH, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                        pos.getZ() + random.nextDouble(), 1, 0.0, 0.0, 0.0, 1.0);
            }
        }
    }

    /**
     * Waters the pot at {@code potPos} from the player's hand if they hold a water bottle, handing back
     * the glass bottle. Returns {@code null} when this is not a watering, so the caller falls through to
     * {@code super} rather than a bare {@code PASS}. Also called by the plants, so a bottle used on the
     * plant waters its pot, which is where a player aims.
     */
    @Nullable
    public static ActionResult tryWater(ItemStack stack, World world, BlockPos potPos, PlayerEntity player,
                                        Hand hand) {
        BlockState pot = world.getBlockState(potPos);
        if (!isWaterBottle(stack) || !isPot(pot)) {
            return null;
        }
        if (!world.isClient()) {
            water(world, potPos, pot, world.getRandom());
            player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
            player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
        }
        return ActionResult.SUCCESS;
    }

    /**
     * A water bottle waters the soil and any fertiliser in {@code #c:fertilizers} feeds it. A water
     * bucket is refused by <b>accepting the click and doing nothing</b> ({@code CONSUME}, so no swing
     * either). Anything short of accepted lets the click go on to the bucket's own use, which pours a
     * water block into the plant's space: {@code FAIL} included, because the client treats a block's
     * {@code FAIL} as "try the item" ({@code ClientPlayerInteractionManager#interactBlockInternal}).
     */
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isOf(Items.WATER_BUCKET)) {
            return ActionResult.CONSUME;
        }
        ActionResult watered = tryWater(stack, world, pos, player, hand);
        if (watered != null) {
            return watered;
        }
        ActionResult fed = GrowMedium.tryFeed(stack, state, world, pos, player);
        return fed != null ? fed : super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    // ----- bone meal feeds the soil -----

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return state.get(FERTILITY) < MAX_FERTILITY;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(FERTILITY, state.get(FERTILITY) + 1), Block.NOTIFY_LISTENERS);
    }

    /** Harvests the plant above first, as the player — see {@link GrowMedium#harvestAbove}. */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        GrowMedium.harvestAbove(world, pos, player);
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FERTILITY, WATERED);
    }
}

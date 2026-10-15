package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Hydro Tray: a deep-water-culture reservoir — an opaque tote, a lid holding a net pot of clay
 * pebbles the plant grows out of, an air pump on the front, and a sight tube showing the level. That is
 * how real DWC systems are built, and it is why the water is hidden: light in a reservoir means algae.
 *
 * <p>Three things have to be true at once for the tray to beat soil, and each one is visible on the
 * block: <b>water</b> in the reservoir ({@link #LEVEL}), <b>nutrients</b> mixed into it
 * ({@link #FED} — the sight tube turns from blue to green), and the <b>pump running</b>
 * ({@link #POWERED} — its indicator lights, fullbright, and bubbles come up through the pebbles). That is a nutrient solution, which is what
 * hydroponics actually is; water alone is just a wet bed.
 *
 * <table>
 *   <tr><th>water</th><th>fed</th><th>pump</th><th>grows like</th></tr>
 *   <tr><td>1–3</td><td>yes</td><td>on</td><td><b>{@value #PUMPED_SPEED}× a field</b></td></tr>
 *   <tr><td>1–3</td><td>yes</td><td>off</td><td>still solution: an ordinary field</td></tr>
 *   <tr><td>1–3</td><td>no</td><td>either</td><td>plain water: an ordinary field</td></tr>
 *   <tr><td>0</td><td>—</td><td>—</td><td>a dry bed — slower than plain dirt</td></tr>
 * </table>
 *
 * <p>A plant that ripens drinks one level, and <b>the nutrients go with the last of the water</b>:
 * an empty tray is unfed, so the cycle is refill, feed, and three more plants.
 *
 * <p><b>Nothing here ever kills a plant.</b> That is vanilla's rule, checked in the jar: farmland
 * carrying a crop never reverts to dirt however long it goes unwatered — it only drops its moisture
 * and slows the crop down. Neglect costs time and, while the plant flowers, its health: a dry tray
 * and a dead pump over water can each stress it ({@link PlantStress}).
 *
 * <p><b>And it does not work in the Nether.</b> Water boils away there, so the tray refuses a bucket
 * and refuses to be fed; vanilla then places the water itself and evaporates it with the usual hiss,
 * which is the clearest possible answer. The rule is vanilla's own
 * {@code WATER_EVAPORATES_GAMEPLAY}, the attribute {@code BucketItem} checks, so a datapack's own
 * scorching dimension gets it for free — and the Grow Pot is left owning hemp in the Nether.
 */
public class HydroTrayBlock extends Block implements GrowMedium {
    public static final MapCodec<HydroTrayBlock> CODEC = simpleCodec(HydroTrayBlock::new);

    public static final int MAX_LEVEL = 3;
    /** Water in the reservoir; one level is drunk per plant that ripens. */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL);
    /** Whether that water has been fed — nutrient solution rather than plain water. */
    public static final BooleanProperty FED = BooleanProperty.create("fed");
    /** The pump. Vanilla's own property, so any redstone a player already understands drives it. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Which way the sight tube and the pump face — towards whoever placed it, like a furnace. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Wet: the best ground vanilla knows — a lone plant on fully watered farmland. */
    public static final float WET_MOISTURE = GrowPotBlock.WET_MOISTURE;
    /** Dry: vanilla's unwatered farmland, which is worse than the field a player started with. */
    public static final float DRY_MOISTURE = GrowPotBlock.DRY_MOISTURE;
    public static final float PUMPED_SPEED = 2.0F;

    public HydroTrayBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LEVEL, 0).setValue(FED, false).setValue(POWERED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    // ----- the bed -----

    @Override
    public float moisture(BlockState state) {
        return state.getValue(LEVEL) > 0 ? WET_MOISTURE : DRY_MOISTURE;
    }

    @Override
    public float speed(BlockState state) {
        return state.getValue(LEVEL) > 0 && state.getValue(FED) && state.getValue(POWERED) ? PUMPED_SPEED : 1.0F;
    }

    @Override
    public void spend(Level world, BlockPos pos, BlockState state) {
        int left = state.getValue(LEVEL) - 1;
        if (left < 0) {
            return;
        }
        // The nutrients drain with the last of the water: an empty reservoir is an unfed one.
        world.setBlock(pos, state.setValue(LEVEL, left).setValue(FED, left > 0 && state.getValue(FED)),
                Block.UPDATE_CLIENTS);
    }

    @Override
    public boolean fed(BlockState state) {
        return state.getValue(FED);
    }

    /**
     * A dry reservoir starves the roots; water with the pump off goes stale round them and they rot,
     * which is the classic way a deep-water-culture grow fails. A running pump over water is safe.
     */
    @Override
    public float stress(BlockState state) {
        if (state.getValue(LEVEL) == 0) {
            return PlantStress.DRY_BED;
        }
        return state.getValue(POWERED) ? 0.0F : PlantStress.STAGNANT;
    }

    /**
     * Whether a reservoir can be kept here at all: the solution is water, and water that would boil
     * away cannot be kept in a tray. On this line that is {@code DimensionType.ultrawarm}, the same
     * test a water bucket makes before it pours ({@code BucketItem}).
     */
    public static boolean boilsAway(LevelReader world, BlockPos pos) {
        return world.dimensionType().ultraWarm();
    }

    // ----- filling and feeding -----

    /** Fills the reservoir, if this one can hold water and is not already full. */
    public static boolean fill(Level world, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof HydroTrayBlock) || state.getValue(LEVEL) >= MAX_LEVEL
                || boilsAway(world, pos)) {
            return false;
        }
        world.setBlock(pos, state.setValue(LEVEL, MAX_LEVEL), Block.UPDATE_CLIENTS);
        world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        return true;
    }

    /** Nutrients need water to go into: an empty tray cannot be fed, and a fed one is already mixed. */
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return state.getValue(LEVEL) > 0 && !state.getValue(FED) && !boilsAway(world, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        world.setBlock(pos, state.setValue(FED, true), Block.UPDATE_CLIENTS);
    }

    /**
     * A water bucket fills the reservoir and hands back the empty, the way the Infuser takes its milk
     * and a cauldron takes its water — <b>not</b> by letting vanilla place a water block in the
     * plant's space, which is what happened before. A full tray refuses the bucket by accepting the
     * click and doing nothing ({@code CONSUME}): anything short of accepted goes on to the bucket's
     * own use and pours into the plant's space all the same. Only in the Nether does the bucket go
     * through, so vanilla can pour it and hiss it away. Any fertiliser mixes the nutrients in, and
     * anything else falls through to {@code super} rather than a bare {@code PASS}, which would
     * swallow the click.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.WATER_BUCKET) && state.getValue(LEVEL) < MAX_LEVEL && !boilsAway(world, pos)) {
            if (!world.isClientSide()) {
                fill(world, pos, state);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                player.awardStat(Stats.ITEM_USED.get(Items.WATER_BUCKET));
            }
            return ItemInteractionResult.sidedSuccess(world.isClientSide);
        }
        if (stack.is(Items.WATER_BUCKET) && !boilsAway(world, pos)) {
            return ItemInteractionResult.CONSUME;
        }
        ItemInteractionResult fed = GrowMedium.tryFeed(stack, state, world, pos, player);
        return fed != null ? fed : super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    /** Harvests the plant above first, as the player — see {@link GrowMedium#harvestAbove}. */
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        GrowMedium.harvestAbove(world, pos, player);
        return super.playerWillDestroy(world, pos, state, player);
    }

    // ----- the pump -----

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock,
                                  BlockPos sourcePos, boolean notify) {
        if (world.isClientSide()) {
            return;
        }
        boolean powered = world.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            // Both ways at once, unlike a redstone lamp's four-tick fade: a pump stops when the
            // signal stops, and there is nothing here for a delay to buy.
            world.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    /** A running pump breaks the surface: the state is on the block, but this is what catches the eye. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED) || state.getValue(LEVEL) == 0) {
            return;
        }
        // The air stone breaks the surface under the net pot, so the bubbles come up through the pebbles.
        world.addParticle(ParticleTypes.BUBBLE_POP,
                pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.95,
                pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.0, 0.0);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, FED, POWERED, FACING);
    }
}

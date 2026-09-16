package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.event.GameEvent;
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
 * and slows the crop down. Neglect costs time, never the harvest.
 *
 * <p><b>And it does not work in the Nether.</b> Water boils away there, so the tray refuses a bucket
 * and refuses to be fed; vanilla then places the water itself and evaporates it with the usual hiss,
 * which is the clearest possible answer. The rule is vanilla's own
 * {@code WATER_EVAPORATES_GAMEPLAY}, the attribute {@code BucketItem} checks, so a datapack's own
 * scorching dimension gets it for free — and the Grow Pot is left owning hemp in the Nether.
 */
public class HydroTrayBlock extends Block implements GrowMedium {
    public static final MapCodec<HydroTrayBlock> CODEC = createCodec(HydroTrayBlock::new);

    public static final int MAX_LEVEL = 3;
    /** Water in the reservoir; one level is drunk per plant that ripens. */
    public static final IntProperty LEVEL = IntProperty.of("level", 0, MAX_LEVEL);
    /** Whether that water has been fed — nutrient solution rather than plain water. */
    public static final BooleanProperty FED = BooleanProperty.of("fed");
    /** The pump. Vanilla's own property, so any redstone a player already understands drives it. */
    public static final BooleanProperty POWERED = Properties.POWERED;
    /** Which way the sight tube and the pump face — towards whoever placed it, like a furnace. */
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    /** Wet: the best ground vanilla knows — a lone plant on fully watered farmland. */
    public static final float WET_MOISTURE = GrowPotBlock.FERTILE_MOISTURE;
    /** Dry: vanilla's unwatered farmland, which is worse than the field a player started with. */
    public static final float DRY_MOISTURE = GrowPotBlock.SPENT_MOISTURE;
    public static final float PUMPED_SPEED = 2.0F;

    public HydroTrayBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(LEVEL, 0).with(FED, false).with(POWERED, false).with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    // ----- the bed -----

    @Override
    public float moisture(BlockState state) {
        return state.get(LEVEL) > 0 ? WET_MOISTURE : DRY_MOISTURE;
    }

    @Override
    public float speed(BlockState state) {
        return state.get(LEVEL) > 0 && state.get(FED) && state.get(POWERED) ? PUMPED_SPEED : 1.0F;
    }

    @Override
    public void spend(World world, BlockPos pos, BlockState state) {
        int left = state.get(LEVEL) - 1;
        if (left < 0) {
            return;
        }
        // The nutrients drain with the last of the water: an empty reservoir is an unfed one.
        world.setBlockState(pos, state.with(LEVEL, left).with(FED, left > 0 && state.get(FED)),
                Block.NOTIFY_LISTENERS);
    }

    /**
     * Whether a reservoir can be kept here at all: the solution is water, and water that would boil
     * away cannot be kept in a tray. {@code DimensionType.ultrawarm} is gone on this line; the
     * question is now the environment attribute {@code WATER_EVAPORATES_GAMEPLAY}.
     */
    public static boolean boilsAway(WorldView world, BlockPos pos) {
        return world.getEnvironmentAttributes()
                .getAttributeValue(EnvironmentAttributes.WATER_EVAPORATES_GAMEPLAY, pos);
    }

    // ----- filling and feeding -----

    /** Fills the reservoir, if this one can hold water and is not already full. */
    public static boolean fill(World world, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof HydroTrayBlock) || state.get(LEVEL) >= MAX_LEVEL
                || boilsAway(world, pos)) {
            return false;
        }
        world.setBlockState(pos, state.with(LEVEL, MAX_LEVEL), Block.NOTIFY_LISTENERS);
        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
        return true;
    }

    /** Nutrients need water to go into: an empty tray cannot be fed, and a fed one is already mixed. */
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return state.get(LEVEL) > 0 && !state.get(FED) && !boilsAway(world, pos);
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(FED, true), Block.NOTIFY_LISTENERS);
    }

    /**
     * A water bucket fills the reservoir and hands back the empty, the way the Infuser takes its milk
     * and a cauldron takes its water — <b>not</b> by letting vanilla place a water block in the
     * plant's space, which is what happened before. Any fertiliser mixes the nutrients in. Anything
     * else, and any tray that cannot take what is offered, falls through to {@code super} rather than
     * a bare {@code PASS}, which would swallow the click.
     */
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.isOf(Items.WATER_BUCKET) && state.get(LEVEL) < MAX_LEVEL && !boilsAway(world, pos)) {
            if (!world.isClient()) {
                fill(world, pos, state);
                player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.BUCKET)));
                player.incrementStat(Stats.USED.getOrCreateStat(Items.WATER_BUCKET));
            }
            return ActionResult.SUCCESS;
        }
        ActionResult fed = GrowMedium.tryFeed(stack, state, world, pos, player);
        return fed != null ? fed : super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    // ----- the pump -----

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock,
                                  @Nullable WireOrientation wireOrientation, boolean notify) {
        if (world.isClient()) {
            return;
        }
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered != state.get(POWERED)) {
            // Both ways at once, unlike a redstone lamp's four-tick fade: a pump stops when the
            // signal stops, and there is nothing here for a delay to buy.
            world.setBlockState(pos, state.with(POWERED, powered), Block.NOTIFY_LISTENERS);
        }
    }

    /** A running pump breaks the surface: the state is on the block, but this is what catches the eye. */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(POWERED) || state.get(LEVEL) == 0) {
            return;
        }
        // The air stone breaks the surface under the net pot, so the bubbles come up through the pebbles.
        world.addParticleClient(ParticleTypes.BUBBLE_POP,
                pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.95,
                pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.0, 0.0);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, FED, POWERED, FACING);
    }
}

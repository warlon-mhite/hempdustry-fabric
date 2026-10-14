package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A pressed slab of hash, sitting on the floor, that you cut pieces off with a blade. Two of these
 * exist — brown {@code hashish_bar} and blonde {@link FilteredHashishBarBlock} — and they differ by
 * exactly one method, {@link #piece()}.
 *
 * <p><b>Both are made of what the Hemp Press makes, because a bar is pressed hash.</b> Charas is the
 * only hash with no bar, and for a reason rather than a quantity: it is the only one that is never
 * pressed. Hand-rubbed resin is rolled between the palms, which is why it gets a ball instead.
 *
 * <h2>Why the bar is a block at all</h2>
 *
 * A concentrate you can only ever hold in a chest tells you nothing. <b>A bar is a stash you can
 * see</b> — it sits on a shelf and visibly shrinks as the week goes on, which is the mod's own
 * "you are not wearing a HUD" principle applied to the one item worth hoarding.
 *
 * <p>It is also the storage block the hash family needs. <b>Five cuts yield 2, 2, 2, 2, 1 — nine
 * pieces</b>, and that last single-piece cut is the whole reason the number is nine: {@code 9 ×
 * hashish → 1 hashish_bar} then works as a lossless reverse craft, exactly like every other 9↔9
 * block in the game. Without it the bar would be an item you can only ever destroy, which is the one
 * storage-block shape vanilla never uses.
 *
 * <p><b>There is deliberately no crafting-grid unpack.</b> Every vanilla storage block unpacks with
 * a shapeless recipe; this one unpacks <em>by being cut</em>, because the cut is the mechanic. A
 * shapeless unpack would make the block, the blade and the durability cost all optional.
 *
 * <h2>The blade, and why not shears</h2>
 *
 * {@link ModTags.Items#HASH_CUTTERS} is swords and knives. <b>Shears are deliberately excluded</b>:
 * they already mean "trim a plant" in this mod ({@link Defoliation#tryCut} gates on
 * {@code #c:tools/shear}) and a verb that means exactly one thing should keep meaning exactly one
 * thing. A blade pressed into a slab is the real motion anyway.
 *
 * <p><b>No tier gate.</b> Vanilla's tier gates exist for stone hardness; hash is soft, and a wooden
 * sword cuts cobweb into string. Gold-sword-only would be a gate on content with nothing behind it.
 *
 * <h2>No block entity, and no components on the item</h2>
 *
 * Everything the bar knows is in the blockstate, which is what makes it safe: <b>a {@code BlockItem}
 * only preserves components into a block that has a block entity</b>, so a bar
 * carrying its cut count as a component would lose it the moment it was placed. The count is
 * {@link #CUTS}, and the item has no components at all. Cake has neither a block entity nor a
 * comparator output either.
 */
public class HashishBarBlock extends Block {

    public static final MapCodec<HashishBarBlock> CODEC = simpleCodec(HashishBarBlock::new);

    /** How many cuts have been taken out of this bar, 0–4. At 4 the next cut removes the block. */
    public static final IntegerProperty CUTS = IntegerProperty.create("cuts", 0, 4);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Pieces the next cut yields — 2, 2, 2, 2, then 1. Nine to a bar, so 9↔9 is lossless. */
    private static final int[] YIELD = {2, 2, 2, 2, 1};

    /**
     * Pieces still in a bar at each cut count — what breaking it drops, and what the loot table
     * reads. <b>Derived from {@link #YIELD} rather than written out</b>: the two have to agree
     * exactly or breaking a part-cut bar dupes or eats pieces, and a second hand-maintained array is
     * the obvious place for that to drift.
     *
     * <p>A method rather than a public array, because a {@code public static final int[]} is
     * mutable by anyone holding the class.
     */
    public static int remaining(int cuts) {
        int left = 0;
        for (int i = cuts; i < YIELD.length; i++) {
            left += YIELD[i];
        }
        return left;
    }

    /** Uncut length along the facing axis, in pixels; each cut takes {@link #CUT_PIXELS} off it. */
    private static final double FULL_LENGTH = 14.0;
    /**
     * How much shorter each cut leaves the bar: 14 → 11 → 8 → 5 → 2.
     *
     * <p>Whole pixels on purpose. Dividing 14 evenly over five states gives 2.8 px a cut, which puts
     * every model's UVs off the texel grid — vanilla never does that, and it shows up as a smeared
     * edge rather than as anything that could be called a bug.
     */
    private static final double CUT_PIXELS = 3.0;

    /**
     * What a cut off this bar yields. Overridden by {@link FilteredHashishBarBlock}, which is the
     * whole of the difference between the two bars — everything else, geometry included, is shared.
     *
     * <p>A method rather than a constructor field so both blocks keep a plain
     * {@code Properties}-only constructor and therefore {@code simpleCodec}, which is the one-liner
     * form of a block's {@code MapCodec}. A field would mean hand-rolling a {@code RecordCodecBuilder}
     * twice for a value neither block ever varies at runtime.
     */
    protected Item piece() {
        return ModItems.HASHISH;
    }

    public HashishBarBlock(Properties settings) {
        super(settings);
        // CUTS spelled out even though an IntegerProperty has no BooleanProperty's first-value trap
        // (left out of the default state, a boolean comes back true): writing it means the next
        // person does not have to go and check.
        registerDefaultState(defaultBlockState().setValue(CUTS, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HashishBarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CUTS, FACING);
    }

    /**
     * Lays the bar <b>across</b> the player's view, not pointing away from them.
     *
     * <p>{@code FACING} names the end the bar is anchored at, and cuts eat from the far end — so
     * where it is laid decides whether the player can <em>see</em> it shrink. Pointing it along the
     * line of sight (which {@code getHorizontalDirection().getOpposite()} does, and which this
     * originally did) shows the 8 px end face and foreshortens the 14 px that actually changes: five
     * cuts and the bar barely looks different.
     *
     * <p>{@code getCounterClockWise} turns it side-on. The long axis now runs left-to-right, the
     * bar is anchored at the player's <b>left</b>, and each cut visibly takes a chunk off the right
     * — which is both the way a bar is laid down to be cut and the way a right-handed person cuts
     * it. Consistent for all four facings: the anchor is always the placer's left.
     *
     * <p>Nothing else moves. The models and the {@link #shape} switch already key off {@code FACING}
     * with the same convention, so this is one expression and no new state.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getCounterClockWise());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * Every shape the bar can have: 4 horizontal facings × 5 cut counts, built once at class-load.
     *
     * <p><b>Not built per call.</b> {@code getShape} runs every frame the player is looking
     * at the block, and {@code BlockBehaviour}'s default {@code getCollisionShape} delegates to it —
     * so every entity standing on or beside a bar queries this <em>every tick</em>. Allocating a
     * {@code VoxelShape} there is exactly the garbage vanilla precomputes to avoid, and this file's
     * neighbour {@code IndicaCropBlock} already keeps its own {@code LOWER_SHAPE_BY_AGE} table for
     * the same reason.
     */
    private static final VoxelShape[][] SHAPES = buildShapes();

    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] shapes = new VoxelShape[4][YIELD.length];
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int cuts = 0; cuts < YIELD.length; cuts++) {
                // 8 px across, 4 px tall, and 14 - 3*cuts long, anchored at the end the bar faces so
                // the cuts come off the far one.
                double length = FULL_LENGTH - cuts * CUT_PIXELS;
                shapes[facing.get2DDataValue()][cuts] = switch (facing) {
                    case NORTH -> box(4.0, 0.0, 1.0, 12.0, 4.0, 1.0 + length);
                    case SOUTH -> box(4.0, 0.0, 15.0 - length, 12.0, 4.0, 15.0);
                    case WEST -> box(1.0, 0.0, 4.0, 1.0 + length, 4.0, 12.0);
                    default -> box(15.0 - length, 0.0, 4.0, 15.0, 4.0, 12.0);
                };
            }
        }
        return shapes;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()][state.getValue(CUTS)];
    }

    /**
     * One cut with a blade.
     *
     * <p><b>A refusal hands back {@code super}, never {@code InteractionResult.PASS}.</b> Since 1.21.2
     * {@code ServerPlayerGameMode} calls {@code useItemOn} on every click, empty hand
     * included, and only falls through to {@link #useWithoutItem} when the result is
     * {@code instanceof InteractionResult.TryEmptyHandInteraction} — which is exactly what
     * {@code BlockBehaviour#useItemOn}'s default is. A plain {@code PASS} swallows the click.
     * This shipped wrong in the Sifting Box and was found by a player, not by the game test.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModTags.Items.HASH_CUTTERS)) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }
        int cuts = state.getValue(CUTS);
        if (world instanceof ServerLevel serverWorld) {
            popResourceFromFace(serverWorld, pos, Direction.UP, new ItemStack(piece(), YIELD[cuts]));
            // The blade is the whole cost of a cut: nothing else is spent, and the bar itself is
            // what shrinks. Same slot dance as Defoliation.tryCut.
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            if (cuts + 1 >= YIELD.length) {
                serverWorld.removeBlock(pos, false);
            } else {
                serverWorld.setBlock(pos, state.setValue(CUTS, cuts + 1), Block.UPDATE_ALL);
            }
        }
        // Honey's break sound at a high pitch: sticky and soft, which is what parting a slab of
        // resin sounds like, and it is a sound event that already exists.
        world.playSound(player, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS,
                0.8F, 1.4F + world.getRandom().nextFloat() * 0.2F);
        return InteractionResult.SUCCESS;
    }
}

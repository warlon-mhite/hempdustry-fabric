package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A pressed slab of hash, sitting on the floor, that you cut pieces off with a blade. Two of these
 * exist — brown {@code hashish_bar} and blonde {@link FilteredHashishBarBlock} — and they differ by
 * exactly one method, {@link #piece()}.
 *
 * <p><b>Both come out of the Dry Sifter, because pressing is what that block does.</b> Charas is the
 * only hash with no bar, and now for a reason rather than a quantity: it is the only one that is
 * never pressed. Hand-rubbed resin is rolled between the palms, not squeezed in a screen.
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
 * only preserves components into a block that has a block entity</b> (CLAUDE.md §5), so a bar
 * carrying its cut count as a component would lose it the moment it was placed. The count is
 * {@link #CUTS}, and the item has no components at all. Cake has neither a block entity nor a
 * comparator output either.
 */
public class HashishBarBlock extends Block {

    public static final MapCodec<HashishBarBlock> CODEC = createCodec(HashishBarBlock::new);

    /** How many cuts have been taken out of this bar, 0–4. At 4 the next cut removes the block. */
    public static final IntProperty CUTS = IntProperty.of("cuts", 0, 4);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

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
     * {@code Settings}-only constructor and therefore {@code createCodec}, which is the one-liner
     * form of a block's {@code MapCodec}. A field would mean hand-rolling a {@code RecordCodecBuilder}
     * twice for a value neither block ever varies at runtime.
     */
    protected Item piece() {
        return ModItems.HASHISH;
    }

    public HashishBarBlock(Settings settings) {
        super(settings);
        // CUTS spelled out even though an IntProperty has no BooleanProperty's first-value trap
        // (CLAUDE.md §5): writing it means the next person does not have to go and check.
        setDefaultState(getDefaultState().with(CUTS, 0).with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HashishBarBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CUTS, FACING);
    }

    /**
     * Lays the bar <b>across</b> the player's view, not pointing away from them.
     *
     * <p>{@code FACING} names the end the bar is anchored at, and cuts eat from the far end — so
     * where it is laid decides whether the player can <em>see</em> it shrink. Pointing it along the
     * line of sight (which {@code getHorizontalPlayerFacing().getOpposite()} does, and which this
     * originally did) shows the 8 px end face and foreshortens the 14 px that actually changes: five
     * cuts and the bar barely looks different.
     *
     * <p>{@code rotateYCounterclockwise} turns it side-on. The long axis now runs left-to-right, the
     * bar is anchored at the player's <b>left</b>, and each cut visibly takes a chunk off the right
     * — which is both the way a bar is laid down to be cut and the way a right-handed person cuts
     * it. Consistent for all four facings: the anchor is always the placer's left.
     *
     * <p>Nothing else moves. The models and the {@link #shape} switch already key off {@code FACING}
     * with the same convention, so this is one expression and no new state.
     */
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().rotateYCounterclockwise());
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    /**
     * Every shape the bar can have: 4 horizontal facings × 5 cut counts, built once at class-load.
     *
     * <p><b>Not built per call.</b> {@code getOutlineShape} runs every frame the player is looking
     * at the block, and {@code AbstractBlock}'s default {@code getCollisionShape} delegates to it —
     * so every entity standing on or beside a bar queries this <em>every tick</em>. Allocating a
     * {@code VoxelShape} there is exactly the garbage vanilla precomputes to avoid, and this file's
     * neighbour {@code IndicaCropBlock} already keeps its own {@code LOWER_SHAPE_BY_AGE} table for
     * the same reason.
     */
    private static final VoxelShape[][] SHAPES = buildShapes();

    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] shapes = new VoxelShape[4][YIELD.length];
        for (Direction facing : Direction.Type.HORIZONTAL) {
            for (int cuts = 0; cuts < YIELD.length; cuts++) {
                // 8 px across, 4 px tall, and 14 - 3*cuts long, anchored at the end the bar faces so
                // the cuts come off the far one.
                double length = FULL_LENGTH - cuts * CUT_PIXELS;
                shapes[facing.getHorizontalQuarterTurns()][cuts] = switch (facing) {
                    case NORTH -> createCuboidShape(4.0, 0.0, 1.0, 12.0, 4.0, 1.0 + length);
                    case SOUTH -> createCuboidShape(4.0, 0.0, 15.0 - length, 12.0, 4.0, 15.0);
                    case WEST -> createCuboidShape(1.0, 0.0, 4.0, 1.0 + length, 4.0, 12.0);
                    default -> createCuboidShape(15.0 - length, 0.0, 4.0, 15.0, 4.0, 12.0);
                };
            }
        }
        return shapes;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(FACING).getHorizontalQuarterTurns()][state.get(CUTS)];
    }

    /**
     * One cut with a blade.
     *
     * <p><b>A refusal hands back {@code super}, never {@code ActionResult.PASS}.</b> Since 1.21.2
     * {@code ServerPlayerInteractionManager} calls {@code onUseWithItem} on every click, empty hand
     * included, and only falls through to {@link #onUse} when the result is
     * {@code instanceof ActionResult.PassToDefaultBlockAction} — which is exactly what
     * {@code AbstractBlock#onUseWithItem}'s default is. A plain {@code PASS} swallows the click.
     * This shipped wrong in the Dry Sifter and was found by a player, not by the game test.
     */
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isIn(ModTags.Items.HASH_CUTTERS)) {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }
        int cuts = state.get(CUTS);
        if (world instanceof ServerWorld serverWorld) {
            dropStack(serverWorld, pos, Direction.UP, new ItemStack(piece(), YIELD[cuts]));
            // The blade is the whole cost of a cut: nothing else is spent, and the bar itself is
            // what shrinks. Same slot dance as Defoliation.tryCut.
            stack.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            if (cuts + 1 >= YIELD.length) {
                serverWorld.removeBlock(pos, false);
            } else {
                serverWorld.setBlockState(pos, state.with(CUTS, cuts + 1), Block.NOTIFY_ALL);
            }
        }
        // Honey's break sound at a high pitch: sticky and soft, which is what parting a slab of
        // resin sounds like, and it is a sound event that already exists.
        world.playSound(player, pos, SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundCategory.BLOCKS,
                0.8F, 1.4F + world.getRandom().nextFloat() * 0.2F);
        return ActionResult.SUCCESS;
    }
}

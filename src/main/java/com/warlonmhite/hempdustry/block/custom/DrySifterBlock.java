package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.block.ShapeContext;

/**
 * The Dry Sifter — a screened box that shakes resin off the plant and presses it into hashish.
 *
 * <h2>The real process, and why it is this shape and not a machine</h2>
 *
 * Cannabinoids are made in the trichomes: resin glands on the flowers and, more thinly, on the
 * leaves around them. <b>Dry sifting is a purely mechanical separation</b> — dried plant material is
 * agitated over a fine screen (70–180 µm in the trade), the brittle trichome heads snap off and fall
 * through, the leaf does not, and the powder that lands underneath is pressed with a little heat
 * into a slab. Nothing is heated, dissolved or cooked, so a block with a fuel slot and a progress
 * bar would be telling a lie about the chemistry.
 *
 * <p>What it actually is, is <b>a container you keep putting plant matter into until it fills up</b>
 * — which is exactly the composter's grammar, down to the per-item chance standing in for the yield
 * ratio (real dry sift runs about 5–10% by weight off cured flower, and better off machine-tumbled
 * trim). So this copies {@code ComposterBlock} rather than {@code DecarboxylatorBlock}: a
 * {@code LEVEL} 0–8 in the blockstate, no block entity, no screen, no recipe type.
 *
 * <h2>Balance — why sifting is worth doing</h2>
 *
 * Hashish is a <b>concentrate, not a multiplier</b>: it compresses many plant-parts into one item
 * that the Decarboxylator turns into {@value com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity#HASHISH_OUTPUT}
 * decarboxylated hemp in a single 500-tick cook. Against the direct route:
 *
 * <table border="1">
 *   <caption>Routes to decarboxylated hemp</caption>
 *   <tr><th></th><th>direct</th><th>via hashish</th><th>oven cooks saved</th></tr>
 *   <tr><td>~8 buds</td><td>32</td><td>32</td><td>8 → 1</td></tr>
 *   <tr><td>~23 fan leaves</td><td>23</td><td>32</td><td>23 → 1</td></tr>
 * </table>
 *
 * <p>So <b>buds break even on yield and win eightfold on throughput</b>, and <b>trim wins on both</b>
 * — which is the right answer twice over. It is what actually happens (hash is a trim product before
 * it is anything else), and it gives the fan leaf, the mod's most abundant byproduct, a job worth
 * doing. Neither route ever loses, so a player cannot mis-sift and be punished for it.
 *
 * <p>The two rates live in tags rather than in a hard item check, so a third strain's buds are
 * siftable the day the strain exists and a datapack can widen either side without touching code.
 *
 * <h2>Deliberately not automatable, for now</h2>
 *
 * Vanilla's composter is hopper-fed because {@code ComposterBlock} implements
 * {@code InventoryProvider} and hands out three different fake {@code SidedInventory}s depending on
 * its level. That is real work for a block whose whole point is that it has no inventory, and none
 * of it is needed to play with the thing. Left out on purpose; if it is ever wanted, it is a
 * self-contained addition that changes nothing already written. See {@code machines.md}.
 */
public class DrySifterBlock extends Block {

    public static final MapCodec<DrySifterBlock> CODEC = createCodec(DrySifterBlock::new);

    /** Vanilla's own 0–8 property, so the blockstate reads exactly like a composter's. */
    public static final IntProperty LEVEL = Properties.LEVEL_8;

    /** The level at which the screen is full and a scheduled tick presses it into a slab. */
    public static final int FULL_LEVEL = 7;
    /** The level at which there is a lump of hashish to take out. */
    public static final int READY_LEVEL = 8;
    /** Ticks between "the screen filled up" and "there is hash in it" — vanilla's composter delay. */
    private static final int PRESS_DELAY = 20;

    /**
     * Chance that one fan leaf advances the screen one level.
     *
     * <p>≈23 leaves per lump, against the ~10% by weight real dry sift gets off trim. Deliberately
     * the slower of the two rates: trim is bulky and mostly not resin, and it is also the input a
     * player has most of.
     */
    public static final float TRIM_CHANCE = 0.35F;
    /**
     * Chance that one bud advances the screen one level — certainty, so exactly 8 buds make a lump.
     *
     * <p>That number is chosen, not rounded to: eight buds decarboxylate to 32 either way, so the
     * flower route is exactly break-even on yield and buys nothing but oven time. A concentrate
     * should not conjure potency out of nowhere.
     */
    public static final float FLOWER_CHANCE = 1.0F;

    /**
     * A full cube with the bowl carved out of it, so the screen is something you can drop into and
     * stand in rather than a solid block with a picture of a box on it. Vanilla's composter shape.
     */
    private static final VoxelShape COLLISION_SHAPE = VoxelShapes.combineAndSimplify(
            VoxelShapes.fullCube(),
            Block.createCuboidShape(2.0, 2.0, 2.0, 14.0, 16.0, 14.0),
            BooleanBiFunction.ONLY_FIRST);

    public DrySifterBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends DrySifterBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION_SHAPE;
    }

    /**
     * Shaking plant matter over the screen.
     *
     * <p><b>A refusal must hand back {@code PASS_TO_DEFAULT_BLOCK_ACTION}, not {@code PASS}</b>, and
     * the difference is the whole block working or not. {@code ServerPlayerInteractionManager}
     * calls {@code onUseWithItem} on <em>every</em> click — an empty hand included — and only runs
     * {@link #onUse} afterwards if the result is specifically
     * {@code instanceof ActionResult.PassToDefaultBlockAction}. A plain {@code PASS} swallows the
     * click, so the screen fills up and then can never be emptied. Returning {@code super} is how
     * vanilla's own {@code ComposterBlock} says this, and {@code AbstractBlock#onUseWithItem}'s
     * default is exactly that constant.
     *
     * <p>This shipped as {@code PASS} and was caught in play, not by the game test, because
     * <b>{@code TestContext#useBlock} is more forgiving than the real click</b>: it falls through to
     * {@code onUse} on any non-accepted result. So the invariant has to be asserted on the returned
     * value itself, which is what {@code DrySifterGameTest} now does.
     */
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        int level = state.get(LEVEL);
        float chance = sieveChance(stack);
        if (level >= FULL_LEVEL || chance <= 0.0F) {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }
        if (world instanceof ServerWorld serverWorld) {
            sift(serverWorld, pos, state, level, chance);
        }
        // Outside the server branch, as vanilla's composter does it: the cost is the same on both
        // sides whatever the roll, so spending it client-side too is what makes the stack shrink on
        // the frame you click rather than on the frame the server's answer arrives.
        stack.decrementUnlessCreative(1, player);
        return ActionResult.SUCCESS;
    }

    /** Taking the pressed lump out. Empty-handed, or holding anything the screen does not accept. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (state.get(LEVEL) != READY_LEVEL) {
            return ActionResult.PASS;
        }
        if (world instanceof ServerWorld serverWorld) {
            collect(serverWorld, pos, state);
        }
        return ActionResult.SUCCESS;
    }

    /** {@link #FULL_LEVEL} → {@link #READY_LEVEL}: the powder gets pressed. */
    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(LEVEL) == FULL_LEVEL) {
            world.setBlockState(pos, state.with(LEVEL, READY_LEVEL), Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_READY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    /** Fill level, 0–8, exactly as a composter reports it. */
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        return state.get(LEVEL);
    }

    /**
     * How much one of this item advances the screen, or 0 if it is not plant matter the screen can
     * separate anything out of.
     */
    private static float sieveChance(ItemStack stack) {
        if (stack.isIn(ModTags.Items.SIFTABLE_FLOWER)) {
            return FLOWER_CHANCE;
        }
        if (stack.isIn(ModTags.Items.SIFTABLE_TRIM)) {
            return TRIM_CHANCE;
        }
        return 0.0F;
    }

    private static void sift(ServerWorld world, BlockPos pos, BlockState state, int level, float chance) {
        if (world.random.nextFloat() < chance) {
            int next = level + 1;
            world.setBlockState(pos, state.with(LEVEL, next), Block.NOTIFY_ALL);
            if (next == FULL_LEVEL) {
                world.scheduleBlockTick(pos, state.getBlock(), PRESS_DELAY);
            }
            world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_FILL_SUCCESS, SoundCategory.BLOCKS, 1.0F, 1.0F);
        } else {
            world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static void collect(ServerWorld world, BlockPos pos, BlockState state) {
        Vec3d spawn = Vec3d.add(pos, 0.5, 1.01, 0.5).addRandom(world.random, 0.7F);
        ItemEntity dropped = new ItemEntity(world, spawn.getX(), spawn.getY(), spawn.getZ(),
                new ItemStack(ModItems.HASHISH));
        dropped.setToDefaultPickupDelay();
        world.spawnEntity(dropped);
        world.setBlockState(pos, state.with(LEVEL, 0), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
}

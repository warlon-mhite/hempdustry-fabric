package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.ModBlocks;
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
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
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
import org.jetbrains.annotations.Nullable;

/**
 * The Dry Sifter — a screened box that shakes resin off the plant and presses it into a hashish bar.
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
 * <b>The screen fills in seven, not eight</b> — {@link #sift} adds one level per accepted item and
 * {@link #onUseWithItem} refuses at {@link #FULL_LEVEL}, which is vanilla's own composter count. So
 * one bar is seven buds, or ~47 fan leaves at {@link #TRIM_CHANCE}, or any mix of the two.
 *
 * <p><b>Nothing that comes out of here decarboxylates.</b> The oven takes plant matter and the bowl
 * takes resin; hash is a smoking material and only a smoking material. That is a rule rather than an
 * omission, and it is what makes the block's two inputs answer two different questions:
 *
 * <table border="1">
 *   <caption>What a sift is worth</caption>
 *   <tr><th>in</th><th>what it would otherwise have been</th><th>what sifting makes it</th></tr>
 *   <tr><td>7 buds</td><td>7 hits of that strain</td><td><b>9 hits of hash</b></td></tr>
 *   <tr><td>~47 fan leaves</td><td>~47 decarboxylated hemp, for the edible chain</td><td><b>9 hits of hash</b></td></tr>
 * </table>
 *
 * <p><b>Buds buy volume and pay for it with identity.</b> Nine pieces out of seven buds is a
 * deliberate 1.29×, and what it costs is the strain — Purple Kush's Resistance and Lemon Haze's
 * Speed both become the same hash body. A concentrate compresses; it does not conjure potency.
 *
 * <p><b>Trim is a genuine fork, not a ladder.</b> A hemp plant yields exactly five fan leaves however
 * it is trimmed — a cut moves leaf-at-harvest into buds 1:1 and hands the leaf straight back — so
 * leaves are <em>fixed</em> rather than abundant, and they now have two sinks worth having. Forty-odd
 * of them is either a great deal of cannabutter or nine smokes: different currencies, neither
 * strictly better. <b>Ten plants' worth of trim makes one bar.</b>
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

    /**
     * What is on the screen. <b>Plant and resin do not share one</b> — that is the whole rule, and
     * it is one rule rather than a list of exceptions.
     *
     * <p>Buds and leaf mix freely, because both are plant matter with trichomes on them and the two
     * rates already say how much. Resin is a different pass entirely: it is a <em>re-sift</em> at a
     * finer mesh, and what comes out is not a bar.
     */
    public enum Content implements StringIdentifiable {
        PLANT, HASH;

        @Override
        public String asString() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Content> CONTENT = EnumProperty.of("content", Content.class);

    /** The level at which the screen is full and a scheduled tick presses it into a slab. */
    public static final int FULL_LEVEL = 7;
    /** The level at which there is a pressed bar to take out. */
    public static final int READY_LEVEL = 8;
    /** Ticks between "the screen filled up" and "there is hash in it" — vanilla's composter delay. */
    private static final int PRESS_DELAY = 20;

    /**
     * Chance that one fan leaf advances the screen one level.
     *
     * <p>{@link #FULL_LEVEL} / 0.15 ≈ <b>47 leaves per lump</b>, which is ~9–10 plants' worth of
     * trim: <b>ten plants make one bar</b> is the whole rule of thumb, and it is one constant.
     * Deliberately the slower of the two rates — trim is bulky and mostly not resin, and it is also
     * the input a player has most of.
     *
     * <p><b>Was 0.35, which was far too generous.</b> At that rate a bar cost ~20 leaves ≈ 4 plants,
     * and those same 4 plants had already handed over 16 buds — so the screen added nine hits on top
     * of sixteen, a 56% free increase in total smokeable output for right-clicking a box with waste.
     * 0.15 makes it ~24%, which is a bonus rather than a second harvest.
     */
    public static final float TRIM_CHANCE = 0.15F;
    /**
     * Chance that one bud advances the screen one level — certainty, so exactly 7 buds make a lump.
     *
     * <p>Certainty rather than a rate, because seven <em>is</em> the price and a player should be
     * able to count it. What sifting buds buys is volume — nine pieces out of seven buds — and what
     * it costs is the strain. A concentrate should not conjure potency out of nowhere.
     */
    public static final float FLOWER_CHANCE = 1.0F;
    /**
     * Chance that one piece of resin advances the screen.
     *
     * <p><b>This is where filtration's cost lives, and it is the whole of it.</b> Both content kinds
     * press a bar and a bar is always nine pieces, so the loss cannot be taken out of the output —
     * it has to be taken out of the input. {@link #FULL_LEVEL} / 0.4 ≈ <b>17.5 hashish per filtered
     * bar</b>, near enough two bars: <b>two bars of hash make one bar of filtered</b>, a ~49% loss.
     *
     * <p>That is what a further sieve pass costs, and why the trade stops at three passes: the
     * purity gain goes marginal and the yield loss does not. And what purity buys is
     * <b>smoothness, not power</b> — filtered hashish has the same effects at the same strength and
     * half the green-out odds, so it is a true sidegrade rather than an upgrade. See
     * {@code Strain.greenOutFactor}.
     *
     * <p>Unlike {@link #FLOWER_CHANCE} this is deliberately <em>not</em> certainty. Seven buds is a
     * number a player should be able to count; "about two bars" is a rule of thumb, and a screen
     * that sometimes takes an extra piece is the composter grammar this block already runs on.
     */
    public static final float HASH_CHANCE = 0.4F;

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
        setDefaultState(getDefaultState().with(LEVEL, 0).with(CONTENT, Content.PLANT));
    }

    @Override
    protected MapCodec<? extends DrySifterBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, CONTENT);
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
        Content content = contentOf(stack);
        // Plant and resin do not share a screen. An empty screen takes whichever arrives first and
        // is stamped with it; a part-full one refuses the other kind outright -- with super, never
        // PASS, so the click still falls through to onUse and a ready screen can still be emptied.
        if (level >= FULL_LEVEL || chance <= 0.0F || content == null
                || (level > 0 && content != state.get(CONTENT))) {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }
        if (world instanceof ServerWorld serverWorld) {
            sift(serverWorld, pos, level == 0 ? state.with(CONTENT, content) : state, level, chance);
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
        if (stack.isIn(ModTags.Items.SIFTABLE_HASH)) {
            return HASH_CHANCE;
        }
        return 0.0F;
    }

    /**
     * Which kind of screen this item belongs on, or {@code null} if the screen takes it at all.
     *
     * <p>Deliberately a companion to {@link #sieveChance} rather than folded into it: the rate and
     * the kind are two different questions, and the two plant rates <em>are</em> the balance of the
     * block while the kind is a rule about what may share a screen.
     */
    @Nullable
    private static Content contentOf(ItemStack stack) {
        if (stack.isIn(ModTags.Items.SIFTABLE_FLOWER) || stack.isIn(ModTags.Items.SIFTABLE_TRIM)) {
            return Content.PLANT;
        }
        return stack.isIn(ModTags.Items.SIFTABLE_HASH) ? Content.HASH : null;
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

    /**
     * Taking the batch out. <b>Both content kinds press a bar, because pressing is what this block
     * does</b> — a brown one from plant matter, a blonde one from resin.
     *
     * <p>They are the same block bar the colour and what a cut yields (see {@code HashishBarBlock}),
     * so a player who has learned one has learned the other, and the cost of filtering is taken out
     * of the <em>input</em> instead — see {@link #HASH_CHANCE}.
     *
     * <p><b>Charas is the only hash with no bar</b>, and now for a reason rather than a quantity: it
     * is the only one that is never pressed. Hand-rubbed resin is rolled between the palms.
     */
    private static void collect(ServerWorld world, BlockPos pos, BlockState state) {
        ItemStack yield = new ItemStack(state.get(CONTENT) == Content.HASH
                ? ModBlocks.FILTERED_HASHISH_BAR
                : ModBlocks.HASHISH_BAR);
        Vec3d spawn = Vec3d.add(pos, 0.5, 1.01, 0.5).addRandom(world.random, 0.7F);
        ItemEntity dropped = new ItemEntity(world, spawn.getX(), spawn.getY(), spawn.getZ(), yield);
        dropped.setToDefaultPickupDelay();
        world.spawnEntity(dropped);
        world.setBlockState(pos, state.with(LEVEL, 0).with(CONTENT, Content.PLANT), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
}

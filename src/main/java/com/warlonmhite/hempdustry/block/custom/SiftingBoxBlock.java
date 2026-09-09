package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
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
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The Sifting Box — a screened tub that separates trichome heads from plant matter. <b>Two
 * processes, one block, and the water is what chooses between them.</b>
 *
 * <h2>The rule this block exists to obey</h2>
 *
 * <b>Separation is a vessel; transformation is a machine.</b> A vessel needs a <em>condition</em> —
 * a screen, cold, water — and gets a blockstate and nothing else: no block entity, no screen, no
 * recipe type. A machine needs <em>fuel or heat</em> and gets a GUI and a recipe type. The Sifting
 * Box is a vessel and the Hemp Press is a machine, and that split is the whole reason this block no
 * longer hands you a pressed bar.
 *
 * <p><b>It used to, and that was the bug.</b> In the trade the powder under the screen is
 * <em>kief</em>, and it is <em>pressed with heat</em> to become hashish — two steps, two tools. This
 * block did both, which was defensible only while the mod had no press. Now it has one, and this
 * block makes powder.
 *
 * <h2>Dry, and wet</h2>
 *
 * <table border="1">
 *   <caption>What the water changes</caption>
 *   <tr><th></th><th>Dry</th><th>Filled, and jacketed in ice</th></tr>
 *   <tr><td>Process</td><td>dry sift — beat it over the screen</td><td>ice-water wash — the Ice-o-lator</td></tr>
 *   <tr><td>Needs</td><td>nothing</td><td>a bucket of water, and ice on all four sides</td></tr>
 *   <tr><td>Yields</td><td>{@value #YIELD} × {@code kief}</td><td>{@value #YIELD} × {@code bubble_hash}</td></tr>
 *   <tr><td>Rate</td><td>full</td><td>the jacket's — at best equal, never better</td></tr>
 * </table>
 *
 * <p>This is what a bubble bag in a bucket physically is, so one block is the honest count rather
 * than a saving. <b>"Ice-o-lator" survives as the name of the wash</b>, which is historically right:
 * it was a brand of <em>bags</em>, never a machine.
 *
 * <h2>The ice jacket</h2>
 *
 * <b>The wash only runs while all four sides are ice</b>, and the tier is the <em>weakest</em> of
 * the four, so a jacket is something you either built or did not. It inverts both machines with no
 * text at all — the Decarboxylator burns fuel, the Infuser is heated from below, and this has to be
 * kept cold — and it is the beacon's pyramid at the smallest possible scale.
 *
 * <table border="1">
 *   <caption>What a jacket costs and what it is worth</caption>
 *   <tr><th>Jacket</th><th>Cost</th><th>Melts?</th><th>Rate</th><th>Buds per batch</th></tr>
 *   <tr><td>Blue ice</td><td>81 ice each</td><td>no</td><td>{@value #BLUE_ICE_RATE}</td><td><b>7</b>, equal to dry</td></tr>
 *   <tr><td>Packed ice</td><td>9 ice each</td><td>no</td><td>{@value #PACKED_ICE_RATE}</td><td>~9</td></tr>
 *   <tr><td>Ice</td><td>free in a snowy biome, or Silk Touch</td><td><b>yes</b>, above light 11</td><td>{@value #ICE_RATE}</td><td>~14</td></tr>
 * </table>
 *
 * <p><b>Even at its best the wash only matches dry sifting, and never beats it</b> — which is what
 * the trade says too: ice-water runs 3–8% off flower against dry sift's 5–10%. What it buys is
 * cleanliness, and cleanliness shows up two steps later as {@code filtered_hashish}'s halved
 * green-out. Anything less than blue ice is a <em>worse</em> yield than simply staying dry, and that
 * is correct rather than a trap: the dry route is always right there, free, in the same block.
 *
 * <p><b>Plain ice melting is the best part and the mod writes no code for it.</b> Vanilla's
 * {@code IceBlock} melts on a random tick above light level 11, so lighting your extraction room
 * dissolves your jacket and the wash quietly stops. Build it dark, or spend nine-to-one on packed
 * ice and stop worrying. Blue ice is also the slipperiest block in the game, so a serious extraction
 * room is a skating rink; that is free.
 *
 * <p>Because that failure is silent by default, <b>the box announces when it is cold</b> — frost
 * while it is washing, nothing when it is not, keyed on exactly the condition the interaction
 * checks. {@code machines.md} records the Infuser shipping with a glow that did not come from its
 * heat source; this is the same lesson, inverted.
 *
 * <h2>Not automatable, for now</h2>
 *
 * Vanilla's composter is hopper-fed because {@code ComposterBlock} implements
 * {@code InventoryProvider} and hands out three different fake {@code SidedInventory}s depending on
 * its level. That is real work for a block whose whole point is that it has no inventory. Left out
 * on purpose; it changes nothing already written.
 */
public class SiftingBoxBlock extends Block {

    public static final MapCodec<SiftingBoxBlock> CODEC = createCodec(SiftingBoxBlock::new);

    /** Vanilla's own 0–8 property, so the blockstate reads exactly like a composter's. */
    public static final IntProperty LEVEL = Properties.LEVEL_8;
    /**
     * Whether there is water in the tub — and therefore <b>which of the two processes is running</b>.
     *
     * <p>Set by a bucket, and spent when the batch is taken out rather than when it finishes. That
     * is deliberate: a finished batch has to still remember which process made it, and carrying the
     * water through to collection is a free way to say so. A separate "what is in here" property
     * would be a second source of truth for the same fact.
     */
    public static final BooleanProperty FILLED = BooleanProperty.of("filled");

    /**
     * What is on the screen. <b>Plant matter and powder do not share one</b> — that is the whole
     * rule, and it is one rule rather than a list of exceptions.
     *
     * <p>Buds and leaf mix freely, because both are plant matter with trichomes on them and the two
     * rates already say how much. Powder is a different pass entirely: it is a <em>re-sift</em> at a
     * finer mesh, and what comes out is not kief.
     */
    public enum Content implements StringIdentifiable {
        PLANT, KIEF;

        @Override
        public String asString() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Content> CONTENT = EnumProperty.of("content", Content.class);

    /** The level at which the screen is full and a scheduled tick settles the batch. */
    public static final int FULL_LEVEL = 7;
    /** The level at which there is a batch to take out. */
    public static final int READY_LEVEL = 8;
    /** Ticks between "the screen filled up" and "there is something in it" — the composter's delay. */
    private static final int SETTLE_DELAY = 20;

    /**
     * What one batch hands over.
     *
     * <p><b>Nine, because a bar is nine</b> — {@code 7 buds → 9 pieces} is the volume gain sifting
     * has always bought, and inserting the press between them must not change it. Each piece then
     * presses one-for-one, so the whole chain still reads: seven buds, nine smokes.
     */
    public static final int YIELD = 9;

    /**
     * Chance that one fan leaf advances the screen one level.
     *
     * <p>{@link #FULL_LEVEL} / 0.15 ≈ <b>47 leaves per batch</b>, which is ~9–10 plants' worth of
     * trim: <b>ten plants make one batch</b> is the whole rule of thumb, and it is one constant.
     * Deliberately the slower of the two rates — trim is bulky and mostly not resin, and it is also
     * the input a player has most of.
     */
    public static final float TRIM_CHANCE = 0.15F;
    /**
     * Chance that one bud advances the screen — certainty, so exactly 7 buds make a batch.
     *
     * <p>Certainty rather than a rate, because seven <em>is</em> the price and a player should be
     * able to count it. What sifting buds buys is volume — nine pieces out of seven buds — and what
     * it costs is the strain. A concentrate should not conjure potency out of nowhere.
     */
    public static final float FLOWER_CHANCE = 1.0F;

    /**
     * Chance that one kief advances the re-sift.
     *
     * <p><b>This is where the dry road's cost lives, and it is the whole of it.</b> A batch is always
     * {@value #YIELD} pieces, so the loss cannot come out of the output — it comes out of the input.
     * {@link #FULL_LEVEL} / 0.25 = <b>28 kief per batch</b>, so about <b>3.1 kief per filtered
     * kief</b>: roughly 22 buds' worth of plant matter against <b>7</b> through a blue-ice jacket.
     *
     * <p>Steep on purpose, and the trade backs it: <i>"past three passes the purity gain goes
     * marginal and the yield loss does not"</i>, which is exactly why the market stopped at three.
     * <b>The two roads reach the same product because they buy the same thing</b> — smoothness, not
     * potency, which is what {@code green_out_factor} models and the only axis the mod has. What
     * separates them is price, so the ice room is an efficiency play rather than a wall:
     *
     * <table border="1">
     *   <caption>Buds per 9 filtered hashish</caption>
     *   <tr><th>dry re-sift</th><th>plain ice</th><th>packed ice</th><th>blue ice</th></tr>
     *   <tr><td>~22</td><td>14</td><td>~9</td><td><b>7</b></td></tr>
     * </table>
     *
     * <p>Deliberately <em>not</em> certainty, unlike {@link #FLOWER_CHANCE}. Seven buds is a number a
     * player should be able to count; "about three kief each" is a rule of thumb, and a screen that
     * sometimes wants one more is the composter grammar this block already runs on.
     */
    public static final float KIEF_CHANCE = 0.25F;

    /** Blue ice: parity with dry sifting. 7 buds a batch, ~47 leaves. */
    public static final float BLUE_ICE_RATE = 1.0F;
    /** Packed ice: ~9 buds a batch. Nine ice a block, and it never melts. */
    public static final float PACKED_ICE_RATE = 0.75F;
    /** Plain ice: ~14 buds a batch — and it melts the moment the room is lit. */
    public static final float ICE_RATE = 0.5F;

    /**
     * A full cube with the tub carved out of it, so the box is something you can drop into and stand
     * in rather than a solid block with a picture of a box on it. Vanilla's composter shape.
     */
    private static final VoxelShape COLLISION_SHAPE = VoxelShapes.combineAndSimplify(
            VoxelShapes.fullCube(),
            Block.createCuboidShape(2.0, 2.0, 2.0, 14.0, 16.0, 14.0),
            BooleanBiFunction.ONLY_FIRST);

    public SiftingBoxBlock(Settings settings) {
        super(settings);
        // Both named explicitly. A BooleanProperty left out of the default state comes back TRUE,
        // because its values are ImmutableSet.of(true, false) -- the trap that shipped pre-trimmed
        // seeds for two days (CLAUDE.md §5). A box that starts full of water is the same bug.
        setDefaultState(getDefaultState().with(LEVEL, 0).with(FILLED, false).with(CONTENT, Content.PLANT));
    }

    @Override
    protected MapCodec<? extends SiftingBoxBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, FILLED, CONTENT);
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
     * Filling the tub, and working plant matter over the screen.
     *
     * <p><b>Every refusal hands back {@code super}, never {@code PASS}.</b> Since 1.21.2
     * {@code ServerPlayerInteractionManager} calls this on every click, empty hand included, and
     * only falls through to {@link #onUse} when the result is {@code PASS_TO_DEFAULT_BLOCK_ACTION} —
     * which is what {@code AbstractBlock#onUseWithItem}'s default is. A plain {@code PASS} swallows
     * the click, so the box fills up and can then never be emptied. This block shipped that bug once
     * and a player found it, not the game test.
     */
    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        int level = state.get(LEVEL);

        // A bucket of water turns the box into an Ice-o-lator. Only into an EMPTY box: a half-sifted
        // dry batch must not be convertible into a wash, or the water would be retroactively
        // deciding what the last three buds became.
        if (stack.isOf(Items.WATER_BUCKET) && !state.get(FILLED) && level == 0) {
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.setBlockState(pos, state.with(FILLED, true), Block.NOTIFY_ALL);
                // Server-side and through ItemUsage#exchangeStack, exactly as vanilla's cauldron and
                // ModCauldronBehaviors do it. Swapping the held stack on both sides mints a bucket
                // the next inventory sync takes away, and creativeOverride is what stops a creative
                // player minting a fresh one on every click.
                player.setStackInHand(hand,
                        ItemUsage.exchangeStack(stack, player, new ItemStack(Items.BUCKET)));
                player.incrementStat(Stats.USED.getOrCreateStat(Items.WATER_BUCKET));
                serverWorld.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                serverWorld.emitGameEvent(player, GameEvent.FLUID_PLACE, pos);
            }
            return ActionResult.SUCCESS;
        }

        Content content = contentOf(stack);
        float chance = sieveChance(stack) * rate(world, pos, state);
        // An empty screen takes whichever arrives first and is stamped with it; a part-full one
        // refuses the other kind outright -- with super, never PASS, so the click still falls
        // through to onUse and a ready screen can still be emptied.
        //
        // A WASH TAKES PLANT MATTER AND NOTHING ELSE. Ice-washing kief is a real thing, but it is a
        // third road to a product that already has two, and every extra road here is one more way
        // for one of them to become a trap.
        if (level >= FULL_LEVEL || chance <= 0.0F || content == null
                || (state.get(FILLED) && content != Content.PLANT)
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

    /** Taking the batch out. Empty-handed, or holding anything the box does not accept. */
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

    /** {@link #FULL_LEVEL} → {@link #READY_LEVEL}: the powder settles and is ready to take. */
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
     * Frost off the surface while the wash is actually cold and actually working.
     *
     * <p>The one thing this block must not do is fail silently, and its failure mode is a torch: the
     * jacket melts, the wash stops, and nothing says so. So the particles are keyed on exactly the
     * condition {@link #onUseWithItem} checks, and they stop the moment it does. A dry box needs no
     * signal, because a dry box has no condition to fail.
     */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(FILLED) || state.get(LEVEL) >= READY_LEVEL || jacketRate(world, pos) <= 0.0F) {
            return;
        }
        world.addParticleClient(ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                pos.getY() + 0.9D,
                pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                0.0D, 0.02D, 0.0D);
    }

    /**
     * The multiplier this box is running at: <b>1.0 dry, the jacket's tier wet, and 0 for a wet box
     * with no jacket at all</b> — which is what makes a melted jacket stop the wash dead.
     */
    public static float rate(BlockView world, BlockPos pos, BlockState state) {
        return state.get(FILLED) ? jacketRate(world, pos) : 1.0F;
    }

    /**
     * How cold the jacket is: the rate of the <b>weakest</b> of the four sides, or {@code 0} if any
     * side is not ice at all.
     *
     * <p>The weakest rather than an average, so a jacket is a thing you either built or did not.
     * Four sides rather than six: the box is open at the top and something has to hold it up.
     */
    public static float jacketRate(BlockView world, BlockPos pos) {
        float worst = BLUE_ICE_RATE;
        for (Direction side : Direction.Type.HORIZONTAL) {
            float rate = iceRate(world.getBlockState(pos.offset(side)));
            if (rate <= 0.0F) {
                return 0.0F;
            }
            worst = Math.min(worst, rate);
        }
        return worst;
    }

    private static float iceRate(BlockState state) {
        if (state.isOf(Blocks.BLUE_ICE)) {
            return BLUE_ICE_RATE;
        }
        if (state.isOf(Blocks.PACKED_ICE)) {
            return PACKED_ICE_RATE;
        }
        // Frosted ice counts, and is a joke worth leaving in: Frost Walker boots keep a jacket alive
        // for exactly as long as you stand there and no longer.
        return state.isOf(Blocks.ICE) || state.isOf(Blocks.FROSTED_ICE) ? ICE_RATE : 0.0F;
    }

    /**
     * How much one of this item is worth on the screen at full rate, or 0 if there is nothing in it
     * to separate.
     *
     * <p><b>Both processes take the same two inputs</b>, which is the point: the water changes what
     * comes out, never what goes in. The two rates live in tags rather than in an item check, so a
     * third strain's buds are siftable the day the strain exists and a datapack can widen either
     * side without touching code.
     *
     * <p>There is no third rate for resin any more. <b>The box does not re-sift</b>: the ice wash
     * replaced that pass, and it is the honest version of it — what a further sieve buys is
     * cleanliness, which is exactly what the wash buys.
     */
    private static float sieveChance(ItemStack stack) {
        if (stack.isIn(ModTags.Items.SIFTABLE_FLOWER)) {
            return FLOWER_CHANCE;
        }
        if (stack.isIn(ModTags.Items.SIFTABLE_TRIM)) {
            return TRIM_CHANCE;
        }
        return stack.isIn(ModTags.Items.SIFTABLE_KIEF) ? KIEF_CHANCE : 0.0F;
    }

    /**
     * Which kind of screen this item belongs on, or {@code null} if the box takes it at all.
     *
     * <p>Deliberately a companion to {@link #sieveChance} rather than folded into it: the rate and
     * the kind are two different questions, and the plant rates <em>are</em> the balance of the block
     * while the kind is a rule about what may share a screen.
     */
    @Nullable
    private static Content contentOf(ItemStack stack) {
        if (stack.isIn(ModTags.Items.SIFTABLE_FLOWER) || stack.isIn(ModTags.Items.SIFTABLE_TRIM)) {
            return Content.PLANT;
        }
        return stack.isIn(ModTags.Items.SIFTABLE_KIEF) ? Content.KIEF : null;
    }

    private static void sift(ServerWorld world, BlockPos pos, BlockState state, int level, float chance) {
        if (world.random.nextFloat() < chance) {
            int next = level + 1;
            world.setBlockState(pos, state.with(LEVEL, next), Block.NOTIFY_ALL);
            if (next == FULL_LEVEL) {
                world.scheduleBlockTick(pos, state.getBlock(), SETTLE_DELAY);
            }
            world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_FILL_SUCCESS, SoundCategory.BLOCKS, 1.0F, 1.0F);
        } else if (state.get(FILLED)) {
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 0.6F, 1.4F);
        } else {
            world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * Taking the batch out: <b>{@value #YIELD} loose pieces, never a bar.</b> Which piece depends on
     * which process ran, and the water is spent along with the batch.
     *
     * <p>Nothing here is pressed. Pressing is the Hemp Press's job and this is a vessel — see the
     * class comment. What comes out of a screen is powder, which is why neither of these is
     * smokeable until it has been through the press.
     */
    private static void collect(ServerWorld world, BlockPos pos, BlockState state) {
        Item yield;
        if (state.get(CONTENT) == Content.KIEF) {
            yield = ModItems.FILTERED_KIEF;   // the dry re-sift; a wash never holds powder
        } else {
            yield = state.get(FILLED) ? ModItems.BUBBLE_HASH : ModItems.KIEF;
        }
        Vec3d spawn = Vec3d.add(pos, 0.5, 1.01, 0.5).addRandom(world.random, 0.7F);
        ItemEntity dropped = new ItemEntity(world, spawn.getX(), spawn.getY(), spawn.getZ(),
                new ItemStack(yield, YIELD));
        dropped.setToDefaultPickupDelay();
        world.spawnEntity(dropped);
        world.setBlockState(pos, state.with(LEVEL, 0).with(FILLED, false).with(CONTENT, Content.PLANT),
                Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
}

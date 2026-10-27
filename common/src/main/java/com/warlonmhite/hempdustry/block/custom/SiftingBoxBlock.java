package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
 * {@code WorldlyContainerHolder} and hands out three different fake {@code WorldlyContainer}s depending on
 * its level. That is real work for a block whose whole point is that it has no inventory. Left out
 * on purpose; it changes nothing already written.
 */
public class SiftingBoxBlock extends Block {

    public static final MapCodec<SiftingBoxBlock> CODEC = simpleCodec(SiftingBoxBlock::new);

    /** Vanilla's own 0–8 property, so the blockstate reads exactly like a composter's. */
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;
    /**
     * Whether there is water in the tub — and therefore <b>which of the two processes is running</b>.
     *
     * <p>Set by a bucket, and spent when the batch is taken out rather than when it finishes. That
     * is deliberate: a finished batch has to still remember which process made it, and carrying the
     * water through to collection is a free way to say so. A separate "what is in here" property
     * would be a second source of truth for the same fact.
     */
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    /**
     * What is on the screen. <b>Plant matter and powder do not share one</b> — that is the whole
     * rule, and it is one rule rather than a list of exceptions.
     *
     * <p>Buds and leaf mix freely, because both are plant matter with trichomes on them and the two
     * rates already say how much. Powder is a different pass entirely: it is a <em>re-sift</em> at a
     * finer mesh, and what comes out is not kief.
     */
    public enum Content implements StringRepresentable {
        PLANT, KIEF;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);

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
     * What one <b>resinous</b> bud moves the screen: <b>two levels</b>, so 4 buds a batch against 7.
     * Beldía, the Rif landrace grown for the sieve, is the one in {@code #hempdustry:siftable/resinous}.
     *
     * <p>The kief is the same kief — a hash is named by its process, never its strain. The plant just
     * gets there on fewer buds, and the jacket's rate multiplies this exactly as it does a bud, so
     * the dry-against-wash price ratio is unchanged. <b>Two, not more:</b> in whole buds 2.5 and 3
     * both come to 3, one bud from here, and a Grow Lamp plant (4 buds, 8 levels) already fills a
     * batch on its own at two.
     */
    public static final float RESINOUS_CHANCE = 2.0F;

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
    private static final VoxelShape COLLISION_SHAPE = Shapes.join(
            Shapes.block(),
            Block.box(2.0, 2.0, 2.0, 14.0, 16.0, 14.0),
            BooleanOp.ONLY_FIRST);

    public SiftingBoxBlock(Properties settings) {
        super(settings);
        // Both named explicitly. A BooleanProperty left out of the default state comes back TRUE,
        // because its values are ImmutableSet.of(true, false) -- the trap that shipped pre-trimmed
        // seeds for two days. A box that starts full of water is the same bug.
        registerDefaultState(defaultBlockState().setValue(LEVEL, 0).setValue(FILLED, false).setValue(CONTENT, Content.PLANT));
    }

    @Override
    protected MapCodec<? extends SiftingBoxBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, FILLED, CONTENT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    /**
     * Filling the tub, and working plant matter over the screen.
     *
     * <p><b>Every refusal hands back {@code super}, never {@code PASS}.</b> Since 1.21.2
     * {@code ServerPlayerGameMode} calls this on every click, empty hand included, and
     * only falls through to {@link #useWithoutItem} when the result is {@code TRY_WITH_EMPTY_HAND} —
     * which is what {@code BlockBehaviour#useItemOn}'s default is. A plain {@code PASS} swallows
     * the click, so the box fills up and can then never be emptied. This block shipped that bug once
     * and a player found it, not the game test.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        int level = state.getValue(LEVEL);

        // A bucket of water turns the box into an Ice-o-lator. Only into an EMPTY box: a half-sifted
        // dry batch must not be convertible into a wash, or the water would be retroactively
        // deciding what the last three buds became.
        if (stack.is(Items.WATER_BUCKET) && !state.getValue(FILLED) && level == 0) {
            if (world instanceof ServerLevel serverWorld) {
                serverWorld.setBlock(pos, state.setValue(FILLED, true), Block.UPDATE_ALL);
                // Server-side and through ItemUtils#createFilledResult, exactly as vanilla's cauldron and
                // ModCauldronBehaviors do it. Swapping the held stack on both sides mints a bucket
                // the next inventory sync takes away, and creativeOverride is what stops a creative
                // player minting a fresh one on every click.
                player.setItemInHand(hand,
                        ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                player.awardStat(Stats.ITEM_USED.get(Items.WATER_BUCKET));
                serverWorld.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                serverWorld.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.SUCCESS;
        }
        // Any other water bucket is turned down with CONSUME, not super: past a refusal the client
        // goes on to the bucket's own use and pours it out on top of the box, which looks as if the
        // box took it. A ready box still falls through, so the bucket's holder can empty it.
        if (stack.is(Items.WATER_BUCKET) && level != READY_LEVEL) {
            return InteractionResult.CONSUME;
        }

        Content content = contentOf(stack);
        float chance = sieveChance(stack) * rate(world, pos, state);
        // An empty screen takes whichever arrives first and is stamped with it; a part-full one
        // refuses the other kind outright -- with super, never PASS, so the click still falls
        // through to useWithoutItem and a ready screen can still be emptied.
        //
        // A WASH TAKES PLANT MATTER AND NOTHING ELSE. Ice-washing kief is a real thing, but it is a
        // third road to a product that already has two, and every extra road here is one more way
        // for one of them to become a trap.
        if (level >= FULL_LEVEL || chance <= 0.0F || content == null
                || (state.getValue(FILLED) && content != Content.PLANT)
                || (level > 0 && content != state.getValue(CONTENT))) {
            explainRefusal(world, player, state, content, level);
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }
        if (world instanceof ServerLevel serverWorld) {
            sift(serverWorld, pos, level == 0 ? state.setValue(CONTENT, content) : state, level, chance);
        }
        // Outside the server branch, as vanilla's composter does it: the cost is the same on both
        // sides whatever the roll, so spending it client-side too is what makes the stack shrink on
        // the frame you click rather than on the frame the server's answer arrives.
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    /**
     * Says on the action bar why the box refused something it would sift. The two rules a player
     * cannot see are the ones worth a line: a wash does nothing without its jacket of ice, and a dry
     * screen never mixes plant matter with kief. Anything the box would never take (a stick, a full
     * screen) is left silent, the way vanilla's composter is. Server side only, or the line would
     * arrive twice.
     */
    private static void explainRefusal(Level world, Player player, BlockState state,
                                       Content content, int level) {
        if (world.isClientSide() || content == null || level >= FULL_LEVEL) {
            return;
        }
        String key = null;
        if (state.getValue(FILLED)) {
            if (content == Content.PLANT) {
                key = "hempdustry.sifting_box.needs_ice";
            }
        } else if (level > 0 && content != state.getValue(CONTENT)) {
            key = content == Content.PLANT
                    ? "hempdustry.sifting_box.no_plant_in_kief"
                    : "hempdustry.sifting_box.no_kief_in_plant";
        }
        if (key != null) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

    /** Taking the batch out. Empty-handed, or holding anything the box does not accept. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(LEVEL) != READY_LEVEL) {
            return InteractionResult.PASS;
        }
        if (world instanceof ServerLevel serverWorld) {
            collect(serverWorld, pos, state);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * A full box settles {@value #SETTLE_DELAY} ticks after it becomes full, however it got there: a
     * sift, a piston push (which drops the tick a sift scheduled) or a command. Vanilla's composter
     * does the same at its own full level. The server calls this on every state change, the same
     * block's included, and a second schedule for a queued tick is ignored.
     */
    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (state.getValue(LEVEL) == FULL_LEVEL) {
            world.scheduleTick(pos, this, SETTLE_DELAY);
        }
    }

    /** {@link #FULL_LEVEL} → {@link #READY_LEVEL}: the powder settles and is ready to take. */
    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(LEVEL) == FULL_LEVEL) {
            world.setBlock(pos, state.setValue(LEVEL, READY_LEVEL), Block.UPDATE_ALL);
            world.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Fill level, 0–8, exactly as a composter reports it. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return state.getValue(LEVEL);
    }

    /**
     * Frost off the surface while the wash is actually cold and actually working.
     *
     * <p>The one thing this block must not do is fail silently, and its failure mode is a torch: the
     * jacket melts, the wash stops, and nothing says so. So the particles are keyed on exactly the
     * condition {@link #useItemOn} checks, and they stop the moment it does. A dry box needs no
     * signal, because a dry box has no condition to fail.
     */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(FILLED) || state.getValue(LEVEL) >= READY_LEVEL || jacketRate(world, pos) <= 0.0F) {
            return;
        }
        world.addParticle(ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                pos.getY() + 0.9D,
                pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                0.0D, 0.02D, 0.0D);
    }

    /**
     * The multiplier this box is running at: <b>1.0 dry, the jacket's tier wet, and 0 for a wet box
     * with no jacket at all</b> — which is what makes a melted jacket stop the wash dead.
     */
    public static float rate(BlockGetter world, BlockPos pos, BlockState state) {
        return state.getValue(FILLED) ? jacketRate(world, pos) : 1.0F;
    }

    /**
     * How cold the jacket is: the rate of the <b>weakest</b> of the four sides, or {@code 0} if any
     * side is not ice at all.
     *
     * <p>The weakest rather than an average, so a jacket is a thing you either built or did not.
     * Four sides rather than six: the box is open at the top and something has to hold it up.
     */
    public static float jacketRate(BlockGetter world, BlockPos pos) {
        float worst = BLUE_ICE_RATE;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            float rate = iceRate(world.getBlockState(pos.relative(side)));
            if (rate <= 0.0F) {
                return 0.0F;
            }
            worst = Math.min(worst, rate);
        }
        return worst;
    }

    private static float iceRate(BlockState state) {
        if (state.is(Blocks.BLUE_ICE)) {
            return BLUE_ICE_RATE;
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return PACKED_ICE_RATE;
        }
        // Frosted ice counts, and is a joke worth leaving in: Frost Walker boots keep a jacket alive
        // for exactly as long as you stand there and no longer.
        return state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE) ? ICE_RATE : 0.0F;
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
     * <p>There is no third rate for resin any more. <b>The box does not re-sift resin</b>: the ice
     * wash replaced that pass, and it is the honest version of it — what a further sieve buys is
     * cleanliness, which is exactly what the wash buys. Kief is another matter: a dry box re-sifts
     * it into filtered kief at {@link #KIEF_CHANCE}.
     */
    private static float sieveChance(ItemStack stack) {
        if (stack.is(ModTags.Items.SIFTABLE_RESINOUS)) {
            return RESINOUS_CHANCE;
        }
        if (stack.is(ModTags.Items.SIFTABLE_FLOWER)) {
            return FLOWER_CHANCE;
        }
        if (stack.is(ModTags.Items.SIFTABLE_TRIM)) {
            return TRIM_CHANCE;
        }
        return stack.is(ModTags.Items.SIFTABLE_KIEF) ? KIEF_CHANCE : 0.0F;
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
        if (stack.is(ModTags.Items.SIFTABLE_FLOWER) || stack.is(ModTags.Items.SIFTABLE_RESINOUS)
                || stack.is(ModTags.Items.SIFTABLE_TRIM)) {
            return Content.PLANT;
        }
        return stack.is(ModTags.Items.SIFTABLE_KIEF) ? Content.KIEF : null;
    }

    /**
     * One item on the screen. A {@code chance} above 1 moves it more than one level: the whole part
     * for certain and the fraction as a roll, so a resinous bud in a packed-ice wash (2 × 0.75) is one
     * level and a coin flip on a second. A level past full is simply lost, as in a composter.
     */
    private static void sift(ServerLevel world, BlockPos pos, BlockState state, int level, float chance) {
        int steps = (int) chance + (world.random.nextFloat() < chance - (int) chance ? 1 : 0);
        if (steps > 0) {
            int next = Math.min(FULL_LEVEL, level + steps);
            // Reaching FULL_LEVEL schedules the settle in onPlace, which this call runs.
            world.setBlock(pos, state.setValue(LEVEL, next), Block.UPDATE_ALL);
            world.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else if (state.getValue(FILLED)) {
            world.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 1.4F);
        } else {
            world.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
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
    private static void collect(ServerLevel world, BlockPos pos, BlockState state) {
        Item yield;
        if (state.getValue(CONTENT) == Content.KIEF) {
            yield = ModItems.FILTERED_KIEF;   // the dry re-sift; a wash never holds powder
        } else {
            yield = state.getValue(FILLED) ? ModItems.BUBBLE_HASH : ModItems.KIEF;
        }
        Vec3 spawn = Vec3.atLowerCornerWithOffset(pos, 0.5, 1.01, 0.5).offsetRandom(world.random, 0.7F);
        ItemEntity dropped = new ItemEntity(world, spawn.x(), spawn.y(), spawn.z(),
                new ItemStack(yield, YIELD));
        dropped.setDefaultPickUpDelay();
        world.addFreshEntity(dropped);
        world.setBlock(pos, state.setValue(LEVEL, 0).setValue(FILLED, false).setValue(CONTENT, Content.PLANT),
                Block.UPDATE_ALL);
        world.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}

package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.advancement.HarvestHempCriterion;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.effect.ModEffects;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiConsumer;

/**
 * Ender Haze, the End's cannabis, and the tallest plant in the mod: <b>five blocks</b> once grown.
 * Built on the same pattern as Lemon Haze's {@link SativaCropBlock} — the canonical age, the trim
 * flags and the light record live on the bottom segment, the <b>root</b>, and every segment above
 * mirrors its age and record — but with a {@link #SEGMENT} counted from the root (0) to the top (4),
 * because no named set of halves describes five blocks.
 *
 * <table>
 *   <tr><th>age</th><th>shape</th></tr>
 *   <tr><td>0–2</td><td>one block</td></tr>
 *   <tr><td>3</td><td>two</td></tr>
 *   <tr><td>4</td><td>three</td></tr>
 *   <tr><td>5</td><td>four</td></tr>
 *   <tr><td>6–7</td><td>five</td></tr>
 * </table>
 *
 * <ul>
 *   <li><b>End stone, or water — never soil.</b> {@code #hempdustry:ender_haze_soil} (end stone, and
 *       {@code #c:end_stones} for a modded End) or a Hydro Tray. Never farmland, and so never a Grow
 *       Pot, which is farmland in a pot.</li>
 *   <li><b>An ordinary crop's light.</b> It needs light 8 to live and 9 to grow, like wheat. The open
 *       End has sky light, so it grows there for nothing; under a roof a torch is enough.</li>
 *   <li><b>Slow, flat, everywhere.</b> 1-in-8 a random tick on any ground, packed or not, watered or
 *       not: there is no farmland under it to be moist, and nothing crowds it. A working Hydro Tray
 *       — water, nutrients, the pump on — is the one thing that doubles it. A dry tray slows it, as
 *       it would any plant.</li>
 *   <li><b>The Grow Lamp pays it, but does not hurry it.</b> The record and the lamp's buds, leaves and
 *       charas odds hold as for every strain; the lamp's growth bonus while the plant is young does not.
 *       Torches grow it, the lamp pays it, the tray hurries it.</li>
 *   <li><b>Bone meal works</b>, one stage at a time, as on every overworld crop.</li>
 *   <li><b>Breaking it wakes the endermen.</b> Every enderman within {@link #PROVOKE_RADIUS} blocks
 *       turns on the player who broke a plant, wild or farmed, unless that player is disguised — a
 *       carved pumpkin, or Averted Gaze, which is what this plant smokes as.</li>
 * </ul>
 *
 * <p>Only the root random-ticks and drops loot, and bone meal on any segment feeds it; breaking any
 * segment takes the whole plant and pays it once, and so does anything else that takes a segment. The
 * wild plant is this block, generated ripe on the outer islands.
 */
public class EnderHazeCropBlock extends CropBlock implements EntityBlock {
    /** How tall the plant stands grown. */
    public static final int HEIGHT = 5;
    /** Which block of the plant this is, counted up from the root at 0. */
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, HEIGHT - 1);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final int MAX_AGE = 7;

    /**
     * The first age at which the plant stands {@code n} blocks tall, at index {@code n}. It gains a
     * block a stage from age 3, so a player watches it climb, and stands five tall at 6 and 7.
     */
    private static final int[] AGE_FOR_HEIGHT = {0, 0, 3, 4, 5, 6};

    /** {@code floor(7 / 1) + 1}: 1-in-8 a random tick, the odds of a packed Lemon Haze field. */
    private static final float GROWTH_RESISTANCE = 7.0F;
    /** What any ground gives it: end stone is no farmland, and nothing crowds it. */
    private static final float FLAT_MOISTURE = 1.0F;

    /** One in this many bonemeal applications does nothing (but is still consumed), as on Lemon Haze. */
    public static final int BONEMEAL_FAILURE_CHANCE = 3;

    /** How far a harvest carries, in blocks: the endermen within it turn on the player. */
    public static final double PROVOKE_RADIUS = 16.0;

    private static final VoxelShape[] ROOT_SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 7.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 11.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D)};
    private static final VoxelShape STEM_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape TOP_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D);

    public EnderHazeCropBlock(Properties settings) {
        super(settings);
        // Defoliation.unworked is load-bearing: a BooleanProperty left out of the default state
        // resolves to *true*, which would plant every seed pre-trimmed.
        this.registerDefaultState(Defoliation.unworked(this.stateDefinition.any()
                .setValue(this.getAgeProperty(), 0)
                .setValue(SEGMENT, 0)));
    }

    /** How many blocks tall a plant of {@code age} stands, with the room to. */
    public static int heightFor(int age) {
        for (int height = HEIGHT; height > 1; height--) {
            if (age >= AGE_FOR_HEIGHT[height]) {
                return height;
            }
        }
        return 1;
    }

    /** Whether this block is the plant's top at its age — what the models and the shape draw as the crown. */
    public static boolean isTop(BlockState state) {
        return state.getValue(SEGMENT) == heightFor(state.getValue(AGE)) - 1;
    }

    static boolean isRoot(BlockState state) {
        return state.getValue(SEGMENT) == 0;
    }

    private BlockState stateFor(int age, int segment) {
        return this.defaultBlockState().setValue(this.getAgeProperty(), age).setValue(SEGMENT, segment);
    }

    /** A segment above the root, carrying a copy of the plant's light record: whatever draws a block reads only that block. */
    private BlockState stateFor(int age, int segment, GrowLight light) {
        return this.stateFor(age, segment).setValue(GrowLight.PROPERTY, light);
    }

    /** End stone, or a Hydro Tray. Never farmland, and so never a Grow Pot. */
    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(ModTags.Blocks.ENDER_HAZE_SOIL) || floor.getBlock() instanceof HydroTrayBlock;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.ENDER_HAZE_SEEDS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (isRoot(state) && heightFor(state.getValue(AGE)) == 1) {
            return ROOT_SHAPE_BY_AGE[Math.min(state.getValue(AGE), ROOT_SHAPE_BY_AGE.length - 1)];
        }
        if (isRoot(state)) {
            return STEM_SHAPE;
        }
        return isTop(state) ? TOP_SHAPE : STEM_SHAPE;
    }

    // ----- support / placement -----
    // Each segment above the root survives only on the one below it, so removing any segment leaves
    // the ones above it unsupported, and VegetationBlock's updateShape turns them to air one after
    // another. The root uses the ordinary crop rule: light, and the right ground below. A segment also
    // asks the root's age, as the other tall plants' do: another mod that harvests and replants the root
    // inside a block update leaves the old segments standing, because the update that would break them
    // is queued until the outer one ends, and by then a seedling stands under them. Only its age tells
    // that they are not its own.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        int segment = state.getValue(SEGMENT);
        if (segment == 0) {
            return super.canSurvive(state, world, pos);
        }
        BlockState below = world.getBlockState(pos.below());
        if (!below.is(this) || below.getValue(SEGMENT) != segment - 1) {
            return false;
        }
        BlockState root = world.getBlockState(pos.below(segment));
        return root.is(this) && isRoot(root) && heightFor(root.getValue(AGE)) > segment;
    }

    /**
     * A block that carries a segment goes when that segment is taken by anything but the plant's own rules
     * ({@link TallHemp#lostItsTop}), as on the other tall plants: each segment gone that way takes the one
     * under it in turn, so a top cut from anywhere on the plant brings the whole plant down, and its harvest
     * falls once, from the root.
     */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        BlockState shaped = super.updateShape(state, world, ticks, pos, direction, neighborPos, neighbor, random);
        if (direction == Direction.UP && shaped == state && TallHemp.lostItsTop(this, world, pos,
                state.getValue(SEGMENT) < heightFor(state.getValue(AGE)) - 1)) {
            return Blocks.AIR.defaultBlockState();
        }
        return shaped;
    }

    /** An explosion that reaches a segment takes the root with it, as that explosion, as on the other tall plants. */
    @Override
    protected void onExplosionHit(BlockState state, ServerLevel world, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> drops) {
        if (!isRoot(state) && this.canSurvive(state, world, pos)) {
            BlockPos rootPos = rootOf(pos, state);
            world.getBlockState(rootPos).onExplosionHit(world, rootPos, explosion, drops);
        }
        super.onExplosionHit(state, world, pos, explosion, drops);
    }

    // ----- growth -----

    /**
     * The root ticks even once ripe, as on the other tall plants: a plant that ripened under a roof is
     * a block or more short, and this tick finishes it once the space is cleared. The cost is a few
     * block reads and no writes — see {@link #isShapeSettled}.
     */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return isRoot(state);
    }

    /**
     * Every segment answers for the plant, from its root, as on the other tall plants: other mods' harvesters
     * read "refuses bone meal" as "ripe" on any block they reach. Refused, and so not consumed, once the plant
     * has grown as far as its headroom allows.
     */
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state, BonemealSource source) {
        BlockPos rootPos = TallHemp.rootOf(this, world, pos, state);
        return rootPos != null && this.getAge(world.getBlockState(rootPos)) < this.maxAgeFor(world, rootPos);
    }

    /**
     * The highest age the plant may grow to at {@code pos} given what is above it: one stage short
     * of the height it cannot reach. Holding back the <em>age</em>, not only the shape, keeps the
     * harvest honest — the loot table and the models read the age.
     */
    private int maxAgeFor(LevelReader world, BlockPos pos) {
        for (int above = 1; above < HEIGHT; above++) {
            if (!this.canOccupy(world, pos.above(above))) {
                return AGE_FOR_HEIGHT[above + 1] - 1;
            }
        }
        return this.getMaxAge();
    }

    // No getAge override: every segment reports its own age, which setAge keeps level with the root's.
    // Other mods' harvesters take any CropBlock whose isMaxAge holds, so a segment that claimed to be
    // ripe was cut off a growing plant. A bee, which would write a root's state into the middle of the
    // plant, is kept to the root by BeeGrowCropsGoalMixin, as on the other tall plants.

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!isRoot(state)) {
            return;
        }
        int age = this.getAge(state);
        int grown = age;
        if (age < this.getMaxAge() && world.getRawBrightness(pos, 0) >= 9) {
            BlockState floor = world.getBlockState(pos.below());
            float moisture = FLAT_MOISTURE;
            float bed = 1.0F;
            if (floor.getBlock() instanceof GrowMedium medium) {
                // A tray's water is no wetter than open ground to this plant, but a dry one starves
                // it like any other, and a working one is the only thing that hurries it.
                moisture = FLAT_MOISTURE * medium.moisture(floor) / HydroTrayBlock.WET_MOISTURE;
                bed = medium.speed(floor);
            }
            // No GrowLight#speedAt here: the Grow Lamp's head start for a young plant is every other
            // strain's, and not this one's.
            boolean rolled = HempGrowth.rolls(random, GROWTH_RESISTANCE, moisture, bed);
            if (Services.PLATFORM.canCropGrow(world, pos, state, rolled)) {
                grown++;
            }
        }
        if (grown != age || !this.isShapeSettled(world, pos, age, state.getValue(GrowLight.PROPERTY))) {
            this.setAge(world, pos, world.getBlockState(pos), grown, StressCause.NONE, random);
        }
        // Told from the age written, not the roll, as on the other tall plants: a plant held back by its
        // headroom rolls while writeAge keeps it where it is.
        BlockState after = world.getBlockState(pos);
        if (after.is(this) && isRoot(after) && this.getAge(after) != age) {
            Services.PLATFORM.cropGrew(world, pos, state);
        }
    }

    /**
     * Whether the plant at {@code pos} already carries every segment its age calls for, as far as
     * there is room, so a reconciling {@link #setAge} at the same age would write nothing above the
     * root. Built on the same {@link #canOccupy} and {@link #stateFor} that setAge uses, so the two
     * cannot drift apart.
     */
    private boolean isShapeSettled(LevelReader world, BlockPos pos, int age, GrowLight light) {
        int height = heightFor(age);
        for (int segment = 1; segment < height; segment++) {
            BlockState found = world.getBlockState(pos.above(segment));
            if (!this.canOccupy(found)) {
                return true; // boxed in here, so setAge stops here too
            }
            if (found != this.stateFor(age, segment, light)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void growCrops(Level world, BlockPos pos, BlockState state) {
        this.boneMeal(world, pos, state, world.getRandom());
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        this.boneMeal(world, pos, state, random);
    }

    /** One bone meal step, from the root — and, on a flowering plant in a fed tray, a chance of overfeeding it. */
    private void boneMeal(Level world, BlockPos pos, BlockState state, RandomSource random) {
        BlockPos rootPos = TallHemp.rootOf(this, world, pos, state);
        if (rootPos == null) {
            return;
        }
        BlockState root = world.getBlockState(rootPos);
        this.setAge(world, rootPos, root, this.getAge(root) + this.getBonemealAgeIncrease(world),
                PlantStress.ofBoneMeal(world, rootPos, root), random);
    }

    /** A flat stage per successful bonemeal, as on the other hemp crops. */
    @Override
    protected int getBonemealAgeIncrease(Level world) {
        return 1;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return random.nextInt(BONEMEAL_FAILURE_CHANCE) != 0;
    }

    /**
     * Sets the root to {@code newAge} and writes the segments above it, bottom up, as far as the age
     * calls for and there is room. {@code newAge} is held to {@link #maxAgeFor}, but never below an
     * age the plant already has: a ripe plant a player later builds over keeps its harvest.
     *
     * <p>{@code current} is the plant the step starts from: the block at {@code pos}, except when
     * {@link #onPlace} replays a raw write, where it is the state that write replaced.
     */
    private void setAge(Level world, BlockPos pos, BlockState current, int newAge, StressCause extraStress,
                        RandomSource random) {
        TallHemp.ownWrite(() -> {
            this.writeAge(world, pos, current, newAge, extraStress, random);
            return true;
        });
    }

    private void writeAge(Level world, BlockPos pos, BlockState current, int newAge, StressCause extraStress,
                          RandomSource random) {
        int currentAge = current.is(this) && isRoot(current) ? this.getAge(current) : 0;
        newAge = Math.min(newAge, this.getMaxAge());
        newAge = Math.min(newAge, Math.max(currentAge, this.maxAgeFor(world, pos)));

        BlockState root = Defoliation.carryOver(current, this.stateFor(newAge, 0));
        if (newAge > currentAge && current.is(this)) {
            root = root.setValue(GrowLight.PROPERTY, PlantStress.afterStep(world, pos,
                    current.getValue(GrowLight.PROPERTY), currentAge, newAge, HEIGHT, extraStress, random, true));
            if (newAge == this.getMaxAge()) {
                BlockPos floorPos = pos.below();
                BlockState floor = world.getBlockState(floorPos);
                if (floor.getBlock() instanceof GrowMedium medium) {
                    medium.spend(world, floorPos, floor);
                }
            }
        }
        world.setBlock(pos, root, Block.UPDATE_CLIENTS);
        if (world.getBlockState(pos) != root) {
            // Something answered that write by taking the plant (a soil that harvests what ripens on it):
            // the segments are no longer this plant's to grow, and the old ones would not fall by themselves
            // before a new root stood under them again, as on the other tall plants.
            this.removeSegmentsAbove(world, pos);
            return;
        }

        // Bottom up: each segment needs the one under it in place, or its canSurvive fails and the
        // neighbour update takes it straight back out.
        GrowLight light = root.getValue(GrowLight.PROPERTY);
        int height = heightFor(newAge);
        for (int segment = 1; segment < height; segment++) {
            BlockPos at = pos.above(segment);
            if (!this.canOccupy(world, at)) {
                break;
            }
            world.setBlock(at, this.stateFor(newAge, segment, light), Block.UPDATE_CLIENTS);
        }
    }

    /** Whether the plant may grow into {@code pos} — empty space, or a segment it already owns. */
    private boolean canOccupy(LevelReader world, BlockPos pos) {
        return this.canOccupy(world.getBlockState(pos));
    }

    private boolean canOccupy(BlockState state) {
        return state.isAir() || (state.is(this) && !isRoot(state));
    }

    /** Where the root of the plant {@code pos} belongs to sits: the segment counts down to it. */
    public static BlockPos rootOf(BlockPos pos, BlockState state) {
        return pos.below(state.getValue(SEGMENT));
    }

    // ----- defoliation -----

    /** Shears trim and rub it from any segment; the flags live on the root. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos rootPos = rootOf(pos, state);
        BlockState root = world.getBlockState(rootPos);
        if (root.is(this) && isRoot(root)) {
            InteractionResult result = Defoliation.tryCut(world, rootPos, root, root.getValue(AGE),
                    stack, player, hand);
            if (result != null) {
                return result;
            }
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    // ----- the smell of a ripe plant -----

    /** Every segment's {@link #AGE} mirrors the root's, so any of them answers truthfully here. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= this.getMaxAge()) {
            RipeAroma.randomDisplayTick(world, pos, random);
        }
    }

    // ----- harvesting -----
    // Breaking any segment takes the whole plant and pays it once: only the root has loot, so the
    // root's is dropped by hand when the player broke another segment, and the rest is cleared from
    // the top down with drops suppressed, as the plant's own write, so that a root losing its segments here
    // is not taken for a plant whose top something else took (updateShape).
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
            TallHemp.ownWrite(() -> {
                this.takeTheRest(world, pos, state, player);
                return true;
            });
            provoke((ServerLevel) world, pos, player);
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    private void takeTheRest(Level world, BlockPos pos, BlockState state, Player player) {
        BlockPos rootPos = rootOf(pos, state);
        BlockState root = world.getBlockState(rootPos);
        if (root.is(this) && isRoot(root)) {
            if (this.isMaxAge(root)) {
                HarvestHempCriterion.trigger(player, (ServerLevel) world, rootPos, root);
            }
            for (int segment = HEIGHT - 1; segment >= 1; segment--) {
                BlockPos at = rootPos.above(segment);
                BlockState found = world.getBlockState(at);
                if (!at.equals(pos) && found.is(this) && !isRoot(found)) {
                    world.setBlock(at, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
            if (!rootPos.equals(pos)) {
                if (!player.isCreative()) {
                    // With the root's record: the buds take the plant's band from it.
                    dropResources(root, world, rootPos, world.getBlockEntity(rootPos), player,
                            player.getMainHandItem());
                }
                world.setBlock(rootPos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }

    /**
     * Turns every enderman within {@link #PROVOKE_RADIUS} of {@code pos} on {@code player}, exactly as
     * if the player had stared at each of them: the scream, the chase, the anger that outlasts the
     * sight of you. A player an enderman would not see — a carved pumpkin, or {@link ModEffects#AVERTED_GAZE}
     * — wakes nobody, and neither does one in creative or spectator mode, on Peaceful, or with the
     * creepers' off switch thrown: a server that turned the mobs' interest in hemp off did not ask
     * for angry endermen instead. One already angry at this player is left as it is.
     */
    public static void provoke(ServerLevel world, BlockPos pos, Player player) {
        if (!HempdustryConfig.get().world().creepersSeekHemp()
                || world.getDifficulty() == Difficulty.PEACEFUL
                || player.isCreative() || player.isSpectator()
                || player.hasEffect(ModEffects.AVERTED_GAZE)) {
            return;
        }
        for (Enderman enderman : world.getEntitiesOfClass(Enderman.class, new AABB(pos).inflate(PROVOKE_RADIUS),
                enderman -> enderman.isAlive() && enderman.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= PROVOKE_RADIUS * PROVOKE_RADIUS)) {
            if (enderman.isAngryAt(player, world) || Services.PLATFORM.disguisedFrom(player, enderman)) {
                continue;
            }
            enderman.setPersistentAngerTarget(EntityReference.of(player));
            enderman.startPersistentAngerTimer();
            enderman.setTarget(player);
            enderman.setBeingStaredAt();
        }
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    // ----- the plant's record -----

    /**
     * The plant's record ({@link PlantRecordBlockEntity}) sits on its root only: every other block of
     * the plant answers none.
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isRoot(state) ? new PlantRecordBlockEntity(pos, state) : null;
    }

    /**
     * Puts right a raw write from another mod, as {@link IndicaCropBlock#onPlace} does, for a plant of five
     * blocks: a root state written onto a segment goes back to that segment, and so does an age alone written
     * there ({@link TallHemp#isAgeWrittenAboveTheRoot}), then grows the plant from its root when the write
     * raised the age ({@link TallHemp#isRaisedAboveTheRoot}); an age alone set back on the root keeps only the
     * flags it could have earned ({@link TallHemp#isSetBackByAge}), a replanting takes every segment away (top
     * first, so none is left standing on nothing), and growth is one step of the plant's own. A root set back
     * to age 0 in place, by any of these, is a new plant, and its record rolls again.
     */
    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, world, pos, old, moved);
        if (world.isClientSide()) {
            return;
        }
        if (old.is(this) && isRoot(old) && isRoot(state)
                && old.getValue(AGE) > 0 && state.getValue(AGE) == 0) {
            PlantRecordBlockEntity.replantAt(world, pos);
        }
        boolean raw = TallHemp.isRawWrite(this, state, old);
        if (raw && !isRoot(old) || TallHemp.isAgeWrittenAboveTheRoot(this, state, old)) {
            boolean raised = TallHemp.isRaisedAboveTheRoot(this, state, old);
            TallHemp.ownWrite(() -> world.setBlock(pos, old, Block.UPDATE_CLIENTS));
            BlockPos rootPos = raised ? TallHemp.rootOf(this, world, pos, old) : null;
            if (rootPos != null) {
                BlockState root = world.getBlockState(rootPos);
                this.setAge(world, rootPos, root, this.getAge(state), StressCause.NONE, world.getRandom());
            }
            return;
        }
        if (!raw) {
            if (TallHemp.isSetBackByAge(this, state, old)) {
                BlockState earned = Defoliation.onlyEarned(state, this.getAge(state));
                if (earned != state) {
                    TallHemp.ownWrite(() -> world.setBlock(pos, earned, Block.UPDATE_CLIENTS));
                }
            }
            return;
        }
        int age = this.getAge(state);
        if (age == this.getAge(old)) {
            return; // a plant set afresh where it stood, as /setblock does: nothing grew, nothing to keep
        }
        boolean replanted = age < this.getAge(old);
        if (replanted) {
            this.removeSegmentsAbove(world, pos);
        }
        this.setAge(world, pos, replanted ? state : old, age, StressCause.NONE, world.getRandom());
    }

    /**
     * Takes away the segments standing on {@code pos}, top down and without drops: their plant's harvest is the
     * root's, and whoever replaced the root has had it.
     */
    private void removeSegmentsAbove(Level world, BlockPos pos) {
        for (int segment = HEIGHT - 1; segment >= 1; segment--) {
            BlockPos at = pos.above(segment);
            BlockState found = world.getBlockState(at);
            if (found.is(this) && !isRoot(found)) {
                TallHemp.ownWrite(() -> world.setBlock(at, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS));
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SEGMENT, Defoliation.TRIMMED_EARLY, Defoliation.TRIMMED_LATE, Defoliation.RUBBED,
                GrowLight.PROPERTY);
    }
}

package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.advancement.HarvestHempCriterion;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.effect.ModEffects;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

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
 * <p>Only the root random-ticks, is fertilizable and drops loot; breaking any segment takes the
 * whole plant and pays it once. The wild plant is this block, generated ripe on the outer islands.
 */
public class EnderHazeCropBlock extends CropBlock {
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

    private static boolean isRoot(BlockState state) {
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
    // another. The root uses the ordinary crop rule: light, and the right ground below.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        int segment = state.getValue(SEGMENT);
        if (segment == 0) {
            return super.canSurvive(state, world, pos);
        }
        BlockState below = world.getBlockState(pos.below());
        return below.is(this) && below.getValue(SEGMENT) == segment - 1;
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

    /** Refused, and so not consumed, once the plant has grown as far as its headroom allows. */
    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return isRoot(state) && this.getAge(state) < this.maxAgeFor(world, pos);
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

    /**
     * The plant's age as seen from this block: the root's own, and {@link #getMaxAge()} — "nothing
     * left to grow here" — from every segment above it. That is what keeps a bee from writing a
     * root's state into the middle of the plant (it skips anything {@code isMaxAge}), and nothing
     * reads a segment's age through this: the models and the loot table read {@link #AGE} directly.
     */
    @Override
    public int getAge(BlockState state) {
        return isRoot(state) ? super.getAge(state) : this.getMaxAge();
    }

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
            if (HempGrowth.rolls(random, GROWTH_RESISTANCE, moisture, bed)) {
                grown++;
            }
        }
        if (grown != age || !this.isShapeSettled(world, pos, age, state.getValue(GrowLight.PROPERTY))) {
            this.setAge(world, pos, grown, 0.0F, random);
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
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        this.boneMeal(world, pos, state, random);
    }

    /** One bone meal step — and, on a flowering plant in a fed tray, a chance of overfeeding it. */
    private void boneMeal(Level world, BlockPos pos, BlockState state, RandomSource random) {
        if (!isRoot(state)) {
            return;
        }
        this.setAge(world, pos, this.getAge(state) + this.getBonemealAgeIncrease(world),
                PlantStress.ofBoneMeal(world, pos, state), random);
    }

    /** A flat stage per successful bonemeal, as on the other hemp crops. */
    @Override
    protected int getBonemealAgeIncrease(Level world) {
        return 1;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextInt(BONEMEAL_FAILURE_CHANCE) != 0;
    }

    /**
     * Sets the root to {@code newAge} and writes the segments above it, bottom up, as far as the age
     * calls for and there is room. {@code newAge} is held to {@link #maxAgeFor}, but never below an
     * age the plant already has: a ripe plant a player later builds over keeps its harvest.
     */
    private void setAge(Level world, BlockPos pos, int newAge, float extraStress, RandomSource random) {
        BlockState current = world.getBlockState(pos);
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

    /** Reads {@link #AGE} directly: every segment above the root reports maturity through {@link #getAge}. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= this.getMaxAge()) {
            RipeAroma.randomDisplayTick(world, pos, random);
        }
    }

    // ----- harvesting -----
    // Breaking any segment takes the whole plant and pays it once: only the root has loot, so the
    // root's is dropped by hand when the player broke another segment, and the rest is cleared from
    // the top down with drops suppressed.
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()) {
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
                        dropResources(root, world, rootPos, null, player, player.getMainHandItem());
                    }
                    world.setBlock(rootPos, Blocks.AIR.defaultBlockState(),
                            Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
            provoke((ServerLevel) world, pos, player);
        }
        return super.playerWillDestroy(world, pos, state, player);
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
        for (EnderMan enderman : world.getEntitiesOfClass(EnderMan.class, new AABB(pos).inflate(PROVOKE_RADIUS),
                enderman -> enderman.isAlive() && enderman.distanceToSqr(pos.getCenter()) <= PROVOKE_RADIUS * PROVOKE_RADIUS)) {
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SEGMENT, Defoliation.TRIMMED_EARLY, Defoliation.TRIMMED_LATE, Defoliation.RUBBED,
                GrowLight.PROPERTY);
    }
}

package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Powdery mildew: the price of the stale air under a roof. A <b>record the plant keeps</b>, like
 * {@link GrowLight}: canonical on the LOWER and copied up every segment, so the blockstate can draw
 * the powder on each of them.
 *
 * <ul>
 *   <li><b>It starts</b> only on a plant under a roof — anything above it that stops movement, glass
 *       included, leaves not — at {@link #ONSET} per growth step. A field never catches it.</li>
 *   <li><b>It spreads</b> one plant at a time: each random tick, a mildewed plant tries one of its
 *       eight neighbours at {@link #SPREAD}. A one-block gap stops it, which is what spacing does
 *       in a real grow room. It never kills a plant.</li>
 *   <li><b>Shears clear it</b> in a trim window: the cut is that window's trim, and the diseased
 *       leaf is thrown away ({@link Defoliation#tryCut}).</li>
 *   <li><b>Vanilla's sulfur gas clears and prevents it</b> — the noxious gas over a potent sulfur
 *       block under water ({@link #gassed}). A flowering plant the gas reaches is left
 *       {@link #SULFURED}: sulfur is no thing to have on a bud you will smoke.</li>
 * </ul>
 *
 * <p>Mildew is <b>not stress</b>: it costs buds to schwag at harvest and nothing else — no bud short
 * and no seeds, since mildew does not make a plant hermaphrodite. The constants are in order of harm,
 * and a plant that is both stressed and mildewed takes the worse of the two, never the sum.
 *
 * <p>A ripe plant is past it, as it is past stress: nothing here costs a finished plant its harvest.
 * {@code #hempdustry:mildew_resistant} names the crops that never catch it, and
 * {@code world.mildew} in the config turns the whole thing off.
 */
public enum Mildew implements StringRepresentable {
    CLEAN("clean"),
    /** Reached by the sulfur gas while flowering. Each bud is a little likelier to be schwag. */
    SULFURED("sulfured"),
    /** White powder on the leaves and buds. Each bud is much likelier to be schwag. */
    MILDEWED("mildewed");

    public static final EnumProperty<Mildew> PROPERTY = EnumProperty.create("mildew", Mildew.class);

    /** The odds a growth step under a roof brings mildew. About 30% of nine-plant grows see a case, and 86% of 49-plant ones. */
    public static final float ONSET = 1.0F / 150;
    /** The odds a mildewed plant's random tick infects the neighbour it picked. */
    public static final float SPREAD = 1.0F / 2;

    /** How far vanilla's noxious gas reaches from its source, which is where it reaches a plant from too. */
    private static final double GAS_REACH = PotentSulfurBlockEntity.EFFECT_RANGE;
    /** How deep a potent sulfur block may sit below the surface its gas rises from, as vanilla walks it. */
    private static final int GAS_DEPTH = 5;

    private final String name;

    Mildew(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /**
     * What a plant's random tick does about mildew, after any growth: nothing outdoors; under a roof,
     * the gas clears or sulfurs it, and a mildewed plant out of the gas reaches for a neighbour.
     * With the config switch off, a plant that still has it is cleaned instead.
     */
    public static void randomTick(Level world, BlockPos lower, RandomSource random) {
        BlockState plant = world.getBlockState(lower);
        if (!plant.hasProperty(PROPERTY)) {
            return;
        }
        Mildew now = plant.getValue(PROPERTY);
        if (!HempdustryConfig.get().world().mildew()) {
            if (now != CLEAN) {
                write(world, lower, CLEAN);
            }
            return;
        }
        if (!roofed(world, lower)) {
            return;
        }
        if (gassed(world, lower)) {
            Mildew next = now;
            if (now == MILDEWED) {
                next = flowering(plant) ? SULFURED : CLEAN;
            } else if (now == CLEAN && flowering(plant) && !ripe(plant)) {
                next = SULFURED;
            }
            if (next != now) {
                write(world, lower, next);
            }
            return;
        }
        if (now == MILDEWED) {
            spread(world, lower, random);
        }
    }

    /**
     * The record after a growth step of the plant at {@code lower}: {@code current} kept, or mildew
     * caught at {@link #ONSET}. Draws from {@code random} only under a roof, so a field plant's step
     * takes exactly the draws it always did.
     */
    public static Mildew afterStep(Level world, BlockPos lower, BlockState plant, Mildew current,
                                   RandomSource random) {
        if (current == MILDEWED || !canCatch(world, lower, plant)) {
            return current;
        }
        return random.nextFloat() < ONSET && !gassed(world, lower) ? MILDEWED : current;
    }

    /** A mildewed plant's reach: one of its eight neighbours, at {@link #SPREAD}. */
    private static void spread(Level world, BlockPos lower, RandomSource random) {
        int pick = random.nextInt(8);
        BlockPos target = lower.offset(NEIGHBOURS[pick][0], 0, NEIGHBOURS[pick][1]);
        BlockState plant = world.getBlockState(target);
        if (!plant.hasProperty(PROPERTY) || plant.getValue(PROPERTY) == MILDEWED
                || world.getBlockState(target.below()).is(plant.getBlock())
                || random.nextFloat() >= SPREAD
                || !canCatch(world, target, plant) || gassed(world, target)) {
            return;
        }
        write(world, target, MILDEWED);
    }

    private static final int[][] NEIGHBOURS = {
            {-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};

    /** Mildew may take hold of this plant at all: switched on, a crop that does not resist it, unripe, under a roof. */
    private static boolean canCatch(Level world, BlockPos lower, BlockState plant) {
        return HempdustryConfig.get().world().mildew() && plant.hasProperty(PROPERTY)
                && !plant.is(ModTags.Blocks.MILDEW_RESISTANT) && !ripe(plant) && roofed(world, lower);
    }

    /**
     * Something above the plant that stops movement: a roof of any block, glass included, so a
     * greenhouse gets mildew too. Leaves are not a roof — a field under a tree is still a field.
     * The plant itself stops nothing, so the heightmap stands at its floor when the sky is open.
     */
    public static boolean roofed(Level world, BlockPos lower) {
        return world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lower.getX(), lower.getZ()) > lower.getY();
    }

    /**
     * Whether vanilla's sulfur gas reaches the plant at {@code lower}: a potent sulfur block that is
     * giving off gas (under water: {@code WET}, or {@code DORMANT} between eruptions), whose gas
     * source lies within {@link #GAS_REACH} of any of the plant's segments with nothing solid between.
     *
     * <p>Vanilla's own test ({@code PotentSulfurBlockEntity#canBeReachedByNoxiousGas}) asks for water
     * under the point it reaches — it is built for a swimmer in the pool, and would never reach a plant
     * on its bed. So this keeps vanilla's source, reach and line of sight, and drops the pool. The
     * block entities are what is searched, not the blocks: a potent sulfur block carries one, and a
     * chunk's handful of them is far cheaper to walk than the box of blocks around a plant.
     */
    public static boolean gassed(Level world, BlockPos lower) {
        int reach = (int) Math.ceil(GAS_REACH);
        int minX = SectionPos.blockToSectionCoord(lower.getX() - reach);
        int maxX = SectionPos.blockToSectionCoord(lower.getX() + reach);
        int minZ = SectionPos.blockToSectionCoord(lower.getZ() - reach);
        int maxZ = SectionPos.blockToSectionCoord(lower.getZ() + reach);
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                if (!world.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = world.getChunk(cx, cz);
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof PotentSulfurBlockEntity
                            && Math.abs(entity.getBlockPos().getX() - lower.getX()) <= reach
                            && Math.abs(entity.getBlockPos().getZ() - lower.getZ()) <= reach
                            && reaches(world, entity.getBlockPos(), lower)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean reaches(Level world, BlockPos sulfur, BlockPos lower) {
        BlockState state = world.getBlockState(sulfur);
        if (!state.hasProperty(PotentSulfurBlock.STATE)) {
            return false;
        }
        PotentSulfurState gas = state.getValue(PotentSulfurBlock.STATE);
        if (gas != PotentSulfurState.WET && gas != PotentSulfurState.DORMANT) {
            return false;
        }
        BlockPos source = gasSource(world, sulfur);
        if (source == null) {
            return false;
        }
        Vec3 from = Vec3.atCenterOf(source);
        Block block = world.getBlockState(lower).getBlock();
        for (BlockPos pos = lower; world.getBlockState(pos).is(block); pos = pos.above()) {
            Vec3 to = Vec3.atCenterOf(pos);
            if (from.distanceToSqr(to) <= GAS_REACH * GAS_REACH
                    && world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    CollisionContext.empty())).getType() != HitResult.Type.BLOCK) {
                return true;
            }
        }
        return false;
    }

    /**
     * Where the gas comes out: the first open block above the water over {@code sulfur}, at most
     * {@link #GAS_DEPTH} up. The same walk as vanilla's private {@code findNoxiousGasSourceBlock}.
     */
    @Nullable
    private static BlockPos gasSource(Level world, BlockPos sulfur) {
        CollisionContext context = CollisionContext.positionContext(sulfur.getY());
        BlockPos.MutableBlockPos pos = sulfur.above().mutable();
        for (; pos.getY() <= sulfur.getY() + GAS_DEPTH; pos.move(Direction.UP)) {
            BlockState state = world.getBlockState(pos);
            if (world.getFluidState(pos).isSourceOfType(Fluids.WATER)
                    && (state.is(Blocks.WATER) || passable(state, world, pos, context))) {
                continue;
            }
            return state.isAir() || passable(state, world, pos, context) ? pos.immutable() : null;
        }
        return null;
    }

    private static boolean passable(BlockState state, Level world, BlockPos pos, CollisionContext context) {
        return state.isAir() || state.is(Blocks.WATER) || state.getCollisionShape(world, pos, context).isEmpty();
    }

    /** Writes {@code value} on the plant at {@code lower} and every segment above it at once, so it shows straight away. */
    public static void write(Level world, BlockPos lower, Mildew value) {
        Block block = world.getBlockState(lower).getBlock();
        for (BlockPos pos = lower; world.getBlockState(pos).is(block); pos = pos.above()) {
            world.setBlock(pos, world.getBlockState(pos).setValue(PROPERTY, value), Block.UPDATE_CLIENTS);
        }
    }

    /** Now and then a drift of white ash off a mildewed segment: the spores, so the powder is seen moving. */
    public static void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (state.hasProperty(PROPERTY) && state.getValue(PROPERTY) == MILDEWED && random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + random.nextDouble(),
                    pos.getY() + 0.3 + random.nextDouble() * 0.6, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }

    private static boolean flowering(BlockState plant) {
        return plant.hasProperty(BlockStateProperties.AGE_7)
                && plant.getValue(BlockStateProperties.AGE_7) >= GrowLight.FLOWERING_AGE;
    }

    private static boolean ripe(BlockState plant) {
        return plant.getBlock() instanceof CropBlock crop && plant.hasProperty(BlockStateProperties.AGE_7)
                && plant.getValue(BlockStateProperties.AGE_7) >= crop.getMaxAge();
    }
}

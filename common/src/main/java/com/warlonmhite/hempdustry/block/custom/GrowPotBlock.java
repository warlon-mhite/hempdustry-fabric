package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A planter for hemp, any strain. It stands in for farmland — never trampled, and a plant in it never
 * competes with its neighbours for roots: vanilla halves a crop's moisture when the same crop stands
 * beside it, and a pot's plant has soil of its own.
 *
 * <p><b>Soil holds food well and water badly</b>, which is how a pot differs from the Hydro Tray:
 *
 * <ul>
 *   <li><b>Water</b> ({@link #WATERED}) is one bottle, by hand or from a dispenser, and a plant that
 *       ripens drinks it. So a pot is watered every plant, the chore a hydroponic reservoir is built to
 *       save. A dry pot grows like dry farmland whatever it has been fed, and stresses a flowering
 *       plant ({@link PlantStress#DRY_BED}). A bottle into a pot that is still wet is overwatering —
 *       the roots sit in water and rot ({@link PlantStress#OVERWATERED}). A bucket is refused: it is
 *       the tray's tool, and letting it through would put a water block in the plant's space.</li>
 *   <li><b>Food</b> ({@link #FERTILITY}, 0–{@value #MAX_FERTILITY}) is bone meal, one a plant, and a
 *       watered pot with food in it grows its plant {@value #FERTILE_SPEED}× faster than the best
 *       field. Hemp is a nitrogen-hungry plant of manured ground, so the pot runs on the composter; a
 *       full pot refuses bone meal rather than eating it, the way a full composter refuses.</li>
 * </ul>
 *
 * <p>The soil shows both the way a player already reads them: vanilla's own dry and moist farmland
 * for the water, and compost crumbs on top, thinning as the plants eat it, for the food. Rain never
 * waters a pot — nothing indoors is rained on, and the tray ignores it too.
 *
 * <p>The fertility is kept when the pot is picked up (the loot table copies it, as a beehive keeps its
 * honey); the water is not, and a freshly crafted pot starts dry and empty.
 */
public class GrowPotBlock extends Block implements GrowMedium {
    public static final MapCodec<GrowPotBlock> CODEC = simpleCodec(GrowPotBlock::new);

    public static final int MAX_FERTILITY = 3;
    public static final IntegerProperty FERTILITY = IntegerProperty.create("fertility", 0, MAX_FERTILITY);
    public static final BooleanProperty WATERED = BooleanProperty.create("watered");

    /** Watered: vanilla's best, a lone plant on moist farmland ringed by moist farmland. */
    public static final float WET_MOISTURE = 10.0F;
    /** Dry: dry farmland ringed by dry farmland. */
    public static final float DRY_MOISTURE = 4.0F;
    public static final float FERTILE_SPEED = 1.5F;

    /** A body inset a pixel under a full-width lip, as the model draws it. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 13.0D, 15.0D),
            Block.box(0.0D, 13.0D, 0.0D, 16.0D, 16.0D, 16.0D));

    public GrowPotBlock(Properties settings) {
        super(settings);
        // WATERED spelled out: a BooleanProperty left out of the default state defaults to true.
        this.registerDefaultState(this.stateDefinition.any().setValue(FERTILITY, 0).setValue(WATERED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static boolean isPot(BlockState state) {
        return state.getBlock() instanceof GrowPotBlock;
    }

    public static boolean isFertile(BlockState state) {
        return isPot(state) && state.getValue(FERTILITY) > 0;
    }

    @Override
    public float moisture(BlockState state) {
        return state.getValue(WATERED) ? WET_MOISTURE : DRY_MOISTURE;
    }

    /** Food only works in water: a dry pot gives nothing, however well it has been fed. */
    @Override
    public float speed(BlockState state) {
        return state.getValue(WATERED) && isFertile(state) ? FERTILE_SPEED : 1.0F;
    }

    /** The plant that ripens eats one charge of food and drinks the water. */
    @Override
    public void spend(Level world, BlockPos pos, BlockState state) {
        BlockState spent = state.setValue(FERTILITY, Math.max(0, state.getValue(FERTILITY) - 1)).setValue(WATERED, false);
        if (spent != state) {
            world.setBlock(pos, spent, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean fed(BlockState state) {
        return isFertile(state);
    }

    @Override
    public float stress(BlockState state) {
        return state.getValue(WATERED) ? 0.0F : PlantStress.DRY_BED;
    }

    // ----- water -----

    public static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS,
                PotionContents.EMPTY).is(Potions.WATER);
    }

    /**
     * Pours one bottle into the pot at {@code pos}: a dry pot is watered, and a wet one is overwatered,
     * which may stress a flowering plant in it. Either way the water is poured. No check for the Nether:
     * vanilla's own bottle makes mud there, by hand and from a dispenser, with no evaporation — only a
     * bucket's water boils away.
     */
    public static void water(Level world, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(WATERED)) {
            PlantStress.roll(world, pos.above(), PlantStress.OVERWATERED, random);
        } else {
            world.setBlock(pos, state.setValue(WATERED, true), Block.UPDATE_CLIENTS);
        }
        // Vanilla's feedback for a bottle poured onto dirt (PotionItem#useOnBlock).
        world.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        world.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        if (world instanceof ServerLevel server) {
            for (int i = 0; i < 5; i++) {
                server.sendParticles(ParticleTypes.SPLASH, pos.getX() + random.nextDouble(), pos.getY() + 1.0,
                        pos.getZ() + random.nextDouble(), 1, 0.0, 0.0, 0.0, 1.0);
            }
        }
    }

    /**
     * Waters the pot at {@code potPos} from the player's hand if they hold a water bottle, handing back
     * the glass bottle. Returns {@code null} when this is not a watering, so the caller falls through to
     * {@code super} rather than a bare {@code PASS}. Also called by the plants, so a bottle used on the
     * plant waters its pot, which is where a player aims.
     */
    @Nullable
    public static InteractionResult tryWater(ItemStack stack, Level world, BlockPos potPos, Player player,
                                        InteractionHand hand) {
        BlockState pot = world.getBlockState(potPos);
        if (!isWaterBottle(stack) || !isPot(pot)) {
            return null;
        }
        if (!world.isClientSide()) {
            water(world, potPos, pot, world.getRandom());
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * A water bottle waters the soil and any fertiliser in {@code #c:fertilizers} feeds it. A water
     * bucket is refused by <b>accepting the click and doing nothing</b> ({@code CONSUME}, so no swing
     * either). Anything short of accepted lets the click go on to the bucket's own use, which pours a
     * water block into the plant's space: {@code FAIL} included, because the client treats a block's
     * {@code FAIL} as "try the item" ({@code MultiPlayerGameMode#performUseItemOn}).
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.WATER_BUCKET)) {
            return InteractionResult.CONSUME;
        }
        InteractionResult watered = tryWater(stack, world, pos, player, hand);
        if (watered != null) {
            return watered;
        }
        InteractionResult fed = GrowMedium.tryFeed(stack, state, world, pos, player);
        return fed != null ? fed : super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    // ----- bone meal feeds the soil -----

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return state.getValue(FERTILITY) < MAX_FERTILITY;
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        world.setBlock(pos, state.setValue(FERTILITY, state.getValue(FERTILITY) + 1), Block.UPDATE_CLIENTS);
    }

    /** Harvests the plant above first, as the player — see {@link GrowMedium#harvestAbove}. */
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        GrowMedium.harvestAbove(world, pos, player);
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FERTILITY, WATERED);
    }
}

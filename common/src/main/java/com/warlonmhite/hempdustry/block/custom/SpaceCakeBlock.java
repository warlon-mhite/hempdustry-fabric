package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Space Cake — vanilla's cake, baked with cannabutter. Seven bites, same shapes, same comparator
 * output, and every bite carries the dose.
 *
 * <h2>The dose lives in the blockstate, not in a block entity</h2>
 *
 * {@code BlockItem} only preserves an item's components into a <b>block entity</b>, and a cake has
 * none — so a placed cake would lose its potency and quality entirely and all seven bites would do
 * nothing. Rather than give a cake a block entity, the two axes are blockstate properties:
 * {@link #POTENCY} 0–4 and {@link #QUALITY}. Placement reads them off the stack, eating reads them
 * back off the block. No block entity, no sync, no NBT — surviving placement is what blockstates are
 * <em>for</em>.
 *
 * <p><b>The generated blockstate JSON needs no change for this.</b> Its variant keys list only
 * {@code bites}, and Minecraft treats unlisted properties as wildcards — the same partial-match trick
 * the hand-written crop blockstates rely on. Seven variants still cover all 140 states.
 *
 * <p>Potency <b>0 means inert</b>, which is what a creative-tab or {@code /give} cake is, and is why
 * the property starts at 0 rather than 1.
 *
 * <h2>Why the candle override is not optional</h2>
 *
 * {@link CakeBlock#useItemOn} matches the held item against {@code CandleCakeBlock.getCandleCake}
 * and, on a hit, <em>replaces the block outright</em> with a vanilla candle cake. Inherited unchanged
 * that would let a player quietly convert a Space Cake into an ordinary candle cake — losing ours and
 * duplicating vanilla's — and would throw the dose away with it.
 */
public class SpaceCakeBlock extends CakeBlock {
    /** Dose per bite. <b>0 is inert</b>, for a cake placed without a component. */
    public static final IntegerProperty POTENCY = IntegerProperty.create("potency", 0, EdibleEffects.MAX_TIER);
    /** Carried so a cake baked from a Perfect batch is still a Perfect cake once it is placed. */
    public static final EnumProperty<Quality> QUALITY = EnumProperty.create("quality", Quality.class);

    public SpaceCakeBlock(Properties settings) {
        super(settings);
        // Spelled out rather than trusted: a property left out of registerDefaultState resolves to the
        // first value of its set, which is exactly how the crops shipped pre-trimmed for two days.
        registerDefaultState(defaultBlockState().setValue(POTENCY, 0).setValue(QUALITY, Quality.ROUGH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POTENCY, QUALITY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        return defaultBlockState()
                .setValue(POTENCY, Mth.clamp(EdibleEffects.potencyOf(stack), 0, EdibleEffects.MAX_TIER))
                .setValue(QUALITY, EdibleEffects.qualityOf(stack));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
                                 BlockHitResult hit) {
        // Read the dose off the pre-use state: super may take a bite and rewrite the block.
        int potency = state.getValue(POTENCY);
        Quality quality = state.getValue(QUALITY);
        InteractionResult result = super.useWithoutItem(state, world, pos, player, hit);
        if (result.consumesAction() && !world.isClientSide) {
            EdibleEffects.consume(player, potency, quality);
        }
        return result;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        // Deliberately never the candle branch. Falling through to useWithoutItem means a player holding any
        // item still gets to eat, which is what they meant by right-clicking a cake.
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}

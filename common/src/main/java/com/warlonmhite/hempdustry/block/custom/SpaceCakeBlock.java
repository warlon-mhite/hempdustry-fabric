package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    /**
     * What the butter was: THC-dominant, balanced or CBD-dominant (the oven and the Infuser turn the
     * other two away, so no cake is ever either). {@link #POTENCY} is the THC tier of a THC-dominant
     * or balanced cake and the CBD tier of a CBD-dominant one; a balanced cake reads it as both, as a
     * balanced batch carries about as much of each. A cake placed by 2.1 has no such property and
     * loads THC-dominant, which is what it was.
     */
    public static final EnumProperty<Chemotype> CHEMOTYPE = EnumProperty.create("chemotype", Chemotype.class,
            chemotype -> chemotype == Chemotype.THC_DOMINANT || chemotype == Chemotype.BALANCED
                    || chemotype == Chemotype.CBD_DOMINANT);

    public SpaceCakeBlock(Properties settings) {
        super(settings);
        // Spelled out rather than trusted: a property left out of registerDefaultState resolves to the
        // first value of its set, which is exactly how the crops shipped pre-trimmed for two days.
        registerDefaultState(defaultBlockState().setValue(POTENCY, 0).setValue(QUALITY, Quality.ROUGH)
                .setValue(CHEMOTYPE, Chemotype.THC_DOMINANT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POTENCY, QUALITY, CHEMOTYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        Chemotype chemotype = Chemotype.of(stack);
        if (!CHEMOTYPE.getPossibleValues().contains(chemotype)) {
            chemotype = Chemotype.THC_DOMINANT;
        }
        int tier = chemotype.hasThc() ? EdibleEffects.potencyOf(stack) : EdibleEffects.cbdPotencyOf(stack);
        return defaultBlockState()
                .setValue(POTENCY, Mth.clamp(tier, 0, EdibleEffects.MAX_TIER))
                .setValue(QUALITY, EdibleEffects.qualityOf(stack))
                .setValue(CHEMOTYPE, chemotype);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
                                 BlockHitResult hit) {
        // Read the dose off the pre-use state: super may take a bite and rewrite the block.
        int potency = state.getValue(POTENCY);
        Quality quality = state.getValue(QUALITY);
        Chemotype chemotype = state.getValue(CHEMOTYPE);
        InteractionResult result = super.useWithoutItem(state, world, pos, player, hit);
        if (result.consumesAction() && !world.isClientSide()) {
            EdibleEffects.consume(player, chemotype.hasThc() ? potency : 0,
                    chemotype.cbdHalves() > 0 ? potency : 0, quality);
        }
        return result;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        // Deliberately never the candle branch. Falling through to useWithoutItem means a player holding any
        // item still gets to eat, which is what they meant by right-clicking a cake.
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}

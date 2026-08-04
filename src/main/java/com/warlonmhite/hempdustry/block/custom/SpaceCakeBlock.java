package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

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
 * {@link CakeBlock#onUseWithItem} matches the held item against {@code CandleCakeBlock.getCandleCake}
 * and, on a hit, <em>replaces the block outright</em> with a vanilla candle cake. Inherited unchanged
 * that would let a player quietly convert a Space Cake into an ordinary candle cake — losing ours and
 * duplicating vanilla's — and would throw the dose away with it.
 */
public class SpaceCakeBlock extends CakeBlock {
    /** Dose per bite. <b>0 is inert</b>, for a cake placed without a component. */
    public static final IntProperty POTENCY = IntProperty.of("potency", 0, EdibleEffects.MAX_TIER);
    /** Carried so a cake baked from a Perfect batch is still a Perfect cake once it is placed. */
    public static final EnumProperty<Quality> QUALITY = EnumProperty.of("quality", Quality.class);

    public SpaceCakeBlock(Settings settings) {
        super(settings);
        // Spelled out rather than trusted: a property left out of setDefaultState resolves to the
        // first value of its set, which is exactly how the crops shipped pre-trimmed for two days.
        setDefaultState(getDefaultState().with(POTENCY, 0).with(QUALITY, Quality.ROUGH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POTENCY, QUALITY);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        ItemStack stack = ctx.getStack();
        return getDefaultState()
                .with(POTENCY, MathHelper.clamp(EdibleEffects.potencyOf(stack), 0, EdibleEffects.MAX_TIER))
                .with(QUALITY, EdibleEffects.qualityOf(stack));
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        // Read the dose off the pre-use state: super may take a bite and rewrite the block.
        int potency = state.get(POTENCY);
        Quality quality = state.get(QUALITY);
        ActionResult result = super.onUse(state, world, pos, player, hit);
        if (result.isAccepted() && world instanceof ServerWorld serverWorld) {
            EdibleEffects.consume(serverWorld, player, potency, quality);
        }
        return result;
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Deliberately never the candle branch. Falling through to onUse means a player holding any
        // item still gets to eat, which is what they meant by right-clicking a cake.
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}

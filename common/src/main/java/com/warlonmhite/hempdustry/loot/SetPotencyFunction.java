package com.warlonmhite.hempdustry.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.block.custom.HempLeavesBlock;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code hempdustry:set_potency} — hands a harvested plant's potency band to what comes off it: its
 * buds carry the band, and its leaves the <i>High potency</i> mark when the plant is high or
 * extra-high. Sits on a whole crop table; seeds, stems and schwag are passed through untouched (schwag
 * is spoiled bud, its THC lost). On the Block of Hemp Leaves it hands the block's own mark back to the
 * block item ({@link HempLeavesBlock#HIGH_POTENCY}), writing nothing for an unmarked block, so its item
 * still stacks with every one 2.1 made — which vanilla's {@code copy_state} would not.
 *
 * <p>The band is the plant's record's ({@link PlantRecordBlockEntity}), which vanilla hands every block
 * loot table that breaks a block entity. A break with no record to read (a harvester that passes none,
 * a loot table rolled by a command) gives mid, which is what every bud 2.1 made reads as. The function
 * never draws on the loot table's random: the plant rolled its band when it was planted.
 */
public final class SetPotencyFunction extends LootItemConditionalFunction {

    public static final MapCodec<SetPotencyFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).apply(instance, SetPotencyFunction::new));

    private SetPotencyFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<SetPotencyFunction> getType() {
        return ModLootEntryTypes.SET_POTENCY;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_ENTITY, LootContextParams.BLOCK_STATE);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (state != null && state.hasProperty(HempLeavesBlock.HIGH_POTENCY) && stack.is(state.getBlock().asItem())) {
            return HempLeavesBlock.markBlockItem(stack,
                    state.getValue(HempLeavesBlock.HIGH_POTENCY) ? PotencyBand.HIGH : PotencyBand.MID);
        }
        BlockEntity blockEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof PlantRecordBlockEntity record)) {
            return stack;
        }
        if (stack.is(ModItems.HEMP_LEAF)) {
            PotencyBand band = record.band();
            return band.marksLeaf() ? PotencyBand.stamp(stack, PotencyBand.HIGH) : stack;
        }
        if (isBud(stack, context.getLevel().registryAccess())) {
            return PotencyBand.stamp(stack, record.band());
        }
        return stack;
    }

    /** Whether {@code stack} is the buds of a plant strain (not schwag, not hash). */
    static boolean isBud(ItemStack stack, HolderLookup.Provider registries) {
        return Strain.fromBuds(registries, stack.getItem())
                .filter(strain -> strain.value().wildFlower().isPresent())
                .isPresent();
    }

    /** The function as a loot table builder takes it. */
    public static LootItemConditionalFunction.Builder<?> setPotency() {
        return simpleBuilder(SetPotencyFunction::new);
    }
}

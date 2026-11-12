package com.warlonmhite.hempdustry.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.strain.BudTier;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code hempdustry:set_tier} — hands a harvest's tier ({@link BudTier}) to every bud that comes off
 * it, Fortune's extras and the light's bonus buds included. Sits on a whole crop table beside
 * {@code hempdustry:set_potency}; leaves, seeds, stems and schwag pass through untouched.
 *
 * <p>The tier is the plant's record's ({@link PlantRecordBlockEntity#harvestTier}): one roll a harvest,
 * from its grow luck and the luck of the player who broke it ({@code this_entity}, which vanilla hands a
 * block's loot with no luck of its own). The roll is on the record's own random, so the loot table draws
 * exactly what it did before tiers existed. A break with no record to read (a loot table rolled by a
 * command, a harvester that passes none) rolls nothing: Mids.
 */
public final class SetTierFunction extends LootItemConditionalFunction {

    public static final MapCodec<SetTierFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).apply(instance, SetTierFunction::new));

    private SetTierFunction(Optional<Holder<LootItemCondition>> condition) {
        super(condition);
    }

    @Override
    public MapCodec<SetTierFunction> codec() {
        return ModLootEntryTypes.SET_TIER;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_ENTITY, LootContextParams.THIS_ENTITY);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!SetPotencyFunction.isBud(stack, context.getLevel().registryAccess())) {
            return stack;
        }
        BlockEntity blockEntity = context.getOptional(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof PlantRecordBlockEntity record)) {
            return BudTier.stamp(stack, BudTier.MIDS);
        }
        return BudTier.stamp(stack, record.harvestTier(context, context.getLevel(), record.getBlockState(),
                context.getOptional(LootContextParams.THIS_ENTITY)));
    }

    /** The function as a loot table builder takes it. */
    public static LootItemConditionalFunction.Builder<?> setTier() {
        return simpleBuilder(SetTierFunction::new);
    }
}

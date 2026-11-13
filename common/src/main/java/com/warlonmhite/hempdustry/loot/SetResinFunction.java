package com.warlonmhite.hempdustry.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.strain.ResinStep;
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
 * {@code hempdustry:set_resin} — hands a harvested plant's resin step ({@link ResinStep}) to every bud that comes off
 * it, Fortune's extras and the light's bonus buds included. Sits on a whole crop table beside
 * {@code hempdustry:set_potency} and {@code hempdustry:set_tier}; leaves, seeds, stems and schwag pass through
 * untouched, since they carry no resin worth a step.
 *
 * <p>The step is the plant's record's ({@link PlantRecordBlockEntity}), rolled when the plant was planted, so the
 * function never draws on the loot table's random. A break with no record to read (a loot table rolled by a
 * command, a harvester that passes none) writes nothing: the buds sieve as every bud did before resin.
 */
public final class SetResinFunction extends LootItemConditionalFunction {

    public static final MapCodec<SetResinFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).apply(instance, SetResinFunction::new));

    private SetResinFunction(Optional<Holder<LootItemCondition>> condition) {
        super(condition);
    }

    @Override
    public MapCodec<SetResinFunction> codec() {
        return ModLootEntryTypes.SET_RESIN;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!SetPotencyFunction.isBud(stack, context.getLevel().registryAccess())) {
            return stack;
        }
        BlockEntity blockEntity = context.getOptional(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof PlantRecordBlockEntity record)) {
            return stack;
        }
        return ResinStep.stamp(stack, record.resin());
    }

    /** The function as a loot table builder takes it. */
    public static LootItemConditionalFunction.Builder<?> setResin() {
        return simpleBuilder(SetResinFunction::new);
    }
}

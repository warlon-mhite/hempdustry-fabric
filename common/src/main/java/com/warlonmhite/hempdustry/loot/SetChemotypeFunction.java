package com.warlonmhite.hempdustry.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.block.custom.ChemotypeBlocks;
import com.warlonmhite.hempdustry.recipe.Stamping;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code hempdustry:set_chemotype} — stamps a dropped piece of hemp matter with the chemotype of what
 * it came from. Three sources, asked in this order:
 *
 * <ol>
 *   <li>{@code "from_block": true} — the broken block's own {@code chemotype} state, for the blocks
 *       that store nine stamped items (the leaf block, the hash bars, the charas ball);</li>
 *   <li>{@code "strain": "<id>"} — the named strain's chemotype as the world loaded it, so a datapack
 *       that makes Purple Kush CBD-dominant gets CBD leaves without touching a loot table. The strain
 *       is named by key and looked up when the loot rolls: a strain this world does not define stamps
 *       nothing, and nothing fails to load;</li>
 *   <li>{@code "chemotype": "<name>"} — a fixed chemotype, for the plants with no strain record
 *       (Industrial Hemp, Ditchweed).</li>
 * </ol>
 *
 * <p>Only stamped matter takes it (a leaf, schwag, hash, a block of them): a plant's buds and seeds
 * are their strain and stay unstamped, so the function can sit on a whole crop table.
 */
public final class SetChemotypeFunction extends LootItemConditionalFunction {

    public static final MapCodec<SetChemotypeFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> commonFields(instance)
            .and(instance.group(
                    Codec.BOOL.optionalFieldOf("from_block", false).forGetter(function -> function.fromBlock),
                    ResourceKey.codec(Strain.REGISTRY_KEY).optionalFieldOf("strain").forGetter(function -> function.strain),
                    Chemotype.CODEC.optionalFieldOf("chemotype").forGetter(function -> function.chemotype)))
            .apply(instance, SetChemotypeFunction::new));

    private final boolean fromBlock;
    private final Optional<ResourceKey<Strain>> strain;
    private final Optional<Chemotype> chemotype;

    private SetChemotypeFunction(Optional<Holder<LootItemCondition>> condition, boolean fromBlock,
                                 Optional<ResourceKey<Strain>> strain, Optional<Chemotype> chemotype) {
        super(condition);
        this.fromBlock = fromBlock;
        this.strain = strain;
        this.chemotype = chemotype;
    }

    @Override
    public MapCodec<SetChemotypeFunction> codec() {
        return ModLootEntryTypes.SET_CHEMOTYPE;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return fromBlock ? Set.of(LootContextParams.BLOCK_STATE) : Set.of();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (fromBlock) {
            BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
            if (state != null && state.hasProperty(ChemotypeBlocks.CHEMOTYPE)) {
                return Stamping.apply(stack, state.getValue(ChemotypeBlocks.CHEMOTYPE));
            }
        }
        if (strain.isPresent()) {
            Optional<Holder.Reference<Strain>> loaded =
                    Strain.registry(context.getLevel().registryAccess()).get(strain.get());
            if (loaded.isPresent()) {
                return Stamping.apply(stack, loaded.get().value().chemotype());
            }
            return stack;
        }
        return chemotype.map(value -> Stamping.apply(stack, value)).orElse(stack);
    }

    /** Stamps with the broken block's own chemotype state. */
    public static LootItemConditionalFunction.Builder<?> fromBlock() {
        return simpleBuilder(condition -> new SetChemotypeFunction(condition, true, Optional.empty(), Optional.empty()));
    }

    /** Stamps with {@code strain}'s chemotype as the world loaded it. */
    public static LootItemConditionalFunction.Builder<?> ofStrain(ResourceKey<Strain> strain) {
        return simpleBuilder(condition -> new SetChemotypeFunction(condition, false, Optional.of(strain), Optional.empty()));
    }

    /** Stamps with a fixed chemotype. */
    public static LootItemConditionalFunction.Builder<?> of(Chemotype chemotype) {
        return simpleBuilder(condition -> new SetChemotypeFunction(condition, false, Optional.empty(), Optional.of(chemotype)));
    }
}

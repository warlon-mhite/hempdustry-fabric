package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootPredicates;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.DispatcherProvider;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConditionalValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.NumberDispatcher;

/**
 * What burns in a furnace and what goes in a composter, as Minecraft keeps it since 26.3: a component
 * on the item ({@code cooking_fuel}, {@code compostable}) that names a number provider, a datapack
 * entry under {@code context_int_provider/}. The mod still lists its fuels and compost in one place,
 * {@code Hempdustry}, through {@code Registration#fuel} and {@code #compostable}; this class turns that
 * list into the two things 26.3 wants:
 *
 * <ul>
 *   <li>one number provider per value, {@code hempdustry:cooking/time_<ticks>} and
 *       {@code hempdustry:compostable/chance_<percent>}, written by the data generator in vanilla's
 *       own shape, so a blast furnace still burns a hemp fuel twice as fast and an empty composter
 *       still always takes a first layer; a datapack can retune hemp alone through them;</li>
 *   <li>the default components, handed to each loader's default-component event.</li>
 * </ul>
 *
 * <p>The values are the ones the mod has always had, so nothing burns or composts differently.
 */
public final class ModFuelAndCompost {
    private static final Map<Item, Integer> FUELS = new LinkedHashMap<>();
    private static final Map<Item, Float> COMPOSTABLES = new LinkedHashMap<>();

    private ModFuelAndCompost() {
    }

    /** A default component to set on an item, as the loaders' events take it. */
    public interface DefaultComponents {
        <T> void set(Item item, DataComponentType<T> type, T value);
    }

    public static void fuel(ItemLike item, int ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("a fuel burns for at least one tick: " + item);
        }
        FUELS.put(item.asItem(), ticks);
    }

    public static void compostable(ItemLike item, float chance) {
        percent(chance);
        COMPOSTABLES.put(item.asItem(), chance);
    }

    public static Map<Item, Integer> fuels() {
        return Collections.unmodifiableMap(FUELS);
    }

    public static Map<Item, Float> compostables() {
        return Collections.unmodifiableMap(COMPOSTABLES);
    }

    public static ResourceKey<ContextIntProvider> burnTime(int ticks) {
        return ResourceKey.create(Registries.CONTEXT_INT_PROVIDER,
                Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "cooking/time_" + ticks));
    }

    public static ResourceKey<ContextIntProvider> compostLayers(float chance) {
        return ResourceKey.create(Registries.CONTEXT_INT_PROVIDER,
                Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "compostable/chance_" + percent(chance)));
    }

    /** Every listed item's fuel and compost components. */
    public static void defaults(DefaultComponents components) {
        FUELS.forEach((item, ticks) -> components.set(item, DataComponents.COOKING_FUEL,
                new CookingFuel(burnTime(ticks), ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)));
        COMPOSTABLES.forEach((item, chance) -> components.set(item, DataComponents.COMPOSTABLE,
                new Compostable(compostLayers(chance))));
    }

    /** One number provider per value the mod uses, for the data generator. */
    public static void bootstrap(BootstrapContext<ContextIntProvider> context) {
        HolderGetter<LootItemCondition> predicates = context.lookup(Registries.PREDICATE);
        HolderGetter<ContextIntProvider> numbers = context.lookup(Registries.CONTEXT_INT_PROVIDER);
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        // Vanilla's fuel shape (ContextIntProviders#cooking): the ticks divided by 2 in a smoker or a
        // blast furnace and by 1 anywhere else, both divisors vanilla's own entries.
        Holder<ContextIntProvider> divisor = Holder.direct(new ConditionalValue(
                predicates.getOrThrow(LootPredicates.FAST_FURNACE),
                numbers.getOrThrow(ContextIntProviders.COOKING_FAST_BURN_TIME_REDUCTION_FACTOR),
                numbers.getOrThrow(ContextIntProviders.COOKING_NORMAL_BURN_TIME_REDUCTION_FACTOR)));
        for (int ticks : new LinkedHashSet<>(FUELS.values())) {
            context.register(burnTime(ticks), ContextIntProviders.div(ContextIntProviders.exactly(ticks), divisor).value());
        }
        Set<Float> chances = new LinkedHashSet<>(COMPOSTABLES.values());
        for (float chance : chances) {
            context.register(compostLayers(chance), compost(blocks, percent(chance)));
        }
    }

    /**
     * Vanilla's compost shape (ContextIntProviders#compostable): an empty composter always takes the
     * layer, any other takes it {@code percent} times in a hundred.
     */
    private static ContextIntProvider compost(HolderGetter<Block> blocks, int percent) {
        if (percent >= 100) {
            return new ConstantValue(1);
        }
        DispatcherProvider.Case<ContextIntProvider> empty = new DispatcherProvider.Case<>(
                Holder.direct(MatchBlock.blockMatches(blocks, Blocks.COMPOSTER, StatePropertiesPredicate.Builder.properties()
                        .hasProperty(ComposterBlock.LEVEL, 0)).build()),
                ContextIntProviders.exactly(1));
        return new NumberDispatcher(List.of(empty), ContextIntProviders.weighted(WeightedList.<Holder<ContextIntProvider>>builder()
                .add(ContextIntProviders.exactly(1), percent)
                .add(ContextIntProviders.exactly(0), 100 - percent)
                .build()));
    }

    /** A chance as whole percent, which is what vanilla's weights can hold; anything finer is refused. */
    private static int percent(float chance) {
        int percent = Math.round(chance * 100.0F);
        if (percent <= 0 || percent > 100 || Math.abs(percent - chance * 100.0F) > 1.0E-3F) {
            throw new IllegalArgumentException("a compost chance is a whole percent above 0: " + chance);
        }
        return percent;
    }
}

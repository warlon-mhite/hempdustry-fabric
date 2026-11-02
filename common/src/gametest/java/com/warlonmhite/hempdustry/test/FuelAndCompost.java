package com.warlonmhite.hempdustry.test;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.NumberDispatcher;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.ints.WeightedListValue;
import net.minecraft.world.phys.Vec3;

/**
 * What the game's furnace and composter make of an item, asked the way they ask it. Since 26.3 both
 * read a component on the item ({@code cooking_fuel}, {@code compostable}) that names a number
 * provider in the world's data, the same on every loader.
 */
final class FuelAndCompost {
    private FuelAndCompost() {
    }

    /** Whether a furnace takes {@code item} as fuel at all. */
    static boolean isFuel(ItemLike item) {
        return new ItemStack(item).has(DataComponents.COOKING_FUEL);
    }

    /**
     * How long {@code item} burns in a plain furnace, resolved as {@code AbstractFurnaceBlockEntity}
     * resolves it, against a furnace's own container context; 0 when it is no fuel.
     */
    static int burnTime(GameTestHelper context, ItemLike item) {
        ServerLevel level = context.getLevel();
        BlockPos pos = context.absolutePos(BlockPos.ZERO);
        BlockState state = Blocks.FURNACE.defaultBlockState();
        FurnaceBlockEntity furnace = new FurnaceBlockEntity(pos, state);
        LootContext loot = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.BLOCK_ENTITY, furnace)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.CONTAINER, furnace)
                .create(LootContextParamSets.CONTAINER_PROCESS))
                .create(Optional.empty());
        return ResolvableInt.getFromItem(new ItemStack(item), DataComponents.COOKING_FUEL, CookingFuel::burnTime, loot, 0);
    }

    /**
     * The chance {@code item} raises a composter that already holds something, or {@code -1} when it
     * does not compost. A composter rolls for it, so this reads the provider rather than rolling: a
     * whole layer or none, or vanilla's shape, an empty composter's case over a weighted one-or-none.
     */
    static float compostChance(GameTestHelper context, ItemLike item) {
        Compostable compostable = new ItemStack(item).get(DataComponents.COMPOSTABLE);
        if (compostable == null) {
            return -1.0F;
        }
        return switch (compostable.layers()) {
            case ResolvableInt.Constant constant -> constant.value() >= 1 ? 1.0F : 0.0F;
            case ResolvableInt.Reference reference -> chance(context.getLevel().getServer().reloadableRegistries().lookup()
                    .lookupOrThrow(Registries.CONTEXT_INT_PROVIDER).getOrThrow(reference.key()).value(), true);
            default -> throw new IllegalStateException("an unknown compost amount: " + compostable.layers());
        };
    }

    private static float chance(ContextIntProvider provider, boolean top) {
        return switch (provider) {
            case ConstantValue constant -> constant.value() >= 1 ? 1.0F : 0.0F;
            // Vanilla's empty-composter case comes first; the default is a composter with something in.
            case NumberDispatcher dispatcher when top -> chance(dispatcher.defaultValue().value(), false);
            case WeightedListValue weighted -> {
                int total = 0;
                int layer = 0;
                for (var entry : weighted.distribution().unwrap()) {
                    total += entry.weight();
                    if (chance(entry.value().value(), false) >= 1.0F) {
                        layer += entry.weight();
                    }
                }
                yield total == 0 ? 0.0F : (float) layer / total;
            }
            default -> throw new IllegalStateException("a compost provider this test cannot read: " + provider);
        };
    }
}

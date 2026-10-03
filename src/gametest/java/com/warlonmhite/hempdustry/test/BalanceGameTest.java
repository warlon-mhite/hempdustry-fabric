package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.test.TestContext;

import java.util.Optional;

/**
 * The balance a world loads is the balance the mod was written with.
 *
 * <p>How the smokeables smoke, what an edible does and what a green-out costs are data, and the
 * shipped files are written by datagen from the built-in values. This reads every entry straight out
 * of the server's registries -- not through the lookups the game uses, which fall back to the
 * built-in numbers when an entry is missing and would pass a world that never loaded the files -- and
 * holds each one to the numbers it replaced, to the tick. The rebalance tests are then the net for
 * what those numbers do.
 */
public final class BalanceGameTest {

    public static void theShippedBalanceIsTheBuiltInOne(TestContext context) {
        RegistryWrapper.WrapperLookup registries = context.getWorld().getRegistryManager();
        for (DeviceType type : DeviceType.values()) {
            assertLoaded(context, registries, DeviceStats.REGISTRY_KEY, DeviceStats.key(type), type.builtInStats());
        }
        assertLoaded(context, registries, DeviceStats.REGISTRY_KEY, DeviceStats.SPLIFF, DeviceStats.SPLIFF_BUILT_IN);
        assertLoaded(context, registries, EdibleBundle.REGISTRY_KEY, EdibleBundle.DEFAULT, EdibleBundle.BUILT_IN);
        assertLoaded(context, registries, GreenOut.REGISTRY_KEY, GreenOut.DEFAULT, GreenOut.BUILT_IN);
        context.complete();
    }

    private static <T> void assertLoaded(TestContext context, RegistryWrapper.WrapperLookup registries,
                                         RegistryKey<Registry<T>> registry, RegistryKey<T> key, T builtIn) {
        Optional<T> loaded = registries.getOrThrow(registry).getOptional(key).map(RegistryEntry.Reference::value);
        context.assertTrue(loaded.isPresent(), key.getValue() + " is not in this world's data");
        context.assertEquals(builtIn, loaded.orElseThrow(), "the numbers " + key.getValue() + " ships");
    }
}

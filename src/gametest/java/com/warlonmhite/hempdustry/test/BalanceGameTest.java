package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;

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

    public static void theShippedBalanceIsTheBuiltInOne(GameTestHelper context) {
        HolderLookup.Provider registries = context.getLevel().registryAccess();
        for (DeviceType type : DeviceType.values()) {
            assertLoaded(context, registries, DeviceStats.REGISTRY_KEY, DeviceStats.key(type), type.builtInStats());
        }
        assertLoaded(context, registries, DeviceStats.REGISTRY_KEY, DeviceStats.SPLIFF, DeviceStats.SPLIFF_BUILT_IN);
        assertLoaded(context, registries, EdibleBundle.REGISTRY_KEY, EdibleBundle.DEFAULT, EdibleBundle.BUILT_IN);
        assertLoaded(context, registries, GreenOut.REGISTRY_KEY, GreenOut.DEFAULT, GreenOut.BUILT_IN);
        context.succeed();
    }

    private static <T> void assertLoaded(GameTestHelper context, HolderLookup.Provider registries,
                                         ResourceKey<Registry<T>> registry, ResourceKey<T> key, T builtIn) {
        Optional<T> loaded = registries.lookupOrThrow(registry).get(key).map(Holder.Reference::value);
        context.assertTrue(loaded.isPresent(), key.identifier() + " is not in this world's data");
        context.assertValueEqual(builtIn, loaded.orElseThrow(), "the numbers " + key.identifier() + " ships");
    }
}

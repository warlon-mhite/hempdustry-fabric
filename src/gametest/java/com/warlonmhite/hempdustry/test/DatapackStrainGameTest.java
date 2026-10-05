package com.warlonmhite.hempdustry.test;

import com.mojang.serialization.Lifecycle;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A datapack strain that reuses another strain's items must not break the creative tab.
 *
 * <p>The tab adds one seeds, buds and flower stack per strain, and vanilla throws on a second
 * identical stack ("Accidentally adding the same item stack twice"). Before 2.0.3 a strain pointing
 * at Purple Kush's items therefore crashed every creative client on pressing E, and took the whole
 * tab out of JEI. The tab is common code, so a headless server can build it: here it is built
 * against the world's strains plus a copy of Purple Kush under another id, exactly what such a pack
 * adds.
 */
public final class DatapackStrainGameTest {

    public static void aStrainReusingItemsKeepsTheTabWhole(TestContext context) {
        ServerWorld world = context.getWorld();
        RegistryWrapper.WrapperLookup real = world.getRegistryManager();
        Registry<Strain> loaded = world.getRegistryManager().getOrThrow(Strain.REGISTRY_KEY);
        Strain purpleKush = loaded.getValueOrThrow(ModStrains.INDICA);

        SimpleRegistry<Strain> strains = new SimpleRegistry<>(Strain.REGISTRY_KEY, Lifecycle.stable());
        loaded.streamEntries().forEach(entry -> Registry.register(strains, entry.registryKey(), entry.value()));
        // A different translation key keeps the record unequal to Purple Kush's; the items are the same.
        Registry.register(strains, RegistryKey.of(Strain.REGISTRY_KEY, Identifier.of("hempdustry-gametest", "copy")),
                new Strain("strain.hempdustry-gametest.copy", purpleKush.color(), purpleKush.modelIndex(),
                        purpleKush.seeds(), purpleKush.buds(), purpleKush.flower(), purpleKush.smokeEffects()));

        RegistryWrapper.WrapperLookup withCopy = new RegistryWrapper.WrapperLookup() {
            @Override
            public Stream<RegistryKey<? extends Registry<?>>> streamAllRegistryKeys() {
                return real.streamAllRegistryKeys();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<? extends RegistryWrapper.Impl<T>> getOptional(
                    RegistryKey<? extends Registry<? extends T>> key) {
                return key.equals(Strain.REGISTRY_KEY)
                        ? Optional.of((RegistryWrapper.Impl<T>) (Object) strains)
                        : real.getOptional(key);
            }
        };

        // What CreativeInventoryScreen does on opening; the duplicate throws from in here.
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.updateEntries(
                new ItemGroup.DisplayContext(world.getEnabledFeatures(), true, withCopy));

        long seeds = ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayStacks().stream()
                .filter(stack -> stack.isOf(purpleKush.seeds()))
                .count();
        context.assertEquals(1L, seeds, "the stacks of Purple Kush's seeds in the tab");
        context.complete();
    }
}

package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
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
public final class DatapackStrainGameTest implements FabricGameTest {

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void aStrainReusingItemsKeepsTheTabWhole(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider real = world.registryAccess();
        Registry<Strain> loaded = world.registryAccess().registryOrThrow(Strain.REGISTRY_KEY);
        Strain purpleKush = loaded.getOrThrow(ModStrains.INDICA);

        MappedRegistry<Strain> strains = new MappedRegistry<>(Strain.REGISTRY_KEY, Lifecycle.stable());
        loaded.holders().forEach(entry -> Registry.register(strains, entry.key(), entry.value()));
        // Purple Kush's own definition through the codec a datapack goes through, under another id and
        // translation key (which keeps the record unequal to Purple Kush's); the items are the same.
        JsonObject json = Strain.CODEC.encodeStart(JsonOps.INSTANCE, purpleKush).getOrThrow().getAsJsonObject();
        json.addProperty("translation_key", "strain.hempdustry-gametest.copy");
        Registry.register(strains, ResourceKey.create(Strain.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("hempdustry-gametest", "copy")),
                Strain.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        strains.freeze();

        HolderLookup.Provider withCopy = new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return real.listRegistries();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> key) {
                return key.equals(Strain.REGISTRY_KEY)
                        ? Optional.of((HolderLookup.RegistryLookup<T>) (Object) strains.asLookup())
                        : real.lookup(key);
            }
        };

        // What CreativeModeInventoryScreen does on opening; the duplicate throws from in here.
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.buildContents(
                new CreativeModeTab.ItemDisplayParameters(world.enabledFeatures(), true, withCopy));

        long buds = ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayItems().stream()
                .filter(stack -> stack.is(purpleKush.buds()))
                .count();
        context.assertValueEqual(buds, 1L, "the stacks of Purple Kush's buds in the tab");
        context.succeed();
    }
}

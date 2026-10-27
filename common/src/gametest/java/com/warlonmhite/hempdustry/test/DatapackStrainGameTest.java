package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;

/**
 * A datapack strain that reuses another strain's items must not break the creative tab.
 *
 * <p>The tab adds one seeds, buds and flower stack per strain, and vanilla throws on a second
 * identical stack ("Accidentally adding the same item stack twice"). Before 2.0.3 a strain pointing
 * at Purple Kush's items therefore crashed every creative client on pressing E, and took the whole
 * tab out of JEI. The tab is common code, so a headless server can build it: here it is built
 * against the world's strains plus a copy of Purple Kush under another id, exactly what such a pack
 * adds.
 *
 * <p>And the buds still pack as Purple Kush. A dynamic registry has no defined order, so a lookup
 * that took whichever strain came first could pack them as the copy on one world load and as Purple
 * Kush on the next; the copy is registered first here so that lookup fails.
 */
public final class DatapackStrainGameTest {

    public static void aStrainReusingItemsKeepsTheTabWhole(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider real = world.registryAccess();
        Registry<Strain> loaded = world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY);
        Strain purpleKush = loaded.getValueOrThrow(ModStrains.INDICA);

        MappedRegistry<Strain> strains = new MappedRegistry<>(Strain.REGISTRY_KEY, Lifecycle.stable());
        // Purple Kush's own definition through the codec a datapack goes through, under another id and
        // translation key (which keeps the record unequal to Purple Kush's); the items are the same.
        JsonObject json = Strain.CODEC.encodeStart(JsonOps.INSTANCE, purpleKush).getOrThrow().getAsJsonObject();
        json.addProperty("translation_key", "strain.hempdustry-gametest.copy");
        Registry.register(strains, ResourceKey.create(Strain.REGISTRY_KEY, Identifier.fromNamespaceAndPath("hempdustry-gametest", "copy")),
                Strain.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        // Then the real strains, after the copy: a lookup that takes whichever comes first finds the copy.
        loaded.listElements().forEach(entry -> Registry.register(strains, entry.key(), entry.value()));

        HolderLookup.Provider withCopy = new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return real.listRegistryKeys();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> key) {
                return key.equals(Strain.REGISTRY_KEY)
                        ? Optional.of((HolderLookup.RegistryLookup<T>) (Object) strains)
                        : real.lookup(key);
            }
        };

        // What CreativeModeInventoryScreen does on opening; the duplicate throws from in here.
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.buildContents(
                new CreativeModeTab.ItemDisplayParameters(world.enabledFeatures(), true, withCopy));

        long buds = ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayItems().stream()
                .filter(stack -> stack.is(purpleKush.buds()))
                .count();
        context.assertValueEqual(1L, buds, "the stacks of Purple Kush's buds in the tab");

        // And packing those buds packs Purple Kush, the strain they belong to, not the copy.
        context.assertTrue(Strain.fromBuds(withCopy, purpleKush.buds()).map(entry -> entry.is(ModStrains.INDICA)).orElse(false),
                "Purple Kush's buds pack as " + Strain.fromBuds(withCopy, purpleKush.buds()).map(entry -> entry.key().toString()).orElse("nothing"));
        context.succeed();
    }
}

package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.component.StackRegistries;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.registry.DataRegistries;
import com.warlonmhite.hempdustry.registry.Holder;
import com.warlonmhite.hempdustry.registry.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A datapack strain that reuses another strain's items must not break the creative tab.
 *
 * <p>The tab adds one seeds, buds and flower stack per strain, and the newer lines' vanilla throws on
 * a second identical stack ("Accidentally adding the same item stack twice"). Before 2.0.3 a strain
 * pointing at Purple Kush's items therefore crashed every creative client on pressing E, and took the
 * whole tab out of JEI. 1.18.2's tab does not throw, but it would list the buds twice, and the mod's
 * one fill is the same code on every line. The tab is common code, so a headless server can build it:
 * here it is built against the world's strains plus a copy of Purple Kush under another id, exactly
 * what such a pack adds.
 *
 * <p>And the buds still pack as Purple Kush. A dynamic registry has no defined order, so a lookup
 * that took whichever strain came first could pack them as the copy on one world load and as Purple
 * Kush on the next; the copy is listed first here so that lookup fails.
 */
public final class DatapackStrainGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aStrainReusingItemsKeepsTheTabWhole(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider real = DataRegistries.of(world);
        HolderLookup.RegistryLookup<Strain> loaded = real.lookupOrThrow(Strain.REGISTRY_KEY);
        Strain purpleKush = loaded.getOrThrow(ModStrains.INDICA).value();

        // Purple Kush's own definition through the codec a datapack goes through, under another id and
        // translation key (which keeps the record unequal to Purple Kush's); the items are the same.
        JsonObject json = Strain.CODEC.encodeStart(JsonOps.INSTANCE, purpleKush).getOrThrow(false, error -> { }).getAsJsonObject();
        json.addProperty("translation_key", "strain.hempdustry-gametest.copy");
        List<Holder.Reference<Strain>> strains = new ArrayList<>();
        strains.add(new Holder.Reference<>(ResourceKey.create(Strain.REGISTRY_KEY, new ResourceLocation("hempdustry-gametest", "copy")),
                Strain.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, error -> { })));
        // Then the real strains, after the copy: a lookup that takes whichever comes first finds the copy.
        strains.addAll(loaded.listElements().collect(Collectors.toList()));

        HolderLookup.RegistryLookup<Strain> withCopyStrains = new HolderLookup.RegistryLookup<Strain>() {
            @Override
            public ResourceKey<? extends Registry<? extends Strain>> key() {
                return Strain.REGISTRY_KEY;
            }

            @Override
            public Stream<Holder.Reference<Strain>> listElements() {
                return strains.stream();
            }

            @Override
            public Optional<Holder.Reference<Strain>> get(ResourceKey<Strain> key) {
                return strains.stream().filter(entry -> entry.key().equals(key)).findFirst();
            }
        };
        HolderLookup.Provider withCopy = new HolderLookup.Provider() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> key) {
                return key.equals(Strain.REGISTRY_KEY)
                        ? Optional.of((HolderLookup.RegistryLookup<T>) (Object) withCopyStrains)
                        : real.lookup(key);
            }
        };

        // What a creative client does on opening the tab, with the lookup the tab is built from; on
        // the newer lines the duplicate throws from in here.
        List<List<ItemStack>> tab = new ArrayList<>();
        StackRegistries.with(withCopy, () -> tab.add(CreativeTab.tab()));

        long buds = tab.get(0).stream()
                .filter(stack -> stack.getItem() == purpleKush.buds())
                .count();
        Line.equal(context, buds, 1L, "the stacks of Purple Kush's buds in the tab");

        // And packing those buds packs Purple Kush, the strain they belong to, not the copy.
        Asserts.assertTrue(context, Strain.fromBuds(withCopy, purpleKush.buds()).map(entry -> entry.is(ModStrains.INDICA)).orElse(false),
                "Purple Kush's buds pack as " + Strain.fromBuds(withCopy, purpleKush.buds()).map(entry -> entry.key().toString()).orElse("nothing"));
        context.succeed();
    }
}

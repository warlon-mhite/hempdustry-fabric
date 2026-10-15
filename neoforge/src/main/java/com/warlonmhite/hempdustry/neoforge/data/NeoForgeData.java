package com.warlonmhite.hempdustry.neoforge.data;

import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.neoforge.NeoForgeRegistration;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The data only NeoForge reads, written from what the mod registered: NeoForge keeps compost chances
 * and fuel times in data maps and adds features to biomes through biome modifiers, all datapack files,
 * where Fabric takes the same numbers in code. Generated from the very calls Fabric's registries take
 * ({@code Hempdustry.init}), so the two jars cannot disagree; {@code ./gradlew :neoforge:runData}
 * writes them to {@code neoforge/src/generated/resources}, which is committed. Everything else the
 * mod's datapack holds is Fabric's datagen, shared by both jars.
 */
public final class NeoForgeData {
    private NeoForgeData() {
    }

    public static void gather(GatherDataEvent event) {
        NeoForgeRegistration registration = (NeoForgeRegistration) Services.REGISTRATION;
        event.createProvider((output, lookup) -> new DataMaps(output, lookup, registration));
        event.createProvider(output -> new BiomeModifiers(output, registration));
    }

    private static final class DataMaps extends DataMapProvider {
        private final NeoForgeRegistration registration;

        DataMaps(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, NeoForgeRegistration registration) {
            super(output, lookup);
            this.registration = registration;
        }

        @Override
        protected void gather(HolderLookup.Provider provider) {
            // A villager farmer composts only vanilla's own short list, on either loader.
            var compostables = builder(NeoForgeDataMaps.COMPOSTABLES);
            registration.compostables().forEach((item, chance) ->
                    compostables.add(item.asItem().builtInRegistryHolder(), new Compostable(chance, false), false));
            var fuels = builder(NeoForgeDataMaps.FURNACE_FUELS);
            registration.fuels().forEach((item, ticks) ->
                    fuels.add(item.asItem().builtInRegistryHolder(), new FurnaceFuel(ticks), false));
        }
    }

    /** One {@code neoforge:add_features} file per feature, named after it. */
    private record BiomeModifiers(PackOutput output, NeoForgeRegistration registration) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            PackOutput.PathProvider files = output.createPathProvider(PackOutput.Target.DATA_PACK, "neoforge/biome_modifier");
            List<CompletableFuture<?>> written = registration.features().stream().<CompletableFuture<?>>map(feature -> {
                JsonObject json = new JsonObject();
                json.addProperty("type", "neoforge:add_features");
                json.addProperty("biomes", "#" + feature.biomes().location());
                json.addProperty("features", feature.feature().location().toString());
                json.addProperty("step", feature.step().getSerializedName());
                return DataProvider.saveStable(cache, json, files.json(feature.feature().location()));
            }).toList();
            return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
        }

        @Override
        public String getName() {
            return "Biome modifiers";
        }
    }
}

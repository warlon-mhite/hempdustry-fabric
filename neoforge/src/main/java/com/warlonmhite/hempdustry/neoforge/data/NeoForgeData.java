package com.warlonmhite.hempdustry.neoforge.data;

import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.neoforge.NeoForgeRegistration;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The data only NeoForge reads, written from what the mod registered: NeoForge adds features to
 * biomes through biome modifiers, datapack files, where Fabric takes the same features in code.
 * (Compost chances and fuel times were NeoForge data maps too, until 26.3 made them item components
 * on every loader.) Generated from the very calls Fabric's registries take
 * ({@code Hempdustry.init}), so the two jars cannot disagree; {@code ./gradlew :neoforge:runData}
 * writes them to {@code neoforge/src/generated/resources}, which is committed. Everything else the
 * mod's datapack holds is Fabric's datagen, shared by both jars.
 */
public final class NeoForgeData {
    private NeoForgeData() {
    }

    public static void gather(GatherDataEvent.Client event) {
        NeoForgeRegistration registration = (NeoForgeRegistration) Services.REGISTRATION;
        event.createProvider(output -> new BiomeModifiers(output, registration));
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
                json.addProperty("features", feature.feature().identifier().toString());
                json.addProperty("step", feature.step().getSerializedName());
                return DataProvider.saveStable(cache, json, files.json(feature.feature().identifier()));
            }).toList();
            return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
        }

        @Override
        public String getName() {
            return "Biome modifiers";
        }
    }
}

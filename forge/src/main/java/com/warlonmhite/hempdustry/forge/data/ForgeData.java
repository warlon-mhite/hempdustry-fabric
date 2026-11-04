package com.warlonmhite.hempdustry.forge.data;

import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.forge.ForgeRegistration;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The data only Forge reads, written from what the mod registered: Forge adds features to biomes
 * through biome modifiers, datapack files, where Fabric takes the same calls in code. Generated from
 * the very calls Fabric's registries take ({@code Hempdustry.init}), so the two jars cannot disagree;
 * {@code ./gradlew :forge:runData} writes them to {@code forge/src/generated/resources}, which is
 * committed. Everything else the mod's datapack holds is Fabric's datagen, shared by both jars.
 */
public final class ForgeData {
    private ForgeData() {
    }

    public static void gather(GatherDataEvent event) {
        ForgeRegistration registration = (ForgeRegistration) Services.REGISTRATION;
        event.getGenerator().addProvider(event.includeServer(),
                new BiomeModifiers(event.getGenerator().getPackOutput(), registration));
    }

    /** One {@code forge:add_features} file per feature, named after it. */
    private record BiomeModifiers(PackOutput output, ForgeRegistration registration) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            PackOutput.PathProvider files = output.createPathProvider(PackOutput.Target.DATA_PACK, "forge/biome_modifier");
            List<CompletableFuture<?>> written = registration.features().stream().<CompletableFuture<?>>map(feature -> {
                JsonObject json = new JsonObject();
                json.addProperty("type", "forge:add_features");
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

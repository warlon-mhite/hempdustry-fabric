package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import java.util.concurrent.CompletableFuture;

/**
 * Writes the mod's own strains out as {@code data/hempdustry/hempdustry/strain/<id>.json}.
 *
 * <p>Kept apart from {@link ModWorldGenerator} — which does the same job for the worldgen registries —
 * because the two have nothing to do with each other, and a provider's name is what shows up in the
 * datagen log when something fails.
 *
 * <p><b>The generated files are the documentation.</b> A server owner who wants to rebalance a strain
 * copies one of these into their own datapack and edits it, so what datagen emits is the worked
 * example of the format.
 */
public class ModStrainProvider extends FabricDynamicRegistryProvider {
    public ModStrainProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, Entries entries) {
        entries.addAll(registries.lookupOrThrow(Strain.REGISTRY_KEY));
    }

    @Override
    public String getName() {
        return "Strains";
    }
}

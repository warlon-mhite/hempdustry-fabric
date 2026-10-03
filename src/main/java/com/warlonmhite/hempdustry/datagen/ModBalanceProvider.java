package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.balance.GreenOut;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

/**
 * Writes the mod's own balance out as {@code data/hempdustry/hempdustry/device/*.json},
 * {@code edible/default.json} and {@code green_out/default.json}. As with the strains, the generated
 * files are the documentation: a server owner who wants a gentler bong copies its file into their
 * own datapack and edits it.
 */
public class ModBalanceProvider extends FabricDynamicRegistryProvider {
    public ModBalanceProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        entries.addAll(registries.getOrThrow(DeviceStats.REGISTRY_KEY));
        entries.addAll(registries.getOrThrow(EdibleBundle.REGISTRY_KEY));
        entries.addAll(registries.getOrThrow(GreenOut.REGISTRY_KEY));
    }

    @Override
    public String getName() {
        return "Balance";
    }
}

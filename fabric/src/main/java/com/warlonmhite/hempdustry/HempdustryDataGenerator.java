package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.datagen.*;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.world.ModConfiguredFeatures;
import com.warlonmhite.hempdustry.world.ModPlacedFeatures;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class HempdustryDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(ModBlockTagProvider::new);
		pack.addProvider(ModItemTagProvider::new);
		pack.addProvider(ModLootTableProvider::new);
		pack.addProvider(ModModelProvider::new);   // client-side: needs configureDataGeneration { client = true }
		pack.addProvider(ModRecipeProvider::new);
		pack.addProvider(ModWorldGenerator::new);
		pack.addProvider(ModStrainProvider::new);
		pack.addProvider(ModBalanceProvider::new);
		pack.addProvider(ModAdvancementProvider::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Strain.REGISTRY_KEY, ModStrains::bootstrap);
		registryBuilder.add(DeviceStats.REGISTRY_KEY, DeviceStats::bootstrap);
		registryBuilder.add(EdibleBundle.REGISTRY_KEY, EdibleBundle::bootstrap);
		registryBuilder.add(GreenOut.REGISTRY_KEY, GreenOut::bootstrap);
		registryBuilder.add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap);
		registryBuilder.add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap);
	}
}

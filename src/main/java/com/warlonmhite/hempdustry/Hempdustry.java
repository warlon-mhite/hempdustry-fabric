package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hempdustry implements ModInitializer {
	public static final String MOD_ID = "hempdustry";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModSounds.registerSounds();

		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_BRICK, 0.05f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.INDICA_SEEDS, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_FIBER, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMPCRETE, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_STEM, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.INDICA_BUDS, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.SATIVA_BUDS, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_BEANNIE, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_SHIRT, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_HAREM_PANTS, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.FLIP_FLOPS, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModBlocks.HEMPCRETE_POWDER_BLOCK, 0.85f);

		FuelRegistry.INSTANCE.add(ModItems.HEMP_STEM, 50);
		FuelRegistry.INSTANCE.add(ModItems.HEMPCRETE, 200);
		FuelRegistry.INSTANCE.add(ModItems.HEMP_HAREM_PANTS, 350);
		FuelRegistry.INSTANCE.add(ModItems.HEMP_BEANNIE, 250);
		FuelRegistry.INSTANCE.add(ModItems.HEMP_SHIRT, 400);
		FuelRegistry.INSTANCE.add(ModItems.FLIP_FLOPS, 200);
		FuelRegistry.INSTANCE.add(ModBlocks.HEMPCRETE_POWDER_BLOCK, 1800);
		FuelRegistry.INSTANCE.add(ModBlocks.HEMP_BALE, 450);

    }
}
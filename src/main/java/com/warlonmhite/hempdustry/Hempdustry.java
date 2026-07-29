package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.advancement.ModCriteria;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.custom.SmokeScheduler;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.loot.ModLootTableModifiers;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.sound.ModSounds;
import com.warlonmhite.hempdustry.world.gen.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hempdustry implements ModInitializer {
	public static final String MOD_ID = "hempdustry";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModComponents.registerModComponents();
		ModCriteria.registerCriteria();
		ModRecipes.registerRecipes();
		SmokeScheduler.init();
		ModLootTableModifiers.modifyLootTables();
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModEntities.registerModEntities();
		ModSounds.registerSounds();
		ModWorldGeneration.generateModWorldGeneration();

		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_BRICK, 0.05f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.INDICA_SEEDS, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.SATIVA_SEEDS, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_FIBER, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMPCRETE, 0.3f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_STEM, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.INDICA_BUDS, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.SATIVA_BUDS, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_BEANNIE, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_SHIRT, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.HEMP_HAREM_PANTS, 0.5f);
		CompostingChanceRegistry.INSTANCE.add(ModItems.FLIP_FLOPS, 0.5f);
		// 0.65 is what vanilla gives every small flower; ours are plants too and had been missed.
		CompostingChanceRegistry.INSTANCE.add(ModBlocks.INDICA_FLOWER, 0.65f);
		CompostingChanceRegistry.INSTANCE.add(ModBlocks.SATIVA_FLOWER, 0.65f);
		CompostingChanceRegistry.INSTANCE.add(ModBlocks.HEMPCRETE_POWDER_BLOCK, 0.85f);

		FuelRegistry.INSTANCE.add(ModItems.HEMP_STEM, 50);
		FuelRegistry.INSTANCE.add(ModItems.HEMPCRETE, 200);
		FuelRegistry.INSTANCE.add(ModItems.HEMP_HAREM_PANTS, 350);
		FuelRegistry.INSTANCE.add(ModItems.HEMP_BEANNIE, 250);
		FuelRegistry.INSTANCE.add(ModItems.HEMP_SHIRT, 400);
		FuelRegistry.INSTANCE.add(ModItems.FLIP_FLOPS, 200);
		FuelRegistry.INSTANCE.add(ModBlocks.HEMPCRETE_POWDER_BLOCK, 1800);
		FuelRegistry.INSTANCE.add(ModBlocks.HEMP_BALE, 450);

		registerFlammables();

		((FabricBlockEntityType) BlockEntityType.SIGN).addSupportedBlock(ModBlocks.HEMP_PLANKS_SIGN);
		((FabricBlockEntityType) BlockEntityType.SIGN).addSupportedBlock(ModBlocks.HEMP_PLANKS_WALL_SIGN);
		((FabricBlockEntityType) BlockEntityType.HANGING_SIGN).addSupportedBlock(ModBlocks.HEMP_PLANKS_HANGING_SIGN);
		((FabricBlockEntityType) BlockEntityType.HANGING_SIGN).addSupportedBlock(ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN);
	}

	private static void registerFlammables() {
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.INDICA_CROP, 10, 25);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.INDICA_FLOWER, 20, 40);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.SATIVA_CROP, 10, 25);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.SATIVA_FLOWER, 20, 40);
		FlammableBlockRegistry.getDefaultInstance().add(ModBlocks.HEMP_BALE, 50, 10);
	}

}

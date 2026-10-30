package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.advancement.ModCriteria;
import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.balance.ModBalance;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.ModCauldronBehaviors;
import com.warlonmhite.hempdustry.block.ModDispenserBehaviors;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.config.ModCommands;
import com.warlonmhite.hempdustry.platform.Registration;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import com.warlonmhite.hempdustry.item.custom.JellyLegs;
import com.warlonmhite.hempdustry.item.custom.SmokeScheduler;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.loot.ModLootEntryTypes;
import com.warlonmhite.hempdustry.loot.ModLootTableModifiers;
import com.warlonmhite.hempdustry.mixin.PointOfInterestTypesAccessor;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.sound.ModSounds;
import com.warlonmhite.hempdustry.particle.ModParticles;
import com.warlonmhite.hempdustry.world.ModPlacementModifiers;
import com.warlonmhite.hempdustry.world.gen.ModWorldGeneration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class Hempdustry {
	public static final String MOD_ID = "hempdustry";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * The datapack registries: the strains, and the balance files a pack can tune. A loader may need
	 * them before anything else is registered -- NeoForge asks for them before its registry events --
	 * so each loader's entry point calls this first, then {@link #init}.
	 *
	 * <p>All four are <b>synced</b>, so a datapack's entries reach every client without a packet of
	 * our own: the tinting and the naming read the loaded strain, and the creative tab and the recipe
	 * viewers show the bowls the server actually packs. No {@code SKIP_WHEN_EMPTY}: the mod's own
	 * entries are always present, and a client without the registry is a client without the mod.
	 */
	public static void registerDynamicRegistries() {
		Registration registration = Services.REGISTRATION;
		registration.registerSyncedDynamicRegistry(Strain.REGISTRY_KEY, Strain.CODEC);
		registration.registerSyncedDynamicRegistry(DeviceStats.REGISTRY_KEY, DeviceStats.CODEC);
		registration.registerSyncedDynamicRegistry(EdibleBundle.REGISTRY_KEY, EdibleBundle.CODEC);
		registration.registerSyncedDynamicRegistry(GreenOut.REGISTRY_KEY, GreenOut.CODEC);
	}

	/** Registers everything else, in order; each loader's entry point calls it once. */
	public static void init() {
		Registration registration = Services.REGISTRATION;
		// The config first: everything below can read it, and a broken file must not stop the mod.
		HempdustryConfig.load();
		ModCommands.registerCommands();
		ModComponents.registerModComponents();
		ModStrains.registerStrains();
		ModBalance.registerBalance();
		ModCriteria.registerCriteria();
		ModRecipes.registerRecipes();
		SmokeScheduler.init();
		EdibleScheduler.init();
		JellyLegs.init();
		ModLootTableModifiers.modifyLootTables();
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		ModScreenHandlers.registerScreenHandlers();
		ModCauldronBehaviors.registerCauldronBehaviors();
		ModDispenserBehaviors.registerDispenserBehaviors();
		ModEntities.registerModEntities();
		ModSounds.registerSounds();
		ModParticles.registerParticles();
		ModPlacementModifiers.registerPlacementModifiers();
		ModLootEntryTypes.registerLootEntryTypes();
		ModWorldGeneration.generateModWorldGeneration();

		registration.compostable(ModItems.HEMP_BRICK, 0.05f);
		registration.compostable(ModItems.INDICA_SEEDS, 0.3f);
		registration.compostable(ModItems.SATIVA_SEEDS, 0.3f);
		registration.compostable(ModItems.BELDIA_SEEDS, 0.3f);
		registration.compostable(ModItems.HEMP_FIBER, 0.3f);
		// Woven cloth, so it sits with the armour at 0.5 rather than with the loose fibre at 0.3.
		registration.compostable(ModItems.HEMP_CANVAS, 0.5f);
		registration.compostable(ModItems.HEMPCRETE, 0.3f);
		registration.compostable(ModItems.HEMP_LEAF, 0.3f);
		registration.compostable(ModItems.HEMP_STEM, 0.5f);
		registration.compostable(ModItems.RETTED_HEMP_STEM, 0.5f);
		registration.compostable(ModItems.INDICA_BUDS, 0.5f);
		registration.compostable(ModItems.SATIVA_BUDS, 0.5f);
		registration.compostable(ModItems.BELDIA_BUDS, 0.5f);
		// The two cannabutter-chain intermediates. Composting them is a ridiculous use of a fuelled
		// oven, which is exactly why it should be allowed: everything else organic in the mod goes in
		// the composter, and a lone pair of items that bounce off it reads as an oversight. They sit
		// at the same 0.5 as the buds and leaf they are made from — dried plant matter, like vanilla's
		// dried kelp block.
		registration.compostable(ModItems.DECARBOXYLATED_HEMP, 0.5f);
		registration.compostable(ModItems.WASHED_DECARBOXYLATED_HEMP, 0.5f);
		// Burnt down to less than it was, so it drops to the loose leaf's 0.3 -- vanilla's dried kelp
		// item rate. This is also its vanilla-shaped way out: nothing else consumes it but the tub.
		registration.compostable(ModItems.SCORCHED_HEMP, 0.3f);
		// A bud's rate: vanilla prices compost by what a thing physically is, never by what it is
		// worth, and schwag is a bud. Vanilla's poisonous potato does not compost at all, which reads
		// as an oversight rather than a rule -- every plant thing in this mod composts.
		registration.compostable(ModItems.SCHWAG, 0.5f);
		registration.compostable(ModItems.HEMP_BEANIE, 0.5f);
		registration.compostable(ModItems.HEMP_SHIRT, 0.5f);
		registration.compostable(ModItems.HEMP_HAREM_PANTS, 0.5f);
		registration.compostable(ModItems.FLIP_FLOPS, 0.5f);
		// 0.65 is what vanilla gives every small flower; ours are plants too and had been missed.
		registration.compostable(ModBlocks.INDICA_FLOWER, 0.65f);
		registration.compostable(ModBlocks.SATIVA_FLOWER, 0.65f);
		registration.compostable(ModBlocks.HEMPCRETE_POWDER_BLOCK, 0.85f);
		// The two storage blocks. Vanilla puts every compacted plant block a tier above the item it
		// is made from -- dried kelp 0.3 to the dried kelp block's 0.5, wheat 0.65 to hay's 0.85 --
		// so the bale sits at hay's 0.85 and the leaf block at the dried kelp block's 0.5. The leaf
		// block is not a tree's leaves, which are 0.3 wherever they appear: it is nine leaves
		// pressed together, which is the dried kelp block's case exactly. The bale had simply been
		// missed: hay_block is 0.85 in the 1.21.11 bytecode.
		//
		// Neither is an exploit in either direction, which is why the numbers can just follow
		// vanilla. Composting nine stems at 0.5 each is worth far more than one bale at 0.85, and
		// nine leaves at 0.3 each far more than one leaf block at 0.5 -- exactly as nine wheat beat
		// a hay bale in vanilla. Compacting to compost is always the worse trade.
		registration.compostable(ModBlocks.HEMP_BALE, 0.85f);
		registration.compostable(ModBlocks.HEMP_LEAVES, 0.5f);

		// Fuel is a build-time event since 1.21.2 rather than a registry you push entries into, so
		// each loader hands these over whenever it builds the fuel table.
		registration.fuel(ModItems.HEMP_STEM, 50);
		registration.fuel(ModItems.RETTED_HEMP_STEM, 50);
		// Hempcrete is crushed stem, so it burns as the stem does; set in lime it is a building
		// material, and the powder block, like vanilla's concrete powder, is no fuel at all.
		registration.fuel(ModItems.HEMPCRETE, 50);
		registration.fuel(ModItems.HEMP_HAREM_PANTS, 350);
		registration.fuel(ModItems.HEMP_BEANIE, 250);
		registration.fuel(ModItems.HEMP_SHIRT, 400);
		registration.fuel(ModItems.FLIP_FLOPS, 200);
		registration.fuel(ModBlocks.HEMP_BALE, 450);
		// 100 ticks, vanilla's rate for #minecraft:wool. Granted directly because hemp wool
		// deliberately stays out of that tag — see ModBlocks.HEMP_WOOL.
		registration.fuel(ModBlocks.HEMP_WOOL, 100);

		registerFlammables(registration);

		registration.addValidBlocks(BlockEntityTypes.SIGN, ModBlocks.HEMP_PLANKS_SIGN);
		registration.addValidBlocks(BlockEntityTypes.SIGN, ModBlocks.HEMP_PLANKS_WALL_SIGN);
		registration.addValidBlocks(BlockEntityTypes.HANGING_SIGN, ModBlocks.HEMP_PLANKS_HANGING_SIGN);
		registration.addValidBlocks(BlockEntityTypes.HANGING_SIGN, ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN);
		// The shelf is vanilla's block and keeps vanilla's block entity, and so its renderer.
		registration.addValidBlocks(BlockEntityTypes.SHELF, ModBlocks.HEMP_PLANKS_SHELF);

		// A villager's home is any bed head, the way vanilla lists its own sixteen.
		PointOfInterestTypesAccessor.hempdustry$registerStates(
				BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(PoiTypes.HOME),
				ModBlocks.HEMP_BED.getStateDefinition().getPossibleStates().stream()
						.filter(state -> state.getValue(BedBlock.PART) == BedPart.HEAD)
						.collect(Collectors.toUnmodifiableSet()));
	}

	private static void registerFlammables(Registration registration) {
		registration.flammable(ModBlocks.INDICA_CROP, 10, 25);
		registration.flammable(ModBlocks.INDICA_FLOWER, 20, 40);
		registration.flammable(ModBlocks.SATIVA_CROP, 10, 25);
		registration.flammable(ModBlocks.BELDIA_CROP, 10, 25);
		registration.flammable(ModBlocks.SATIVA_FLOWER, 20, 40);
		registration.flammable(ModBlocks.HEMP_BALE, 50, 10);
		// Vanilla leaves parity, exactly (30/60). Nothing about hemp foliage argues for a
		// different number than oak foliage, and a leaf block that burns unlike every other
		// leaf block would read as a bug.
		registration.flammable(ModBlocks.HEMP_LEAVES, 30, 60);
		// Hemp cloth burns *more* readily than the sheep wool it stands in for, not less. Sheep
		// wool is the outlier among natural fibres — high nitrogen and moisture content, LOI ~25%,
		// self-extinguishing, which is why it is what firefighter base layers and aircraft carpet
		// are made of. Cellulosic fibres (hemp, flax, cotton) sit around LOI 18-19% and sustain a
		// flame; commercial hemp insulation batts are dosed with fire retardants precisely because
		// the raw fibre doesn't need help catching.
		//
		// So 60 burn chance rather than wool's 30 — the tier vanilla gives grass, hay, carpets and
		// scaffolding — with wool's own 60 spread. That exact pair is vanilla's (scaffolding,
		// bamboo), so nothing here is invented. Note hempcrete's real Euroclass B-s1,d0 rating does
		// NOT transfer: that is hurds in a lime binder, a mineral composite, not a textile.
		registration.flammable(ModBlocks.HEMP_WOOL, 60, 60);
		// Vanilla carpet parity (60/20), because vanilla already puts carpets at the top burn
		// chance and there is no headroom left to say "more flammable still" — raising the spread
		// to 60 would turn carpets into a fire highway, which is a gameplay change rather than a
		// material one.
		registration.flammable(ModBlocks.HEMP_CARPET, 60, 20);
		// No vanilla bed burns. This one does, at the rate of the hemp wool it is made from: most of
		// a bed is its cloth, and hemp cloth is the one hemp material that catches.
		registration.flammable(ModBlocks.HEMP_BED, 60, 60);
	}

}

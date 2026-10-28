package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.BongBlock;
import com.warlonmhite.hempdustry.item.ModItemProperties;
import com.warlonmhite.hempdustry.item.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.BlockFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.warlonmhite.hempdustry.client.item.StrainModelIndexProperty;
import com.warlonmhite.hempdustry.client.item.StrainTintSource;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModArmorMaterials;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.joml.Vector3f;

public class ModModelProvider extends FabricModelProvider {
    /**
     * The loaded strain registry, needed to tell a plant strain from a hash one.
     *
     * <p>{@code FabricModelProvider} takes only an output, so this arrives through
     * {@code Pack.RegistryDependentFactory} and is joined at generate time. Fabric resolves the
     * future before any provider runs, so the join never blocks.
     */
    private final CompletableFuture<HolderLookup.Provider> registryLookup;

    public ModModelProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output);
        this.registryLookup = registryLookup;
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        BlockModelGenerators.BlockFamilyProvider hempBricksPool = blockStateModelGenerator.family(ModBlocks.HEMP_BRICKS_BLOCK);
        BlockModelGenerators.BlockFamilyProvider hempPlanksPool = blockStateModelGenerator.family(ModBlocks.HEMP_PLANKS);
        // The signs, as vanilla makes its own since 26.2: block models on the sign templates, every
        // rotation and the wall form, from block/hemp_planks_sign and block/hemp_planks_hanging_sign
        // (the old entity sheets cut the way vanilla cut its woods'), and the flat items. The hanging
        // sign is the custom kind, its particle the planks: hemp has no stripped log to take it from.
        hempPlanksPool.generateFor(new BlockFamily.Builder(ModBlocks.HEMP_PLANKS)
                .sign(ModBlocks.HEMP_PLANKS_SIGN, ModBlocks.HEMP_PLANKS_WALL_SIGN)
                .customHangingSign(ModBlocks.HEMP_PLANKS_HANGING_SIGN, ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN)
                .getFamily());
        blockStateModelGenerator.createTrivialCube(ModBlocks.HEMPCRETE_BLOCK);
        blockStateModelGenerator.createTrivialCube(ModBlocks.HEMPCRETE_POWDER_BLOCK);
        // Vanilla's own wool+carpet helper — the carpet model reuses the wool texture, same as
        // every vanilla carpet does, so hemp_carpet needs no art of its own.
        blockStateModelGenerator.createFullAndCarpetBlocks(ModBlocks.HEMP_WOOL, ModBlocks.HEMP_CARPET);

        hempBricksPool.stairs(ModBlocks.HEMP_BRICKS_STAIRS);
        hempBricksPool.slab(ModBlocks.HEMP_BRICKS_SLAB);
        hempBricksPool.wall(ModBlocks.HEMP_BRICKS_WALL);

        hempPlanksPool.stairs(ModBlocks.HEMP_PLANKS_STAIRS);
        hempPlanksPool.slab(ModBlocks.HEMP_PLANKS_SLAB);
        hempPlanksPool.button(ModBlocks.HEMP_PLANKS_BUTTON);
        hempPlanksPool.pressurePlate(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE);
        hempPlanksPool.fence(ModBlocks.HEMP_PLANKS_FENCE);
        hempPlanksPool.fenceGate(ModBlocks.HEMP_PLANKS_FENCE_GATE);

        registerBongBlocks(blockStateModelGenerator);

        blockStateModelGenerator.createDoor(ModBlocks.HEMP_PLANKS_DOOR);
        blockStateModelGenerator.createTrapdoor(ModBlocks.HEMP_PLANKS_TRAPDOOR);
        // Vanilla's shelf helper; the planks stand in for the stripped log it takes its break
        // particles from, as they do in the recipe.
        blockStateModelGenerator.createShelf(ModBlocks.HEMP_PLANKS_SHELF, ModBlocks.HEMP_PLANKS);

        blockStateModelGenerator.family(ModBlocks.HEMP_PARQUET)
                .stairs(ModBlocks.HEMP_PARQUET_STAIRS)
                .slab(ModBlocks.HEMP_PARQUET_SLAB);

        registerHempBed(blockStateModelGenerator);

        blockStateModelGenerator.woodProvider(ModBlocks.HEMP_BALE).logWithHorizontal(ModBlocks.HEMP_BALE);
        // TexturedModel.LEAVES, not createTrivialCube: it parents to minecraft:block/leaves,
        // which is cube_all with ambient occlusion off -- the reason vanilla foliage does not
        // shade itself into a dark blob.
        blockStateModelGenerator.createTrivialBlock(ModBlocks.HEMP_LEAVES, TexturedModel.LEAVES);

        registerSpaceCake(blockStateModelGenerator);

        // TINTED, not NOT_TINTED: both wild flowers take the biome tint that HempdustryClient
        // registers, the same as the crops do. TintType only decides whether the generated models
        // carry "tintindex": 0 — vanilla's own ferns are potted through the tinted pair, so a
        // potted wild flower picking up the room's biome is what a player already expects.
        blockStateModelGenerator.createPlantWithDefaultItem(ModBlocks.INDICA_FLOWER, ModBlocks.POTTED_INDICA_FLOWER, BlockModelGenerators.PlantType.TINTED);
        registerTallFlowerPotPlant(blockStateModelGenerator, ModBlocks.SATIVA_FLOWER, ModBlocks.POTTED_SATIVA_FLOWER);

        // The crops' blockstates and stage models are hand-written under resources/ — the model
        // generator has no notion of a two- or three-tall crop, so INDICA_CROP and SATIVA_CROP
        // are deliberately absent here.
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerator) {
        itemModelGenerator.generateFlatItem(ModItems.CANNABUTTER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_BRICK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_FIBER, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_CANVAS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_FLOUR, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_STEM, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.RETTED_HEMP_STEM, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_LEAF, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DECARBOXYLATED_HEMP, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.WASHED_DECARBOXYLATED_HEMP, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SCORCHED_HEMP, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SCHWAG, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.KIEF, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.FILTERED_KIEF, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BUBBLE_HASH, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HASHISH, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.CHARAS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.FILTERED_HASHISH, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.ROSIN, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMPCRETE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.TOASTED_HEMP_SEEDS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_FLAPJACK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_MILK_BUCKET, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SIEMIENIOTKA, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.CANNABUTTER_TOAST, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SPACE_COOKIE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SPACE_BROWNIE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BHANG_BUCKET, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.DAWAMESK, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.INDICA_SEEDS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.INDICA_BUDS, ModelTemplates.FLAT_ITEM);
        for (Item disc : ModItems.MUSIC_DISCS) {
            itemModelGenerator.generateFlatItem(disc, ModelTemplates.FLAT_ITEM);
        }

        itemModelGenerator.generateFlatItem(ModItems.SATIVA_BUDS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.SATIVA_SEEDS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BELDIA_SEEDS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.BELDIA_BUDS, ModelTemplates.FLAT_ITEM);
        // Smoking gear. One item per device carries every strain in a component, so the visual
        // per-strain split lives in the *client item definition* rather than in separate items —
        // exactly how vanilla varies a bow by "pulling" or a crossbow by "charged".
        //
        // TWO questions, answered by two different mechanisms (see ModItemProperties):
        //
        //   is anything loaded?   minecraft:has_component on hempdustry:smoke_contents. Vanilla's,
        //                         since 1.21.4 — the mod's old hempdustry:packed property is gone.
        //   whose art?            hempdustry:strain, a numeric property carrying the strain's
        //                         model_index, dispatched on with >= thresholds listed ascending.
        //
        // Driven off ModStrains.BUILT_IN rather than the loaded registry, and that is the honest
        // boundary: bespoke art exists only for the strains the mod itself carries.
        //
        // Plant strains only. A hash-family strain has no bespoke spliff art and never will: a pure
        // hash spliff cannot be rolled (a joint needs something to burn), and a hash spliff's art is
        // the plant's, because the plant is the primary entry. Generating one would upload a model
        // pointing at a texture that does not exist. The devices are unaffected -- their packed art
        // is shared and tinted by the strain's colour, which is exactly what a hash entry wants.
        List<ResourceKey<Strain>> plantStrains = plantStrains();
        for (ResourceKey<Strain> strain : plantStrains) {
            uploadGenerated(itemModelGenerator, spliffModel(strain), texture(ModStrains.id(strain) + "_spliff"));
        }
        List<RangeSelectItemModel.Entry> spliffArt = new ArrayList<>();
        for (ResourceKey<Strain> strain : plantStrains) {
            spliffArt.add(ItemModelUtils.override(
                    ItemModelUtils.plainModel(spliffModel(strain)), ModStrains.modelIndex(strain)));
        }
        // The spliff's FALLBACK is what a strain with no bespoke art falls back to — which is every
        // strain a datapack can add, since a datapack cannot ship a texture. It is therefore two
        // layers: the shared roll, plus a mask of the lit tip that the strain's colour tints.
        //
        // layer0 is indica's art rather than a third drawing, and that is derivation not laziness:
        // both shipped spliffs are the same matrix under two palettes (see textures-src/sativa.mctex),
        // and of the 49 opaque pixels only 7 differ strongly between them -- the tip. The other 42
        // are the roll, where indica's white paper is the neutral of the two. So the tip becomes the
        // tinted layer and the roll stays as drawn.
        //
        // The per-strain entries above still win for indica and sativa, which keep their own art
        // untinted. Dropping those entries would put every strain on this tinted base instead;
        // that is a texture decision, not a code one, and it costs one line here when wanted.
        Identifier spliffTinted = ModelTemplates.TWO_LAYERED_ITEM.create(
                Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/spliff_tinted"),
                TextureMapping.layered(texture(ModStrains.id(plantStrains.get(0)) + "_spliff"),
                        texture("spliff_load")),
                itemModelGenerator.modelOutput);
        // A range picks the highest threshold at or below the strain's index, so without a way back
        // every index above a shipped one would wear that strain's art: the hash family's 3-6 and
        // anything past Beldía's 7. Right after each piece of art, the index above goes back to the
        // tinted look — unless it is another strain's art.
        Set<Integer> shipped = new HashSet<>();
        plantStrains.forEach(strain -> shipped.add(ModStrains.modelIndex(strain)));
        for (int index : shipped) {
            if (!shipped.contains(index + 1)) {
                spliffArt.add(ItemModelUtils.override(strainTinted(spliffTinted), index + 1));
            }
        }
        spliffArt.sort(Comparator.comparingDouble(RangeSelectItemModel.Entry::threshold));
        itemModelGenerator.itemModelOutput.accept(ModItems.SPLIFF, ItemModelUtils.rangeSelect(
                new StrainModelIndexProperty(), strainTinted(spliffTinted), spliffArt));

        // The devices share one packed texture, which they always did. The switch is on "packed at
        // all", so giving a strain its own packed art later is a range dispatch inside the packed
        // branch — plus the PNG.
        // A packed device is the empty device plus a mask of what is in the bowl, and the mask is the
        // layer the strain's colour tints. Two layers rather than a second full texture, because a
        // flat item model hands layerN the tint index N, and the tints array can then colour index 1
        // while leaving the wood and glass of index 0 alone — see ModItemProperties.LOAD_TINT_INDEX.
        //
        // This is the half of the strain system a datapack can actually reach. Bespoke art needs a
        // texture and a datapack cannot ship one, so before this a datapack's strain packed a device
        // that looked exactly like every other strain's.
        for (Map.Entry<DeviceType, Item> deviceEntry : ModItems.devices().entrySet()) {
            DeviceType device = deviceEntry.getKey();
            registerDevice(itemModelGenerator, deviceEntry.getValue(), device.baseName(),
                    device.packedModel(), device.packedModel() + "_load");
        }
        // The coloured bongs: the clear bong's art in other glass (textures-src/bong_glass.py), and
        // the clear bong's load mask, since what sits in the bowl does not care what it is blown from.
        for (Item bong : ModItems.COLORED_BONGS) {
            String name = BuiltInRegistries.ITEM.getKey(bong).getPath();
            registerDevice(itemModelGenerator, bong, name, "packed_" + name,
                    DeviceType.BONG.packedModel() + "_load");
        }

        // The moon rock: one item for every strain, tinted by whichever one is stuck to it. Two
        // layers, exactly as a packed device is -- the hashish crust is the same on all of them and
        // the bud showing through it is not.
        Identifier moonRock = ModelTemplates.TWO_LAYERED_ITEM.create(
                ModelLocationUtils.getModelLocation(ModItems.MOON_ROCK),
                TextureMapping.layered(texture("moon_rock"), texture("moon_rock_load")),
                itemModelGenerator.modelOutput);
        itemModelGenerator.itemModelOutput.accept(ModItems.MOON_ROCK, strainTinted(moonRock));

        itemModelGenerator.generateFlatItem(ModItems.HEMP_HARNESS, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_BOAT, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_CHEST_BOAT, ModelTemplates.FLAT_ITEM);

        // The beanie's icon is the flat sprite everywhere but on a head, where it is the slouchy tam:
        // a hand-written model (models/item/hemp_beanie_worn.json), since it is made of cubes. A head
        // item with no equipment model is drawn in the HEAD display context, as a carved pumpkin is.
        Identifier beanieIcon = ModelTemplates.FLAT_ITEM.create(ModItems.HEMP_BEANIE,
                TextureMapping.layer0(ModItems.HEMP_BEANIE), itemModelGenerator.modelOutput);
        itemModelGenerator.itemModelOutput.accept(ModItems.HEMP_BEANIE, ItemModelUtils.select(new DisplayContext(),
                ItemModelUtils.plainModel(beanieIcon), ItemModelUtils.when(ItemDisplayContext.HEAD,
                        ItemModelUtils.plainModel(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/hemp_beanie_worn")))));
        registerDyeableArmor(itemModelGenerator, ModItems.HEMP_SHIRT, ItemModelGenerators.TRIM_PREFIX_CHESTPLATE);
        registerDyeableArmor(itemModelGenerator, ModItems.HEMP_HAREM_PANTS, ItemModelGenerators.TRIM_PREFIX_LEGGINGS);
        // Worn, the flip-flops are FlipFlopsModel's; the icon is a plain sprite, untrimmable.
        itemModelGenerator.generateFlatItem(ModItems.FLIP_FLOPS, ModelTemplates.FLAT_ITEM);
    }

    /**
     * {@code generateTrimmableItem(…, true)}, the way leather is drawn, with two differences. Vanilla's
     * hard-codes leather's brown as the undyed tint, so an undyed shirt would come out brown; this
     * takes {@link ModArmorMaterials#HEMP_UNDYED_COLOR}. And the layers are named the other way
     * round: vanilla tints {@code item/<id>} and lays {@code item/<id>_overlay} on top, while here the
     * grey cloth is the new {@code item/<id>_dyeable} and {@code item/<id>} — the name a resource
     * pack already redraws — stays the untinted layer on top. A pack that redrew the whole garment
     * then still shows its own art, undyed, instead of having it tinted beige.
     */
    private static void registerDyeableArmor(ItemModelGenerators generator, Item item, Identifier trimPrefix) {
        Identifier model = ModelLocationUtils.getModelLocation(item);
        Material cloth = TextureMapping.getItemTexture(item, "_dyeable");
        Material drawn = TextureMapping.getItemTexture(item);
        Dye dye = new Dye(ModArmorMaterials.HEMP_UNDYED_COLOR);
        List<SelectItemModel.SwitchCase<ResourceKey<TrimMaterial>>> trims = new ArrayList<>();
        for (ItemModelGenerators.TrimMaterialData trim : ItemModelGenerators.TRIM_MATERIAL_MODELS) {
            Identifier trimmed = model.withSuffix("_" + trim.assets().base().suffix() + "_trim");
            Material trimTexture = new Material(trimPrefix.withSuffix(
                    "_" + trim.assets().assetId(ModArmorMaterials.HEMP_EQUIPMENT_ASSET).suffix()));
            ModelTemplates.THREE_LAYERED_ITEM.create(trimmed, TextureMapping.layered(cloth, drawn, trimTexture),
                    generator.modelOutput);
            trims.add(ItemModelUtils.when(trim.materialKey(), ItemModelUtils.tintedModel(trimmed, dye)));
        }
        Identifier plain = ModelTemplates.TWO_LAYERED_ITEM.create(model, TextureMapping.layered(cloth, drawn),
                generator.modelOutput);
        generator.itemModelOutput.accept(item, ItemModelUtils.select(new TrimMaterialProperty(), ItemModelUtils.tintedModel(plain, dye), trims));
    }

    /**
     * The built-in strains that grew on a plant, in {@link ModStrains#BUILT_IN} order.
     *
     * <p>{@code wildFlower().isPresent()} is the mod-wide predicate for "this grew on a plant" — the
     * same one the creative tab, the seed pools, the siftable tag and the spliff recipes key on —
     * so anything hash-shaped added later is excluded here for free.
     */
    private List<ResourceKey<Strain>> plantStrains() {
        HolderLookup.RegistryLookup<Strain> strains = Strain.registry(registryLookup.join());
        return ModStrains.BUILT_IN.stream()
                .filter(key -> strains.getOrThrow(key).value().wildFlower().isPresent())
                .toList();
    }

    /**
     * A two-layer model whose second layer takes the loaded strain's colour.
     *
     * <p>The tints array answers <b>by position</b>: entry 0 is tint index 0 ({@code layer0}, the
     * object as drawn, so a constant white multiplies to no change) and entry 1 is tint index 1
     * ({@code layer1}, the load mask). See {@link ModItemProperties#LOAD_TINT_INDEX}.
     */
    private static ItemModel.Unbaked strainTinted(Identifier model) {
        return ItemModelUtils.tintedModel(model,
                ItemModelUtils.constantTint(NO_TINT),
                new StrainTintSource(NO_TINT));
    }

    /**
     * A device's empty model, its packed one (the art plus a strain-tinted load), and the switch.
     *
     * <p>A device that stands as a block ({@link ModBlocks#DEVICE_BLOCKS}) is also 3D in the hand:
     * vanilla's trident/spyglass split, the flat sprite in the inventory, on the ground, in a frame
     * and on a shelf, and the placed block's own model everywhere else. Packed, the hand model is
     * the same bong with a strain-tinted load sitting in the bowl.
     */
    private static void registerDevice(ItemModelGenerators generator, Item item, String art,
                                       String packedModelName, String loadTexture) {
        Identifier packedModel = ModelTemplates.TWO_LAYERED_ITEM.create(
                Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/" + packedModelName),
                TextureMapping.layered(texture(art), texture(loadTexture)),
                generator.modelOutput);
        Identifier emptyModel = ModelTemplates.FLAT_ITEM.create(
                ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(texture(art)),
                generator.modelOutput);
        ItemModel.Unbaked flat = ItemModelUtils.conditional(
                ItemModelUtils.hasComponent(ModComponents.SMOKE_CONTENTS),
                strainTinted(packedModel), ItemModelUtils.plainModel(emptyModel));
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (!ModBlocks.DEVICE_BLOCKS.containsKey(name)) {
            generator.itemModelOutput.accept(item, flat);
            return;
        }
        // The placed packed bong's own model (registerBongBlocks), so the hand and the table show
        // the same bowl -- tinted by the strain here, where there is a stack to ask.
        ItemModel.Unbaked packedInHand = strainTinted(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/" + name + "_packed"));
        // The draw, in the bow's grammar: while it is being used, the bong steps through the
        // stages on how far into the draw it is -- smoke gathering over the water, then climbing
        // the neck -- exactly as a bow steps through pulling_0..2. Thresholds at a quarter, a half
        // and three quarters of the draw; the scale turns ticks into that fraction.
        RangeSelectItemModel.Entry[] draw = new RangeSelectItemModel.Entry[BONG_DRAW_TEMPLATES.size()];
        for (int stage = 1; stage <= draw.length; stage++) {
            Identifier model = BONG_DRAW_TEMPLATES.get(stage - 1).create(
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/" + packedModelName + "_drawing_" + stage),
                    bongTextures(name), generator.modelOutput);
            draw[stage - 1] = ItemModelUtils.override(strainTinted(model), stage / (draw.length + 1f));
        }
        ItemModel.Unbaked inHand = ItemModelUtils.conditional(
                ItemModelUtils.hasComponent(ModComponents.SMOKE_CONTENTS),
                ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
                        ItemModelUtils.rangeSelect(new UseDuration(false),
                                1f / DeviceType.BONG.drawTicks(), packedInHand, draw),
                        packedInHand),
                ItemModelUtils.plainModel(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/" + name)));
        generator.itemModelOutput.accept(item, ItemModelGenerators.createFlatModelDispatch(flat, inHand));
    }

    // The placed bong, hand-written once as block/bong_template.json and re-textured per glass.
    private static final TextureSlot BONG_SHEET = TextureSlot.create("bong");
    private static final ModelTemplate BONG_TEMPLATE = new ModelTemplate(
            Optional.of(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/bong_template")), Optional.empty(), BONG_SHEET);
    private static final ModelTemplate BONG_PACKED_TEMPLATE = new ModelTemplate(
            Optional.of(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/bong_template_packed")), Optional.empty(), BONG_SHEET);
    private static final List<ModelTemplate> BONG_DRAW_TEMPLATES = java.util.stream.IntStream.rangeClosed(1, 3)
            .mapToObj(stage -> new ModelTemplate(Optional.of(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID,
                    "item/bong_template_draw_" + stage)), Optional.empty(), BONG_SHEET))
            .toList();

    private static TextureMapping bongTextures(String name) {
        return new TextureMapping().put(BONG_SHEET, new Material(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/" + name)));
    }

    /**
     * One model per glass and a facing blockstate. North-default, rotated the way every horizontal
     * block of vanilla's is, with the downstem to the east of the direction it faces.
     */
    private static void registerBongBlocks(BlockModelGenerators generator) {
        var facing = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
                .select(Direction.NORTH, BlockModelGenerators.NOP)
                .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
                .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                .select(Direction.WEST, BlockModelGenerators.Y_ROT_270);
        for (Map.Entry<String, Block> entry : ModBlocks.DEVICE_BLOCKS.entrySet()) {
            Block block = entry.getValue();
            Identifier model = BONG_TEMPLATE.create(block, bongTextures(entry.getKey()), generator.modelOutput);
            Identifier packed = BONG_PACKED_TEMPLATE.createWithSuffix(block, "_packed", bongTextures(entry.getKey()),
                    generator.modelOutput);
            generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                    .with(PropertyDispatch.initial(BongBlock.PACKED)
                            .select(false, BlockModelGenerators.plainVariant(model))
                            .select(true, BlockModelGenerators.plainVariant(packed)))
                    .with(facing));
        }
    }

    /** White: a tint is a multiply, so this leaves a layer exactly as it was drawn. */
    private static final int NO_TINT = 0xFFFFFF;

    private static Material texture(String name) {
        return new Material(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/" + name));
    }

    private static Identifier spliffModel(ResourceKey<Strain> strain) {
        return Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/spliff_" + ModStrains.id(strain));
    }

    private static void uploadGenerated(ItemModelGenerators generator, Identifier modelId, Material texture) {
        ModelTemplates.FLAT_ITEM.create(modelId, TextureMapping.layer0(texture), generator.modelOutput);
    }

    /**
     * Space Cake's blockstate and its seven models.
     *
     * <p>Each model is a one-line child of the matching vanilla cake model with only the texture map
     * replaced — model inheritance carries the {@code elements} across, so the bite geometry, the
     * cullfaces and the shrinking hitbox all come from Mojang and cannot drift out of step with them.
     * The vanilla {@link Models} helpers can't express "parent plus textures", so these go straight
     * to the generator's model collector.
     */
    /**
     * Vanilla's {@code createBed}, which only takes a {@code DyeColor}: since 26.2 a bed is a block
     * model, a head and a foot made from the bed templates and seven face textures of its own
     * ({@code block/hemp_bed_<part>_<face>}, cut from the old entity texture exactly as vanilla cut
     * its beds'), turned to each facing; the headboard end and the underside are vanilla's shared
     * {@code bed_head_north} and {@code bed_down}. The item is the two halves laid end to end.
     */
    private static void registerHempBed(BlockModelGenerators generator) {
        Block bed = ModBlocks.HEMP_BED;
        Identifier head = ModelTemplates.BED_HEAD.createWithSuffix(bed, "_" + BedPart.HEAD,
                TextureMapping.bed(bed, BedPart.HEAD), generator.modelOutput);
        Identifier foot = ModelTemplates.BED_FOOT.createWithSuffix(bed, "_" + BedPart.FOOT,
                TextureMapping.bed(bed, BedPart.FOOT), generator.modelOutput);
        MultiVariant headModel = BlockModelGenerators.plainVariant(head);
        MultiVariant footModel = BlockModelGenerators.plainVariant(foot);
        generator.blockStateOutput.accept(MultiVariantGenerator.dispatch(bed).with(
                PropertyDispatch.initial(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.BED_PART)
                        .select(Direction.NORTH, BedPart.HEAD, headModel)
                        .select(Direction.SOUTH, BedPart.HEAD, headModel.with(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.EAST, BedPart.HEAD, headModel.with(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.WEST, BedPart.HEAD, headModel.with(BlockModelGenerators.Y_ROT_270))
                        .select(Direction.NORTH, BedPart.FOOT, footModel)
                        .select(Direction.SOUTH, BedPart.FOOT, footModel.with(BlockModelGenerators.Y_ROT_180))
                        .select(Direction.EAST, BedPart.FOOT, footModel.with(BlockModelGenerators.Y_ROT_90))
                        .select(Direction.WEST, BedPart.FOOT, footModel.with(BlockModelGenerators.Y_ROT_270))));
        Transformation footTransformation = new Transformation(new Vector3f(0.0F, 0.0F, 1.0F), null, null, null);
        generator.itemModelOutput.accept(bed.asItem(), ItemModelUtils.composite(
                ItemModelUtils.plainModel(head), ItemModelUtils.plainModel(foot, footTransformation)));
    }

    private static void registerSpaceCake(BlockModelGenerators generator) {
        PropertyDispatch.C1<MultiVariant, Integer> variants =
                PropertyDispatch.initial(CakeBlock.BITES);
        for (int bites = 0; bites <= CakeBlock.MAX_BITES; bites++) {
            String suffix = bites == 0 ? "" : "_slice" + bites;
            Identifier model = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/space_cake" + suffix);
            uploadRetextured(generator, model, Identifier.withDefaultNamespace("block/cake" + suffix), bites > 0);
            variants.select(bites, BlockModelGenerators.plainVariant(model));
        }
        generator.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(ModBlocks.SPACE_CAKE).with(variants));
        // The item is the whole, uneaten cake, exactly as vanilla's cake item is.
        generator.registerSimpleItemModel(ModBlocks.SPACE_CAKE.asItem(),
                ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(ModBlocks.SPACE_CAKE.asItem()),
                        TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/space_cake"))),
                        generator.modelOutput));
    }

    private static void uploadRetextured(BlockModelGenerators generator, Identifier modelId,
                                         Identifier parent, boolean sliced) {
        generator.modelOutput.accept(modelId, () -> {
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", cakeTexture("side"));
            textures.addProperty("bottom", cakeTexture("bottom"));
            textures.addProperty("top", cakeTexture("top"));
            textures.addProperty("side", cakeTexture("side"));
            if (sliced) {
                textures.addProperty("inside", cakeTexture("inner"));
            }
            JsonObject json = new JsonObject();
            json.addProperty("parent", parent.toString());
            json.add("textures", textures);
            return (JsonElement) json;
        });
    }

    private static String cakeTexture(String face) {
        return Hempdustry.MOD_ID + ":block/space_cake_" + face;
    }


    /**
     * {@code createPlant} for a plant that is two blocks tall, which vanilla has no
     * helper for — nothing in vanilla is both a double plant and pottable.
     *
     * <p>Three of the four pieces are vanilla's own: {@code createDoubleBlock} writes the
     * {@code half=lower|upper} blockstate, and both halves upload as {@code TINTED_CROSS} so the
     * biome tint in {@code HempdustryClient} has a {@code tintindex} to land on.
     *
     * <p><b>The item and the flower pot deliberately take the BOTTOM texture.</b> Vanilla's
     * {@code createDoublePlant(block, PlantType)} would hand both the {@code _top} one — right for
     * a rose bush, whose flowers are up top, and wrong here. Lemon Haze is the lanky strain: its
     * top half is a sparse scatter of leaflets up a bare stem, while the bottom is the dense,
     * recognisable half. That is the better inventory icon, and a pot wants the base of a plant in
     * it rather than its tip. That single difference is why this does not just call the vanilla
     * overload.
     */
    private static void registerTallFlowerPotPlant(BlockModelGenerators generator, Block flower, Block potted) {
        MultiVariant top = BlockModelGenerators.plainVariant(
                ModelTemplates.TINTED_CROSS.createWithSuffix(flower, "_top",
                        TextureMapping.cross(TextureMapping.getBlockTexture(flower, "_top")), generator.modelOutput));
        MultiVariant bottom = BlockModelGenerators.plainVariant(
                ModelTemplates.TINTED_CROSS.createWithSuffix(flower, "_bottom",
                        TextureMapping.cross(TextureMapping.getBlockTexture(flower, "_bottom")), generator.modelOutput));
        generator.createDoubleBlock(flower, top, bottom);
        generator.registerSimpleFlatItemModel(flower, "_bottom");

        MultiVariant pot = BlockModelGenerators.plainVariant(
                ModelTemplates.TINTED_FLOWER_POT_CROSS.create(potted,
                        TextureMapping.plant(TextureMapping.getBlockTexture(flower, "_bottom")), generator.modelOutput));
        generator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(potted, pot));
    }
}

package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.BongBlock;
import com.warlonmhite.hempdustry.item.ModItemProperties;
import com.warlonmhite.hempdustry.item.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ModModelProvider extends FabricModelProvider {
    /**
     * The loaded strain registry, needed to tell a plant strain from a hash one.
     *
     * <p>{@code FabricModelProvider} takes only an output, so this arrives through
     * {@code Pack.RegistryDependentFactory} and is joined at generate time. Fabric resolves the
     * future before any provider runs, so the join never blocks.
     */
    private final CompletableFuture<HolderLookup.Provider> registryLookup;

    public ModModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output);
        this.registryLookup = registryLookup;
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockStateModelGenerator) {
        BlockModelGenerators.BlockFamilyProvider hempBricksPool = blockStateModelGenerator.family(ModBlocks.HEMP_BRICKS_BLOCK);
        BlockModelGenerators.BlockFamilyProvider hempPlanksPool = blockStateModelGenerator.family(ModBlocks.HEMP_PLANKS);
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
        // registers, the same as the crops do. TintState only decides whether the generated models
        // carry "tintindex": 0 — vanilla's own ferns are potted through the tinted pair, so a
        // potted wild flower picking up the room's biome is what a player already expects.
        blockStateModelGenerator.createPlant(ModBlocks.INDICA_FLOWER, ModBlocks.POTTED_INDICA_FLOWER, BlockModelGenerators.TintState.TINTED);
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
        // Smoking gear. One item per device now carries every strain in a component, so the visual
        // per-strain split moved from separate items to *model overrides* on a shared item —
        // exactly how vanilla varies a bow by "pulling" or a crossbow by "charged". Predicate
        // matching is >=, so overrides must be listed ascending.
        //
        // TWO predicates, because there are two questions (see HempdustryClient):
        //
        //   hempdustry:packed  0 or 1 — is anything loaded. This is what switches a device between
        //                      its empty and packed models.
        //   hempdustry:strain  the loaded strain's model_index — for a strain shipping *bespoke*
        //                      art instead of the shared look.
        //
        // The devices used to do the first job with the second predicate (`strain >= 1`), which
        // worked only because every strain carried a non-zero index. It is not a safe test once
        // model_index 0 means "no bespoke art of my own", which is the normal case.
        //
        // Driven off ModStrains.BUILT_IN rather than the loaded registry, and that is the honest
        // boundary: bespoke art exists only for the strains the mod itself carries.
        // ModStrains.modelIndex is the single source for the number written into the data and
        // matched here.
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
        List<ModelOverride> spliffOverrides = new ArrayList<>();
        for (ResourceKey<Strain> strain : plantStrains) {
            spliffOverrides.add(new ModelOverride(STRAIN_PREDICATE, ModStrains.modelIndex(strain), spliffModel(strain)));
        }
        // The spliff's BASE model is the one a strain with no bespoke art falls back to — which is
        // every strain a datapack can add, since a datapack cannot ship a texture. It is therefore
        // two layers: the shared roll, plus a mask of the lit tip that the strain's colour tints.
        //
        // layer0 is indica's art rather than a third drawing, and that is derivation not laziness:
        // both shipped spliffs are the same matrix under two palettes (see textures-src/sativa.mctex),
        // and of the 49 opaque pixels only 7 differ strongly between them -- the tip. The other 42
        // are the roll, where indica's white paper is the neutral of the two. So the tip becomes the
        // tinted layer and the roll stays as drawn.
        //
        // The per-strain overrides above still win for indica and sativa, which keep their own art
        // untinted. Dropping those overrides would put every strain on this tinted base instead;
        // that is a texture decision, not a code one, and it costs one line here when wanted.
        uploadTintable(itemModelGenerator, ModelLocationUtils.getModelLocation(ModItems.SPLIFF),
                texture(ModStrains.id(plantStrains.get(0)) + "_spliff"),
                texture("spliff_load"), spliffOverrides);

        // The devices share one packed texture, which they always did. The override is on "packed at
        // all", so giving a strain its own packed art later is one more entry here — keyed on
        // STRAIN_PREDICATE and listed after this one — plus the PNG.
        // A packed device is the empty device plus a mask of what is in the bowl, and the mask is the
        // layer the strain's colour tints. Two layers rather than a second full texture, because
        // ItemModelGenerator hands layerN the tint index N and a provider can then colour layer1
        // while leaving the wood and glass of layer0 alone — see ModItemProperties.LOAD_TINT_INDEX.
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
        ModelTemplates.TWO_LAYERED_ITEM.create(
                ModelLocationUtils.getModelLocation(ModItems.MOON_ROCK),
                TextureMapping.layered(texture("moon_rock"), texture("moon_rock_load")),
                itemModelGenerator.output);

        itemModelGenerator.generateFlatItem(ModItems.HEMP_PLANKS_SIGN, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_PLANKS_HANGING_SIGN, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_BOAT, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.HEMP_CHEST_BOAT, ModelTemplates.FLAT_ITEM);

        // The beanie's icon is the flat sprite; worn, it is the slouchy tam HempBeanieRenderer draws.
        // Worn, the flip-flops are FlipFlopsRenderer's. Both icons are plain sprites, untrimmable.
        itemModelGenerator.generateFlatItem(ModItems.HEMP_BEANIE, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(ModItems.FLIP_FLOPS, ModelTemplates.FLAT_ITEM);
        registerDyeableArmor(itemModelGenerator, ModItems.HEMP_SHIRT, "chestplate");
        registerDyeableArmor(itemModelGenerator, ModItems.HEMP_HAREM_PANTS, "leggings");
    }

    /**
     * Vanilla's trim materials in the order {@code ItemModelGenerator.TRIM_MATERIALS} lists them, each
     * at {@code trim_type} (its index + 1) / 10 — the list is private, and these are the overrides
     * {@code generateArmorTrims} wrote for the garments before they took dye. Hemp is no vanilla material,
     * so none takes its darker variant.
     */
    private static final List<String> TRIM_MATERIALS = List.of(
            "quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst");

    /**
     * {@code generateArmorTrims} for leather, the way leather is drawn, with two differences. Vanilla's
     * colour provider hard-codes leather's brown as the undyed tint, so the garments have their own
     * ({@code HempdustryClient}), and the layers are named the other way round: vanilla tints
     * {@code item/<id>} and lays {@code item/<id>_overlay} on top, while here the grey cloth is the
     * new {@code item/<id>_dyeable} and {@code item/<id>} — the name a resource pack already redraws —
     * stays the untinted layer on top. A pack that redrew the whole garment then still shows its own
     * art, undyed, instead of having it tinted beige. Layer 0 is the one the colour provider tints.
     */
    private static void registerDyeableArmor(ItemModelGenerators generator, Item item, String trimSlot) {
        ResourceLocation model = ModelLocationUtils.getModelLocation(item);
        ResourceLocation cloth = TextureMapping.getItemTexture(item, "_dyeable");
        ResourceLocation drawn = TextureMapping.getItemTexture(item);
        List<ModelOverride> trims = new ArrayList<>();
        for (int i = 0; i < TRIM_MATERIALS.size(); i++) {
            String material = TRIM_MATERIALS.get(i);
            ResourceLocation trimmed = model.withSuffix("_" + material + "_trim");
            generator.generateLayeredItem(trimmed, cloth, drawn,
                    ResourceLocation.withDefaultNamespace("trims/items/" + trimSlot + "_trim_" + material));
            trims.add(new ModelOverride(Map.of(ItemModelGenerators.TRIM_TYPE_PREDICATE_ID, (i + 1) / 10.0F), trimmed));
        }
        generator.output.accept(model, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("parent", "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", cloth.toString());
            textures.addProperty("layer1", drawn.toString());
            json.add("textures", textures);
            json.add("overrides", overridesJson(trims));
            return (JsonElement) json;
        });
    }

    /** @see ModItemProperties#STRAIN */
    private static final ResourceLocation STRAIN_PREDICATE = ModItemProperties.STRAIN;

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

    /** @see ModItemProperties#PACKED */
    private static final ResourceLocation PACKED_PREDICATE = ModItemProperties.PACKED;

    /**
     * A device's empty model, its packed one (the art plus a strain-tinted load), and the switch.
     *
     * <p>A device that stands as a block ({@link ModBlocks#DEVICE_BLOCKS}) is also 3D in the hand:
     * vanilla's trident/spyglass split, the flat sprite in the inventory, on the ground and in a
     * frame, and the placed block's own model everywhere else. Packed, the hand model is the same
     * bong with a strain-tinted load sitting in the bowl. Without item definitions that switch is
     * not data on this line: {@code item/<bong>_in_hand} holds the hand model and its packed
     * override, and the client's {@code ItemRendererMixin} chooses it the way vanilla chooses
     * {@code trident_in_hand}.
     */
    private static void registerDevice(ItemModelGenerators generator, Item item, String art,
                                       String packedModelName, String loadTexture) {
        ResourceLocation packedModel = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/" + packedModelName);
        ModelTemplates.TWO_LAYERED_ITEM.create(packedModel,
                TextureMapping.layered(texture(art), texture(loadTexture)), generator.output);
        uploadWithOverrides(generator, ModelLocationUtils.getModelLocation(item),
                texture(art), List.of(new ModelOverride(PACKED_PREDICATE, 1, packedModel)));
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (!ModBlocks.DEVICE_BLOCKS.containsKey(name)) {
            return;
        }
        // The placed packed bong's own model (registerBongBlocks), so the hand and the table show
        // the same bowl -- tinted by the strain here, where there is a stack to ask.
        ResourceLocation packedInHand = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/" + name + "_packed");
        // The draw, in the bow's grammar: while it is being used, the bong steps through the
        // stages on how far into the draw it is -- smoke gathering over the water, then climbing
        // the neck -- exactly as a bow steps through pulling_0..2. Thresholds at a quarter, a half
        // and three quarters of the draw, the fraction ModItemProperties.DRAW reports.
        List<ModelOverride> overrides = new ArrayList<>();
        overrides.add(ModelOverride.of(packedInHand, PACKED_PREDICATE, 1));
        for (int stage = 1; stage <= BONG_DRAW_TEMPLATES.size(); stage++) {
            ResourceLocation model = BONG_DRAW_TEMPLATES.get(stage - 1).create(
                    ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/" + packedModelName + "_drawing_" + stage),
                    bongTextures(name), generator.output);
            overrides.add(ModelOverride.of(model, PACKED_PREDICATE, 1, ModItemProperties.DRAWING, 1,
                    ModItemProperties.DRAW, stage / (BONG_DRAW_TEMPLATES.size() + 1f)));
        }
        uploadParented(generator, ModItemProperties.inHandModel(item),
                ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/" + name), overrides);
    }

    // The placed bong, hand-written once as block/bong_template.json and re-textured per glass.
    private static final TextureSlot BONG_SHEET = TextureSlot.create("bong");
    private static final ModelTemplate BONG_TEMPLATE = new ModelTemplate(
            Optional.of(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/bong_template")), Optional.empty(), BONG_SHEET);
    private static final ModelTemplate BONG_PACKED_TEMPLATE = new ModelTemplate(
            Optional.of(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/bong_template_packed")), Optional.empty(), BONG_SHEET);
    private static final List<ModelTemplate> BONG_DRAW_TEMPLATES = java.util.stream.IntStream.rangeClosed(1, 3)
            .mapToObj(stage -> new ModelTemplate(Optional.of(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID,
                    "item/bong_template_draw_" + stage)), Optional.empty(), BONG_SHEET))
            .toList();

    private static TextureMapping bongTextures(String name) {
        return new TextureMapping().put(BONG_SHEET, ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/" + name));
    }

    /**
     * One model per glass and a facing blockstate. North-default, rotated the way every horizontal
     * block of vanilla's is, with the downstem to the east of the direction it faces.
     */
    private static void registerBongBlocks(BlockModelGenerators generator) {
        for (Map.Entry<String, Block> entry : ModBlocks.DEVICE_BLOCKS.entrySet()) {
            Block block = entry.getValue();
            ResourceLocation model = BONG_TEMPLATE.create(block, bongTextures(entry.getKey()), generator.modelOutput);
            ResourceLocation packed = BONG_PACKED_TEMPLATE.createWithSuffix(block, "_packed", bongTextures(entry.getKey()),
                    generator.modelOutput);
            PropertyDispatch variants = PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BongBlock.PACKED)
                    .generate((facing, packedState) -> {
                        Variant variant = Variant.variant()
                                .with(VariantProperties.MODEL, packedState ? packed : model);
                        return facing == Direction.NORTH ? variant : variant.with(VariantProperties.Y_ROT, switch (facing) {
                            case EAST -> VariantProperties.Rotation.R90;
                            case SOUTH -> VariantProperties.Rotation.R180;
                            default -> VariantProperties.Rotation.R270;
                        });
                    });
            generator.blockStateOutput.accept(MultiVariantGenerator.multiVariant(block).with(variants));
        }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/" + name);
    }

    private static ResourceLocation spliffModel(ResourceKey<Strain> strain) {
        return ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/spliff_" + ModStrains.id(strain));
    }

    private static void uploadGenerated(ItemModelGenerators generator, ResourceLocation modelId, ResourceLocation texture) {
        ModelTemplates.FLAT_ITEM.create(modelId, TextureMapping.layer0(texture), generator.output);
    }

    /**
     * A {@code minecraft:item/generated} model with an {@code overrides} array, which the vanilla
     * {@link ModelTemplates} helpers can't express. Written straight to the generator's writer rather than
     * through a {@link ModelTemplate}, since the whole point is the extra key.
     */
    private static void uploadWithOverrides(ItemModelGenerators generator, ResourceLocation modelId,
                                            ResourceLocation texture, List<ModelOverride> overrides) {
        uploadTintable(generator, modelId, texture, null, overrides);
    }

    /**
     * As {@link #uploadWithOverrides}, plus an optional second layer.
     *
     * <p>{@code layer1} is the strain-tinted one: {@code ItemModelGenerator} gives each {@code layerN}
     * the tint index {@code N}, and the colour provider paints index 1 and leaves index 0 alone. Pass
     * {@code null} for a single-layer model.
     */
    private static void uploadTintable(ItemModelGenerators generator, ResourceLocation modelId,
                                       ResourceLocation texture, ResourceLocation tintedLayer,
                                       List<ModelOverride> overrides) {
        generator.output.accept(modelId, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("parent", "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", texture.toString());
            if (tintedLayer != null) {
                textures.addProperty("layer1", tintedLayer.toString());
            }
            json.add("textures", textures);

            json.add("overrides", overridesJson(overrides));
            return (JsonElement) json;
        });
    }

    /**
     * A model that inherits everything from {@code parent} and adds an {@code overrides} array —
     * the bongs' 3D hand models, which are the placed block plus the packed switch.
     */
    private static void uploadParented(ItemModelGenerators generator, ResourceLocation modelId, ResourceLocation parent,
                                       List<ModelOverride> overrides) {
        generator.output.accept(modelId, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("parent", parent.toString());
            json.add("overrides", overridesJson(overrides));
            return (JsonElement) json;
        });
    }

    private static JsonArray overridesJson(List<ModelOverride> overrides) {
        JsonArray array = new JsonArray();
        for (ModelOverride override : overrides) {
            JsonObject predicate = new JsonObject();
            // Vanilla writes its own properties bare (trim_type), as it reads them.
            override.predicates().forEach((property, threshold) -> predicate.addProperty(
                    property.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) ? property.getPath() : property.toString(),
                    threshold));
            JsonObject entry = new JsonObject();
            entry.add("predicate", predicate);
            entry.addProperty("model", override.model().toString());
            array.add(entry);
        }
        return array;
    }

    /**
     * One {@code overrides} entry: the model, and each property to test with its {@code >=} threshold.
     * Every property has to pass for the entry to match, and the last matching entry wins.
     */
    private record ModelOverride(Map<ResourceLocation, Number> predicates, ResourceLocation model) {
        ModelOverride(ResourceLocation predicate, int threshold, ResourceLocation model) {
            this(Map.of(predicate, threshold), model);
        }

        /** {@code model} when every (property, threshold) pair holds, written in the order given. */
        static ModelOverride of(ResourceLocation model, Object... propertyThresholdPairs) {
            Map<ResourceLocation, Number> predicates = new LinkedHashMap<>();
            for (int i = 0; i < propertyThresholdPairs.length; i += 2) {
                predicates.put((ResourceLocation) propertyThresholdPairs[i], (Number) propertyThresholdPairs[i + 1]);
            }
            return new ModelOverride(predicates, model);
        }
    }


    /**
     * Space Cake's blockstate and its seven models.
     *
     * <p>Each model is a one-line child of the matching vanilla cake model with only the texture map
     * replaced — model inheritance carries the {@code elements} across, so the bite geometry, the
     * cullfaces and the shrinking hitbox all come from Mojang and cannot drift out of step with them.
     * The vanilla {@link ModelTemplates} helpers can't express "parent plus textures", so these go straight
     * to the generator's model collector, same as the smoking-gear overrides above.
     */
    /**
     * Vanilla's beds, by hand: {@code createBedItem} writes only their item. The block points at
     * vanilla's own particle-only {@code block/bed} model, as all sixteen beds do (the renderer draws
     * the bed); the item is vanilla's {@code template_bed}, a {@code builtin/entity} model, with hemp
     * wool for its particle the way each vanilla bed item uses its wool. What draws that item is the
     * builtin renderer {@code HempdustryClient} registers for it.
     */
    private static void registerHempBed(BlockModelGenerators generator) {
        generator.blockStateOutput.accept(MultiVariantGenerator.multiVariant(ModBlocks.HEMP_BED,
                Variant.variant().with(VariantProperties.MODEL, ModelLocationUtils.decorateBlockModelLocation("bed"))));
        ModelTemplates.BED_INVENTORY.create(ModelLocationUtils.getModelLocation(ModBlocks.HEMP_BED.asItem()),
                TextureMapping.particle(ModBlocks.HEMP_WOOL), generator.modelOutput);
    }

    private static void registerSpaceCake(BlockModelGenerators generator) {
        PropertyDispatch.C1<Integer> variants = PropertyDispatch.property(CakeBlock.BITES);
        for (int bites = 0; bites <= CakeBlock.MAX_BITES; bites++) {
            String suffix = bites == 0 ? "" : "_slice" + bites;
            ResourceLocation model = ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "block/space_cake" + suffix);
            uploadRetextured(generator, model, ResourceLocation.withDefaultNamespace("block/cake" + suffix), bites > 0);
            variants.select(bites, Variant.variant().with(VariantProperties.MODEL, model));
        }
        generator.blockStateOutput.accept(
                MultiVariantGenerator.multiVariant(ModBlocks.SPACE_CAKE).with(variants));
        // The item is the whole, uneaten cake, exactly as vanilla's cake item is.
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(ModBlocks.SPACE_CAKE.asItem()),
                TextureMapping.layer0(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "item/space_cake")),
                generator.modelOutput);
    }

    private static void uploadRetextured(BlockModelGenerators generator, ResourceLocation modelId,
                                         ResourceLocation parent, boolean sliced) {
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
     * {@code createDoublePlant(block, TintState)} would hand both the {@code _top} one — right for
     * a rose bush, whose flowers are up top, and wrong here. Lemon Haze is the lanky strain: its
     * top half is a sparse scatter of leaflets up a bare stem, while the bottom is the dense,
     * recognisable half. That is the better inventory icon, and a pot wants the base of a plant in
     * it rather than its tip. That single difference is why this does not just call the vanilla
     * overload.
     */
    private static void registerTallFlowerPotPlant(BlockModelGenerators generator, Block flower, Block potted) {
        ResourceLocation top = ModelTemplates.TINTED_CROSS.createWithSuffix(flower, "_top",
                TextureMapping.cross(TextureMapping.getBlockTexture(flower, "_top")), generator.modelOutput);
        ResourceLocation bottom = ModelTemplates.TINTED_CROSS.createWithSuffix(flower, "_bottom",
                TextureMapping.cross(TextureMapping.getBlockTexture(flower, "_bottom")), generator.modelOutput);
        generator.createDoubleBlock(flower, top, bottom);
        generator.createSimpleFlatItemModel(flower, "_bottom");

        ResourceLocation pot = ModelTemplates.TINTED_FLOWER_POT_CROSS.create(potted,
                TextureMapping.plant(TextureMapping.getBlockTexture(flower, "_bottom")), generator.modelOutput);
        generator.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(potted, pot));
    }
}

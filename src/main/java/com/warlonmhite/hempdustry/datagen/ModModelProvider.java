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
import net.minecraft.block.Block;
import net.minecraft.block.CakeBlock;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import net.minecraft.registry.RegistryWrapper;

import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

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
    private final CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup;

    public ModModelProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output);
        this.registryLookup = registryLookup;
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        BlockStateModelGenerator.BlockTexturePool hempBricksPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.HEMP_BRICKS_BLOCK);
        BlockStateModelGenerator.BlockTexturePool hempPlanksPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.HEMP_PLANKS);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.HEMPCRETE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.HEMPCRETE_POWDER_BLOCK);
        // Vanilla's own wool+carpet helper — the carpet model reuses the wool texture, same as
        // every vanilla carpet does, so hemp_carpet needs no art of its own.
        blockStateModelGenerator.registerWoolAndCarpet(ModBlocks.HEMP_WOOL, ModBlocks.HEMP_CARPET);

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

        blockStateModelGenerator.registerDoor(ModBlocks.HEMP_PLANKS_DOOR);
        blockStateModelGenerator.registerTrapdoor(ModBlocks.HEMP_PLANKS_TRAPDOOR);

        blockStateModelGenerator.registerLog(ModBlocks.HEMP_BALE).log(ModBlocks.HEMP_BALE);
        // TexturedModel.LEAVES, not registerSimpleCubeAll: it parents to minecraft:block/leaves,
        // which is cube_all with ambient occlusion off -- the reason vanilla foliage does not
        // shade itself into a dark blob.
        blockStateModelGenerator.registerSingleton(ModBlocks.HEMP_LEAVES, TexturedModel.LEAVES);

        registerSpaceCake(blockStateModelGenerator);

        // TINTED, not NOT_TINTED: both wild flowers take the biome tint that HempdustryClient
        // registers, the same as the crops do. TintType only decides whether the generated models
        // carry "tintindex": 0 — vanilla's own ferns are potted through the tinted pair, so a
        // potted wild flower picking up the room's biome is what a player already expects.
        blockStateModelGenerator.registerFlowerPotPlant(ModBlocks.INDICA_FLOWER, ModBlocks.POTTED_INDICA_FLOWER, BlockStateModelGenerator.TintType.TINTED);
        registerTallFlowerPotPlant(blockStateModelGenerator, ModBlocks.SATIVA_FLOWER, ModBlocks.POTTED_SATIVA_FLOWER);

        // The crops' blockstates and stage models are hand-written under resources/ — the model
        // generator has no notion of a two- or three-tall crop, so INDICA_CROP and SATIVA_CROP
        // are deliberately absent here.
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.CANNABUTTER, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BRICK, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FIBER, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_CANVAS, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FLOUR, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_STEM, Models.GENERATED);
        itemModelGenerator.register(ModItems.RETTED_HEMP_STEM, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_LEAF, Models.GENERATED);
        itemModelGenerator.register(ModItems.DECARBOXYLATED_HEMP, Models.GENERATED);
        itemModelGenerator.register(ModItems.WASHED_DECARBOXYLATED_HEMP, Models.GENERATED);
        itemModelGenerator.register(ModItems.SCORCHED_HEMP, Models.GENERATED);
        itemModelGenerator.register(ModItems.KIEF, Models.GENERATED);
        itemModelGenerator.register(ModItems.FILTERED_KIEF, Models.GENERATED);
        itemModelGenerator.register(ModItems.BUBBLE_HASH, Models.GENERATED);
        itemModelGenerator.register(ModItems.HASHISH, Models.GENERATED);
        itemModelGenerator.register(ModItems.CHARAS, Models.GENERATED);
        itemModelGenerator.register(ModItems.FILTERED_HASHISH, Models.GENERATED);
        itemModelGenerator.register(ModItems.ROSIN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMPCRETE, Models.GENERATED);
        itemModelGenerator.register(ModItems.TOASTED_HEMP_SEEDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FLAPJACK, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_MILK_BUCKET, Models.GENERATED);
        itemModelGenerator.register(ModItems.SIEMIENIOTKA, Models.GENERATED);
        itemModelGenerator.register(ModItems.CANNABUTTER_TOAST, Models.GENERATED);
        itemModelGenerator.register(ModItems.SPACE_COOKIE, Models.GENERATED);
        itemModelGenerator.register(ModItems.SPACE_BROWNIE, Models.GENERATED);
        itemModelGenerator.register(ModItems.BHANG_BUCKET, Models.GENERATED);
        itemModelGenerator.register(ModItems.DAWAMESK, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_SEEDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_BUDS, Models.GENERATED);
        for (Item disc : ModItems.MUSIC_DISCS) {
            itemModelGenerator.register(disc, Models.GENERATED);
        }

        itemModelGenerator.register(ModItems.SATIVA_BUDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.SATIVA_SEEDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.BELDIA_SEEDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.BELDIA_BUDS, Models.GENERATED);
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
        List<RegistryKey<Strain>> plantStrains = plantStrains();
        for (RegistryKey<Strain> strain : plantStrains) {
            uploadGenerated(itemModelGenerator, spliffModel(strain), texture(ModStrains.id(strain) + "_spliff"));
        }
        List<ModelOverride> spliffOverrides = new ArrayList<>();
        for (RegistryKey<Strain> strain : plantStrains) {
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
        uploadTintable(itemModelGenerator, ModelIds.getItemModelId(ModItems.SPLIFF),
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
            String name = Registries.ITEM.getId(bong).getPath();
            registerDevice(itemModelGenerator, bong, name, "packed_" + name,
                    DeviceType.BONG.packedModel() + "_load");
        }

        // The moon rock: one item for every strain, tinted by whichever one is stuck to it. Two
        // layers, exactly as a packed device is -- the hashish crust is the same on all of them and
        // the bud showing through it is not.
        Models.GENERATED_TWO_LAYERS.upload(
                ModelIds.getItemModelId(ModItems.MOON_ROCK),
                TextureMap.layered(texture("moon_rock"), texture("moon_rock_load")),
                itemModelGenerator.writer);

        itemModelGenerator.register(ModItems.HEMP_PLANKS_SIGN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_PLANKS_HANGING_SIGN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BOAT, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_CHEST_BOAT, Models.GENERATED);

        itemModelGenerator.registerArmor(((ArmorItem) ModItems.FLIP_FLOPS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_BEANIE));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_HAREM_PANTS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_SHIRT));
    }

    /** @see ModItemProperties#STRAIN */
    private static final Identifier STRAIN_PREDICATE = ModItemProperties.STRAIN;

    /**
     * The built-in strains that grew on a plant, in {@link ModStrains#BUILT_IN} order.
     *
     * <p>{@code flower().isPresent()} is the mod-wide predicate for "this grew on a plant" — the
     * same one the creative tab, the seed pools, the siftable tag and the spliff recipes key on —
     * so anything hash-shaped added later is excluded here for free.
     */
    private List<RegistryKey<Strain>> plantStrains() {
        RegistryWrapper.Impl<Strain> strains = Strain.registry(registryLookup.join());
        return ModStrains.BUILT_IN.stream()
                .filter(key -> strains.getOrThrow(key).value().flower().isPresent())
                .toList();
    }

    /** @see ModItemProperties#PACKED */
    private static final Identifier PACKED_PREDICATE = ModItemProperties.PACKED;

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
    private static void registerDevice(ItemModelGenerator generator, Item item, String art,
                                       String packedModelName, String loadTexture) {
        Identifier packedModel = Identifier.of(Hempdustry.MOD_ID, "item/" + packedModelName);
        Models.GENERATED_TWO_LAYERS.upload(packedModel,
                TextureMap.layered(texture(art), texture(loadTexture)), generator.writer);
        uploadWithOverrides(generator, ModelIds.getItemModelId(item),
                texture(art), List.of(new ModelOverride(PACKED_PREDICATE, 1, packedModel)));
        String name = Registries.ITEM.getId(item).getPath();
        if (!ModBlocks.DEVICE_BLOCKS.containsKey(name)) {
            return;
        }
        // The placed packed bong's own model (registerBongBlocks), so the hand and the table show
        // the same bowl -- tinted by the strain here, where there is a stack to ask.
        Identifier packedInHand = Identifier.of(Hempdustry.MOD_ID, "block/" + name + "_packed");
        // The draw, in the bow's grammar: while it is being used, the bong steps through the
        // stages on how far into the draw it is -- smoke gathering over the water, then climbing
        // the neck -- exactly as a bow steps through pulling_0..2. Thresholds at a quarter, a half
        // and three quarters of the draw, the fraction ModItemProperties.DRAW reports.
        List<ModelOverride> overrides = new ArrayList<>();
        overrides.add(ModelOverride.of(packedInHand, PACKED_PREDICATE, 1));
        for (int stage = 1; stage <= BONG_DRAW_TEMPLATES.size(); stage++) {
            Identifier model = BONG_DRAW_TEMPLATES.get(stage - 1).upload(
                    Identifier.of(Hempdustry.MOD_ID, "item/" + packedModelName + "_drawing_" + stage),
                    bongTextures(name), generator.writer);
            overrides.add(ModelOverride.of(model, PACKED_PREDICATE, 1, ModItemProperties.DRAWING, 1,
                    ModItemProperties.DRAW, stage / (BONG_DRAW_TEMPLATES.size() + 1f)));
        }
        uploadParented(generator, ModItemProperties.inHandModel(item),
                Identifier.of(Hempdustry.MOD_ID, "block/" + name), overrides);
    }

    // The placed bong, hand-written once as block/bong_template.json and re-textured per glass.
    private static final TextureKey BONG_SHEET = TextureKey.of("bong");
    private static final Model BONG_TEMPLATE = new Model(
            Optional.of(Identifier.of(Hempdustry.MOD_ID, "block/bong_template")), Optional.empty(), BONG_SHEET);
    private static final Model BONG_PACKED_TEMPLATE = new Model(
            Optional.of(Identifier.of(Hempdustry.MOD_ID, "item/bong_template_packed")), Optional.empty(), BONG_SHEET);
    private static final List<Model> BONG_DRAW_TEMPLATES = java.util.stream.IntStream.rangeClosed(1, 3)
            .mapToObj(stage -> new Model(Optional.of(Identifier.of(Hempdustry.MOD_ID,
                    "item/bong_template_draw_" + stage)), Optional.empty(), BONG_SHEET))
            .toList();

    private static TextureMap bongTextures(String name) {
        return new TextureMap().put(BONG_SHEET, Identifier.of(Hempdustry.MOD_ID, "block/" + name));
    }

    /**
     * One model per glass and a facing blockstate. North-default, rotated the way every horizontal
     * block of vanilla's is, with the downstem to the east of the direction it faces.
     */
    private static void registerBongBlocks(BlockStateModelGenerator generator) {
        for (Map.Entry<String, Block> entry : ModBlocks.DEVICE_BLOCKS.entrySet()) {
            Block block = entry.getValue();
            Identifier model = BONG_TEMPLATE.upload(block, bongTextures(entry.getKey()), generator.modelCollector);
            Identifier packed = BONG_PACKED_TEMPLATE.upload(block, "_packed", bongTextures(entry.getKey()),
                    generator.modelCollector);
            BlockStateVariantMap variants = BlockStateVariantMap.create(Properties.HORIZONTAL_FACING, BongBlock.PACKED)
                    .register((facing, packedState) -> {
                        BlockStateVariant variant = BlockStateVariant.create()
                                .put(VariantSettings.MODEL, packedState ? packed : model);
                        return facing == Direction.NORTH ? variant : variant.put(VariantSettings.Y, switch (facing) {
                            case EAST -> VariantSettings.Rotation.R90;
                            case SOUTH -> VariantSettings.Rotation.R180;
                            default -> VariantSettings.Rotation.R270;
                        });
                    });
            generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(variants));
        }
    }

    private static Identifier texture(String name) {
        return Identifier.of(Hempdustry.MOD_ID, "item/" + name);
    }

    private static Identifier spliffModel(RegistryKey<Strain> strain) {
        return Identifier.of(Hempdustry.MOD_ID, "item/spliff_" + ModStrains.id(strain));
    }

    private static void uploadGenerated(ItemModelGenerator generator, Identifier modelId, Identifier texture) {
        Models.GENERATED.upload(modelId, TextureMap.layer0(texture), generator.writer);
    }

    /**
     * A {@code minecraft:item/generated} model with an {@code overrides} array, which the vanilla
     * {@link Models} helpers can't express. Written straight to the generator's writer rather than
     * through a {@link Model}, since the whole point is the extra key.
     */
    private static void uploadWithOverrides(ItemModelGenerator generator, Identifier modelId,
                                            Identifier texture, List<ModelOverride> overrides) {
        uploadTintable(generator, modelId, texture, null, overrides);
    }

    /**
     * As {@link #uploadWithOverrides}, plus an optional second layer.
     *
     * <p>{@code layer1} is the strain-tinted one: {@code ItemModelGenerator} gives each {@code layerN}
     * the tint index {@code N}, and the colour provider paints index 1 and leaves index 0 alone. Pass
     * {@code null} for a single-layer model.
     */
    private static void uploadTintable(ItemModelGenerator generator, Identifier modelId,
                                       Identifier texture, Identifier tintedLayer,
                                       List<ModelOverride> overrides) {
        generator.writer.accept(modelId, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("parent", "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", texture.toString());
            if (tintedLayer != null) {
                textures.addProperty("layer1", tintedLayer.toString());
            }
            json.add("textures", textures);

            JsonArray array = new JsonArray();
            for (ModelOverride override : overrides) {
                JsonObject predicate = new JsonObject();
                override.predicates().forEach((property, threshold) ->
                        predicate.addProperty(property.toString(), threshold));
                JsonObject entry = new JsonObject();
                entry.add("predicate", predicate);
                entry.addProperty("model", override.model().toString());
                array.add(entry);
            }
            json.add("overrides", array);
            return (JsonElement) json;
        });
    }

    /**
     * A model that inherits everything from {@code parent} and adds an {@code overrides} array —
     * the bongs' 3D hand models, which are the placed block plus the packed switch.
     */
    private static void uploadParented(ItemModelGenerator generator, Identifier modelId, Identifier parent,
                                       List<ModelOverride> overrides) {
        generator.writer.accept(modelId, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("parent", parent.toString());
            JsonArray array = new JsonArray();
            for (ModelOverride override : overrides) {
                JsonObject predicate = new JsonObject();
                override.predicates().forEach((property, threshold) ->
                        predicate.addProperty(property.toString(), threshold));
                JsonObject entry = new JsonObject();
                entry.add("predicate", predicate);
                entry.addProperty("model", override.model().toString());
                array.add(entry);
            }
            json.add("overrides", array);
            return (JsonElement) json;
        });
    }

    /**
     * One {@code overrides} entry: the model, and each property to test with its {@code >=} threshold.
     * Every property has to pass for the entry to match, and the last matching entry wins.
     */
    private record ModelOverride(Map<Identifier, Number> predicates, Identifier model) {
        ModelOverride(Identifier predicate, int threshold, Identifier model) {
            this(Map.of(predicate, threshold), model);
        }

        /** {@code model} when every (property, threshold) pair holds, written in the order given. */
        static ModelOverride of(Identifier model, Object... propertyThresholdPairs) {
            Map<Identifier, Number> predicates = new LinkedHashMap<>();
            for (int i = 0; i < propertyThresholdPairs.length; i += 2) {
                predicates.put((Identifier) propertyThresholdPairs[i], (Number) propertyThresholdPairs[i + 1]);
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
     * The vanilla {@link Models} helpers can't express "parent plus textures", so these go straight
     * to the generator's model collector, same as the smoking-gear overrides above.
     */
    private static void registerSpaceCake(BlockStateModelGenerator generator) {
        BlockStateVariantMap.SingleProperty<Integer> variants = BlockStateVariantMap.create(CakeBlock.BITES);
        for (int bites = 0; bites <= CakeBlock.MAX_BITES; bites++) {
            String suffix = bites == 0 ? "" : "_slice" + bites;
            Identifier model = Identifier.of(Hempdustry.MOD_ID, "block/space_cake" + suffix);
            uploadRetextured(generator, model, Identifier.ofVanilla("block/cake" + suffix), bites > 0);
            variants.register(bites, BlockStateVariant.create().put(VariantSettings.MODEL, model));
        }
        generator.blockStateCollector.accept(
                VariantsBlockStateSupplier.create(ModBlocks.SPACE_CAKE).coordinate(variants));
        // The item is the whole, uneaten cake, exactly as vanilla's cake item is.
        Models.GENERATED.upload(ModelIds.getItemModelId(ModBlocks.SPACE_CAKE.asItem()),
                TextureMap.layer0(Identifier.of(Hempdustry.MOD_ID, "item/space_cake")),
                generator.modelCollector);
    }

    private static void uploadRetextured(BlockStateModelGenerator generator, Identifier modelId,
                                         Identifier parent, boolean sliced) {
        generator.modelCollector.accept(modelId, () -> {
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
     * {@code registerFlowerPotPlant} for a plant that is two blocks tall, which vanilla has no
     * helper for — nothing in vanilla is both a double plant and pottable.
     *
     * <p>Three of the four pieces are vanilla's own: {@code registerDoubleBlock} writes the
     * {@code half=lower|upper} blockstate, and both halves upload as {@code TINTED_CROSS} so the
     * biome tint in {@code HempdustryClient} has a {@code tintindex} to land on.
     *
     * <p><b>The item and the flower pot deliberately take the BOTTOM texture.</b> Vanilla's
     * {@code registerDoubleBlock(block, TintType)} would hand both the {@code _top} one — right for
     * a rose bush, whose flowers are up top, and wrong here. Lemon Haze is the lanky strain: its
     * top half is a sparse scatter of leaflets up a bare stem, while the bottom is the dense,
     * recognisable half. That is the better inventory icon, and a pot wants the base of a plant in
     * it rather than its tip. That single difference is why this does not just call the vanilla
     * overload.
     */
    private static void registerTallFlowerPotPlant(BlockStateModelGenerator generator, Block flower, Block potted) {
        Identifier top = Models.TINTED_CROSS.upload(flower, "_top",
                TextureMap.cross(TextureMap.getSubId(flower, "_top")), generator.modelCollector);
        Identifier bottom = Models.TINTED_CROSS.upload(flower, "_bottom",
                TextureMap.cross(TextureMap.getSubId(flower, "_bottom")), generator.modelCollector);
        generator.registerDoubleBlock(flower, top, bottom);
        generator.registerItemModel(flower, "_bottom");

        Identifier pot = Models.TINTED_FLOWER_POT_CROSS.upload(potted,
                TextureMap.plant(TextureMap.getSubId(flower, "_bottom")), generator.modelCollector);
        generator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(potted, pot));
    }
}

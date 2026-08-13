package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItemProperties;
import com.warlonmhite.hempdustry.item.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import net.minecraft.block.CakeBlock;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.registry.RegistryKey;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
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

        blockStateModelGenerator.registerDoor(ModBlocks.HEMP_PLANKS_DOOR);
        blockStateModelGenerator.registerTrapdoor(ModBlocks.HEMP_PLANKS_TRAPDOOR);

        blockStateModelGenerator.registerLog(ModBlocks.HEMP_BALE).log(ModBlocks.HEMP_BALE);

        registerSpaceCake(blockStateModelGenerator);

        blockStateModelGenerator.registerFlowerPotPlant(ModBlocks.INDICA_FLOWER, ModBlocks.POTTED_INDICA_FLOWER, BlockStateModelGenerator.TintType.NOT_TINTED);
        blockStateModelGenerator.registerFlowerPotPlant(ModBlocks.SATIVA_FLOWER, ModBlocks.POTTED_SATIVA_FLOWER, BlockStateModelGenerator.TintType.NOT_TINTED);

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
        itemModelGenerator.register(ModItems.HEMP_LEAF, Models.GENERATED);
        itemModelGenerator.register(ModItems.DECARBOXYLATED_HEMP, Models.GENERATED);
        itemModelGenerator.register(ModItems.WASHED_DECARBOXYLATED_HEMP, Models.GENERATED);
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
        // item/sativa_seeds.png is currently a copy of the indica one — a hemp seed is a hemp seed.
        itemModelGenerator.register(ModItems.SATIVA_SEEDS, Models.GENERATED);
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
        for (RegistryKey<Strain> strain : ModStrains.BUILT_IN) {
            uploadGenerated(itemModelGenerator, spliffModel(strain), texture(ModStrains.id(strain) + "_spliff"));
        }
        List<ModelOverride> spliffOverrides = new ArrayList<>();
        for (RegistryKey<Strain> strain : ModStrains.BUILT_IN) {
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
                texture(ModStrains.id(ModStrains.BUILT_IN.get(0)) + "_spliff"),
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
        for (DeviceType device : DeviceType.values()) {
            Item item = device == DeviceType.PIPE ? ModItems.WOODEN_PIPE : ModItems.BONG;
            Identifier packedModel = Identifier.of(Hempdustry.MOD_ID, "item/" + device.packedModel());
            Models.GENERATED_TWO_LAYERS.upload(packedModel,
                    TextureMap.layered(texture(device.baseName()), texture(device.packedModel() + "_load")),
                    itemModelGenerator.writer);
            uploadWithOverrides(itemModelGenerator, ModelIds.getItemModelId(item),
                    texture(device.baseName()), List.of(new ModelOverride(PACKED_PREDICATE, 1, packedModel)));
        }

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

    /** @see ModItemProperties#PACKED */
    private static final Identifier PACKED_PREDICATE = ModItemProperties.PACKED;

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
                predicate.addProperty(override.predicate().toString(), override.threshold());
                JsonObject entry = new JsonObject();
                entry.add("predicate", predicate);
                entry.addProperty("model", override.model().toString());
                array.add(entry);
            }
            json.add("overrides", array);
            return (JsonElement) json;
        });
    }

    /** One {@code overrides} entry: which property to test, the {@code >=} threshold, and the model. */
    private record ModelOverride(Identifier predicate, int threshold, Identifier model) {
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

}
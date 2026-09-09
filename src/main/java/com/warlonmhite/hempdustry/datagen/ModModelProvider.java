package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
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
import net.minecraft.registry.RegistryKey;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.*;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.RangeDispatchItemModel;
import net.minecraft.client.render.model.json.WeightedVariant;
import com.warlonmhite.hempdustry.client.item.StrainModelIndexProperty;
import com.warlonmhite.hempdustry.client.item.StrainTintSource;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        blockStateModelGenerator.registerDoor(ModBlocks.HEMP_PLANKS_DOOR);
        blockStateModelGenerator.registerTrapdoor(ModBlocks.HEMP_PLANKS_TRAPDOOR);

        blockStateModelGenerator.createLogTexturePool(ModBlocks.HEMP_BALE).log(ModBlocks.HEMP_BALE);

        registerSpaceCake(blockStateModelGenerator);

        // TINTED, not NOT_TINTED: both wild flowers take the biome tint that HempdustryClient
        // registers, the same as the crops do. TintType only decides whether the generated models
        // carry "tintindex": 0 — vanilla's own ferns are potted through the tinted pair, so a
        // potted wild flower picking up the room's biome is what a player already expects.
        blockStateModelGenerator.registerFlowerPotPlantAndItem(ModBlocks.INDICA_FLOWER, ModBlocks.POTTED_INDICA_FLOWER, BlockStateModelGenerator.CrossType.TINTED);
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
        itemModelGenerator.register(ModItems.HASHISH, Models.GENERATED);
        itemModelGenerator.register(ModItems.CHARAS, Models.GENERATED);
        itemModelGenerator.register(ModItems.FILTERED_HASHISH, Models.GENERATED);
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
        List<RegistryKey<Strain>> plantStrains = plantStrains();
        for (RegistryKey<Strain> strain : plantStrains) {
            uploadGenerated(itemModelGenerator, spliffModel(strain), texture(ModStrains.id(strain) + "_spliff"));
        }
        List<RangeDispatchItemModel.Entry> spliffArt = new ArrayList<>();
        for (RegistryKey<Strain> strain : plantStrains) {
            spliffArt.add(ItemModels.rangeDispatchEntry(
                    ItemModels.basic(spliffModel(strain)), ModStrains.modelIndex(strain)));
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
        Identifier spliffTinted = Models.GENERATED_TWO_LAYERS.upload(
                Identifier.of(Hempdustry.MOD_ID, "item/spliff_tinted"),
                TextureMap.layered(texture(ModStrains.id(plantStrains.get(0)) + "_spliff"),
                        texture("spliff_load")),
                itemModelGenerator.modelCollector);
        itemModelGenerator.output.accept(ModItems.SPLIFF, ItemModels.rangeDispatch(
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
            Item item = deviceEntry.getValue();
            Identifier packedModel = Models.GENERATED_TWO_LAYERS.upload(
                    Identifier.of(Hempdustry.MOD_ID, "item/" + device.packedModel()),
                    TextureMap.layered(texture(device.baseName()), texture(device.packedModel() + "_load")),
                    itemModelGenerator.modelCollector);
            Identifier emptyModel = Models.GENERATED.upload(
                    ModelIds.getItemModelId(item), TextureMap.layer0(texture(device.baseName())),
                    itemModelGenerator.modelCollector);
            itemModelGenerator.output.accept(item, ItemModels.condition(
                    ItemModels.hasComponentProperty(ModComponents.SMOKE_CONTENTS),
                    strainTinted(packedModel), ItemModels.basic(emptyModel)));
        }

        itemModelGenerator.register(ModItems.HEMP_PLANKS_SIGN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_PLANKS_HANGING_SIGN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BOAT, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_CHEST_BOAT, Models.GENERATED);

        // Armour models name the equipment asset now, and the trim prefix that goes with the slot.
        // false = "no trim overlays", which is right: the mod ships no trimmed hemp textures.
        itemModelGenerator.registerArmor(ModItems.HEMP_BEANIE, ModArmorMaterials.HEMP_EQUIPMENT_ASSET,
                ItemModelGenerator.HELMET_TRIM_ID_PREFIX, false);
        itemModelGenerator.registerArmor(ModItems.HEMP_SHIRT, ModArmorMaterials.HEMP_EQUIPMENT_ASSET,
                ItemModelGenerator.CHESTPLATE_TRIM_ID_PREFIX, false);
        itemModelGenerator.registerArmor(ModItems.HEMP_HAREM_PANTS, ModArmorMaterials.HEMP_EQUIPMENT_ASSET,
                ItemModelGenerator.LEGGINGS_TRIM_ID_PREFIX, false);
        itemModelGenerator.registerArmor(ModItems.FLIP_FLOPS, ModArmorMaterials.HEMP_EQUIPMENT_ASSET,
                ItemModelGenerator.BOOTS_TRIM_ID_PREFIX, false);
    }

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

    /**
     * A two-layer model whose second layer takes the loaded strain's colour.
     *
     * <p>The tints array answers <b>by position</b>: entry 0 is tint index 0 ({@code layer0}, the
     * object as drawn, so a constant white multiplies to no change) and entry 1 is tint index 1
     * ({@code layer1}, the load mask). See {@link ModItemProperties#LOAD_TINT_INDEX}.
     */
    private static ItemModel.Unbaked strainTinted(Identifier model) {
        return ItemModels.tinted(model,
                ItemModels.constantTintSource(NO_TINT),
                new StrainTintSource(NO_TINT));
    }

    /** White: a tint is a multiply, so this leaves a layer exactly as it was drawn. */
    private static final int NO_TINT = 0xFFFFFF;

    private static Identifier texture(String name) {
        return Identifier.of(Hempdustry.MOD_ID, "item/" + name);
    }

    private static Identifier spliffModel(RegistryKey<Strain> strain) {
        return Identifier.of(Hempdustry.MOD_ID, "item/spliff_" + ModStrains.id(strain));
    }

    private static void uploadGenerated(ItemModelGenerator generator, Identifier modelId, Identifier texture) {
        Models.GENERATED.upload(modelId, TextureMap.layer0(texture), generator.modelCollector);
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
    private static void registerSpaceCake(BlockStateModelGenerator generator) {
        BlockStateVariantMap.SingleProperty<WeightedVariant, Integer> variants =
                BlockStateVariantMap.models(CakeBlock.BITES);
        for (int bites = 0; bites <= CakeBlock.MAX_BITES; bites++) {
            String suffix = bites == 0 ? "" : "_slice" + bites;
            Identifier model = Identifier.of(Hempdustry.MOD_ID, "block/space_cake" + suffix);
            uploadRetextured(generator, model, Identifier.ofVanilla("block/cake" + suffix), bites > 0);
            variants.register(bites, BlockStateModelGenerator.createWeightedVariant(model));
        }
        generator.blockStateCollector.accept(
                VariantsBlockModelDefinitionCreator.of(ModBlocks.SPACE_CAKE).with(variants));
        // The item is the whole, uneaten cake, exactly as vanilla's cake item is.
        generator.registerItemModel(ModBlocks.SPACE_CAKE.asItem(),
                Models.GENERATED.upload(ModelIds.getItemModelId(ModBlocks.SPACE_CAKE.asItem()),
                        TextureMap.layer0(Identifier.of(Hempdustry.MOD_ID, "item/space_cake")),
                        generator.modelCollector));
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
        WeightedVariant top = BlockStateModelGenerator.createWeightedVariant(
                Models.TINTED_CROSS.upload(flower, "_top",
                        TextureMap.cross(TextureMap.getSubId(flower, "_top")), generator.modelCollector));
        WeightedVariant bottom = BlockStateModelGenerator.createWeightedVariant(
                Models.TINTED_CROSS.upload(flower, "_bottom",
                        TextureMap.cross(TextureMap.getSubId(flower, "_bottom")), generator.modelCollector));
        generator.registerDoubleBlock(flower, top, bottom);
        generator.registerItemModel(flower, "_bottom");

        WeightedVariant pot = BlockStateModelGenerator.createWeightedVariant(
                Models.TINTED_FLOWER_POT_CROSS.upload(potted,
                        TextureMap.plant(TextureMap.getSubId(flower, "_bottom")), generator.modelCollector));
        generator.blockStateCollector.accept(BlockStateModelGenerator.createSingletonBlockState(potted, pot));
    }
}

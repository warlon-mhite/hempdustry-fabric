package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import net.minecraft.block.CakeBlock;
import com.warlonmhite.hempdustry.item.custom.Strain;
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
        itemModelGenerator.register(ModItems.MUSIC_DISC_GANJA, Models.GENERATED);

        itemModelGenerator.register(ModItems.SATIVA_BUDS, Models.GENERATED);
        // item/sativa_seeds.png is currently a copy of the indica one — a hemp seed is a hemp seed.
        itemModelGenerator.register(ModItems.SATIVA_SEEDS, Models.GENERATED);
        // Smoking gear. One item per device now carries every strain in a component, so the visual
        // per-strain split moved from separate items to *model overrides* on a shared item —
        // exactly how vanilla varies a bow by "pulling" or a crossbow by "charged". The predicate is
        // hempdustry:strain, 0 when nothing is loaded and the strain's index + 1 otherwise (see
        // HempdustryClient). Predicate matching is >=, so overrides must be listed ascending.
        //
        // The spliff keeps a texture per strain, which it always had. The devices all share one
        // packed texture, which they always did — the override is at >= 1 ("packed at all"), so
        // giving a strain its own packed art later is one more entry here plus the PNG.
        for (Strain strain : Strain.ACTIVE) {
            uploadGenerated(itemModelGenerator, spliffModel(strain), texture(strain.id() + "_spliff"));
        }
        List<ModelOverride> spliffOverrides = new ArrayList<>();
        for (Strain strain : Strain.ACTIVE) {
            spliffOverrides.add(new ModelOverride(strain.ordinal() + 1, spliffModel(strain)));
        }
        uploadWithOverrides(itemModelGenerator, ModelIds.getItemModelId(ModItems.SPLIFF),
                texture(Strain.ACTIVE.get(0).id() + "_spliff"), spliffOverrides);

        for (DeviceType device : DeviceType.values()) {
            Item item = device == DeviceType.PIPE ? ModItems.WOODEN_PIPE : ModItems.BONG;
            Identifier packedModel = Identifier.of(Hempdustry.MOD_ID, "item/" + device.packedTexture());
            uploadGenerated(itemModelGenerator, packedModel, texture(device.packedTexture()));
            uploadWithOverrides(itemModelGenerator, ModelIds.getItemModelId(item),
                    texture(device.baseName()), List.of(new ModelOverride(1, packedModel)));
        }

        itemModelGenerator.register(ModItems.HEMP_PLANKS_SIGN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_PLANKS_HANGING_SIGN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BOAT, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_CHEST_BOAT, Models.GENERATED);

        itemModelGenerator.registerArmor(((ArmorItem) ModItems.FLIP_FLOPS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_BEANNIE));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_HAREM_PANTS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_SHIRT));
    }

    /** The {@code hempdustry:strain} item property both the models and the client predicate key on. */
    public static final Identifier STRAIN_PREDICATE = Identifier.of(Hempdustry.MOD_ID, "strain");

    private static Identifier texture(String name) {
        return Identifier.of(Hempdustry.MOD_ID, "item/" + name);
    }

    private static Identifier spliffModel(Strain strain) {
        return Identifier.of(Hempdustry.MOD_ID, "item/spliff_" + strain.id());
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
        generator.writer.accept(modelId, () -> {
            JsonObject json = new JsonObject();
            json.addProperty("parent", "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", texture.toString());
            json.add("textures", textures);

            JsonArray array = new JsonArray();
            for (ModelOverride override : overrides) {
                JsonObject predicate = new JsonObject();
                predicate.addProperty(STRAIN_PREDICATE.toString(), override.threshold());
                JsonObject entry = new JsonObject();
                entry.add("predicate", predicate);
                entry.addProperty("model", override.model().toString());
                array.add(entry);
            }
            json.add("overrides", array);
            return (JsonElement) json;
        });
    }

    private record ModelOverride(int threshold, Identifier model) {
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
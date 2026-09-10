package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.item.ModItemProperties;
import com.warlonmhite.hempdustry.mixin.client.ModelPredicateProviderRegistryAccessor;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.client.color.world.BiomeColors;
import com.warlonmhite.hempdustry.config.HempdustryConfig;

import java.util.HashMap;
import com.warlonmhite.hempdustry.client.UpdateChecker;
import com.warlonmhite.hempdustry.client.render.HempBoatEntityRenderer;
import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreen;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;

public class HempdustryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UpdateChecker.init();

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HEMP_PLANKS_DOOR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HEMP_PLANKS_TRAPDOOR, RenderLayer.getCutout());


        // Vanilla's leaves texture, holes and all — solid would fill them in with black.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.HEMP_LEAVES, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INDICA_CROP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.INDICA_FLOWER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_INDICA_FLOWER, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SATIVA_CROP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SATIVA_FLOWER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POTTED_SATIVA_FLOWER, RenderLayer.getCutout());

        EntityRendererRegistry.register(ModEntities.HEMP_BOAT, ctx -> new HempBoatEntityRenderer(ctx, false));
        EntityRendererRegistry.register(ModEntities.HEMP_CHEST_BOAT, ctx -> new HempBoatEntityRenderer(ctx, true));

        HandledScreens.register(ModScreenHandlers.DECARBOXYLATOR, DecarboxylatorScreen::new);
        HandledScreens.register(ModScreenHandlers.INFUSER, InfuserScreen::new);

        registerItemProperties();
        registerItemColors();
        registerBlockColors();
    }

    /** Neutral white: multiplied into a texel it changes nothing, which is what "no tint" means. */
    private static final int NO_TINT = 0xFFFFFF;

    /**
     * Pulls the living plants part-way towards the grass colour of the biome they stand in, so a
     * field of hemp stops fighting the ground it grows out of.
     *
     * <h2>Why part-way, and not vanilla's tint</h2>
     *
     * Grass, ferns and sugar cane hand vanilla's grass colour straight through, but they are drawn
     * as near-greyscale art <em>meant</em> to be coloured by the biome. Ours are not: the two
     * strains are told apart by their greens — Purple Kush's muted grey-green
     * ({@code #435949}, {@code #4F7460}) against Lemon Haze's bright lime ({@code #549154},
     * {@code #6BB269}) — and a full tint multiplies both down onto the same biome colour, erasing
     * the difference. Blending the tint towards white instead keeps each strain's own hue and only
     * nudges it: at the default 0.35, a sativa leaf reads {@code #478340} in plains and
     * {@code #40863D} in a jungle, and the gold pistils stay gold ({@code #C9891F} / {@code #B68C1D})
     * rather than turning olive.
     *
     * <p>The tint is applied by the <em>model</em>, not here: only faces carrying
     * {@code "tintindex": 0} ask, which is why the crop stage models parent
     * {@code minecraft:block/tinted_cross} and the flowers are datagen'd {@code TintType.TINTED}.
     * The bud, seed and flower <b>items</b> are untouched — their models declare no tint index, so
     * an item in a hand or a slot looks exactly as drawn, with no biome to ask about anyway.
     *
     * <p>Break and step particles need no work: {@code BlockDustParticle} multiplies itself by
     * {@code BlockColors.getColor(state, world, pos, 0)}, so they follow this automatically.
     *
     * <p>Returning ARGB is deliberate but, unlike the item tint above, the alpha byte is
     * <em>ignored</em> here — {@code BlockModelRenderer} reads only the three colour bytes off a
     * block tint. {@link ColorHelper.Argb#lerp} is used for the blend all the same, so the value
     * that leaves this method is a well-formed colour rather than one that only works by luck.
     */
    private static void registerBlockColors() {
        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
            // view/pos are null when a block is coloured outside a world — an inventory render. No
            // biome exists there, so the honest answer is the art as drawn.
            if (tintIndex != 0 || view == null || pos == null) {
                return NO_TINT;
            }
            return biomeTint(BiomeColors.getGrassColor(view, pos));
        }, ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP,
                ModBlocks.INDICA_FLOWER, ModBlocks.SATIVA_FLOWER,
                ModBlocks.POTTED_INDICA_FLOWER, ModBlocks.POTTED_SATIVA_FLOWER);
    }

    /**
     * {@code grassColor} blended towards white by {@code 1 - client.biomeTintStrength}.
     *
     * <p>Strength {@code 0} therefore returns pure white — a multiply by 1.0, so the config's "off"
     * costs a lerp and nothing else, and no branch is needed to honour it.
     */
    private static int biomeTint(int grassColor) {
        return ColorHelper.Argb.lerp(
                (float) HempdustryConfig.get().client().biomeTintStrength(),
                NO_TINT,
                grassColor);
    }

    /**
     * Paints the loaded strain's colour onto the smoking gear.
     *
     * <p>This is what makes a strain a <b>datapack</b> feature rather than a code one. Bespoke art
     * needs a texture, and a datapack cannot ship a texture — so without a tint, every strain added
     * by a datapack renders identically to the first one the mod happens to carry art for. With it,
     * a strain is distinguishable from the colour in its own JSON and nothing else.
     *
     * <p>Only {@link ModItemProperties#LOAD_TINT_INDEX} is tinted; every other layer answers
     * {@code -1} and keeps its own colours. See that constant for why the layer split is the whole
     * mechanism, and note that <b>this is inert until a model actually declares that layer</b> — an
     * item whose model has only {@code layer0} never asks about index 1.
     *
     * <p>Empty contents answer {@code -1} rather than {@link SmokeContents#color}'s white: white is
     * the right neutral for averaging colours together, but as a tint it would be a no-op that still
     * costs a lookup, and returning {@code -1} says "not tinted" outright.
     *
     * <p>The bud items are deliberately <em>not</em> registered here. Their colour cannot come from a
     * component — a bud carries none — so it would have to be found by scanning the strain registry
     * for whichever entry claims that item, which is a per-frame cost for something no model asks for
     * yet. It belongs with the bud art if that lands, not before.
     */
    private static void registerItemColors() {
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex != ModItemProperties.LOAD_TINT_INDEX) {
                return -1;
            }
            SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
            // fullAlpha is load-bearing, not decoration. ItemRenderer feeds this value's ALPHA byte
            // straight into the vertex alpha (ItemRenderer:197), and a strain's colour is 24-bit RGB
            // out of its JSON — so returning it raw means alpha 0x00 and a layer that draws nothing
            // at all. The symptom is a packed device looking exactly like an empty one, with no
            // error anywhere. Every vanilla tint does the same wrap: potions, spawn eggs, maps.
            return contents.isEmpty() ? -1 : ColorHelper.Argb.fullAlpha(contents.color());
        }, ModItems.SPLIFF, ModItems.WOODEN_PIPE, ModItems.BONG);
    }

    /**
     * The two item properties the smoking gear's models key on. They answer different questions and
     * were, until this was split, wrongly answered by one:
     *
     * <ul>
     *   <li>{@code hempdustry:packed} — 0 or 1, "is there anything loaded in here". Drives the empty
     *       ⇄ packed model on a pipe and a bong.</li>
     *   <li>{@code hempdustry:strain} — the loaded strain's {@code model_index}, for the rarer case
     *       of a strain that ships <em>bespoke</em> art rather than taking the shared tinted look.</li>
     * </ul>
     *
     * <h2>Why they had to be split</h2>
     *
     * The pipe and bong used to discriminate empty from packed with {@code strain >= 1}, which only
     * worked because every strain happened to carry a non-zero index. Now that {@code model_index}
     * {@code 0} means "no bespoke art, use the shared look" — the normal case — that test would match
     * nothing and a packed pipe would render as an empty one. "Is it loaded" is a different question
     * from "whose art", so it gets its own property.
     *
     * <p>The spliff deliberately has no {@code packed} property. <b>A spliff is not a device</b>: it
     * is consumed whole rather than emptied, so there is no legitimate unpacked spliff to render —
     * the empty equivalent is just paper.
     *
     * <h2>Why {@code strain} is registered the hard way</h2>
     *
     * {@code ModelPredicateProviderRegistry.register} only accepts a {@code ClampedModelPredicate-
     * Provider}, which pins its result to 0..1 — so any index above 1 collapsed onto the first
     * strain's override. {@code packed} is unaffected, being 0 or 1 by construction, and uses the
     * public call. {@code strain} goes through
     * {@link ModelPredicateProviderRegistryAccessor} instead. See that class for the full reasoning.
     *
     * <p>Both properties are registered on the devices even where the mod ships no override for them,
     * because that is what lets a <b>resource pack</b> add per-strain packed art without any code:
     * overrides match with {@code >=}, so a pack lists them ascending exactly as {@code
     * ModModelProvider} does.
     */
    private static void registerItemProperties() {
        Identifier strainId = ModItemProperties.STRAIN;
        ModelPredicateProvider strain = (stack, world, entity, seed) -> {
            RegistryEntry<Strain> loaded =
                    stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).primaryStrain();
            return loaded == null ? 0f : loaded.value().modelIndex();
        };
        registerUnclamped(ModItems.SPLIFF, strainId, strain);
        registerUnclamped(ModItems.WOODEN_PIPE, strainId, strain);
        registerUnclamped(ModItems.BONG, strainId, strain);

        Identifier packedId = ModItemProperties.PACKED;
        ClampedModelPredicateProvider packed = (stack, world, entity, seed) ->
                stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).isEmpty() ? 0f : 1f;
        ModelPredicateProviderRegistry.register(ModItems.WOODEN_PIPE, packedId, packed);
        ModelPredicateProviderRegistry.register(ModItems.BONG, packedId, packed);
    }

    /**
     * Registers an item property that may return a value above 1, which
     * {@link ModelPredicateProviderRegistry#register} cannot express. Mirrors what that method does
     * — {@code computeIfAbsent} then {@code put} — straight onto the backing map.
     */
    private static void registerUnclamped(Item item, Identifier id, ModelPredicateProvider provider) {
        ModelPredicateProviderRegistryAccessor.getItemSpecific()
                .computeIfAbsent(item, key -> new HashMap<>())
                .put(id, provider);
    }
}

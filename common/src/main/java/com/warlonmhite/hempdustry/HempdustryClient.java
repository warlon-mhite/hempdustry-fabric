package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.client.render.FlipFlopsModel;
import com.warlonmhite.hempdustry.item.ModItemProperties;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.client.item.StrainModelIndexProperty;
import com.warlonmhite.hempdustry.client.item.StrainTintSource;
import com.warlonmhite.hempdustry.config.HempdustryConfig;

import com.warlonmhite.hempdustry.client.UpdateChecker;
import com.warlonmhite.hempdustry.client.particle.AromaParticle;
import com.warlonmhite.hempdustry.client.sound.BongDrawSoundInstance;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.client.render.HempBoatEntityRenderer;
import com.warlonmhite.hempdustry.client.render.HempPressBlockEntityRenderer;
import com.warlonmhite.hempdustry.client.render.ModEntityModelLayers;
import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.particle.ModParticles;
import com.warlonmhite.hempdustry.platform.client.ClientRegistration;
import com.warlonmhite.hempdustry.platform.client.ClientServices;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreen;
import com.warlonmhite.hempdustry.screen.custom.HempPressScreen;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreen;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class HempdustryClient {

    /** Everything the client registers, in order; each loader's client entry point calls it once. */
    public static void init() {
        ClientRegistration client = ClientServices.CLIENT;
        UpdateChecker.init();

        // Which pass a block is drawn in -- solid, cut-out or translucent -- is read off its textures
        // since 26.1, quad by quad: the door's and the leaves' holes, the bongs' stained glass, the
        // Grow Lamp's chains and the pot's compost crumbs each pick their own, with nothing to declare.

        client.modelLayer(ModEntityModelLayers.HEMP_BOAT,
                BoatModel::createBoatModel);
        client.modelLayer(ModEntityModelLayers.HEMP_CHEST_BOAT,
                BoatModel::createChestBoatModel);
        client.entityRenderer(ModEntities.HEMP_BOAT, ctx -> new HempBoatEntityRenderer(ctx, false));
        client.entityRenderer(ModEntities.HEMP_CHEST_BOAT, ctx -> new HempBoatEntityRenderer(ctx, true));

        // Flip-flops are drawn by a model of their own rather than as boots (the beanie needs no
        // code: vanilla draws a head item with no equipment model as its item model).
        client.modelLayer(ModEntityModelLayers.FLIP_FLOPS, FlipFlopsModel::createLayer);

        client.menuScreen(ModScreenHandlers.DECARBOXYLATOR, DecarboxylatorScreen::new);
        client.menuScreen(ModScreenHandlers.INFUSER, InfuserScreen::new);
        client.menuScreen(ModScreenHandlers.HEMP_PRESS, HempPressScreen::new);

        client.particleProvider(ModParticles.AROMA, AromaParticle.Factory::new);

        // The press's platen and capstan move, so a renderer draws them rather than the block model.
        client.blockEntityRenderer(ModBlockEntities.HEMP_PRESS, HempPressBlockEntityRenderer::new);

        registerItemModelHooks(client);
        registerBlockColors(client);

        // A bong's bubbling is played here, per client, so it can stop when the drawer lets go.
        SmokingDeviceItem.drawSound = drawer -> Minecraft.getInstance().getSoundManager()
                .play(new BongDrawSoundInstance(drawer));
    }

    /** Neutral white: multiplied into a texel it changes nothing, which is what "no tint" means. */
    private static final int NO_TINT = 0xFFFFFFFF;
    /** Where a lamp-grown plant's tint is pulled: a deeper, bluer green. */
    private static final int LAMP_GROWN_TINT = 0xFF3C7A56;

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
     * {@code hempdustry:block/tinted_crop} and the flowers are datagen'd {@code TintType.TINTED}.
     * The bud, seed and flower <b>items</b> are untouched — their models declare no tint index, so
     * an item in a hand or a slot looks exactly as drawn, with no biome to ask about anyway.
     *
     * <p>Break and step particles follow this automatically — a tint source's
     * {@code colorAsTerrainParticle} is its {@code colorInWorld} unless it says otherwise — <b>but
     * they are coloured with the broken state, not the world's</b>: by then {@code pos} may hold air
     * on this client. So a source here reads the plant off {@code state} and asks the world for
     * nothing but its biome. Outside a world ({@code color}, the general context) there is no biome,
     * and the honest answer is the art as drawn.
     *
     * <p>Every colour is opaque ARGB, as vanilla's own tint sources return; {@link ARGB#srgbLerp}
     * does the blend, so the value that leaves here is a well-formed colour.
     */
    private static void registerBlockColors(ClientRegistration client) {
        client.blockTint(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return NO_TINT;
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter view, BlockPos pos) {
                return biomeTint(BiomeColors.getAverageGrassColor(view, pos));
            }
        }), ModBlocks.INDICA_FLOWER, ModBlocks.SATIVA_FLOWER,
                ModBlocks.POTTED_INDICA_FLOWER, ModBlocks.POTTED_SATIVA_FLOWER);
        // The crops take the same biome tint, then show the light they grew under: a lamp-grown
        // plant a shade deeper. A STRESSED plant is not tinted at all — it has textures of its own,
        // because a tint is a multiply and a multiply cannot bleach Purple Kush's dark flowers; the
        // blockstate picks those, and a second yellow cue on top would only muddy them.
        // Every segment carries a copy of the record, so this reads the state it was handed and never
        // the world: that is also the broken state a break particle is coloured with.
        client.blockTint(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return NO_TINT;
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter view, BlockPos pos) {
                // Beldía is drawn in its own colours, so it skips the biome and keeps only the light
                // cue: a desert's grass colour is a dry olive that would turn a sand-grown plant to
                // straw (and a potted one would change colour with the room it stands in).
                int tint = state.is(ModBlocks.BELDIA_CROP) ? NO_TINT : biomeTint(BiomeColors.getAverageGrassColor(view, pos));
                GrowLight light = state.getValue(GrowLight.PROPERTY);
                return light.artificial() && light != GrowLight.STRESSED
                        ? ARGB.srgbLerp(0.25F, tint, LAMP_GROWN_TINT) : tint;
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return Set.of(GrowLight.PROPERTY);
            }
        }), ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP, ModBlocks.BELDIA_CROP);

        // A packed bong set down shows its load on tint index 1. A block tint can ask the world and
        // not the stack, so it is one dried-bud green whatever the strain; the strain's own colour
        // is the hand's, where the item definition has the stack to read.
        client.blockTint(List.of(state -> NO_TINT, state -> PLACED_LOAD),
                ModBlocks.DEVICE_BLOCKS.values().toArray(Block[]::new));
    }

    private static final int PLACED_LOAD = 0xFF6F8F3A;

    /**
     * {@code grassColor} blended towards white by {@code 1 - client.biomeTintStrength}.
     *
     * <p>Strength {@code 0} therefore returns pure white — a multiply by 1.0, so the config's "off"
     * costs a lerp and nothing else, and no branch is needed to honour it.
     */
    private static int biomeTint(int grassColor) {
        return ARGB.srgbLerp(
                (float) HempdustryConfig.get().client().biomeTintStrength(),
                NO_TINT,
                grassColor);
    }

    /**
     * The two hooks the smoking gear's client item definitions name.
     *
     * <p>Since 1.21.4 an item's look is data — {@code assets/hempdustry/items/*.json} — and code's
     * job is only to make the pieces those files reference exist: a numeric property they can
     * dispatch on, and a tint source they can list. Everything the old
     * {@code ModelPredicateProviderRegistry} pair did is now expressed there instead, which is why
     * this mod no longer needs an accessor mixin to reach a private map.
     *
     * <p>{@code hempdustry:packed} is gone with no replacement needed: vanilla's own
     * {@code minecraft:has_component} answers "is anything loaded" exactly.
     */
    private static void registerItemModelHooks(ClientRegistration client) {
        client.rangeSelectProperty(ModItemProperties.STRAIN, StrainModelIndexProperty.CODEC);
        client.itemTintSource(ModItemProperties.STRAIN_TINT, StrainTintSource.CODEC);
    }
}

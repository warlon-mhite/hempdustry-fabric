package com.warlonmhite.hempdustry;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.item.ModItemProperties;
import com.warlonmhite.hempdustry.config.HempdustryConfig;

import java.util.ArrayList;
import java.util.List;
import com.warlonmhite.hempdustry.client.UpdateChecker;
import com.warlonmhite.hempdustry.client.particle.AromaParticle;
import com.warlonmhite.hempdustry.client.sound.BongDrawSoundInstance;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.client.render.HempBoatEntityRenderer;
import com.warlonmhite.hempdustry.client.render.HempBedBlockEntityRenderer;
import com.warlonmhite.hempdustry.client.render.HempPressBlockEntityRenderer;
import com.warlonmhite.hempdustry.client.render.CompositeItemModel;
import com.warlonmhite.hempdustry.client.render.HempBeanieRenderer;
import com.warlonmhite.hempdustry.component.StackRegistries;
import com.warlonmhite.hempdustry.item.ModArmorMaterials;
import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.particle.ModParticles;
import com.warlonmhite.hempdustry.platform.client.ClientRegistration;
import com.warlonmhite.hempdustry.platform.client.ClientServices;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import com.warlonmhite.hempdustry.screen.custom.DecarboxylatorScreen;
import com.warlonmhite.hempdustry.screen.custom.HempPressScreen;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import com.warlonmhite.hempdustry.registry.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.level.block.Block;
import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;
import com.warlonmhite.hempdustry.registry.DataRegistries;

public class HempdustryClient {

    /** Everything the client registers, in order; each loader's client entry point calls it once. */
    public static void init() {
        ClientRegistration client = ClientServices.CLIENT;
        UpdateChecker.init();
        // A stack's strain resolves, on the client, against the registries its server sent.
        StackRegistries.client(() -> Minecraft.getInstance().getConnection() != null);

        client.blockLayer(ModBlocks.HEMP_PLANKS_DOOR, RenderType.cutout());
        client.blockLayer(ModBlocks.HEMP_PLANKS_TRAPDOOR, RenderType.cutout());


        // Vanilla's leaves texture, holes and all — solid would fill them in with black.
        client.blockLayer(ModBlocks.HEMP_LEAVES, RenderType.cutout());

        client.blockLayer(ModBlocks.INDICA_CROP, RenderType.cutout());
        client.blockLayer(ModBlocks.INDICA_FLOWER, RenderType.cutout());
        client.blockLayer(ModBlocks.POTTED_INDICA_FLOWER, RenderType.cutout());

        // Stained glass is drawn at partial alpha, so the placed bongs need the translucent pass.
        ModBlocks.DEVICE_BLOCKS.values().forEach(bong -> client.blockLayer(bong, RenderType.translucent()));

        client.blockLayer(ModBlocks.SATIVA_CROP, RenderType.cutout());
        client.blockLayer(ModBlocks.SATIVA_FLOWER, RenderType.cutout());
        client.blockLayer(ModBlocks.POTTED_SATIVA_FLOWER, RenderType.cutout());

        client.blockLayer(ModBlocks.BELDIA_CROP, RenderType.cutout());
        // The chains the Grow Lamp hangs from are see-through between the links.
        client.blockLayer(ModBlocks.GROW_LAMP, RenderType.cutout());
        // The compost crumbs over the pot's soil are drawn with see-through pixels.
        client.blockLayer(ModBlocks.GROW_POT, RenderType.cutout());

        client.entityRenderer(ModEntities.HEMP_BOAT, HempBoatEntityRenderer::new);

        client.menuScreen(ModScreenHandlers.DECARBOXYLATOR, DecarboxylatorScreen::new);
        client.menuScreen(ModScreenHandlers.INFUSER, InfuserScreen::new);
        client.menuScreen(ModScreenHandlers.HEMP_PRESS, HempPressScreen::new);

        client.particleProvider(ModParticles.AROMA, AromaParticle.Factory::new);

        // The press's platen and capstan move, so a renderer draws them rather than the block model.
        client.blockEntityRenderer(ModBlockEntities.HEMP_PRESS, HempPressBlockEntityRenderer::new);
        // A bed has no block model: its block entity draws it, like every vanilla bed. Its item is drawn
        // by the same renderer through each loader's item hook (HempBedBlockEntityRenderer#renderItem).
        client.blockEntityRenderer(ModBlockEntities.HEMP_BED, HempBedBlockEntityRenderer::new);
        // 1.16.5's beds atlas holds only the sixteen vanilla beds unless asked for more.
        client.atlasSprite(Sheets.BED_SHEET, HempBedBlockEntityRenderer.TEXTURE.texture());
        // ...and they are models no block state names, so they have to be asked for by id. So are the
        // bongs' 3D hand models, which no item names either: ItemRendererMixin draws them.
        client.extraModels(HempPressBlockEntityRenderer.PARTS);
        client.extraModels(ModItems.bongs().stream().map(ModItemProperties::inHandModel).collect(Collectors.toList()));
        client.extraModels(ImmutableList.of(HempBeanieRenderer.BAND, HempBeanieRenderer.CROWN));
        // The press's icon is its four part models at rest, put together as it bakes -- the
        // composite item model the 1.21.11 build names in data. Its own model, which parents the
        // frame's, gives the display transforms.
        client.composedModel(HempPressBlockEntityRenderer.ICON, HempPressBlockEntityRenderer.ICON_PARTS,
                CompositeItemModel::new);

        registerItemProperties(client);
        registerItemColors(client);

        registerBlockColors(client);

        // A bong's bubbling is played here, per client, so it can stop when the drawer lets go.
        SmokingDeviceItem.drawSound = drawer -> Minecraft.getInstance().getSoundManager()
                .play(new BongDrawSoundInstance(drawer));
    }

    /** Neutral white: multiplied into a texel it changes nothing, which is what "no tint" means. */
    private static final int NO_TINT = 0xFFFFFF;
    /** Where a lamp-grown plant's tint is pulled: a deeper, bluer green. */
    private static final int LAMP_GROWN_TINT = 0x3C7A56;

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
     * {@code hempdustry:block/tinted_crop} and the flowers are datagen'd {@code TintState.TINTED}.
     * The bud, seed and flower <b>items</b> are untouched — their models declare no tint index, so
     * an item in a hand or a slot looks exactly as drawn, with no biome to ask about anyway.
     *
     * <p>Break and step particles follow this automatically — {@code TerrainParticle} multiplies
     * itself by {@code BlockColors.getColor(state, world, pos, 0)} — <b>but they are coloured with
     * the broken state, not the world's</b>: by then {@code pos} may hold air on this client. So a
     * provider here reads the plant off {@code state} and asks the world for nothing but its biome.
     *
     * <p>Returning ARGB is deliberate but, unlike the item tint above, the alpha byte is
     * <em>ignored</em> here — {@code ModelBlockRenderer} reads only the three colour bytes off a
     * block tint. {@link #lerp} blends all four bytes all the same, so the value
     * that leaves this method is a well-formed colour rather than one that only works by luck.
     */
    private static void registerBlockColors(ClientRegistration client) {
        client.blockColor((state, view, pos, tintIndex) -> {
            // view/pos are null when a block is coloured outside a world — an inventory render. No
            // biome exists there, so the honest answer is the art as drawn.
            if (tintIndex != 0 || view == null || pos == null) {
                return NO_TINT;
            }
            return biomeTint(BiomeColors.getAverageGrassColor(view, pos));
        }, ModBlocks.INDICA_FLOWER, ModBlocks.SATIVA_FLOWER,
                ModBlocks.POTTED_INDICA_FLOWER, ModBlocks.POTTED_SATIVA_FLOWER);
        // The crops take the same biome tint, then show the light they grew under: a lamp-grown
        // plant a shade deeper. A STRESSED plant is not tinted at all — it has textures of its own,
        // because a tint is a multiply and a multiply cannot bleach Purple Kush's dark flowers; the
        // blockstate picks those, and a second yellow cue on top would only muddy them.
        // Every segment carries a copy of the record, so this reads the state it was handed and never
        // the world: that is also the broken state a break particle is coloured with.
        client.blockColor((state, view, pos, tintIndex) -> {
            if (tintIndex != 0 || view == null || pos == null) {
                return NO_TINT;
            }
            // Beldía is drawn in its own colours, so it skips the biome and keeps only the light
            // cue: a desert's grass colour is a dry olive that would turn a sand-grown plant to straw
            // (and a potted one would change colour with the room it stands in).
            int tint = state.is(ModBlocks.BELDIA_CROP) ? NO_TINT : biomeTint(BiomeColors.getAverageGrassColor(view, pos));
            GrowLight light = state.getValue(GrowLight.PROPERTY);
            return light.artificial() && light != GrowLight.STRESSED
                    ? lerp(0.25F, tint, LAMP_GROWN_TINT) : tint;
        }, ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP, ModBlocks.BELDIA_CROP);

        // A packed bong set down shows its load on tint index 1. A block tint can ask the world and
        // not the stack, so it is one dried-bud green whatever the strain; the strain's own colour
        // is the hand's, where the item definition has the stack to read.
        client.blockColor((state, view, pos, tintIndex) ->
                        tintIndex == 1 ? PLACED_LOAD : NO_TINT,
                ModBlocks.DEVICE_BLOCKS.values().toArray(new Block[0]));
    }

    private static final int PLACED_LOAD = 0x6F8F3A;

    /**
     * Each byte of {@code from} moved {@code delta} of the way to {@code to}: what later versions
     * ship as {@code FastColor.ARGB32.lerp}, written out the same way, which 1.16.5 does not have.
     */
    private static int lerp(float delta, int from, int to) {
        return FastColor.ARGB32.color(
                lerpByte(delta, FastColor.ARGB32.alpha(from), FastColor.ARGB32.alpha(to)),
                lerpByte(delta, FastColor.ARGB32.red(from), FastColor.ARGB32.red(to)),
                lerpByte(delta, FastColor.ARGB32.green(from), FastColor.ARGB32.green(to)),
                lerpByte(delta, FastColor.ARGB32.blue(from), FastColor.ARGB32.blue(to)));
    }

    private static int lerpByte(float delta, int from, int to) {
        return from + Mth.floor(delta * (float) (to - from));
    }

    /**
     * {@code grassColor} blended towards white by {@code 1 - client.biomeTintStrength}.
     *
     * <p>Strength {@code 0} therefore returns pure white — a multiply by 1.0, so the config's "off"
     * costs a lerp and nothing else, and no branch is needed to honour it.
     */
    private static int biomeTint(int grassColor) {
        return lerp(
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
    private static void registerItemColors(ClientRegistration client) {
        client.itemColor((stack, tintIndex) -> {
            if (tintIndex != ModItemProperties.LOAD_TINT_INDEX) {
                return -1;
            }
            SmokeContents contents = ModComponents.SMOKE_CONTENTS.getOrDefault(stack, SmokeContents.EMPTY);
            // The opaque alpha is load-bearing, not decoration. ItemRenderer feeds this value's ALPHA byte
            // straight into the vertex alpha (ItemRenderer:197), and a strain's colour is 24-bit RGB
            // out of its JSON — so returning it raw means alpha 0x00 and a layer that draws nothing
            // at all. The symptom is a packed device looking exactly like an empty one, with no
            // error anywhere. Every vanilla tint does the same wrap: potions, spawn eggs, maps.
            return contents.isEmpty() ? -1 : 0xFF000000 | contents.color();
        }, tinted());
        // The two garments' icons, as leather's are tinted: the grey cloth (layer 0) takes the dye,
        // or unbleached hemp undyed, and the drawn layer on top stays as drawn.
        client.itemColor((stack, tintIndex) -> tintIndex != 0 ? -1
                        : ((DyeableLeatherItem) stack.getItem()).getColor(stack),
                ModItems.HEMP_SHIRT, ModItems.HEMP_HAREM_PANTS);
    }

    /**
     * The two item properties the smoking gear's models key on. They answer different questions and
     * were, until this was split, wrongly answered by one:
     *
     * <ul>
     *   <li>{@code hempdustry:packed} — 0 or 1, "is there anything loaded in here". Drives the empty
     *       ⇄ packed model on every device.</li>
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
     * <h2>Why {@code strain} is not clamped</h2>
     *
     * From 1.17 on, {@code ItemProperties.register} only accepts a {@code ClampedItemProperty-
     * Function}, which pins its result to 0..1 — so any index above 1 collapsed onto the first
     * strain's override, and the newer lines put the property on vanilla's map directly. 1.16.5
     * never clamps: every property goes on as given ({@link ClientRegistration#itemProperty}), so
     * {@code strain} needs nothing, and {@code draw} clamps itself as the newer lines' clamped
     * function does.
     *
     * <p>Both properties are registered on the devices even where the mod ships no override for them,
     * because that is what lets a <b>resource pack</b> add per-strain packed art without any code:
     * overrides match with {@code >=}, so a pack lists them ascending exactly as {@code
     * ModModelProvider} does.
     */
    private static void registerItemProperties(ClientRegistration client) {
        ResourceLocation strainId = ModItemProperties.STRAIN;
        ItemPropertyFunction strain = (stack, world, entity) -> {
            Holder<Strain> loaded =
                    ModComponents.SMOKE_CONTENTS.getOrDefault(stack, SmokeContents.EMPTY).primaryStrain();
            return loaded == null ? 0f : loaded.value().modelIndex();
        };
        for (Item item : smokingGear()) {
            client.itemProperty(item, strainId, strain);
        }

        ResourceLocation packedId = ModItemProperties.PACKED;
        ItemPropertyFunction packed = (stack, world, entity) ->
                ModComponents.SMOKE_CONTENTS.getOrDefault(stack, SmokeContents.EMPTY).isEmpty() ? 0f : 1f;
        for (Item device : everyDevice()) {
            client.itemProperty(device, packedId, packed);
        }

        // The bong's draw, for its 3D hand model: the bow's pulling and pull, measured against the
        // draw's own length rather than a bow's twenty ticks.
        ItemPropertyFunction drawing = (stack, world, entity) ->
                entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f;
        ItemPropertyFunction draw = (stack, world, entity) -> {
            if (entity == null || entity.getUseItem() != stack
                    || !(stack.getItem() instanceof SmokingDeviceItem device) || device.device().drawTicks() <= 0) {
                return 0f;
            }
            return Mth.clamp((stack.getUseDuration() - entity.getUseItemRemainingTicks())
                    / (float) device.device().drawTicks(), 0.0F, 1.0F);
        };
        for (Item bong : ModItems.bongs()) {
            client.itemProperty(bong, ModItemProperties.DRAWING, drawing);
            client.itemProperty(bong, ModItemProperties.DRAW, draw);
        }
    }

    /**
     * The spliff and every device. On this line the colour and the two model properties are
     * registered per item, so a device missing here draws untinted and never shows as packed — no
     * error, just the wrong picture. Listing the devices by name is how a vaporizer would have been
     * left out.
     */
    private static Item[] smokingGear() {
        List<Item> items = new ArrayList<>();
        items.add(ModItems.SPLIFF);
        items.addAll(everyDevice());
        return items.toArray(new Item[0]);
    }

    /**
     * {@link ModItems#devices()} and the coloured bongs, which that map leaves out on purpose (one
     * canonical item per device) but which pack, tint and switch models like any other bong.
     */
    private static List<Item> everyDevice() {
        List<Item> devices = new ArrayList<>(ModItems.devices().values());
        devices.addAll(ModItems.COLORED_BONGS);
        return devices;
    }

    /**
     * Everything whose load mask takes the strain's colour: the smoking gear, and the moon rock,
     * one item for every strain with the bud showing through its crust in that strain's colour.
     */
    private static Item[] tinted() {
        List<Item> items = new ArrayList<>(ImmutableList.copyOf(smokingGear()));
        items.add(ModItems.MOON_ROCK);
        return items.toArray(new Item[0]);
    }
}

package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.RipeAroma;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import com.warlonmhite.hempdustry.particle.ModParticles;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.strain.Terpene;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A terpene is a smell, a word and a colour, and nothing else: every plant strain names one of its
 * own, the smoke and the vapour take a share of its colour, the ripe plant's wisp too, and a strain
 * naming a terpene nobody defined still loads and smokes plain.
 */
public final class TerpeneGameTest {

    private static final int WHITE = 0xFFFFFF;

    /** The ten, with the colours the spec gives them. A pack may recolour one; the mod's own stay put. */
    private static final Map<ResourceKey<Terpene>, Integer> COLOURS = new LinkedHashMap<>();

    static {
        COLOURS.put(Terpene.LIMONENE, 0xF5D547);
        COLOURS.put(Terpene.LINALOOL, 0xB9A0F2);
        COLOURS.put(Terpene.MYRCENE, 0xE8923A);
        COLOURS.put(Terpene.CARYOPHYLLENE, 0xD0603F);
        COLOURS.put(Terpene.PINENE, 0x6DB360);
        COLOURS.put(Terpene.EUCALYPTOL, 0x3FA7C4);
        COLOURS.put(Terpene.TERPINOLENE, 0xA7D84E);
        COLOURS.put(Terpene.GERANIOL, 0xF592B8);
        COLOURS.put(Terpene.HUMULENE, 0xB3AE5C);
        COLOURS.put(Terpene.NEROLIDOL, 0xB0835A);
    }

    /** The ten load with their colours; each plant strain names its own; schwag and hash do not. */
    public static void everyPlantStrainSmellsOfItsOwnTerpene(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();

        context.assertValueEqual(registries.lookupOrThrow(Terpene.REGISTRY_KEY).listElements().count(), 10L,
                "the terpenes loaded");
        COLOURS.forEach((key, colour) -> {
            Optional<Terpene> terpene = Terpene.resolve(registries, key);
            context.assertTrue(terpene.isPresent(), key.identifier() + " did not load");
            context.assertValueEqual(terpene.get().color(), colour, key.identifier() + "'s colour");
            context.assertValueEqual(terpene.get().translationKey(), "hempdustry.terpene." + key.identifier().getPath(),
                    key.identifier() + "'s translation key");
        });

        Map<ResourceKey<Terpene>, String> namedBy = new HashMap<>();
        int plants = 0;
        for (Holder.Reference<Strain> strain : Strain.all(registries)) {
            String id = strain.key().identifier().toString();
            Strain value = strain.value();
            if (value.wildFlower().isEmpty()) {
                // Hash, rosin, scorched hemp and schwag are no plant: no smell of their own.
                context.assertTrue(value.terpene().isEmpty(), id + " is no plant but names a terpene");
                continue;
            }
            plants++;
            context.assertTrue(value.terpene().isPresent(), id + " names no terpene");
            ResourceKey<Terpene> key = value.terpene().get();
            context.assertTrue(value.loadedTerpene(registries).isPresent(), id + "'s " + key.identifier() + " does not resolve");
            String other = namedBy.put(key, id);
            context.assertTrue(other == null, id + " and " + other + " share " + key.identifier());
        }
        Sweep.atLeast(context, "plant strains", plants, 9);

        context.assertValueEqual(strain(world, ModStrains.SCHWAG).value().smokeTint(registries), Optional.of(0xA89A6E),
                "schwag's smoke, stale hay");
        for (ResourceKey<Strain> plain : List.of(ModStrains.HASHISH, ModStrains.CHARAS, ModStrains.FILTERED_HASHISH,
                ModStrains.ROSIN, ModStrains.SCORCHED_HEMP)) {
            context.assertValueEqual(strain(world, plain).value().smokeTint(registries), Optional.<Integer>empty(),
                    plain.identifier() + "'s smoke tint");
        }

        // The buds' tooltip names it, in its colour, after the chemotype word.
        Strain kush = strain(world, ModStrains.INDICA).value();
        List<Component> lines = new ItemStack(kush.buds()).getTooltipLines(Item.TooltipContext.of(world), null, TooltipFlag.NORMAL);
        Optional<Component> linalool = lines.stream()
                .filter(line -> line.getContents() instanceof TranslatableContents translatable
                        && translatable.getKey().equals("hempdustry.terpene.linalool"))
                .findFirst();
        context.assertTrue(linalool.isPresent(), "Purple Kush's buds don't name linalool: " + lines);
        context.assertValueEqual(linalool.get().getStyle().getColor().getValue(), 0xB9A0F2, "linalool's tooltip colour");

        // The fibre family is no strain, and still smells of humulene: Industrial Hemp's seeds and Ditchweed.
        assertNames(context, world, new ItemStack(ModItems.INDUSTRIAL_HEMP_SEEDS), "hempdustry.chemotype.cbd_dominant");
        assertNames(context, world, new ItemStack(ModBlocks.DITCHWEED), "hempdustry.chemotype.balanced");
        context.succeed();
    }

    /** The exhale and the ripe plant's wisp take the strain's colour; a load with none stays plain. */
    public static void theSmokeTakesTheTerpenesColour(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> kush = strain(world, ModStrains.INDICA);
        int linalool = COLOURS.get(Terpene.LINALOOL);

        assertTint(context, Smoking.tinted(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, SmokeContents.of(kush, 3)),
                ModParticles.TERPENE_SMOKE, Terpene.mix(WHITE, linalool, Terpene.SMOKE_SHARE), "a Purple Kush bong's exhale");
        assertTint(context, Smoking.tinted(world, ParticleTypes.CLOUD, SmokeContents.of(kush, 2)),
                ModParticles.TERPENE_VAPOUR, Terpene.mix(WHITE, linalool, Terpene.VAPOUR_SHARE), "a Purple Kush vaporizer's");
        assertTint(context, Smoking.tinted(world, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        SmokeContents.of(strain(world, ModStrains.SCHWAG), 1)),
                ModParticles.TERPENE_SMOKE, Terpene.mix(WHITE, 0xA89A6E, Terpene.SMOKE_SHARE), "a schwag pipe's exhale");
        context.assertTrue(Smoking.tinted(world, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        SmokeContents.of(strain(world, ModStrains.HASHISH), 2)) == ParticleTypes.CAMPFIRE_COSY_SMOKE,
                "a hash bowl's exhale is not the plain smoke");

        // A pack's strain naming a terpene no pack defines loads, and smokes plain.
        JsonObject json = Strain.CODEC.encodeStart(JsonOps.INSTANCE, kush.value()).getOrThrow().getAsJsonObject();
        json.addProperty("terpene", "hempdustry-gametest:missing");
        Strain stray = Strain.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        context.assertValueEqual(stray.terpene().map(key -> key.identifier()),
                Optional.of(Identifier.fromNamespaceAndPath("hempdustry-gametest", "missing")), "the stray strain's terpene key");
        context.assertTrue(Smoking.tinted(world, ParticleTypes.CLOUD, SmokeContents.of(Holder.direct(stray), 2)) == ParticleTypes.CLOUD,
                "a strain naming a missing terpene does not vape plain");

        // Every plant strain's crop gives off its own wisp; the fibre crop, which no strain names, humulene's.
        for (Holder.Reference<Strain> strain : Strain.all(world.registryAccess())) {
            Optional<Item> seeds = strain.value().seedItem();
            Optional<Terpene> terpene = strain.value().loadedTerpene(world.registryAccess());
            if (seeds.isEmpty() || !(seeds.get() instanceof BlockItem blockItem) || terpene.isEmpty()) {
                continue;
            }
            assertTint(context, RipeAroma.wisp(world, blockItem.getBlock()), ModParticles.TERPENE_AROMA,
                    Terpene.mix(WHITE, terpene.get().color(), Terpene.SMOKE_SHARE),
                    strain.key().identifier() + "'s ripe wisp");
        }
        assertTint(context, RipeAroma.wisp(world, ModBlocks.INDUSTRIAL_HEMP_CROP), ModParticles.TERPENE_AROMA,
                Terpene.mix(WHITE, COLOURS.get(Terpene.HUMULENE), Terpene.SMOKE_SHARE), "Industrial Hemp's ripe wisp");
        context.assertTrue(RipeAroma.wisp(world, net.minecraft.world.level.block.Blocks.WHEAT) == ModParticles.AROMA,
                "a plant with no terpene (wheat) does not get the plain aroma");
        context.succeed();
    }

    /** The stack's tooltip names {@code chemotypeKey}, then humulene in its colour. */
    private static void assertNames(GameTestHelper context, ServerLevel world, ItemStack stack, String chemotypeKey) {
        List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(world), null, TooltipFlag.NORMAL);
        List<String> keys = lines.stream()
                .map(line -> line.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "")
                .toList();
        int chemotype = keys.indexOf(chemotypeKey);
        int humulene = keys.indexOf("hempdustry.terpene.humulene");
        context.assertTrue(chemotype > 0 && humulene == chemotype + 1,
                stack.getItem() + "'s tooltip does not read " + chemotypeKey + " then humulene: " + keys);
        context.assertValueEqual(lines.get(humulene).getStyle().getColor().getValue(), COLOURS.get(Terpene.HUMULENE),
                "humulene's tooltip colour on " + stack.getItem());
    }

    private static void assertTint(GameTestHelper context, ParticleOptions particle, ParticleType<ColorParticleOption> type,
                                   int rgb, String what) {
        context.assertTrue(particle instanceof ColorParticleOption, what + " is " + particle + ", not tinted");
        ColorParticleOption tinted = (ColorParticleOption) particle;
        context.assertTrue(tinted.getType() == type, what + " is " + tinted.getType() + ", not " + type);
        int expected = ARGB.opaque(rgb);
        int got = ARGB.color(Math.round(tinted.getAlpha() * 255), Math.round(tinted.getRed() * 255),
                Math.round(tinted.getGreen() * 255), Math.round(tinted.getBlue() * 255));
        context.assertValueEqual(Integer.toHexString(got), Integer.toHexString(expected), what + "'s colour");
    }

    private static Holder<Strain> strain(ServerLevel world, ResourceKey<Strain> key) {
        return world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }
}

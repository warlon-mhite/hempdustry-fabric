package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.DecarboxylatingRecipe;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * What a potency band is worth beyond the oven: in a bowl the level rounds down and the exact THC buys
 * time (THC-labelled effects only, at most twice); the strain's base is the one count the oven and the
 * bowl share; a moon rock counts its bud's band once; a bowl's CBD sets how long a CBD-labelled effect
 * lasts. Mid is 2.1 exactly throughout.
 */
public final class BandWorthGameTest {

    /** A device duration every stretch in this file divides evenly. */
    private static final int TICKS = 800;

    /**
     * THC-labelled effects last the device's duration × (1 + (D − b) ÷ 2), at most × 2; strain- and
     * smoke-labelled ones keep the bud count rule, so a band never lengthens Beldía's Invisibility.
     */
    public static void aBandBuysTimeNotLevel(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> kush = strain(world, ModStrains.INDICA);
        Holder<Strain> beldia = strain(world, ModStrains.BELDIA);

        // Mid is 2.1 exactly: one bud and a pipe of two last the device's time, a bong of three half again.
        expect(context, kush, 1, PotencyBand.MID, MobEffects.HUNGER, 800);
        expect(context, kush, 2, PotencyBand.MID, MobEffects.HUNGER, 800);
        expect(context, kush, 3, PotencyBand.MID, MobEffects.HUNGER, 1200);
        // High buys an eighth, a quarter, seven eighths; low takes an eighth from one bud.
        expect(context, kush, 1, PotencyBand.HIGH, MobEffects.HUNGER, 900);
        expect(context, kush, 2, PotencyBand.HIGH, MobEffects.HUNGER, 1000);
        expect(context, kush, 3, PotencyBand.HIGH, MobEffects.HUNGER, 1500);
        expect(context, kush, 1, PotencyBand.LOW, MobEffects.HUNGER, 700);
        expect(context, kush, 2, PotencyBand.LOW, MobEffects.HUNGER, 1000);
        // Extra-high pipe of two: dose 3, level III and a half again; a bong of three stops at twice.
        expect(context, kush, 2, PotencyBand.EXTRA_HIGH, MobEffects.HUNGER, 1200);
        expect(context, kush, 3, PotencyBand.EXTRA_HIGH, MobEffects.HUNGER, 1600);
        context.assertValueEqual(2, find(load(kush, 2, PotencyBand.EXTRA_HIGH), MobEffects.HUNGER).getAmplifier(),
                "an extra-high pipe of two's Hunger level, which the band moves");
        // The buff is held at II whatever the band: the band bought time, never a level past the cap.
        context.assertValueEqual(1, find(load(kush, 3, PotencyBand.EXTRA_HIGH), MobEffects.RESISTANCE).getAmplifier(),
                "an extra-high bong of three's Resistance level");

        // A balanced bong of three has less THC in it: its THC effects stretch a quarter, not a half...
        expect(context, beldia, 3, PotencyBand.MID, MobEffects.HUNGER, 1000);
        // ...while its strain effect keeps the bud count rule, band or none.
        expect(context, beldia, 3, PotencyBand.MID, MobEffects.INVISIBILITY, 1200);
        expect(context, beldia, 3, PotencyBand.EXTRA_HIGH, MobEffects.INVISIBILITY, 1200);
        expect(context, beldia, 1, PotencyBand.LOW, MobEffects.INVISIBILITY, 800);

        // Balanced rosin weighs 1½ where THC rosin weighs 3, with no press rule of its own: one piece
        // is three units of a strainless concentrate at half the THC, level I, and the half buys time.
        Holder<Strain> rosin = strain(world, ModStrains.ROSIN);
        SmokeContents balancedRosin = SmokeContents.of(rosin, rosin.value().dosePerItem(), Chemotype.BALANCED);
        context.assertTrue(Math.abs(balancedRosin.entries().getFirst().thcUnits() - 1.5) < 1e-9,
                "a piece of balanced rosin's THC: " + balancedRosin.entries().getFirst().thcUnits());
        context.assertValueEqual(1, balancedRosin.thcDose(), "a piece of balanced rosin's THC dose");
        context.succeed();
    }

    /**
     * The strain's {@code potency.base} is the one count the oven and the bowl share: every bud recipe
     * the mod ships takes it, and a recipe that writes its own count moves the oven alone.
     */
    public static void theStrainsBaseIsTheOvensCount(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        int buds = 0;
        for (RecipeHolder<DecarboxylatingRecipe> holder : ModRecipes.allOfType(world, ModRecipes.DECARBOXYLATING_TYPE)) {
            DecarboxylatingRecipe recipe = holder.value();
            for (Holder<net.minecraft.world.item.Item> item : recipe.ingredient().items().toList()) {
                boolean bud = Strain.fromBuds(registries, item.value())
                        .filter(strain -> strain.value().wildFlower().isPresent()).isPresent();
                if (bud) {
                    buds++;
                    context.assertTrue(recipe.countFromStrain(), holder.id().identifier()
                            + " writes its own count for a plant's bud, so the oven and the bowl can part");
                }
            }
        }
        context.assertTrue(buds >= 7, "saw only " + buds + " bud recipes");

        // A count of its own moves the oven alone; taking the strain's ignores what the result says.
        ItemStack kush = new ItemStack(ModItems.INDICA_BUDS);
        ItemStack nine = new ItemStack(ModItems.DECARBOXYLATED_HEMP, 9);
        context.assertValueEqual(4, new DecarboxylatingRecipe(Ingredient.of(ModItems.INDICA_BUDS), nine, true)
                .midCount(kush, registries), "a bud recipe taking the strain's count");
        context.assertValueEqual(9, new DecarboxylatingRecipe(Ingredient.of(ModItems.INDICA_BUDS), nine)
                .midCount(kush, registries), "a bud recipe writing its own count");
        // A leaf has no strain, so "the strain's" falls back to the recipe's own count.
        context.assertValueEqual(9, new DecarboxylatingRecipe(Ingredient.of(ModItems.HEMP_LEAF), nine, true)
                .midCount(new ItemStack(ModItems.HEMP_LEAF), registries), "a leaf recipe asking for a strain's count");
        context.succeed();
    }

    /**
     * A moon rock is one bud, a rosin and a coat: its plant entry carries the bud's band once, since
     * the other two units are concentrate and carry none.
     */
    public static void aMoonRockCountsItsBandOnce(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        for (PotencyBand band : PotencyBand.values()) {
            ItemStack bud = PotencyBand.stamp(new ItemStack(ModItems.ENDER_HAZE_BUDS), band);
            CraftingInput grid = CraftingInput.of(3, 1, List.of(bud, new ItemStack(ModItems.ROSIN), new ItemStack(ModItems.HASHISH)));
            ItemStack rock = world.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid, world)
                    .map(entry -> entry.value().assemble(grid, world.registryAccess())).orElse(ItemStack.EMPTY);
            SmokeContents load = rock.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
            context.assertTrue(rock.is(ModItems.MOON_ROCK) && !load.isEmpty(), "a " + band + " Ender Haze moon rock came out " + rock);
            SmokeContents.Entry plant = load.entries().stream()
                    .filter(entry -> entry.strain().value().wildFlower().isPresent()).findFirst().orElseThrow();
            context.assertValueEqual(band.offset(), plant.potency(),
                    "a " + band + " moon rock's plant entry's band, which counts once and not per unit");
        }
        context.succeed();
    }

    /**
     * A bowl's CBD: CBD-labelled smoke effects at level I for 50 ticks per unit of CBD, the band moving
     * it too, at most 150, whatever the device.
     */
    public static void aCbdBowlLastsByItsCbd(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        HolderLookup.Provider registries = world.registryAccess();
        // No strain the mod ships has a CBD-labelled smoke effect yet; a datapack's would read like this.
        Strain cbd = Strain.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString("""
                {"translation_key": "test.cbd", "color": 0, "buds": "hempdustry:indica_buds",
                 "flower": "hempdustry:indica_flower", "chemotype": "cbd_dominant",
                 "effects": [{"effect": "minecraft:regeneration", "label": "cbd"}]}""")).getOrThrow();
        Holder<Strain> holder = Holder.direct(cbd);
        int[] mid = {50, 100, 150};
        for (int count = 1; count <= 3; count++) {
            MobEffectInstance regeneration = find(SmokeContents.of(holder, count, Chemotype.THC_DOMINANT, PotencyBand.MID),
                    MobEffects.REGENERATION);
            context.assertValueEqual(mid[count - 1], regeneration.getDuration(), "a CBD load of " + count + "'s Regeneration ticks");
            context.assertValueEqual(0, regeneration.getAmplifier(), "a CBD load of " + count + "'s Regeneration level");
        }
        // The band moves the CBD: a high bud is 5/4 of a mid one; an extra-high bong of three stops at 150.
        context.assertValueEqual(63, find(SmokeContents.of(holder, 1, Chemotype.THC_DOMINANT, PotencyBand.HIGH),
                MobEffects.REGENERATION).getDuration(), "a high CBD bud's Regeneration ticks");
        context.assertValueEqual(150, find(SmokeContents.of(holder, 3, Chemotype.THC_DOMINANT, PotencyBand.EXTRA_HIGH),
                MobEffects.REGENERATION).getDuration(), "an extra-high CBD bong of three's Regeneration ticks");
        // Whatever the device: the device's duration plays no part.
        context.assertValueEqual(100, find(SmokeContents.of(holder, 2, Chemotype.THC_DOMINANT, PotencyBand.MID).effects(
                4000, false, () -> 0L), MobEffects.REGENERATION).getDuration(), "a CBD pipe of two's ticks on a long device");
        // Half the CBD in a balanced bud: Beldía's CBD is worth half a CBD bud's.
        context.assertTrue(Math.abs(SmokeContents.of(strain(world, ModStrains.BELDIA), 3).entries().getFirst().cbdUnits() - 1.5) < 1e-9,
                "a balanced bong of three's CBD");
        context.succeed();
    }

    private static Holder<Strain> strain(ServerLevel world, ResourceKey<Strain> key) {
        return Strain.registry(world.registryAccess()).getOrThrow(key);
    }

    private static SmokeContents load(Holder<Strain> strain, int count, PotencyBand band) {
        return SmokeContents.of(strain, count, Chemotype.THC_DOMINANT, band);
    }

    private static void expect(GameTestHelper context, Holder<Strain> strain, int count, PotencyBand band,
                               Holder<MobEffect> effect, int ticks) {
        context.assertValueEqual(ticks, find(load(strain, count, band), effect).getDuration(),
                strain.unwrapKey().orElseThrow().identifier().getPath() + " × " + count + " " + band + " " + effect.getRegisteredName() + " ticks");
    }

    private static MobEffectInstance find(SmokeContents contents, Holder<MobEffect> wanted) {
        return find(contents.effects(TICKS, false, () -> 0L), wanted);
    }

    private static MobEffectInstance find(List<MobEffectInstance> effects, Holder<MobEffect> wanted) {
        for (MobEffectInstance effect : effects) {
            if (effect.getEffect() == wanted) {
                return effect;
            }
        }
        throw new AssertionError("no " + wanted.getRegisteredName() + " in " + effects);
    }

    private BandWorthGameTest() {
    }
}

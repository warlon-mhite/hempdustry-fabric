package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The two loads that are not "one bud, one dose", asserted against the <b>real</b>
 * {@link PackingRecipe} rather than against the numbers it is supposed to read.
 *
 * <p>That distinction is the whole point. {@code dosePerItem} and {@code maxDose} are constants, and
 * a recipe that quietly ignored either would agree with every constant in the mod and still hand out
 * the wrong thing. Both failures here are silent and both are generous: a rosin that packs as dose 1
 * is a concentrate that does nothing, and a moon rock a pipe accepts is level III out of a device
 * priced for II.
 */
public final class ConcentrateGameTest {

    /**
     * One rosin fills a bong, and fits nothing else.
     *
     * <p><b>Bong-only is not a rule anywhere in the code</b> — it is arithmetic: rosin's
     * {@code dosePerItem} is 3, the cap is on the largest entry, and only the bong's
     * {@code maxDose} is 3. That is exactly the kind of emergent gate that breaks the day somebody
     * "simplifies" the cap back to a total, so it is asserted here rather than assumed.
     */
    public static void oneRosinFillsABong(TestContext context) {
        ServerWorld world = context.getWorld();
        PackingRecipe recipe = new PackingRecipe(CraftingRecipeCategory.MISC);
        ItemStack rosin = new ItemStack(ModItems.ROSIN);

        CraftingRecipeInput one = grid(new ItemStack(ModItems.BONG), rosin);
        context.assertTrue(recipe.matches(one, world),
                "a bong and one rosin is not a packing recipe at all");

        ItemStack packed = recipe.craft(one, world.getRegistryManager());
        SmokeContents load = packed.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        context.assertEquals(load.dose(), 3,
                "one rosin did not pack as dose 3 — dose_per_item is not being read");
        context.assertEquals(load.primaryCount(), 3,
                "the load's level is not III, so the item's name would lie about what it does");
        context.assertEquals(packed.getOrDefault(ModComponents.CHARGES, 0),
                DeviceType.BONG.bowlSize(), "packing did not load a full bowl");

        // Two rosin is dose 6, over the bong's cap. A dab is THE dose, not a step on a ladder.
        context.assertTrue(!recipe.matches(grid(new ItemStack(ModItems.BONG), rosin, rosin), world),
                "the bong accepted two rosin — that is dose 6, over its own maxDose");

        // A pipe is maxDose 2 and a vaporizer 1. A rolled paper and a wooden bowl have no hot
        // surface to vaporise a concentrate off, and this is what says so.
        context.assertTrue(!recipe.matches(grid(new ItemStack(ModItems.WOODEN_PIPE), rosin), world),
                "a pipe accepted rosin — dose 3 against maxDose 2");
        context.assertTrue(!recipe.matches(grid(new ItemStack(ModItems.VAPORIZER), rosin), world),
                "a vaporizer accepted rosin — dose 3 against maxDose 1");

        // The control: the same devices still pack normally, so the refusals above cannot be a
        // packing recipe that is simply broken.
        context.assertTrue(recipe.matches(
                        grid(new ItemStack(ModItems.WOODEN_PIPE), new ItemStack(buds(world, ModStrains.INDICA))), world),
                "the pipe refused a plain bud, so the refusals above prove nothing");
        context.complete();
    }

    /**
     * A moon rock is one bowl, and it is the only load in the mod with two entries in it.
     *
     * <p>Four separate ways this goes quiet:
     * <ul>
     *   <li><b>The load not carrying across</b> — a moon rock resolved as a plain bud would pack as
     *       dose 1 and lose the hash entirely.</li>
     *   <li><b>A pipe accepting it</b> — level III out of a device priced for two buds.</li>
     *   <li><b>Sharing the grid with a loose bud</b> — one moon rock <em>is</em> the bowl, so a
     *       moon rock plus a bud is two loads and must be refused. This is the case the
     *       bowl-filling branch exists to get right, and the one a naive "just add the counts"
     *       reading would let through.</li>
     *   <li><b>Two moon rocks</b> — same thing, twice as loudly.</li>
     * </ul>
     */
    public static void moonRockPacksAsATwoEntryBowl(TestContext context) {
        ServerWorld world = context.getWorld();
        PackingRecipe recipe = new PackingRecipe(CraftingRecipeCategory.MISC);
        RegistryEntry<Strain> indica = strain(world, ModStrains.INDICA);
        RegistryEntry<Strain> hashish = strain(world, ModStrains.HASHISH);
        ItemStack moonRock = ModItems.moonRock(indica, hashish);

        CraftingRecipeInput one = grid(new ItemStack(ModItems.BONG), moonRock);
        context.assertTrue(recipe.matches(one, world),
                "a bong and one moon rock is not a packing recipe at all");

        ItemStack packed = recipe.craft(one, world.getRegistryManager());
        SmokeContents load = packed.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        context.assertEquals(load.entries().size(), 2,
                "the moon rock's two-entry load did not carry onto the bong");
        context.assertEquals(load.primaryCount(), ModItems.MOON_ROCK_DOSE,
                "the plant is not the primary entry at level III");
        context.assertEquals(load.dose(), ModItems.MOON_ROCK_DOSE + ModItems.MOON_ROCK_HASH_DOSE,
                "the hashish coat did not come along");
        context.assertTrue(load.hashAdditive().isPresent(),
                "the load has no hash additive, so the tooltip would not name the coat");
        context.assertTrue(load.primaryStrain() == indica,
                "the moon rock packed as something other than its own plant strain");
        context.assertEquals(packed.getOrDefault(ModComponents.CHARGES, 0),
                DeviceType.BONG.bowlSize(), "packing did not load a full bowl");

        // A pipe is maxDose 2 against the plant entry's 3. No rule was written for this; the cap
        // being on the largest entry rather than the total is what does it.
        context.assertTrue(!recipe.matches(grid(new ItemStack(ModItems.WOODEN_PIPE), moonRock), world),
                "a pipe accepted a moon rock — dose 3 against maxDose 2");

        // One moon rock IS the bowl. It may not share the grid with anything else loadable.
        ItemStack bud = new ItemStack(buds(world, ModStrains.INDICA));
        context.assertTrue(!recipe.matches(grid(new ItemStack(ModItems.BONG), moonRock, bud), world),
                "a moon rock and a loose bud packed together — that is two bowls in one");
        context.assertTrue(!recipe.matches(
                        grid(new ItemStack(ModItems.BONG), moonRock, moonRock.copy()), world),
                "two moon rocks packed together");
        context.assertTrue(!recipe.matches(
                        grid(new ItemStack(ModItems.BONG), moonRock.copyWithCount(2)), world),
                "a stack of two moon rocks packed as one");
        context.complete();
    }

    private static RegistryEntry<Strain> strain(ServerWorld world,
                                                net.minecraft.registry.RegistryKey<Strain> key) {
        return world.getRegistryManager().getOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static net.minecraft.item.Item buds(ServerWorld world,
                                                net.minecraft.registry.RegistryKey<Strain> key) {
        return strain(world, key).value().buds();
    }

    /** A 3x3 grid holding these stacks, as the crafting table would present them. */
    private static CraftingRecipeInput grid(ItemStack... stacks) {
        List<ItemStack> slots = new ArrayList<>(Arrays.asList(stacks));
        while (slots.size() < 9) {
            slots.add(ItemStack.EMPTY);
        }
        return CraftingRecipeInput.create(3, 3, slots);
    }
}

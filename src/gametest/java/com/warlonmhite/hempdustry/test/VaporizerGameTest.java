package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

/**
 * The two things about the vaporizer that fail <em>silently</em>.
 *
 * <p>Neither is visible to the compiler and neither throws when it breaks. A missed AVB looks like
 * a device that simply doesn't do that, and a vaporizer that accepts two buds looks like a device
 * that is merely a little strong — which is exactly the direction nobody reports.
 */
public final class VaporizerGameTest {

    /**
     * A finished bowl hands back exactly one scorched hemp, and only the vaporizer does.
     *
     * <p>Three separate ways this goes wrong, all of them quiet:
     * <ul>
     *   <li><b>Yielding per hit rather than per bowl</b> doubles the return and makes it farmable by
     *       taking more hits — the one thing that would put it in competition with the oven.</li>
     *   <li><b>Yielding from every device</b> would hand the pipe and bong a return they are not
     *       priced for, and there is no error to see: {@code spentYield()} is 0 for both, so a
     *       branch that ignored it would just work.</li>
     *   <li><b>Yielding nothing at all</b> is the mechanic missing entirely, and a player has no way
     *       to tell that from "this device doesn't do that".</li>
     * </ul>
     *
     * <p>Driven through {@code player.interactionManager.interactItem}, not by calling
     * {@code Item#use} — the cooldown, the veto and the creative-mode branch all sit inside the real
     * path, and a test that skipped it would be exercising a method rather than the item.
     */
    public static void vaporizerBowlReturnsOneAvb(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        // Survival, because the whole mechanic lives in the branch creative mode skips: creative
        // spends no charges, so the bowl would never finish and this test would pass on anything.
        player.changeGameMode(GameMode.SURVIVAL);

        RegistryEntry<Strain> strain = strain(world);

        ItemStack vaporizer = packed(ModItems.VAPORIZER, strain, DeviceType.VAPORIZER);
        context.assertEquals(vaporizer.getOrDefault(ModComponents.CHARGES, 0), 2,
                "a vaporizer bowl is not two hits");

        player.setStackInHand(Hand.MAIN_HAND, vaporizer);
        hit(player, world, Hand.MAIN_HAND);
        context.assertEquals(count(player, ModItems.SCORCHED_HEMP), 0,
                "the vaporizer paid out AVB after one hit — the yield is per bowl, not per hit");
        context.assertEquals(player.getStackInHand(Hand.MAIN_HAND)
                        .getOrDefault(ModComponents.CHARGES, 0), 1,
                "the first hit did not spend exactly one charge");

        hit(player, world, Hand.MAIN_HAND);
        ItemStack spent = player.getStackInHand(Hand.MAIN_HAND);
        context.assertEquals(count(player, ModItems.SCORCHED_HEMP), 1,
                "a finished vaporizer bowl did not hand back exactly one scorched hemp");
        context.assertTrue(!spent.contains(ModComponents.SMOKE_CONTENTS),
                "the finished bowl left its contents on the vaporizer");
        context.assertTrue(!spent.contains(ModComponents.CHARGES),
                "the finished bowl left its charges on the vaporizer");
        context.assertTrue(spent.getDamage() == 2,
                "two hits did not cost two durability");

        // The other half, and the reason spentYield is a field rather than an "if vaporizer": a
        // device with no yield must finish its bowl and hand back nothing.
        ItemStack pipe = packed(ModItems.WOODEN_PIPE, strain, DeviceType.PIPE);
        player.setStackInHand(Hand.MAIN_HAND, pipe);
        for (int i = 0; i < DeviceType.PIPE.bowlSize(); i++) {
            hit(player, world, Hand.MAIN_HAND);
        }
        context.assertTrue(!player.getStackInHand(Hand.MAIN_HAND).contains(ModComponents.CHARGES),
                "the pipe's bowl did not finish, so this half proves nothing");
        context.assertEquals(count(player, ModItems.SCORCHED_HEMP), 1,
                "a finished pipe bowl handed back AVB — spentYield is not being read");

        // End of life, and it is not a corner case: maxDamage is a whole number of bowls, so the
        // hit that breaks a vaporizer is ALWAYS the last hit of a bowl — hit 32 of 32, once in the
        // life of every one ever crafted. Yielding from inside an isEmpty() guard would eat that
        // last AVB every single time, and the player would never know it had happened.
        ItemStack worn = packed(ModItems.VAPORIZER, strain, DeviceType.VAPORIZER);
        worn.setDamage(DeviceType.VAPORIZER.maxDamage() - DeviceType.VAPORIZER.bowlSize());
        player.setStackInHand(Hand.MAIN_HAND, worn);
        for (int i = 0; i < DeviceType.VAPORIZER.bowlSize(); i++) {
            hit(player, world, Hand.MAIN_HAND);
        }
        context.assertTrue(player.getStackInHand(Hand.MAIN_HAND).isEmpty(),
                "the vaporizer did not break on its last hit, so this half proves nothing");
        context.assertEquals(count(player, ModItems.SCORCHED_HEMP), 2,
                "a vaporizer that broke on the hit that finished its bowl ate the AVB");
        context.complete();
    }

    /**
     * Only a bowl of plant matter leaves anything behind. A vaporizer also holds resin — every
     * hash-family entry is dose 1 — and scorched hemp itself, and both have to finish with nothing.
     *
     * <p>Two quiet failures, and the first shipped: <b>resin handing back hemp</b> walks the hash
     * family into cannabutter, which is the one thing that family must never reach, and nothing on
     * screen says anything is wrong — the player just gets a bonus. <b>Scorched hemp handing back
     * scorched hemp</b> is a bowl that never ends. The indica bowl at the end is the control: the
     * same player, the same device, still paid out, so the two zeroes above it are the guard and not
     * a vaporizer that has stopped yielding altogether.
     */
    public static void vaporizerOnlyReturnsWhatGrewOnAPlant(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        smokeABowl(player, world, strain(world, ModStrains.HASHISH));
        context.assertEquals(count(player, ModItems.SCORCHED_HEMP), 0,
                "a bowl of hashish handed back scorched hemp — hash walks into the Infuser");
        context.assertEquals(count(player, ModItems.DECARBOXYLATED_HEMP), 0,
                "a bowl of hashish handed back decarboxylated hemp — hash walks into the Infuser");

        smokeABowl(player, world, strain(world, ModStrains.SCORCHED_HEMP));
        context.assertEquals(count(player, ModItems.SCORCHED_HEMP), 0,
                "a bowl of scorched hemp handed back more scorched hemp — the bowl never ends");

        smokeABowl(player, world, strain(world));
        context.assertEquals(count(player, ModItems.SCORCHED_HEMP), 1,
                "a bowl of buds handed nothing back, so the two refusals above prove nothing");
        context.complete();
    }

    private static void smokeABowl(ServerPlayerEntity player, ServerWorld world, RegistryEntry<Strain> strain) {
        player.setStackInHand(Hand.MAIN_HAND, packed(ModItems.VAPORIZER, strain, DeviceType.VAPORIZER));
        for (int i = 0; i < DeviceType.VAPORIZER.bowlSize(); i++) {
            hit(player, world, Hand.MAIN_HAND);
        }
    }

    private static RegistryEntry<Strain> strain(ServerWorld world,
                                                net.minecraft.registry.RegistryKey<Strain> key) {
        return world.getRegistryManager().getOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    /**
     * The vaporizer's bowl takes one bud and refuses two — the whole trade-away, and the reason it
     * can never green you out.
     *
     * <p>Asserted against the real {@link PackingRecipe} rather than against
     * {@link DeviceType#maxDose()}, because the number is only worth anything if the recipe reads
     * it: a packing recipe that had hard-coded a ceiling would agree with the enum and still let a
     * dose-2 vaporizer be crafted. The bong's dose 2 is asserted in the same breath so that a
     * refusal caused by the recipe being broken outright cannot pass as a fix.
     */
    public static void vaporizerRefusesTwoBuds(TestContext context) {
        ServerWorld world = context.getWorld();
        PackingRecipe recipe = new PackingRecipe(CraftingRecipeCategory.MISC);
        Item buds = strain(world).value().buds();

        CraftingRecipeInput one = grid(new ItemStack(ModItems.VAPORIZER), buds, 1);
        context.assertTrue(recipe.matches(one, world),
                "a vaporizer and one bud is not a packing recipe at all");
        ItemStack packed = recipe.craft(one, world.getRegistryManager());
        context.assertEquals(packed.getOrDefault(ModComponents.CHARGES, 0),
                DeviceType.VAPORIZER.bowlSize(), "packing did not load a full bowl");
        context.assertEquals(packed.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY)
                .dose(), 1, "packing a vaporizer with one bud did not give dose 1");

        context.assertTrue(!recipe.matches(grid(new ItemStack(ModItems.VAPORIZER), buds, 2), world),
                "the vaporizer accepted two buds — maxDose 1 is not being enforced");

        context.assertTrue(recipe.matches(grid(new ItemStack(ModItems.BONG), buds, 2), world),
                "the bong refused two buds, so the refusal above proves nothing");
        context.complete();
    }

    /** The first plant strain the server loaded — any of them packs the same. */
    private static RegistryEntry<Strain> strain(ServerWorld world) {
        return world.getRegistryManager().getOrThrow(Strain.REGISTRY_KEY)
                .getOrThrow(ModStrains.INDICA);
    }

    private static ItemStack packed(Item device, RegistryEntry<Strain> strain, DeviceType type) {
        ItemStack stack = new ItemStack(device);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, 1));
        stack.set(ModComponents.CHARGES, type.bowlSize());
        return stack;
    }

    /**
     * One hit, with the shared cooldown wound back first.
     *
     * <p>{@code Smoking.startCooldown} arms <em>every</em> smokeable for a minute, which is the
     * point of it — but a test that has to take two hits in a row cannot tick 60 times inside a
     * synchronous test function. Clearing the group is the honest way round: it leaves the cooldown
     * mechanism itself intact rather than routing past {@code use()}.
     */
    private static void hit(ServerPlayerEntity player, ServerWorld world, Hand hand) {
        for (Item device : ModItems.devices().values()) {
            player.getItemCooldownManager().remove(Registries.ITEM.getId(device));
        }
        player.getItemCooldownManager().remove(Registries.ITEM.getId(ModItems.SPLIFF));
        player.interactionManager.interactItem(player, world, player.getStackInHand(hand), hand);
    }

    /** A 3x3 grid holding the device and {@code budCount} buds, as the crafting table would. */
    private static CraftingRecipeInput grid(ItemStack device, Item buds, int budCount) {
        List<ItemStack> slots = new ArrayList<>();
        slots.add(device);
        for (int i = 0; i < budCount; i++) {
            slots.add(new ItemStack(buds));
        }
        while (slots.size() < 9) {
            slots.add(ItemStack.EMPTY);
        }
        return CraftingRecipeInput.create(3, 3, slots);
    }

    private static int count(ServerPlayerEntity player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}

package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.registry.DataRegistries;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.PackingRecipe;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import com.warlonmhite.hempdustry.registry.Holder;
import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

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
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void vaporizerBowlReturnsOneAvb(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = Players.mock(context);
        // Survival, because the whole mechanic lives in the branch creative mode skips: creative
        // spends no charges, so the bowl would never finish and this test would pass on anything.
        player.setGameMode(GameType.SURVIVAL);

        Holder<Strain> strain = strain(world);

        ItemStack vaporizer = packed(ModItems.VAPORIZER, strain, DeviceType.VAPORIZER);
        Line.equal(context, 2, ModComponents.CHARGES.getOrDefault(vaporizer, 0),
                "a vaporizer bowl is not two hits");

        player.setItemInHand(InteractionHand.MAIN_HAND, vaporizer);
        hit(player, world, InteractionHand.MAIN_HAND);
        Line.equal(context, 0, count(player, ModItems.SCORCHED_HEMP),
                "the vaporizer paid out AVB after one hit — the yield is per bowl, not per hit");
        Line.equal(context, 1,
                ModComponents.CHARGES.getOrDefault(player.getItemInHand(InteractionHand.MAIN_HAND), 0),
                "the first hit did not spend exactly one charge");

        hit(player, world, InteractionHand.MAIN_HAND);
        ItemStack spent = player.getItemInHand(InteractionHand.MAIN_HAND);
        Line.equal(context, 1, count(player, ModItems.SCORCHED_HEMP),
                "a finished vaporizer bowl did not hand back exactly one scorched hemp");
        context.assertTrue(!ModComponents.SMOKE_CONTENTS.has(spent),
                "the finished bowl left its contents on the vaporizer");
        context.assertTrue(!ModComponents.CHARGES.has(spent),
                "the finished bowl left its charges on the vaporizer");
        context.assertTrue(spent.getDamageValue() == 2,
                "two hits did not cost two durability");

        // The other half, and the reason spentYield is a field rather than an "if vaporizer": a
        // device with no yield must finish its bowl and hand back nothing.
        ItemStack pipe = packed(ModItems.WOODEN_PIPE, strain, DeviceType.PIPE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        for (int i = 0; i < DeviceType.PIPE.builtInBowl().hits(); i++) {
            hit(player, world, InteractionHand.MAIN_HAND);
        }
        context.assertTrue(!ModComponents.CHARGES.has(player.getItemInHand(InteractionHand.MAIN_HAND)),
                "the pipe's bowl did not finish, so this half proves nothing");
        Line.equal(context, 1, count(player, ModItems.SCORCHED_HEMP),
                "a finished pipe bowl handed back AVB — spentYield is not being read");

        // End of life, and it is not a corner case: maxDamage is a whole number of bowls, so the
        // hit that breaks a vaporizer is ALWAYS the last hit of a bowl — hit 32 of 32, once in the
        // life of every one ever crafted. Yielding from inside an isEmpty() guard would eat that
        // last AVB every single time, and the player would never know it had happened.
        ItemStack worn = packed(ModItems.VAPORIZER, strain, DeviceType.VAPORIZER);
        worn.setDamageValue(DeviceType.VAPORIZER.maxDamage() - DeviceType.VAPORIZER.builtInBowl().hits());
        player.setItemInHand(InteractionHand.MAIN_HAND, worn);
        for (int i = 0; i < DeviceType.VAPORIZER.builtInBowl().hits(); i++) {
            hit(player, world, InteractionHand.MAIN_HAND);
        }
        context.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "the vaporizer did not break on its last hit, so this half proves nothing");
        Line.equal(context, 2, count(player, ModItems.SCORCHED_HEMP),
                "a vaporizer that broke on the hit that finished its bowl ate the AVB");
        context.succeed();
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
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void vaporizerOnlyReturnsWhatGrewOnAPlant(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = Players.mock(context);
        player.setGameMode(GameType.SURVIVAL);

        smokeABowl(player, world, strain(world, ModStrains.HASHISH));
        Line.equal(context, 0, count(player, ModItems.SCORCHED_HEMP),
                "a bowl of hashish handed back scorched hemp — hash walks into the Infuser");
        Line.equal(context, 0, count(player, ModItems.DECARBOXYLATED_HEMP),
                "a bowl of hashish handed back decarboxylated hemp — hash walks into the Infuser");

        smokeABowl(player, world, strain(world, ModStrains.SCORCHED_HEMP));
        Line.equal(context, 0, count(player, ModItems.SCORCHED_HEMP),
                "a bowl of scorched hemp handed back more scorched hemp — the bowl never ends");

        // Schwag is plant matter with no plant of its own, which is the one case the plant predicate
        // would get wrong: a spoiled bud still leaves a spent one.
        smokeABowl(player, world, strain(world, ModStrains.SCHWAG));
        Line.equal(context, count(player, ModItems.SCORCHED_HEMP), 1,
                "a bowl of schwag handed nothing back — it grew on a plant like any bud");

        smokeABowl(player, world, strain(world));
        Line.equal(context, 2, count(player, ModItems.SCORCHED_HEMP),
                "a bowl of buds handed nothing back, so the refusals above prove nothing");
        context.succeed();
    }

    /**
     * A full inventory does not swallow the spent bud: it lands at the player's feet.
     *
     * <p>{@code Player#addItem} only tries the inventory and answers false when nothing fits; it
     * never drops. A bowl finished with every slot taken used to hand its scorched hemp to nobody,
     * and nothing on screen said so -- the commonest state of a survival inventory is full.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void vaporizerBowlOnAFullInventoryDropsItsAvb(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = Players.mock(context);
        player.setGameMode(GameType.SURVIVAL);
        net.minecraft.core.BlockPos feet = context.absolutePos(new net.minecraft.core.BlockPos(4, 1, 4));
        player.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0, 0);

        // The first bowl, smoked with room to spare, is the control: it must still land in the inventory.
        smokeABowl(player, world, strain(world));
        Line.equal(context, count(player, ModItems.SCORCHED_HEMP), 1,
                "a bowl with room in the inventory did not hand its scorched hemp over");

        // Every slot but the hand full of something the scorched hemp cannot join, its own stack included.
        for (int i = 0; i < player.inventory.items.size(); i++) {
            if (i != player.inventory.selected) {
                player.inventory.items.set(i, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
            }
        }
        player.inventory.offhand.set(0, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 64));
        player.inventory.items.set(player.inventory.selected,
                packed(ModItems.VAPORIZER, strain(world), DeviceType.VAPORIZER));
        for (int i = 0; i < DeviceType.VAPORIZER.builtInBowl().hits(); i++) {
            hit(player, world, InteractionHand.MAIN_HAND);
        }
        int onTheGround = 0;
        for (net.minecraft.world.entity.item.ItemEntity entity
                : Line.entities(context, net.minecraft.world.entity.EntityType.ITEM)) {
            if (entity.getItem().getItem() == ModItems.SCORCHED_HEMP) {
                onTheGround += entity.getItem().getCount();
            }
        }
        Line.equal(context, onTheGround, 1,
                "a bowl finished with a full inventory lost its scorched hemp instead of dropping it");
        context.succeed();
    }

    private static void smokeABowl(ServerPlayer player, ServerLevel world, Holder<Strain> strain) {
        player.setItemInHand(InteractionHand.MAIN_HAND, packed(ModItems.VAPORIZER, strain, DeviceType.VAPORIZER));
        for (int i = 0; i < DeviceType.VAPORIZER.builtInBowl().hits(); i++) {
            hit(player, world, InteractionHand.MAIN_HAND);
        }
    }

    private static Holder<Strain> strain(ServerLevel world,
                                                net.minecraft.resources.ResourceKey<Strain> key) {
        return DataRegistries.of(world).lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    /**
     * The vaporizer's bowl takes one bud and refuses two — the whole trade-away, and the reason it
     * can never green you out.
     *
     * <p>Asserted against the real {@link PackingRecipe} rather than against
     * the vaporizer's {@code max_dose}, because the number is only worth anything if the recipe reads
     * it: a packing recipe that had hard-coded a ceiling would agree with the data and still let a
     * dose-2 vaporizer be crafted. The bong's dose 2 is asserted in the same breath so that a
     * refusal caused by the recipe being broken outright cannot pass as a fix.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void vaporizerRefusesTwoBuds(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        PackingRecipe recipe = new PackingRecipe(new ResourceLocation("hempdustry", "packing"));
        Item buds = strain(world).value().buds();

        CraftingInput one = grid(new ItemStack(ModItems.VAPORIZER), buds, 1);
        context.assertTrue(recipe.matches(one, world),
                "a vaporizer and one bud is not a packing recipe at all");
        ItemStack packed = recipe.assemble(one);
        Line.equal(context, DeviceType.VAPORIZER.builtInBowl().hits(),
                ModComponents.CHARGES.getOrDefault(packed, 0), "packing did not load a full bowl");
        Line.equal(context, 1, ModComponents.SMOKE_CONTENTS.getOrDefault(packed, SmokeContents.EMPTY)
                .dose(), "packing a vaporizer with one bud did not give dose 1");

        context.assertTrue(!recipe.matches(grid(new ItemStack(ModItems.VAPORIZER), buds, 2), world),
                "the vaporizer accepted two buds — maxDose 1 is not being enforced");

        context.assertTrue(recipe.matches(grid(new ItemStack(ModItems.BONG), buds, 2), world),
                "the bong refused two buds, so the refusal above proves nothing");
        context.succeed();
    }

    /** The first plant strain the server loaded — any of them packs the same. */
    private static Holder<Strain> strain(ServerLevel world) {
        return DataRegistries.of(world).lookupOrThrow(Strain.REGISTRY_KEY)
                .getOrThrow(ModStrains.INDICA);
    }

    private static ItemStack packed(Item device, Holder<Strain> strain, DeviceType type) {
        ItemStack stack = new ItemStack(device);
        ModComponents.SMOKE_CONTENTS.set(stack, SmokeContents.of(strain, 1));
        ModComponents.CHARGES.set(stack, type.builtInBowl().hits());
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
    private static void hit(ServerPlayer player, ServerLevel world, InteractionHand hand) {
        for (Item device : ModItems.devices().values()) {
            Line.clearCooldown(player, device);
        }
        Line.clearCooldown(player, ModItems.SPLIFF);
        player.gameMode.useItem(player, world, player.getItemInHand(hand), hand);
    }

    /** A 3x3 grid holding the device and {@code budCount} buds, as the crafting table would. */
    private static CraftingInput grid(ItemStack device, Item buds, int budCount) {
        List<ItemStack> slots = new ArrayList<>();
        slots.add(device);
        for (int i = 0; i < budCount; i++) {
            slots.add(new ItemStack(buds));
        }
        while (slots.size() < 9) {
            slots.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, slots);
    }

    private static int count(ServerPlayer player, Item item) {
        int total = 0;
        for (int i = 0; i < player.inventory.getContainerSize(); i++) {
            ItemStack stack = player.inventory.getItem(i);
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }
}

package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.HashishBarBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Optional;

/**
 * The advancement tree, driven against the tree <b>as the server loaded it</b>. A test that
 * rebuilt the criteria would agree with a wrong one; the risk lives in the generated JSON and in the
 * tags it names. Each test also checks where its nodes hang, since the parent is half of what a node
 * teaches.
 *
 * <p>Every trigger fires from the game's own site: a food through {@code Item#finishUsingItem}, the
 * same call the end of an eating animation makes; a harvest by breaking a real plant; a hit through
 * the real click. Every positive has a negative beside it, since a criterion that had widened to
 * "anything" would pass the positives alone.
 */
public final class AdvancementGameTest implements FabricGameTest {

    private static final BlockPos BED = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = BED.above();

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void eatingGrantsTheFoodNodes(GameTestHelper context) {
        AdvancementHolder hempHearts = loaded(context, "hemp_hearts", "hempdustry");
        AdvancementHolder gotBhang = loaded(context, "got_bhang", "activation_energy");
        AdvancementHolder club = loaded(context, "club_des_hashischins", "butter_late_than_never");

        // Bread is a food and in no tag of ours; cannabutter toast is an edible but neither the jam
        // nor the drink. Neither may grant anything here.
        ServerPlayer player = freshPlayer(context);
        consume(context, player, Items.BREAD);
        consume(context, player, ModItems.CANNABUTTER_TOAST);
        assertNotDone(context, player, hempHearts, "bread or cannabutter toast");
        assertNotDone(context, player, gotBhang, "bread or cannabutter toast");
        assertNotDone(context, player, club, "bread or cannabutter toast");

        // Every seed food grants Hemp Hearts on its own — a fresh player each, so one entry that
        // had fallen out of the tag cannot hide behind another.
        for (Item food : new Item[]{ModItems.TOASTED_HEMP_SEEDS, ModItems.HEMP_FLAPJACK, ModItems.SIEMIENIOTKA}) {
            ServerPlayer eater = freshPlayer(context);
            consume(context, eater, food);
            context.assertTrue(eater.getAdvancements().getOrStartProgress(hempHearts).isDone(),
                    "eating " + food + " did not grant Hemp Hearts — it has left #hempdustry:hemp_seed_foods");
            assertNotDone(context, eater, club, food.toString());
        }

        // Hemp milk is seeds too, but it is drunk and it is an Infuser ingredient: not a food.
        ServerPlayer drinker = freshPlayer(context);
        consume(context, drinker, ModItems.HEMP_MILK_BUCKET);
        assertNotDone(context, drinker, hempHearts, "hemp milk");

        ServerPlayer bhang = freshPlayer(context);
        consume(context, bhang, ModItems.BHANG_BUCKET);
        context.assertTrue(bhang.getAdvancements().getOrStartProgress(gotBhang).isDone(),
                "drinking bhang did not grant Got Bhang?");
        assertNotDone(context, bhang, club, "bhang");

        ServerPlayer jam = freshPlayer(context);
        consume(context, jam, ModItems.DAWAMESK);
        context.assertTrue(jam.getAdvancements().getOrStartProgress(club).isDone(),
                "eating dawamesk did not grant Club des Hashischins");
        assertNotDone(context, jam, gotBhang, "dawamesk");

        context.succeed();
    }

    /**
     * The indoor chain reads the plant, never the room. Each harvest breaks a real plant through the
     * interaction manager, so the criterion fires from the crop's own {@code playerWillDestroy} and reads the
     * bed while it is still under the plant. A fresh player per plant keeps one grant from hiding
     * another.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void harvestNodesReadThePlant(GameTestHelper context) {
        AdvancementHolder pothead = loaded(context, "pothead", "hemp_builder");
        AdvancementHolder seaOfGreen = loaded(context, "sea_of_green", "pothead");
        AdvancementHolder midnightSun = loaded(context, "midnight_sun", "pothead");
        AdvancementHolder topShelf = loaded(context, "top_shelf", "midnight_sun");

        // A plant under a Grow Lamp on farmland: the lamp, not the tray, and not the whole set.
        ServerPlayer lampOnly = harvestIndica(context, Blocks.FARMLAND, GrowLight.GROW_LAMP, true, true, false);
        assertDone(context, lampOnly, midnightSun, "a lamp-grown plant on farmland");
        assertNotDone(context, lampOnly, seaOfGreen, "a plant on farmland");
        assertNotDone(context, lampOnly, topShelf, "a plant on farmland");

        // A tray with no lamp over it, then a tray whose lamp was lost while flowering: the record
        // is what counts, and STRESSED is not GROW_LAMP.
        for (GrowLight light : new GrowLight[]{GrowLight.NATURAL, GrowLight.STRESSED}) {
            ServerPlayer tray = harvestIndica(context, ModBlocks.HYDRO_TRAY, light, true, true, false);
            assertDone(context, tray, seaOfGreen, "a " + light.getSerializedName() + " plant in a tray");
            assertNotDone(context, tray, midnightSun, "a " + light.getSerializedName() + " plant");
            assertNotDone(context, tray, topShelf, "a " + light.getSerializedName() + " plant");
        }

        // Everything but the second trim is not Top Shelf.
        ServerPlayer halfTrimmed = harvestIndica(context, ModBlocks.HYDRO_TRAY, GrowLight.GROW_LAMP, true, false, false);
        assertDone(context, halfTrimmed, midnightSun, "a lamp-grown plant in a tray");
        assertNotDone(context, halfTrimmed, topShelf, "a plant trimmed only once");

        // The whole set, broken from the TOP half: the criterion must resolve down to the lower
        // segment, which is the only one carrying the trim flags.
        ServerPlayer indica = harvestIndica(context, ModBlocks.HYDRO_TRAY, GrowLight.GROW_LAMP, true, true, true);
        assertDone(context, indica, topShelf, "a fully trimmed, lamp-grown Purple Kush broken from its top");

        // And a three-tall Lemon Haze broken from its top segment, two blocks above its bed.
        ServerPlayer sativa = freshPlayer(context);
        context.setBlock(BED, ModBlocks.HYDRO_TRAY);
        BlockState ripe = Defoliation.unworked(ModBlocks.SATIVA_CROP.defaultBlockState())
                .setValue(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE)
                .setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP);
        context.setBlock(CROP, ripe.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                .setValue(Defoliation.TRIMMED_EARLY, true).setValue(Defoliation.TRIMMED_LATE, true));
        context.setBlock(CROP.above(), ripe.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.MIDDLE));
        context.setBlock(CROP.above(2), ripe.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.UPPER));
        sativa.gameMode.destroyBlock(context.absolutePos(CROP.above(2)));
        context.assertTrue(context.getBlockState(CROP).isAir(), "the Lemon Haze was not harvested, so this proves nothing");
        assertDone(context, sativa, topShelf, "a fully trimmed, lamp-grown Lemon Haze broken from its top");

        // Pothead is placing a pot, through the real click.
        ServerPlayer placer = freshPlayer(context);
        context.setBlock(CROP, Blocks.AIR);
        context.setBlock(BED, Blocks.STONE);
        ItemStack pot = new ItemStack(ModBlocks.GROW_POT);
        placer.setItemInHand(InteractionHand.MAIN_HAND, pot);
        BlockPos stone = context.absolutePos(BED);
        placer.gameMode.useItemOn(placer, context.getLevel(), pot, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(stone).add(0, 0.5, 0), Direction.UP, stone, false));
        context.assertTrue(context.getBlockState(CROP).is(ModBlocks.GROW_POT), "the Grow Pot was not placed");
        assertDone(context, placer, pothead, "placing a Grow Pot");
        context.succeed();
    }

    /**
     * The smoking nodes, each through a real hit. Vapor Trail is the device; You Get What You Pay
     * For is the strain in the load, whatever it is smoked from — so schwag in a pipe grants it and
     * Purple Kush in a Vaporizer does not.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void smokeNodesReadTheLoad(GameTestHelper context) {
        AdvancementHolder vaporTrail = loaded(context, "vapor_trail", "first_contact");
        AdvancementHolder schwagHit = loaded(context, "you_get_what_you_pay_for", "schwag");
        ServerLevel world = context.getLevel();

        ServerPlayer vaper = freshPlayer(context);
        hit(vaper, world, packed(ModItems.VAPORIZER, strain(world, ModStrains.INDICA), DeviceType.VAPORIZER));
        assertDone(context, vaper, vaporTrail, "a Vaporizer hit");
        assertNotDone(context, vaper, schwagHit, "a hit of Purple Kush");

        ServerPlayer piper = freshPlayer(context);
        hit(piper, world, packed(ModItems.WOODEN_PIPE, strain(world, ModStrains.SCHWAG), DeviceType.PIPE));
        assertDone(context, piper, schwagHit, "a pipe of schwag");
        assertNotDone(context, piper, vaporTrail, "a pipe");
        context.succeed();
    }

    /**
     * The nodes keyed on obtaining an item, fired through vanilla's own trigger with the item really
     * in the inventory. Around the World is the one worth the test: five criteria ANDed, so the
     * resins may come one at a time — the inventory is emptied between them — and four is not five.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void obtainingGrantsTheExtractionNodes(GameTestHelper context) {
        record Node(String id, String parent, Item item) {}
        List<Node> nodes = List.of(
                new Node("kif_country", "hempdustry", ModItems.BELDIA_BUDS),
                new Node("sticky_fingers", "trim_season", ModItems.CHARAS),
                new Node("well_done", "trim_season", ModItems.SCORCHED_HEMP),
                new Node("its_not_pollen", "green_threads", ModItems.KIEF),
                new Node("hot_off_the_press", "its_not_pollen", ModBlocks.HASHISH_BAR.asItem()),
                new Node("one_small_step", "cloud_nine", ModItems.MOON_ROCK),
                new Node("double_zero", "hot_off_the_press", ModItems.FILTERED_HASHISH),
                new Node("under_pressure", "double_zero", ModItems.ROSIN),
                new Node("see_you_ice_o_later", "its_not_pollen", ModItems.BUBBLE_HASH));
        for (Node node : nodes) {
            AdvancementHolder entry = loaded(context, node.id(), node.parent());
            ServerPlayer player = freshPlayer(context);
            obtain(player, new ItemStack(Items.BREAD));
            assertNotDone(context, player, entry, "bread");
            obtain(player, new ItemStack(node.item()));
            assertDone(context, player, entry, "obtaining " + node.item());
        }

        // Cloud Nine is the cut, not the hashish: the press makes hashish before any bar exists.
        AdvancementHolder cloudNine = loaded(context, "cloud_nine", "hot_off_the_press");
        ServerPlayer cutter = freshPlayer(context);
        obtain(cutter, new ItemStack(ModItems.HASHISH));
        assertNotDone(context, cutter, cloudNine, "holding hashish, with no bar ever cut");
        context.setBlock(BED, ModBlocks.HASHISH_BAR);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        cutter.setItemInHand(InteractionHand.MAIN_HAND, sword);
        BlockPos bar = context.absolutePos(BED);
        cutter.gameMode.useItemOn(cutter, context.getLevel(), sword, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(bar), Direction.UP, bar, false));
        context.assertTrue(context.getBlockState(BED).getValue(HashishBarBlock.CUTS) == 1, "the sword did not cut the bar, so this proves nothing");
        assertDone(context, cutter, cloudNine, "cutting a Hashish Bar with a sword");

        AdvancementHolder aroundTheWorld = loaded(context, "around_the_world_in_80_grams", "its_not_pollen");
        ServerPlayer collector = freshPlayer(context);
        List<Item> resins = List.of(ModItems.CHARAS, ModItems.HASHISH, ModItems.FILTERED_HASHISH,
                ModItems.BUBBLE_HASH, ModItems.ROSIN);
        for (Item resin : resins) {
            context.assertFalse(collector.getAdvancements().getOrStartProgress(aroundTheWorld).isDone(),
                    "Around the World was granted before " + resin + " was ever held — it wants all five");
            collector.getInventory().clearContent();
            obtain(collector, new ItemStack(resin));
        }
        assertDone(context, collector, aroundTheWorld, "all five resins, one at a time");
        context.succeed();
    }

    /**
     * Breaking the bed under a ripe plant harvests the plant first, as the player, while the bed is
     * still under it. Any other way, the plant pops off after its bed has gone: Sea of Green and
     * Midnight Sun silently never fire, and the loot, which reads the same bed, pays the full stem.
     * Sea of Green is the proof the bed was there at the harvest; the stem count itself carries a
     * bonus roll, so it cannot be asserted exactly.
     */
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void breakingTheBedHarvestsThePlantFirst(GameTestHelper context) {
        AdvancementHolder seaOfGreen = loaded(context, "sea_of_green", "pothead");
        AdvancementHolder midnightSun = loaded(context, "midnight_sun", "pothead");
        Block[] beds = {ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY};
        for (int i = 0; i < beds.length; i++) {
            BlockPos bed = new BlockPos(1 + 3 * i, 1, 1);
            context.setBlock(bed, beds[i]);
            BlockState ripe = Defoliation.unworked(ModBlocks.INDICA_CROP.defaultBlockState())
                    .setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE)
                    .setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP);
            context.setBlock(bed.above(), ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER));
            context.setBlock(bed.above(2), ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER));
            ServerPlayer player = freshPlayer(context);
            player.gameMode.destroyBlock(context.absolutePos(bed));

            String what = "breaking a " + beds[i].getDescriptionId() + " under a ripe, lamp-grown plant";
            context.assertTrue(context.getBlockState(bed.above()).isAir(), what + " left the plant standing");
            assertDone(context, player, midnightSun, what);
            if (beds[i] == ModBlocks.HYDRO_TRAY) {
                assertDone(context, player, seaOfGreen, what);
            }
        }
        context.succeed();
    }

    /** A ripe Purple Kush on {@code bed}, harvested by a fresh survival player; returns the player. */
    private static ServerPlayer harvestIndica(GameTestHelper context, Block bed, GrowLight light,
                                                    boolean early, boolean late, boolean fromTop) {
        context.setBlock(BED, bed);
        BlockState ripe = Defoliation.unworked(ModBlocks.INDICA_CROP.defaultBlockState())
                .setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE)
                .setValue(GrowLight.PROPERTY, light);
        context.setBlock(CROP, ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(Defoliation.TRIMMED_EARLY, early).setValue(Defoliation.TRIMMED_LATE, late));
        context.setBlock(CROP.above(), ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER));
        ServerPlayer player = freshPlayer(context);
        player.gameMode.destroyBlock(context.absolutePos(fromTop ? CROP.above() : CROP));
        context.assertTrue(context.getBlockState(CROP).isAir(), "the plant was not harvested, so this proves nothing");
        return player;
    }

    private static Holder<Strain> strain(ServerLevel world, ResourceKey<Strain> key) {
        return world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static ItemStack packed(Item device, Holder<Strain> strain, DeviceType type) {
        ItemStack stack = new ItemStack(device);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, 1));
        stack.set(ModComponents.CHARGES, type.builtInBowl().hits());
        return stack;
    }

    /** One hit through the real click, with every smokeable's shared cooldown wound back first. */
    private static void hit(ServerPlayer player, ServerLevel world, ItemStack device) {
        for (Item item : ModItems.devices().values()) {
            player.getCooldowns().removeCooldown(item);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, device);
        player.gameMode.useItem(player, world, device, InteractionHand.MAIN_HAND);
    }

    /**
     * Puts the stack in the inventory and fires {@code minecraft:inventory_changed} the way the
     * player's screen handler does when it syncs the slot — a mock player never ticks, so nothing
     * else would.
     */
    private static void obtain(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
    }

    private static void assertDone(GameTestHelper context, ServerPlayer player, AdvancementHolder entry,
                                   String what) {
        context.assertTrue(player.getAdvancements().getOrStartProgress(entry).isDone(),
                what + " did not grant " + entry.id());
    }

    /** Fetches a loaded advancement and checks where it hangs, which is half of what it teaches. */
    private static AdvancementHolder loaded(GameTestHelper context, String id, String parent) {
        AdvancementHolder entry = context.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath("hempdustry", id));
        context.assertTrue(entry != null, "hempdustry:" + id + " is not loaded at all");
        context.assertTrue(entry.value().parent().equals(Optional.of(ResourceLocation.fromNamespaceAndPath("hempdustry", parent))),
                "hempdustry:" + id + " hangs under " + entry.value().parent() + ", not hempdustry:" + parent);
        return entry;
    }

    /** Survival, so the stack is really spent and the player is not treated as a creative tester. */
    private static ServerPlayer freshPlayer(GameTestHelper context) {
        return Players.survival(context);
    }

    private static void consume(GameTestHelper context, ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getItem().finishUsingItem(stack, context.getLevel(), player);
    }

    private static void assertNotDone(GameTestHelper context, ServerPlayer player, AdvancementHolder entry,
                                      String what) {
        context.assertFalse(player.getAdvancements().getOrStartProgress(entry).isDone(),
                what + " granted " + entry.id() + ", so its criterion matches more than it should");
    }
}
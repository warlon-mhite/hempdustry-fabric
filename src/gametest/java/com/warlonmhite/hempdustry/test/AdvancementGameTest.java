package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
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
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The advancement tree, driven against the tree <b>as the server loaded it</b>. A test that
 * rebuilt the criteria would agree with a wrong one; the risk lives in the generated JSON and in the
 * tags it names. Each test also checks where its nodes hang, since the parent is half of what a node
 * teaches.
 *
 * <p>Every trigger fires from the game's own site: a food through {@code Item#finishUsing}, the
 * same call the end of an eating animation makes; a harvest by breaking a real plant; a hit through
 * the real click. Every positive has a negative beside it, since a criterion that had widened to
 * "anything" would pass the positives alone.
 */
public final class AdvancementGameTest {

    private static final BlockPos BED = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = BED.up();

    public static void eatingGrantsTheFoodNodes(TestContext context) {
        AdvancementEntry hempHearts = loaded(context, "hemp_hearts", "hempdustry");
        AdvancementEntry gotBhang = loaded(context, "got_bhang", "activation_energy");
        AdvancementEntry club = loaded(context, "club_des_hashischins", "butter_late_than_never");

        // Bread is a food and in no tag of ours; cannabutter toast is an edible but neither the jam
        // nor the drink. Neither may grant anything here.
        ServerPlayerEntity player = freshPlayer(context);
        consume(context, player, Items.BREAD);
        consume(context, player, ModItems.CANNABUTTER_TOAST);
        assertNotDone(context, player, hempHearts, "bread or cannabutter toast");
        assertNotDone(context, player, gotBhang, "bread or cannabutter toast");
        assertNotDone(context, player, club, "bread or cannabutter toast");

        // Every seed food grants Hemp Hearts on its own — a fresh player each, so one entry that
        // had fallen out of the tag cannot hide behind another.
        for (Item food : new Item[]{ModItems.TOASTED_HEMP_SEEDS, ModItems.HEMP_FLAPJACK, ModItems.SIEMIENIOTKA}) {
            ServerPlayerEntity eater = freshPlayer(context);
            consume(context, eater, food);
            context.assertTrue(eater.getAdvancementTracker().getProgress(hempHearts).isDone(),
                    "eating " + food + " did not grant Hemp Hearts — it has left #hempdustry:hemp_seed_foods");
            assertNotDone(context, eater, club, food.toString());
        }

        // Hemp milk is seeds too, but it is drunk and it is an Infuser ingredient: not a food.
        ServerPlayerEntity drinker = freshPlayer(context);
        consume(context, drinker, ModItems.HEMP_MILK_BUCKET);
        assertNotDone(context, drinker, hempHearts, "hemp milk");

        ServerPlayerEntity bhang = freshPlayer(context);
        consume(context, bhang, ModItems.BHANG_BUCKET);
        context.assertTrue(bhang.getAdvancementTracker().getProgress(gotBhang).isDone(),
                "drinking bhang did not grant Got Bhang?");
        assertNotDone(context, bhang, club, "bhang");

        ServerPlayerEntity jam = freshPlayer(context);
        consume(context, jam, ModItems.DAWAMESK);
        context.assertTrue(jam.getAdvancementTracker().getProgress(club).isDone(),
                "eating dawamesk did not grant Club des Hashischins");
        assertNotDone(context, jam, gotBhang, "dawamesk");

        context.complete();
    }

    /**
     * The indoor chain reads the plant, never the room. Each harvest breaks a real plant through the
     * interaction manager, so the criterion fires from the crop's own {@code onBreak} and reads the
     * bed while it is still under the plant. A fresh player per plant keeps one grant from hiding
     * another.
     */
    public static void harvestNodesReadThePlant(TestContext context) {
        AdvancementEntry pothead = loaded(context, "pothead", "hemp_builder");
        AdvancementEntry seaOfGreen = loaded(context, "sea_of_green", "pothead");
        AdvancementEntry midnightSun = loaded(context, "midnight_sun", "pothead");
        AdvancementEntry topShelf = loaded(context, "top_shelf", "midnight_sun");

        // A plant under a Grow Lamp on farmland: the lamp, not the tray, and not the whole set.
        ServerPlayerEntity lampOnly = harvestIndica(context, Blocks.FARMLAND, GrowLight.GROW_LAMP, true, true, false);
        assertDone(context, lampOnly, midnightSun, "a lamp-grown plant on farmland");
        assertNotDone(context, lampOnly, seaOfGreen, "a plant on farmland");
        assertNotDone(context, lampOnly, topShelf, "a plant on farmland");

        // A tray with no lamp over it, then a tray whose lamp was lost while flowering: the record
        // is what counts, and STRESSED is not GROW_LAMP.
        for (GrowLight light : new GrowLight[]{GrowLight.NATURAL, GrowLight.STRESSED}) {
            ServerPlayerEntity tray = harvestIndica(context, ModBlocks.HYDRO_TRAY, light, true, true, false);
            assertDone(context, tray, seaOfGreen, "a " + light.asString() + " plant in a tray");
            assertNotDone(context, tray, midnightSun, "a " + light.asString() + " plant");
            assertNotDone(context, tray, topShelf, "a " + light.asString() + " plant");
        }

        // Everything but the second trim is not Top Shelf.
        ServerPlayerEntity halfTrimmed = harvestIndica(context, ModBlocks.HYDRO_TRAY, GrowLight.GROW_LAMP, true, false, false);
        assertDone(context, halfTrimmed, midnightSun, "a lamp-grown plant in a tray");
        assertNotDone(context, halfTrimmed, topShelf, "a plant trimmed only once");

        // The whole set, broken from the TOP half: the criterion must resolve down to the lower
        // segment, which is the only one carrying the trim flags.
        ServerPlayerEntity indica = harvestIndica(context, ModBlocks.HYDRO_TRAY, GrowLight.GROW_LAMP, true, true, true);
        assertDone(context, indica, topShelf, "a fully trimmed, lamp-grown Purple Kush broken from its top");

        // And a three-tall Lemon Haze broken from its top segment, two blocks above its bed.
        ServerPlayerEntity sativa = freshPlayer(context);
        context.setBlockState(BED, ModBlocks.HYDRO_TRAY);
        BlockState ripe = Defoliation.unworked(ModBlocks.SATIVA_CROP.getDefaultState())
                .with(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE)
                .with(GrowLight.PROPERTY, GrowLight.GROW_LAMP);
        context.setBlockState(CROP, ripe.with(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                .with(Defoliation.TRIMMED_EARLY, true).with(Defoliation.TRIMMED_LATE, true));
        context.setBlockState(CROP.up(), ripe.with(SativaCropBlock.SEGMENT, TriplePlantSegment.MIDDLE));
        context.setBlockState(CROP.up(2), ripe.with(SativaCropBlock.SEGMENT, TriplePlantSegment.UPPER));
        sativa.interactionManager.tryBreakBlock(context.getAbsolutePos(CROP.up(2)));
        context.assertTrue(context.getBlockState(CROP).isAir(), "the Lemon Haze was not harvested, so this proves nothing");
        assertDone(context, sativa, topShelf, "a fully trimmed, lamp-grown Lemon Haze broken from its top");

        // Pothead is placing a pot, through the real click.
        ServerPlayerEntity placer = freshPlayer(context);
        context.setBlockState(CROP, Blocks.AIR);
        context.setBlockState(BED, Blocks.STONE);
        ItemStack pot = new ItemStack(ModBlocks.GROW_POT);
        placer.setStackInHand(Hand.MAIN_HAND, pot);
        BlockPos stone = context.getAbsolutePos(BED);
        placer.interactionManager.interactBlock(placer, context.getWorld(), pot, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(stone).add(0, 0.5, 0), Direction.UP, stone, false));
        context.assertTrue(context.getBlockState(CROP).isOf(ModBlocks.GROW_POT), "the Grow Pot was not placed");
        assertDone(context, placer, pothead, "placing a Grow Pot");
        context.complete();
    }

    /**
     * The smoking nodes, each through a real hit. Vapor Trail is the device; You Get What You Pay
     * For is the strain in the load, whatever it is smoked from — so schwag in a pipe grants it and
     * Purple Kush in a Vaporizer does not.
     */
    public static void smokeNodesReadTheLoad(TestContext context) {
        AdvancementEntry vaporTrail = loaded(context, "vapor_trail", "first_contact");
        AdvancementEntry schwagHit = loaded(context, "you_get_what_you_pay_for", "schwag");
        ServerWorld world = context.getWorld();

        ServerPlayerEntity vaper = freshPlayer(context);
        hit(vaper, world, packed(ModItems.VAPORIZER, strain(world, ModStrains.INDICA), DeviceType.VAPORIZER));
        assertDone(context, vaper, vaporTrail, "a Vaporizer hit");
        assertNotDone(context, vaper, schwagHit, "a hit of Purple Kush");

        ServerPlayerEntity piper = freshPlayer(context);
        hit(piper, world, packed(ModItems.WOODEN_PIPE, strain(world, ModStrains.SCHWAG), DeviceType.PIPE));
        assertDone(context, piper, schwagHit, "a pipe of schwag");
        assertNotDone(context, piper, vaporTrail, "a pipe");
        context.complete();
    }

    /**
     * The nodes keyed on obtaining an item, fired through vanilla's own trigger with the item really
     * in the inventory. Around the World is the one worth the test: five criteria ANDed, so the
     * resins may come one at a time — the inventory is emptied between them — and four is not five.
     */
    public static void obtainingGrantsTheExtractionNodes(TestContext context) {
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
            AdvancementEntry entry = loaded(context, node.id(), node.parent());
            ServerPlayerEntity player = freshPlayer(context);
            obtain(player, new ItemStack(Items.BREAD));
            assertNotDone(context, player, entry, "bread");
            obtain(player, new ItemStack(node.item()));
            assertDone(context, player, entry, "obtaining " + node.item());
        }

        // Cloud Nine is the cut, not the hashish: the press makes hashish before any bar exists.
        AdvancementEntry cloudNine = loaded(context, "cloud_nine", "hot_off_the_press");
        ServerPlayerEntity cutter = freshPlayer(context);
        obtain(cutter, new ItemStack(ModItems.HASHISH));
        assertNotDone(context, cutter, cloudNine, "holding hashish, with no bar ever cut");
        context.setBlockState(BED, ModBlocks.HASHISH_BAR);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        cutter.setStackInHand(Hand.MAIN_HAND, sword);
        BlockPos bar = context.getAbsolutePos(BED);
        cutter.interactionManager.interactBlock(cutter, context.getWorld(), sword, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(bar), Direction.UP, bar, false));
        context.assertTrue(context.getBlockState(BED).get(HashishBarBlock.CUTS) == 1, "the sword did not cut the bar, so this proves nothing");
        assertDone(context, cutter, cloudNine, "cutting a Hashish Bar with a sword");

        AdvancementEntry aroundTheWorld = loaded(context, "around_the_world_in_80_grams", "its_not_pollen");
        ServerPlayerEntity collector = freshPlayer(context);
        List<Item> resins = List.of(ModItems.CHARAS, ModItems.HASHISH, ModItems.FILTERED_HASHISH,
                ModItems.BUBBLE_HASH, ModItems.ROSIN);
        for (Item resin : resins) {
            context.assertFalse(collector.getAdvancementTracker().getProgress(aroundTheWorld).isDone(),
                    "Around the World was granted before " + resin + " was ever held — it wants all five");
            collector.getInventory().clear();
            obtain(collector, new ItemStack(resin));
        }
        assertDone(context, collector, aroundTheWorld, "all five resins, one at a time");
        context.complete();
    }

    /**
     * Breaking the bed under a ripe plant harvests the plant first, as the player, while the bed is
     * still under it. Any other way, the plant pops off after its bed has gone: Sea of Green and
     * Midnight Sun silently never fire, and the loot, which reads the same bed, pays the full stem.
     * Sea of Green is the proof the bed was there at the harvest; the stem count itself carries a
     * bonus roll, so it cannot be asserted exactly.
     */
    public static void breakingTheBedHarvestsThePlantFirst(TestContext context) {
        AdvancementEntry seaOfGreen = loaded(context, "sea_of_green", "pothead");
        AdvancementEntry midnightSun = loaded(context, "midnight_sun", "pothead");
        Block[] beds = {ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY};
        for (int i = 0; i < beds.length; i++) {
            BlockPos bed = new BlockPos(1 + 3 * i, 1, 1);
            context.setBlockState(bed, beds[i]);
            BlockState ripe = Defoliation.unworked(ModBlocks.INDICA_CROP.getDefaultState())
                    .with(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE)
                    .with(GrowLight.PROPERTY, GrowLight.GROW_LAMP);
            context.setBlockState(bed.up(), ripe.with(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER));
            context.setBlockState(bed.up(2), ripe.with(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER));
            ServerPlayerEntity player = freshPlayer(context);
            player.interactionManager.tryBreakBlock(context.getAbsolutePos(bed));

            String what = "breaking a " + beds[i].getTranslationKey() + " under a ripe, lamp-grown plant";
            context.assertTrue(context.getBlockState(bed.up()).isAir(), what + " left the plant standing");
            assertDone(context, player, midnightSun, what);
            if (beds[i] == ModBlocks.HYDRO_TRAY) {
                assertDone(context, player, seaOfGreen, what);
            }
        }
        context.complete();
    }

    /** A ripe Purple Kush on {@code bed}, harvested by a fresh survival player; returns the player. */
    private static ServerPlayerEntity harvestIndica(TestContext context, Block bed, GrowLight light,
                                                    boolean early, boolean late, boolean fromTop) {
        context.setBlockState(BED, bed);
        BlockState ripe = Defoliation.unworked(ModBlocks.INDICA_CROP.getDefaultState())
                .with(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE)
                .with(GrowLight.PROPERTY, light);
        context.setBlockState(CROP, ripe.with(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .with(Defoliation.TRIMMED_EARLY, early).with(Defoliation.TRIMMED_LATE, late));
        context.setBlockState(CROP.up(), ripe.with(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER));
        ServerPlayerEntity player = freshPlayer(context);
        player.interactionManager.tryBreakBlock(context.getAbsolutePos(fromTop ? CROP.up() : CROP));
        context.assertTrue(context.getBlockState(CROP).isAir(), "the plant was not harvested, so this proves nothing");
        return player;
    }

    private static RegistryEntry<Strain> strain(ServerWorld world, RegistryKey<Strain> key) {
        return world.getRegistryManager().getOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static ItemStack packed(Item device, RegistryEntry<Strain> strain, DeviceType type) {
        ItemStack stack = new ItemStack(device);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, 1));
        stack.set(ModComponents.CHARGES, type.builtInBowl().hits());
        return stack;
    }

    /** One hit through the real click, with every smokeable's shared cooldown wound back first. */
    private static void hit(ServerPlayerEntity player, ServerWorld world, ItemStack device) {
        for (Item item : ModItems.devices().values()) {
            player.getItemCooldownManager().remove(Registries.ITEM.getId(item));
        }
        player.setStackInHand(Hand.MAIN_HAND, device);
        player.interactionManager.interactItem(player, world, device, Hand.MAIN_HAND);
    }

    /**
     * Puts the stack in the inventory and fires {@code minecraft:inventory_changed} the way the
     * player's screen handler does when it syncs the slot — a mock player never ticks, so nothing
     * else would.
     */
    private static void obtain(ServerPlayerEntity player, ItemStack stack) {
        player.getInventory().insertStack(stack.copy());
        Criteria.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
    }

    private static void assertDone(TestContext context, ServerPlayerEntity player, AdvancementEntry entry,
                                   String what) {
        context.assertTrue(player.getAdvancementTracker().getProgress(entry).isDone(),
                what + " did not grant " + entry.id());
    }

    /** Fetches a loaded advancement and checks where it hangs, which is half of what it teaches. */
    private static AdvancementEntry loaded(TestContext context, String id, String parent) {
        AdvancementEntry entry = context.getWorld().getServer().getAdvancementLoader()
                .get(Identifier.of("hempdustry", id));
        context.assertTrue(entry != null, "hempdustry:" + id + " is not loaded at all");
        context.assertTrue(entry.value().parent().equals(Optional.of(Identifier.of("hempdustry", parent))),
                "hempdustry:" + id + " hangs under " + entry.value().parent() + ", not hempdustry:" + parent);
        return entry;
    }

    /** Survival, so the stack is really spent and the player is not treated as a creative tester. */
    private static ServerPlayerEntity freshPlayer(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        return player;
    }

    private static void consume(TestContext context, ServerPlayerEntity player, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.getItem().finishUsing(stack, context.getWorld(), player);
    }

    private static void assertNotDone(TestContext context, ServerPlayerEntity player, AdvancementEntry entry,
                                      String what) {
        context.assertFalse(player.getAdvancementTracker().getProgress(entry).isDone(),
                what + " granted " + entry.id() + ", so its criterion matches more than it should");
    }

    /**
     * Every advancement 2.0.x shipped, with the criterion names a player's file stores its progress
     * under. The game drops a saved criterion the advancement no longer has, so renaming or removing
     * one of these silently un-earns the advancement for everyone who had it: 2.1 renamed Trim
     * Season's {@code sheared_hemp_crop} into one criterion per trim age and revoked it for every 2.0.x
     * player (the 2.1.0-beta review, #1). Copied from the 2.0.2 jar's advancement files; 2.0.3 has the
     * same. New advancements and new criteria are free; these names are frozen (compat.md).
     */
    private static final Map<String, List<String>> SHIPPED_CRITERIA = Map.ofEntries(
            Map.entry("activation_energy", List.of("has_decarboxylator")),
            Map.entry("blaze_it", List.of("smoked_at_420")),
            Map.entry("bong_voyage", List.of("smoked_a_bong")),
            Map.entry("burnout", List.of("smoked_a_device_to_death")),
            Map.entry("butter_late_than_never", List.of("has_cannabutter")),
            Map.entry("chill_set", List.of("full_hemp_armor")),
            Map.entry("club_des_hashischins", List.of("ate_dawamesk")),
            Map.entry("first_contact", List.of("took_a_hit")),
            Map.entry("give_it_an_hour", List.of("ate_an_edible")),
            Map.entry("got_bhang", List.of("drank_bhang")),
            Map.entry("green_threads", List.of("has_hemp_fiber")),
            Map.entry("hemp_builder", List.of("has_hemp_brick")),
            Map.entry("hemp_hearts", List.of("ate_a_hemp_seed_food")),
            Map.entry("hempdustry", List.of("has_hemp_seeds")),
            Map.entry("hemprepreneurs", List.of("has_hemp_stem")),
            Map.entry("indica_strain", List.of("has_indica_buds")),
            Map.entry("parrot_tamer", List.of("fed_a_parrot_hemp_seeds", "tamed_a_parrot")),
            Map.entry("perfect_batch", List.of("has_perfect_cannabutter")),
            Map.entry("perfect_cut", List.of("harvested_fully_trimmed")),
            Map.entry("pipe_dream", List.of("smoked_a_pipe")),
            Map.entry("rinse_cycle", List.of("has_washed_hemp")),
            Map.entry("sativa_strain", List.of("has_sativa_buds")),
            Map.entry("trim_season", List.of("sheared_hemp_crop")),
            Map.entry("wake_and_bake", List.of("smoked_at_dawn")));

    /** Every advancement 2.0.x shipped still exists, and still has every criterion name it had. */
    public static void advancementCriteriaKeepTheirNames(TestContext context) {
        var loader = context.getWorld().getServer().getAdvancementLoader();
        for (Map.Entry<String, List<String>> shipped : SHIPPED_CRITERIA.entrySet()) {
            AdvancementEntry entry = loader.get(Identifier.of(Hempdustry.MOD_ID, shipped.getKey()));
            context.assertTrue(entry != null, "the 2.0.x advancement " + shipped.getKey() + " is gone");
            for (String criterion : shipped.getValue()) {
                context.assertTrue(entry.value().criteria().containsKey(criterion),
                        shipped.getKey() + " lost its 2.0.x criterion " + criterion + ", so every player who"
                                + " earned it loses it; it has " + entry.value().criteria().keySet());
            }
        }
        context.complete();
    }
}
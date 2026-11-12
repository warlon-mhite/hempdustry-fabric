package com.warlonmhite.hempdustry.test;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.buffer.Unpooled;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.stash.StashFill;
import com.warlonmhite.hempdustry.block.custom.stash.StashJarBlock;
import com.warlonmhite.hempdustry.block.custom.stash.StashPieces;
import com.warlonmhite.hempdustry.block.entity.custom.StashJarBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.component.StashContents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.StashJarItem;
import com.warlonmhite.hempdustry.platform.ItemTransfer;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.recipe.StashJarUpgradeRecipe;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The Stash Jar: a bundle for buds that stands as a block and drops itself whole. What no sweep states:
 * how much each jar holds and in what order it gives things back, what it refuses, the upgrade that
 * keeps the stash, every way a placed jar comes back as an item, the comparator, hoppers both ways, the
 * fill levels and how the pieces are shared out.
 */
public final class StashJarGameTest {
    private static final Map<Block, Integer> CAPACITY = Map.of(
            ModBlocks.STASH_JAR, 64, ModBlocks.GILDED_STASH_JAR, 128, ModBlocks.DIAMOND_STASH_JAR, 256);

    /** Each jar holds its capacity in items whatever their kinds, one entry per kind, does not stack, and gives the last kind in back first. */
    public static void aJarHoldsItsCapacityLastInFirstOut(GameTestHelper context) {
        for (Map.Entry<Block, Integer> jar : CAPACITY.entrySet()) {
            Item item = jar.getKey().asItem();
            context.assertTrue(item instanceof StashJarItem held && held.capacity() == jar.getValue(),
                    item + " does not hold " + jar.getValue());
            context.assertTrue(new ItemStack(item).getMaxStackSize() == 1, item + " stacks");
            StashContents.Mutable stash = StashContents.EMPTY.mutable();
            int in = 0;
            for (Item bud : List.of(ModItems.INDICA_BUDS, ModItems.SATIVA_BUDS, ModItems.SCHWAG, ModItems.INDICA_BUDS)) {
                in += stash.insert(new ItemStack(bud, 64), jar.getValue());
            }
            in += stash.insert(new ItemStack(ModItems.SATIVA_BUDS, 64), jar.getValue());
            context.assertTrue(in == Math.min(jar.getValue(), 320) && stash.count() == jar.getValue(),
                    item + " took " + in + " items, not its " + jar.getValue());
            context.assertTrue(stash.insert(new ItemStack(ModItems.INDICA_BUDS), jar.getValue()) == 0,
                    "a full " + item + " took one more bud");
            long kinds = stash.toImmutable().entries().stream().map(entry -> entry.item().typeHolder().value()).distinct().count();
            context.assertTrue(stash.toImmutable().size() == kinds, item + " kept " + stash.toImmutable().size()
                    + " entries for " + kinds + " kinds");
        }
        StashContents.Mutable order = StashContents.EMPTY.mutable();
        order.insert(new ItemStack(ModItems.INDICA_BUDS, 5), 64);
        order.insert(new ItemStack(ModItems.SCHWAG, 3), 64);
        order.insert(new ItemStack(ModItems.SATIVA_BUDS, 2), 64);
        // A kind already inside joins its entry, which comes to the front.
        order.insert(new ItemStack(ModItems.SCHWAG, 1), 64);
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack next = order.removeOne(); !next.isEmpty(); next = order.removeOne()) {
            out.add(next);
        }
        context.assertTrue(out.size() == 3 && out.get(0).is(ModItems.SCHWAG) && out.get(0).getCount() == 4
                        && out.get(1).is(ModItems.SATIVA_BUDS) && out.get(2).is(ModItems.INDICA_BUDS),
                "the jar gave back " + out + ", not the last stack in first");
        context.succeed();
    }

    /** Only the tag goes in: every bud of a plant and schwag, never a leaf, a stick, a bundle or another jar. */
    public static void aJarTakesOnlyBudsAndSchwag(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        for (var key : ModStrains.BUILT_IN) {
            Strain strain = level.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key).value();
            boolean bud = strain.wildFlower().isPresent();
            context.assertTrue(StashContents.storable(new ItemStack(strain.buds())) == bud,
                    strain.buds() + (bud ? " is a bud and the jar refuses it" : " is no bud and the jar takes it"));
        }
        context.assertTrue(StashContents.storable(new ItemStack(ModItems.SCHWAG)), "the jar refuses schwag");
        for (Item refused : List.of(Items.STICK, ModItems.HEMP_LEAF, Items.BUNDLE, Items.SHULKER_BOX,
                ModBlocks.STASH_JAR.asItem(), ModBlocks.DIAMOND_STASH_JAR.asItem())) {
            StashContents.Mutable stash = StashContents.EMPTY.mutable();
            context.assertTrue(stash.insert(new ItemStack(refused), 64) == 0 && stash.isEmpty(),
                    "a Stash Jar took " + refused);
        }
        context.succeed();
    }

    /** A ring of gold around a jar makes a Gilded one, diamonds around that a Diamond one, the stash and name kept; nothing else does. */
    public static void theUpgradeKeepsTheStash(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ItemStack jar = new ItemStack(ModBlocks.STASH_JAR);
        jar.set(ModComponents.STASH_CONTENTS, stash(ModItems.INDICA_BUDS, 40, ModItems.SCHWAG, 9));
        jar.set(DataComponents.CUSTOM_NAME, Component.literal("Grandma's"));
        ItemStack gilded = craft(context, ring(Items.GOLD_INGOT, jar));
        context.assertTrue(gilded.is(ModBlocks.GILDED_STASH_JAR.asItem()), "eight gold round a Stash Jar made " + gilded);
        context.assertTrue(gilded.getComponentsPatch().equals(jar.getComponentsPatch()),
                "the Gilded jar came out as " + gilded.getComponentsPatch() + ", not with " + jar.getComponentsPatch());
        ItemStack diamond = craft(context, ring(Items.DIAMOND, gilded));
        context.assertTrue(diamond.is(ModBlocks.DIAMOND_STASH_JAR.asItem())
                        && StashJarItem.contentsOf(diamond).equals(StashJarItem.contentsOf(jar)),
                "eight diamonds round a Gilded jar made " + diamond);
        // The chain only, and only a full ring.
        context.assertTrue(craft(context, ring(Items.DIAMOND, jar)).isEmpty(), "diamonds round a plain Stash Jar made something");
        context.assertTrue(craft(context, ring(Items.GOLD_INGOT, gilded)).isEmpty(), "gold round a Gilded jar made something");
        List<ItemStack> gap = ring(Items.GOLD_INGOT, jar);
        gap.set(0, Items.IRON_INGOT.getDefaultInstance());
        context.assertTrue(craft(context, gap).isEmpty(), "seven gold and an iron ingot made something");
        context.succeed();
    }

    /** By hand, by explosion and by piston a jar drops itself with everything inside; creative keeps the shulker box's rule. */
    public static void aJarAlwaysDropsItselfWhole(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        StashContents stash = stash(ModItems.SATIVA_BUDS, 30, ModItems.INDICA_BUDS, 3);
        ServerPlayer player = Players.survival(context);

        BlockPos byHand = place(context, new BlockPos(1, 2, 1), ModBlocks.STASH_JAR, stash);
        Players.breakAs(context, player, byHand);
        expectJar(context, byHand, ModBlocks.STASH_JAR, stash, "broken by hand");

        BlockPos blown = place(context, new BlockPos(6, 2, 6), ModBlocks.GILDED_STASH_JAR, stash);
        BlockPos at = context.absolutePos(blown);
        // Small, so it breaks the jar and none of the others: glass resists nothing.
        level.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 1.0F, Level.ExplosionInteraction.BLOCK);
        expectJar(context, blown, ModBlocks.GILDED_STASH_JAR, stash, "blown up");

        BlockPos pushed = place(context, new BlockPos(6, 2, 1), ModBlocks.DIAMOND_STASH_JAR, stash);
        context.setBlock(new BlockPos(5, 2, 1), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        context.setBlock(new BlockPos(5, 3, 1), Blocks.REDSTONE_BLOCK);

        ServerPlayer creative = Players.survival(context);
        creative.setGameMode(GameType.CREATIVE);
        BlockPos full = place(context, new BlockPos(3, 2, 3), ModBlocks.STASH_JAR, stash);
        Players.breakAs(context, creative, full);
        BlockPos empty = place(context, new BlockPos(3, 2, 6), ModBlocks.STASH_JAR, StashContents.EMPTY);
        Players.breakAs(context, creative, empty);

        context.runAfterDelay(10, () -> {
            // The piston's head stands where the jar stood.
            context.assertTrue(!(level.getBlockState(context.absolutePos(pushed)).getBlock() instanceof StashJarBlock),
                    "the piston did not pop the jar");
            expectJar(context, pushed, ModBlocks.DIAMOND_STASH_JAR, stash, "popped by a piston");
            expectJar(context, full, ModBlocks.STASH_JAR, stash, "broken in creative");
            context.assertTrue(drops(context, empty).stream().noneMatch(stack -> stack.is(ModBlocks.STASH_JAR.asItem())),
                    "an empty jar broken in creative dropped itself");
            context.succeed();
        });
    }


    /**
     * A jar set down in water keeps the water, as a lantern does: the jar is waterlogged, stays so when its fill
     * level changes, and leaves the water behind when it is broken, dropping itself whole. Set down in air it is dry.
     */
    public static void aJarSetInWaterKeepsIt(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = Players.survival(context);
        BlockPos wetFloor = new BlockPos(1, 1, 1);
        BlockPos dryFloor = new BlockPos(4, 1, 1);
        context.setBlock(wetFloor, Blocks.STONE);
        context.setBlock(dryFloor, Blocks.STONE);
        context.setBlock(wetFloor.above(), Blocks.WATER);
        Players.click(context, player, wetFloor, new ItemStack(ModBlocks.STASH_JAR));
        Players.click(context, player, dryFloor, new ItemStack(ModBlocks.STASH_JAR));
        BlockPos wet = wetFloor.above();
        BlockPos dry = dryFloor.above();

        BlockState wetState = level.getBlockState(context.absolutePos(wet));
        context.assertTrue(wetState.is(ModBlocks.STASH_JAR), "no jar was placed in the water: " + wetState);
        context.assertTrue(wetState.getValue(BlockStateProperties.WATERLOGGED), "a jar placed in water is not waterlogged");
        context.assertTrue(wetState.getFluidState().is(Fluids.WATER), "a waterlogged jar holds no water");
        BlockState dryState = level.getBlockState(context.absolutePos(dry));
        context.assertTrue(dryState.is(ModBlocks.STASH_JAR) && !dryState.getValue(BlockStateProperties.WATERLOGGED),
                "a jar placed in air is not a dry jar: " + dryState);

        Players.click(context, player, wet, new ItemStack(ModItems.INDICA_BUDS, 40));
        BlockState filled = level.getBlockState(context.absolutePos(wet));
        context.assertTrue(filled.getValue(StashFill.PROPERTY) != StashFill.EMPTY, "the buds did not go into the wet jar");
        context.assertTrue(filled.getValue(BlockStateProperties.WATERLOGGED), "the jar lost its water when its fill level changed");

        Players.breakAs(context, player, wet);
        context.assertTrue(level.getBlockState(context.absolutePos(wet)).is(Blocks.WATER),
                "breaking a waterlogged jar did not leave its water: " + level.getBlockState(context.absolutePos(wet)));
        expectJar(context, wet, ModBlocks.STASH_JAR, stash(ModItems.INDICA_BUDS, 40), "broken in water");
        context.succeed();
    }
    /** A comparator reads 0 from an empty jar, at least 1 with one item, 15 when full: the shulker box's reading. */
    public static void theComparatorReadsHowFullTheJarIs(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        for (Map.Entry<Block, Integer> jar : CAPACITY.entrySet()) {
            int cap = jar.getValue();
            BlockPos pos = context.absolutePos(place(context, new BlockPos(3, 2, 3), jar.getKey(), StashContents.EMPTY));
            int[][] expected = {{0, 0}, {1, 1}, {cap / 2, 8}, {cap - 1, 14}, {cap, 15}};
            for (int[] reading : expected) {
                StashJarBlockEntity entity = (StashJarBlockEntity) level.getBlockEntity(pos);
                entity.setContents(StashContents.EMPTY);
                if (reading[0] > 0) {
                    entity.setContents(stash(ModItems.INDICA_BUDS, reading[0]));
                }
                int signal = level.getBlockState(pos).getAnalogOutputSignal(level, pos, Direction.NORTH);
                context.assertTrue(signal == reading[1], jar.getKey() + " with " + reading[0] + " items reads " + signal
                        + ", not " + reading[1]);
            }
        }
        context.succeed();
    }

    /** A hopper above fills a jar with what the tag takes and nothing else; one below empties it, the last stack in first; a pipe finds it. */
    public static void hoppersFillAndEmptyAJar(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        BlockPos filled = place(context, new BlockPos(1, 2, 1), ModBlocks.STASH_JAR, StashContents.EMPTY);
        context.setBlock(filled.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity above = (HopperBlockEntity) level.getBlockEntity(context.absolutePos(filled.above()));
        above.setItem(0, new ItemStack(Items.STICK));
        above.setItem(1, new ItemStack(ModItems.INDICA_BUDS, 2));

        // Placed by a player sneaking onto the hopper's rim, as a player would.
        BlockPos emptied = new BlockPos(5, 3, 5);
        context.setBlock(emptied.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        ServerPlayer placer = Players.survival(context);
        placer.setShiftKeyDown(true);
        Players.click(context, placer, emptied.below(), new ItemStack(ModBlocks.STASH_JAR));
        context.assertTrue(level.getBlockEntity(context.absolutePos(emptied)) instanceof StashJarBlockEntity,
                "a Stash Jar could not be placed on a hopper");
        StashJarBlockEntity source = (StashJarBlockEntity) level.getBlockEntity(context.absolutePos(emptied));
        source.setContents(stash(ModItems.INDICA_BUDS, 1));
        source.insert(new ItemStack(ModItems.SCHWAG, 1));

        BlockPos piped = place(context, new BlockPos(1, 2, 5), ModBlocks.STASH_JAR, StashContents.EMPTY);
        BlockPos pipedAt = context.absolutePos(piped);
        context.assertTrue(Services.TRANSFER.insertWhole(level, pipedAt, Direction.UP, new ItemStack(ModItems.SATIVA_BUDS, 3))
                == ItemTransfer.Result.MOVED, "the loader's transfer API put no buds into a jar");
        context.assertTrue(Services.TRANSFER.insertWhole(level, pipedAt, Direction.UP, new ItemStack(Items.STICK))
                != ItemTransfer.Result.MOVED, "the loader's transfer API put a stick into a jar");

        context.runAfterDelay(40, () -> {
            StashContents in = ((StashJarBlockEntity) level.getBlockEntity(context.absolutePos(filled))).contents();
            context.assertTrue(in.count() == 2 && in.entries().getFirst().item().typeHolder().value() == ModItems.INDICA_BUDS,
                    "a hopper above filled the jar with " + in);
            context.assertTrue(above.getItem(0).is(Items.STICK), "a hopper above put its stick in the jar");
            HopperBlockEntity below = (HopperBlockEntity) level.getBlockEntity(context.absolutePos(emptied.below()));
            context.assertTrue(below.getItem(0).is(ModItems.SCHWAG),
                    "a hopper below took " + below.getItem(0) + " first, not the last stack put in (schwag)");
            context.assertTrue(((StashJarBlockEntity) level.getBlockEntity(pipedAt)).contents().count() == 3,
                    "the buds a pipe put in are not in the jar");
            context.succeed();
        });
    }

    /** A jar shows empty, low up to a third, mid up to two thirds, full above; one item is low; the placed block follows its stash. */
    public static void theFillLevelFollowsTheStash(GameTestHelper context) {
        for (int cap : CAPACITY.values()) {
            int third = cap / 3;
            int twoThirds = cap * 2 / 3;
            int[][] cases = {{0, 0}, {1, 1}, {third, 1}, {third + 1, 2}, {twoThirds, 2}, {twoThirds + 1, 3}, {cap, 3}};
            for (int[] c : cases) {
                StashFill fill = StashFill.of(c[0], cap);
                context.assertTrue(fill.ordinal() == c[1], c[0] + " of " + cap + " reads " + fill);
            }
        }
        ServerLevel level = context.getLevel();
        BlockPos pos = context.absolutePos(place(context, new BlockPos(3, 2, 3), ModBlocks.STASH_JAR, StashContents.EMPTY));
        StashJarBlockEntity jar = (StashJarBlockEntity) level.getBlockEntity(pos);
        jar.insert(new ItemStack(ModItems.INDICA_BUDS, 22));
        context.assertTrue(level.getBlockState(pos).getValue(StashFill.PROPERTY) == StashFill.MID,
                "22 buds in a Stash Jar stand as " + level.getBlockState(pos).getValue(StashFill.PROPERTY));
        jar.removeLast();
        context.assertTrue(level.getBlockState(pos).getValue(StashFill.PROPERTY) == StashFill.EMPTY,
                "an emptied jar still stands as " + level.getBlockState(pos).getValue(StashFill.PROPERTY));
        context.succeed();
    }

    /** The pieces go by each strain's share, largest remainder: 60 / 40 of five is 3 / 2; a strain's bands count together; bud-green bodies, strain-coloured pistils; schwag is brown. */
    public static void thePiecesShareTheStrains(GameTestHelper context) {
        context.assertTrue(Arrays.equals(StashPieces.share(new int[] {60, 40}, 5), new int[] {3, 2}),
                "60 / 40 of five pieces is " + Arrays.toString(StashPieces.share(new int[] {60, 40}, 5)));
        context.assertTrue(Arrays.equals(StashPieces.share(new int[] {10, 10, 10}, 5), new int[] {2, 2, 1}),
                "three equal shares of five pieces are " + Arrays.toString(StashPieces.share(new int[] {10, 10, 10}, 5)));
        ServerLevel level = context.getLevel();
        var strains = level.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY);
        // With no jar_colour of their own, the built-in strains' pistils take their strain colour.
        int purple = strains.getOrThrow(ModStrains.INDICA).value().color();
        int lemon = strains.getOrThrow(ModStrains.SATIVA).value().color();
        // A strain's high-band buds and its plain ones are one share.
        ItemStack high = new ItemStack(ModItems.INDICA_BUDS, 10);
        high.set(ModComponents.POTENCY_BAND, PotencyBand.HIGH);
        StashContents.Mutable mix = StashContents.EMPTY.mutable();
        mix.insert(new ItemStack(ModItems.INDICA_BUDS, 20), 64);
        mix.insert(new ItemStack(ModItems.SATIVA_BUDS, 20), 64);
        mix.insert(high, 64);
        StashPieces.Colours colours = mix.toImmutable().pieceColours(64, level.registryAccess());
        context.assertTrue(Arrays.equals(colours.pistils(), new int[] {purple, purple, purple, lemon, lemon}),
                "30 Purple Kush and 20 Lemon Haze show pistils " + hex(colours.pistils()));
        int green = StashPieces.BUD_GREEN;
        context.assertTrue(Arrays.equals(colours.bodies(), new int[] {green, green, green, green, green}),
                "the bud pieces' bodies are " + hex(colours.bodies()) + ", not all bud green");
        StashPieces.Colours schwag = stash(ModItems.SCHWAG, 1).pieceColours(64, level.registryAccess());
        context.assertTrue(Arrays.equals(schwag.bodies(), new int[] {StashPieces.SCHWAG_COLOUR})
                        && Arrays.equals(schwag.pistils(), new int[] {StashPieces.SCHWAG_COLOUR}),
                "one schwag shows a body " + hex(schwag.bodies()) + " and pistils " + hex(schwag.pistils()));
        context.assertTrue(StashContents.EMPTY.pieceColours(64, level.registryAccess()).count() == 0, "an empty jar shows pieces");
        context.succeed();
    }

    /** A strain file without {@code jar_colour} still loads, its pistils its own colour; one with it keeps it. */
    public static void aStrainsJarColourIsOptional(GameTestHelper context) {
        String plain = "{\"translation_key\":\"x\",\"color\":1,\"buds\":\"minecraft:wheat\"}";
        Strain without = Strain.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(plain)).getOrThrow();
        context.assertTrue(without.jarColour().isEmpty() && without.jarColourOrDefault() == 1,
                "a strain file without jar_colour (color 1) reads " + without.jarColourOrDefault());
        Strain with = Strain.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(plain.replace("}", ",\"jar_colour\":4660}"))).getOrThrow();
        context.assertTrue(with.jarColourOrDefault() == 4660, "jar_colour 4660 reads " + with.jarColourOrDefault());
        context.succeed();
    }

    /** Clicks: a storable stack goes in as far as it fits, an empty hand takes the last stack out, anything else is its own use. */
    public static void aPlacedJarTakesClicksNotMenus(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = Players.survival(context);
        BlockPos floor = new BlockPos(3, 1, 3);
        context.setBlock(floor, Blocks.STONE);
        Players.click(context, player, floor, new ItemStack(ModBlocks.STASH_JAR));
        BlockPos jarPos = floor.above();
        context.assertTrue(level.getBlockState(context.absolutePos(jarPos)).getBlock() instanceof StashJarBlock,
                "a Stash Jar did not stand on the floor");
        StashJarBlockEntity jar = (StashJarBlockEntity) level.getBlockEntity(context.absolutePos(jarPos));

        jar.setContents(stash(ModItems.SATIVA_BUDS, 60));
        Players.click(context, player, jarPos, new ItemStack(ModItems.INDICA_BUDS, 10));
        context.assertTrue(jar.contents().count() == 64 && player.getMainHandItem().getCount() == 6,
                "ten buds clicked into a jar with room for four left " + jar.contents().count() + " inside and "
                        + player.getMainHandItem().getCount() + " in hand");

        Players.click(context, player, jarPos, new ItemStack(Items.STICK));
        context.assertTrue(jar.contents().count() == 64 && player.getMainHandItem().is(Items.STICK),
                "a click holding a stick took something out of the jar");
        Players.click(context, player, jarPos, new ItemStack(Items.DIRT));
        context.assertTrue(level.getBlockState(context.absolutePos(jarPos.above())).is(Blocks.DIRT),
                "a click holding dirt on a jar did not place the dirt");

        Players.click(context, player, jarPos, ItemStack.EMPTY);
        context.assertTrue(player.getMainHandItem().is(ModItems.INDICA_BUDS) && player.getMainHandItem().getCount() == 4,
                "an empty hand took " + player.getMainHandItem() + " out, not the four buds put in last");

        // In the inventory: a stack clicked onto the jar goes in, a right-click with nothing carried takes it back out.
        ItemStack held = new ItemStack(ModBlocks.GILDED_STASH_JAR);
        Slot slot = new Slot(player.getInventory(), 9, 0, 0);
        slot.set(held);
        ItemStack[] carried = {new ItemStack(ModItems.SCHWAG, 7)};
        SlotAccess cursor = SlotAccess.of(() -> carried[0], stack -> carried[0] = stack);
        held.getItem().overrideOtherStackedOnMe(held, carried[0], slot, ClickAction.PRIMARY, player, cursor);
        context.assertTrue(StashJarItem.contentsOf(held).count() == 7 && carried[0].isEmpty(),
                "seven schwag clicked onto a jar in the inventory left " + StashJarItem.contentsOf(held).count() + " inside");
        held.getItem().overrideOtherStackedOnMe(held, ItemStack.EMPTY, slot, ClickAction.SECONDARY, player, cursor);
        context.assertTrue(carried[0].is(ModItems.SCHWAG) && carried[0].getCount() == 7 && StashJarItem.contentsOf(held).isEmpty(),
                "a right-click on a jar in the inventory took " + carried[0] + " out");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        context.succeed();
    }

    /**
     * A jar is placed only on a top face that holds a lantern or a rail (a hopper's rim does, a bottom
     * slab and air do not), and then needs nothing: taking the block below leaves it standing, stash and all.
     */
    public static void aJarStandsWhereItWasPut(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ServerPlayer player = Players.survival(context);
        player.setShiftKeyDown(true);
        Map<String, BlockState> floors = Map.of(
                "stone", Blocks.STONE.defaultBlockState(),
                "a hopper", Blocks.HOPPER.defaultBlockState(),
                "a fence", Blocks.OAK_FENCE.defaultBlockState(),
                "a bottom slab", Blocks.STONE_SLAB.defaultBlockState(),
                "farmland", Blocks.FARMLAND.defaultBlockState());
        Map<String, Boolean> expected = Map.of("stone", true, "a hopper", true, "a fence", true,
                "a bottom slab", false, "farmland", false);
        int x = 0;
        for (Map.Entry<String, BlockState> floor : floors.entrySet()) {
            BlockPos below = new BlockPos(x, 1, 1);
            x += 1;
            context.setBlock(below, floor.getValue());
            Players.click(context, player, below, new ItemStack(ModBlocks.STASH_JAR));
            boolean placed = level.getBlockState(context.absolutePos(below.above())).getBlock() instanceof StashJarBlock;
            context.assertTrue(placed == expected.get(floor.getKey()),
                    placed ? "a Stash Jar was placed on " + floor.getKey() : "a Stash Jar could not be placed on " + floor.getKey());
        }
        // Once placed, it needs nothing: the floor going leaves it where it stood, stash and all.
        StashContents stash = stash(ModItems.INDICA_BUDS, 12);
        BlockPos standing = place(context, new BlockPos(1, 2, 5), ModBlocks.GILDED_STASH_JAR, stash);
        context.setBlock(standing.below(), Blocks.AIR);
        context.runAfterDelay(5, () -> {
            context.assertTrue(level.getBlockState(context.absolutePos(standing)).is(ModBlocks.GILDED_STASH_JAR),
                    "a jar left without its floor did not stay");
            context.assertTrue(((StashJarBlockEntity) level.getBlockEntity(context.absolutePos(standing))).contents().equals(stash),
                    "a jar left without its floor lost its stash");
            context.assertTrue(drops(context, standing).isEmpty(), "a jar left without its floor dropped something");
            context.succeed();
        });
    }

    /**
     * What a jar carries and to whom: never into a shulker box or a bundle; to a client only the
     * pieces' colours and the count, never the stash; a scroll choice stays in the inventory; and a stack
     * taken out that finds no room goes back whole, whatever the tag says.
     */
    public static void aJarTravelsLight(GameTestHelper context) {
        ServerLevel level = context.getLevel();
        ItemStack full = new ItemStack(ModBlocks.DIAMOND_STASH_JAR);
        full.set(ModComponents.STASH_CONTENTS, stash(ModItems.INDICA_BUDS, 40, ModItems.SATIVA_BUDS, 20));
        context.assertTrue(!full.getItem().canFitInsideContainerItems(), "a Stash Jar fits inside a shulker box");
        context.assertTrue(!net.minecraft.world.item.component.BundleContents.canItemBeInBundle(full),
                "a Stash Jar fits inside a bundle");

        BlockPos pos = place(context, new BlockPos(3, 2, 3), ModBlocks.STASH_JAR, stash(ModItems.INDICA_BUDS, 30));
        StashJarBlockEntity jar = (StashJarBlockEntity) level.getBlockEntity(context.absolutePos(pos));
        var update = jar.getUpdateTag(level.registryAccess());
        context.assertTrue(!update.contains("contents") && update.getIntOr("count", -1) == 30
                        && update.getIntArray("piece_bodies").map(bodies -> bodies.length).orElse(-1) == jar.pieceColours().count(),
                "a jar's update for the client is " + update + ", not its pieces and count alone");

        // A jar carried with a scroll choice set is placed with none: the empty hand takes the newest.
        ItemStack chosen = new ItemStack(ModBlocks.STASH_JAR);
        chosen.set(ModComponents.STASH_CONTENTS, stash(ModItems.INDICA_BUDS, 5, ModItems.SATIVA_BUDS, 3));
        StashJarItem.toggleSelected(chosen, 1);
        context.assertTrue(StashJarItem.contentsOf(chosen).selected() == 1, "the scroll choice did not take");
        ServerPlayer player = Players.survival(context);
        BlockPos floor = new BlockPos(6, 1, 6);
        context.setBlock(floor, Blocks.STONE);
        Players.click(context, player, floor, chosen);
        StashJarBlockEntity placed = (StashJarBlockEntity) level.getBlockEntity(context.absolutePos(floor.above()));
        ItemStack out = placed.removeLast();
        context.assertTrue(out.is(ModItems.SATIVA_BUDS), "a jar placed with a scroll choice gave " + out + ", not the newest stack");

        // Taken out onto a slot that refuses it, the stack goes back in, even if the tag no longer takes it.
        ItemStack held = new ItemStack(ModBlocks.STASH_JAR);
        held.set(ModComponents.STASH_CONTENTS, stash(ModItems.INDICA_BUDS, 9));
        Slot refusing = new Slot(player.getInventory(), 10, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
        refusing.set(ItemStack.EMPTY);
        held.getItem().overrideStackedOnOther(held, refusing, ClickAction.SECONDARY, player);
        context.assertTrue(StashJarItem.contentsOf(held).count() == 9 && refusing.getItem().isEmpty(),
                "a stack taken out onto a refusing slot left " + StashJarItem.contentsOf(held).count() + " in the jar");
        StashContents.Mutable back = StashContents.EMPTY.mutable();
        back.putBack(new ItemStack(Items.STICK, 3));
        context.assertTrue(back.count() == 3, "a stack put back was filtered by the tag");

        // From the wire, as a creative client can write it: an entry of no buds, or fewer, is refused
        // as a save refuses it; one of two comes through.
        ItemStackTemplate bud = new ItemStackTemplate(ModItems.INDICA_BUDS.builtInRegistryHolder(), 1, DataComponentPatch.EMPTY);
        for (int count : new int[]{0, -3, 2}) {
            RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            ItemStackTemplate.STREAM_CODEC.encode(wire, bud);
            ByteBufCodecs.VAR_INT.encode(wire, count);
            ByteBufCodecs.VAR_INT.encode(wire, count);
            ByteBufCodecs.VAR_INT.encode(wire, 0);
            StashContents.Entry read = null;
            try {
                read = StashContents.Entry.STREAM_CODEC.decode(wire);
            } catch (DecoderException refused) {
                // what an empty entry must meet
            }
            context.assertTrue(count < 1 ? read == null : read != null && read.count() == count,
                    "an entry of " + count + " buds from the wire read " + read);
        }
        context.succeed();
    }

    /**
     * The jar blends each strain (round 21): tier and band are levelled exactly, nothing made or lost,
     * never past the inputs, order-free, no gain from taking out and putting back, best out first, and
     * the tooltip names what comes out; a chemotype never blends, nor a stack carrying anything else.
     */
    public static void aJarBlendsEachStrainHonestly(GameTestHelper context) {
        // The spec's table.
        expectLevelled(context, List.of(bud(32, BudTier.DANK, PotencyBand.MID), bud(32, BudTier.TOP_SHELF, PotencyBand.MID)),
                List.of(bud(32, BudTier.TOP_SHELF, PotencyBand.MID), bud(32, BudTier.DANK, PotencyBand.MID)));
        expectLevelled(context, List.of(bud(10, BudTier.MIDS, PotencyBand.MID), bud(1, BudTier.EPIC, PotencyBand.MID)),
                List.of(bud(3, BudTier.DANK, PotencyBand.MID), bud(8, BudTier.MIDS, PotencyBand.MID)));
        expectLevelled(context, List.of(bud(32, BudTier.MIDS, PotencyBand.MID), bud(32, BudTier.EPIC, PotencyBand.MID)),
                List.of(bud(32, BudTier.TOP_SHELF, PotencyBand.MID), bud(32, BudTier.DANK, PotencyBand.MID)));
        expectLevelled(context, List.of(bud(20, BudTier.DANK, PotencyBand.LOW), bud(20, BudTier.DANK, PotencyBand.HIGH)),
                List.of(bud(40, BudTier.DANK, PotencyBand.MID)));
        // Both axes at once, the best tier paired with the best band: three stacks at most.
        expectLevelled(context, List.of(bud(3, BudTier.EPIC, PotencyBand.EXTRA_HIGH), bud(7, BudTier.MIDS, PotencyBand.LOW)),
                List.of(bud(9, BudTier.DANK, PotencyBand.MID), bud(1, BudTier.MIDS, PotencyBand.LOW)));
        expectLevelled(context, List.of(bud(1, BudTier.EPIC, PotencyBand.EXTRA_HIGH), bud(1, BudTier.TOP_SHELF, PotencyBand.MID),
                        bud(2, BudTier.MIDS, PotencyBand.MID)),
                List.of(bud(1, BudTier.TOP_SHELF, PotencyBand.HIGH), bud(1, BudTier.DANK, PotencyBand.HIGH), bud(2, BudTier.DANK, PotencyBand.MID)));
        // A bud made before tiers and bands reads Dank and Mid.
        expectLevelled(context, List.of(new ItemStack(ModItems.INDICA_BUDS, 10), bud(10, BudTier.EPIC, PotencyBand.MID)),
                List.of(bud(20, BudTier.TOP_SHELF, PotencyBand.MID)));

        // Order-free, and never past the inputs.
        List<ItemStack> mix = List.of(bud(5, BudTier.EPIC, PotencyBand.HIGH), bud(9, BudTier.MIDS, PotencyBand.LOW),
                bud(4, BudTier.TOP_SHELF, PotencyBand.EXTRA_HIGH), bud(13, BudTier.DANK, PotencyBand.MID), new ItemStack(ModItems.INDICA_BUDS, 6));
        StashContents forward = blend(mix);
        List<ItemStack> backward = new ArrayList<>(mix);
        java.util.Collections.reverse(backward);
        List<ItemStack> shuffled = new ArrayList<>(mix);
        java.util.Collections.shuffle(shuffled, new java.util.Random(4242));
        context.assertTrue(forward.equals(blend(backward)) && forward.equals(blend(shuffled)),
                "one strain's buds blend to " + forward + " in one order and " + blend(backward) + " in another");
        for (ItemStack part : forward.parts()) {
            context.assertTrue(BudTier.of(part).rank() <= BudTier.EPIC.rank() && BudTier.of(part).rank() >= BudTier.MIDS.rank()
                            && PotencyBand.of(part).offset() <= PotencyBand.EXTRA_HIGH.offset(),
                    "a blend gave " + part + ", past what went in");
        }
        // Nothing made or lost: the tier steps and band steps out are the ones in (the oven bakes by band).
        context.assertTrue(rankSum(forward.parts()) == rankSum(mix) && offsetSum(forward.parts()) == offsetSum(mix),
                "the blend's steps out " + rankSum(forward.parts()) + "/" + offsetSum(forward.parts())
                        + " are not the steps in " + rankSum(mix) + "/" + offsetSum(mix));

        // Best out first, by hand and by pipe alike, and taking out then putting back changes nothing.
        StashContents.Mutable byHand = forward.mutable();
        ItemStack first = byHand.removeOne();
        context.assertTrue(ItemStack.isSameItemSameComponents(first, forward.parts().getFirst())
                        && first.getCount() == forward.parts().getFirst().getCount(),
                "an empty hand took " + first + ", not the best stack " + forward.parts().getFirst());
        List<ItemStack> byPipe = forward.mutable().extract(stack -> true, first.getCount());
        context.assertTrue(byPipe.size() == 1 && ItemStack.matches(byPipe.getFirst(), first),
                "a pipe took " + byPipe + " where a hand took " + first);
        StashContents.Mutable loop = forward.mutable();
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack next = loop.removeOne(); !next.isEmpty(); next = loop.removeOne()) {
            out.add(next);
        }
        for (ItemStack back : out) {
            loop.insert(back, Integer.MAX_VALUE);
        }
        context.assertTrue(loop.toImmutable().equals(forward), "taking a blend out and putting it back made " + loop.toImmutable()
                + " of " + forward);

        // A chemotype never blends: schwag is one entry per chemotype; strains never blend with each other.
        StashContents.Mutable schwag = StashContents.EMPTY.mutable();
        schwag.insert(new ItemStack(ModItems.SCHWAG, 5), 256);
        schwag.insert(Chemotype.stamp(new ItemStack(ModItems.SCHWAG, 5), Chemotype.CBD_DOMINANT), 256);
        schwag.insert(new ItemStack(ModItems.SATIVA_BUDS, 5), 256);
        schwag.insert(new ItemStack(ModItems.INDICA_BUDS, 5), 256);
        context.assertTrue(schwag.toImmutable().size() == 4, "THC and CBD schwag and two strains made "
                + schwag.toImmutable().size() + " entries, not four");
        // Anything else on a stack keeps it exact.
        ItemStack named = bud(4, BudTier.EPIC, PotencyBand.MID);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Grandma's"));
        StashContents.Mutable exact = StashContents.EMPTY.mutable();
        exact.insert(named.copy(), 64);
        exact.insert(bud(4, BudTier.MIDS, PotencyBand.MID), 64);
        exact.insert(named.copy(), 64);
        ItemStack namedMids = bud(4, BudTier.MIDS, PotencyBand.MID);
        namedMids.set(DataComponents.CUSTOM_NAME, Component.literal("Grandma's"));
        exact.insert(namedMids.copy(), 64);
        StashContents kept = exact.toImmutable();
        ItemStack namedOut = kept.entries().get(1).parts().getFirst();
        ItemStack namedMidsOut = kept.entries().getFirst().parts().getFirst();
        context.assertTrue(kept.size() == 3 && namedOut.getCount() == 8 && ItemStack.isSameItemSameComponents(namedOut, named)
                        && ItemStack.matches(namedMidsOut, namedMids),
                "named Epic and Mids stacks blended into " + kept);

        // The tooltip line names exactly what comes out: Purple Kush ×11: 3 Dank, 8 Mids.
        StashContents.Entry eleven = blend(List.of(bud(10, BudTier.MIDS, PotencyBand.MID), bud(1, BudTier.EPIC, PotencyBand.MID))).entries().getFirst();
        Component line = StashJarItem.entryLine(eleven);
        Object[] args = ((net.minecraft.network.chat.contents.TranslatableContents) line.getContents()).getArgs();
        List<String> parts = ((Component) args[2]).getSiblings().stream()
                .filter(part -> part.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents)
                .map(part -> {
                    Object[] a = ((net.minecraft.network.chat.contents.TranslatableContents) part.getContents()).getArgs();
                    return a[0] + " " + ((net.minecraft.network.chat.contents.TranslatableContents) ((Component) a[1]).getContents()).getKey();
                }).toList();
        context.assertTrue(args[1].equals(11) && parts.equals(List.of("3 hempdustry.tier.dank", "8 hempdustry.tier.mids")),
                "the tooltip says ×" + args[1] + " " + parts + " for 3 Dank and 8 Mids");
        // Schwag names no chemotype, only its count.
        StashContents.Entry cbd = schwag.toImmutable().entries().get(2);
        Component schwagLine = StashJarItem.entryLine(cbd);
        context.assertTrue(((net.minecraft.network.chat.contents.TranslatableContents) schwagLine.getContents()).getKey()
                        .equals("item.hempdustry.stash_jar.entry_plain"),
                "a CBD schwag entry's tooltip line is " + schwagLine + ", not its name and count alone");
        context.succeed();
    }

    private static ItemStack bud(int count, BudTier tier, PotencyBand band) {
        return PotencyBand.stamp(BudTier.stamp(new ItemStack(ModItems.INDICA_BUDS, count), tier), band);
    }

    private static StashContents blend(List<ItemStack> stacks) {
        StashContents.Mutable stash = StashContents.EMPTY.mutable();
        for (ItemStack stack : stacks) {
            stash.insert(stack.copy(), Integer.MAX_VALUE);
        }
        return stash.toImmutable();
    }

    private static void expectLevelled(GameTestHelper context, List<ItemStack> in, List<ItemStack> expected) {
        StashContents stash = blend(in);
        List<ItemStack> parts = stash.parts();
        boolean same = stash.size() == 1 && parts.size() == expected.size();
        for (int i = 0; same && i < parts.size(); i++) {
            same = ItemStack.matches(parts.get(i), expected.get(i));
        }
        context.assertTrue(same, describe(in) + " blended to " + describe(parts) + ", not " + describe(expected));
    }

    private static int rankSum(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(stack -> stack.getCount() * BudTier.of(stack).rank()).sum();
    }

    private static int offsetSum(List<ItemStack> stacks) {
        return stacks.stream().mapToInt(stack -> stack.getCount() * PotencyBand.of(stack).offset()).sum();
    }

    private static String describe(List<ItemStack> stacks) {
        return stacks.stream().map(stack -> stack.getCount() + " " + BudTier.of(stack) + "/" + PotencyBand.of(stack)).toList().toString();
    }

    private static StashContents stash(Object... kindsAndCounts) {
        StashContents.Mutable stash = StashContents.EMPTY.mutable();
        for (int i = 0; i < kindsAndCounts.length; i += 2) {
            stash.insert(new ItemStack((Item) kindsAndCounts[i], (Integer) kindsAndCounts[i + 1]), Integer.MAX_VALUE);
        }
        return stash.toImmutable();
    }

    /** A jar standing at {@code pos} on stone, holding {@code stash}; returns {@code pos}. */
    private static BlockPos place(GameTestHelper context, BlockPos pos, Block jar, StashContents stash) {
        context.setBlock(pos.below(), Blocks.STONE);
        context.setBlock(pos, jar.defaultBlockState());
        ((StashJarBlockEntity) context.getLevel().getBlockEntity(context.absolutePos(pos))).setContents(stash);
        return pos;
    }

    private static List<ItemStack> ring(Item material, ItemStack jar) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(i == 4 ? jar.copy() : new ItemStack(material));
        }
        return grid;
    }

    /** What the game's own crafting gives for that grid, through the recipe manager, or empty. */
    private static ItemStack craft(GameTestHelper context, List<ItemStack> grid) {
        ServerLevel level = context.getLevel();
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                .filter(holder -> holder.value() instanceof StashJarUpgradeRecipe)
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    private static List<ItemStack> drops(GameTestHelper context, BlockPos pos) {
        List<ItemStack> found = new ArrayList<>();
        for (ItemEntity drop : context.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(context.absolutePos(pos)).inflate(0.75), entity -> true)) {
            found.add(drop.getItem());
        }
        return found;
    }

    private static void expectJar(GameTestHelper context, BlockPos pos, Block jar, StashContents stash, String how) {
        List<ItemStack> dropped = drops(context, pos);
        context.assertTrue(dropped.stream().anyMatch(stack -> stack.is(jar.asItem())
                        && StashJarItem.contentsOf(stack).equals(stash)),
                "a jar " + how + " dropped " + dropped + ", not itself with its stash");
        context.assertTrue(dropped.stream().noneMatch(stack -> StashContents.storable(stack)),
                "a jar " + how + " spilled its stash");
    }

    private static String hex(int[] colours) {
        StringBuilder out = new StringBuilder("[");
        for (int colour : colours) {
            out.append(String.format(" #%06X", colour));
        }
        return out.append(" ]").toString();
    }
}

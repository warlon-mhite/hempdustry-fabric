package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The mod's single creative tab.
 *
 * <h2>How the order is decided</h2>
 *
 * <b>Mojang's rules govern what happens inside a section; the mod decides the section order</b>,
 * because vanilla would scatter this content across seven tabs and we have one. The rules, all read
 * out of {@code CreativeModeTabs} rather than remembered:
 *
 * <ul>
 *   <li><b>Group by kind, not by variant.</b> Vanilla's Natural tab runs every seed together —
 *       wheat, cocoa, pumpkin, melon, beetroot, torchflower, pitcher — rather than grouping each
 *       plant's parts. So the tab lists every strain's seeds, then every strain's buds, then every
 *       strain's flowers.</li>
 *   <li><b>Fixed shape order within a family.</b> Wood is planks, stairs, slab, fence, fence gate,
 *       door, trapdoor, pressure plate, button. Stone-like is block, stairs, slab, wall.</li>
 *   <li><b>Raw before refined</b> — raw ore, nugget, ingot; beef then cooked beef.</li>
 *   <li><b>Finished before its powder.</b> Colored Blocks lists concrete <em>before</em> concrete
 *       powder, which is the opposite of the order you use them in.</li>
 *   <li><b>Drinks last.</b> Food & Drinks ends with the milk bucket and the honey bottle.</li>
 *   <li><b>Colour runs use vanilla's dye order</b> (white, light gray, gray, black, brown, red,
 *       orange, yellow, lime, green, cyan, light blue, blue, purple, magenta, pink) — the shape any
 *       future hempcrete colours or tinted bongs should take.</li>
 * </ul>
 *
 * <p>Section order is the compromise, and it runs <b>by play</b>: the plant, the machines, what they
 * turn it into, then what is made of that, then the building sets last — the same content that would
 * move to a second tab if one is ever needed. Vanilla's own tab order (building blocks first) would
 * open a hemp mod with planks, which is exactly backwards for a mod whose subject is the plant.
 *
 * <h2>Future-proofing</h2>
 *
 * Every section is a <b>container for a kind of thing</b>, not a fixed list, so planned content lands
 * in an obvious place without a reshuffle:
 *
 * <ul>
 *   <li><b>A third strain</b> costs no code here at all — the plant runs and the smokeables are both
 *       driven off the strain registry.</li>
 *   <li><b>New work blocks</b> (press, drying rack, mixer, extractor, grow light) append to the
 *       machine run.</li>
 *   <li><b>New transformed hemp</b> (hashish, oil, rosin) appends to the processed run, which is
 *       deliberately the <em>strain-agnostic</em> section — that is what those items will be.</li>
 *   <li><b>New smoking gear</b> appends to the smoking run, empty form then one per strain.</li>
 *   <li><b>Hempcrete colours</b> extend the hempcrete run as a dye-ordered colour block.</li>
 * </ul>
 *
 * <p><b>Row alignment is deliberately not chased.</b> The grid is nine wide and entries flow
 * continuously — vanilla pads nothing either, and there is no filler slot to pad with. Tuning a
 * section to land on a row boundary would only survive until the next item was added, which is the
 * opposite of the goal.
 */
public class ModItemGroups {
    public static final CreativeModeTab HEMPDUSTRY_ITEMS_GROUP = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "hempdustry_items"),
            // The leaf, not a strain's buds: it is the plant's iconic silhouette, it is the one
            // strain-agnostic part, and it stays right when a third strain lands.
            Services.REGISTRATION.creativeTabBuilder().icon(() -> new ItemStack(ModItems.HEMP_LEAF))
                    .title(Component.translatable("itemgroup.hempdustry"))
                    .displayItems((displayContext, entries) -> {
                        // --- The plant -------------------------------------------------
                        // One run per kind, in the strain registry's own stable order, so a strain
                        // added or edited by a datapack slots into all three runs without touching
                        // this file. The tab reads displayContext.holders() for that, exactly as
                        // vanilla's Food & Drinks reads the potion registry.
                        List<Holder.Reference<Strain>> strains = Strain.all(displayContext.holders());
                        // Only a strain that grew on a plant has seeds and a flower — hashish has
                        // neither, and its buds go in the processed run below with the rest of the
                        // strain-agnostic material. See Strain.
                        //
                        // A datapack strain may name another strain's items, and the tab throws on
                        // a second identical stack, so each item goes in once, where it first comes.
                        Set<Item> plant = new HashSet<>();
                        Consumer<ItemLike> addOnce = item -> {
                            if (plant.add(item.asItem())) {
                                entries.accept(item);
                            }
                        };
                        strains.forEach(strain -> strain.value().seedItem().ifPresent(addOnce));
                        strains.forEach(strain -> {
                            if (strain.value().wildFlower().isPresent()) {
                                addOnce.accept(strain.value().buds());
                            }
                        });
                        // The bud gone wrong, at the end of the buds it can come from. Not a plant
                        // strain, so the run above never adds it.
                        entries.accept(ModItems.SCHWAG);
                        // Beldía's wild flower is its own crop, and a crop block's item IS its
                        // seeds -- already in the run above, which addOnce remembers. A flower with
                        // no item at all is skipped: the tab refuses an empty stack.
                        strains.forEach(strain -> strain.value().wildFlower().map(Block::asItem)
                                .filter(item -> item != Items.AIR)
                                .ifPresent(addOnce));
                        entries.accept(ModItems.HEMP_STEM);
                        entries.accept(ModItems.RETTED_HEMP_STEM);
                        entries.accept(ModItems.HEMP_LEAF);
                        // The two storage blocks, where vanilla keeps the hay bale: Natural, with
                        // the plant, not with the building sets.
                        entries.accept(ModBlocks.HEMP_BALE);
                        entries.accept(ModBlocks.HEMP_LEAVES);

                        // --- Work blocks -----------------------------------------------
                        // In unlock order. The press, drying rack, mixer and extractor go here.
                        entries.accept(ModBlocks.DECARBOXYLATOR);
                        entries.accept(ModBlocks.INFUSER);
                        entries.accept(ModBlocks.SIFTING_BOX);
                        entries.accept(ModBlocks.HEMP_PRESS);
                        entries.accept(ModBlocks.GROW_POT);
                        entries.accept(ModBlocks.GROW_LAMP);
                        entries.accept(ModBlocks.HYDRO_TRAY);

                        // --- Processed hemp, raw to refined ----------------------------
                        // Strain-agnostic by construction: decarboxylation is where strain
                        // identity ends. Hashish, oil and rosin belong in this run.
                        entries.accept(ModItems.DECARBOXYLATED_HEMP);
                        entries.accept(ModItems.WASHED_DECARBOXYLATED_HEMP);
                        // Beside the oven's two, because it is the Infuser's third input -- the
                        // furnace's cruder answer to the same heat, and what a vaporizer hands back.
                        entries.accept(ModItems.SCORCHED_HEMP);
                        // The two loose intermediates sit before the pressed forms they become,
                        // which is the order the chain runs in.
                        entries.accept(ModItems.KIEF);
                        entries.accept(ModItems.FILTERED_KIEF);
                        entries.accept(ModItems.BUBBLE_HASH);
                        // The bar first, then the piece it cuts into: the order a player meets them.
                        // Charas last of the family -- it is the one you can have before you own the
                        // block, but it is also the rarest, and the run reads bar -> piece -> pinch.
                        entries.accept(ModBlocks.HASHISH_BAR);
                        entries.accept(ModItems.HASHISH);
                        entries.accept(ModBlocks.CHARAS_BALL);
                        entries.accept(ModItems.CHARAS);
                        // Bar then piece again, so the run reads as two matching pairs with the
                        // one unpressed material between them.
                        entries.accept(ModBlocks.FILTERED_HASHISH_BAR);
                        entries.accept(ModItems.FILTERED_HASHISH);
                        entries.accept(ModItems.ROSIN);
                        // One per plant strain, each already loaded the way its recipe loads it --
                        // a moon rock with no smoke_contents is an inert nug and would be a lie in
                        // the tab, so the bare item is deliberately not listed. The grid shows the
                        // hashish coat; search lists every coat, as it does every dose.
                        ModItems.showcaseMoonRocks(displayContext.holders())
                                .forEach(stack -> entries.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY));
                        ModItems.moonRocks(displayContext.holders())
                                .forEach(stack -> entries.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY));
                        entries.accept(ModItems.showcaseCannabutter(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);

                        // --- Fibre and building materials ------------------------------
                        entries.accept(ModItems.HEMP_FIBER);
                        entries.accept(ModItems.HEMP_CANVAS);
                        entries.accept(ModItems.HEMPCRETE);
                        entries.accept(ModItems.HEMP_BRICK);

                        // --- Food: the ingredient, then what is made of it --------------
                        // No THC in this run, which is why it sits before the edibles.
                        // Stew before drink, drink last — vanilla's Food & Drinks ordering.
                        entries.accept(ModItems.HEMP_FLOUR);
                        entries.accept(ModItems.TOASTED_HEMP_SEEDS);
                        entries.accept(ModItems.HEMP_FLAPJACK);
                        entries.accept(ModItems.SIEMIENIOTKA);
                        entries.accept(ModItems.HEMP_MILK_BUCKET);

                        // --- Edibles ---------------------------------------------------
                        // Baked run (vanilla's bread, cookie, cake), then the confection,
                        // then the drink. The grid gets the floor of both axes; the whole
                        // potency × quality matrix goes to the search tab, as the enchanted
                        // books do. Cannabutter's own search stacks ride along in allDosed().
                        ModItems.showcaseEdibles()
                                .forEach(stack -> entries.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY));
                        ModItems.allDosed()
                                .forEach(stack -> entries.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY));

                        // --- Smoking ---------------------------------------------------
                        // Gear-free first, then the devices in progression order; each device
                        // empty, then one per strain. Doses live in the search tab only.
                        ModItems.showcaseSmokeables(displayContext.holders())
                                .forEach(stack -> entries.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY));
                        ModItems.allSmokeables(displayContext.holders())
                                .forEach(stack -> entries.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY));
                        // The glass run, empty only -- the way vanilla follows the bundle with its
                        // sixteen dyed ones. A packed coloured bong differs from a packed clear one
                        // by glass alone, so listing its loads would be the matrix all over again.
                        ModItems.COLORED_BONGS.forEach(entries::accept);

                        // --- Armour, head to foot --------------------------------------
                        entries.accept(ModItems.HEMP_BEANIE);
                        entries.accept(ModItems.HEMP_SHIRT);
                        entries.accept(ModItems.HEMP_HAREM_PANTS);
                        entries.accept(ModItems.FLIP_FLOPS);

                        // --- Building: the wood family, in vanilla's order --------------
                        entries.accept(ModBlocks.HEMP_PLANKS);
                        entries.accept(ModBlocks.HEMP_PLANKS_STAIRS);
                        entries.accept(ModBlocks.HEMP_PLANKS_SLAB);
                        entries.accept(ModBlocks.HEMP_PLANKS_FENCE);
                        entries.accept(ModBlocks.HEMP_PLANKS_FENCE_GATE);
                        entries.accept(ModBlocks.HEMP_PLANKS_DOOR);
                        entries.accept(ModBlocks.HEMP_PLANKS_TRAPDOOR);
                        entries.accept(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE);
                        entries.accept(ModBlocks.HEMP_PLANKS_BUTTON);

                        // --- Building: parquet, the planks' decorative cut ------------
                        // After the family rather than woven through it the way vanilla weaves
                        // bamboo mosaic, so the planks keep vanilla's shape order unbroken.
                        entries.accept(ModBlocks.HEMP_PARQUET);
                        entries.accept(ModBlocks.HEMP_PARQUET_STAIRS);
                        entries.accept(ModBlocks.HEMP_PARQUET_SLAB);

                        // --- Building: the brick family, in vanilla's order -------------
                        entries.accept(ModBlocks.HEMP_BRICKS_BLOCK);
                        entries.accept(ModBlocks.HEMP_BRICKS_STAIRS);
                        entries.accept(ModBlocks.HEMP_BRICKS_SLAB);
                        entries.accept(ModBlocks.HEMP_BRICKS_WALL);

                        // --- Building: hempcrete, then cloth and the bed it makes -------
                        // Set block before powder, the way Colored Blocks lists concrete
                        // before concrete powder. Colour variants extend this run.
                        entries.accept(ModBlocks.HEMPCRETE_BLOCK);
                        entries.accept(ModBlocks.HEMPCRETE_POWDER_BLOCK);
                        entries.accept(ModBlocks.HEMP_WOOL);
                        entries.accept(ModBlocks.HEMP_CARPET);
                        entries.accept(ModBlocks.HEMP_BED);

                        // --- What the wood set carries, then the discs ------------------
                        // Vanilla's own tab order: signs and shelves are Functional, boats and
                        // discs Tools.
                        entries.accept(ModItems.HEMP_PLANKS_SIGN);
                        entries.accept(ModItems.HEMP_PLANKS_HANGING_SIGN);
                        entries.accept(ModBlocks.HEMP_PLANKS_SHELF);
                        // Vanilla's Tools tab puts the harnesses just before the boats.
                        entries.accept(ModItems.HEMP_HARNESS);
                        entries.accept(ModItems.HEMP_BOAT);
                        entries.accept(ModItems.HEMP_CHEST_BOAT);
                        ModItems.MUSIC_DISCS.forEach(entries::accept);
                    }).build());

    public static void registerItemGroups(){
        Hempdustry.LOGGER.info("Registering Item Groups for "+ Hempdustry.MOD_ID);
    }
}

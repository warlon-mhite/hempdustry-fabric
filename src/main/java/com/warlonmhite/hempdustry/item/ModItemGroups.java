package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The mod's single creative tab.
 *
 * <h2>How the order is decided</h2>
 *
 * <b>Mojang's rules govern what happens inside a section; the mod decides the section order</b>,
 * because vanilla would scatter this content across seven tabs and we have one. The rules, all read
 * out of {@code ItemGroups} rather than remembered:
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
    public static final ItemGroup HEMPDUSTRY_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(Hempdustry.MOD_ID, "hempdustry_items"),
            // The leaf, not a strain's buds: it is the plant's iconic silhouette, it is the one
            // strain-agnostic part, and it stays right when a third strain lands.
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.HEMP_LEAF))
                    .displayName(Text.translatable("itemgroup.hempdustry"))
                    .entries((displayContext, entries) -> {
                        // --- The plant -------------------------------------------------
                        // One run per kind, in the strain registry's own stable order, so a strain
                        // added or edited by a datapack slots into all three runs without touching
                        // this file. The tab reads displayContext.lookup() for that, exactly as
                        // vanilla's Food & Drinks reads the potion registry.
                        List<RegistryEntry.Reference<Strain>> strains = Strain.all(displayContext.lookup());
                        strains.forEach(strain -> entries.add(strain.value().seeds()));
                        strains.forEach(strain -> entries.add(strain.value().buds()));
                        strains.forEach(strain -> entries.add(strain.value().flower()));
                        entries.add(ModItems.HEMP_STEM);
                        entries.add(ModItems.RETTED_HEMP_STEM);
                        entries.add(ModItems.HEMP_LEAF);
                        // The crop's storage block, where vanilla keeps the hay bale: Natural,
                        // with the plant, not with the building sets.
                        entries.add(ModBlocks.HEMP_BALE);

                        // --- Work blocks -----------------------------------------------
                        // In unlock order. The press, drying rack, mixer and extractor go here.
                        entries.add(ModBlocks.DECARBOXYLATOR);
                        entries.add(ModBlocks.INFUSER);
                        entries.add(ModBlocks.DRY_SIFTER);

                        // --- Processed hemp, raw to refined ----------------------------
                        // Strain-agnostic by construction: decarboxylation is where strain
                        // identity ends. Hashish, oil and rosin belong in this run.
                        entries.add(ModItems.DECARBOXYLATED_HEMP);
                        entries.add(ModItems.WASHED_DECARBOXYLATED_HEMP);
                        entries.add(ModItems.HASHISH);
                        entries.add(ModItems.showcaseCannabutter(), ItemGroup.StackVisibility.PARENT_TAB_ONLY);

                        // --- Fibre and building materials ------------------------------
                        entries.add(ModItems.HEMP_FIBER);
                        entries.add(ModItems.HEMP_CANVAS);
                        entries.add(ModItems.HEMPCRETE);
                        entries.add(ModItems.HEMP_BRICK);

                        // --- Food: the ingredient, then what is made of it --------------
                        // No THC in this run, which is why it sits before the edibles.
                        // Stew before drink, drink last — vanilla's Food & Drinks ordering.
                        entries.add(ModItems.HEMP_FLOUR);
                        entries.add(ModItems.TOASTED_HEMP_SEEDS);
                        entries.add(ModItems.HEMP_FLAPJACK);
                        entries.add(ModItems.SIEMIENIOTKA);
                        entries.add(ModItems.HEMP_MILK_BUCKET);

                        // --- Edibles ---------------------------------------------------
                        // Baked run (vanilla's bread, cookie, cake), then the confection,
                        // then the drink. The grid gets the floor of both axes; the whole
                        // potency × quality matrix goes to the search tab, as the enchanted
                        // books do. Cannabutter's own search stacks ride along in allDosed().
                        ModItems.showcaseEdibles()
                                .forEach(stack -> entries.add(stack, ItemGroup.StackVisibility.PARENT_TAB_ONLY));
                        ModItems.allDosed()
                                .forEach(stack -> entries.add(stack, ItemGroup.StackVisibility.SEARCH_TAB_ONLY));

                        // --- Smoking ---------------------------------------------------
                        // Gear-free first, then the devices in progression order; each device
                        // empty, then one per strain. Doses live in the search tab only.
                        ModItems.showcaseSmokeables(displayContext.lookup())
                                .forEach(stack -> entries.add(stack, ItemGroup.StackVisibility.PARENT_TAB_ONLY));
                        ModItems.allSmokeables(displayContext.lookup())
                                .forEach(stack -> entries.add(stack, ItemGroup.StackVisibility.SEARCH_TAB_ONLY));

                        // --- Armour, head to foot --------------------------------------
                        entries.add(ModItems.HEMP_BEANIE);
                        entries.add(ModItems.HEMP_SHIRT);
                        entries.add(ModItems.HEMP_HAREM_PANTS);
                        entries.add(ModItems.FLIP_FLOPS);

                        // --- Building: the wood family, in vanilla's order --------------
                        entries.add(ModBlocks.HEMP_PLANKS);
                        entries.add(ModBlocks.HEMP_PLANKS_STAIRS);
                        entries.add(ModBlocks.HEMP_PLANKS_SLAB);
                        entries.add(ModBlocks.HEMP_PLANKS_FENCE);
                        entries.add(ModBlocks.HEMP_PLANKS_FENCE_GATE);
                        entries.add(ModBlocks.HEMP_PLANKS_DOOR);
                        entries.add(ModBlocks.HEMP_PLANKS_TRAPDOOR);
                        entries.add(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE);
                        entries.add(ModBlocks.HEMP_PLANKS_BUTTON);

                        // --- Building: the brick family, in vanilla's order -------------
                        entries.add(ModBlocks.HEMP_BRICKS_BLOCK);
                        entries.add(ModBlocks.HEMP_BRICKS_STAIRS);
                        entries.add(ModBlocks.HEMP_BRICKS_SLAB);
                        entries.add(ModBlocks.HEMP_BRICKS_WALL);

                        // --- Building: hempcrete, then cloth ----------------------------
                        // Set block before powder, the way Colored Blocks lists concrete
                        // before concrete powder. Colour variants extend this run.
                        entries.add(ModBlocks.HEMPCRETE_BLOCK);
                        entries.add(ModBlocks.HEMPCRETE_POWDER_BLOCK);
                        entries.add(ModBlocks.HEMP_WOOL);
                        entries.add(ModBlocks.HEMP_CARPET);

                        // --- What the wood set carries, then the discs ------------------
                        // Vanilla's own tab order: signs are Functional, boats and discs Tools.
                        entries.add(ModItems.HEMP_PLANKS_SIGN);
                        entries.add(ModItems.HEMP_PLANKS_HANGING_SIGN);
                        entries.add(ModItems.HEMP_BOAT);
                        entries.add(ModItems.HEMP_CHEST_BOAT);
                        ModItems.MUSIC_DISCS.forEach(entries::add);
                    }).build());

    public static void registerItemGroups(){
        Hempdustry.LOGGER.info("Registering Item Groups for "+ Hempdustry.MOD_ID);
    }
}

package com.warlonmhite.hempdustry.compat.rei;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.client.BuiltinClientPlugin;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The REI half of the recipe-viewer support. Loaded by REI through the {@code rei_client}
 * entrypoint in {@code fabric.mod.json} and by nothing else, so a player without REI never touches
 * this class.
 *
 * <p>The item comparators live next door in {@link HempdustryReiCommonPlugin}: REI moved
 * {@code registerItemComparators} off {@code REIPlugin} and onto {@code REICommonPlugin}, which is
 * a different entrypoint. On the 1.21.1 branch, against REI 16, it is still on the client plugin.
 *
 * <p>What is shown, and why each thing is shown that way, is in {@link ViewerRecipes}. This is the
 * adapter.
 */
public class HempdustryReiPlugin implements REIClientPlugin {

    private static final CategoryIdentifier<EntryReiDisplay> DECARBOXYLATING =
            CategoryIdentifier.of(ViewerRecipes.DECARBOXYLATING);
    private static final CategoryIdentifier<EntryReiDisplay> INFUSING =
            CategoryIdentifier.of(ViewerRecipes.INFUSING);
    private static final CategoryIdentifier<EntryReiDisplay> CAULDRON =
            CategoryIdentifier.of(ViewerRecipes.CAULDRON);
    private static final CategoryIdentifier<EntryReiDisplay> PRESSING =
            CategoryIdentifier.of(ViewerRecipes.PRESSING);
    private static final CategoryIdentifier<EntryReiDisplay> SIFTING =
            CategoryIdentifier.of(ViewerRecipes.SIFTING);
    private static final CategoryIdentifier<EntryReiDisplay> ICE_O_LATOR =
            CategoryIdentifier.of(ViewerRecipes.ICE_O_LATOR);
    private static final CategoryIdentifier<EntryReiDisplay> WORLD =
            CategoryIdentifier.of(ViewerRecipes.WORLD);

    /** Kept so each can be handed its rows and measure its page off all of them. */
    private final Map<CategoryIdentifier<EntryReiDisplay>, EntryReiCategory> categories = new HashMap<>();

    @Override
    public void registerCategories(CategoryRegistry registry) {
        // The note-line counts are fixed per category rather than measured, because REI asks for a
        // category's height once and uses it for every page in it.
        add(registry, DECARBOXYLATING, ModBlocks.DECARBOXYLATOR, 1);
        add(registry, INFUSING, ModBlocks.INFUSER, 4);
        add(registry, CAULDRON, Blocks.WATER_CAULDRON, 1);
        add(registry, PRESSING, ModBlocks.HEMP_PRESS, 2);
        add(registry, SIFTING, ModBlocks.SIFTING_BOX, 1);
        add(registry, ICE_O_LATOR, ModBlocks.SIFTING_BOX, 2);
        add(registry, WORLD, Items.SHEARS, 2);

        // The block you stand in front of to do the thing. REI draws these beside the category and
        // lets a player click one to get here from the item.
        registry.addWorkstations(DECARBOXYLATING, EntryStacks.of(ModBlocks.DECARBOXYLATOR));
        registry.addWorkstations(INFUSING, EntryStacks.of(ModBlocks.INFUSER));
        registry.addWorkstations(CAULDRON, EntryStacks.of(Blocks.WATER_CAULDRON),
                EntryStacks.of(Blocks.CAULDRON));
        registry.addWorkstations(PRESSING, EntryStacks.of(ModBlocks.HEMP_PRESS));
        registry.addWorkstations(SIFTING, EntryStacks.of(ModBlocks.SIFTING_BOX));
        registry.addWorkstations(ICE_O_LATOR, EntryStacks.of(ModBlocks.SIFTING_BOX));
    }

    private void add(CategoryRegistry registry, CategoryIdentifier<EntryReiDisplay> id,
                     ItemConvertible icon, int noteLines) {
        EntryReiCategory category = new EntryReiCategory(id.getIdentifier(), icon, noteLines);
        categories.put(id, category);
        registry.add(category);
    }

    /** Hands a category its rows, so it can size every page to the widest, and registers them. */
    private void add(DisplayRegistry registry, CategoryIdentifier<EntryReiDisplay> id,
                     List<ViewerRecipes.Entry> entries) {
        categories.get(id).fitted(entries);
        for (ViewerRecipes.Entry entry : entries) {
            registry.add(new EntryReiDisplay(id, entry));
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            // REI is building its index before a world is joined. Both machine categories read the
            // recipe manager and packing reads the strain registry, and neither exists yet; REI
            // rebuilds on world join, which is when this becomes answerable.
            return;
        }

        add(registry, DECARBOXYLATING, ViewerRecipes.decarboxylating(client.world));
        add(registry, INFUSING, ViewerRecipes.infusing(client.world));
        add(registry, CAULDRON, ViewerRecipes.cauldron());
        add(registry, PRESSING, ViewerRecipes.pressing(client.world));
        add(registry, SIFTING, ViewerRecipes.sifting(client.world));
        add(registry, ICE_O_LATOR, ViewerRecipes.iceOLator(client.world));
        add(registry, WORLD, ViewerRecipes.world(client.world));
        ViewerRecipes.info().forEach((item, text) -> BuiltinClientPlugin.getInstance()
                .registerInformation(EntryStacks.of(item), item.getName(), lines -> {
                    lines.add(text);
                    return lines;
                }));

        // Packing goes into REI's own crafting category rather than one of ours, which is what
        // makes REI's built-in "move ingredients into the grid" work on it without a transfer
        // handler of our own. See ViewerRecipes#packing. Each slot is built from stacks, so the
        // moon rock shows its load and the bong slot every glass.
        for (ViewerRecipes.Packing packing : ViewerRecipes.packing(client.world.getRegistryManager())) {
            registry.add(new DefaultCustomShapelessDisplay(
                    packing.inputs().stream().map(EntryIngredients::ofItemStacks).toList(),
                    List.of(EntryIngredients.of(packing.output())),
                    Optional.empty()));
        }
    }
}

package com.warlonmhite.hempdustry.compat.rei;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

import java.util.List;
import java.util.Optional;

/**
 * The REI half of the recipe-viewer support. Loaded by REI through the {@code rei_client}
 * entrypoint in {@code fabric.mod.json} and by nothing else, so a player without REI never touches
 * this class.
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

    /**
     * Tells REI that what is loaded makes a smokeable a different item.
     *
     * <p>Without this every packed pipe, bong and spliff collapses into one entry in the item list,
     * and so do the packing rows below, which differ only by their output's components. Keyed on
     * the smoke contents alone; see {@link ViewerRecipes#smokeKey} for why damage and charges are
     * deliberately left out — {@code registerComponents} would have been one line, and would have
     * split the item list into an entry per durability point.
     *
     * <p><b>This is a client plugin doing a common plugin's job, and REI allows it here.</b> The
     * comparator registry belongs to REI's <i>common</i> plugin manager, which is fed by
     * {@code REIPlugin.class.isAssignableFrom(provider.getPluginProviderClass())} — true for a
     * client plugin — so a class registered under {@code rei_client} lands in both managers. REI
     * moved this method onto {@code REICommonPlugin} later; on the 1.21.11 branch it is therefore a
     * second class behind a {@code rei_common} entrypoint.
     */
    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        for (Item smokeable : ViewerRecipes.smokeables()) {
            registry.register((context, stack) -> ViewerRecipes.smokeKey(stack).hashCode(), smokeable);
        }
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        // The note-line counts are fixed per category rather than measured, because REI asks for a
        // category's height once and uses it for every page in it.
        registry.add(new EntryReiCategory(ViewerRecipes.DECARBOXYLATING, ModBlocks.DECARBOXYLATOR, 1));
        registry.add(new EntryReiCategory(ViewerRecipes.INFUSING, ModBlocks.INFUSER, 2));
        registry.add(new EntryReiCategory(ViewerRecipes.CAULDRON, Blocks.WATER_CAULDRON, 1));

        // The block you stand in front of to do the thing. REI draws these beside the category and
        // lets a player click one to get here from the item.
        registry.addWorkstations(DECARBOXYLATING, EntryStacks.of(ModBlocks.DECARBOXYLATOR));
        registry.addWorkstations(INFUSING, EntryStacks.of(ModBlocks.INFUSER));
        registry.addWorkstations(CAULDRON, EntryStacks.of(Blocks.WATER_CAULDRON),
                EntryStacks.of(Blocks.CAULDRON));
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

        for (ViewerRecipes.Entry entry : ViewerRecipes.decarboxylating(registry.getRecipeManager())) {
            registry.add(new EntryReiDisplay(DECARBOXYLATING, entry));
        }
        for (ViewerRecipes.Entry entry : ViewerRecipes.infusing(registry.getRecipeManager())) {
            registry.add(new EntryReiDisplay(INFUSING, entry));
        }
        for (ViewerRecipes.Entry entry : ViewerRecipes.cauldron()) {
            registry.add(new EntryReiDisplay(CAULDRON, entry));
        }

        // Packing goes into REI's own crafting category rather than one of ours, which is what
        // makes REI's built-in "move ingredients into the grid" work on it without a transfer
        // handler of our own. See ViewerRecipes#packing.
        for (ViewerRecipes.Packing packing : ViewerRecipes.packing(client.world.getRegistryManager())) {
            registry.add(DefaultCustomShapelessDisplay.simple(
                    EntryIngredients.ofIngredients(packing.inputs()),
                    List.of(EntryIngredients.of(packing.output())),
                    Optional.empty()));
        }
    }
}

package com.warlonmhite.hempdustry.compat.rei;

import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Tells REI that what is loaded makes a smokeable a different item, and what stamped matter carries
 * (its chemotype and a leaf's high mark) a different entry.
 *
 * <p>Without this every packed pipe, bong and spliff collapses into one entry in the item list, and
 * so do the packing rows in {@link HempdustryReiPlugin}, which differ only by their output's
 * components. Keyed on the smoke contents alone; see {@link ViewerRecipes#smokeKey} for why damage
 * and charges are deliberately left out — {@code ItemComparatorRegistry#registerComponents} would
 * have been one line, and would have split the item list into an entry per durability point.
 *
 * <p><b>Why this is a second class.</b> {@code registerItemComparators} sits on
 * {@code REICommonPlugin} rather than on the client plugin, and REI loads common plugins from a
 * separate {@code rei_common} entrypoint. Nothing here is client-only, which is the point: a
 * dedicated server running REI hashes stacks the same way the client does.
 */
public class HempdustryReiCommonPlugin implements REICommonPlugin {

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        for (Item smokeable : ViewerRecipes.smokeables()) {
            registry.register((context, stack) -> ViewerRecipes.smokeKey(stack).hashCode(), smokeable);
        }
        // A CBD leaf, a balanced hash, a high-potency leaf: each its own entry, as in JEI, so a stamped
        // recipe output (a Beldía bud's scorched hemp is balanced) shows what it really makes. Exact
        // comparisons only: a recipe takes the item whatever its stamp, and REI matches recipes fuzzily,
        // so a CBD leaf still finds the recipes a plain one does. A plain stack hashes as before.
        EntryComparator<ItemStack> stamp = (context, stack) -> ViewerRecipes.stampKey(stack).hashCode();
        for (Item stamped : ViewerRecipes.stamped()) {
            registry.register(stamp.onlyExact(), stamped);
        }
    }
}

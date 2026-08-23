package com.warlonmhite.hempdustry.compat.emi;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The EMI half of the recipe-viewer support. Loaded by EMI through the {@code emi} entrypoint in
 * {@code fabric.mod.json} and by nothing else, so a player without EMI never touches this class.
 *
 * <p>What is shown, and why each thing is shown that way, is in {@link ViewerRecipes}. This is the
 * adapter.
 */
public class HempdustryEmiPlugin implements EmiPlugin {

    private static final EmiRecipeCategory DECARBOXYLATING =
            category(ViewerRecipes.DECARBOXYLATING, ModBlocks.DECARBOXYLATOR);
    private static final EmiRecipeCategory INFUSING =
            category(ViewerRecipes.INFUSING, ModBlocks.INFUSER);
    private static final EmiRecipeCategory CAULDRON =
            category(ViewerRecipes.CAULDRON, Blocks.WATER_CAULDRON);

    /**
     * A category whose name comes from <b>this mod's</b> lang key rather than EMI's.
     *
     * <p>{@code EmiRecipeCategory.getName()} defaults to {@code emi.category.<namespace>.<path>},
     * which would mean maintaining a second set of titles in eight locales purely so EMI and JEI
     * could say the same words. Overriding it points both viewers at one key.
     */
    private static EmiRecipeCategory category(Identifier id, ItemConvertible icon) {
        return new EmiRecipeCategory(id, EmiStack.of(icon)) {
            @Override
            public Text getName() {
                return Text.translatable("hempdustry.category." + id.getPath());
            }
        };
    }

    @Override
    public void register(EmiRegistry registry) {
        // What is loaded makes a device a different entry in the item list, and a different recipe
        // output. Without this every packed pipe folds into the empty one -- and so do the ten
        // packing rows below, which differ only by their output's components. Keyed on the smoke
        // contents alone; see ViewerRecipes#smokeKey for why damage and charges are left out.
        for (net.minecraft.item.Item smokeable : ViewerRecipes.smokeables()) {
            registry.setDefaultComparison(EmiStack.of(smokeable),
                    Comparison.compareData(stack -> ViewerRecipes.smokeKey(stack.getItemStack())));
        }

        registry.addCategory(DECARBOXYLATING);
        registry.addCategory(INFUSING);
        registry.addCategory(CAULDRON);

        // The block you stand in front of to do the thing. EMI draws these beside the category and
        // lets a player click one to get here from the item.
        registry.addWorkstation(DECARBOXYLATING, EmiStack.of(ModBlocks.DECARBOXYLATOR));
        registry.addWorkstation(INFUSING, EmiStack.of(ModBlocks.INFUSER));
        registry.addWorkstation(CAULDRON, EmiStack.of(Blocks.WATER_CAULDRON));
        registry.addWorkstation(CAULDRON, EmiStack.of(Blocks.CAULDRON));

        for (ViewerRecipes.Entry entry : ViewerRecipes.decarboxylating(registry.getRecipeManager())) {
            registry.addRecipe(new EntryEmiRecipe(DECARBOXYLATING, entry));
        }
        for (ViewerRecipes.Entry entry : ViewerRecipes.infusing(registry.getRecipeManager())) {
            registry.addRecipe(new EntryEmiRecipe(INFUSING, entry));
        }
        for (ViewerRecipes.Entry entry : ViewerRecipes.cauldron()) {
            registry.addRecipe(new EntryEmiRecipe(CAULDRON, entry));
        }

        // Packing goes in EMI's own crafting category rather than one of ours, which is what makes
        // "move ingredients into the grid" work on it without a line of transfer code. See
        // ViewerRecipes#packing.
        RegistryWrapper.WrapperLookup registries = registries();
        if (registries != null) {
            for (ViewerRecipes.Packing packing : ViewerRecipes.packing(registries)) {
                registry.addRecipe(new EmiCraftingRecipe(
                        packing.inputs().stream().map(HempdustryEmiPlugin::ingredient).toList(),
                        EmiStack.of(packing.output()),
                        ViewerRecipes.synthetic(packing.id()),
                        true));
            }
        }
    }

    private static EmiIngredient ingredient(Ingredient ingredient) {
        return EmiIngredient.of(List.of(ingredient.getMatchingStacks()).stream().map(EmiStack::of).toList());
    }

    /**
     * The loaded registries, for the strains packing has to enumerate.
     *
     * <p>Taken off the client's world rather than a static, because strains are a <b>datapack</b>
     * registry: what exists depends on the server that was joined, and EMI rebuilds its index on
     * every reload. A null world means EMI is registering before a world is joined, in which case
     * there are no strains to enumerate yet and it will run again once there are.
     */
    private static RegistryWrapper.WrapperLookup registries() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? null : client.world.getRegistryManager();
    }
}

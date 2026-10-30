package com.warlonmhite.hempdustry.compat.jei;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;

/**
 * The JEI half of the recipe-viewer support.
 *
 * <h2>Two things are needed to be found, and only one of them is obvious</h2>
 *
 * <b>On Fabric, {@link JeiPlugin} alone does nothing.</b> JEI finds plugins through a Fabric
 * entrypoint named {@code jei_mod_plugin} — see {@code mezz.jei.fabric.startup.FabricPluginFinder},
 * and JEI's own {@code fabric.mod.json}, which registers its four built-in plugins that way. With
 * only the annotation this class is never constructed, JEI never mentions it in its startup log,
 * and every category here is silently missing. The annotation is kept because it is what the Forge
 * side reads and it says what this class is; the entrypoint is what actually loads it here.
 *
 * <p>Being an entrypoint is also what keeps it optional: nothing loads it unless JEI is installed,
 * so a player without JEI never touches this class.
 *
 * <p>What is shown, and why each thing is shown that way, is in {@link ViewerRecipes}. This is the
 * adapter.
 */
@JeiPlugin
public class HempdustryJeiPlugin implements IModPlugin {

    private EntryJeiCategory decarboxylating;
    private EntryJeiCategory infusing;
    private EntryJeiCategory cauldron;
    private EntryJeiCategory pressing;
    private EntryJeiCategory sifting;
    private EntryJeiCategory iceOLator;
    private EntryJeiCategory world;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "jei");
    }

    /**
     * Tells JEI that what is loaded makes a smokeable a different item.
     *
     * <p>JEI reports the need for this itself on startup — <i>"5 duplicate items were found in
     * 'Hempdustry' creative tab's: displayItems … may indicate that these types of item need a
     * subtype interpreter"</i> — and without it every packed pipe, bong and spliff collapses into
     * one entry in the item list. It would also break the packing recipes below, which differ only
     * by their output's components.
     */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (net.minecraft.world.item.Item smokeable : ViewerRecipes.smokeables()) {
            registration.registerSubtypeInterpreter(smokeable,
                    (ItemStack stack, UidContext context) -> ViewerRecipes.smokeKey(stack));
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        // The note-line counts are fixed per category rather than measured, because JEI asks for a
        // category's height once and uses it for every page in it.
        decarboxylating = new EntryJeiCategory(gui, ViewerRecipes.DECARBOXYLATING,
                new ItemStack(ModBlocks.DECARBOXYLATOR), 1);
        infusing = new EntryJeiCategory(gui, ViewerRecipes.INFUSING,
                new ItemStack(ModBlocks.INFUSER), 4);
        cauldron = new EntryJeiCategory(gui, ViewerRecipes.CAULDRON,
                new ItemStack(Blocks.WATER_CAULDRON), 1);
        pressing = new EntryJeiCategory(gui, ViewerRecipes.PRESSING,
                new ItemStack(ModBlocks.HEMP_PRESS), 2);
        // Both of the Sifting Box's modes, catalysed by the same block. Two categories rather than
        // one because the water changes what comes out, and one page could not say that.
        sifting = new EntryJeiCategory(gui, ViewerRecipes.SIFTING,
                new ItemStack(ModBlocks.SIFTING_BOX), 1);
        iceOLator = new EntryJeiCategory(gui, ViewerRecipes.ICE_O_LATOR,
                new ItemStack(ModBlocks.SIFTING_BOX), 2);
        world = new EntryJeiCategory(gui, ViewerRecipes.WORLD, new ItemStack(Items.SHEARS), 2);
        registration.addRecipeCategories(decarboxylating, infusing, cauldron, pressing, sifting, iceOLator, world);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            // JEI is building its index before a world is joined. Both machine categories read the
            // recipe manager and packing reads the strain registry, and neither exists yet; JEI
            // rebuilds on world join, which is when this becomes answerable.
            return;
        }

        registration.addRecipes(decarboxylating.getRecipeType(), decarboxylating.fitted(ViewerRecipes.decarboxylating(client.level)));
        registration.addRecipes(infusing.getRecipeType(), infusing.fitted(ViewerRecipes.infusing(client.level)));
        registration.addRecipes(cauldron.getRecipeType(), cauldron.fitted(ViewerRecipes.cauldron()));
        registration.addRecipes(pressing.getRecipeType(), pressing.fitted(ViewerRecipes.pressing(client.level)));
        registration.addRecipes(sifting.getRecipeType(), sifting.fitted(ViewerRecipes.sifting(client.level)));
        registration.addRecipes(iceOLator.getRecipeType(), iceOLator.fitted(ViewerRecipes.iceOLator(client.level)));
        registration.addRecipes(world.getRecipeType(), world.fitted(ViewerRecipes.world(client.level)));
        ViewerRecipes.info().forEach((item, text) -> registration.addIngredientInfo(item, text));

        // Packing goes into JEI's own crafting category rather than one of ours, which is what makes
        // JEI's built-in "move ingredients into the grid" work on it without a transfer handler of
        // our own. See ViewerRecipes#packing.
        registration.addRecipes(RecipeTypes.CRAFTING, packingAsCrafting(client.level.registryAccess()));
    }

    /**
     * Each packing permutation as a plain shapeless recipe.
     *
     * <p>These are display-only objects and are deliberately <b>not</b> registered with the game:
     * the real {@code hempdustry:packing} special recipe is what actually crafts, and it already
     * matches exactly these ingredients. Building a {@link ShapelessRecipe} here is a way of
     * describing that match in the one shape JEI's crafting category and its transfer handler both
     * understand.
     *
     * <p><b>JEI draws a crafting recipe from its {@code display()}, not its ingredients</b>, and
     * a display slot can hold a stack with components where an {@code Ingredient} cannot. That is
     * how the moon rock slot shows the loaded moon rock, and the bong slot every glass; the
     * ingredients underneath are the bare items, which is all a recipe can match on anyway.
     */
    private static List<RecipeHolder<CraftingRecipe>> packingAsCrafting(HolderLookup.Provider registries) {
        List<RecipeHolder<CraftingRecipe>> out = new ArrayList<>();
        for (ViewerRecipes.Packing packing : ViewerRecipes.packing(registries)) {
            List<Ingredient> ingredients = packing.inputs().stream()
                    .map(stacks -> Ingredient.of(stacks.stream().map(ItemStack::getItem)))
                    .toList();
            List<SlotDisplay> slots = packing.inputs().stream().map(HempdustryJeiPlugin::display).toList();
            RecipeDisplay display = new ShapelessCraftingRecipeDisplay(slots,
                    new SlotDisplay.ItemStackSlotDisplay(packing.output()),
                    new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE));
            // A RecipeHolder is keyed by a ResourceKey<Recipe<?>> since 1.21.4, and a shapeless
            // recipe takes a plain List rather than a NonNullList.
            out.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, packing.id()),
                    new ShapelessRecipe("", CraftingBookCategory.MISC, packing.output(), ingredients) {
                        @Override
                        public List<RecipeDisplay> display() {
                            return List.of(display);
                        }
                    }));
        }
        return out;
    }

    /** One slot's stacks, exactly as given: a single stack, or a list JEI cycles through. */
    private static SlotDisplay display(List<ItemStack> stacks) {
        if (stacks.size() == 1) {
            return new SlotDisplay.ItemStackSlotDisplay(stacks.getFirst());
        }
        return new SlotDisplay.Composite(stacks.stream()
                .<SlotDisplay>map(SlotDisplay.ItemStackSlotDisplay::new).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // The block you stand in front of. JEI shows these beside the category and lets a player
        // click one to get to its recipes from the item itself.
        registration.addCraftingStation(decarboxylating.getRecipeType(), ModBlocks.DECARBOXYLATOR);
        registration.addCraftingStation(infusing.getRecipeType(), ModBlocks.INFUSER);
        registration.addCraftingStation(cauldron.getRecipeType(), Blocks.WATER_CAULDRON, Blocks.CAULDRON);
        registration.addCraftingStation(pressing.getRecipeType(), ModBlocks.HEMP_PRESS);
        registration.addCraftingStation(sifting.getRecipeType(), ModBlocks.SIFTING_BOX);
        registration.addCraftingStation(iceOLator.getRecipeType(), ModBlocks.SIFTING_BOX);
    }
}

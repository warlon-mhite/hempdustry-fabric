package com.warlonmhite.hempdustry.compat.jei;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.compat.ViewerRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import com.warlonmhite.hempdustry.registry.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import com.warlonmhite.hempdustry.registry.DataRegistries;

/**
 * The JEI half of the recipe-viewer support.
 *
 * <p>Forge finds it by {@link JeiPlugin}, and only when JEI is installed, so a player without JEI
 * never touches this class. It lives in the Forge project because JEI for 1.16.5 is a Forge jar
 * only, with no build against plain Minecraft for common code to compile with.
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
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Hempdustry.MOD_ID, "jei");
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
                    (IIngredientSubtypeInterpreter<ItemStack>) (stack, context) -> ViewerRecipes.smokeKey(stack));
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
                new ItemStack(Blocks.CAULDRON), 1);
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
        net.minecraft.world.item.crafting.RecipeManager recipes = client.level.getRecipeManager();

        registration.addRecipes(decarboxylating.fitted(ViewerRecipes.decarboxylating(recipes)), decarboxylating.getUid());
        registration.addRecipes(infusing.fitted(ViewerRecipes.infusing(recipes)), infusing.getUid());
        registration.addRecipes(cauldron.fitted(ViewerRecipes.cauldron()), cauldron.getUid());
        registration.addRecipes(pressing.fitted(ViewerRecipes.pressing(recipes)), pressing.getUid());
        registration.addRecipes(sifting.fitted(ViewerRecipes.sifting()), sifting.getUid());
        registration.addRecipes(iceOLator.fitted(ViewerRecipes.iceOLator()), iceOLator.getUid());
        registration.addRecipes(world.fitted(ViewerRecipes.world(DataRegistries.of(client.level))), world.getUid());
        ViewerRecipes.info().forEach((item, text) ->
                registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM, text));

        // Packing goes into JEI's own crafting category rather than one of ours, which is what makes
        // JEI's built-in "move ingredients into the grid" work on it without a transfer handler of
        // our own. See ViewerRecipes#packing.
        registration.addRecipes(packingAsCrafting(DataRegistries.of(client.level)), VanillaRecipeCategoryUid.CRAFTING);
    }

    /**
     * Each packing permutation as a plain shapeless recipe.
     *
     * <p>These are display-only objects and are deliberately <b>not</b> registered with the game:
     * the real {@code hempdustry:packing} special recipe is what actually crafts, and it already
     * matches exactly these ingredients. Building a {@link ShapelessRecipe} here is a way of
     * describing that match in the one shape JEI's crafting category and its transfer handler both
     * understand.
     */
    private static List<CraftingRecipe> packingAsCrafting(HolderLookup.Provider registries) {
        List<CraftingRecipe> out = new ArrayList<>();
        for (ViewerRecipes.Packing packing : ViewerRecipes.packing(registries)) {
            // Each slot is an Ingredient over the slot's own stacks. On 1.16.5 an Ingredient keeps
            // the stacks it was built from and draws them as they are, components and all, while
            // matching on the item alone -- so the moon rock slot shows the loaded rock and the bong
            // slot cycles every glass, and the recipe still takes whatever rock or glass is put in.
            NonNullList<Ingredient> ingredients = NonNullList.withSize(packing.inputs().size(), Ingredient.EMPTY);
            for (int i = 0; i < packing.inputs().size(); i++) {
                ingredients.set(i, Ingredient.of(packing.inputs().get(i).stream()));
            }
            out.add(new ShapelessRecipe(packing.id(), "", packing.output(), ingredients));
        }
        return out;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // The block you stand in front of. JEI shows these beside the category and lets a player
        // click one to get to its recipes from the item itself.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DECARBOXYLATOR), decarboxylating.getUid());
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INFUSER), infusing.getUid());
        registration.addRecipeCatalyst(new ItemStack(Blocks.CAULDRON), cauldron.getUid());
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.HEMP_PRESS), pressing.getUid());
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SIFTING_BOX), sifting.getUid());
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SIFTING_BOX), iceOLator.getUid());
    }
}

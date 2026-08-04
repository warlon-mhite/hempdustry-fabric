package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModRecipes {

    public static final RecipeSerializer<PackingRecipe> PACKING = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Hempdustry.MOD_ID, "packing"),
            new SpecialRecipeSerializer<>(PackingRecipe::new));

    /**
     * Shapeless, but the ingredients' containers travel into the result rather than being handed
     * back. Bhang needs it: a milk bucket's remainder plus a bucket returned on drinking is one
     * bucket in and two out. See {@link ContainerCarriedRecipe}.
     */
    public static final RecipeSerializer<ContainerCarriedRecipe> CONTAINER_CARRIED = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Hempdustry.MOD_ID, "container_carried"),
            new ContainerCarriedRecipe.Serializer());

    public static void registerRecipes() {
        Hempdustry.LOGGER.info("Registering Recipe Serializers for " + Hempdustry.MOD_ID);
    }
}

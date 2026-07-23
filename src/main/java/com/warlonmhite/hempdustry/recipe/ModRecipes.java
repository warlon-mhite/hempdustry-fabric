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

    public static void registerRecipes() {
        Hempdustry.LOGGER.info("Registering Recipe Serializers for " + Hempdustry.MOD_ID);
    }
}

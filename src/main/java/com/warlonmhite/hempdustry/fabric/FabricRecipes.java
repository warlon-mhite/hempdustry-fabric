package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.platform.Recipes;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/** Fabric's synchronized view of the recipes, which the server and the client both have. */
public final class FabricRecipes implements Recipes {
    @Override
    public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> allOfType(Level level, RecipeType<T> type) {
        return level.recipeAccess().getSynchronizedRecipes().getAllOfType(type);
    }

    @Override
    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> firstMatch(Level level, RecipeType<T> type, I input) {
        return level.recipeAccess().getSynchronizedRecipes().getFirstMatch(type, input, level);
    }
}

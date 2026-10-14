package com.warlonmhite.hempdustry.neoforge;

import com.warlonmhite.hempdustry.platform.Recipes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * A server reads its own recipe manager; a client reads what the server last sent it, which NeoForge
 * hands over in an event ({@code RecipesReceivedEvent}, kept by the client's entry point).
 */
public final class NeoForgeRecipes implements Recipes {
    /** The recipes the server sent this client: only the types asked for, the mod's machines among them. */
    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;

    /** Keeps what the server has just sent; called by the client on NeoForge's {@code RecipesReceivedEvent}. */
    public static void received(RecipeMap recipes) {
        clientRecipes = recipes;
    }

    @Override
    public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> allOfType(Level level, RecipeType<T> type) {
        return recipes(level).byType(type);
    }

    @Override
    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> firstMatch(Level level, RecipeType<T> type, I input) {
        return recipes(level).getRecipesFor(type, input, level).findFirst();
    }

    private static RecipeMap recipes(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess().recipeMap() : clientRecipes;
    }
}

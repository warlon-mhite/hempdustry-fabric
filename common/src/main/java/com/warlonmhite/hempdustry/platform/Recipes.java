package com.warlonmhite.hempdustry.platform;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * The recipes of the mod's own types, the same on a server and on a client.
 *
 * <p>Since 1.21.2 a client is not handed the recipe list wholesale, so the machine types opt in to
 * being synced ({@link Registration#syncRecipeSerializer}) and are read back here. Vanilla's own
 * lookup works on a server and quietly finds nothing on a client, which is exactly the split a
 * machine's screen and a recipe viewer land on.
 */
public interface Recipes {

    /** Every recipe of {@code type} the world knows about. */
    <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> allOfType(Level level, RecipeType<T> type);

    /** The first recipe of {@code type} that takes {@code input}. */
    <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> firstMatch(Level level, RecipeType<T> type, I input);
}

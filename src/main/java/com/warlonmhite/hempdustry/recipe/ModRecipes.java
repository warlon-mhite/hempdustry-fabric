package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collection;

public class ModRecipes {

    public static final RecipeSerializer<PackingRecipe> PACKING = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Hempdustry.MOD_ID, "packing"),
            new SpecialCraftingRecipe.SpecialRecipeSerializer<>(PackingRecipe::new));

    /**
     * Shapeless, but the ingredients' containers travel into the result rather than being handed
     * back. Bhang needs it: a milk bucket's remainder plus a bucket returned on drinking is one
     * bucket in and two out. See {@link ContainerCarriedRecipe}.
     */
    public static final RecipeSerializer<ContainerCarriedRecipe> CONTAINER_CARRIED = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Hempdustry.MOD_ID, "container_carried"),
            new ContainerCarriedRecipe.Serializer());

    /**
     * Shaped and shapeless recipes that carry cannabutter's potency and quality onto the edible they
     * bake. Real recipe types rather than special ones, so they stay visible to the recipe book and
     * to JEI/EMI — see {@link Infusion}.
     */
    public static final RecipeSerializer<InfusedShapedRecipe> INFUSED_SHAPED = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Hempdustry.MOD_ID, "infused_shaped"),
            new InfusedShapedRecipe.Serializer());

    public static final RecipeSerializer<InfusedShapelessRecipe> INFUSED_SHAPELESS = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Hempdustry.MOD_ID, "infused_shapeless"),
            new InfusedShapelessRecipe.Serializer());

    /**
     * The two machines' conversions, as real recipe types.
     *
     * <p>Both were item-identity comparisons in Java until this landed, which is where the mod's own
     * extensibility stopped: a third-party strain could reach the oven through the strain registry
     * and the tags and then had nowhere to go, no pack could rebalance the chain with KubeJS or
     * CraftTweaker, and no recipe viewer could show either machine. See {@link DecarboxylatingRecipe}
     * and {@link InfusingRecipe}.
     *
     * <p>A {@code RecipeType} is two registrations, not one — the type itself, which is what
     * {@code RecipeManager} files a loaded recipe under, and the serializer that reads its JSON.
     */
    public static final RecipeType<DecarboxylatingRecipe> DECARBOXYLATING_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            Identifier.of(Hempdustry.MOD_ID, "decarboxylating"),
            recipeType("decarboxylating"));

    public static final RecipeSerializer<DecarboxylatingRecipe> DECARBOXYLATING = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Hempdustry.MOD_ID, "decarboxylating"),
            new DecarboxylatingRecipe.Serializer());

    public static final RecipeType<InfusingRecipe> INFUSING_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            Identifier.of(Hempdustry.MOD_ID, "infusing"),
            recipeType("infusing"));

    public static final RecipeSerializer<InfusingRecipe> INFUSING = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Identifier.of(Hempdustry.MOD_ID, "infusing"),
            new InfusingRecipe.Serializer());

    /**
     * A bare {@code RecipeType}, ready to be registered.
     *
     * <p><b>{@code RecipeType.register} cannot be used here</b>: it builds its own id with
     * {@code Identifier.ofVanilla}, so a namespaced string arrives as the <em>path</em> and the game
     * refuses it — {@code Non [a-z0-9/._-] character in path of location:
     * minecraft:hempdustry:decarboxylating}, thrown at class-load before anything else runs. All the
     * method adds over this is the {@code toString}, which is worth keeping for a crash report.
     */
    private static <T extends net.minecraft.recipe.Recipe<?>> RecipeType<T> recipeType(String path) {
        String name = Hempdustry.MOD_ID + ":" + path;
        return new RecipeType<>() {
            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Every recipe of {@code type} this world knows about.
     *
     * <p>Since 1.21.4 the client is not handed the recipe list wholesale, so the two machine types
     * have to opt in to being synced (below) and everything reads them back through Fabric's
     * {@code SynchronizedRecipes} view — which the dedicated server, the integrated server and the
     * client all have. {@code ServerRecipeManager#getAllOfType} would work on a server and quietly
     * not exist on a client, which is exactly the split a recipe viewer lands on.
     */
    public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeEntry<T>> allOfType(
            World world, RecipeType<T> type) {
        return world.getRecipeManager().getSynchronizedRecipes().getAllOfType(type);
    }

    public static void registerRecipes() {
        Hempdustry.LOGGER.info("Registering Recipe Serializers for " + Hempdustry.MOD_ID);
        // Without this a recipe viewer on a client sees neither machine: only serializers that ask
        // are sent over the wire.
        RecipeSynchronization.synchronizeRecipeSerializer(DECARBOXYLATING);
        RecipeSynchronization.synchronizeRecipeSerializer(INFUSING);
    }
}

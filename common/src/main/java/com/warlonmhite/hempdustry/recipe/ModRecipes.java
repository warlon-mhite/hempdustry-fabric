package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import java.util.Collection;

public class ModRecipes {

    public static final RecipeSerializer<PackingRecipe> PACKING = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "packing"),
            new RecipeSerializer<>(PackingRecipe.MAP_CODEC, PackingRecipe.STREAM_CODEC));

    /**
     * Shapeless, but the ingredients' containers travel into the result rather than being handed
     * back. Bhang needs it: a milk bucket's remainder plus a bucket returned on drinking is one
     * bucket in and two out. See {@link ContainerCarriedRecipe}.
     */
    public static final RecipeSerializer<ContainerCarriedRecipe> CONTAINER_CARRIED = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "container_carried"),
            new RecipeSerializer<>(ContainerCarriedRecipe.MAP_CODEC, ContainerCarriedRecipe.STREAM_CODEC));

    /**
     * Shaped and shapeless recipes that carry cannabutter's potency and quality onto the edible they
     * bake. Real recipe types rather than special ones, so they stay visible to the recipe book and
     * to JEI/EMI — see {@link Infusion}.
     */
    public static final RecipeSerializer<InfusedShapedRecipe> INFUSED_SHAPED = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "infused_shaped"),
            new RecipeSerializer<>(InfusedShapedRecipe.MAP_CODEC, InfusedShapedRecipe.STREAM_CODEC));

    public static final RecipeSerializer<InfusedShapelessRecipe> INFUSED_SHAPELESS = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "infused_shapeless"),
            new RecipeSerializer<>(InfusedShapelessRecipe.MAP_CODEC, InfusedShapelessRecipe.STREAM_CODEC));

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
            BuiltInRegistries.RECIPE_TYPE,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "decarboxylating"),
            recipeType("decarboxylating"));

    public static final RecipeSerializer<DecarboxylatingRecipe> DECARBOXYLATING = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "decarboxylating"),
            new RecipeSerializer<>(DecarboxylatingRecipe.MAP_CODEC, DecarboxylatingRecipe.STREAM_CODEC));

    public static final RecipeType<InfusingRecipe> INFUSING_TYPE = Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "infusing"),
            recipeType("infusing"));

    public static final RecipeSerializer<InfusingRecipe> INFUSING = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "infusing"),
            new RecipeSerializer<>(InfusingRecipe.MAP_CODEC, InfusingRecipe.STREAM_CODEC));

    /** What the Hemp Press turns into what — the third machine recipe type. See {@link PressingRecipe}. */
    public static final RecipeType<PressingRecipe> PRESSING_TYPE = Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "pressing"),
            recipeType("pressing"));

    public static final RecipeSerializer<PressingRecipe> PRESSING = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "pressing"),
            new RecipeSerializer<>(PressingRecipe.MAP_CODEC, PressingRecipe.STREAM_CODEC));

    /**
     * A bare {@code RecipeType}, ready to be registered.
     *
     * <p><b>{@code RecipeType.register} cannot be used here</b>: it builds its own id with
     * {@code Identifier.withDefaultNamespace}, so a namespaced string arrives as the <em>path</em> and the game
     * refuses it — {@code Non [a-z0-9/._-] character in path of location:
     * minecraft:hempdustry:decarboxylating}, thrown at class-load before anything else runs. All the
     * method adds over this is the {@code toString}, which is worth keeping for a crash report.
     */
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeType<T> recipeType(String path) {
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
     * <p>Since 1.21.4 the client is not handed the recipe list wholesale, so the machine types have
     * to opt in to being synced (below) and everything reads them back through the loader's synced
     * view ({@link com.warlonmhite.hempdustry.platform.Recipes}) — which the dedicated server, the
     * integrated server and the client all have. {@code RecipeMap#byType} would work on a server and
     * quietly not exist on a client, which is exactly the split a recipe viewer lands on.
     */
    public static <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> allOfType(
            Level world, RecipeType<T> type) {
        return Services.RECIPES.allOfType(world, type);
    }

    public static void registerRecipes() {
        Hempdustry.LOGGER.info("Registering Recipe Serializers for " + Hempdustry.MOD_ID);
        // Without this a recipe viewer on a client sees neither machine: only serializers that ask
        // are sent over the wire.
        Services.REGISTRATION.syncRecipes(DECARBOXYLATING_TYPE, DECARBOXYLATING);
        Services.REGISTRATION.syncRecipes(INFUSING_TYPE, INFUSING);
        Services.REGISTRATION.syncRecipes(PRESSING_TYPE, PRESSING);
        PackingRecipe.trackServerRegistries();
    }
}

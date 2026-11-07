package com.warlonmhite.hempdustry.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;

/**
 * What every recipe serializer of the mod extends, so it reads and writes its own recipe.
 *
 * <p><b>Why a vanilla class, and this one.</b> On Forge 1.16.5 a recipe serializer is a Forge registry
 * entry: Forge's {@code RecipeSerializer} demands a registry name, which vanilla's own serializers get by
 * extending Forge's {@code ForgeRegistryEntry} (Forge patches that in). The mod's common code compiles
 * against vanilla and cannot name Forge, but {@link SimpleRecipeSerializer} is one of those patched
 * serializers, so extending it gives the mod's the same superclass on Forge and changes nothing on
 * vanilla. Its own recipe factory is never used: each serializer here reads its JSON and packets itself.
 */
public abstract class ModRecipeSerializer<T extends Recipe<?>> extends SimpleRecipeSerializer<T> {

    protected ModRecipeSerializer() {
        super(id -> {
            throw new UnsupportedOperationException("Read by the serializer itself: " + id);
        });
    }

    @Override
    public abstract T fromJson(ResourceLocation id, JsonObject json);

    @Override
    public abstract T fromNetwork(ResourceLocation id, FriendlyByteBuf buf);

    @Override
    public abstract void toNetwork(FriendlyByteBuf buf, T recipe);
}

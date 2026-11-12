package com.warlonmhite.hempdustry.recipe;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Stash Jar one size up, with everything inside it: a jar in the middle of the grid and a ring of
 * eight around it, eight gold ingots for a Gilded jar, eight diamonds around a Gilded jar for a Diamond
 * one. A chain, so a plain jar with diamonds makes nothing.
 *
 * <p>Special, because a shaped recipe's result is a fixed stack and the jar must keep its contents: the
 * result takes the jar's whole component patch, the way vanilla's bundle dyeing does
 * ({@code TransmuteRecipe}), so a name from the anvil comes along too. The recipe book cannot show a
 * special recipe; JEI and REI each list the two steps in their crafting pages ({@code ViewerRecipes}).
 */
public class StashJarUpgradeRecipe extends CustomRecipe {
    public static final StashJarUpgradeRecipe INSTANCE = new StashJarUpgradeRecipe();
    public static final MapCodec<StashJarUpgradeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, StashJarUpgradeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static final TagKey<Item> GOLD_INGOTS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/gold"));
    public static final TagKey<Item> DIAMONDS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "gems/diamond"));

    /** One step up: {@code from} in the middle, eight of {@code ring} around it, {@code to} out. */
    public record Step(Item from, TagKey<Item> ring, Item to) {
    }

    /** The two steps, smallest jar first. */
    public static List<Step> steps() {
        return List.of(
                new Step(ModBlocks.STASH_JAR.asItem(), GOLD_INGOTS, ModBlocks.GILDED_STASH_JAR.asItem()),
                new Step(ModBlocks.GILDED_STASH_JAR.asItem(), DIAMONDS, ModBlocks.DIAMOND_STASH_JAR.asItem()));
    }

    private static final int MIDDLE = 4;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Step step = find(input);
        if (step == null) {
            return ItemStack.EMPTY;
        }
        ItemStack jar = input.getItem(MIDDLE);
        return new ItemStack(step.to().builtInRegistryHolder(), 1, jar.getComponentsPatch());
    }

    private static @Nullable Step find(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3 || input.ingredientCount() != 9) {
            return null;
        }
        ItemStack jar = input.getItem(MIDDLE);
        for (Step step : steps()) {
            if (!jar.is(step.from())) {
                continue;
            }
            for (int i = 0; i < 9; i++) {
                if (i != MIDDLE && !input.getItem(i).is(step.ring())) {
                    return null;
                }
            }
            return step;
        }
        return null;
    }

    @Override
    public RecipeSerializer<? extends StashJarUpgradeRecipe> getSerializer() {
        return ModRecipes.STASH_JAR_UPGRADE;
    }
}

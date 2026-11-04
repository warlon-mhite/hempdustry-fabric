package com.warlonmhite.hempdustry.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItems;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Hemp seed as bird feed: a parrot tames on it and a chicken follows it and breeds on it, as on every
 * seed of vanilla's. (Hemp seed is the classic caged-bird and coarse-fishing feed, chènevis.) The
 * newer lines join {@code #minecraft:parrot_food} and {@code #minecraft:chicken_food}; on 1.20.1 each
 * animal keeps its food in a field of its own, opened by the access widener and transformer, and the
 * seeds join both here. The goat's browse, a method rather than a field, is {@code GoatFoodMixin}.
 *
 * <p>Run once, after the items are registered and before any world loads: a chicken's tempt goal
 * takes the food it is born with. Forge's convention for it is its common setup, on the main thread.
 */
public final class ModAnimalFood {
    private ModAnimalFood() {
    }

    public static void register() {
        Hempdustry.LOGGER.info("Feeding hemp seeds to the birds for " + Hempdustry.MOD_ID);
        Parrot.TAME_FOOD.addAll(ModItems.hempSeeds());
        Chicken.FOOD_ITEMS = Ingredient.of(Stream.concat(Arrays.stream(Chicken.FOOD_ITEMS.getItems()),
                        ModItems.hempSeeds().stream().map(ItemStack::new))
                .filter(stack -> !stack.isEmpty())
                .distinct());
    }
}

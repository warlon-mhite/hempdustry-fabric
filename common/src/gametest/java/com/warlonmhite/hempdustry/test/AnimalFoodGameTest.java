package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.goat.GoatAi;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Hemp seed is bird feed and the hemp leaf goat browse, as on the newer lines' food tags: a parrot
 * tames on every hemp seed, a chicken follows and breeds on them, a goat is tempted by the leaf and
 * breeds on it. On 1.18.2 each animal keeps its food in code of its own, so each is asked here, of a
 * real animal where there is one to ask. Nothing else eats the leaf: a cow is the control.
 */
public final class AnimalFoodGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void birdsEatHempSeedsAndGoatsTheLeaf(GameTestHelper context) {
        Chicken chicken = context.spawn(EntityType.CHICKEN, new BlockPos(1, 1, 1));
        for (Item seeds : ModItems.hempSeeds()) {
            Asserts.assertTrue(context, Parrot.TAME_FOOD.contains(seeds), "a parrot does not tame on " + seeds);
            Asserts.assertTrue(context, chicken.isFood(new ItemStack(seeds)), "a chicken does not eat " + seeds);
        }
        Asserts.assertTrue(context, chicken.isFood(new ItemStack(Items.WHEAT_SEEDS)), "a chicken no longer eats wheat seeds");

        Goat goat = context.spawn(EntityType.GOAT, new BlockPos(4, 1, 1));
        ItemStack leaf = new ItemStack(ModItems.HEMP_LEAF);
        Asserts.assertTrue(context, goat.isFood(leaf), "a goat does not breed on the hemp leaf");
        Asserts.assertTrue(context, GoatAi.getTemptations().test(leaf), "the hemp leaf does not tempt a goat");
        Asserts.assertTrue(context, goat.isFood(new ItemStack(Items.WHEAT)) && GoatAi.getTemptations().test(new ItemStack(Items.WHEAT)),
                "a goat no longer eats wheat");

        Cow cow = context.spawn(EntityType.COW, new BlockPos(1, 1, 4));
        Asserts.assertFalse(context, cow.isFood(leaf), "a cow eats the hemp leaf, which only a goat browses");
        context.succeed();
    }
}

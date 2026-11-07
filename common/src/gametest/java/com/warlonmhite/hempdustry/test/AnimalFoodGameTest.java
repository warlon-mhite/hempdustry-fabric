package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Hemp seed is bird feed and the hemp leaf goat browse, as on the newer lines' food tags: a parrot
 * tames on every hemp seed, a chicken follows and breeds on them, a goat is tempted by the leaf and
 * breeds on it. On 1.16.5 each animal keeps its food in code of its own, so each is asked here, of a
 * real animal where there is one to ask -- and there are no goats before 1.17. Nothing else eats the
 * leaf: a cow is the control.
 */
public final class AnimalFoodGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void birdsEatHempSeedsAndGoatsTheLeaf(GameTestHelper context) {
        Chicken chicken = context.spawn(EntityType.CHICKEN, new BlockPos(1, 1, 1));
        for (Item seeds : ModItems.hempSeeds()) {
            context.assertTrue(Parrot.TAME_FOOD.contains(seeds), "a parrot does not tame on " + seeds);
            context.assertTrue(chicken.isFood(new ItemStack(seeds)), "a chicken does not eat " + seeds);
        }
        context.assertTrue(chicken.isFood(new ItemStack(Items.WHEAT_SEEDS)), "a chicken no longer eats wheat seeds");

        // 1.16.5 has no goats (1.17), so the leaf's half -- a goat tempted by it and breeding on it -- is
        // not asked here; that nothing else eats it still is.
        ItemStack leaf = new ItemStack(ModItems.HEMP_LEAF);

        Cow cow = context.spawn(EntityType.COW, new BlockPos(1, 1, 4));
        context.assertFalse(cow.isFood(leaf), "a cow eats the hemp leaf, which only a goat browses on the newer lines");
        context.succeed();
    }
}

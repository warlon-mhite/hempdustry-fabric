package com.warlonmhite.hempdustry.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;

/**
 * Bhang — decarboxylated hemp worked into hot milk with sugar. The only <em>drink</em> in the
 * edibles set, and the only one that skips cannabutter entirely.
 *
 * <p>Kept and drunk from the bucket, exactly as vanilla milk is: {@code maxCount(1)}, and finishing
 * it hands the bucket back — via {@code FoodComponent.Builder.usingConvertsTo}, which is how 1.21.1's
 * own stews return their bowls, rather than a hand-written {@code finishUsing}. That is a real drawback next to the stackable edibles — one serving per
 * inventory slot — and it is the right one for the crudest preparation in the set.
 *
 * <h2>Why it takes decarboxylated hemp rather than cannabutter</h2>
 *
 * Traditional bhang is the ground plant worked straight into hot milk, with the milk's own fat doing
 * the extraction (often boosted with ghee). Butter as a concentrated carrier is the Western
 * preparation — cannabutter into milk would be making a Western edible and calling it bhang.
 *
 * <p>It also gives {@code decarboxylated_hemp} a second destination. Until now it existed only to
 * feed the Infuser, and the rejected green-dye proposal was turned down on the grounds that the
 * answer to "more uses for decarboxylated hemp" was downstream content rather than an economically
 * dominated sink. This is that content.
 *
 * <h2>Why it is the weak one, and why that is the point</h2>
 *
 * Milk is about 3.5% fat; butter is about 80%. Milk extraction is genuinely far less efficient, so
 * bhang being cruder than anything butter-based is honest rather than a balance patch. What it buys
 * is <b>immediacy</b>: this is a crafting-grid recipe, where a cannabutter batch is 5–15 real
 * minutes in an Infuser. Patience against immediacy is already the mod's central axis through the
 * Infuser's Quality grading; this extends it instead of inventing a new one.
 *
 * <p>Note it does <em>not</em> undercut the Infuser on cost — the Infuser is 36 hemp stem and the
 * Decarboxylator, which gates this too, is 504. The trade is purely time and potency.
 *
 * <p>Deliberately <b>unwashed</b> hemp: washing exists for the Infuser's Quality axis, and a recipe
 * with no quality axis would be wasting the prep. Traditional bhang is famously grassy anyway —
 * tasting of the plant is the point. Each intermediate now has a distinct destination.
 *
 * <h2>The milk irony, for the record</h2>
 *
 * Vanilla's {@code MilkBucketItem} calls {@code LivingEntity.clearStatusEffects()} with no
 * arguments, so milk wipes buffs and debuffs alike. Making the mod's drinkable edible out of milk
 * means the delivery vehicle and the universal antidote are the same substance. There is no
 * conflict — only the {@code milk_bucket} item clears effects, and this is its own item — and the
 * strain bundles already make milk an all-or-nothing escape rather than a way to keep the good half.
 * It is a joke, not a bug.
 */
public class BhangItem extends Item {
    /** Honey bottle's drink time. A drink should not be quicker to down than vanilla's. */
    private static final int MAX_USE_TIME = 40;

    public BhangItem(Settings settings) {
        super(settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_USE_TIME;
    }

}

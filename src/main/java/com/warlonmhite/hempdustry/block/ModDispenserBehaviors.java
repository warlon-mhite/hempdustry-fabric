package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A dispenser pours milk into an Infuser in front of it, and keeps the empty bucket.
 *
 * <p>This is the Infuser's milk automation. Milk used to go in through a slot a hopper could feed;
 * it is poured by hand now, and the vanilla way to automate "use this item on that block" is a
 * dispenser — it charges a respawn anchor with glowstone, fills bottles at a beehive, and scoops
 * fluid into buckets it then keeps. A comparator behind the tub reads 0 once a batch is collected,
 * so a dispenser can refill it on its own.
 *
 * <p><b>Refusing keeps the bucket.</b> A full tub, or milk with no recipe to go into, fails with
 * the dispenser's failure click and the milk stays put — spitting it out in front of a busy tub
 * would be the worse outcome.
 *
 * <p><b>Registered per item, so only vanilla's milk and hemp milk get it.</b> The tub accepts all of
 * {@code #hempdustry:milk_buckets} by hand, but dispenser behaviours are keyed by item and a tag's
 * contents are not known at startup. Anything not facing an Infuser falls through to whatever the
 * item did before, so another mod's milk behaviour is wrapped rather than replaced.
 */
public final class ModDispenserBehaviors {
    private static final FallibleItemDispenserBehavior POUR_INTO_INFUSER = new FallibleItemDispenserBehavior() {
        @Override
        protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
            InfuserBlockEntity infuser = infuserInFront(pointer);
            ItemStack empty = InfuserBlockEntity.emptiedContainer(stack);
            this.setSuccess(infuser != null && infuser.fill(stack, null));
            return this.isSuccess() ? this.decrementStackWithRemainder(pointer, stack, empty) : stack;
        }
    };

    private ModDispenserBehaviors() {
    }

    public static void registerDispenserBehaviors() {
        pourIntoInfusers(Items.MILK_BUCKET);
        pourIntoInfusers(ModItems.HEMP_MILK_BUCKET);
    }

    private static void pourIntoInfusers(Item milk) {
        // Nobody registers anything for milk in vanilla, so this is normally the plain "drop it"
        // behaviour. Spelled out rather than trusting the map's own default: 1.21.1's map has one,
        // 1.21.11's does not and answers null.
        DispenserBehavior previous = DispenserBlock.BEHAVIORS.getOrDefault(milk, new ItemDispenserBehavior());
        DispenserBlock.registerBehavior(milk, (pointer, stack) -> infuserInFront(pointer) != null
                ? POUR_INTO_INFUSER.dispense(pointer, stack)
                : previous.dispense(pointer, stack));
    }

    @Nullable
    private static InfuserBlockEntity infuserInFront(BlockPointer pointer) {
        BlockPos front = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
        return pointer.world().getBlockEntity(front) instanceof InfuserBlockEntity infuser ? infuser : null;
    }
}

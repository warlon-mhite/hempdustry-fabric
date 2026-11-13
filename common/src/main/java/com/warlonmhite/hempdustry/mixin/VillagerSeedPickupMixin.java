package com.warlonmhite.hempdustry.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A farmer picks up any seed in {@code #minecraft:villager_plantable_seeds}, hemp seed included.
 *
 * <p>Vanilla 1.20.1's {@code Villager#wantsToPickUp} asks a fixed list: its own food ({@code WANTED_ITEMS}) and
 * the profession's {@code requestedItems}, which for a farmer is wheat, wheat seeds, beetroot seeds and bone meal.
 * The farmer plants whatever seed of the tag it holds, but it never picks a tagged seed up, so in survival it
 * never holds one and never replants hemp it reaped (MC-274244). Forge 47 leaves the method as it is. Later
 * NeoForge patches it to take the tag for a villager whose profession works farmland, and Minecraft 1.21.2 reads
 * a tag of its own; this mixin gives both loaders here that answer, so a farmer grows hemp as it does on every
 * newer Minecraft.
 *
 * <p>It only ever turns a "no" into a "yes": for a profession whose secondary point of interest is farmland
 * (the farmer, or a mod's profession built like it), a seed of the tag, and room in the inventory, which is
 * the check vanilla ends with. Every other answer passes through, and {@code @ModifyReturnValue} composes with
 * other mods that amend the same return.
 */
@Mixin(Villager.class)
public abstract class VillagerSeedPickupMixin {

    @ModifyReturnValue(method = "wantsToPickUp", at = @At("RETURN"))
    private boolean hempdustry$farmerTakesPlantableSeeds(boolean wants, ItemStack stack) {
        if (wants || !stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
            return wants;
        }
        Villager villager = (Villager) (Object) this;
        return villager.getVillagerData().getProfession().secondaryPoi().contains(Blocks.FARMLAND)
                && villager.getInventory().canAddItem(stack);
    }
}

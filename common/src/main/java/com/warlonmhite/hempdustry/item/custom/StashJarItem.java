package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.block.custom.stash.StashJarBlock;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.component.StashContents;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.ResinStep;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;

/**
 * A Stash Jar in the inventory: a bundle for buds that blends each strain (see {@link StashContents}).
 * Click a bud onto it to put it in, right-click it onto an empty slot to take the best stack of the last
 * strain out, scroll over it to choose which strain; the tooltip lists what each strain would give back
 * and the bar shows how full it is, against this jar's own capacity. Placed, it is a
 * {@link StashJarBlock}.
 *
 * <p>Vanilla's {@code BundleItem} reads its own component, so its behaviour is written out here
 * against {@code hempdustry:stash_contents}, click for click. One difference by decision: holding
 * right-click in the air does nothing, where a bundle throws its contents out; a jar's right-click
 * is a place.
 */
public class StashJarItem extends BlockItem {
    // BundleItem's own two colours, so a jar's bar reads as a bundle's.
    private static final int BAR_COLOR = ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F);
    private static final int FULL_BAR_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.33F, 0.33F);

    public StashJarItem(Block block, Properties settings) {
        super(block, settings);
    }

    /** Items this jar holds. */
    public int capacity() {
        return getBlock() instanceof StashJarBlock jar ? jar.capacity() : 0;
    }

    public static StashContents contentsOf(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STASH_CONTENTS, StashContents.EMPTY);
    }

    /** Sets a jar's contents; an empty jar carries no component, so it is the same as a new one. */
    public static void setContents(ItemStack stack, StashContents contents) {
        if (contents.isEmpty()) {
            stack.remove(ModComponents.STASH_CONTENTS);
        } else {
            stack.set(ModComponents.STASH_CONTENTS, contents);
        }
    }

    /** The jar carried onto a slot: a primary click takes that slot's items in, a secondary one drops the last stack there. */
    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        ItemStack other = slot.getItem();
        StashContents.Mutable contents = contentsOf(self).mutable();
        if (action == ClickAction.PRIMARY && !other.isEmpty()) {
            int room = capacity() - contents.count();
            ItemStack taken = StashContents.storable(other) && room > 0
                    ? slot.safeTake(other.getCount(), room, player) : ItemStack.EMPTY;
            if (!taken.isEmpty() && contents.insert(taken, capacity()) > 0) {
                playInsertSound(player);
            } else {
                playInsertFailSound(player);
            }
            if (!taken.isEmpty()) {
                slot.safeInsert(taken); // whatever did not fit goes back
            }
            setContents(self, contents.toImmutable());
            broadcastChangesOnContainerMenu(player);
            return true;
        }
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            ItemStack out = contents.removeOne();
            if (!out.isEmpty()) {
                ItemStack rest = slot.safeInsert(out);
                if (!rest.isEmpty()) {
                    // Back where it came from, whatever the tag says now: it was in the jar a moment ago.
                    contents.putBack(rest);
                } else {
                    playRemoveOneSound(player);
                }
            }
            setContents(self, contents.toImmutable());
            broadcastChangesOnContainerMenu(player);
            return true;
        }
        return false;
    }

    /** A stack carried onto the jar: a primary click puts it in, a secondary one with nothing carried takes the last stack out. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess carried) {
        if (action == ClickAction.PRIMARY && other.isEmpty()) {
            toggleSelected(self, StashContents.NONE);
            return false;
        }
        StashContents.Mutable contents = contentsOf(self).mutable();
        if (action == ClickAction.PRIMARY) {
            if (slot.allowModification(player) && contents.insert(other, capacity()) > 0) {
                playInsertSound(player);
            } else {
                playInsertFailSound(player);
            }
            setContents(self, contents.toImmutable());
            broadcastChangesOnContainerMenu(player);
            return true;
        }
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            if (slot.allowModification(player)) {
                ItemStack out = contents.removeOne();
                if (!out.isEmpty()) {
                    playRemoveOneSound(player);
                    carried.set(out);
                }
            }
            setContents(self, contents.toImmutable());
            broadcastChangesOnContainerMenu(player);
            return true;
        }
        toggleSelected(self, StashContents.NONE);
        return false;
    }

    /**
     * A jar never goes inside a shulker box, a bundle or another jar, as a shulker box never goes in
     * one: a stash of stashes would carry thousands of buds in one slot and one packet.
     */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    /** Points the scroll menu at entry {@code index} (or clears it), as {@code BundleItem#toggleSelectedItem}. */
    public static void toggleSelected(ItemStack stack, int index) {
        StashContents contents = stack.get(ModComponents.STASH_CONTENTS);
        if (contents != null) {
            stack.set(ModComponents.STASH_CONTENTS, contents.toggleSelected(index));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return contentsOf(stack).count() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(1 + (int) (contentsOf(stack).fullness(capacity()) * 12), 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return contentsOf(stack).count() >= capacity() ? FULL_BAR_COLOR : BAR_COLOR;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        return display.shows(ModComponents.STASH_CONTENTS)
                ? Optional.of(new StashTooltip(contentsOf(stack), capacity()))
                : Optional.empty();
    }

    /** A jar burnt in lava or blown up as an item lets its contents out, as a bundle and a shulker box do. */
    @Override
    public void onDestroyed(ItemEntity entity) {
        StashContents contents = entity.getItem().get(ModComponents.STASH_CONTENTS);
        if (contents != null) {
            entity.getItem().remove(ModComponents.STASH_CONTENTS);
            ItemUtils.onContainerDestroyed(entity, contents.spill().stream());
        }
    }

    private static void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.level().getRandom().nextFloat() * 0.4F);
    }

    private static void playInsertFailSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    private static void broadcastChangesOnContainerMenu(Player player) {
        if (player.containerMenu != null) {
            player.containerMenu.slotsChanged(player.getInventory());
        }
    }

    /**
     * One entry as the tooltip lists it, what will come out and nothing else: "Purple Kush ×11: 3 Dank,
     * 8 Mids", each stack's band where it is not Mid and its resin where it is not average; an item that
     * carries no tier just its count ("Schwag ×40"). Never a chemotype: the tooltip names what comes out, not
     * what it does.
     */
    public static Component entryLine(StashContents.Entry entry) {
        List<ItemStack> parts = entry.parts();
        ItemStack first = parts.getFirst();
        MutableComponent what = Component.empty();
        if (first.getPrototype().has(ModComponents.BUD_TIER)) {
            for (int i = 0; i < parts.size(); i++) {
                ItemStack part = parts.get(i);
                if (i > 0) {
                    what.append(", ");
                }
                PotencyBand band = PotencyBand.of(part);
                ResinStep resin = ResinStep.of(part);
                Component tier = Component.translatable(BudTier.of(part).getTranslationKey());
                // What sets this stack apart besides its tier, in the order of a bud's own lines: the band where
                // it is not Mid, the resin where it is not average.
                MutableComponent marks = Component.empty();
                if (band != PotencyBand.MID) {
                    marks.append(Component.translatable(band.getTranslationKey()));
                }
                if (resin != ResinStep.AVERAGE) {
                    if (band != PotencyBand.MID) {
                        marks.append(", ");
                    }
                    marks.append(Component.translatable(resin.getTranslationKey()));
                }
                what.append(band == PotencyBand.MID && resin == ResinStep.AVERAGE
                        ? Component.translatable("item.hempdustry.stash_jar.part", part.getCount(), tier)
                        : Component.translatable("item.hempdustry.stash_jar.part_band", part.getCount(), tier, marks));
            }
        } else {
            return Component.translatable("item.hempdustry.stash_jar.entry_plain", first.getHoverName(), entry.count());
        }
        return Component.translatable("item.hempdustry.stash_jar.entry", first.getHoverName(), entry.count(), what);
    }

    /** What the tooltip draws: the contents and the jar's capacity, for the bar. */
    public record StashTooltip(StashContents contents, int capacity) implements TooltipComponent {
    }
}

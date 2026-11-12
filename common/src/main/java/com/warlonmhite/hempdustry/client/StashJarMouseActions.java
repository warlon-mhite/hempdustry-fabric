package com.warlonmhite.hempdustry.client;

import com.warlonmhite.hempdustry.item.custom.StashJarItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

/**
 * The 26.x bundle menu on a Stash Jar: scroll over the jar to choose which of the shown stacks a
 * right-click takes out. Vanilla's {@code BundleMouseActions} answers only {@code #minecraft:bundles},
 * which a jar cannot join (that tag is what bundle dyeing takes), so this is that class against the
 * jar's own component. The choice reaches the server in vanilla's own packet, which the server hands
 * to the jar too ({@code AbstractContainerMenuMixin}).
 */
public final class StashJarMouseActions implements ItemSlotMouseAction {
    private final Minecraft minecraft;
    private final ScrollWheelHandler scrollWheelHandler = new ScrollWheelHandler();

    public StashJarMouseActions(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public boolean matches(Slot slot) {
        return slot.getItem().getItem() instanceof StashJarItem;
    }

    @Override
    public boolean onMouseScrolled(double scrollX, double scrollY, int slotIndex, ItemStack stack) {
        int shown = StashJarItem.contentsOf(stack).shown();
        if (shown == 0) {
            return false;
        }
        Vector2i wheelXY = scrollWheelHandler.onMouseScroll(scrollX, scrollY);
        int wheel = wheelXY.y == 0 ? -wheelXY.x : wheelXY.y;
        if (wheel != 0) {
            int selected = StashJarItem.contentsOf(stack).selected();
            int next = ScrollWheelHandler.getNextScrollWheelSelection(wheel, selected, shown);
            if (selected != next) {
                select(stack, slotIndex, next);
            }
        }
        return true;
    }

    @Override
    public void onStopHovering(Slot hoveredSlot) {
        select(hoveredSlot.getItem(), hoveredSlot.index, -1);
    }

    @Override
    public void onSlotClicked(Slot slot, ContainerInput input) {
        if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.SWAP) {
            select(slot.getItem(), slot.index, -1);
        }
    }

    private void select(ItemStack jar, int slotIndex, int index) {
        if (minecraft.getConnection() != null && index < StashJarItem.contentsOf(jar).shown()) {
            StashJarItem.toggleSelected(jar, index);
            minecraft.getConnection().send(new ServerboundSelectBundleItemPacket(slotIndex, index));
        }
    }
}

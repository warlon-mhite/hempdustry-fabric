package com.warlonmhite.hempdustry.forge;

import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import com.warlonmhite.hempdustry.platform.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.List;

/**
 * The pending edibles in the player's Forge data, saved with the player under
 * {@code ForgeData.hempdustry:pending_edibles}. Forge keeps that data across neither a death nor the
 * End portal's return home, since both make a new player; this copies it across the return home and
 * not across a death -- the rule wanted, and the one the other loader's attachment keeps.
 *
 * <p>Fabric writes its attachments elsewhere, so a world moved between loaders drops the doses still
 * on their way, minutes of effects at most.
 */
public final class ForgePlayerData implements PlayerData {
    private static final String KEY = EdibleScheduler.PENDING_ID.toString();

    /** Copies the pending doses to the new player the End portal makes; called while the game registers. */
    static void register() {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            if (!event.isWasDeath() && event.getOriginal().getPersistentData().contains(KEY)) {
                event.getEntity().getPersistentData().put(KEY, event.getOriginal().getPersistentData().get(KEY).copy());
            }
        });
    }

    @Override
    public List<EdibleScheduler.Pending> pendingEdibles(Player player) {
        Tag saved = player.getPersistentData().get(KEY);
        if (saved == null) {
            return List.of();
        }
        return EdibleScheduler.PENDING_CODEC.parse(NbtOps.INSTANCE, saved).result().orElse(List.of());
    }

    @Override
    public void setPendingEdibles(Player player, List<EdibleScheduler.Pending> pending) {
        CompoundTag data = player.getPersistentData();
        if (pending.isEmpty()) {
            data.remove(KEY);
            return;
        }
        EdibleScheduler.PENDING_CODEC.encodeStart(NbtOps.INSTANCE, pending).result()
                .ifPresent(tag -> data.put(KEY, tag));
    }
}

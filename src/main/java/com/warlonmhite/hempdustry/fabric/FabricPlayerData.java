package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import com.warlonmhite.hempdustry.platform.PlayerData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * The pending edibles as a Fabric attachment, saved with the player. A persistent attachment is
 * copied across the End portal's return home and not across a death, which is the rule wanted.
 */
public final class FabricPlayerData implements PlayerData {
    private static final AttachmentType<List<EdibleScheduler.Pending>> PENDING = AttachmentRegistry.createPersistent(
            EdibleScheduler.PENDING_ID, EdibleScheduler.PENDING_CODEC);

    @Override
    public List<EdibleScheduler.Pending> pendingEdibles(Player player) {
        return player.getAttachedOrElse(PENDING, List.of());
    }

    @Override
    public void setPendingEdibles(Player player, List<EdibleScheduler.Pending> pending) {
        if (pending.isEmpty()) {
            player.removeAttached(PENDING);
        } else {
            player.setAttached(PENDING, pending);
        }
    }
}

package com.warlonmhite.hempdustry.neoforge;

import com.warlonmhite.hempdustry.item.custom.EdibleScheduler;
import com.warlonmhite.hempdustry.platform.PlayerData;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/**
 * The pending edibles as a NeoForge attachment, saved with the player. NeoForge copies a saved
 * attachment across the End portal's return home, and across a death only when the type asks to,
 * which this one does not -- the rule wanted.
 *
 * <p>Saved under {@code neoforge:attachments}, where Fabric writes {@code fabric:attachments}: a world
 * moved between loaders drops the doses still on their way, minutes of effects at most.
 */
public final class NeoForgePlayerData implements PlayerData {
    static final AttachmentType<List<EdibleScheduler.Pending>> PENDING = AttachmentType.<List<EdibleScheduler.Pending>>builder(() -> List.of())
            .serialize(EdibleScheduler.PENDING_CODEC.fieldOf("pending"), pending -> !pending.isEmpty())
            .build();

    /** Registers the attachment type; called while the game registers its content. */
    static void register() {
        Registry.register(NeoForgeRegistries.ATTACHMENT_TYPES, EdibleScheduler.PENDING_ID, PENDING);
    }

    @Override
    public List<EdibleScheduler.Pending> pendingEdibles(Player player) {
        return player.getExistingData(PENDING).orElse(List.of());
    }

    @Override
    public void setPendingEdibles(Player player, List<EdibleScheduler.Pending> pending) {
        if (pending.isEmpty()) {
            player.removeData(PENDING);
        } else {
            player.setData(PENDING, pending);
        }
    }
}

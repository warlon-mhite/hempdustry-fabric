package com.warlonmhite.hempdustry.client.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.item.custom.StashJarItem;
import com.warlonmhite.hempdustry.block.custom.stash.StashPieces;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * {@code hempdustry:stash_piece} — one stash piece's colour in a Stash Jar's item, the same as the placed
 * jar's piece of that number: {@code piece} 0 is the largest share's first piece, and {@code part} picks its
 * {@code body} (bud green, or schwag's brown) or its {@code pistils} (its strain's colour).
 *
 * <p>The pieces are grey art, and this multiplies them, as the placed block's tint does. Opaque:
 * a strain's colour is RGB, and a tint's alpha byte is the vertex alpha.
 *
 * @param piece   which piece, 0 to 4
 * @param pistils the pistils' colour rather than the body's
 */
public record StashPieceTintSource(int piece, boolean pistils) implements ItemTintSource {
    public static final MapCodec<StashPieceTintSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.intRange(0, 4).fieldOf("piece").forGetter(StashPieceTintSource::piece),
                    Codec.STRING.xmap("pistils"::equals, on -> on ? "pistils" : "body")
                            .optionalFieldOf("part", false).forGetter(StashPieceTintSource::pistils)
            ).apply(instance, StashPieceTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity) {
        int fallback = pistils ? StashPieces.PLAIN_PISTILS : StashPieces.BUD_GREEN;
        if (!(stack.getItem() instanceof StashJarItem jar)) {
            return ARGB.opaque(fallback);
        }
        StashPieces.Colours colours = StashJarItem.contentsOf(stack).pieceColours(jar.capacity(),
                world == null ? null : world.registryAccess());
        if (piece >= colours.count()) {
            return ARGB.opaque(fallback);
        }
        return ARGB.opaque(pistils ? colours.pistils()[piece] : colours.bodies()[piece]);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return CODEC;
    }
}

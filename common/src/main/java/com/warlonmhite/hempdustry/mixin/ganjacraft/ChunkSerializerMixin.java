package com.warlonmhite.hempdustry.mixin.ganjacraft;

import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A chunk's blocks, converted before the chunk is read, and the block entities the conversion lets go
 * taken out. Since 1.18 a section's block palette is
 * decoded by the block-state codec, which looks each name up and drops the ones it does not know, so
 * the old ids are replaced in the saved palette first -- each entry by what the table makes of it,
 * written as a palette entry again. Two old entries may become the same state; a palette may hold a
 * state twice, and each entry keeps its place, so the blocks pointing at it stay where they are.
 */
@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {

    @Inject(method = "read", at = @At("HEAD"))
    private static void hempdustry$readGanjaCraft(ServerLevel level, PoiManager poi, ChunkPos pos, CompoundTag saved,
                                                  CallbackInfoReturnable<ProtoChunk> cir) {
        ListTag sections = saved.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag states = sections.getCompound(i).getCompound("block_states");
            ListTag palette = states.getList("palette", Tag.TAG_COMPOUND);
            for (int j = 0; j < palette.size(); j++) {
                BlockState converted = GanjaCraftWorlds.block(palette.getCompound(j));
                if (converted != null) {
                    palette.set(j, NbtUtils.writeBlockState(converted));
                }
            }
        }
        // A block entity the table lets go (an MCreator crop stage's growth timer: the crop it becomes has
        // none) is taken out here too: 1.18.2 checks each saved block entity against its block before
        // reading it, and warns of every one whose block now has no block entity.
        ListTag blockEntities = saved.getList("block_entities", Tag.TAG_COMPOUND);
        for (int i = blockEntities.size() - 1; i >= 0; i--) {
            Optional<String> becomes = GanjaCraftWorlds.blockEntity(blockEntities.getCompound(i).getString("id"));
            if (becomes != null && !becomes.isPresent()) {
                blockEntities.remove(i);
            }
        }
    }
}

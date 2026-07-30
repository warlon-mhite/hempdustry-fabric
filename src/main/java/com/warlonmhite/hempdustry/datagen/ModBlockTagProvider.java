package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        // Without this, farmland that dries out (no water within 4 blocks, no rain) reverts to dirt
        // *even though a crop is standing on it* — FarmlandBlock's "is there still a crop here?"
        // check is this tag, not "is this a CropBlock" — and the unsupported plant then pops off.
        // Vanilla lists every one of its crops here; ours have to opt in the same way.
        getOrCreateTagBuilder(BlockTags.MAINTAINS_FARMLAND)
                .add(ModBlocks.INDICA_CROP)
                .add(ModBlocks.SATIVA_CROP);

        // Nothing in 1.21.1's *code* reads #minecraft:crops (the only class referencing it is the
        // vanilla tag provider that builds it) — its one effect is that #minecraft:bee_growables
        // includes it, which is exactly what we're after: a bee carrying pollen that flies over a
        // hemp plant fertilises it, same as it would wheat.
        //
        // Requires the crops' isMature() override to be in place, or bees decapitate tall plants —
        // see IndicaCropBlock#isMature for why.
        getOrCreateTagBuilder(BlockTags.CROPS)
                .add(ModBlocks.INDICA_CROP)
                .add(ModBlocks.SATIVA_CROP);

        // What will heat an Infuser standing on top of it. Anything here that carries a LIT
        // property must also be lit (see InfuserBlockEntity#isHeatedFrom), which is what makes the
        // campfire the practical choice — it is permanently lit and cheap, where a furnace is only
        // lit while it is itself busy smelting, and a magma block is a Nether trip. Listing all
        // three gives the player a genuine early/mid/exotic ladder rather than one right answer.
        //
        // Note this is an explicit list, NOT "anything hot": there is no vanilla or Fabric
        // convention tag for heat sources to inherit from (the nearest, c:player_workstations/
        // furnaces, means "a villager works here", not "this is hot"). So a modded forge or
        // crucible will NOT work unless that mod, or a datapack, adds itself to this tag.
        //
        // #minecraft:campfires rather than the two campfires by name is the one free win available:
        // modded campfires join that vanilla tag on their own, so they work here without either
        // side knowing about the other.
        getOrCreateTagBuilder(ModTags.Blocks.HEAT_SOURCES)
                .forceAddTag(BlockTags.CAMPFIRES)
                .add(Blocks.FURNACE)
                .add(Blocks.SMOKER)
                .add(Blocks.BLAST_FURNACE)
                .add(Blocks.MAGMA_BLOCK)
                // Our own oven counts, which lets the two machines be stacked: the Decarboxylator's
                // fire heats the Infuser sitting on it. Same LIT caveat as a furnace — it only
                // radiates while it is actually cooking something.
                .add(ModBlocks.DECARBOXYLATOR);

        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.HEMPCRETE_BLOCK)
                .add(ModBlocks.DECARBOXYLATOR)
                .add(ModBlocks.INFUSER);

        getOrCreateTagBuilder(BlockTags.SHOVEL_MINEABLE)
                .add(ModBlocks.HEMPCRETE_POWDER_BLOCK);

        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                .add(ModBlocks.HEMP_BRICKS_BLOCK)
                .add(ModBlocks.HEMP_BRICKS_SLAB)
                .add(ModBlocks.HEMP_BRICKS_STAIRS)
                .add(ModBlocks.HEMP_BRICKS_WALL)
                .add(ModBlocks.HEMP_PLANKS)
                .add(ModBlocks.HEMP_PLANKS_STAIRS)
                .add(ModBlocks.HEMP_PLANKS_BUTTON)
                .add(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE)
                .add(ModBlocks.HEMP_PLANKS_SLAB)
                .add(ModBlocks.HEMP_PLANKS_FENCE)
                .add(ModBlocks.HEMP_PLANKS_FENCE_GATE)
                .add(ModBlocks.HEMP_PLANKS_DOOR)
                .add(ModBlocks.HEMP_PLANKS_TRAPDOOR)
                .add(ModBlocks.HEMP_PLANKS_SIGN)
                .add(ModBlocks.HEMP_PLANKS_WALL_SIGN)
                .add(ModBlocks.HEMP_PLANKS_HANGING_SIGN)
                .add(ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN);

        getOrCreateTagBuilder(BlockTags.PLANKS).add(ModBlocks.HEMP_PLANKS);
        getOrCreateTagBuilder(BlockTags.WOODEN_SLABS).add(ModBlocks.HEMP_PLANKS_SLAB);

        getOrCreateTagBuilder(BlockTags.FENCES).add(ModBlocks.HEMP_PLANKS_FENCE);
        getOrCreateTagBuilder(BlockTags.FENCE_GATES).add(ModBlocks.HEMP_PLANKS_FENCE_GATE);
        getOrCreateTagBuilder(BlockTags.WALLS).add(ModBlocks.HEMP_BRICKS_WALL);

        getOrCreateTagBuilder(BlockTags.STANDING_SIGNS).add(ModBlocks.HEMP_PLANKS_SIGN);
        getOrCreateTagBuilder(BlockTags.WALL_SIGNS).add(ModBlocks.HEMP_PLANKS_WALL_SIGN);
        getOrCreateTagBuilder(BlockTags.SIGNS).add(ModBlocks.HEMP_PLANKS_SIGN, ModBlocks.HEMP_PLANKS_WALL_SIGN);
        getOrCreateTagBuilder(BlockTags.CEILING_HANGING_SIGNS).add(ModBlocks.HEMP_PLANKS_HANGING_SIGN);
        getOrCreateTagBuilder(BlockTags.WALL_HANGING_SIGNS).add(ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN);
        getOrCreateTagBuilder(BlockTags.ALL_HANGING_SIGNS)
                .add(ModBlocks.HEMP_PLANKS_HANGING_SIGN, ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN);
        getOrCreateTagBuilder(BlockTags.ALL_SIGNS)
                .add(ModBlocks.HEMP_PLANKS_SIGN, ModBlocks.HEMP_PLANKS_WALL_SIGN,
                        ModBlocks.HEMP_PLANKS_HANGING_SIGN, ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN);
    }
}

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

        // "A hemp plant you can defoliate", strain-agnostic. The "Trim Season" advancement matches
        // shears-used-on-a-block-in-this-tag, which is exactly where Defoliation#tryCut succeeds
        // and nowhere else — see ModAdvancementProvider.
        getOrCreateTagBuilder(ModTags.Blocks.HEMP_CROPS)
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

        // The bale is a copy of hay in every other respect and hay is hoe-mineable. It was in no
        // mineable tag at all, so no tool sped it up.
        //
        // Hoe *only*. Hay is also in #minecraft:horse_food and #minecraft:llama_food; the hemp bale
        // is deliberately not, and that was a decision rather than an oversight — the mod feeds
        // goats hemp leaf and nothing else, and a bale that feeds horses would start it down the
        // "hemp is a worse wheat for every farm animal" road that goat_food was scoped to avoid.
        getOrCreateTagBuilder(BlockTags.HOE_MINEABLE)
                .add(ModBlocks.HEMP_BALE);

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

        // What hemp wool gets *instead of* joining #minecraft:wool. That tag is referenced by
        // exactly three things in 1.21.1 — the painting recipe, these two vibration tags, and a
        // 100-tick fuel entry in AbstractFurnaceBlockEntity#createFuelTimeMap. Beds, banners and
        // carpets are all keyed on the specific coloured wool *item*, not the tag, so joining it
        // would buy nothing except handing the painting recipe back to vanilla's wool version and
        // undercutting the explicit hemp_wool one. Grant the two useful halves directly instead.
        //
        // Well grounded independently: hemp fibre insulation and hemp acoustic panels are a real
        // product category, and hempcrete measures ~0.8 absorption with 50-59 dB sound reduction.
        getOrCreateTagBuilder(BlockTags.DAMPENS_VIBRATIONS).add(ModBlocks.HEMP_WOOL);
        getOrCreateTagBuilder(BlockTags.OCCLUDES_VIBRATION_SIGNALS).add(ModBlocks.HEMP_WOOL);

        // The carpet joins #minecraft:wool_carpets instead, and that tag is safe where
        // #minecraft:wool is not: it gates only vibration damping, the step-sound blend, llama
        // decoration and a 67-tick fuel entry — no recipe. Damping and fuel therefore come for free
        // via the tag, which is why the carpet is absent from the two tags above.
        //
        // Deliberately NOT in OCCLUDES_VIBRATION_SIGNALS: vanilla's carpets dampen but do not
        // occlude (a carpet is too thin to block a signal outright), and we mirror that exactly.
        getOrCreateTagBuilder(BlockTags.WOOL_CARPETS).add(ModBlocks.HEMP_CARPET);

        getOrCreateTagBuilder(BlockTags.PLANKS).add(ModBlocks.HEMP_PLANKS);
        getOrCreateTagBuilder(BlockTags.WOODEN_SLABS).add(ModBlocks.HEMP_PLANKS_SLAB);

        // The *wooden* tags, not the umbrella ones — the umbrellas include them, so joining at this
        // level gets #minecraft:fences, /doors and /trapdoors free, and the wooden level is the one
        // the game actually reads:
        //
        //   FenceBlock#canConnectToFence requires *matching* WOODEN_FENCES membership on both sides.
        //     The fence used to join #minecraft:fences directly, which made it read as a non-wooden
        //     fence: it refused to connect to oak and connected to nether brick fence instead.
        //   LandPathNodeMaker gives #minecraft:trapdoors its own PathNodeType, and FallLocation
        //     reads the same tag for the fall death message.
        //   MoveControl suppresses its jump-at-obstacle reflex for #minecraft:doors, so without it
        //     a mob walking into a hemp door bounces off it instead of pathing.
        //
        // None of this costs the fire immunity. The only *other* thing reading these four in 1.21.1
        // is AbstractFurnaceBlockEntity's fuel map, and its addFuel skips anything in
        // #minecraft:non_flammable_wood — which every one of these already is.
        getOrCreateTagBuilder(BlockTags.WOODEN_FENCES).add(ModBlocks.HEMP_PLANKS_FENCE);
        getOrCreateTagBuilder(BlockTags.WOODEN_DOORS).add(ModBlocks.HEMP_PLANKS_DOOR);
        getOrCreateTagBuilder(BlockTags.WOODEN_TRAPDOORS).add(ModBlocks.HEMP_PLANKS_TRAPDOOR);
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

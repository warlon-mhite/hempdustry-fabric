package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(ModTags.Items.HEMP_SEEDS)
                .add(ModItems.INDICA_SEEDS);

        // Strain-agnostic: any hemp seed variant (future Lemon Haze included) feeds/tames parrots,
        // same as vanilla's wheat/melon/pumpkin/beetroot seeds.
        getOrCreateTagBuilder(ItemTags.PARROT_FOOD)
                .addTag(ModTags.Items.HEMP_SEEDS);

        getOrCreateTagBuilder(ItemTags.TRIMMABLE_ARMOR)
            .add(ModItems.HEMP_BEANNIE)
            .add(ModItems.HEMP_SHIRT)
            .add(ModItems.HEMP_HAREM_PANTS)
            .add(ModItems.FLIP_FLOPS);

        getOrCreateTagBuilder(ItemTags.PLANKS).add(ModBlocks.HEMP_PLANKS.asItem());
        getOrCreateTagBuilder(ItemTags.WOODEN_SLABS).add(ModBlocks.HEMP_PLANKS_SLAB.asItem());

        getOrCreateTagBuilder(ItemTags.SIGNS).add(ModItems.HEMP_PLANKS_SIGN);
        getOrCreateTagBuilder(ItemTags.HANGING_SIGNS).add(ModItems.HEMP_PLANKS_HANGING_SIGN);

        // Hemp lumber is intentionally fire-immune (like Crimson/Warped nether wood): this is
        // the item-side "can't be used as furnace fuel" half; the block-side "fire won't spread
        // to it" half is simply never registering these blocks in FlammableBlockRegistry.
        getOrCreateTagBuilder(ItemTags.NON_FLAMMABLE_WOOD)
                .add(ModBlocks.HEMP_PLANKS.asItem())
                .add(ModBlocks.HEMP_PLANKS_STAIRS.asItem())
                .add(ModBlocks.HEMP_PLANKS_SLAB.asItem())
                .add(ModBlocks.HEMP_PLANKS_FENCE.asItem())
                .add(ModBlocks.HEMP_PLANKS_FENCE_GATE.asItem())
                .add(ModBlocks.HEMP_PLANKS_DOOR.asItem())
                .add(ModBlocks.HEMP_PLANKS_TRAPDOOR.asItem())
                .add(ModBlocks.HEMP_PLANKS_BUTTON.asItem())
                .add(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE.asItem())
                .add(ModItems.HEMP_PLANKS_SIGN)
                .add(ModItems.HEMP_PLANKS_HANGING_SIGN);

        // Empty pipe/bong accept Unbreaking/Mending (and enchant at the table). Packed variants
        // inherit any enchantment through the component copy, so they don't need listing here.
        getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE)
                .add(ModItems.WOODEN_PIPE)
                .add(ModItems.BONG);

        // Puts the Ganja disc on exactly the same footing as vanilla's twelve common discs: the
        // creeper loot table rolls this tag (expand:true, one entry each) when a skeleton lands
        // the kill, so joining the tag *is* the drop — no loot-table surgery needed.
        getOrCreateTagBuilder(ItemTags.CREEPER_DROP_MUSIC_DISCS)
                .add(ModItems.MUSIC_DISC_GANJA);

        // Cross-mod convention tag, so anything that reasons about discs (jukebox blocks, storage
        // filters, JEI-style lookups) picks ours up too.
        getOrCreateTagBuilder(ConventionalItemTags.MUSIC_DISCS)
                .add(ModItems.MUSIC_DISC_GANJA);
        }
    }


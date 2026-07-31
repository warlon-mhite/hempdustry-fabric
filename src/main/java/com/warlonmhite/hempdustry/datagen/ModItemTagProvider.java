package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.item.Items;
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
                .add(ModItems.INDICA_SEEDS)
                .add(ModItems.SATIVA_SEEDS);

        // A tag rather than a hard milk_bucket check in the Infuser, so another mod's milk works
        // and a datapack can widen it without a code change.
        getOrCreateTagBuilder(ModTags.Items.MILK_BUCKETS)
                .add(Items.MILK_BUCKET);

        // Strain-agnostic: any hemp seed variant (future Lemon Haze included) feeds/tames parrots,
        // same as vanilla's wheat/melon/pumpkin/beetroot seeds.
        getOrCreateTagBuilder(ItemTags.PARROT_FOOD)
                .addTag(ModTags.Items.HEMP_SEEDS);

        // Vanilla's chicken food is literally *every seed in the game* — wheat, melon, pumpkin,
        // beetroot, torchflower and pitcher pod — so hemp seeds being absent read as an oversight
        // rather than a decision, especially with parrots already fed above. Hemp seed is also the
        // classic European bird feed: chènevis is what's in a caged-bird mix, and it is standard
        // coarse-fishing bait for the same reason birds and fish both go for it.
        getOrCreateTagBuilder(ItemTags.CHICKEN_FOOD)
                .addTag(ModTags.Items.HEMP_SEEDS);

        // Goats browse. Vanilla's goat food is wheat and nothing else, and a fan leaf is exactly the
        // kind of thing a real goat would strip off a plant — they eat leaves and shrubs rather than
        // grazing grass, which is the one genuine difference between them and vanilla's sheep and
        // cows. Deliberately goats *only*: making hemp leaf feed every farm animal would just be a
        // worse wheat, where one animal that eats it is a fact worth knowing.
        //
        // Joining the tag is the whole feature — GoatEntity#isBreedingItem reads it, and the tempt
        // goal is built from that same check, so breeding, leading a goat around and speeding up a
        // kid all come for free. These per-animal food tags are Mojang's own extension point.
        getOrCreateTagBuilder(ItemTags.GOAT_FOOD)
                .add(ModItems.HEMP_LEAF);

        // Cross-mod cordage. This was agreed as `#c:ropes` and is deliberately `#c:strings` instead:
        // hemp fibre crafts 1:1 into vanilla string, which makes it string-tier, while `#c:ropes` by
        // convention means an actual laid rope — the hangable, climbable kind Supplementaries and
        // Farmer's Delight ship. Fibre is what rope is *made from*, not rope. Putting it in `#c:ropes`
        // would hand every mod that expects a rope something four-to-a-stem cheap.
        //
        // `#c:ropes` is the right tag the day the mod has a real hemp_rope item, and it should have
        // one — rope is the single most archetypal hemp product there is. See CLAUDE.md §5b.
        //
        // Nothing in vanilla reads `#c:strings`; the whole value here is other mods' recipes, which
        // now take fibre directly and skip a 1:1 crafting step. No balance change, just less friction.
        getOrCreateTagBuilder(ConventionalItemTags.STRINGS)
                .add(ModItems.HEMP_FIBER);

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


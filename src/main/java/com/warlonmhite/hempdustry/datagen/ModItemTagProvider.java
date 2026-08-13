package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.item.Item;
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
        //
        // The mod's own hemp seed milk belongs in here on the merits, not as a courtesy: what the
        // Infuser is actually doing is dissolving cannabinoids into fat, and hemp seed milk is a
        // pressed-seed emulsion carrying real hemp seed oil — the same solvent, from the same plant.
        // It also closes the chain: crop -> Decarboxylator -> cauldron -> hemp milk -> cannabutter
        // can now be walked without ever finding a cow, which is the version of this mod a player
        // is probably imagining. Not an economy shortcut either way, since a cow is free and
        // endlessly reusable where hemp milk costs three seeds a bucket.
        //
        // It also folds in the ecosystem's own #c:buckets/milk, which is the half of this that
        // reaches other mods. A tag cannot retrofit somebody else's recipe -- a recipe accepts a tag
        // only if its author asked for one -- so the only lever that works across mods is for both
        // sides to name the same convention tag. Optional rather than a hard include: an absent tag
        // in a required entry fails the whole tag load, and vanilla's milk is listed outright above
        // precisely so this one can never take the Infuser's milk slot down with it.
        getOrCreateTagBuilder(ModTags.Items.MILK_BUCKETS)
                .add(Items.MILK_BUCKET)
                .add(ModItems.HEMP_MILK_BUCKET)
                .addOptionalTag(ConventionalItemTags.MILK_BUCKETS);

        // The outbound half, and the same reasoning as #c:strings below: nothing in vanilla reads
        // #c:buckets/milk, and the entire value is other mods' recipes taking hemp milk for free.
        getOrCreateTagBuilder(ConventionalItemTags.MILK_BUCKETS)
                .add(ModItems.HEMP_MILK_BUCKET);

        // The dosed foods, as one group. Space Cake is absent on purpose: a slice is eaten by using
        // the *block*, which never fires minecraft:consume_item — see ModTags.Items.EDIBLES.
        getOrCreateTagBuilder(ModTags.Items.EDIBLES)
                .add(ModItems.CANNABUTTER_TOAST)
                .add(ModItems.SPACE_COOKIE)
                .add(ModItems.SPACE_BROWNIE)
                .add(ModItems.BHANG_BUCKET)
                .add(ModItems.DAWAMESK);

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
            .add(ModItems.HEMP_BEANIE)
            .add(ModItems.HEMP_SHIRT)
            .add(ModItems.HEMP_HAREM_PANTS)
            .add(ModItems.FLIP_FLOPS);

        // The four base armour tags, and they are the whole enchantment story: every
        // #minecraft:enchantable/* tag is built on top of these four, so a modded armour item that
        // joins none of them can be enchanted with *nothing* — not Protection, not Unbreaking, not
        // even a curse. The set shipped that way, which made `enchantability 20` on the material a
        // dead letter and, because #minecraft:trimmable_armor *was* joined, made it look deliberate.
        // Joining these grants enchantable/armor, /durability, /equippable, /vanishing and the
        // per-slot tags transitively; nothing else needs adding.
        //
        // It stays bad armour (1/2/1/1) — this buys the *right* to enchant, not protection.
        getOrCreateTagBuilder(ItemTags.HEAD_ARMOR).add(ModItems.HEMP_BEANIE);
        getOrCreateTagBuilder(ItemTags.CHEST_ARMOR).add(ModItems.HEMP_SHIRT);
        getOrCreateTagBuilder(ItemTags.LEG_ARMOR).add(ModItems.HEMP_HAREM_PANTS);
        getOrCreateTagBuilder(ItemTags.FOOT_ARMOR).add(ModItems.FLIP_FLOPS);

        // Canvas is leather-tier by the mod's own standing rule — it substitutes wherever vanilla
        // uses leather, and the cloth chain's balance anchors are set at leather parity — so the
        // convention tag is that rule extended to the mods that asked for it. Nothing in vanilla
        // reads #c:leathers; the whole effect is cross-mod.
        getOrCreateTagBuilder(ConventionalItemTags.LEATHERS).add(ModItems.HEMP_CANVAS);

        getOrCreateTagBuilder(ConventionalItemTags.ARMORS)
                .add(ModItems.HEMP_BEANIE)
                .add(ModItems.HEMP_SHIRT)
                .add(ModItems.HEMP_HAREM_PANTS)
                .add(ModItems.FLIP_FLOPS);

        // #c:foods. Each item goes in the most specific subtag that fits and nowhere else, because
        // #c:foods already includes every subtag — listing an item twice would be noise.
        //
        // NOTE the singular names. Fabric ships plural aliases (foods/soups, foods/cookies, …) which
        // carry @Deprecated and the note "this tag was typoed"; #c:foods lists only the singulars.
        // A plural would still *work* — each singular includes its plural as an optional entry, so
        // an item in foods/soups reaches #c:foods one hop later — but it is the deprecated spelling
        // and the compatibility shim is not something to write new code against.
        getOrCreateTagBuilder(ConventionalItemTags.SOUP_FOODS).add(ModItems.SIEMIENIOTKA);
        getOrCreateTagBuilder(ConventionalItemTags.COOKIE_FOODS).add(ModItems.SPACE_COOKIE);
        getOrCreateTagBuilder(ConventionalItemTags.EDIBLE_WHEN_PLACED_FOODS).add(ModBlocks.SPACE_CAKE.asItem());

        // Everything with no subtag that fits. The dosed half is in here on purpose: a Space Brownie
        // is food, and a pack's food-handling machinery should treat it as such — which does mean an
        // auto-feeder could pick one. That is the honest answer and a funny one.
        //
        // hemp_milk_bucket is deliberately absent: it is not food, exactly as a cow's milk bucket is
        // not food and is likewise not in this tag. See food.md.
        getOrCreateTagBuilder(ConventionalItemTags.FOODS)
                .add(ModItems.TOASTED_HEMP_SEEDS)
                .add(ModItems.HEMP_FLAPJACK)
                .add(ModItems.CANNABUTTER_TOAST)
                .add(ModItems.SPACE_BROWNIE)
                .add(ModItems.DAWAMESK)
                .add(ModItems.BHANG_BUCKET);

        // ---------------------------------------------------------------------
        // Convention tags, the outbound half. Nothing in vanilla reads any of these; the entire
        // value is other mods' recipes and machinery finding this mod's materials without either
        // side having heard of the other.
        //
        // c:crops, c:bricks and c:storage_blocks hold nothing but subtags, so each gets a
        // c:<parent>/hemp folded into its parent rather than the item shoved in at the top level --
        // see ModTags.Conventional. The parent references are optional, which costs nothing and
        // means a stripped-down Fabric API that omits one cannot take the whole tag down with it
        // (one unresolvable *required* entry drops every other entry in the file -- CLAUDE.md §5).
        getOrCreateTagBuilder(ModTags.Conventional.HEMP_CROPS)
                .add(ModItems.INDICA_BUDS)
                .add(ModItems.SATIVA_BUDS)
                .add(ModItems.HEMP_LEAF);
        getOrCreateTagBuilder(ConventionalItemTags.CROPS)
                .addOptionalTag(ModTags.Conventional.HEMP_CROPS);

        getOrCreateTagBuilder(ModTags.Conventional.HEMP_BRICKS).add(ModItems.HEMP_BRICK);
        getOrCreateTagBuilder(ConventionalItemTags.BRICKS)
                .addOptionalTag(ModTags.Conventional.HEMP_BRICKS);

        // The bale is the crop's 9:1 storage block, which is exactly where vanilla files the hay
        // block (c:storage_blocks/wheat).
        getOrCreateTagBuilder(ModTags.Conventional.HEMP_STORAGE_BLOCKS).add(ModBlocks.HEMP_BALE.asItem());
        getOrCreateTagBuilder(ConventionalItemTags.STORAGE_BLOCKS)
                .addOptionalTag(ModTags.Conventional.HEMP_STORAGE_BLOCKS);

        // Hempcrete as concrete is a judgement, not an identity: the real material is hemp hurd in a
        // lime binder, a bio-composite, where Minecraft's concrete stands in for the Portland kind.
        // It joins anyway because these tags describe what a block *is for* in a modded world --
        // a set building block whose powder cures on contact with water, which is precisely what
        // hempcrete does and precisely what a mod reading #c:concretes wants to find. Both are flat
        // item lists in the convention set, so these go in directly rather than as a subtag.
        getOrCreateTagBuilder(ConventionalItemTags.CONCRETES).add(ModBlocks.HEMPCRETE_BLOCK.asItem());
        getOrCreateTagBuilder(ConventionalItemTags.CONCRETE_POWDERS)
                .add(ModBlocks.HEMPCRETE_POWDER_BLOCK.asItem());

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

        // Puts our discs on exactly the same footing as vanilla's twelve common discs: the
        // creeper loot table rolls this tag (expand:true, one entry each) when a skeleton lands
        // the kill, so joining the tag *is* the drop — no loot-table surgery needed. Each disc
        // added here is one more equal-weight entry, so it also dilutes the others slightly.
        var creeperDiscs = getOrCreateTagBuilder(ItemTags.CREEPER_DROP_MUSIC_DISCS);
        // Cross-mod convention tag, so anything that reasons about discs (jukebox blocks, storage
        // filters, JEI-style lookups) picks ours up too.
        var conventionDiscs = getOrCreateTagBuilder(ConventionalItemTags.MUSIC_DISCS);
        for (Item disc : ModItems.MUSIC_DISCS) {
            creeperDiscs.add(disc);
            conventionDiscs.add(disc);
        }

        // The item half of the vibration damping granted in ModBlockTagProvider — vanilla keeps
        // #minecraft:dampens_vibrations as both a block and an item tag, so hemp wool joins both.
        getOrCreateTagBuilder(ItemTags.DAMPENS_VIBRATIONS)
                .add(ModBlocks.HEMP_WOOL.asItem());

        // The carpet's damping and its 67-tick fuel time both ride on this tag rather than being
        // granted separately — see ModBlockTagProvider for why #wool_carpets is safe to join.
        getOrCreateTagBuilder(ItemTags.WOOL_CARPETS)
                .add(ModBlocks.HEMP_CARPET.asItem());
        }
    }


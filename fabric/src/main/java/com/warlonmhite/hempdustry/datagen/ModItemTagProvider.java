package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {
    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        var hempSeeds = getOrCreateTagBuilder(ModTags.Items.HEMP_SEEDS);
        ModItems.hempSeeds().forEach(hempSeeds::add);

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
                .addOptionalTag(ConventionalItemTags.MILK_BUCKETS)
                .addOptionalTag(ConventionalItemTags.OLD_MILK_BUCKETS);

        // The outbound half, and the same reasoning as #c:strings below: nothing in vanilla reads
        // #c:buckets/milk, and the entire value is other mods' recipes taking hemp milk for free.
        getOrCreateTagBuilder(ConventionalItemTags.MILK_BUCKETS)
                .add(ModItems.HEMP_MILK_BUCKET);
        getOrCreateTagBuilder(ConventionalItemTags.OLD_MILK_BUCKETS)
                .add(ModItems.HEMP_MILK_BUCKET);

        // The dosed foods, as one group. Space Cake is absent on purpose: a slice is eaten by using
        // the *block*, which never fires minecraft:consume_item — see ModTags.Items.EDIBLES.
        getOrCreateTagBuilder(ModTags.Items.EDIBLES)
                .add(ModItems.CANNABUTTER_TOAST)
                .add(ModItems.SPACE_COOKIE)
                .add(ModItems.SPACE_BROWNIE)
                .add(ModItems.BHANG_BUCKET)
                .add(ModItems.DAWAMESK);

        // The undosed foods, as one group — the counterpart of EDIBLES above, and disjoint from it.
        getOrCreateTagBuilder(ModTags.Items.HEMP_SEED_FOODS)
                .add(ModItems.TOASTED_HEMP_SEEDS)
                .add(ModItems.HEMP_FLAPJACK)
                .add(ModItems.SIEMIENIOTKA);

        // Hemp seed as bird feed (parrots and chickens) and the fan leaf as goat browse are per-animal
        // food tags from Minecraft 1.20.5 on. On 1.20.1 each animal's food is a list in its own class,
        // so there is no tag here to join (ModAnimalFood, GoatFoodMixin).

        // Cross-mod cordage. This was agreed as `#c:ropes` and is deliberately `#c:strings` instead:
        // hemp fibre crafts 1:1 into vanilla string, which makes it string-tier, while `#c:ropes` by
        // convention means an actual laid rope — the hangable, climbable kind Supplementaries and
        // Farmer's Delight ship. Fibre is what rope is *made from*, not rope. Putting it in `#c:ropes`
        // would hand every mod that expects a rope something four-to-a-stem cheap.
        //
        // `#c:ropes` is the right tag the day the mod has a real hemp_rope item, and it should have
        // one — rope is the single most archetypal hemp product there is.
        //
        // Nothing in vanilla reads `#c:strings`; the whole value here is other mods' recipes, which
        // now take fibre directly and skip a 1:1 crafting step. No balance change, just less friction.
        getOrCreateTagBuilder(ConventionalItemTags.STRINGS)
                .add(ModItems.HEMP_FIBER);

        // Only the two garments: the beanie and the flip-flops have models of their own, and vanilla's
        // trim textures are painted for the armour shape, so a trim on either would never be drawn.
        getOrCreateTagBuilder(ItemTags.TRIMMABLE_ARMOR)
            .add(ModItems.HEMP_SHIRT)
            .add(ModItems.HEMP_HAREM_PANTS);

        // No enchantable tags on 1.20.1: an armour piece takes the enchantments of its slot by its class
        // (ArmorItem), and the material's enchantability opens the table to it.

        // The beanie is the warm piece: knitted, a winter hat. Hemp cloth itself is no warmer than
        // cotton, so the shirt, the pants and above all the flip-flops stay out. One piece in this tag
        // is full immunity (LivingEntity#canFreeze returns false on the first one it finds), and
        // armour points never help in powder snow: freeze damage is in #minecraft:bypasses_armor.
        // Walking on powder snow is a different check, leather boots by name, so flip-flops sink.
        getOrCreateTagBuilder(ItemTags.FREEZE_IMMUNE_WEARABLES).add(ModItems.HEMP_BEANIE);

        // The two garments take dye as leather does on 1.20.1, by being DyeableLeatherItems
        // (HempClothItem); there is no #dyeable tag yet.

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

        // What the Sifting Box will shake resin out of, split by how much resin there is to shake.
        // Flower is where the trichomes actually are, so a bud fills a level every time; leaf is
        // bulk with a thin dusting on it and takes about three. The two rates *are* the balance of
        // the block, which is why they are two tags rather than one — see SiftingBoxBlock.
        //
        // Buds come off the strain registry rather than being named, exactly as the spliff and
        // decarboxylating recipes do, so a third strain is siftable the moment it exists.
        HolderLookup.RegistryLookup<Strain> strains = Strain.registry(wrapperLookup);
        var flower = getOrCreateTagBuilder(ModTags.Items.SIFTABLE_FLOWER);
        for (ResourceKey<Strain> key : ModStrains.BUILT_IN) {
            Strain strain = strains.getOrThrow(key).value();
            // Only things that grew on a plant are plant matter. Hashish is a strain entry too, but
            // its "buds" are resin, and resin never goes back into the screen: only kief re-sifts.
            // Beldía's buds sit in their own tag below, at their own rate.
            if (strain.wildFlower().isPresent() && strain.buds() != ModItems.BELDIA_BUDS) {
                flower.add(strain.buds());
            }
        }
        getOrCreateTagBuilder(ModTags.Items.SIFTABLE_RESINOUS)
                .add(ModItems.BELDIA_BUDS);
        getOrCreateTagBuilder(ModTags.Items.SIFTABLE_TRIM)
                .add(ModItems.HEMP_LEAF);
        // The re-sift. Only kief is in here: bubble hash and filtered kief are each already the
        // clean end of their own road, and a further pass on either would be a pure loss for no
        // mechanical gain. No route through this block may be a trap.
        getOrCreateTagBuilder(ModTags.Items.SIFTABLE_KIEF)
                .add(ModItems.KIEF);

        // ---------------------------------------------------------------------
        // Convention tags, the outbound half. Nothing in vanilla reads any of these; the entire
        // value is other mods' recipes and machinery finding this mod's materials without either
        // side having heard of the other.
        //
        // c:crops, c:bricks and c:storage_blocks hold nothing but subtags, so each gets a
        // c:<parent>/hemp folded into its parent rather than the item shoved in at the top level --
        // see ModTags.Conventional. The parent references are optional, which costs nothing and
        // means a stripped-down Fabric API that omits one cannot take the whole tag down with it
        // (one unresolvable *required* entry drops every other entry in the file).
        getOrCreateTagBuilder(ModTags.Conventional.HEMP_CROPS)
                .add(ModItems.INDICA_BUDS)
                .add(ModItems.SATIVA_BUDS)
                .add(ModItems.BELDIA_BUDS)
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

        // The same, in Forge's names, for the Forge packs of 1.20.1: forge:crops and
        // forge:storage_blocks hold subtags as c:'s do, and string, leather and armour are flat lists
        // there. Forge has no tag for bricks, concrete, discs, milk or the food kinds.
        getOrCreateTagBuilder(ConventionalItemTags.FORGE_HEMP_CROPS)
                .addTag(ModTags.Conventional.HEMP_CROPS);
        getOrCreateTagBuilder(ConventionalItemTags.FORGE_CROPS)
                .addOptionalTag(ConventionalItemTags.FORGE_HEMP_CROPS);
        getOrCreateTagBuilder(ConventionalItemTags.FORGE_HEMP_STORAGE_BLOCKS)
                .addTag(ModTags.Conventional.HEMP_STORAGE_BLOCKS);
        getOrCreateTagBuilder(ConventionalItemTags.FORGE_STORAGE_BLOCKS)
                .addOptionalTag(ConventionalItemTags.FORGE_HEMP_STORAGE_BLOCKS);
        getOrCreateTagBuilder(ConventionalItemTags.FORGE_STRING).add(ModItems.HEMP_FIBER);
        getOrCreateTagBuilder(ConventionalItemTags.FORGE_LEATHER).add(ModItems.HEMP_CANVAS);
        getOrCreateTagBuilder(ConventionalItemTags.FORGE_ARMORS)
                .add(ModItems.HEMP_BEANIE)
                .add(ModItems.HEMP_SHIRT)
                .add(ModItems.HEMP_HAREM_PANTS)
                .add(ModItems.FLIP_FLOPS);

        // The inbound half: what trims a plant and what feeds a Grow Pot, every mod's. On 1.20.1 no
        // single convention tag holds every pair of shears or every fertiliser -- Fabric's, Forge's
        // and the newer c: names each hold some -- so the mod's own tags take all of them, each
        // optional, beside vanilla's own item.
        getOrCreateTagBuilder(ModTags.Items.SHEARS)
                .add(Items.SHEARS)
                .addOptionalTag(ConventionalItemTags.SHEAR_TOOLS)
                .addOptionalTag(ConventionalItemTags.OLD_SHEARS)
                .addOptionalTag(ConventionalItemTags.FORGE_SHEARS);
        getOrCreateTagBuilder(ModTags.Items.FERTILIZERS)
                .add(Items.BONE_MEAL)
                .addOptionalTag(ConventionalItemTags.FERTILIZERS)
                .addOptionalTag(ConventionalItemTags.FORGE_FERTILIZERS);

        getOrCreateTagBuilder(ItemTags.PLANKS).add(ModBlocks.HEMP_PLANKS.asItem());
        getOrCreateTagBuilder(ItemTags.WOODEN_SLABS).add(ModBlocks.HEMP_PLANKS_SLAB.asItem());
        // The rest of the plank set's wooden tags, as the block side has them. Vanilla makes every one
        // of them fuel; #minecraft:non_flammable_wood below takes the set out again.
        getOrCreateTagBuilder(ItemTags.WOODEN_STAIRS).add(ModBlocks.HEMP_PLANKS_STAIRS.asItem());
        getOrCreateTagBuilder(ItemTags.WOODEN_FENCES).add(ModBlocks.HEMP_PLANKS_FENCE.asItem());
        getOrCreateTagBuilder(ItemTags.FENCE_GATES).add(ModBlocks.HEMP_PLANKS_FENCE_GATE.asItem());
        getOrCreateTagBuilder(ItemTags.WOODEN_DOORS).add(ModBlocks.HEMP_PLANKS_DOOR.asItem());
        getOrCreateTagBuilder(ItemTags.WOODEN_TRAPDOORS).add(ModBlocks.HEMP_PLANKS_TRAPDOOR.asItem());
        getOrCreateTagBuilder(ItemTags.WOODEN_BUTTONS).add(ModBlocks.HEMP_PLANKS_BUTTON.asItem());
        getOrCreateTagBuilder(ItemTags.WOODEN_PRESSURE_PLATES).add(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE.asItem());
        getOrCreateTagBuilder(ItemTags.WALLS).add(ModBlocks.HEMP_BRICKS_WALL.asItem());

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
                .add(ModItems.HEMP_PLANKS_HANGING_SIGN)
                // The parquet is in no fuel tag, and joins so the rule stays "the whole family",
                // not a list to keep checking.
                .add(ModBlocks.HEMP_PARQUET.asItem())
                .add(ModBlocks.HEMP_PARQUET_STAIRS.asItem())
                .add(ModBlocks.HEMP_PARQUET_SLAB.asItem());

        getOrCreateTagBuilder(ItemTags.STAIRS).add(ModBlocks.HEMP_PARQUET_STAIRS.asItem()).add(ModBlocks.HEMP_BRICKS_STAIRS.asItem());
        getOrCreateTagBuilder(ItemTags.SLABS).add(ModBlocks.HEMP_PARQUET_SLAB.asItem()).add(ModBlocks.HEMP_BRICKS_SLAB.asItem());
        getOrCreateTagBuilder(ItemTags.BEDS).add(ModBlocks.HEMP_BED.asItem());

        // Every device accepts Unbreaking and Mending on 1.20.1 by being damageable, which is what
        // those two enchantments ask of an item; there is no #enchantable/durability yet.

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


package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.custom.BhangItem;
import com.warlonmhite.hempdustry.item.custom.CannabutterItem;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.EdibleItem;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.HempMilkItem;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.HempBoatItem;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.item.custom.SpliffItem;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.sound.ModSounds;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.HangingSignItem;
import net.minecraft.item.equipment.EquipmentType;
import com.warlonmhite.hempdustry.component.ModComponents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SignItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ModItems {

    public static final Item INDICA_SEEDS = registerItem("indica_seeds", settings -> new BlockItem(ModBlocks.INDICA_CROP, settings));
    public static final Item INDICA_BUDS = registerItem("indica_buds", settings -> new Item(settings));
    public static final Item SATIVA_SEEDS = registerItem("sativa_seeds", settings -> new BlockItem(ModBlocks.SATIVA_CROP, settings));
    public static final Item SATIVA_BUDS = registerItem("sativa_buds", settings -> new Item(settings));
    public static final Item HEMP_STEM = registerItem("hemp_stem", settings -> new Item(settings));
    /**
     * A stalk that has been soaked until the pectin gluing its bast fibre to the woody core has
     * rotted away — the first of the four traditional steps ({@code rouissage}, {@code broyage},
     * {@code teillage}, {@code peignage}). It is not fibre yet, and that is the whole point: the
     * cauldron rets, the crafting grid does the breaking and scutching. See {@code materials.md}.
     */
    public static final Item RETTED_HEMP_STEM = registerItem("retted_hemp_stem", settings -> new Item(settings));
    /**
     * The fan leaf — and, since 2026-09-08, the loom's cannabis-leaf banner pattern item.
     *
     * <p>{@code provides_banner_patterns} is what a loom actually reads; there is no separate
     * {@code *_banner_pattern} item, because the leaf itself is the natural stencil and vanilla
     * never consumes a pattern item anyway (the loom takes only the banner and the dye, confirmed
     * in {@code LoomScreenHandler}). So this costs a player one leaf they keep for ever, which is
     * the right price for something purely cosmetic. The tag it names is the other half — see
     * {@link ModTags.BannerPatterns#HEMP_LEAF_PATTERN_ITEM}.
     */
    public static final Item HEMP_LEAF = registerItem("hemp_leaf", settings -> new Item(
            settings.component(DataComponentTypes.PROVIDES_BANNER_PATTERNS, ModTags.BannerPatterns.HEMP_LEAF_PATTERN_ITEM)));

    // The cannabutter chain's two intermediates. Both are strain-agnostic: every strain's buds and
    // the leaf all decarboxylate to the same thing, so the pipeline downstream stays a single line
    // of items. Strain identity is carried by the smoking system, not by edibles.
    public static final Item DECARBOXYLATED_HEMP = registerItem("decarboxylated_hemp", settings -> new Item(settings));
    public static final Item WASHED_DECARBOXYLATED_HEMP = registerItem("washed_decarboxylated_hemp", settings -> new Item(settings));

    /**
     * Pressed dry-sift hashish — resin separated from the plant and squeezed into a slab.
     *
     * <p><b>Strain-agnostic</b>, like everything else this far down the chain. Sifting is a
     * mechanical separation, not a chemical one: it keeps the trichome heads and throws the leaf
     * away, and a trichome head is a trichome head whichever plant grew it. That also means it can
     * never carry a strain the way a spliff does, which is why it is here beside the oven's two
     * intermediates rather than in the smoking run.
     *
     * <p><b>Inert in the hand, like the rest of the raw plant.</b> Sifting concentrates THCA; it
     * does not decarboxylate it. Eating a lump of hash does close to nothing, so this has no food
     * component and no effects — it goes through the Decarboxylator like everything else. See
     * CLAUDE.md, <i>heat activates</i>.
     */
    public static final Item HASHISH = registerItem("hashish", settings -> new Item(settings));

    /**
     * Charas — soft black resin rubbed off a <b>living</b> plant, and the only hash you can have
     * before you own a single block.
     *
     * <p>It comes off the shears while trimming ({@link com.warlonmhite.hempdustry.block.custom.Defoliation#tryCut},
     * 1-in-8 per cut) and <b>the plant survives</b>, which is what makes it a different verb from
     * everything else in the mod: every other material comes from killing a plant or feeding a
     * block, and this one comes from looking after one.
     *
     * <p><b>No bar and no block, ever.</b> Only pressed hash gets a bar, because pressing is what a
     * bar <em>is</em>. Charas is soft, unpressed and scarce enough that you will rarely hold nine at
     * once, so a storage block would be one nobody fills.
     *
     * <p><b>It really is scissor hash.</b> The resin that gums up a trimmer's blades, scraped off
     * and smoked, is a known trimmer's perk with its own name; charas proper is rubbed from living
     * plants by hand. Both are resin taken from a plant that is still growing, which is why both
     * come out black and soft and keep the monoterpenes that drying destroys.
     *
     * <p><b>Strain-agnostic and inert in the hand</b>, exactly like {@link #HASHISH}: resin carries
     * no memory of which plant it came off, and it never decarboxylates — see CLAUDE.md,
     * <i>heat activates</i>, and {@code hashish.md} §5.
     */
    public static final Item CHARAS = registerItem("charas", settings -> new Item(settings));

    /**
     * Filtered hashish — loose blonde powder, the cleanest thing the screen makes.
     *
     * <p><b>Filtering is real and it is about purity, not power.</b> Successive sieve passes at
     * shrinking mesh — 160 → 90 → 70 µm in the trade — with the plant matter pulled out between
     * each. Trichome heads run 25–200 µm, so every pass drops more leaf and keeps less. Seven
     * hashish in, four out: a 43% yield loss, which is the honest cost of a pass.
     *
     * <p><b>What that buys is smoothness, not strength.</b> Same effects at the same amplifiers, and
     * <em>half</em> the green-out odds via {@code green_out_factor = 2.0}. That is the honest
     * version: less leaf means less chlorophyll and less coughing, which is the entire reason anyone
     * filters. It is a true sidegrade — 43% of your hash for a gentler ride.
     *
     * <p><b>Loose, and there is no bar.</b> The Dry Sifter presses <em>plant-derived</em> powder into
     * a slab because there is enough of it to press; re-sifting resin leaves a small quantity of very
     * fine powder and there is not. Bubble hash comes out of the bags the same way, and it is what
     * gives the block's two content kinds two different shapes of output rather than two numbers.
     *
     * <p><b>Deliberately not named for a pass count.</b> It was "Triple-Filtered" until 2026-09-09
     * and that was simply wrong arithmetic: the block runs <em>two</em> passes, the bar and the
     * re-sift. But the fix is not "Double-Filtered" — <b>a number in the name promises a ladder</b>,
     * and this mod made purity a single sidegrade rather than a grade ladder on purpose (the family's
     * axis is <em>method</em>, not grade). The registered id never carried a number either.
     *
     * <p><b>It is not siftable again</b>, so the mod's one filtering step stands for the trade's
     * whole 1×–3× range. A third pass would be a second near-identical blonde for no mechanic, and
     * the 43% loss already says what a pass costs.
     */
    public static final Item FILTERED_HASHISH = registerItem("filtered_hashish", settings -> new Item(settings));

    public static final Item HEMP_PLANKS_SIGN = registerItem("hemp_planks_sign", settings -> new SignItem(ModBlocks.HEMP_PLANKS_SIGN, ModBlocks.HEMP_PLANKS_WALL_SIGN, settings.maxCount(16)));
    public static final Item HEMP_PLANKS_HANGING_SIGN = registerItem("hemp_planks_hanging_sign", settings -> new HangingSignItem(ModBlocks.HEMP_PLANKS_HANGING_SIGN, ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN, settings.maxCount(16)));

    public static final Item HEMP_BOAT = registerItem("hemp_boat", settings -> new HempBoatItem(false, settings.maxCount(1)));
    public static final Item HEMP_CHEST_BOAT = registerItem("hemp_chest_boat", settings -> new HempBoatItem(true, settings.maxCount(1)));

    public static final Item HEMP_FLOUR = registerItem("hemp_flour", settings -> new Item(settings));
    public static final Item HEMP_FIBER = registerItem("hemp_fiber", settings -> new Item(settings));
    /**
     * Woven hemp cloth, and the mod's stand-in for leather. Hemp canvas is the oldest use the plant
     * has — the word "canvas" is itself a corruption of "cannabis" — and sailcloth and rope were
     * what states grew hemp for long before anyone smoked it. It currently substitutes leather in
     * the item frame recipe only; see CLAUDE.md for the open question of which other leather recipes
     * it should reach.
     */
    public static final Item HEMP_CANVAS = registerItem("hemp_canvas", settings -> new Item(settings));
    public static final Item HEMPCRETE = registerItem("hempcrete", settings -> new Item(settings));
    public static final Item HEMP_BRICK = registerItem("hemp_brick", settings -> new Item(settings));
    public static final Item CANNABUTTER = registerItem("cannabutter", settings -> new CannabutterItem(settings));

    // ---------------------------------------------------------------------
    // Wholesome hemp-seed food. No THC anywhere in this block — hemp seed is
    // one of the most nutritionally complete foods there is (9% carbohydrate,
    // 49% fat, 31% protein, all eight essential amino acids) and the mod had
    // nothing to say about it. None of these are alwaysEdible: that flag is
    // for the dosed edibles below, where being unable to eat when full means
    // being unable to dose. Ordinary food should obey ordinary hunger.
    // ---------------------------------------------------------------------

    /**
     * Roasted hemp seed — vanilla's core food verb (cook the raw thing) applied to the one raw thing
     * the mod had no cooked form of. 2 / 0.6: low nutrition, high saturation modifier, which is what
     * a small, very dense seed honestly is. Dried-kelp tier.
     */
    public static final Item TOASTED_HEMP_SEEDS = registerItem("toasted_hemp_seeds", settings -> new Item(settings.food(new FoodComponent.Builder()
                    .nutrition(2).saturationModifier(0.6F).build(),
                    ConsumableComponents.food().consumeSeconds(0.8F).build())));

    /** Seeds bound with honey. Portable and saturation-heavy, the shape a flapjack actually is. */
    public static final Item HEMP_FLAPJACK = registerItem("hemp_flapjack", settings -> new Item(settings.food(new FoodComponent.Builder()
                    .nutrition(4).saturationModifier(0.6F).build())));

    /**
     * Hemp seed milk. Vanilla's milk bucket with a different source — clears effects, not food. See
     * {@link HempMilkItem} for why both halves of that are deliberate. The recipe remainder is what
     * lets it be poured into siemieniotka without losing the bucket.
     */
    public static final Item HEMP_MILK_BUCKET = registerItem("hemp_milk_bucket",
            settings -> new HempMilkItem(settings.maxCount(1).recipeRemainder(Items.BUCKET)
                    // Drinking is a component since 1.21.2, not a pair of Item overrides: this is
                    // what makes it play the drink sound, use the drinking animation and take
                    // vanilla milk's 1.6 seconds. ConsumableComponents.DRINK is exactly that and
                    // nothing else; the effect-clearing stays in HempMilkItem#finishUsing.
                    .component(DataComponentTypes.CONSUMABLE, ConsumableComponents.DRINK)));

    /**
     * Siemieniotka — the Silesian hemp-seed Christmas Eve soup. Vanilla stew parity (6 / 0.6) and
     * vanilla's own bowl, returned on eating the way every vanilla stew returns its own.
     */
    public static final Item SIEMIENIOTKA = registerItem("siemieniotka", settings -> new Item(settings.maxCount(1).useRemainder(Items.BOWL).food(new FoodComponent.Builder()
                    .nutrition(6).saturationModifier(0.6F).build())));

    // ---------------------------------------------------------------------
    // Edibles. Cannabutter's first real use — the payoff for the whole
    // crop -> Decarboxylator -> wash -> Infuser chain.
    //
    // Nutrition/saturation are pitched against the vanilla foods each one is
    // modelled on; none of them do anything beyond feeding you yet, which is
    // the effects pass (CLAUDE.md §5 #14). They are all alwaysEdible, because
    // an edible you cannot eat when full is an edible you cannot dose with —
    // vanilla marks the golden apple the same way for the same reason.
    // ---------------------------------------------------------------------

    /**
     * The bottom rung, and the only one that isn't baked: butter spread on bread you already have.
     * 2 / 0.3, three to a loaf — so a loaf's 5 nutrition and 6.0 saturation becomes 6 nutrition and
     * 3.6 across three slices. Nutrition up, saturation down, nothing created. What it buys is
     * speed: no hemp flour, no cocoa, no sugar.
     */
    public static final Item CANNABUTTER_TOAST = registerItem("cannabutter_toast", settings -> new EdibleItem(settings.food(new FoodComponent.Builder()
                    .nutrition(2).saturationModifier(0.3F).alwaysEdible().build())));

    /** Vanilla cookie parity (2 / 0.1), eight to a batch. The cheap, low-dose entry point. */
    public static final Item SPACE_COOKIE = registerItem("space_cookie", settings -> new EdibleItem(settings.food(new FoodComponent.Builder()
                    .nutrition(2).saturationModifier(0.1F).alwaysEdible().build())));

    /** Richer than a cookie, pitched at an apple (4 / 0.3). Four to a batch. */
    public static final Item SPACE_BROWNIE = registerItem("space_brownie", settings -> new EdibleItem(settings.food(new FoodComponent.Builder()
                    .nutrition(4).saturationModifier(0.3F).alwaysEdible().build())));

    /**
     * Bhang — the drink, and the only edible that skips cannabutter. Kept and drunk from the bucket
     * exactly as vanilla milk is, so it is {@code maxCount(1)} and hands the bucket back when you
     * finish it. 6 / 0.3: filling, but below dawamesk on saturation, which is the right ordering for
     * the cruder preparation. See {@link BhangItem} for the design.
     */
    public static final Item BHANG_BUCKET = registerItem("bhang_bucket", settings -> new BhangItem(settings.maxCount(1).useRemainder(Items.BUCKET).food(new FoodComponent.Builder()
                    .nutrition(6).saturationModifier(0.3F).alwaysEdible().build(),
                    // Food, but a DRINK: the single-argument food() attaches
                    // ConsumableComponents.FOOD, which is where the eating sound and animation come
                    // from since 1.21.2. Two seconds is honey bottle's, so a drink is never quicker
                    // to down than vanilla's.
                    ConsumableComponents.drink().consumeSeconds(2.0F).build())));

    /**
     * Dawamesk — the top of the ladder, and the only edible here with a real history rather than a
     * folk name. Sugar, honey and fruit around the fat make it calorie-dense, so it lands above
     * bread on saturation (6 / 0.6 = 7.2) while costing a whole cannabutter for a single item.
     */
    public static final Item DAWAMESK = registerItem("dawamesk", settings -> new EdibleItem(settings.maxCount(16).food(new FoodComponent.Builder()
                    .nutrition(6).saturationModifier(0.6F).alwaysEdible().build())));

    /**
     * An edible that inherits its dose from cannabutter, and its step on the potency ladder: {@code +1}
     * concentrates, {@code 0} is neutral, {@code -1} spreads the butter thin.
     *
     * @see com.warlonmhite.hempdustry.recipe.Infusion
     */
    public record InfusedEdible(ItemConvertible item, int potencyOffset) {
        /** Lowest tier this edible can ever carry — {@link EdibleEffects#applyOffset} clamps at 1. */
        public int minTier() {
            return EdibleEffects.applyOffset(1, potencyOffset);
        }

        /** Highest tier this edible can ever carry, clamped at {@link EdibleEffects#MAX_TIER}. */
        public int maxTier() {
            return EdibleEffects.applyOffset(EdibleEffects.MAX_TIER, potencyOffset);
        }
    }

    /**
     * Every butter-derived edible with its offset. <b>This is the only place the offsets live</b> —
     * {@code ModRecipeProvider} reads them when it writes the infused recipes, and the creative tab
     * reads them to know which tiers an edible can actually reach. They were duplicated literals in
     * the recipe provider until 2026-08-12; a drift between the two would have advertised tiers no
     * recipe could produce.
     *
     * <p>{@link #BHANG_BUCKET} is deliberately absent: it never touches cannabutter and its recipe
     * fixes it at tier I, Rough, so it has exactly one form.
     */
    public static final List<InfusedEdible> INFUSED_EDIBLES = List.of(
            new InfusedEdible(CANNABUTTER_TOAST, -1),
            new InfusedEdible(SPACE_COOKIE, -1),
            new InfusedEdible(SPACE_BROWNIE, 0),
            new InfusedEdible(ModBlocks.SPACE_CAKE, 0),
            new InfusedEdible(DAWAMESK, +1));

    /** The offset {@code item} was declared with. Throws rather than guessing — datagen reads this. */
    public static int potencyOffsetOf(ItemConvertible item) {
        for (InfusedEdible edible : INFUSED_EDIBLES) {
            if (edible.item() == item) {
                return edible.potencyOffset();
            }
        }
        throw new IllegalArgumentException("Not an infused edible: " + item);
    }

    // One item each, for every strain and every dose. What is rolled or packed into them lives in
    // the smoke_contents component, the way a potion carries potion_contents — so a new strain adds
    // no items, no models and no per-device lang keys. See CLAUDE.md §5b D10.
    public static final Item SPLIFF = registerItem("spliff", settings -> new SpliffItem(settings.rarity(Rarity.COMMON).maxCount(16)));

    /**
     * Every device, keyed by its {@link DeviceType}. <b>Declared above the devices themselves on
     * purpose</b> — static initialisers run in source order, so a map declared below them would
     * still be null when {@code registerDevice} tried to fill it.
     *
     * <p>This exists because the three sites that iterate {@code DeviceType.values()} — the creative
     * tab twice over, the model provider and the viewer pages — used to map the enum back to an item
     * with {@code device == PIPE ? WOODEN_PIPE : BONG}, which silently turns <em>every</em> device
     * after the second into a bong. It failed by showing the wrong item, not by throwing. Iterate
     * {@link #devices()} instead of {@code values()} and a device added later cannot be missed.
     *
     * <p>An {@link EnumMap} because it iterates in ordinal order, which is what keeps the creative
     * tab's device run stable between launches.
     */
    private static final Map<DeviceType, Item> DEVICES = new EnumMap<>(DeviceType.class);

    public static final Item WOODEN_PIPE = registerDevice(DeviceType.PIPE);
    public static final Item BONG = registerDevice(DeviceType.BONG);
    public static final Item VAPORIZER = registerDevice(DeviceType.VAPORIZER);

    // Same shape as a vanilla common disc (single-stack, uncommon, jukebox-playable). The song data
    // — length, comparator output, "Now Playing" label — lives in the JUKEBOX_SONG entry it points at.
    public static final Item MUSIC_DISC_MOONLIGHT = registerItem("music_disc_moonlight", settings -> new Item(settings.maxCount(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(ModSounds.MOONLIGHT_SONG)));
    public static final Item MUSIC_DISC_ROBADOB = registerItem("music_disc_robadob", settings -> new Item(settings.maxCount(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(ModSounds.ROBADOB_SONG)));

    /**
     * Every disc the mod ships, in one place — the creative tab, the two disc tags and the chest
     * loot pool all read this, so a new disc is one entry here plus its item, song and sounds.json
     * lines. Same "one list, no per-item plumbing" shape as {@link Strain#ACTIVE}.
     */
    public static final List<Item> MUSIC_DISCS = List.of(MUSIC_DISC_MOONLIGHT, MUSIC_DISC_ROBADOB);

    public static final Item HEMP_BEANIE = registerItem("hemp_beanie", settings -> new Item(settings
            .armor(ModArmorMaterials.HEMP_ARMOR_MATERIAL, EquipmentType.HELMET)));
    public static final Item HEMP_SHIRT = registerItem("hemp_shirt", settings -> new Item(settings
            .armor(ModArmorMaterials.HEMP_ARMOR_MATERIAL, EquipmentType.CHESTPLATE)));
    public static final Item HEMP_HAREM_PANTS = registerItem("hemp_harem_pants", settings -> new Item(settings
            .armor(ModArmorMaterials.HEMP_ARMOR_MATERIAL, EquipmentType.LEGGINGS)));
    public static final Item FLIP_FLOPS = registerItem("flip_flops", settings -> new Item(settings
            .armor(ModArmorMaterials.HEMP_ARMOR_MATERIAL, EquipmentType.BOOTS)));

    private static Item registerDevice(DeviceType device) {
        Item item = registerItem(device.baseName(), settings -> {
            settings.maxCount(1).maxDamage(device.maxDamage()).rarity(Rarity.COMMON)
                    // Enchantability and repair material are components since 1.21.5, not Item
                    // overrides: every device repairs with the material it is built from — pipe
                    // planks, bong glass, vaporizer iron. Nothing in vanilla repairs with redstone,
                    // so redstone-as-repair would have been the modded tell.
                    .enchantable(device.enchantability());
            switch (device) {
                case PIPE -> settings.repairable(ItemTags.PLANKS);
                case BONG -> settings.repairable(Items.GLASS);
                case VAPORIZER -> settings.repairable(Items.IRON_INGOT);
            }
            return new SmokingDeviceItem(device, settings);
        });
        DEVICES.put(device, item);
        return item;
    }

    /**
     * Every device, in enum order — the one place anything should iterate devices from. See
     * {@link #DEVICES}.
     */
    public static Map<DeviceType, Item> devices() {
        return Collections.unmodifiableMap(DEVICES);
    }

    /**
     * The smokeables the creative <em>grid</em> shows: <b>one per strain at dose 1</b>, plus each
     * device empty. Eight entries at two strains, growing by three per strain rather than eight.
     *
     * <h2>Why strain is listed and dose is not</h2>
     *
     * This copies the enchanted book: {@code ItemGroups} pairs
     * {@code addMaxLevelEnchantedBooks(…, PARENT_TAB_ONLY)} with
     * {@code addAllLevelEnchantedBooks(…, SEARCH_TAB_ONLY)}, so the Ingredients tab shows one book per
     * enchantment while every level lives in the search tab.
     *
     * <p><b>Vanilla would arguably enumerate at our size.</b> It shows all 46 potions including the
     * nine "strong" level-II ones, and it lists integer component levels outright — ominous bottle
     * amplifier 0–4, firework flight 1–3. Its dividing line is scale, not levels, and 16 stacks is
     * potion-scale rather than book-scale. <b>What decides it here is that vanilla's variant set is
     * closed and this one is not</b>: every strain is +8 stacks, and mixing — the thing
     * {@link SmokeContents} is list-shaped for — multiplies the matrix rather than adding to it.
     * Collapsing now costs nothing and means the tab never needs reorganising as content lands.
     *
     * <p><b>Nothing is lost to a modpack by that.</b> Both recipe viewers read the search set, not
     * the visible grid: JEI's {@code ItemStackListFactory} walks {@code getDisplayItems()} <em>and</em>
     * {@code getSearchTabDisplayItems()}, and EMI's {@code EmiStackList} reads
     * {@code getSearchTabStacks()} outright. A search-only stack is a fully listed stack to both.
     *
     * <p>The showcase carries <b>dose 1</b> where vanilla's book carries the max level. Deliberate,
     * and the same reason vanilla writes "Potion of Strength" for the level-I one: dose 1 is the
     * ordinary form of a smokeable — cheapest, and the only one that can never green you out.
     *
     * @see #allSmokeables()
     */
    public static List<ItemStack> showcaseSmokeables(RegistryWrapper.WrapperLookup registries) {
        List<ItemStack> out = new ArrayList<>();
        List<RegistryEntry.Reference<Strain>> strains = Strain.all(registries);
        // Plants only: a pure-hash spliff has no recipe (a joint needs something to burn), so
        // offering one in the tab would be a stack nothing can make. Pipes and bongs take anything.
        for (RegistryEntry.Reference<Strain> strain : rollable(strains)) {
            out.add(loaded(SPLIFF, strain, 1, 0));
        }
        for (Map.Entry<DeviceType, Item> entry : DEVICES.entrySet()) {
            DeviceType device = entry.getKey();
            Item item = entry.getValue();
            out.add(new ItemStack(item));
            for (RegistryEntry<Strain> strain : strains) {
                out.add(loaded(item, strain, 1, device.bowlSize()));
            }
        }
        return out;
    }

    /**
     * Every smokeable that exists: each device empty, and one stack per device per dose per active
     * strain. This is the {@code SEARCH_TAB_ONLY} half of the pair — what the creative search field
     * and the recipe viewers see. It is a superset of {@link #showcaseSmokeables()}, and re-adding
     * those stacks is safe because the duplicate guard in {@code ItemGroup.EntriesImpl#add} exempts
     * search-only entries. Vanilla's two enchanted-book passes overlap the same way.
     */
    public static List<ItemStack> allSmokeables(RegistryWrapper.WrapperLookup registries) {
        List<ItemStack> out = new ArrayList<>();
        List<RegistryEntry.Reference<Strain>> strains = Strain.all(registries);
        for (RegistryEntry.Reference<Strain> strain : rollable(strains)) {
            for (int dose = 1; dose <= SPLIFF_MAX_DOSE; dose++) {
                out.add(loaded(SPLIFF, strain, dose, 0));
            }
        }
        for (Map.Entry<DeviceType, Item> entry : DEVICES.entrySet()) {
            DeviceType device = entry.getKey();
            Item item = entry.getValue();
            out.add(new ItemStack(item));
            for (RegistryEntry<Strain> strain : strains) {
                for (int dose = 1; dose <= device.maxDose(); dose++) {
                    out.add(loaded(item, strain, dose, device.bowlSize()));
                }
            }
        }
        return out;
    }

    /**
     * The strains a spliff can be rolled from on its own — the ones that grew on a plant.
     *
     * <p>A joint needs something to burn and this mod has no tobacco, so pure hash never rolls;
     * it goes in <em>alongside</em> two buds instead. {@code flower().isPresent()} is the mod-wide
     * predicate for "this grew on a plant" and covers anything hash-shaped added later for free.
     */
    private static List<RegistryEntry.Reference<Strain>> rollable(List<RegistryEntry.Reference<Strain>> strains) {
        return strains.stream().filter(strain -> strain.value().flower().isPresent()).toList();
    }

    /** Highest dose a spliff can be rolled at. Devices carry their own ceiling on {@link DeviceType}. */
    public static final int SPLIFF_MAX_DOSE = 3;

    // ---------------------------------------------------------------------
    // Cannabutter and the edibles, for the creative tab
    //
    // Same split as the smokeables above: the grid shows one real form of
    // each, the whole matrix goes in the search tab. What is different is
    // that these have *two* level axes and no identity axis at all, so the
    // search pass is a cross-product -- which is exactly what vanilla's
    // addAllLevelEnchantedBooks does with enchantment x level.
    //
    // The grid entries carry components where they used to carry none.
    // A potency-0 edible does nothing and no recipe can make one, so listing
    // it was listing the mod's own uncraftable potion; vanilla lists neither
    // that nor a blank enchanted book.
    // ---------------------------------------------------------------------

    /**
     * One strength per potency tier: the <b>lowest</b> hemp count that reaches each — the same floor
     * rule the grid entries follow, and it is what makes the grid's strength-1 butter a member of the
     * search set rather than a stack that exists nowhere else.
     */
    private static final int[] TIER_STRENGTHS = {1, 7, 13, 19};

    /**
     * Cannabutter as the grid shows it, and the rule both this and {@link #showcaseEdibles()} follow:
     * <b>the floor of both axes</b> — the lowest tier the item can reach, at Rough. The weakest real
     * form, matching the smokeables showing dose 1, and honest about what an unimproved batch gives.
     *
     * <p>It sits with the processed hemp rather than with the edibles because it <em>is</em> one — the
     * last thing the machines make. Vanilla splits the same way, wheat and sugar in Ingredients while
     * the bread and the cake are in Food & Drinks.
     */
    public static ItemStack showcaseCannabutter() {
        return butter(1, Quality.ROUGH);
    }

    /**
     * The edibles as the grid shows them, in vanilla's food order: the baked run first (bread, cookie,
     * cake is vanilla's own sequence), then the confection, then <b>the drink last</b> — Food & Drinks
     * ends with the milk bucket and the honey bottle for the same reason.
     */
    public static List<ItemStack> showcaseEdibles() {
        List<ItemStack> out = new ArrayList<>();
        for (InfusedEdible edible : INFUSED_EDIBLES) {
            out.add(dosed(edible.item(), edible.minTier(), Quality.ROUGH));
        }
        out.add(dosed(BHANG_BUCKET, 1, Quality.ROUGH));
        return out;
    }

    /**
     * Every dosed stack the mod can produce: cannabutter at each tier × each quality, and every
     * edible across its own reachable tiers × each quality. The {@code SEARCH_TAB_ONLY} half.
     *
     * <p>Cannabutter's raw strength is 1–24, but <b>only its tier is ever read</b>
     * ({@link EdibleEffects#tierFromStrength}), so listing 24 stacks per quality would be listing the
     * same four things six times over. One strength per quartile stands in for the tier.
     */
    public static List<ItemStack> allDosed() {
        List<ItemStack> out = new ArrayList<>();
        for (int strength : TIER_STRENGTHS) {
            for (Quality quality : Quality.values()) {
                out.add(butter(strength, quality));
            }
        }
        for (InfusedEdible edible : INFUSED_EDIBLES) {
            for (int tier = edible.minTier(); tier <= edible.maxTier(); tier++) {
                for (Quality quality : Quality.values()) {
                    out.add(dosed(edible.item(), tier, quality));
                }
            }
        }
        // Bhang has one form and always will: no butter to inherit from, and its recipe pins it.
        out.add(dosed(BHANG_BUCKET, 1, Quality.ROUGH));
        return out;
    }

    private static ItemStack butter(int strength, Quality quality) {
        ItemStack stack = new ItemStack(CANNABUTTER);
        stack.set(ModComponents.STRENGTH, strength);
        stack.set(ModComponents.QUALITY, quality);
        return stack;
    }

    private static ItemStack dosed(ItemConvertible item, int potency, Quality quality) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.POTENCY, potency);
        stack.set(ModComponents.QUALITY, quality);
        return stack;
    }

    /** A spliff or device holding {@code dose} buds of one strain, with a full bowl where it has one. */
    private static ItemStack loaded(Item item, RegistryEntry<Strain> strain, int dose, int charges) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, dose));
        if (charges > 0) {
            stack.set(ModComponents.CHARGES, charges);
        }
        return stack;
    }

    // Since 1.21.3 an item is built from settings that already carry its own RegistryKey, so the
    // caller hands over a factory rather than a finished Item.
    private static Item registerItem(String name, Function<Item.Settings, Item> factory) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Hempdustry.MOD_ID, name));
        return Registry.register(Registries.ITEM, key, factory.apply(new Item.Settings().registryKey(key)));
    }
    public static void registerModItems(){
        Hempdustry.LOGGER.info("Registering Mod Items for " + Hempdustry.MOD_ID);
    }
}

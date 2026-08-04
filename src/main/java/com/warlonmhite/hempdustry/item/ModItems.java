package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.custom.BhangItem;
import com.warlonmhite.hempdustry.item.custom.CannabutterItem;
import com.warlonmhite.hempdustry.item.custom.EdibleItem;
import com.warlonmhite.hempdustry.item.custom.HempMilkItem;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.HempBoatItem;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.item.custom.SpliffItem;
import com.warlonmhite.hempdustry.item.custom.Strain;
import com.warlonmhite.hempdustry.sound.ModSounds;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.HangingSignItem;
import com.warlonmhite.hempdustry.component.ModComponents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SignItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;

public class ModItems {

    public static final Item INDICA_SEEDS = registerItem("indica_seeds", new AliasedBlockItem(ModBlocks.INDICA_CROP, new Item.Settings()));
    public static final Item INDICA_BUDS = registerItem("indica_buds", new Item(new Item.Settings()));
    public static final Item SATIVA_SEEDS = registerItem("sativa_seeds", new AliasedBlockItem(ModBlocks.SATIVA_CROP, new Item.Settings()));
    public static final Item SATIVA_BUDS = registerItem("sativa_buds", new Item(new Item.Settings()));
    public static final Item HEMP_STEM = registerItem("hemp_stem", new Item(new Item.Settings()));
    public static final Item HEMP_LEAF = registerItem("hemp_leaf", new Item(new Item.Settings()));

    // The cannabutter chain's two intermediates. Both are strain-agnostic: every strain's buds and
    // the leaf all decarboxylate to the same thing, so the pipeline downstream stays a single line
    // of items. Strain identity is carried by the smoking system, not by edibles.
    public static final Item DECARBOXYLATED_HEMP = registerItem("decarboxylated_hemp", new Item(new Item.Settings()));
    public static final Item WASHED_DECARBOXYLATED_HEMP = registerItem("washed_decarboxylated_hemp", new Item(new Item.Settings()));

    public static final Item HEMP_PLANKS_SIGN = registerItem("hemp_planks_sign",
            new SignItem(new Item.Settings().maxCount(16), ModBlocks.HEMP_PLANKS_SIGN, ModBlocks.HEMP_PLANKS_WALL_SIGN));
    public static final Item HEMP_PLANKS_HANGING_SIGN = registerItem("hemp_planks_hanging_sign",
            new HangingSignItem(ModBlocks.HEMP_PLANKS_HANGING_SIGN, ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN, new Item.Settings().maxCount(16)));

    public static final Item HEMP_BOAT = registerItem("hemp_boat", new HempBoatItem(false, new Item.Settings().maxCount(1)));
    public static final Item HEMP_CHEST_BOAT = registerItem("hemp_chest_boat", new HempBoatItem(true, new Item.Settings().maxCount(1)));

    public static final Item HEMP_FLOUR = registerItem("hemp_flour", new Item(new Item.Settings()));
    public static final Item HEMP_FIBER = registerItem("hemp_fiber", new Item(new Item.Settings()));
    /**
     * Woven hemp cloth, and the mod's stand-in for leather. Hemp canvas is the oldest use the plant
     * has — the word "canvas" is itself a corruption of "cannabis" — and sailcloth and rope were
     * what states grew hemp for long before anyone smoked it. It currently substitutes leather in
     * the item frame recipe only; see CLAUDE.md for the open question of which other leather recipes
     * it should reach.
     */
    public static final Item HEMP_CANVAS = registerItem("hemp_canvas", new Item(new Item.Settings()));
    public static final Item HEMPCRETE = registerItem("hempcrete", new Item(new Item.Settings()));
    public static final Item HEMP_BRICK = registerItem("hemp_brick", new Item(new Item.Settings()));
    public static final Item CANNABUTTER = registerItem("cannabutter", new CannabutterItem(new Item.Settings()));

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
    public static final Item TOASTED_HEMP_SEEDS = registerItem("toasted_hemp_seeds",
            new Item(new Item.Settings().food(new FoodComponent.Builder()
                    .nutrition(2).saturationModifier(0.6F).snack().build())));

    /** Seeds bound with honey. Portable and saturation-heavy, the shape a flapjack actually is. */
    public static final Item HEMP_FLAPJACK = registerItem("hemp_flapjack",
            new Item(new Item.Settings().food(new FoodComponent.Builder()
                    .nutrition(4).saturationModifier(0.6F).build())));

    /**
     * Hemp seed milk. Vanilla's milk bucket with a different source — clears effects, not food. See
     * {@link HempMilkItem} for why both halves of that are deliberate. The recipe remainder is what
     * lets it be poured into siemieniotka without losing the bucket.
     */
    public static final Item HEMP_MILK_BUCKET = registerItem("hemp_milk_bucket",
            new HempMilkItem(new Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET)));

    /**
     * Siemieniotka — the Silesian hemp-seed Christmas Eve soup. Vanilla stew parity (6 / 0.6) and
     * vanilla's own bowl, returned on eating the way every vanilla stew returns its own.
     */
    public static final Item SIEMIENIOTKA = registerItem("siemieniotka",
            new Item(new Item.Settings().maxCount(1).food(new FoodComponent.Builder()
                    .nutrition(6).saturationModifier(0.6F).usingConvertsTo(Items.BOWL).build())));

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
    public static final Item CANNABUTTER_TOAST = registerItem("cannabutter_toast",
            new EdibleItem(new Item.Settings().food(new FoodComponent.Builder()
                    .nutrition(2).saturationModifier(0.3F).alwaysEdible().build())));

    /** Vanilla cookie parity (2 / 0.1), eight to a batch. The cheap, low-dose entry point. */
    public static final Item SPACE_COOKIE = registerItem("space_cookie",
            new EdibleItem(new Item.Settings().food(new FoodComponent.Builder()
                    .nutrition(2).saturationModifier(0.1F).alwaysEdible().build())));

    /** Richer than a cookie, pitched at an apple (4 / 0.3). Four to a batch. */
    public static final Item SPACE_BROWNIE = registerItem("space_brownie",
            new EdibleItem(new Item.Settings().food(new FoodComponent.Builder()
                    .nutrition(4).saturationModifier(0.3F).alwaysEdible().build())));

    /**
     * Bhang — the drink, and the only edible that skips cannabutter. Kept and drunk from the bucket
     * exactly as vanilla milk is, so it is {@code maxCount(1)} and hands the bucket back when you
     * finish it. 6 / 0.3: filling, but below dawamesk on saturation, which is the right ordering for
     * the cruder preparation. See {@link BhangItem} for the design.
     */
    public static final Item BHANG_BUCKET = registerItem("bhang_bucket",
            new BhangItem(new Item.Settings().maxCount(1).food(new FoodComponent.Builder()
                    .nutrition(6).saturationModifier(0.3F).alwaysEdible()
                    .usingConvertsTo(Items.BUCKET).build())));

    /**
     * Dawamesk — the top of the ladder, and the only edible here with a real history rather than a
     * folk name. Sugar, honey and fruit around the fat make it calorie-dense, so it lands above
     * bread on saturation (6 / 0.6 = 7.2) while costing a whole cannabutter for a single item.
     */
    public static final Item DAWAMESK = registerItem("dawamesk",
            new EdibleItem(new Item.Settings().maxCount(16).food(new FoodComponent.Builder()
                    .nutrition(6).saturationModifier(0.6F).alwaysEdible().build())));


    // One item each, for every strain and every dose. What is rolled or packed into them lives in
    // the smoke_contents component, the way a potion carries potion_contents — so a new strain adds
    // no items, no models and no per-device lang keys. See CLAUDE.md §5b D10.
    public static final Item SPLIFF = registerItem("spliff",
            new SpliffItem(new Item.Settings().rarity(Rarity.COMMON).maxCount(16)));

    public static final Item WOODEN_PIPE = registerDevice(DeviceType.PIPE);
    public static final Item BONG = registerDevice(DeviceType.BONG);

    // Same shape as a vanilla common disc (single-stack, uncommon, jukebox-playable). The song data
    // — length, comparator output, "Now Playing" label — lives in the JUKEBOX_SONG entry it points at.
    public static final Item MUSIC_DISC_GANJA = registerItem("music_disc_ganja",
            new Item(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(ModSounds.GANJA_SONG)));

    public static final Item HEMP_BEANNIE = registerItem("hemp_beannie", new ArmorItem(ModArmorMaterials.HEMP_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings()
            .maxDamage(ArmorItem.Type.HELMET.getMaxDamage(3))));
    public static final Item HEMP_SHIRT = registerItem("hemp_shirt", new ArmorItem(ModArmorMaterials.HEMP_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings()
            .maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(3))));
    public static final Item HEMP_HAREM_PANTS = registerItem("hemp_harem_pants", new ArmorItem(ModArmorMaterials.HEMP_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings()
            .maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(3))));
    public static final Item FLIP_FLOPS = registerItem("flip_flops", new ArmorItem(ModArmorMaterials.HEMP_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings()
            .maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(3))));

    private static Item registerDevice(DeviceType device) {
        return registerItem(device.baseName(), new SmokingDeviceItem(device,
                new Item.Settings().maxCount(1).maxDamage(device.maxDamage()).rarity(Rarity.COMMON)));
    }

    /**
     * Every loaded smokeable worth showing in the creative tab: one <em>stack</em> per device per
     * dose per active strain, plus a spliff per dose per strain.
     *
     * <p>Stacks rather than items is how vanilla lists its forty-odd potions off one registered item,
     * and it is also what keeps them all visible in JEI/EMI, which build their item lists from
     * creative tabs.
     */
    public static List<ItemStack> loadedSmokeables() {
        List<ItemStack> out = new ArrayList<>();
        for (Strain strain : Strain.ACTIVE) {
            for (int dose = 1; dose <= SPLIFF_MAX_DOSE; dose++) {
                out.add(loaded(SPLIFF, strain, dose, 0));
            }
        }
        for (DeviceType device : DeviceType.values()) {
            Item item = device == DeviceType.PIPE ? WOODEN_PIPE : BONG;
            for (Strain strain : Strain.ACTIVE) {
                for (int dose = 1; dose <= device.maxDose(); dose++) {
                    out.add(loaded(item, strain, dose, device.bowlSize()));
                }
            }
        }
        return out;
    }

    /** Highest dose a spliff can be rolled at. Devices carry their own ceiling on {@link DeviceType}. */
    public static final int SPLIFF_MAX_DOSE = 3;

    private static ItemStack loaded(Item item, Strain strain, int dose, int charges) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, dose));
        if (charges > 0) {
            stack.set(ModComponents.CHARGES, charges);
        }
        return stack;
    }

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name), item);
    }
    public static void registerModItems(){
        Hempdustry.LOGGER.info("Registering Mod Items for " + Hempdustry.MOD_ID);
    }
}

package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.HempBoatItem;
import com.warlonmhite.hempdustry.item.custom.PackedSmokingDeviceItem;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.item.custom.SpliffItem;
import com.warlonmhite.hempdustry.item.custom.Strain;
import com.warlonmhite.hempdustry.sound.ModSounds;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.HangingSignItem;
import net.minecraft.item.Item;
import net.minecraft.item.SignItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModItems {
    // Smoking-device registries — populated by registerDevice() as the fields below initialise.
    // Declared first so the WOODEN_PIPE/BONG initialisers can write into them.
    private static final Map<DeviceType, Item> EMPTY_DEVICES = new EnumMap<>(DeviceType.class);
    private static final Map<DeviceType, Map<Strain, Item>> PACKED_DEVICES = new EnumMap<>(DeviceType.class);

    public static final Item INDICA_SEEDS = registerItem("indica_seeds", new AliasedBlockItem(ModBlocks.INDICA_CROP, new Item.Settings()));
    public static final Item INDICA_BUDS = registerItem("indica_buds", new Item(new Item.Settings()));
    public static final Item SATIVA_BUDS = registerItem("sativa_buds", new Item(new Item.Settings()));
    public static final Item HEMP_STEM = registerItem("hemp_stem", new Item(new Item.Settings()));

    public static final Item HEMP_PLANKS_SIGN = registerItem("hemp_planks_sign",
            new SignItem(new Item.Settings().maxCount(16), ModBlocks.HEMP_PLANKS_SIGN, ModBlocks.HEMP_PLANKS_WALL_SIGN));
    public static final Item HEMP_PLANKS_HANGING_SIGN = registerItem("hemp_planks_hanging_sign",
            new HangingSignItem(ModBlocks.HEMP_PLANKS_HANGING_SIGN, ModBlocks.HEMP_PLANKS_WALL_HANGING_SIGN, new Item.Settings().maxCount(16)));

    public static final Item HEMP_BOAT = registerItem("hemp_boat", new HempBoatItem(false, new Item.Settings().maxCount(1)));
    public static final Item HEMP_CHEST_BOAT = registerItem("hemp_chest_boat", new HempBoatItem(true, new Item.Settings().maxCount(1)));

    public static final Item HEMP_FLOUR = registerItem("hemp_flour", new Item(new Item.Settings()));
    public static final Item HEMP_FIBER = registerItem("hemp_fiber", new Item(new Item.Settings()));
    public static final Item HEMPCRETE = registerItem("hempcrete", new Item(new Item.Settings()));
    public static final Item HEMP_BRICK = registerItem("hemp_brick", new Item(new Item.Settings()));
    public static final Item CANNABUTTER = registerItem("cannabutter", new Item(new Item.Settings()));


    public static final Item INDICA_SPLIFF = registerItem("indica_spliff",
            new SpliffItem(Strain.INDICA, new Item.Settings().rarity(Rarity.COMMON).maxCount(16)));

    // Empty, packable devices. Each call also registers that device's per-strain packed variants
    // (one per Strain.ACTIVE) — adding a strain needs no new lines here.
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

    /**
     * Registers an empty {@code device} plus one packed variant per active strain, and records both
     * in the lookup maps. Returns the empty device so it can back a {@code public static final} field.
     */
    private static Item registerDevice(DeviceType device) {
        Item empty = registerItem(device.baseName(),
                new SmokingDeviceItem(device, new Item.Settings().maxCount(1).maxDamage(device.maxDamage()).rarity(Rarity.COMMON)));
        EMPTY_DEVICES.put(device, empty);

        Map<Strain, Item> perStrain = new EnumMap<>(Strain.class);
        for (Strain strain : Strain.ACTIVE) {
            perStrain.put(strain, registerItem(device.baseName() + "_" + strain.id(),
                    new PackedSmokingDeviceItem(device, strain,
                            new Item.Settings().maxCount(1).maxDamage(device.maxDamage()).rarity(Rarity.COMMON))));
        }
        PACKED_DEVICES.put(device, perStrain);
        return empty;
    }

    /** The empty (unpacked) item for a device. */
    public static Item emptyDevice(DeviceType device) {
        return EMPTY_DEVICES.get(device);
    }

    /** The packed item for a device + strain. */
    public static Item packedDevice(DeviceType device, Strain strain) {
        return PACKED_DEVICES.get(device).get(strain);
    }

    /** Every registered packed variant, for the creative tab and datagen. */
    public static List<Item> packedDevices() {
        List<Item> out = new ArrayList<>();
        for (Map<Strain, Item> byStrain : PACKED_DEVICES.values()) {
            out.addAll(byStrain.values());
        }
        return out;
    }

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Hempdustry.MOD_ID, name), item);
    }
    public static void registerModItems(){
        Hempdustry.LOGGER.info("Registering Mod Items for " + Hempdustry.MOD_ID);
    }
}

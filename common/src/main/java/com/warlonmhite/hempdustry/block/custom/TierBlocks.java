package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.strain.BudTier;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The tier a block of tiered hash holds ({@link BudTier}): a hash bar of nine pieces of one tier, and
 * the Sifting Box's batch while it fills. Kept as block state, like the chemotype
 * ({@link ChemotypeBlocks}), so a bar placed, cut or broken hands its tier back, and a saved block with
 * no such property loads as Dank, which every bar 2.1 made is.
 */
public final class TierBlocks {
    private TierBlocks() {
    }

    public static final EnumProperty<BudTier> TIER = EnumProperty.create("tier", BudTier.class);

    /** The tier a placed block holds, Dank on a block without the property. */
    public static BudTier of(BlockState state) {
        return state.hasProperty(TIER) ? state.getValue(TIER) : BudTier.DANK;
    }

    /** Marks a block item with the tier its block will be placed with, and its name with the tier's rarity; Dank clears both. */
    public static ItemStack stampBlockItem(ItemStack stack, BudTier tier) {
        BlockItemStateProperties properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        Map<String, String> kept = new HashMap<>(properties.properties());
        if (tier == BudTier.DANK) {
            kept.remove(TIER.getName());
        } else {
            kept.put(TIER.getName(), tier.getSerializedName());
        }
        if (kept.isEmpty()) {
            stack.remove(DataComponents.BLOCK_STATE);
        } else {
            stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(kept));
        }
        BudTier.stampRarity(stack, tier);
        return stack;
    }

    /** The tier a block item will place with, Dank when it names none. */
    public static BudTier ofBlockItem(ItemStack stack) {
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        if (properties == null) {
            return BudTier.DANK;
        }
        BudTier value = properties.get(TIER);
        return value == null ? BudTier.DANK : value;
    }

    /** Whether {@code stack} is the item of a block that holds a tier. */
    public static boolean holdsTier(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock().defaultBlockState().hasProperty(TIER);
    }
}

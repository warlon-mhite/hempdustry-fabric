package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.strain.Chemotype;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The blocks that store nine pieces of stamped hemp matter — the Block of Hemp Leaves, the two hash
 * bars and the charas ball — keep the chemotype of what went in as a block-state property, so a CBD
 * bar placed and broken gives CBD hashish back. No block entity: five values on a block is ten or
 * twenty states, and a state survives every piston, structure and world edit a block does.
 *
 * <h2>Old worlds</h2>
 *
 * A saved block with no {@code chemotype} property loads as the default, THC-dominant, which is
 * what every such block 2.1 placed was.
 *
 * <h2>Item to block and back</h2>
 *
 * The block item carries vanilla's {@code minecraft:block_state} component, which vanilla's
 * {@code BlockItem} already reads on placement; THC-dominant writes nothing, so it still stacks with
 * every block item 2.1 made. Breaking stamps the drops through {@code hempdustry:set_chemotype}
 * ({@code "from_block": true}).
 */
public final class ChemotypeBlocks {
    private ChemotypeBlocks() {
    }

    public static final EnumProperty<Chemotype> CHEMOTYPE = EnumProperty.create("chemotype", Chemotype.class);

    /** The chemotype a placed block holds, THC-dominant on a block without the property. */
    public static Chemotype of(BlockState state) {
        return state.hasProperty(CHEMOTYPE) ? state.getValue(CHEMOTYPE) : Chemotype.THC_DOMINANT;
    }

    /** Marks a block item with the chemotype its block will be placed with; THC-dominant clears it. */
    public static ItemStack stampBlockItem(ItemStack stack, Chemotype chemotype) {
        BlockItemStateProperties properties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        properties = chemotype == Chemotype.THC_DOMINANT
                ? withoutChemotype(properties)
                : properties.with(CHEMOTYPE, chemotype);
        if (properties.isEmpty()) {
            stack.remove(DataComponents.BLOCK_STATE);
        } else {
            stack.set(DataComponents.BLOCK_STATE, properties);
        }
        return stack;
    }

    /** The chemotype a block item will place with. */
    public static Chemotype ofBlockItem(ItemStack stack) {
        BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        if (properties == null) {
            return Chemotype.THC_DOMINANT;
        }
        Chemotype value = properties.get(CHEMOTYPE);
        return value == null ? Chemotype.THC_DOMINANT : value;
    }

    private static BlockItemStateProperties withoutChemotype(BlockItemStateProperties properties) {
        java.util.Map<String, String> kept = new java.util.HashMap<>(properties.properties());
        kept.remove(CHEMOTYPE.getName());
        return new BlockItemStateProperties(kept);
    }
}

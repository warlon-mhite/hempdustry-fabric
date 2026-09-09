package com.warlonmhite.hempdustry.recipe;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.MoonRockItem;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

/**
 * Packs an empty pipe/bong with buds in the crafting grid. The number of buds is the <b>dose</b>,
 * which becomes the effect level of every hit from that bowl — 1 bud is level I, up to the device's
 * {@link DeviceType#maxDose()}. See CLAUDE.md §5b D10.
 *
 * <h2>Why this stays a special recipe</h2>
 *
 * A plain shapeless recipe's result is a fixed {@link ItemStack}, so it cannot carry the device's
 * durability, enchantments and repair cost across — it would hand back a pristine device and make
 * the thing effectively unbreakable. It also cannot vary its output by how many buds were supplied.
 * One instance covers every device × strain × dose by inspecting the grid.
 *
 * <p>The cost of staying special is that neither the recipe book nor JEI/EMI can see this at all,
 * which is why a recipe-viewer plugin is a hard requirement rather than a nicety — CLAUDE.md §5b D11.
 *
 * <h2>Dose is not the same number as the stack count, and the cap is not the same number as dose</h2>
 *
 * Two rules, both of which used to be one:
 *
 * <ul>
 *   <li><b>One item is worth {@code Strain.dosePerItem}</b>, not necessarily one dose. It is 1 for
 *       every bud and every pinch of hash; rosin is 3, because a dab is not a step on a ladder —
 *       one piece <em>is</em> the bowl.</li>
 *   <li><b>The cap is on the largest entry, not on the total.</b> Every load the mod could build
 *       before this had exactly one entry, so the two were the same number and nothing changes for
 *       any of them. What it buys is that a moon rock — the plant at 3 with a pinch of hashish
 *       riding along, total 4 — is measured on the 3 that decides its effect level, and is refused
 *       by a pipe (max 2) and a vaporizer (max 1) without a word being written about either.</li>
 * </ul>
 *
 * <p>Together they are also the whole of "concentrates are bong-only": only the bong's
 * {@code maxDose} is 3, so rosin and moon rocks fit nothing else. No rule says so; the numbers do.
 */
public class PackingRecipe extends SpecialCraftingRecipe {
    public PackingRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return find(input, world.getRegistryManager()) != null;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        Match match = find(input, lookup);
        if (match == null) {
            return ItemStack.EMPTY;
        }
        DeviceType device = ((SmokingDeviceItem) match.device.getItem()).device();
        // Copying the stack rather than building a fresh one is what carries durability and
        // enchantments through. Packing is now just two components being set on the same item.
        ItemStack packed = match.device.copyWithCount(1);
        packed.set(ModComponents.SMOKE_CONTENTS, match.contents);
        packed.set(ModComponents.CHARGES, device.bowlSize());
        return packed;
    }

    @Override
    public RecipeSerializer<? extends PackingRecipe> getSerializer() {
        return ModRecipes.PACKING;
    }

    /**
     * Requires exactly one <em>empty</em> device plus one load's worth of material, else
     * {@code null}. A load is either 1..maxDose buds of a single strain, or exactly one moon rock.
     *
     * <p>An already-packed device is rejected deliberately: allowing it would silently discard the
     * bowl already in there along with whatever charges were left on it.
     */
    private static Match find(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        ItemStack device = ItemStack.EMPTY;
        // A moon rock arrives with its load already built -- the plant at 3 and a pinch of hashish --
        // so it is copied across rather than counted. It is the one item in the grid that IS a bowl,
        // which is why it may not share the grid with loose buds: the two would be two loads.
        SmokeContents preloaded = null;
        RegistryEntry<Strain> strain = null;
        int dose = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SmokingDeviceItem) {
                if (!device.isEmpty() || !SmokingDeviceItem.contentsOf(stack).isEmpty()) {
                    return null; // more than one device, or one that is already packed
                }
                device = stack;
            } else if (stack.getItem() instanceof MoonRockItem) {
                SmokeContents load = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
                if (preloaded != null || stack.getCount() != 1 || load.isEmpty()) {
                    return null; // two of them, a whole stack of them, or one that carries nothing
                }
                preloaded = load;
            } else {
                // Resolved against the world's strain registry rather than a fixed list, so a
                // datapack-defined strain packs like any other.
                RegistryEntry<Strain> budStrain =
                        Strain.fromBuds(registries, stack.getItem()).orElse(null);
                if (budStrain == null || (strain != null && strain != budStrain)) {
                    return null; // a foreign item, or a second strain (no mixing yet)
                }
                strain = budStrain;
                // One item is not necessarily one dose. See the class comment.
                dose += stack.getCount() * budStrain.value().dosePerItem();
            }
        }
        if (device.isEmpty()) {
            return null;
        }
        SmokeContents contents;
        if (preloaded != null) {
            if (strain != null) {
                return null; // a moon rock AND loose buds -- that is two bowls, not one
            }
            contents = preloaded;
        } else if (strain != null && dose >= 1) {
            contents = SmokeContents.of(strain, dose);
        } else {
            return null;
        }
        DeviceType type = ((SmokingDeviceItem) device.getItem()).device();
        // The largest entry, not the total: it is the number that becomes the effect level, and it
        // is what a device's bowl is measured in.
        int level = 0;
        for (SmokeContents.Entry entry : contents.entries()) {
            level = Math.max(level, entry.count());
        }
        return level >= 1 && level <= type.maxDose() ? new Match(device, contents) : null;
    }

    private record Match(ItemStack device, SmokeContents contents) {
    }
}

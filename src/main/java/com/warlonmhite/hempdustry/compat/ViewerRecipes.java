package com.warlonmhite.hempdustry.compat;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModCauldronBehaviors;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.recipe.DecarboxylatingRecipe;
import com.warlonmhite.hempdustry.recipe.InfusingRecipe;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * What a recipe viewer should be shown, worked out once and read by both plugins.
 *
 * <h2>Why this class exists</h2>
 *
 * EMI and JEI have entirely separate APIs, but they are being asked the same question — <em>what
 * does this machine turn into what, and what does the player have to know that the slots do not
 * say</em>. Answering it twice is how the two viewers drift apart, and a mod whose EMI page and JEI
 * page disagree is worse than one with neither. So the answer lives here, in plain Minecraft types,
 * and each plugin is a thin adapter over it.
 *
 * <p><b>Nothing in this package may be referenced from common code.</b> It is only ever loaded by a
 * viewer that is present; see {@code compat/emi} and {@code compat/jei}.
 *
 * <h2>What each category is for</h2>
 *
 * <ul>
 *   <li><b>Decarboxylating</b> and <b>Infusing</b> read the real {@code RecipeType}s, so a pack that
 *       rebalances them with KubeJS gets a truthful page for free.</li>
 *   <li><b>Cauldron</b> has no recipe to read: retting and washing are {@code CauldronBehavior}
 *       entries, which no viewer can see and no datapack can express. They are built here from the
 *       same constants the behaviours use, so the page cannot drift from the code.</li>
 *   <li><b>Packing</b> is not a category at all — see {@link #packing}.</li>
 * </ul>
 */
public final class ViewerRecipes {

    private ViewerRecipes() {
    }

    public static final Identifier DECARBOXYLATING = Identifier.of(Hempdustry.MOD_ID, "decarboxylating");
    public static final Identifier INFUSING = Identifier.of(Hempdustry.MOD_ID, "infusing");
    public static final Identifier CAULDRON = Identifier.of(Hempdustry.MOD_ID, "cauldron");

    /**
     * One row in a viewer: what goes in, what comes out, and the lines of text that carry whatever
     * the slots cannot say.
     *
     * <p>{@code notes} is the part that matters most here. Both machines have a precondition no
     * arrangement of slots can express — the Decarboxylator burns fuel, the Infuser needs something
     * hot <em>underneath</em> it — and a player reading a viewer page is exactly the player who has
     * not found that out yet.
     *
     * @param id        unique per row; a viewer uses it for lookups and for its own bookkeeping
     * @param inputs    in display order
     * @param output    the result, already carrying any components it should show
     * @param notes     lines drawn under the slots, already translated
     * @param synthetic whether this row has no matching recipe in the recipe manager. EMI checks,
     *                  and wants a synthetic id's path prefixed with {@code /} so it can tell the
     *                  difference between "made up for display" and "a real recipe that has gone
     *                  missing" — the second being a bug worth reporting and the first not.
     */
    public record Entry(Identifier id, List<Ingredient> inputs, ItemStack output, List<Text> notes,
                        boolean synthetic) {
    }

    /** Every tray-load the oven will accept, read from {@code hempdustry:decarboxylating}. */
    public static List<Entry> decarboxylating(World world) {
        List<Entry> out = new ArrayList<>();
        for (RecipeEntry<DecarboxylatingRecipe> entry : ModRecipes.allOfType(world, ModRecipes.DECARBOXYLATING_TYPE)) {
            DecarboxylatingRecipe recipe = entry.value();
            out.add(new Entry(entry.id().getValue(), List.of(recipe.ingredient()), recipe.result(),
                    List.of(Text.translatable("hempdustry.category.decarboxylating.info",
                            seconds(DecarboxylatorBlockEntity.cookTime()))),
                    false));
        }
        return out;
    }

    /**
     * The tub's conversion, read from {@code hempdustry:infusing}.
     *
     * <p>Shown as <b>milk + unwashed + washed</b> even though a batch never needs all three: washed
     * and unwashed are interchangeable in the slots and mixing them is the only way to reach the
     * better grades, so listing both is the honest picture of what the machine takes. The output
     * carries no strength or quality component because those are measured per batch, not per recipe
     * — the note says so rather than the page implying a fixed result.
     */
    public static List<Entry> infusing(World world) {
        List<Entry> out = new ArrayList<>();
        for (RecipeEntry<InfusingRecipe> entry : ModRecipes.allOfType(world, ModRecipes.INFUSING_TYPE)) {
            InfusingRecipe recipe = entry.value();
            out.add(new Entry(entry.id().getValue(),
                    List.of(recipe.container(), recipe.hemp(), recipe.washedHemp()),
                    recipe.result(),
                    List.of(Text.translatable("hempdustry.category.infusing.heat"),
                            Text.translatable("hempdustry.category.infusing.batch",
                                    InfuserBlockEntity.BATCH_CAP,
                                    minutes(InfuserBlockEntity.minTime()),
                                    minutes(InfuserBlockEntity.fullTime()))),
                    false));
        }
        return out;
    }

    /**
     * The two water-cauldron behaviours, which are the only step in the whole chain a player cannot
     * discover from anywhere else: they are not recipes, so the recipe book cannot show them, and
     * they are code rather than data, so a datapack cannot even list them.
     *
     * <p>Built from {@link ModCauldronBehaviors}' own constants so the numbers on the page are the
     * numbers the block uses.
     */
    public static List<Entry> cauldron() {
        return List.of(
                soak("retting", ModItems.HEMP_STEM, ModItems.RETTED_HEMP_STEM,
                        ModCauldronBehaviors.RET_PER_LEVEL),
                soak("washing", ModItems.DECARBOXYLATED_HEMP, ModItems.WASHED_DECARBOXYLATED_HEMP,
                        ModCauldronBehaviors.WASH_PER_LEVEL));
    }

    private static Entry soak(String name, Item input, Item output, int perLevel) {
        return new Entry(Identifier.of(Hempdustry.MOD_ID, "cauldron/" + name),
                List.of(Ingredient.ofItems(input), Ingredient.ofItems(Items.WATER_BUCKET)),
                new ItemStack(output),
                List.of(Text.translatable("hempdustry.category.cauldron.info", perLevel)),
                true);
    }

    /**
     * Every way a device can be packed: an empty pipe or bong plus 1..{@code maxDose} buds of one
     * strain, yielding that device loaded to that dose.
     *
     * <h2>These are handed to the viewers as ordinary crafting recipes, on purpose</h2>
     *
     * Packing is a {@code SpecialCraftingRecipe} — it has to be, because durability and enchantments
     * must survive it and the result varies with the bud count — and a special recipe is invisible
     * to the recipe book and to every viewer. The obvious fix is a bespoke "packing" category. The
     * better one is to hand each permutation over as a plain shapeless crafting recipe, because that
     * is <em>what the player actually does</em>: put a device and some buds in the grid. It costs no
     * custom category, it lands in the crafting tab where a player already looks, and both viewers'
     * built-in "move ingredients into the grid" support works on it for free — which is the single
     * feature a player most expects to behave identically across every mod.
     *
     * <p>The permutations are real, not illustrative: the same components the real recipe sets, so
     * the entry's output is byte-for-byte the item crafting one produces.
     */
    public static List<Packing> packing(RegistryWrapper.WrapperLookup registries) {
        List<Packing> out = new ArrayList<>();
        for (RegistryEntry.Reference<Strain> strain : Strain.all(registries)) {
            for (Item device : List.of(ModItems.WOODEN_PIPE, ModItems.BONG)) {
                if (!(device instanceof SmokingDeviceItem smokeable)) {
                    continue;
                }
                DeviceType type = smokeable.device();
                for (int dose = 1; dose <= type.maxDose(); dose++) {
                    out.add(packed(strain, device, type, dose));
                }
            }
        }
        return out;
    }

    private static Packing packed(RegistryEntry.Reference<Strain> strain, Item device,
                                  DeviceType type, int dose) {
        ItemStack result = new ItemStack(device);
        result.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, dose));
        result.set(ModComponents.CHARGES, type.bowlSize());

        List<Ingredient> inputs = new ArrayList<>();
        inputs.add(Ingredient.ofItems(device));
        for (int i = 0; i < dose; i++) {
            inputs.add(Ingredient.ofItems(strain.value().buds()));
        }
        Identifier id = Identifier.of(Hempdustry.MOD_ID,
                "packing/" + strain.registryKey().getValue().getPath() + "/"
                        + type.name().toLowerCase(java.util.Locale.ROOT) + "_" + dose);
        return new Packing(id, inputs, result);
    }

    /** One packing permutation, shaped for whatever a viewer calls a shapeless crafting recipe. */
    public record Packing(Identifier id, List<Ingredient> inputs, ItemStack output) {
    }

    /**
     * An id marked as made-up, EMI's way: a leading {@code /} on the path.
     *
     * <p>EMI checks every recipe id against the recipe manager and logs an error for each one it
     * cannot find, because a real recipe that has vanished is a bug worth shouting about. The
     * prefix is how a plugin says "this row was never a recipe" — the cauldron behaviours and the
     * packing permutations both are. Legal in an {@code Identifier}, which allows {@code /} in a
     * path.
     */
    public static Identifier synthetic(Identifier id) {
        return Identifier.of(id.getNamespace(), "/" + id.getPath());
    }

    /**
     * What makes two stacks of the same smokeable <b>different items</b> to a recipe viewer.
     *
     * <h2>Why this is needed at all</h2>
     *
     * A viewer indexes by item, and everything a packed pipe carries lives in components — so
     * without being told otherwise, an empty bong, a Purple Kush bong and a Lemon Haze bong are one
     * entry. JEI says so out loud on startup ("5 duplicate items were found in 'Hempdustry' creative
     * tab"); EMI says nothing and simply collapses them. <b>It also breaks this mod's own plugin</b>:
     * the packing entries differ only by their output's components, so ten recipes would fold into
     * one or two.
     *
     * <p>Keyed on {@code smoke_contents} and <b>nothing else</b>. Charges and damage are deliberately
     * excluded: a half-smoked bowl and a chipped bong are the same <em>kind</em> of thing, and
     * including them would split the item list into a separate entry per durability point.
     *
     * <p>An empty device answers the empty string, which is what both viewers read as "no subtype".
     */
    public static String smokeKey(ItemStack stack) {
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        if (contents.isEmpty()) {
            return "";
        }
        StringBuilder key = new StringBuilder();
        for (SmokeContents.Entry entry : contents.entries()) {
            if (!key.isEmpty()) {
                key.append(',');
            }
            key.append(entry.strain().getKey().map(k -> k.getValue().toString()).orElse("?"))
                    .append('x').append(entry.count());
        }
        return key.toString();
    }

    /** The items {@link #smokeKey} applies to — everything that can be packed. */
    public static List<Item> smokeables() {
        return List.of(ModItems.SPLIFF, ModItems.WOODEN_PIPE, ModItems.BONG);
    }

    /** Ticks as whole seconds, for a note. */
    private static String seconds(int ticks) {
        return String.valueOf(Math.max(1, ticks / 20));
    }

    /** Ticks as whole minutes of real time, which is what a player waiting at the tub experiences. */
    private static String minutes(int ticks) {
        return String.valueOf(Math.max(1, ticks / (20 * 60)));
    }
}

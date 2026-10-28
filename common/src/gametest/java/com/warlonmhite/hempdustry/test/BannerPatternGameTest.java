package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * The hemp leaf is a loom pattern item — checked the way the loom checks it.
 *
 * <p>This exists because the rest of the banner work was verified by looking at the files, and files
 * being present is not the same claim as the game finding them. <b>Three separate things have to
 * line up</b>, and a fourth since 26.1, each of which fails silently on its own:
 *
 * <ol>
 *   <li>the {@code minecraft:provides_banner_patterns} component has to actually be on the item —
 *       it is attached in {@code Item.Properties}, so a refactor that rebuilt the settings would drop
 *       it and nothing would complain;</li>
 *   <li>the tag it names has to exist at
 *       {@code data/hempdustry/tags/banner_pattern/pattern_item/hemp_leaf.json} — a vanilla
 *       registry, so <em>no</em> second {@code hempdustry/} path segment, unlike the mod's own
 *       {@code hempdustry:strain};</li>
 *   <li>the pattern the tag points at has to be loaded from
 *       {@code data/hempdustry/banner_pattern/hemp_leaf.json};</li>
 *   <li>the leaf has to be in {@code #minecraft:loom_patterns}: since 26.1 the loom's pattern slot
 *       asks for the tag as well as the component, so a leaf with only the component goes back to
 *       the hotbar.</li>
 * </ol>
 *
 * <p>Get any one wrong and the loom simply does not offer the pattern. There is no error, no magenta
 * texture and no log line — the button is just not there, which is indistinguishable from not having
 * built the feature.
 *
 * <p>What the loom does is read the component off the stack and resolve that tag against
 * {@code Registries.BANNER_PATTERN} (`LoomMenu`), which is exactly what this does; whether the leaf
 * goes into the pattern slot at all is asked of a loom's own slot, so the test follows vanilla if its
 * rule changes again.
 */
public final class BannerPatternGameTest {

    private static final Identifier HEMP_LEAF_PATTERN = Identifier.fromNamespaceAndPath("hempdustry", "hemp_leaf");

    public static void hempLeafIsALoomPatternItem(GameTestHelper context) {
        ItemStack leaf = new ItemStack(ModItems.HEMP_LEAF);

        LoomMenu loom = new LoomMenu(0, context.makeMockPlayer(GameType.SURVIVAL).getInventory());
        context.assertTrue(loom.getPatternSlot().mayPlace(leaf),
                "a loom's pattern slot refuses hemp_leaf -- since 26.1 it must be in #minecraft:loom_patterns "
                        + "as well as carry the component");

        HolderSet<BannerPattern> providedSet = leaf.get(DataComponents.PROVIDES_BANNER_PATTERNS);
        context.assertTrue(providedSet != null,
                "hemp_leaf has no provides_banner_patterns component, so a loom will never offer it");
        TagKey<BannerPattern> provided = providedSet.unwrapKey().orElse(null);
        context.assertTrue(ModTags.BannerPatterns.HEMP_LEAF_PATTERN_ITEM.equals(provided),
                "hemp_leaf points at " + provided + " rather than the mod's own pattern tag");

        List<Holder<BannerPattern>> patterns = context.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.BANNER_PATTERN)
                .get(provided)
                .map(named -> named.stream().toList())
                .orElse(List.of());

        context.assertTrue(!patterns.isEmpty(),
                "the tag " + provided.location() + " resolved to nothing — either the tag file is missing "
                        + "or it is in the wrong directory");
        context.assertTrue(
                patterns.stream().anyMatch(entry -> entry.unwrapKey()
                        .map(key -> key.identifier().equals(HEMP_LEAF_PATTERN))
                        .orElse(false)),
                "the tag does not contain " + HEMP_LEAF_PATTERN + ", so the loom would offer some "
                        + "other pattern or none");
        context.succeed();
    }
}

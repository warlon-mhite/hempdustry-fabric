package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * The hemp leaf is a loom pattern item — checked the way the loom checks it.
 *
 * <p>This exists because the rest of the banner work was verified by looking at the files, and files
 * being present is not the same claim as the game finding them. <b>Three separate things have to
 * line up</b>, each of which fails silently on its own:
 *
 * <ol>
 *   <li>the item has to actually be a {@code BannerPatternItem} naming the tag — the loom asks
 *       {@code instanceof} and nothing else, so a refactor that made the leaf a plain item again
 *       would drop the pattern and nothing would complain;</li>
 *   <li>the tag it names has to exist at
 *       {@code data/hempdustry/tags/banner_pattern/pattern_item/hemp_leaf.json} — a vanilla
 *       registry, so <em>no</em> second {@code hempdustry/} path segment, unlike the mod's own
 *       {@code hempdustry:strain};</li>
 *   <li>the pattern the tag points at has to be loaded from
 *       {@code data/hempdustry/banner_pattern/hemp_leaf.json}.</li>
 * </ol>
 *
 * <p>Get any one wrong and the loom simply does not offer the pattern. There is no error, no magenta
 * texture and no log line — the button is just not there, which is indistinguishable from not having
 * built the feature.
 *
 * <p>What the loom does is read the tag off the pattern item and resolve it against
 * {@code Registries.BANNER_PATTERN} (`LoomMenu`), which is exactly what this does.
 */
public final class BannerPatternGameTest implements FabricGameTest {

    private static final ResourceLocation HEMP_LEAF_PATTERN = ResourceLocation.fromNamespaceAndPath("hempdustry", "hemp_leaf");

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
    public void hempLeafIsALoomPatternItem(GameTestHelper context) {
        ItemStack leaf = new ItemStack(ModItems.HEMP_LEAF);

        TagKey<BannerPattern> provided = leaf.getItem() instanceof BannerPatternItem pattern ? pattern.getBannerPattern() : null;
        context.assertTrue(provided != null,
                "hemp_leaf is not a banner pattern item, so a loom will never offer it");
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
                        .map(key -> key.location().equals(HEMP_LEAF_PATTERN))
                        .orElse(false)),
                "the tag does not contain " + HEMP_LEAF_PATTERN + ", so the loom would offer some "
                        + "other pattern or none");
        context.succeed();
    }
}

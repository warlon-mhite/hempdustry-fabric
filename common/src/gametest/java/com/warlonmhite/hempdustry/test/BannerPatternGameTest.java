package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBannerPatterns;
import com.warlonmhite.hempdustry.item.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * The hemp leaf is a loom pattern item — checked the way the loom checks it.
 *
 * <p>This exists because the rest of the banner work was verified by looking at the files, and files
 * being present is not the same claim as the game finding them. On the newer lines three files have
 * to line up (the item naming a tag, the tag, the pattern); 1.18.2 has no banner pattern registry
 * and no pattern tags, so a pattern is a value of vanilla's {@code BannerPattern} enum, which Forge
 * extends. <b>Three things still have to line up</b>, each of which fails silently on its own:
 *
 * <ol>
 *   <li>the item has to actually be a {@code BannerPatternItem} naming the mod's pattern — the loom
 *       asks {@code instanceof} and nothing else, so a refactor that made the leaf a plain item again
 *       would drop the pattern and nothing would complain;</li>
 *   <li>the pattern has to be one of the enum's values, which is what Forge's extension adds and
 *       what the loom makes the layer from;</li>
 *   <li>its hash name, which a banner saves for each layer, has to be the newer lines' id
 *       {@code hempdustry:hemp_leaf}, or a banner loses its leaf in a world upgraded to them.</li>
 * </ol>
 *
 * <p>Get any one wrong and the loom simply does not offer the pattern, or a banner drops it. There is
 * no error, no magenta texture and no log line — the button is just not there, which is
 * indistinguishable from not having built the feature.
 *
 * <p>What 1.18.2's loom does is read the pattern straight off the pattern item ({@code LoomMenu}),
 * which is exactly what this does.
 */
public final class BannerPatternGameTest {

    private static final ResourceLocation HEMP_LEAF_PATTERN = new ResourceLocation("hempdustry", "hemp_leaf");

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void hempLeafIsALoomPatternItem(GameTestHelper context) {
        ItemStack leaf = new ItemStack(ModItems.HEMP_LEAF);

        BannerPattern provided = leaf.getItem() instanceof BannerPatternItem pattern ? pattern.getBannerPattern() : null;
        Asserts.assertTrue(context, provided != null,
                "hemp_leaf is not a banner pattern item, so a loom will never offer it");
        Asserts.assertTrue(context, provided == ModBannerPatterns.HEMP_LEAF,
                "hemp_leaf points at " + provided + " rather than the mod's own pattern");

        // The loom selects the item's pattern by its ordinal and makes the layer from
        // values()[ordinal], so the pattern must sit at its own ordinal in values().
        BannerPattern[] values = BannerPattern.values();
        Asserts.assertTrue(context, provided.ordinal() > 0 && provided.ordinal() < values.length && values[provided.ordinal()] == provided,
                "the pattern " + provided + " is not one of the banner patterns, so the loom cannot make it");
        Asserts.assertTrue(context, HEMP_LEAF_PATTERN.toString().equals(provided.getHashname()),
                "the pattern saves as " + provided.getHashname() + ", not as " + HEMP_LEAF_PATTERN
                        + ", so a banner would lose its leaf on the newer lines");
        context.succeed();
    }
}

package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SulfurCubeArchetype;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A sulfur cube that swallows a hemp bale becomes a footbag: no bounce, it stops where it lands, and
 * a hit sends it up more than along. The archetype is data ({@code hempdustry:footbag}), and a cube
 * reads it the way it reads vanilla's twelve: every archetype whose {@code items} tag holds the
 * swallowed block applies ({@code SulfurCube#matchingArchetypes}).
 */
public final class FootbagGameTest {

    public static void aHempBaleMakesAFootbag(GameTestHelper context) {
        ItemStack bale = new ItemStack(ModBlocks.HEMP_BALE);
        context.assertTrue(bale.is(ItemTags.SULFUR_CUBE_SWALLOWABLE),
                "a sulfur cube will not swallow a hemp bale: #hempdustry:sulfur_cube_archetype/footbag has left "
                        + "#minecraft:sulfur_cube_swallowable");

        Registry<SulfurCubeArchetype> archetypes = context.getLevel().registryAccess()
                .lookupOrThrow(Registries.SULFUR_CUBE_ARCHETYPE);
        SulfurCubeArchetype footbag = archetypes.getValue(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "footbag"));
        context.assertTrue(footbag != null, "hempdustry:footbag is not a sulfur cube archetype");

        SulfurCube cube = context.spawn(EntityTypes.SULFUR_CUBE, new BlockPos(3, 2, 3));
        List<SulfurCubeArchetype> matching = cube.matchingArchetypes(bale);
        context.assertTrue(matching.equals(List.of(footbag)),
                "a hemp bale matches " + matching.size() + " archetypes, where it should be the footbag alone");
        context.assertFalse(cube.matchingArchetypes(new ItemStack(Items.HAY_BLOCK)).contains(footbag),
                "hay makes a footbag too, so the archetype's tag holds more than it should");

        // Every vanilla archetype sends a cube along more than up; the footbag is the other way round.
        SulfurCubeArchetype.KnockbackModifiers kick = footbag.knockbackModifiers();
        context.assertTrue(kick.verticalPower() > kick.horizontalPower(),
                "a hit sends the footbag along (" + kick.horizontalPower() + ") more than up (" + kick.verticalPower() + ")");
        context.assertTrue(footbag.buoyant(), "the footbag sinks, though hemp hurd floats");

        double bareBounce = cube.getAttributeValue(Attributes.BOUNCINESS);
        double bareFriction = cube.getAttributeValue(Attributes.FRICTION_MODIFIER);
        cube.setItemSlot(EquipmentSlot.BODY, bale);
        // The cube applies an archetype when it notices its new body item, on its next tick.
        // Knockback resistance is asked for its modifier, not its value: vanilla 26.2 lets the attribute go
        // down to -2, but NeoForge 26.2.0.93 replaces it with an attribute whose floor is still 0, so there
        // the -1 every cube archetype adds reads as 0, vanilla's cubes as much as this one.
        Identifier kickModifier = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "footbag_add_knockback_resistance");
        context.succeedWhen(() -> {
            context.assertTrue(cube.getAttribute(Attributes.KNOCKBACK_RESISTANCE).hasModifier(kickModifier),
                    "the footbag's knockback resistance modifier is not on the cube (holding "
                            + cube.getItemBySlot(EquipmentSlot.BODY) + ")");
            context.assertTrue(cube.getAttributeValue(Attributes.BOUNCINESS) == bareBounce,
                    "a footbag bounces: " + cube.getAttributeValue(Attributes.BOUNCINESS));
            context.assertTrue(cube.getAttributeValue(Attributes.FRICTION_MODIFIER) == bareFriction * 2,
                    "a footbag's friction is " + cube.getAttributeValue(Attributes.FRICTION_MODIFIER)
                            + ", not twice a bare cube's " + bareFriction);
        });
    }
}

package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The addon events reach another mod's listener, on the loader's own bus, with what they say they
 * carry: a hit asks first and can be refused, then lands; an edible tells at the swallow; a batch
 * tells when it is collected. They are what other mods compile against -- Fabric's
 * {@code HempdustryEvents}, NeoForge's classes in {@code com.warlonmhite.hempdustry.api} -- so each
 * jar is held to them, through a listener the loader's {@link TestPlatform} registers.
 */
public final class AddonEventGameTest {
    private static final BlockPos TUB = new BlockPos(1, 1, 1);

    private AddonEventGameTest() {
    }

    public static void theAddonEventsReachAListener(GameTestHelper context) {
        TestPlatform.AddonProbe probe = TestPlatform.INSTANCE.addonProbe();
        ServerPlayer player = Players.survival(context);
        // Everything here happens inside this one call, so no other test's events land in between.
        probe.take();

        ItemStack pipe = packedPipe(context);
        SmokeContents contents = pipe.get(ModComponents.SMOKE_CONTENTS);
        smoke(context, player, pipe);
        List<TestPlatform.Heard> hit = probe.take();
        context.assertValueEqual(List.of("ALLOW_SMOKE", "AFTER_SMOKE"), names(hit), "the events of one hit");
        for (TestPlatform.Heard event : hit) {
            List<Object> payload = event.payload();
            context.assertTrue(payload.get(0) == player && contents.equals(payload.get(1)) && payload.get(2) == pipe,
                    event.event() + " carried " + payload + " rather than the smoker, the pipe's load and the pipe");
        }

        // A listener refuses: nothing after the question, and nothing spent.
        player.removeAllEffects();
        player.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(ModItems.WOODEN_PIPE));
        ItemStack refused = packedPipe(context);
        probe.refuseHits(true);
        try {
            smoke(context, player, refused);
        } finally {
            probe.refuseHits(false);
        }
        context.assertValueEqual(List.of("ALLOW_SMOKE"), names(probe.take()), "the events of a refused hit");
        context.assertValueEqual(4, refused.get(ModComponents.CHARGES), "the charges a refused pipe kept");
        context.assertTrue(player.getActiveEffects().isEmpty(), "a refused hit gave " + player.getActiveEffects());

        EdibleEffects.consume(player, 2, Quality.CLEAN);
        List<TestPlatform.Heard> eaten = probe.take();
        context.assertValueEqual(List.of("AFTER_EAT"), names(eaten), "the events of an edible");
        context.assertValueEqual(List.<Object>of(player, 2, Quality.CLEAN), eaten.get(0).payload(), "what AFTER_EAT carried");

        InfuserBlockEntity infuser = InfuserBreakGameTest.place(context, TUB, InfuserBlockEntity.minTime());
        infuser.onPreviewTaken();
        List<TestPlatform.Heard> collected = probe.take();
        context.assertValueEqual(List.of("AFTER_INFUSE"), names(collected), "the events of a collected batch");
        List<Object> batch = collected.get(0).payload();
        context.assertTrue(batch.get(0) == context.getLevel() && context.absolutePos(TUB).equals(batch.get(1))
                        && batch.get(2) instanceof Integer strength && strength > 0 && batch.get(3) instanceof Quality,
                "AFTER_INFUSE carried " + batch + " rather than the tub's level, its position, a strength and a grade");
        context.succeed();
    }

    private static ItemStack packedPipe(GameTestHelper context) {
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        pipe.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(context.getLevel().registryAccess()
                .lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA), 1));
        pipe.set(ModComponents.CHARGES, 4);
        return pipe;
    }

    private static void smoke(GameTestHelper context, ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(context.getLevel(), player, InteractionHand.MAIN_HAND);
    }

    private static List<String> names(List<TestPlatform.Heard> heard) {
        return heard.stream().map(TestPlatform.Heard::event).toList();
    }
}

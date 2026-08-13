package com.warlonmhite.hempdustry.mixin.client;

import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Reaches {@code ModelPredicateProviderRegistry.ITEM_SPECIFIC} so an item property can return a
 * value above 1.
 *
 * <h2>Why this exists</h2>
 *
 * The only public way to give one item a model property is
 * {@link ModelPredicateProviderRegistry#register(Item, Identifier, net.minecraft.client.item.ClampedModelPredicateProvider)},
 * and its parameter type is the trap. {@code ClampedModelPredicateProvider} overrides {@code call}
 * with:
 *
 * <pre>{@code return MathHelper.clamp(this.unclampedCall(...), 0.0F, 1.0F);}</pre>
 *
 * and {@code ModelOverrideList} evaluates overrides through {@code call}, never
 * {@code unclampedCall}. So a property that returns a small integer is silently pinned to 1: with
 * {@code hempdustry:strain} returning a strain's {@code model_index}, <b>every strain from the
 * second onwards matched the first strain's override</b> and a Lemon Haze spliff rendered with
 * Purple Kush art. It failed visually rather than loudly, which is the worst way for it to fail.
 *
 * <p>The backing map is already typed {@code Map<Item, Map<Identifier, ModelPredicateProvider>>} —
 * the <em>unclamped</em> interface. Only the public {@code register} signature narrows it, and
 * {@code ModelPredicateProviderRegistry.get} reads the map without caring which subtype it finds.
 * So this accessor does not defeat a safety check; it uses the field at the type it already has.
 *
 * <h2>Why an accessor rather than rescaling</h2>
 *
 * The alternative was to keep the clamp and divide the index by a power of two, making the property
 * a fraction. That works, but it moves the cost onto <b>resource packs</b>: a pack adding art for a
 * strain would have to write {@code "hempdustry:strain": 0.01171875} instead of {@code 3}. Since
 * per-strain art is exactly the extension point this property exists to offer, paying five lines
 * here to keep it an integer is the better trade.
 *
 * <h2>Compatibility</h2>
 *
 * An {@code @Accessor} adds a method to the target and injects nothing into any method body, so it
 * cannot conflict with another mod's mixin the way an {@code @Inject} or {@code @Redirect} can.
 * Client-only: {@code ModelPredicateProviderRegistry} does not exist on a dedicated server, so this
 * lives in its own config declared {@code "environment": "client"} in {@code fabric.mod.json}.
 *
 * <p>Note that this whole model-property system carries {@code @Deprecated} upstream — Mojang
 * replaced it with data-driven item models later in the 1.21 line. Anything built on it here will
 * need rewriting on the next major game version, not merely porting.
 */
@Mixin(ModelPredicateProviderRegistry.class)
public interface ModelPredicateProviderRegistryAccessor {

    @Accessor("ITEM_SPECIFIC")
    static Map<Item, Map<Identifier, ModelPredicateProvider>> getItemSpecific() {
        throw new AssertionError("mixin did not apply");
    }
}

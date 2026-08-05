package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.advancement.HarvestHempCriterion;
import com.warlonmhite.hempdustry.advancement.SmokeCriterion;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.ConsumeItemCriterion;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.ItemCriterion;
import net.minecraft.advancement.criterion.ItemDurabilityChangedCriterion;
import net.minecraft.advancement.criterion.PlayerInteractedWithEntityCriterion;
import net.minecraft.advancement.criterion.TameAnimalCriterion;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends FabricAdvancementProvider {

    /**
     * Background for every advancement in the tree. A placeholder — it is a plain block texture
     * rather than a tiled advancement background, which is why the tab tiles oddly (CLAUDE.md §5
     * #3). Kept in one constant so the real art is a one-line change.
     */
    private static final Identifier BACKGROUND =
            Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png");

    public ModAdvancementProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup);
    }

    /**
     * The display block, which is otherwise eight positional arguments repeated twenty-odd times.
     * Title and description are always {@code advancements.hempdustry.<id>.title} / {@code .description},
     * so an advancement can never drift from its lang keys.
     *
     * <p>The three trailing flags are vanilla's {@code showToast} / {@code announceToChat} /
     * {@code hidden}; only the last varies here.
     */
    private static AdvancementDisplay display(ItemConvertible icon, String id, AdvancementFrame frame,
                                              boolean hidden) {
        return new AdvancementDisplay(new ItemStack(icon),
                Text.translatable("advancements.hempdustry." + id + ".title"),
                Text.translatable("advancements.hempdustry." + id + ".description"),
                Optional.of(BACKGROUND), frame, true, true, hidden);
    }

    private static AdvancementDisplay display(ItemConvertible icon, String id, AdvancementFrame frame) {
        return display(icon, id, frame, false);
    }

    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup registryLookup, Consumer<AdvancementEntry> consumer) {

        // Entry point for the whole mod, so it must fire on *any* strain's seed, not just Purple
        // Kush's — hence the #hempdustry:hemp_seeds tag rather than a list of items. (Note
        // Conditions.items(ItemConvertible...) is an AND: one predicate per item, all required —
        // which is what `chill_set` below wants, and the opposite of what this one does.)
        // A future strain joins the tag in ModItemTagProvider and is picked up here for free.
        AdvancementEntry rootAdvancement = Advancement.Builder.create()
                .display(display(ModItems.INDICA_SEEDS, "hempdustry", AdvancementFrame.TASK))
                .criterion("has_hemp_seeds", InventoryChangedCriterion.Conditions.items(
                        ItemPredicate.Builder.create().tag(ModTags.Items.HEMP_SEEDS)))
                .build(consumer, Hempdustry.MOD_ID + ":hempdustry");


        Advancement.Builder.create()
                .display(display(ModItems.INDICA_BUDS, "indica_strain", AdvancementFrame.TASK))
                .criterion("has_indica_buds", InventoryChangedCriterion.Conditions.items(ModItems.INDICA_BUDS))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":indica_strain");

        Advancement.Builder.create()
                .display(display(ModItems.SATIVA_BUDS, "sativa_strain", AdvancementFrame.TASK))
                .criterion("has_sativa_buds", InventoryChangedCriterion.Conditions.items(ModItems.SATIVA_BUDS))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":sativa_strain");

        // ---------------------------------------------------------------------
        // Cultivation
        // ---------------------------------------------------------------------

        // Shearing a growing plant needs NO custom criterion. Vanilla fires
        // minecraft:item_used_on_block from ServerPlayerInteractionManager#interactBlock the moment
        // BlockState#onUseWithItem returns an accepted result — which, for a hemp crop held against
        // shears, happens exactly where Defoliation#tryCut succeeds and nowhere else. The item
        // filter is not optional though: bonemeal reaches the same trigger down the
        // ItemStack#useOnBlock path, so without it this would fire on fertilising too.
        //
        // Parented to the root rather than to a strain, for the same reason first_contact is: an
        // advancement has exactly one parent, and trimming is not a Purple Kush thing.
        AdvancementEntry trimSeason = Advancement.Builder.create()
                .display(display(Items.SHEARS, "trim_season", AdvancementFrame.TASK))
                .criterion("sheared_hemp_crop", ItemCriterion.Conditions.createItemUsedOnBlock(
                        LocationPredicate.Builder.create().block(
                                BlockPredicate.Builder.create().tag(ModTags.Blocks.HEMP_CROPS)),
                        ItemPredicate.Builder.create().tag(ConventionalItemTags.SHEARS_TOOLS)))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":trim_season");

        // TASK, not GOAL. Vanilla spends GOAL six times in the whole game, always on a large
        // deliberate investment (a full beacon, a cured villager); the closest analogues to "do the
        // fiddly thing correctly" — safely_harvest_honey, lightning_rod_with_villager_no_fire — are
        // both TASK. The mod's one GOAL goes to the Decarboxylator below, which really is beacon-tier.
        Advancement.Builder.create()
                .display(display(ModItems.HEMP_LEAF, "perfect_cut", AdvancementFrame.TASK))
                .criterion("harvested_fully_trimmed", HarvestHempCriterion.Conditions.fullyTrimmed())
                .parent(trimSeason)
                .build(consumer, Hempdustry.MOD_ID + ":perfect_cut");

        // Two criteria, ANDed (which is what an advancement does with several criteria by default),
        // because neither says the whole thing on its own. minecraft:tame_animal carries no item —
        // taming a parrot with *wheat* seeds would otherwise grant a hemp advancement — and
        // minecraft:player_interacted_with_entity carries no outcome, only that the interaction was
        // accepted. Together they mean "you have tamed a parrot, and you have fed one hemp seeds",
        // which is the claim the title makes.
        Advancement.Builder.create()
                .display(display(Items.FEATHER, "parrot_tamer", AdvancementFrame.TASK))
                .criterion("tamed_a_parrot", TameAnimalCriterion.Conditions.create(
                        EntityPredicate.Builder.create().type(EntityType.PARROT)))
                .criterion("fed_a_parrot_hemp_seeds", PlayerInteractedWithEntityCriterion.Conditions.create(
                        ItemPredicate.Builder.create().tag(ModTags.Items.HEMP_SEEDS),
                        Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(
                                EntityPredicate.Builder.create().type(EntityType.PARROT)))))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":parrot_tamer");

        // ---------------------------------------------------------------------
        // Smoking
        // ---------------------------------------------------------------------

        // Granted the first time the player takes a hit, regardless of device *or* strain — the
        // SmokeCriterion is fired from Smoking.takeHit, which every smokeable funnels through,
        // rather than being an inventory check.
        //
        // An advancement has exactly one parent, so "a common child of every strain" is not a thing
        // the tree can express. It therefore hangs off the **root**, as a sibling of the strain
        // nodes rather than a child of one of them: parenting it to Purple Kush read as "you must
        // smoke Purple Kush", which is wrong and stays wrong for every strain added later.
        AdvancementEntry firstContact = Advancement.Builder.create()
                .display(display(ModItems.SPLIFF, "first_contact", AdvancementFrame.TASK))
                .criterion("took_a_hit", SmokeCriterion.Conditions.any())
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":first_contact");

        // Hidden easter-egg: take a hit while the in-game clock reads ~4:20 PM. Vanilla day = 24000
        // ticks, tick 0 = 6:00 AM, 1000 ticks/hour → 4:20 PM = 10333. Window ±210 ticks (10123–10543,
        // 420 ticks wide ≈ 21 real seconds) as a nod to the number. Time gate lives in the criterion
        // conditions; hidden = last display flag is true so it stays secret until earned.
        Advancement.Builder.create()
                .display(display(Items.CLOCK, "blaze_it", AdvancementFrame.CHALLENGE, true))
                .criterion("smoked_at_420", SmokeCriterion.Conditions.during(10123L, 10543L))
                .parent(firstContact)
                .build(consumer, Hempdustry.MOD_ID + ":blaze_it");

        // The other end of the same joke, and it needs no bed-tracking to work: ServerWorld sets the
        // time on wake to (t + 24000) - (t + 24000) % 24000, i.e. exactly tick 0. So the first 420
        // ticks of a day genuinely *are* the 420 ticks after getting up. It also catches anyone who
        // stayed up through the night and lit one at sunrise, which is if anything more on-theme.
        Advancement.Builder.create()
                .display(display(Items.RED_BED, "wake_and_bake", AdvancementFrame.CHALLENGE, true))
                .criterion("smoked_at_dawn", SmokeCriterion.Conditions.during(0L, 420L))
                .parent(firstContact)
                .build(consumer, Hempdustry.MOD_ID + ":wake_and_bake");

        // The device ladder. An ItemPredicate rather than a device field on the criterion — the
        // stack handed to the trigger is still the packed one, and since packing is a component on
        // the same item now, matching the item is all this needs. Pipe before bong because that is
        // the cost order (planks vs. glass), the same way story/iron_tools gates upgrade_tools.
        AdvancementEntry pipeDream = Advancement.Builder.create()
                .display(display(ModItems.WOODEN_PIPE, "pipe_dream", AdvancementFrame.TASK))
                .criterion("smoked_a_pipe", SmokeCriterion.Conditions.with(ModItems.WOODEN_PIPE))
                .parent(firstContact)
                .build(consumer, Hempdustry.MOD_ID + ":pipe_dream");

        Advancement.Builder.create()
                .display(display(ModItems.BONG, "bong_voyage", AdvancementFrame.TASK))
                .criterion("smoked_a_bong", SmokeCriterion.Conditions.with(ModItems.BONG))
                .parent(pipeDream)
                .build(consumer, Hempdustry.MOD_ID + ":bong_voyage");

        // Durability is measured in hits, so "ran it into the ground" is literally readable off the
        // vanilla criterion: ItemStack#damage passes the *new damage* value, and the conditions test
        // maxDamage - damage, i.e. hits remaining. {max: 0} is therefore the hit that breaks it.
        //
        // This is deliberately not an anvil-repair advancement: nothing in vanilla observes an
        // anvil (ITEM_DURABILITY_CHANGED is triggered from ItemStack#damage and nowhere else), so
        // that would have cost a second mixin. This is the moment that teaches you devices wear
        // out, which is what sends you to the anvil anyway.
        Advancement.Builder.create()
                .display(display(Items.ANVIL, "burnout", AdvancementFrame.TASK))
                .criterion("smoked_a_device_to_death", ItemDurabilityChangedCriterion.Conditions.create(
                        Optional.of(ItemPredicate.Builder.create()
                                .items(ModItems.WOODEN_PIPE, ModItems.BONG).build()),
                        NumberRange.IntRange.atMost(0)))
                .parent(pipeDream)
                .build(consumer, Hempdustry.MOD_ID + ":burnout");

        // ---------------------------------------------------------------------
        // Industry
        // ---------------------------------------------------------------------

        AdvancementEntry hemprepreneurs = Advancement.Builder.create()
                .display(display(ModItems.HEMP_STEM, "hemprepreneurs", AdvancementFrame.TASK))
                .criterion("has_hemp_stem", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_STEM))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":hemprepreneurs");

        AdvancementEntry hempBuilder = Advancement.Builder.create()
                .display(display(ModItems.HEMP_BRICK, "hemp_builder", AdvancementFrame.TASK))
                .criterion("has_hemp_brick", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_BRICK))
                .parent(hemprepreneurs)
                .build(consumer, Hempdustry.MOD_ID + ":hemp_builder");

        // The mod's only GOAL, and it earns it the way vanilla's create_full_beacon does: the
        // Decarboxylator is 7 hemp bricks *blocks* — 504 hemp stem, roughly 252 indica or 126 sativa
        // mature plants. Everything else in this tree is a TASK by comparison.
        AdvancementEntry activationEnergy = Advancement.Builder.create()
                .display(display(ModBlocks.DECARBOXYLATOR, "activation_energy", AdvancementFrame.GOAL))
                .criterion("has_decarboxylator", InventoryChangedCriterion.Conditions.items(ModBlocks.DECARBOXYLATOR))
                .parent(hempBuilder)
                .build(consumer, Hempdustry.MOD_ID + ":activation_energy");

        // Deliberately the only step between owning the machine and this node. "You decarboxylated
        // something" is an unavoidable consequence of building the oven, not a discovery of its own,
        // so it gets no node; the cauldron rinse is a separate thing the player has to work out.
        AdvancementEntry rinseCycle = Advancement.Builder.create()
                .display(display(ModItems.WASHED_DECARBOXYLATED_HEMP, "rinse_cycle", AdvancementFrame.TASK))
                .criterion("has_washed_hemp", InventoryChangedCriterion.Conditions.items(ModItems.WASHED_DECARBOXYLATED_HEMP))
                .parent(activationEnergy)
                .build(consumer, Hempdustry.MOD_ID + ":rinse_cycle");

        AdvancementEntry cannabutter = Advancement.Builder.create()
                .display(display(ModItems.CANNABUTTER, "butter_late_than_never", AdvancementFrame.TASK))
                .criterion("has_cannabutter", InventoryChangedCriterion.Conditions.items(ModItems.CANNABUTTER))
                .parent(rinseCycle)
                .build(consumer, Hempdustry.MOD_ID + ":butter_late_than_never");

        // The payoff node for the whole four-step chain. Keyed on the #hempdustry:edibles tag so a
        // future edible needs no edit here — and note Space Cake is legitimately absent from that
        // tag, since a slice is eaten by using the *block* and never fires consume_item.
        Advancement.Builder.create()
                .display(display(ModItems.SPACE_COOKIE, "give_it_an_hour", AdvancementFrame.TASK))
                .criterion("ate_an_edible", ConsumeItemCriterion.Conditions.predicate(
                        ItemPredicate.Builder.create().tag(ModTags.Items.EDIBLES)))
                .parent(cannabutter)
                .build(consumer, Hempdustry.MOD_ID + ":give_it_an_hour");

        // Perfect is the one grade gated on *both* dials maxed — every item washed and a full
        // simmer — so it is a genuine mastery feat rather than a milestone, which is what CHALLENGE
        // is for. Matched with a component predicate: ComponentPredicate is exact-value matching, so
        // listing only `quality` leaves `strength` free, and a weak-but-flawless batch still counts.
        Advancement.Builder.create()
                .display(display(ModBlocks.INFUSER, "perfect_batch", AdvancementFrame.CHALLENGE, true))
                .criterion("has_perfect_cannabutter", InventoryChangedCriterion.Conditions.items(
                        ItemPredicate.Builder.create()
                                .items(ModItems.CANNABUTTER)
                                .component(ComponentPredicate.of(ComponentMap.builder()
                                        .add(ModComponents.QUALITY, Quality.PERFECT)
                                        .build()))))
                .parent(cannabutter)
                .build(consumer, Hempdustry.MOD_ID + ":perfect_batch");

        // ---------------------------------------------------------------------
        // Textiles
        // ---------------------------------------------------------------------

        AdvancementEntry greenThreads = Advancement.Builder.create()
                .display(display(ModItems.HEMP_FIBER, "green_threads", AdvancementFrame.TASK))
                .criterion("has_hemp_fiber", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_FIBER))
                .parent(hemprepreneurs)
                .build(consumer, Hempdustry.MOD_ID + ":green_threads");

        Advancement.Builder.create()
                .display(display(ModItems.HEMP_BEANNIE, "chill_set", AdvancementFrame.TASK))
                .criterion("full_hemp_armor", InventoryChangedCriterion.Conditions.items(
                        ModItems.HEMP_BEANNIE,
                        ModItems.HEMP_SHIRT,
                        ModItems.HEMP_HAREM_PANTS,
                        ModItems.FLIP_FLOPS
                ))
                .parent(greenThreads)
                .build(consumer, Hempdustry.MOD_ID + ":chill_set");
    }
}

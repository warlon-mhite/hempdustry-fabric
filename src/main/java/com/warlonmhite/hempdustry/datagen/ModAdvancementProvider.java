package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.advancement.SmokeCriterion;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends FabricAdvancementProvider {

    public ModAdvancementProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup registryLookup, Consumer<AdvancementEntry> consumer) {

        // Entry point for the whole mod, so it must fire on *any* strain's seed, not just Purple
        // Kush's — hence the #hempdustry:hemp_seeds tag rather than a list of items. (Note
        // Conditions.items(ItemConvertible...) is an AND: one predicate per item, all required —
        // which is what `chill_set` below wants, and the opposite of what this one does.)
        // A future strain joins the tag in ModItemTagProvider and is picked up here for free.
        AdvancementEntry rootAdvancement = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.INDICA_SEEDS),
                        Text.translatable("advancements.hempdustry.hempdustry.title"), Text.translatable("advancements.hempdustry.hempdustry.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_seeds", InventoryChangedCriterion.Conditions.items(
                        ItemPredicate.Builder.create().tag(ModTags.Items.HEMP_SEEDS)))
                .build(consumer, Hempdustry.MOD_ID + ":hempdustry");


        Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.INDICA_BUDS),
                        Text.translatable("advancements.hempdustry.indica_strain.title"), Text.translatable("advancements.hempdustry.indica_strain.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_indica_buds", InventoryChangedCriterion.Conditions.items(ModItems.INDICA_BUDS))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":indica_strain");

        Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.SATIVA_BUDS),
                        Text.translatable("advancements.hempdustry.sativa_strain.title"), Text.translatable("advancements.hempdustry.sativa_strain.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_sativa_buds", InventoryChangedCriterion.Conditions.items(ModItems.SATIVA_BUDS))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":sativa_strain");

        // Granted the first time the player takes a hit, regardless of device *or* strain — the
        // SmokeCriterion is fired from Smoking.takeHit, which every smokeable funnels through,
        // rather than being an inventory check.
        //
        // An advancement has exactly one parent, so "a common child of every strain" is not a thing
        // the tree can express. It therefore hangs off the **root**, as a sibling of the strain
        // nodes rather than a child of one of them: parenting it to Purple Kush read as "you must
        // smoke Purple Kush", which is wrong and stays wrong for every strain added later.
        AdvancementEntry firstContact = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.INDICA_SPLIFF),
                        Text.translatable("advancements.hempdustry.first_contact.title"), Text.translatable("advancements.hempdustry.first_contact.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("took_a_hit", SmokeCriterion.Conditions.any())
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":first_contact");

        // Hidden easter-egg: take a hit while the in-game clock reads ~4:20 PM. Vanilla day = 24000
        // ticks, tick 0 = 6:00 AM, 1000 ticks/hour → 4:20 PM = 10333. Window ±210 ticks (10123–10543,
        // 420 ticks wide ≈ 21 real seconds) as a nod to the number. Time gate lives in the criterion
        // conditions; hidden = last display flag is true so it stays secret until earned.
        Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(Items.CLOCK),
                        Text.translatable("advancements.hempdustry.blaze_it.title"), Text.translatable("advancements.hempdustry.blaze_it.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.CHALLENGE,
                        true,true,true))
                .criterion("smoked_at_420", SmokeCriterion.Conditions.during(10123L, 10543L))
                .parent(firstContact)
                .build(consumer, Hempdustry.MOD_ID + ":blaze_it");


        AdvancementEntry hemprepreneurs = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_STEM),
                        Text.translatable("advancements.hempdustry.hemprepreneurs.title"), Text.translatable("advancements.hempdustry.hemprepreneurs.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_stem", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_STEM))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":hemprepreneurs");

        AdvancementEntry hempBuilder = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_BRICK),
                        Text.translatable("advancements.hempdustry.hemp_builder.title"), Text.translatable("advancements.hempdustry.hemp_builder.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_brick", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_BRICK))
                .parent(hemprepreneurs)
                .build(consumer, Hempdustry.MOD_ID + ":hemp_builder");

        AdvancementEntry greenThreads = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_FIBER),
                        Text.translatable("advancements.hempdustry.green_threads.title"), Text.translatable("advancements.hempdustry.green_threads.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_fiber", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_FIBER))
                .parent(hemprepreneurs)
                .build(consumer, Hempdustry.MOD_ID + ":green_threads");

        AdvancementEntry chillSet = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_BEANNIE),
                        Text.translatable("advancements.hempdustry.chill_set.title"), Text.translatable("advancements.hempdustry.chill_set.description"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
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


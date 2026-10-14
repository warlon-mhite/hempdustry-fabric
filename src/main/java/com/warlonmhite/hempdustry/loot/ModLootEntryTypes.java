package com.warlonmhite.hempdustry.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ModLootEntryTypes {

    public static final LootPoolEntryType SPOILING = Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE,
            Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "spoiling"), new LootPoolEntryType(SpoilingEntry.CODEC));

    public static void registerLootEntryTypes() {
        Hempdustry.LOGGER.info("Registering Loot Entry Types for " + Hempdustry.MOD_ID);
    }

    /**
     * Some of what another entry drops comes out as a different, worse item instead — a ripe
     * plant's buds turning to schwag. <b>A conversion, never an extra drop</b>: what comes out adds
     * up to exactly what the wrapped entry made, so a spoiled bud is a bud lost.
     *
     * <pre>
     * { "type": "hempdustry:spoiling",
     *   "child": { ...any entry, with its own conditions and functions... },
     *   "into": "hempdustry:schwag",
     *   "enchantment": "minecraft:fortune",
     *   "one_item_chances":  [0.02, 0, 0, 0],
     *   "each_item_chances": [0.25, 0.2, 0.15, 0.1] }
     * </pre>
     *
     * <p>Two odds, both per level of {@code enchantment} on the tool, read exactly as vanilla's
     * {@code table_bonus} condition reads its {@code chances} (the last entry covers every higher
     * level; an absent enchantment or tool is level 0; an empty list is never):
     * <ul>
     *   <li>{@code one_item_chances} — rolled once: one item converts. The poisonous potato's rule,
     *       a small chance per harvest of one bad item.</li>
     *   <li>{@code each_item_chances} — rolled for every item that is left, each on its own.</li>
     * </ul>
     *
     * <p>The item is converted <b>after</b> the child's functions, so Fortune's bonus buds can spoil
     * like the rest. Its own conditions and the child's both decide whether it fires. <b>Inside an
     * {@code alternatives} chain, put the conditions on this entry, not on the child</b>: vanilla's
     * validation reads only the conditions of the chain's own children, so a spoiling entry with none
     * of its own reports every later alternative as "Unreachable entry!" at each load — the drops
     * are right either way, and the log fills with warnings.
     *
     * <p><b>It takes exactly one draw from the loot random each time it fires, whatever it decides</b>,
     * and rolls on a random seeded from that. So everything rolled after it in the table comes out the
     * same whether or not anything spoiled, and two plants harvested on the same seed differ only in
     * what they are — the property every matched-seed harvest test leans on.
     */
    public static final class SpoilingEntry extends LootPoolEntryContainer {
        // Above CODEC: a record codec is built eagerly, and a field declared below it is still null.
        private static final Codec<List<Float>> CHANCES = Codec.floatRange(0.0F, 1.0F).listOf();

        public static final MapCodec<SpoilingEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootPoolEntries.CODEC.fieldOf("child").forGetter(entry -> entry.child),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("into").forGetter(entry -> entry.into),
                Enchantment.CODEC.optionalFieldOf("enchantment").forGetter(entry -> entry.enchantment),
                CHANCES.optionalFieldOf("one_item_chances", List.of()).forGetter(entry -> entry.oneItemChances),
                CHANCES.optionalFieldOf("each_item_chances", List.of()).forGetter(entry -> entry.eachItemChances)
        ).and(commonFields(instance).t1()).apply(instance, SpoilingEntry::new));

        private final LootPoolEntryContainer child;
        private final Item into;
        private final Optional<Holder<Enchantment>> enchantment;
        private final List<Float> oneItemChances;
        private final List<Float> eachItemChances;

        private SpoilingEntry(LootPoolEntryContainer child, Item into, Optional<Holder<Enchantment>> enchantment,
                              List<Float> oneItemChances, List<Float> eachItemChances,
                              List<LootItemCondition> conditions) {
            super(conditions);
            this.child = child;
            this.into = into;
            this.enchantment = enchantment;
            this.oneItemChances = List.copyOf(oneItemChances);
            this.eachItemChances = List.copyOf(eachItemChances);
        }

        @Override
        public LootPoolEntryType getType() {
            return SPOILING;
        }

        @Override
        public void validate(ValidationContext reporter) {
            super.validate(reporter);
            child.validate(reporter.forChild(new ProblemReporter.FieldPathElement("child")));
        }

        @Override
        public boolean expand(LootContext context, Consumer<LootPoolEntry> choiceConsumer) {
            if (!canRun(context)) {
                return false;
            }
            return child.expand(context, choice -> choiceConsumer.accept(new LootPoolEntry() {
                @Override
                public int getWeight(float luck) {
                    return choice.getWeight(luck);
                }

                @Override
                public void createItemStack(Consumer<ItemStack> lootConsumer, LootContext lootContext) {
                    RandomSource random = RandomSource.create(lootContext.getRandom().nextLong());
                    int level = level(lootContext);
                    choice.createItemStack(stack -> spoil(stack, random, level, lootConsumer), lootContext);
                }
            }));
        }

        private void spoil(ItemStack stack, RandomSource random, int level, Consumer<ItemStack> lootConsumer) {
            int spoiled = 0;
            if (stack.getCount() > 0 && random.nextFloat() < chance(oneItemChances, level)) {
                spoiled = 1;
            }
            float each = chance(eachItemChances, level);
            if (each > 0.0F) {
                for (int i = spoiled; i < stack.getCount(); i++) {
                    if (random.nextFloat() < each) {
                        spoiled++;
                    }
                }
            }
            if (spoiled > 0) {
                lootConsumer.accept(new ItemStack(into, spoiled));
                stack.shrink(spoiled);
            }
            if (!stack.isEmpty()) {
                lootConsumer.accept(stack);
            }
        }

        private int level(LootContext context) {
            ItemStack tool = context.getOptionalParameter(LootContextParams.TOOL);
            return enchantment.isPresent() && tool != null ? EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), tool) : 0;
        }

        private static float chance(List<Float> chances, int level) {
            return chances.isEmpty() ? 0.0F : chances.get(Math.min(level, chances.size() - 1));
        }

        public static com.warlonmhite.hempdustry.loot.ModLootEntryTypes.SpoilingEntry.Builder builder(LootPoolEntryContainer.Builder<?> child, Item into) {
            return new com.warlonmhite.hempdustry.loot.ModLootEntryTypes.SpoilingEntry.Builder(child, into);
        }

        public static final class Builder extends LootPoolEntryContainer.Builder<com.warlonmhite.hempdustry.loot.ModLootEntryTypes.SpoilingEntry.Builder> {
            private final LootPoolEntryContainer.Builder<?> child;
            private final Item into;
            private Optional<Holder<Enchantment>> enchantment = Optional.empty();
            private List<Float> oneItemChances = List.of();
            private List<Float> eachItemChances = List.of();

            private Builder(LootPoolEntryContainer.Builder<?> child, Item into) {
                this.child = child;
                this.into = into;
            }

            public com.warlonmhite.hempdustry.loot.ModLootEntryTypes.SpoilingEntry.Builder enchantment(Holder<Enchantment> enchantment) {
                this.enchantment = Optional.of(enchantment);
                return this;
            }

            public com.warlonmhite.hempdustry.loot.ModLootEntryTypes.SpoilingEntry.Builder oneItem(Float... chances) {
                this.oneItemChances = List.of(chances);
                return this;
            }

            public com.warlonmhite.hempdustry.loot.ModLootEntryTypes.SpoilingEntry.Builder eachItem(Float... chances) {
                this.eachItemChances = List.of(chances);
                return this;
            }

            @Override
            protected com.warlonmhite.hempdustry.loot.ModLootEntryTypes.SpoilingEntry.Builder getThis() {
                return this;
            }

            @Override
            public LootPoolEntryContainer build() {
                return new SpoilingEntry(child.build(), into, enchantment, oneItemChances, eachItemChances,
                        getConditions());
            }
        }
    }
}

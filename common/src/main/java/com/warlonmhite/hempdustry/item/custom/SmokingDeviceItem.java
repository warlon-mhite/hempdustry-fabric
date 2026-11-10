package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.ModStrains;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * A smoking device — wooden pipe, bong or vaporizer — in <em>either</em> state. Empty and packed are the same
 * item; what is loaded lives in the {@code hempdustry:smoke_contents} component, the way a potion
 * carries {@code potion_contents}.
 *
 * <p>That collapses what used to be one registered item per device × strain, and it makes strain
 * mixing representable at all — a separate item per combination is combinatorially hopeless. Three
 * things fall out of the unification for free:
 *
 * <ul>
 *   <li><b>Durability and enchantments never move.</b> Packing and emptying are a component being
 *       set and cleared on the same stack, so the old {@code transmuteCopy} dance between
 *       two items — and the class of bug that came with it — is gone.</li>
 *   <li><b>Repair works in either state.</b> Deliberate, and vanilla-consistent: a loaded crossbow
 *       carrying {@code charged_projectiles} is anvil-repairable too.</li>
 *   <li><b>The cooldown is shared across strains</b>, because {@code ItemCooldowns} is keyed
 *       by a cooldown group, which is the item's id when it carries no {@code use_cooldown}
 *       component. This closes an exploit — carrying one bong per strain used to give
 *       independent cooldowns and double the smoke rate. {@link Smoking#startCooldown} now widens
 *       it further, to every smokeable at once, so a spliff in the other hand is no way round it
 *       either.</li>
 * </ul>
 */
public class SmokingDeviceItem extends Item {
    private final DeviceType device;
    private final @Nullable Block placed;

    /** @param placed what an empty one stands as when put down, or {@code null} if it does not. */
    public SmokingDeviceItem(DeviceType device, @Nullable Block placed, Properties settings) {
        super(settings);
        this.device = device;
        this.placed = placed;
        Smoking.registerSmokeable(this);
        if (placed != null) {
            // What BlockItem#appendBlocks does at registration. Block#asItem reads this map, and it
            // is how pick-block and the block's loot table find their way back to the device.
            Item.BY_BLOCK.put(placed, this);
        }
    }

    public DeviceType device() {
        return device;
    }

    /** What is currently loaded, or {@link SmokeContents#EMPTY} for an unpacked device. */
    public static SmokeContents contentsOf(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
    }

    // Enchantability and material repair are data components since 1.21.5, not overrides — both
    // are set on the settings in ModItems#registerDevice. Repair stays allowed packed or empty,
    // because durability is the same component either way.

    @Override
    public Component getName(ItemStack stack) {
        SmokeContents contents = contentsOf(stack);
        // The packed format is the device's, not this item's: a packed Red Bong is "Purple Kush
        // Bong", because the glass is on the model and the name has room for one thing only.
        return contents.isEmpty()
                ? super.getName(stack)
                : SmokeContents.packedName("item." + Hempdustry.MOD_ID + "." + device.baseName() + ".packed", contents);
    }

    /**
     * Says how to set a packed bong down, which nothing else in the game does: a plain right-click
     * smokes it, so it places only while sneaking ({@link #useOn}). An empty one places on a
     * plain click and needs no line.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
                              Consumer<Component> textConsumer, TooltipFlag type) {
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
        if (placed != null && !contentsOf(stack).isEmpty()) {
            textConsumer.accept(Component.translatable("hempdustry.bong.set_down").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        Smoking.expire(stack, world);
    }

    /**
     * Stands a device on the block clicked, if it has a block to stand as — the bong does, the pipe
     * and vaporizer do not. An empty one places on a plain click. A packed one places only when
     * sneaking: right-click is how it is smoked, and sneaking is already vanilla's "use the item on
     * this block"; otherwise this passes and {@link #use} starts the draw. Broken, it comes back
     * packed — the load is two more components the block entity keeps.
     *
     * <p>{@code BlockItem#place} cut down to what a decorative block needs, since this class is
     * every device and cannot be a {@code BlockItem}. What keeps it short is
     * {@code applyComponentsFromItemStack}: the block entity holds on to durability, enchantments and a name
     * without a line of ours, and hands them back when broken ({@code BongBlockEntity}).
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (placed == null || (!contentsOf(stack).isEmpty() && !context.isSecondaryUseActive())) {
            return InteractionResult.PASS;
        }
        BlockPlaceContext placement = new BlockPlaceContext(context);
        Level world = placement.getLevel();
        BlockPos pos = placement.getClickedPos();
        Player player = placement.getPlayer();
        BlockState state = placement.canPlace() ? placed.getStateForPlacement(placement) : null;
        if (state == null || !state.canSurvive(world, pos)
                || !world.isUnobstructed(state, pos, player == null ? CollisionContext.empty() : CollisionContext.of(player))
                || !world.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE)) {
            return InteractionResult.FAIL;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.applyComponentsFromItemStack(stack);
            blockEntity.setChanged();
        }
        placed.setPlacedBy(world, pos, state, player, stack);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        }
        SoundType sounds = state.getSoundType();
        world.playSound(player, pos, sounds.getPlaceSound(), SoundSource.BLOCKS,
                (sounds.getVolume() + 1f) / 2f, sounds.getPitch() * 0.8f);
        world.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SmokeContents contents = contentsOf(stack);
        if (contents.isEmpty() || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (device.drawTicks() > 0) {
            // Hold to rip: the hit lands in finishUsingItem once the draw is complete, and letting go
            // first spends nothing -- not even the bubbling, which onUseTick starts on each client.
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (!world.isClientSide()) {
            // A veto costs the player nothing: no effects, no charge spent, no cooldown. Checked
            // here rather than beside the emptiness test because it is the expensive one of the
            // three and the only one another mod can answer.
            if (!Smoking.allowed(player, stack, contents)) {
                return InteractionResult.PASS;
            }
            hit(world, player, stack, contents, hand, device.soundDelayTicks());
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The end of a draw. The veto is asked here and not when the draw began, so a listener still
     * hears exactly one question per hit — the frozen {@code ALLOW_SMOKE} contract — and a hit
     * refused at the last moment costs nothing, only the breath.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        SmokeContents contents = contentsOf(stack);
        if (!world.isClientSide() && user instanceof Player player && !contents.isEmpty()
                && Smoking.allowed(player, stack, contents)) {
            // The bubbling began with the draw, drawTicks ago, and the inhale's delay counts from there.
            hit(world, player, stack, contents, user.getUsedItemHand(),
                    Math.max(0, device.soundDelayTicks() - device.drawTicks()));
        }
        return stack;
    }

    // Overridden the way InstrumentItem overrides them: ItemStack asks the item, and Item's own
    // answer comes from a consumable component this device does not have. 0 and NONE for every
    // device that hits on the click, which is what they were before.
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return device.drawTicks();
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return device.drawTicks() > 0 ? ItemUseAnimation.TOOT_HORN : ItemUseAnimation.NONE;
    }

    /**
     * Starts a bong's bubbling on this client — set by {@code HempdustryClient}, a no-op on a
     * dedicated server. Client-side because the bubbling has to stop the moment the drawer lets
     * go, and only a sound instance that belongs to the client can stop itself (vanilla's elytra
     * wind is the same shape). A sound the server sent would bubble on for three seconds after a
     * draw that never became a hit.
     */
    public static Consumer<LivingEntity> drawSound = user -> {};

    private static final int WISP_EVERY_TICKS = 5;

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        int drawn = getUseDuration(stack, user) - remainingUseTicks;
        if (world.isClientSide()) {
            // The first tick of a draw, on every client that can see it: a remote player's draw
            // ticks here too, from the use flag the server syncs.
            if (drawn == 0 && device == DeviceType.BONG) {
                drawSound.accept(user);
            }
        } else if (drawn % WISP_EVERY_TICKS == 0 && world instanceof ServerLevel serverWorld) {
            wisp(serverWorld, user);
        }
    }

    /**
     * A wisp off the lit bowl, about where the hand holds it — the draw you can see from across a
     * room. Placed off the player's yaw rather than their look, so it stays at the bowl whether
     * they are looking at their feet or the sky.
     */
    private static void wisp(ServerLevel world, LivingEntity user) {
        boolean rightHand = (user.getUsedItemHand() == InteractionHand.MAIN_HAND) == (user.getMainArm() == HumanoidArm.RIGHT);
        Vec3 forward = Vec3.directionFromRotation(0, user.getYRot());
        Vec3 side = Vec3.directionFromRotation(0, user.getYRot() + (rightHand ? 90 : -90));
        Vec3 bowl = user.getEyePosition().add(forward.scale(0.4)).add(side.scale(0.3)).add(0, -0.5, 0);
        world.sendParticles(ParticleTypes.SMOKE, bowl.x, bowl.y, bowl.z, 1, 0.02, 0.02, 0.02, 0.005);
    }

    /** One hit, spent: effects, cooldown, a charge and a point of durability, and the bowl's end. */
    private void hit(Level world, Player player, ItemStack stack, SmokeContents contents,
                     InteractionHand hand, int soundDelayTicks) {
        DeviceStats stats = DeviceStats.of(world.registryAccess(), device);
        GreenOut greenOut = GreenOut.of(world.registryAccess());
        boolean lockedOut = Smoking.takeHit(world, player, stack, contents, stats.durationTicks(),
                stats.coughOneIn(), stats.nauseaOneIn(),
                Smoking.greenOutChanceOneIn(greenOut, contents.dose(), stats.greenOutFactor()),
                device.exhaleParticle(), soundDelayTicks);
        Smoking.startCooldown(player, stack,
                EffectPolicy.cooldown(lockedOut ? greenOut.lockoutTicks() : stats.cooldownTicks()));

        if (player.getAbilities().instabuild) {
            return;
        }
        int remaining = stack.getOrDefault(ModComponents.CHARGES, 0) - 1;
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        // A device never breaks with hits left in its bowl: the hit that would take its last point
        // leaves it at one, and the hit that empties the bowl breaks it. Every device's maxDamage
        // is a whole number of bowls new, but repairs, Unbreaking, Mending and anvil-combining put
        // durability out of step, and breaking early threw the rest of the bowl away.
        boolean lastPoint = stack.isDamageableItem() && stack.getDamageValue() + 1 >= stack.getMaxDamage();
        if (remaining <= 0 || !lastPoint) {
            stack.hurtAndBreak(1, player, slot);
        }
        if (remaining <= 0) {
            // The bowl is spent whether or not the device survived the hit, and both halves
            // of that matter. The shot that breaks a device is always the last shot of a bowl
            // (above) — for the vaporizer, new, that is hit 32 of 32, once in the life of every
            // single one. Yielding inside the isEmpty() guard would have silently eaten that
            // last AVB every time.
            yieldSpent(player, contents);
            if (!stack.isEmpty()) {
                // Clearing the two components is the whole "revert to empty" — durability
                // and enchantments are already where they need to be.
                stack.remove(ModComponents.SMOKE_CONTENTS);
                stack.remove(ModComponents.CHARGES);
            }
        } else if (!stack.isEmpty()) {
            stack.set(ModComponents.CHARGES, remaining);
        }
    }

    /**
     * Hands back what the finished bowl left behind — <b>AVB</b>, "already vaped bud", as
     * {@code scorched_hemp}. A no-op for every device that burns its load; see
     * {@link DeviceType#builtInStats()} for why only the vaporizer has any and why it is 1.
     *
     * <p><b>Only a bowl of plant matter leaves anything</b>, and every entry has to be one. What
     * else a vaporizer can hold is resin — hashish, charas, filtered hashish, all dose 1 — or
     * scorched hemp itself. Resin handing back hemp was a real leak: the hash family's one rule is
     * that it never reaches cannabutter, and before this guard a vaporizer walked it there.
     * Scorched hemp handing back more scorched hemp would be an endless bowl. The mod-wide
     * predicate for plant matter answers both, and anything hash-shaped a datapack adds. Schwag
     * passes it: a spoiled bud still leaves a spent one.
     *
     * <p>{@code addItem} puts it in the inventory and drops the remainder at the player's feet
     * if there is no room, which is vanilla's own behaviour for a bucket emptying or a bundle
     * spilling — the yield can never be lost to a full hotbar.
     */
    private void yieldSpent(Player player, SmokeContents contents) {
        int yield = DeviceStats.bowl(player.registryAccess(), device).spentYield();
        if (yield > 0 && contents.entries().stream()
                .allMatch(entry -> ModStrains.isPlantMatter(entry.strain()))) {
            player.addItem(new ItemStack(ModItems.SCORCHED_HEMP, yield));
        }
    }
}

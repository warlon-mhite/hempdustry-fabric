package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * A smoking device — wooden pipe, bong or vaporizer — in <em>either</em> state. Empty and packed are the same
 * item; what is loaded lives in the {@code hempdustry:smoke_contents} component, the way a potion
 * carries {@code potion_contents}. See CLAUDE.md §5b D10.
 *
 * <p>That collapses what used to be one registered item per device × strain, and it makes strain
 * mixing representable at all — a separate item per combination is combinatorially hopeless. Three
 * things fall out of the unification for free:
 *
 * <ul>
 *   <li><b>Durability and enchantments never move.</b> Packing and emptying are a component being
 *       set and cleared on the same stack, so the old {@code copyComponentsToNewStack} dance between
 *       two items — and the class of bug that came with it — is gone.</li>
 *   <li><b>Repair works in either state.</b> Deliberate, and vanilla-consistent: a loaded crossbow
 *       carrying {@code charged_projectiles} is anvil-repairable too.</li>
 *   <li><b>The cooldown is shared across strains</b>, because {@code ItemCooldownManager} is keyed
 *       by {@code Item}. This closes an exploit — carrying one bong per strain used to give
 *       independent cooldowns and double the smoke rate. {@link Smoking#startCooldown} now widens
 *       it further, to every smokeable at once, so a spliff in the other hand is no way round it
 *       either.</li>
 * </ul>
 */
public class SmokingDeviceItem extends Item {
    private final DeviceType device;
    private final @Nullable Block placed;

    /** @param placed what an empty one stands as when put down, or {@code null} if it does not. */
    public SmokingDeviceItem(DeviceType device, @Nullable Block placed, Settings settings) {
        super(settings);
        this.device = device;
        this.placed = placed;
        Smoking.registerSmokeable(this);
        if (placed != null) {
            // What BlockItem#appendBlocks does at registration. Block#asItem reads this map, and it
            // is how pick-block and the block's loot table find their way back to the device.
            Item.BLOCK_ITEMS.put(placed, this);
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
    public Text getName(ItemStack stack) {
        SmokeContents contents = contentsOf(stack);
        // The packed format is the device's, not this item's: a packed Red Bong is "Purple Kush
        // Bong", because the glass is on the model and the name has room for one thing only.
        return contents.isEmpty()
                ? super.getName(stack)
                : SmokeContents.packedName("item." + Hempdustry.MOD_ID + "." + device.baseName() + ".packed", contents);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
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
     * {@code readComponents}: the block entity holds on to durability, enchantments and a name
     * without a line of ours, and hands them back when broken ({@code BongBlockEntity}).
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        if (placed == null || (!contentsOf(stack).isEmpty() && !context.shouldCancelInteraction())) {
            return ActionResult.PASS;
        }
        ItemPlacementContext placement = new ItemPlacementContext(context);
        World world = placement.getWorld();
        BlockPos pos = placement.getBlockPos();
        PlayerEntity player = placement.getPlayer();
        BlockState state = placement.canPlace() ? placed.getPlacementState(placement) : null;
        if (state == null || !state.canPlaceAt(world, pos)
                || !world.canPlace(state, pos, player == null ? ShapeContext.absent() : ShapeContext.of(player))
                || !world.setBlockState(pos, state, Block.NOTIFY_ALL_AND_REDRAW)) {
            return ActionResult.FAIL;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            blockEntity.readComponents(stack);
            blockEntity.markDirty();
        }
        placed.onPlaced(world, pos, state, player, stack);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Criteria.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        }
        BlockSoundGroup sounds = state.getSoundGroup();
        world.playSound(player, pos, sounds.getPlaceSound(), SoundCategory.BLOCKS,
                (sounds.getVolume() + 1f) / 2f, sounds.getPitch() * 0.8f);
        world.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(player, state));
        stack.decrementUnlessCreative(1, player);
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        SmokeContents contents = contentsOf(stack);
        if (contents.isEmpty() || player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.PASS;
        }
        if (device.drawTicks() > 0) {
            // Hold to rip: the hit lands in finishUsing once the draw is complete, and letting go
            // first spends nothing -- not even the bubbling, which usageTick starts on each client.
            player.setCurrentHand(hand);
            return ActionResult.CONSUME;
        }
        if (!world.isClient()) {
            // A veto costs the player nothing: no effects, no charge spent, no cooldown. Checked
            // here rather than beside the emptiness test because it is the expensive one of the
            // three and the only one another mod can answer.
            if (!Smoking.allowed(player, stack, contents)) {
                return ActionResult.PASS;
            }
            hit(world, player, stack, contents, hand, device.soundDelayTicks());
        }
        return ActionResult.SUCCESS;
    }

    /**
     * The end of a draw. The veto is asked here and not when the draw began, so a listener still
     * hears exactly one question per hit — the frozen {@code ALLOW_SMOKE} contract — and a hit
     * refused at the last moment costs nothing, only the breath.
     */
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        SmokeContents contents = contentsOf(stack);
        if (!world.isClient() && user instanceof PlayerEntity player && !contents.isEmpty()
                && Smoking.allowed(player, stack, contents)) {
            // The bubbling began with the draw, drawTicks ago, and the inhale's delay counts from there.
            hit(world, player, stack, contents, user.getActiveHand(),
                    Math.max(0, device.soundDelayTicks() - device.drawTicks()));
        }
        return stack;
    }

    // Overridden the way GoatHornItem overrides them: ItemStack asks the item, and Item's own
    // answer comes from a consumable component this device does not have. 0 and NONE for every
    // device that hits on the click, which is what they were before.
    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return device.drawTicks();
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return device.drawTicks() > 0 ? UseAction.TOOT_HORN : UseAction.NONE;
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
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        int drawn = getMaxUseTime(stack, user) - remainingUseTicks;
        if (world.isClient()) {
            // The first tick of a draw, on every client that can see it: a remote player's draw
            // ticks here too, from the use flag the server syncs.
            if (drawn == 0 && device == DeviceType.BONG) {
                drawSound.accept(user);
            }
        } else if (drawn % WISP_EVERY_TICKS == 0 && world instanceof ServerWorld serverWorld) {
            wisp(serverWorld, user);
        }
    }

    /**
     * A wisp off the lit bowl, about where the hand holds it — the draw you can see from across a
     * room. Placed off the player's yaw rather than their look, so it stays at the bowl whether
     * they are looking at their feet or the sky.
     */
    private static void wisp(ServerWorld world, LivingEntity user) {
        boolean rightHand = (user.getActiveHand() == Hand.MAIN_HAND) == (user.getMainArm() == Arm.RIGHT);
        Vec3d forward = Vec3d.fromPolar(0, user.getYaw());
        Vec3d side = Vec3d.fromPolar(0, user.getYaw() + (rightHand ? 90 : -90));
        Vec3d bowl = user.getEyePos().add(forward.multiply(0.4)).add(side.multiply(0.3)).add(0, -0.5, 0);
        world.spawnParticles(ParticleTypes.SMOKE, bowl.x, bowl.y, bowl.z, 1, 0.02, 0.02, 0.02, 0.005);
    }

    /** One hit, spent: effects, cooldown, a charge and a point of durability, and the bowl's end. */
    private void hit(World world, PlayerEntity player, ItemStack stack, SmokeContents contents,
                     Hand hand, int soundDelayTicks) {
        Smoking.takeHit(world, player, stack, contents, device.durationTicks(),
                device.coughChanceOneIn(), device.nauseaChanceOneIn(),
                Smoking.greenOutChanceOneIn(contents.dose(), false), device.exhaleParticle(),
                soundDelayTicks);
        Smoking.startCooldown(player, stack, EffectPolicy.cooldown(device.cooldownTicks()));

        if (player.getAbilities().creativeMode) {
            return;
        }
        int remaining = stack.getOrDefault(ModComponents.CHARGES, 0) - 1;
        EquipmentSlot slot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.damage(1, player, slot);
        if (remaining <= 0) {
            // The bowl is spent whether or not the device survived the hit, and both halves
            // of that matter. Every device's maxDamage is a whole number of bowls, so the
            // shot that breaks one is ALWAYS the last shot of a bowl — for the vaporizer
            // that is hit 32 of 32, once in the life of every single one. Yielding inside
            // the isEmpty() guard would have silently eaten that last AVB every time.
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
     * {@link DeviceType#spentYield()} for why only the vaporizer has any and why it is 1.
     *
     * <p><b>Only a bowl of plant matter leaves anything</b>, and every entry has to be one. What
     * else a vaporizer can hold is resin — hashish, charas, filtered hashish, all dose 1 — or
     * scorched hemp itself. Resin handing back hemp was a real leak: the hash family's one rule is
     * that it never reaches cannabutter, and before this guard a vaporizer walked it there.
     * Scorched hemp handing back more scorched hemp would be an endless bowl. The mod-wide
     * predicate for "this grew on a plant" answers both, and anything hash-shaped a datapack adds.
     *
     * <p>{@code giveItemStack} puts it in the inventory and drops the remainder at the player's feet
     * if there is no room, which is vanilla's own behaviour for a bucket emptying or a bundle
     * spilling — the yield can never be lost to a full hotbar.
     */
    private void yieldSpent(PlayerEntity player, SmokeContents contents) {
        int yield = device.spentYield();
        if (yield > 0 && contents.entries().stream()
                .allMatch(entry -> entry.strain().value().flower().isPresent())) {
            player.giveItemStack(new ItemStack(ModItems.SCORCHED_HEMP, yield));
        }
    }
}

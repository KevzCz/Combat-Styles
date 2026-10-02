package net.pixeldreamstudios.combat_styles.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentUse;
import net.pixeldreamstudios.combat_styles.equipment.UseSpec;
import net.pixeldreamstudios.combat_styles.mixin.LivingEntityTurnInvoker;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class UseAnimationBinding {

    private static final int LAYER_PRIORITY = 8500;
    private static final int PITCH_INDEX = 0;
    private static final int FADE_TICKS = 4;
    private static final int RELEASE_FADE_TICKS = 3;
    private static final int FIRST_TICK = 0;
    private static final float NO_ANIM_STEP = 0.0F;

    private record Bound(ModifierLayer<KeyframeAnimationPlayer> layer, String playing,
                         boolean releasing, AdjustmentModifier pitch) {
    }

    private static final Map<UUID, Bound> BOUND = new WeakHashMap<>();

    private UseAnimationBinding() {
    }

    public static void tick(AbstractClientPlayer player) {
        if (player == null || !CombatStyles.isPlayerAnimatorLoaded()) {
            return;
        }
        ItemStack held = player.getUseItem();
        UseSpec spec = player.isUsingItem() ? EquipmentUse.specOf(held) : UseSpec.NONE;
        String wanted = wantedAnimation(player, spec);
        if (wanted != null) {
            faceLook(player);
        }
        Bound bound = BOUND.get(player.getUUID());
        if (bound != null && bound.releasing()) {
            if (stillPlaying(bound)) {
                return;
            }
            play(player, wanted, false);
            return;
        }
        if (bound != null && sameAnimation(bound.playing(), wanted)) {
            return;
        }
        play(player, wanted, false);
    }

    public static void pressed(Player player, ItemStack stack) {
        if (!(player instanceof AbstractClientPlayer client)
                || !CombatStyles.isPlayerAnimatorLoaded()) {
            return;
        }
        UseSpec spec = EquipmentUse.specOf(stack);
        String animation = spec.releaseAnimation().orElse(null);
        if (animation == null) {
            return;
        }
        play(client, animation, true);
    }

    public static void channelStarted(Player player, ItemStack stack) {
        if (!(player instanceof AbstractClientPlayer client)
                || !CombatStyles.isPlayerAnimatorLoaded()) {
            return;
        }
        UseSpec spec = EquipmentUse.specOf(stack);
        if (!spec.channels()) {
            return;
        }
        play(client, spec.channelAnimation().orElse(null), false);
    }

    public static void release(AbstractClientPlayer player, ItemStack stack, int heldTicks) {
        if (player == null || !CombatStyles.isPlayerAnimatorLoaded()) {
            return;
        }
        UseSpec spec = EquipmentUse.specOf(stack);
        String animation = spec.releaseAnimation().orElse(null);
        if (animation == null || heldTicks < spec.required()) {
            play(player, null, false);
            return;
        }
        play(player, animation, true);
    }

    private static boolean stillPlaying(Bound bound) {
        KeyframeAnimationPlayer playing = bound.layer().getAnimation();
        if (playing == null || playing.isLoopStarted()) {
            return false;
        }
        return playing.getTick() <= 0 || playing.isActive();
    }

    public static void forget(UUID playerId) {
        BOUND.remove(playerId);
    }

    private static String wantedAnimation(AbstractClientPlayer player, UseSpec spec) {
        if (spec.isEmpty() || !player.isUsingItem() || !spec.channels()) {
            return null;
        }
        return spec.channelAnimation().orElse(null);
    }

    private static void faceLook(AbstractClientPlayer player) {
        ((LivingEntityTurnInvoker) player).combat_styles$tickHeadTurn(player.getYHeadRot(),
                NO_ANIM_STEP);
    }

    private static boolean sameAnimation(String playing, String wanted) {
        return playing == null ? wanted == null : playing.equals(wanted);
    }

    private static void play(AbstractClientPlayer player, String animationId,
                             boolean releasing) {
        AnimationStack stack = stackFor(player);
        if (stack == null) {
            return;
        }
        Bound bound = BOUND.get(player.getUUID());
        ModifierLayer<KeyframeAnimationPlayer> layer = bound == null
                ? new ModifierLayer<>()
                : bound.layer();
        AdjustmentModifier pitch = bound == null ? null : bound.pitch();
        if (bound == null) {
            stack.addAnimLayer(LAYER_PRIORITY, layer);
        }
        KeyframeAnimation animation = animationId == null ? null : lookup(animationId);
        KeyframeAnimation prepared = animation == null ? null : prepared(player, animation);
        KeyframeAnimationPlayer next = prepared == null ? null : firstPerson(prepared);
        pitch = adjustPitch(player, layer, pitch, next != null);
        if (layer.getAnimation() == null) {
            layer.setAnimation(next);
        } else {
            int fade = prepared != null && prepared.beginTick > 0
                    ? prepared.beginTick
                    : (releasing ? RELEASE_FADE_TICKS : FADE_TICKS);
            layer.replaceAnimationWithFade(
                    AbstractFadeModifier.standardFadeIn(fade, Ease.INOUTSINE), next);
        }
        BOUND.put(player.getUUID(), new Bound(layer, animation == null ? null : animationId,
                releasing && animation != null, pitch));
    }

    private static AdjustmentModifier adjustPitch(AbstractClientPlayer player,
                                                  ModifierLayer<KeyframeAnimationPlayer> layer,
                                                  AdjustmentModifier current,
                                                  boolean wanted) {
        if (current != null) {
            current.enabled = wanted;
            return current;
        }
        if (!wanted) {
            return null;
        }
        AdjustmentModifier added = UsePitch.modifierFor(player);
        layer.addModifier(added, PITCH_INDEX);
        return added;
    }

    private static KeyframeAnimation prepared(AbstractClientPlayer player,
                                              KeyframeAnimation animation) {
        KeyframeAnimation.AnimationBuilder builder = animation.mutableCopy();
        if (player.isPassenger()) {
            builder.rightLeg.fullyEnablePart(false);
            builder.leftLeg.fullyEnablePart(false);
        }
        builder.torso.fullyEnablePart(true);
        builder.head.pitch.setEnabled(false);
        return builder.build();
    }

    private static KeyframeAnimationPlayer firstPerson(KeyframeAnimation animation) {
        KeyframeAnimationPlayer player = new KeyframeAnimationPlayer(animation, FIRST_TICK);
        player.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
        return player;
    }

    private static KeyframeAnimation lookup(String animationId) {
        ResourceLocation parsed = ResourceLocation.tryParse(animationId);
        if (parsed == null) {
            return null;
        }
        try {
            return PlayerAnimationRegistry.getAnimation(parsed) instanceof KeyframeAnimation found
                    ? found
                    : null;
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Use animation lookup failed for {}", animationId, throwable);
            return null;
        }
    }

    private static AnimationStack stackFor(AbstractClientPlayer player) {
        try {
            return PlayerAnimationAccess.getPlayerAnimLayer(player);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Unable to access the player animation stack", throwable);
            return null;
        }
    }
}

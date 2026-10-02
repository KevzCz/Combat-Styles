package net.pixeldreamstudios.combat_styles.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.fx.ParticlePlacement;
import net.bettercombat.api.fx.TrailAppearance;
import net.bettercombat.client.collision.TargetFinder;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.network.Packets;
import net.bettercombat.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.pixeldreamstudios.combat_styles.client.ExtraAttackScheduler;
import net.pixeldreamstudios.combat_styles.client.DraggedTrail;
import net.pixeldreamstudios.combat_styles.client.PendingClaims;
import net.pixeldreamstudios.combat_styles.network.HitboxClaimPayload;
import net.pixeldreamstudios.combat_styles.client.SlashScheduler;
import net.pixeldreamstudios.combat_styles.equipment.SwingDrag;
import net.pixeldreamstudios.combat_styles.equipment.ClientPass;
import net.pixeldreamstudios.combat_styles.equipment.SwingDash;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackShaping;
import net.pixeldreamstudios.combat_styles.style.HitboxOffset;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

@Mixin(value = Minecraft.class, priority = 1500)
public class SwingClientMixin {

    @WrapOperation(
            method = "performAttack",
            at = @At(value = "INVOKE",
                    target = "Lnet/bettercombat/client/collision/TargetFinder;"
                            + "findAttackTargets(Lnet/minecraft/world/entity/player/Player;"
                            + "Lnet/minecraft/world/entity/Entity;"
                            + "Lnet/bettercombat/api/WeaponAttributes$Attack;D)Ljava/util/List;"),
            remap = false,
            require = 1)
    private List<Entity> combat_styles$widenAttackTargets(Player player, Entity cursorTarget,
                                                          WeaponAttributes.Attack attack,
                                                          double range,
                                                          Operation<List<Entity>> original) {
        combat_styles$queueExtras(player, attack);
        List<Entity> targets = original.call(player, cursorTarget, attack, range);
        PendingClaims.flush(player, SwingDrag.NO_EXTRA, HitboxClaimPayload.FIRST_STEP,
                PendingClaims.idOf(cursorTarget));
        List<Entity> merged = SwingDash.merge(player, targets);
        SwingDash.rememberStruck(targets);
        SwingDash.markSwung();
        return merged;
    }

    @WrapOperation(
            method = "performAttack",
            at = @At(value = "INVOKE",
                    target = "Lnet/bettercombat/client/particle/SlashParticleUtil;"
                            + "spawnParticles(Lnet/minecraft/client/player/AbstractClientPlayer;"
                            + "ZFLjava/util/List;"
                            + "Lnet/bettercombat/api/fx/TrailAppearance;)V"),
            remap = false,
            require = 1)
    private void combat_styles$followingTrail(AbstractClientPlayer player, boolean isOffhand,
                                              float weaponRange,
                                              List<ParticlePlacement> placements,
                                              TrailAppearance appearance,
                                              Operation<Void> original) {
        boolean drags = SwingDash.trailing(player);
        if (drags) {
            DraggedTrail.begin(player);
        }
        try {
            original.call(player, isOffhand, weaponRange,
                    combat_styles$shapeSlashes(player, isOffhand, weaponRange,
                            placements, appearance), appearance);
        } finally {
            if (drags) {
                DraggedTrail.end();
            }
        }
    }

    @Unique
    private static void combat_styles$queueExtras(Player player, WeaponAttributes.Attack attack) {
        if (!(player instanceof AbstractClientPlayer client)) {
            return;
        }
        List<ExtraAttack> hits = PendingExtraHit.hitsOf(player, attack);
        AttackOverride parent = AttackShaping.overrideFor(player, attack);
        if (hits.isEmpty()) {
            return;
        }
        ExtraAttackScheduler.schedule(client, combat_styles$comboOf(), attack, parent, hits);
    }

    @Unique
    private static int combat_styles$comboOf() {
        return Minecraft.getInstance() instanceof MinecraftClient_BetterCombat combat
                ? combat.getComboCount()
                : 0;
    }

    @Unique
    private static HitboxOffset combat_styles$offsetOf(SlashPlacement slash,
                                                     AttackOverride parent) {
        if (!slash.anchor().followsHitbox()) {
            return HitboxOffset.NONE;
        }
        return slash.asAttack().shapeFrom(parent).behaviourOrDefault().offsetOrNone().clamped();
    }

    @Unique
    private static List<ParticlePlacement> combat_styles$shapeSlashes(
            AbstractClientPlayer player, boolean isOffhand, float weaponRange,
            List<ParticlePlacement> placements, TrailAppearance appearance) {
        if (placements == null || placements.isEmpty()) {
            return placements;
        }
        AttackHand hand = player instanceof EntityPlayer_BetterCombat holder
                ? holder.getCurrentAttack()
                : null;
        if (hand == null) {
            return placements;
        }
        List<SlashPlacement> slashes =
                AttackShaping.behaviourFor(player, hand.attack()).slashes();
        if (slashes.isEmpty()) {
            return placements;
        }
        ParticlePlacement base = placements.getFirst();
        AttackOverride parent = AttackShaping.overrideFor(player, hand.attack());
        List<ParticlePlacement> shaped = new ArrayList<>(placements);
        for (SlashPlacement slash : slashes) {
            if (!slash.mode().spawnsParticle()) {
                continue;
            }
            ParticlePlacement placement = slash.toBetterCombat(base,
                    combat_styles$offsetOf(slash, parent));
            if (slash.delayTicks() > 0) {
                SlashScheduler.schedule(player, isOffhand, weaponRange,
                        placement, appearance, slash.delayTicks());
            } else {
                shaped.add(placement);
            }
        }
        return shaped;
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 1)
    private void combat_styles$clientTick(CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        AbstractClientPlayer player = client.player;
        if (player == null) {
            return;
        }
        SlashScheduler.tick(player.level().getGameTime());
        ExtraAttackScheduler.tick(client, player.level().getGameTime());
        MinecraftClient_BetterCombat combat = (MinecraftClient_BetterCombat) client;
        if (!SwingDash.sweeping(player)) {
            return;
        }
        combat_styles$sample(client, combat, player);
    }

    @Unique
    private void combat_styles$sample(Minecraft client, MinecraftClient_BetterCombat combat,
                                      AbstractClientPlayer player) {
        int armed = SwingDash.armedCombo();
        AttackHand hand = SwingDash.armedHand(player);
        if (hand == null) {
            hand = armed < 0
                    ? combat.getCurrentAttackHand()
                    : PlayerAttackHelper.getCurrentAttack(player, armed);
            if (hand == null) {
                hand = combat.getCurrentAttackHand();
            }
            if (hand == null) {
                return;
            }
            SwingDash.rememberHand(player, hand);
        }
        double range = PlayerAttackHelper.getRangeForItem(player, hand.itemStack())
                * hand.attack().rangeMultiplier();
        List<Entity> found = new ArrayList<>(TargetFinder.findAttackTargets(
                player, combat.getCursorTarget(), hand.attack(), range));
        Vec3 from = SwingDash.advanceSample(player);
        if (from != null) {
            Vec3 to = player.position();
            double travelled = from.distanceTo(to);
            if (travelled > 0.05D) {
                AABB path = new AABB(from, to).inflate(range);
                for (Entity candidate : player.level().getEntities(player, path)) {
                    if (candidate.isAttackable() && !found.contains(candidate)) {
                        found.add(candidate);
                    }
                }
            }
        }
        List<Entity> fresh = SwingDash.newlyDragged(player, found);
        if (fresh.isEmpty()) {
            PendingClaims.clear();
            return;
        }
        int armedExtra = SwingDash.armedExtra();
        PendingClaims.flush(player, armedExtra < 0 ? HitboxClaimPayload.SAME_SWING : armedExtra,
                HitboxClaimPayload.FIRST_STEP, PendingClaims.idOf(combat.getCursorTarget()));
        Packets.C2S_AttackRequest packet = new Packets.C2S_AttackRequest(
                armedExtra < 0
                        ? SwingDrag.encodeSweep(SwingDash.armedCombo())
                        : SwingDrag.encodeExtra(SwingDash.armedCombo(), armedExtra),
                player.isShiftKeyDown(),
                player.getInventory().selected, combat.getCursorTarget(), fresh);
        Platform.networkC2S_Send(packet);
        SwingDash.atFullStrength(player, () -> {
            for (Entity target : fresh) {
                player.attack(target);
            }
        });
    }

    @Inject(method = "cancelUpswing", at = @At("HEAD"), remap = false, require = 1)
    private void combat_styles$clearSwingOnCancel(CallbackInfo callback) {
        SwingDash.clear();
        ClientPass.release();
    }
}

package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.collision.OrientedBoundingBox;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.equipment.SwingDrag;
import net.pixeldreamstudios.combat_styles.network.HitboxClaimPayload;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeclaredClaims {

    private static final double CLAIM_SLACK = 1.0D;
    private static final float MAX_VIEW_DRIFT = 45.0F;

    private record Source(HitboxSet boxes, OrientedBoundingBox base, HitboxGeometry.View view) {
    }

    private static final Map<UUID, HitboxClaimPayload> DEFERRED = new ConcurrentHashMap<>();

    private DeclaredClaims() {
    }

    public static void accept(ServerPlayer player, HitboxClaimPayload payload) {
        if (payload.extraIndex() == SwingDrag.NO_EXTRA) {
            DEFERRED.put(player.getUUID(), payload);
            return;
        }
        apply(player, payload);
    }

    public static void applyDeferred(Player player) {
        if (player instanceof ServerPlayer served) {
            HitboxClaimPayload payload = DEFERRED.remove(served.getUUID());
            if (payload != null) {
                apply(served, payload);
            }
        }
    }

    public static void forget(UUID playerId) {
        DEFERRED.remove(playerId);
    }

    private static void apply(ServerPlayer player, HitboxClaimPayload payload) {
        List<Integer> ids = payload.entityIds();
        List<Integer> indices = payload.boxIndices();
        if (ids.size() != indices.size()) {
            return;
        }
        Source source = sourceFor(player, payload);
        if (source == null || source.boxes().isEmpty()) {
            return;
        }
        HitboxSet boxes = source.boxes();
        for (int slot = 0; slot < ids.size(); slot++) {
            Entity target = player.level().getEntity(ids.get(slot));
            int index = indices.get(slot);
            if (target == null || !target.isAlive()
                    || index < 0 || index >= boxes.shapes().size()) {
                continue;
            }
            OrientedBoundingBox claimed = HitboxGeometry.shaped(source.base(), source.view(),
                    boxes.shapes().get(index));
            if (!claimed.intersects(target.getBoundingBox().inflate(CLAIM_SLACK))) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "claim — rejected box " + index + " on "
                            + target.getName().getString() + " (box at " + claimed.center
                            + ", target at " + target.position() + ", view "
                            + source.view().yaw() + "/" + source.view().pitch()
                            + ", client " + payload.yaw() + "/" + payload.pitch() + ")");
                }
                continue;
            }
            HitboxClaims.applyDeclared(player, target, boxes, index);
        }
    }

    private static Source sourceFor(ServerPlayer player, HitboxClaimPayload payload) {
        HitboxGeometry.View view = viewOf(player, payload);
        WeaponAttributes.Attack attack = CombatState.swungAttack(player);
        if (attack == null) {
            return null;
        }
        Entity cursorTarget = payload.cursorTargetId() == HitboxClaimPayload.NO_TARGET
                ? null
                : player.level().getEntity(payload.cursorTargetId());
        if (payload.extraIndex() < 0) {
            AttackBehaviour behaviour = AttackShaping.behaviourFor(player, attack);
            HitboxSet boxes = behaviour.boxesOrNone();
            if (boxes.staggered()) {
                boxes = boxes.atDelay(HitboxClaimPayload.FIRST_STEP);
            }
            AttackHand hand = CombatState.swungHand(player);
            ItemStack weapon = hand == null ? player.getMainHandItem() : hand.itemStack();
            return new Source(boxes, baseFor(player, view, attack, behaviour, weapon,
                    SlashAnchor.WEAPON, cursorTarget), view);
        }
        List<ExtraAttack> hits = PendingExtraHit.hitsOf(player, attack);
        if (payload.extraIndex() >= hits.size()) {
            return null;
        }
        ExtraAttack hit = hits.get(payload.extraIndex());
        AttackOverride shape = hit.shapeFrom(AttackShaping.overrideFor(player, attack));
        AttackBehaviour behaviour = shape.behaviourOrDefault();
        HitboxSet boxes = behaviour.boxesOrNone();
        if (boxes.staggered()) {
            boxes = boxes.atDelay(payload.delayStep());
            behaviour = behaviour.withBoxes(boxes);
        }
        WeaponAttributes.Attack shaped = PendingExtraHit.shapedAttack(attack, shape);
        return new Source(boxes, baseFor(player, view, shaped, behaviour, player.getMainHandItem(),
                hit.anchor(), cursorTarget), view);
    }

    private static HitboxGeometry.View viewOf(ServerPlayer player, HitboxClaimPayload payload) {
        HitboxGeometry.View server = HitboxGeometry.View.of(player);
        boolean plausible = Math.abs(Mth.wrapDegrees(payload.yaw() - server.yaw())) <= MAX_VIEW_DRIFT
                && Math.abs(payload.pitch() - server.pitch()) <= MAX_VIEW_DRIFT;
        return plausible ? new HitboxGeometry.View(payload.yaw(), payload.pitch()) : server;
    }

    private static OrientedBoundingBox baseFor(ServerPlayer player, HitboxGeometry.View view,
                                               WeaponAttributes.Attack attack,
                                               AttackBehaviour behaviour, ItemStack weapon,
                                               SlashAnchor anchor, Entity cursorTarget) {
        double range = PlayerAttackHelper.getRangeForItem(player, weapon)
                * attack.rangeMultiplier();
        OrientedBoundingBox base = HitboxGeometry.base(player, view, attack, range);
        return HitboxGeometry.moved(base, player, view, behaviour, HitboxGeometry.spins(attack),
                anchor, cursorTarget);
    }
}

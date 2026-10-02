package net.pixeldreamstudios.combat_styles.mixin;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.collision.OrientedBoundingBox;
import net.bettercombat.client.collision.TargetFinder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.client.PendingClaims;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.style.AttackBehaviour;
import net.pixeldreamstudios.combat_styles.style.AttackPlacement;
import net.pixeldreamstudios.combat_styles.style.AttackShaping;
import net.pixeldreamstudios.combat_styles.style.HitboxClaims;
import net.pixeldreamstudios.combat_styles.style.HitboxGeometry;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.ExtraAttackScope;
import net.pixeldreamstudios.combat_styles.style.HitboxOffset;
import net.pixeldreamstudios.combat_styles.style.SlashAnchor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(value = TargetFinder.class, remap = false)
public class HitboxOffsetMixin {

    @Unique
    private static final String DISTANCE_FORMAT = "%.1f";

    @Unique
    private static void combat_styles$matchBoxes(Player player, OrientedBoundingBox base,
                                                 HitboxSet boxes, List<Entity> candidates,
                                                 List<Entity> matched) {
        boolean debug = CombatStylesConfig.get().debugLogging() && !PendingClaims.previewing();
        List<OrientedBoundingBox> built = new ArrayList<>(boxes.shapes().size());
        for (HitboxSet.Shape shape : boxes.shapes()) {
            OrientedBoundingBox shaped =
                    HitboxGeometry.shaped(base, player, shape);
            built.add(shaped);
            if (debug) {
                CombatStyles.LOGGER.info("[BOX] shape at {} size {}x{}x{} delay={} "
                                + "(base at {}, {} candidate(s))",
                        shaped.center, shape.clampedWidth(), shape.clampedHeight(),
                        shape.clampedDepth(), shape.clampedDelay(), base.center,
                        candidates.size());
            }
        }
        for (Entity candidate : candidates) {
            HitboxSet.Shape claimed = null;
            int claimedIndex = -1;
            for (int index = 0; index < built.size(); index++) {
                if (!built.get(index).intersects(candidate.getBoundingBox())) {
                    continue;
                }
                HitboxSet.Shape shape = boxes.shapes().get(index);
                if (!boxes.usesPriority()) {
                    claimed = shape;
                    claimedIndex = index;
                    break;
                }
                if (claimed == null || shape.priority() > claimed.priority()) {
                    claimed = shape;
                    claimedIndex = index;
                }
            }
            if (claimed == null) {
                continue;
            }
            if (PendingClaims.previewing()) {
                matched.add(candidate);
                continue;
            }
            if (!HitboxClaims.record(player, candidate, claimed, claimedIndex)) {
                continue;
            }
            matched.add(candidate);
            if (player.level().isClientSide()) {
                PendingClaims.declare(candidate.getId(), claimedIndex, claimed.priority());
            }
        }
    }

    @Unique
    private static List<Entity> combat_styles$aroundBox(Player player,
                                                        OrientedBoundingBox box,
                                                        double reach) {
        AABB search = new AABB(box.center, box.center).inflate(reach);
        return player.level().getEntities(player, search,
                candidate -> candidate.isPickable() && candidate.isAlive());
    }

    @Inject(method = "findAttackTargetResult", at = @At("RETURN"), require = 1)
    private static void combat_styles$offsetHitbox(Player player, Entity cursorTarget,
                                                   WeaponAttributes.Attack attack, double range,
                                                   CallbackInfoReturnable<TargetFinder.TargetResult> callback) {
        TargetFinder.TargetResult result = callback.getReturnValue();
        if (result == null || result.obb == null || player == null) {
            return;
        }
        boolean debug = CombatStylesConfig.get().debugLogging() && !PendingClaims.previewing();
        AttackBehaviour scoped = ExtraAttackScope.active();
        AttackBehaviour behaviour = scoped == null
                ? AttackShaping.behaviourFor(player, attack)
                : scoped;
        HitboxOffset offset = behaviour.offsetOrNone().clamped();
        AttackPlacement placement = behaviour.placementOrNone();
        SlashAnchor anchor = ExtraAttackScope.anchor();
        if (offset.isEmpty() && placement.isEmpty()
                && behaviour.boxesOrNone().isEmpty() && !anchor.worldAligned()) {
            if (debug) {
                CombatStyles.LOGGER.info("[OFFSET] none for {} (side={})",
                        attack.animation(), player.level().isClientSide() ? "client" : "server");
            }
            return;
        }

        OrientedBoundingBox moved = HitboxGeometry.moved(result.obb, player, behaviour,
                HitboxGeometry.spins(attack), anchor, cursorTarget);
        result.obb = moved;

        HitboxSet boxes = behaviour.boxesOrNone();
        if (scoped == null && boxes.staggered()) {
            boxes = boxes.atDelay(0);
        }
        double shift = Math.sqrt(offset.forward() * offset.forward()
                + offset.up() * offset.up()
                + offset.right() * offset.right());
        double reach = range + (shift + boxes.reach()) * 2.0D;
        List<Entity> candidates = placement.followsCursor() || !boxes.isEmpty()
                || offset.reorients() || anchor.worldAligned()
                ? combat_styles$aroundBox(player, moved, reach)
                : TargetFinder.getInitialTargets(player, cursorTarget, reach);
        List<Entity> matched = new ArrayList<>();
        if (boxes.isEmpty()) {
            for (Entity candidate : candidates) {
                if (moved.intersects(candidate.getBoundingBox())) {
                    matched.add(candidate);
                }
            }
        } else {
            combat_styles$matchBoxes(player, moved, boxes, candidates, matched);
        }
        result.entities = matched;
        if (debug) {
            StringBuilder nearby = new StringBuilder();
            for (Entity candidate : candidates) {
                nearby.append(' ').append(candidate.getName().getString())
                        .append('@').append(String.format(Locale.ROOT, DISTANCE_FORMAT,
                                candidate.position().distanceTo(moved.center)));
            }
            CombatStyles.LOGGER.info("[OFFSET] {} fwd={} up={} right={} range={} "
                            + "boxAt={} playerAt={} candidates={} matched={} dists={}",
                    attack.animation(), offset.forward(), offset.up(), offset.right(),
                    range, moved.center, player.position(),
                    candidates.size(), matched.size(), nearby);
        }
    }
}

package net.pixeldreamstudios.combat_styles.client;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.fx.ParticlePlacement;
import net.bettercombat.api.fx.TrailAppearance;
import net.bettercombat.client.collision.TargetFinder;
import net.bettercombat.client.particle.SlashParticleUtil;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.network.Packets;
import net.bettercombat.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.equipment.SwingDash;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.equipment.SwingDrag;
import net.pixeldreamstudios.combat_styles.style.AttackDrag;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.HitboxOffset;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.SlashPlacement;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.ExtraAttackScope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ExtraAttackScheduler {

    private record Pending(AbstractClientPlayer player, int comboCount,
                           ExtraAttack hit, AttackOverride shape,
                           WeaponAttributes.Attack attack, int hitIndex, int step,
                           long dueAt) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private ExtraAttackScheduler() {
    }

    public static void schedule(AbstractClientPlayer player, int comboCount,
                                WeaponAttributes.Attack attack, AttackOverride parent,
                                List<ExtraAttack> hits) {
        if (player == null || hits == null || hits.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        Minecraft client = Minecraft.getInstance();
        for (int index = 0; index < hits.size(); index++) {
            ExtraAttack hit = hits.get(index);
            AttackOverride shape = hit.shapeFrom(parent);
            int base = Math.min(hit.delayTicks(), ExtraAttack.MAX_DELAY);
            HitboxSet boxes = shape.behaviourOrDefault().boxesOrNone();
            if (!boxes.staggered()) {
                queue(client, new Pending(player, comboCount, hit, shape, attack,
                        index, 0, now + base), base);
                continue;
            }
            for (int step : boxes.delaySteps()) {
                AttackOverride staged = shape.withBehaviour(
                        shape.behaviourOrDefault().withBoxes(boxes.atDelay(step)));
                queue(client, new Pending(player, comboCount, hit, staged, attack,
                        index, step, now + base + step), base + step);
            }
        }
    }

    private static void queue(Minecraft client, Pending pending, int delay) {
        if (delay <= 0) {
            fire(client, pending);
            return;
        }
        PENDING.add(pending);
    }

    public static void tick(Minecraft client, long gameTime) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (!pending.player().isAlive() || pending.player().isRemoved()) {
                iterator.remove();
                continue;
            }
            if (gameTime < pending.dueAt()) {
                continue;
            }
            iterator.remove();
            fire(client, pending);
        }
    }

    private static void spawnOwnSlashes(AbstractClientPlayer player, AttackOverride shape) {
        List<SlashPlacement> slashes = shape.behaviourOrDefault().slashes();
        if (slashes.isEmpty()) {
            return;
        }
        AttackHand hand = player instanceof EntityPlayer_BetterCombat holder
                ? holder.getCurrentAttack()
                : null;
        if (hand == null) {
            return;
        }
        float range = (float) PlayerAttackHelper.getRangeForItem(player,
                player.getMainHandItem());
        TrailAppearance appearance = SlashParticleUtil.appearanceFor(player,
                player.getMainHandItem());
        HitboxOffset offset = shape.behaviourOrDefault().offsetOrNone().clamped();
        for (SlashPlacement slash : slashes) {
            if (!slash.mode().spawnsParticle()) {
                continue;
            }
            ParticlePlacement placement = slash.toBetterCombat(SlashPlacement.FALLBACK,
                    slash.anchor().followsHitbox() ? offset : HitboxOffset.NONE);
            if (slash.delayTicks() > 0) {
                SlashScheduler.schedule(player, false, range, placement, appearance,
                        slash.delayTicks());
            } else {
                SlashParticleUtil.spawnParticles(player, false, range,
                        List.of(placement), appearance);
            }
        }
    }

    private static void fire(Minecraft client, Pending pending) {
        AbstractClientPlayer player = pending.player();
        WeaponAttributes.Attack shaped = shapedAttack(pending);
        if (shaped == null) {
            return;
        }
        double range = PlayerAttackHelper.getRangeForItem(player, player.getMainHandItem())
                * shaped.rangeMultiplier();
        List<Entity> targets;
        ExtraAttackScope.push(pending.shape().behaviourOrDefault(), pending.hit().anchor());
        try {
            targets = TargetFinder.findAttackTargets(player,
                    cursorTarget(client), shaped, range);
        } finally {
            ExtraAttackScope.pop();
        }
        AttackOverride shape = pending.shape();
        spawnOwnSlashes(player, shape);
        SwingDash.armExtra(player, pending.comboCount(), pending.hitIndex(),
                AttackDrag.effective(shape, List.of()),
                !shape.behaviourOrDefault().motionOrNone().isEmpty(), targets);
        CombatEffectDealer.runExtraMotion(player, shape);
        if (targets.isEmpty()) {
            PendingClaims.clear();
            if (CombatStylesConfig.get().debugLogging()) {
                CombatStyles.LOGGER.info("[EXTRA] hit {} found no targets (delay={})",
                        pending.hitIndex(), pending.hit().delayTicks());
            }
            return;
        }
        PendingClaims.flush(player, pending.hitIndex(), pending.step(),
                PendingClaims.idOf(cursorTarget(client)));
        Packets.C2S_AttackRequest packet = new Packets.C2S_AttackRequest(
                SwingDrag.encodeExtra(pending.comboCount(), pending.hitIndex()),
                player.isShiftKeyDown(),
                player.getInventory().selected, cursorTarget(client), targets);
        Platform.networkC2S_Send(packet);
        SwingDash.atFullStrength(player, () -> {
            for (Entity target : targets) {
                player.attack(target);
            }
        });
    }

    private static WeaponAttributes.Attack shapedAttack(Pending pending) {
        return PendingExtraHit.shapedAttack(pending.attack(), pending.shape());
    }

    private static Entity cursorTarget(Minecraft client) {
        return client instanceof MinecraftClient_BetterCombat combat
                ? combat.getCursorTarget()
                : null;
    }

    public static void clear() {
        PENDING.clear();
    }
}

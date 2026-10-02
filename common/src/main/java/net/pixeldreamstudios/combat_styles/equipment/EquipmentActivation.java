package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.api.CombatScheduler;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixDamageDealer;
import net.pixeldreamstudios.combat_styles.affix.AffixImpacts;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.EquipmentAffix;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class EquipmentActivation {

    private static final int OWNER_DEPTH = 4;

    private static boolean firing;

    private EquipmentActivation() {
    }

    public static void shot(Player player, Entity projectile, boolean viaSummon) {
        if (projectile == null) {
            return;
        }
        ItemStack weapon = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        ShotEvent event = new ShotEvent(projectile, weapon, viaSummon);
        react(player, EquipmentContext.shotBy(player, event),
                trigger -> trigger.activation().reactsToShoot(), "shot");
    }

    public static void blocked(Player player, BlockEvent event) {
        if (event == null) {
            return;
        }
        react(player, EquipmentContext.blockedBy(player, event),
                trigger -> trigger.activation().reactsToBlock(), "shield block");
    }

    public static void used(Player player, UseEvent event) {
        if (event == null) {
            return;
        }
        react(player, EquipmentContext.usedBy(player, event),
                trigger -> trigger.activation().usesItem(), "use");
    }

    public static void cast(Player player, SpellCastEvent event) {
        if (event == null) {
            return;
        }
        react(player, EquipmentContext.castBy(player, event),
                trigger -> trigger.activation().reactsToCast(), "spell cast");
    }

    public static void projectileHit(Player player, ProjectileHitEvent event) {
        if (event == null) {
            return;
        }
        react(player, EquipmentContext.projectileHitBy(player, event),
                trigger -> trigger.activation().reactsToProjectileHit(), "projectile hit");
    }

    public static Player ownerOf(Entity projectile) {
        if (!(projectile instanceof Projectile shot)) {
            return null;
        }
        Entity owner = shot.getOwner();
        for (int depth = 0; owner != null && depth < OWNER_DEPTH; depth++) {
            if (owner instanceof Player player) {
                return player;
            }
            owner = owner instanceof OwnableEntity owned ? owned.getOwner() : null;
        }
        return null;
    }

    private static void react(Player player, EquipmentContext context,
                              Predicate<EquipmentTrigger> wanted, String label) {
        if (player == null || player.level().isClientSide() || firing) {
            return;
        }
        if (EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return;
        }
        firing = true;
        try {
            EventKind kind = EventKind.activationOf(context);
            List<EquipmentRuntime.Match> heard = new ArrayList<>();
            for (EquipmentRuntime.Match match : EquipmentRuntime.matching(player, context)) {
                if (match.hears(wanted) || match.effect().events().has(kind)) {
                    heard.add(match);
                }
            }
            for (EquipmentRuntime.Match match : EquipmentRuntime.pickGroups(player, context, heard)) {
                List<AffixEffects.OnHit> payloads = match.effect().events().get(kind);
                boolean arming = EffectWindows.arms(match)
                        && match.trigger().eventFiring(context);
                if (!arming && payloads.isEmpty()
                        && match.entry().selfEffects().isEmpty()) {
                    continue;
                }
                try {
                    if (arming) {
                        if (EffectWindows.arm(player, match)) {
                            if (CombatDebug.enabled(player)) {
                                CombatDebug.fired(player, match.id(), "armed on " + label);
                            }
                            sync(player, match);
                        }
                        if (payloads.isEmpty()) {
                            continue;
                        }
                    }
                    if (!EffectWindows.claim(player, match)) {
                        continue;
                    }
                    StyleAffix affix = EquipmentAffix.of(match);
                    if (CombatDebug.enabled(player)) {
                        CombatDebug.fired(player, match.id(), "procced on " + label);
                    }
                    sync(player, match);
                    LivingEntity target = context.target();
                    Runnable fire = () -> {
                        AffixImpacts.applySelfEffects(player, match.entry().selfEffects());
                        for (AffixEffects.OnHit payload : payloads) {
                            AffixDamageDealer.applyHurtReaction(player, target, affix,
                                    payload, 0.0F);
                        }
                    };
                    if (!CombatScheduler.after(player, match.entry().delayTicks(),
                            match.id(), fire)) {
                        fire.run();
                    }
                } catch (Throwable throwable) {
                    CombatStyles.LOGGER.error("Failed to apply {} reaction {}",
                            label, match.id(), throwable);
                }
            }
        } finally {
            firing = false;
        }
    }

    private static void sync(Player player, EquipmentRuntime.Match match) {
        if (player instanceof ServerPlayer served) {
            NetworkManager.syncCooldowns(served, match.itemId());
        }
    }
}

package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.api.CombatScheduler;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.affix.AffixImpacts;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.EquipmentAffix;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EquipmentHurt {

    public static final int TOTAL_WINDOW_TICKS = 200;

    private record Running(float total, long expiresAt) {
    }

    private static final Map<UUID, Running> TOTALS = new ConcurrentHashMap<>();

    private EquipmentHurt() {
    }

    public static void forget(UUID playerId) {
        TOTALS.remove(playerId);
    }

    public static void react(Player player, DamageSource source, float amount) {
        if (player == null || player.level().isClientSide() || amount <= 0.0F) {
            return;
        }
        if (EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return;
        }
        float total = accumulate(player, amount);
        HurtEvent hurt = new HurtEvent(source, amount, total);
        EquipmentContext context = EquipmentContext.hurtBy(player, hurt);

        LivingEntity attacker = source == null || !(source.getEntity() instanceof LivingEntity living)
                ? null
                : living;
        List<EquipmentRuntime.Match> heard = new ArrayList<>();
        for (EquipmentRuntime.Match match : EquipmentRuntime.matching(player, context)) {
            if (match.hears(EquipmentTrigger::reactsToHurt)
                    || match.effect().events().has(EventKind.ON_HURT)) {
                heard.add(match);
            }
        }
        for (EquipmentRuntime.Match match : EquipmentRuntime.pickGroups(player, context, heard)) {
            List<AffixEffects.OnHit> payloads = match.effect().events().get(EventKind.ON_HURT);
            boolean arming = EffectWindows.arms(match) && match.trigger().eventFiring(context);
            if (!arming && payloads.isEmpty()
                    && match.entry().selfEffects().isEmpty()) {
                continue;
            }
            try {
                if (arming) {
                    if (EffectWindows.arm(player, match)) {
                        if (CombatDebug.enabled(player)) {
                            CombatDebug.fired(player, match.id(),
                                    "armed on hurt — " + amount + " damage, running total " + total);
                        }
                        if (player instanceof ServerPlayer served) {
                            NetworkManager.syncCooldowns(served, match.itemId());
                        }
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
                    CombatDebug.fired(player, match.id(),
                            "procced on hurt — " + amount + " damage, running total " + total);
                }
                if (player instanceof ServerPlayer served) {
                    NetworkManager.syncCooldowns(served, match.itemId());
                }
                Runnable fire = () -> {
                    AffixImpacts.applySelfEffects(player, match.entry().selfEffects());
                    for (AffixEffects.OnHit payload : payloads) {
                        CombatEffectDealer.applyHurtReaction(player, attacker, affix,
                                payload, amount);
                    }
                };
                if (!CombatScheduler.after(player, match.entry().delayTicks(),
                        match.id(), fire)) {
                    fire.run();
                }
            } catch (Throwable throwable) {
                CombatStyles.LOGGER.error("Failed to apply hurt reaction {}",
                        match.id(), throwable);
            }
        }
    }

    private static float accumulate(Player player, float amount) {
        long now = player.level().getGameTime();
        Running running = TOTALS.get(player.getUUID());
        float total = running == null || now > running.expiresAt()
                ? amount
                : running.total() + amount;
        TOTALS.put(player.getUUID(), new Running(total, now + TOTAL_WINDOW_TICKS));
        return total;
    }
}

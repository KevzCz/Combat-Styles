package net.pixeldreamstudios.combat_styles.skill;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillDots {

    public record Boost(Holder<MobEffect> effect, Holder<Attribute> scaling, float perPoint, float flat) {
    }

    private static final Map<UUID, Map<String, List<Boost>>> BOOSTS = new ConcurrentHashMap<>();
    private static final Map<LivingEntity, Map<Holder<MobEffect>, UUID>> APPLIERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SkillDots() {
    }

    public static void define(ServerPlayer player, String source, List<Boost> boosts) {
        if (player == null || source == null) {
            return;
        }
        Map<String, List<Boost>> sources = BOOSTS.computeIfAbsent(player.getUUID(), key -> new ConcurrentHashMap<>());
        if (boosts == null || boosts.isEmpty()) {
            sources.remove(source);
        } else {
            sources.put(source, List.copyOf(boosts));
        }
    }

    public static void applied(LivingEntity target, Holder<MobEffect> effect, Entity source) {
        if (target == null || effect == null || target.level().isClientSide()) {
            return;
        }
        Player player = playerOf(source);
        if (player == null || player == target || !BOOSTS.containsKey(player.getUUID())) {
            return;
        }
        APPLIERS.computeIfAbsent(target, key -> new HashMap<>()).put(effect, player.getUUID());
    }

    public static float tick(LivingEntity victim, Holder<MobEffect> effect, float base) {
        if (victim == null || !(victim.level() instanceof ServerLevel level)) {
            return base;
        }
        Map<Holder<MobEffect>, UUID> appliers = APPLIERS.get(victim);
        UUID applier = appliers == null ? null : appliers.get(effect);
        Map<String, List<Boost>> sources = applier == null ? null : BOOSTS.get(applier);
        if (sources == null) {
            return base;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(applier);
        if (player == null) {
            return base;
        }
        float amount = base;
        for (List<Boost> boosts : new ArrayList<>(sources.values())) {
            for (Boost boost : boosts) {
                if (!boost.effect().equals(effect)) {
                    continue;
                }
                amount += boost.flat();
                if (boost.scaling() != null && player.getAttribute(boost.scaling()) != null) {
                    amount += (float) player.getAttributeValue(boost.scaling()) * boost.perPoint();
                }
            }
        }
        return amount;
    }

    public static void forget(UUID playerId) {
        BOOSTS.remove(playerId);
    }

    private static Player playerOf(Entity source) {
        if (source instanceof Player player) {
            return player;
        }
        if (source instanceof Projectile projectile && projectile.getOwner() instanceof Player owner) {
            return owner;
        }
        return SummonOwner.ownerPlayer(source);
    }
}

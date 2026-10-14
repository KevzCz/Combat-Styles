package net.pixeldreamstudios.combat_styles.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillEvents {

    public enum Type {
        HURT, HIT, KILL, EVASION, SPELL_CAST, HEAL, TICK, SUMMON_HIT, SUMMON_KILL, COUNTER_MAX;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type byId(String id) {
            if (id == null) {
                return null;
            }
            for (Type type : values()) {
                if (type.id().equals(id)) {
                    return type;
                }
            }
            return null;
        }
    }

    public record Event(Type type, ServerPlayer player, LivingEntity other, String spell, String school,
                        String counter, float amount) {

        public static Event of(Type type, ServerPlayer player, LivingEntity other) {
            return new Event(type, player, other, null, null, null, 0.0F);
        }
    }

    @FunctionalInterface
    public interface Listener {
        void on(Event event);
    }

    private static final Map<UUID, Map<String, Listener>> LISTENERS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_SCHOOL = new ConcurrentHashMap<>();

    private SkillEvents() {
    }

    public static void listen(ServerPlayer player, String key, Listener listener) {
        if (player == null || key == null) {
            return;
        }
        Map<String, Listener> listeners = LISTENERS.computeIfAbsent(player.getUUID(),
                id -> new ConcurrentHashMap<>());
        if (listener == null) {
            listeners.remove(key);
        } else {
            listeners.put(key, listener);
        }
    }

    public static void unlisten(ServerPlayer player, String key) {
        listen(player, key, null);
    }

    public static boolean hasListeners(Player player) {
        Map<String, Listener> listeners = player == null ? null : LISTENERS.get(player.getUUID());
        return listeners != null && !listeners.isEmpty();
    }

    public static String lastSchool(Player player) {
        return player == null ? null : LAST_SCHOOL.get(player.getUUID());
    }

    public static void fire(Event event) {
        if (event == null || event.player() == null) {
            return;
        }
        Map<String, Listener> listeners = LISTENERS.get(event.player().getUUID());
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (Listener listener : new ArrayList<>(listeners.values())) {
            try {
                listener.on(event);
            } catch (Throwable throwable) {
                CombatStyles.LOGGER.error("Skill event {} failed", event.type().id(), throwable);
            }
        }
    }

    public static void hurt(LivingEntity victim, DamageSource source, float amount) {
        if (victim == null || victim.level().isClientSide()) {
            return;
        }
        Entity attacker = attackerOf(source);
        LivingEntity living = attacker instanceof LivingEntity entity ? entity : null;
        if (victim instanceof ServerPlayer player) {
            fire(new Event(Type.HURT, player, living, null, null, null, amount));
        }
        if (attacker instanceof ServerPlayer player && attacker != victim) {
            fire(new Event(Type.HIT, player, victim, null, null, null, amount));
            return;
        }
        ServerPlayer owner = ownerOf(attacker);
        if (owner != null && owner != victim) {
            fire(new Event(Type.SUMMON_HIT, owner, victim, null, null, null, amount));
        }
    }

    public static void died(LivingEntity dying, DamageSource source) {
        if (dying == null || dying.level().isClientSide()) {
            return;
        }
        Entity killer = attackerOf(source);
        if (killer instanceof ServerPlayer player && killer != dying) {
            fire(Event.of(Type.KILL, player, dying));
            return;
        }
        ServerPlayer owner = ownerOf(killer);
        if (owner != null && owner != dying) {
            fire(Event.of(Type.SUMMON_KILL, owner, dying));
        }
    }

    public static void spellCast(Player caster, String spellId, String school, LivingEntity target) {
        if (!(caster instanceof ServerPlayer player)) {
            return;
        }
        fire(new Event(Type.SPELL_CAST, player, target, spellId, school, null, 0.0F));
        if (school != null) {
            LAST_SCHOOL.put(player.getUUID(), school);
        }
    }

    public static void heal(Entity caster, String spellId, LivingEntity target, float amount) {
        if (caster instanceof ServerPlayer player) {
            fire(new Event(Type.HEAL, player, target, spellId, null, null, amount));
        }
    }

    public static void evaded(LivingEntity entity, DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            Entity attacker = attackerOf(source);
            fire(Event.of(Type.EVASION, player, attacker instanceof LivingEntity living ? living : null));
        }
    }

    public static void tick(ServerPlayer player) {
        if (hasListeners(player)) {
            fire(Event.of(Type.TICK, player, null));
        }
    }

    public static void counterMax(ServerPlayer player, String counter) {
        fire(new Event(Type.COUNTER_MAX, player, null, null, null, counter, 0.0F));
    }

    public static void forget(UUID playerId) {
        LISTENERS.remove(playerId);
        LAST_SCHOOL.remove(playerId);
    }

    private static Entity attackerOf(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity attacker = source.getEntity();
        if (attacker == null && source.getDirectEntity() instanceof Projectile projectile) {
            attacker = projectile.getOwner();
        }
        return attacker;
    }

    private static ServerPlayer ownerOf(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (SummonOwner.isSummon(entity) && SummonOwner.ownerPlayer(entity) instanceof ServerPlayer owner) {
            return owner;
        }
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer owner) {
            return owner;
        }
        return null;
    }
}

package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.AbilityGrant;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityHosts {

    private static final int NO_FINALE = 0;
    private static final float NO_DAMAGE = 0.0F;
    private static final Map<UUID, MobHost> HOSTS = new ConcurrentHashMap<>();
    private static final Map<UUID, SummonPassives> SUMMON_PASSIVES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> RUNNING = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<List<Deferred>> DEFERRED = ThreadLocal.withInitial(ArrayList::new);

    private record Deferred(Entity summon, PassiveTrigger on, LivingEntity other) {
    }

    private record SummonPassives(ConfigurableSummon summon, Player owner, PassiveRunner runner) {
        private Entity body() {
            return (Entity) summon;
        }
    }

    private AbilityHosts() {
    }

    public static boolean wants(SummonProfile profile) {
        return profile != null && (!profile.abilities().isEmpty()
                || profile.cadence().recastTicks().orElse(0) > 0
                || !profile.cadence().passives().isEmpty()
                || profile.finale().isPresent()
                || profile.variants().stream().anyMatch(variant -> !variant.abilities().isEmpty()
                        || variant.cadence().recastTicks().orElse(0) > 0
                        || !variant.cadence().passives().isEmpty()
                        || variant.finale().isPresent()));
    }

    public static void track(Player owner, LivingEntity mob, String profileKey, int slot,
                             float damage) {
        if (owner == null || mob == null || mob instanceof ConfigurableSummon) {
            return;
        }
        MobHost host = new MobHost(mob, owner.getUUID(), damage, false);
        host.caster.bind(profileKey, slot);
        HOSTS.put(mob.getUUID(), host);
    }

    public static void hostGranted(Player owner, LivingEntity mob) {
        if (owner == null || mob == null || mob instanceof ConfigurableSummon) {
            return;
        }
        HOSTS.computeIfAbsent(mob.getUUID(),
                missing -> new MobHost(mob, owner.getUUID(), grantDamageOf(mob), true));
    }

    public static boolean hosts(Entity entity) {
        return entity != null && HOSTS.containsKey(entity.getUUID());
    }

    public static void guarded(Runnable action) {
        boolean outer = RUNNING.get();
        RUNNING.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            RUNNING.set(outer);
        }
    }

    public static void tick(long gameTime) {
        HOSTS.values().removeIf(host -> !host.alive() || host.expired());
        for (MobHost host : HOSTS.values()) {
            try {
                host.tick();
            } catch (Throwable throwable) {
                CombatStyles.LOGGER.error("Summon ability on {} failed",
                        AbilityCadence.entityIdOf(host.mob), throwable);
                host.caster.stop();
                host.passives.stop();
            }
            replayDeferred();
        }
        SUMMON_PASSIVES.values().removeIf(entry -> !entry.body().isAlive() || entry.body().isRemoved()
                || (entry.runner().idle() && !entry.runner().hasPassives()));
        for (SummonPassives entry : SUMMON_PASSIVES.values()) {
            if (!(entry.body().level() instanceof ServerLevel level)) {
                continue;
            }
            try {
                entry.runner().tick(level);
            } catch (Throwable throwable) {
                CombatStyles.LOGGER.error("Summon passive on {} failed",
                        AbilityCadence.entityIdOf(entry.body()), throwable);
                entry.runner().stop();
            }
            replayDeferred();
        }
    }

    private static void replayDeferred() {
        List<Deferred> waiting = DEFERRED.get();
        if (waiting.isEmpty()) {
            return;
        }
        List<Deferred> replaying = List.copyOf(waiting);
        waiting.clear();
        for (Deferred deferred : replaying) {
            react(deferred.summon(), deferred.on(), deferred.other());
        }
    }

    public static void react(Entity summon, PassiveTrigger on, LivingEntity other) {
        if (summon == null || on == null || summon.level().isClientSide()) {
            return;
        }
        if (RUNNING.get()) {
            DEFERRED.get().add(new Deferred(summon, on, other));
            return;
        }
        MobHost host = HOSTS.get(summon.getUUID());
        if (host != null) {
            if (host.owner() != null) {
                host.passives.react(on, other);
            }
            return;
        }
        if (summon instanceof ConfigurableSummon configurable) {
            PassiveRunner runner = summonRunner(configurable);
            if (runner != null) {
                runner.react(on, other);
            }
        }
    }

    private static PassiveRunner summonRunner(ConfigurableSummon summon) {
        Entity body = (Entity) summon;
        Player owner = SummonOwner.ownerPlayer(body);
        if (owner == null) {
            return null;
        }
        SummonPassives known = SUMMON_PASSIVES.get(body.getUUID());
        if (known != null && known.owner() == owner) {
            return known.runner();
        }
        PassiveRunner runner = new PassiveRunner(AbilityHost.of(summon, owner));
        if (!runner.hasPassives()) {
            return null;
        }
        SUMMON_PASSIVES.put(body.getUUID(), new SummonPassives(summon, owner, runner));
        return runner;
    }

    public static int beginFinale(Entity entity) {
        MobHost host = entity == null ? null : HOSTS.get(entity.getUUID());
        if (host == null || host.caster.finaleSpent()) {
            return NO_FINALE;
        }
        host.caster.spendFinale();
        SummonProfile.Resolved resolved = AbilityCadence.profileOf(host);
        String finale = resolved == null ? null : resolved.finale();
        if (finale == null || !AbilityRegistry.has(finale)) {
            return NO_FINALE;
        }
        return host.begin(finale);
    }

    public static void forget(Entity entity) {
        if (entity != null) {
            HOSTS.remove(entity.getUUID());
            SUMMON_PASSIVES.remove(entity.getUUID());
        }
    }

    public static void clear() {
        HOSTS.clear();
        SUMMON_PASSIVES.clear();
    }

    private static float grantDamageOf(LivingEntity mob) {
        return mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)
                ? (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE)
                : NO_DAMAGE;
    }

    private static final class MobHost implements AbilityHost {

        private final LivingEntity mob;
        private final UUID ownerId;
        private final float damage;
        private final boolean grantOnly;
        private final AbilityCaster caster = new AbilityCaster();
        private final PassiveRunner passives = new PassiveRunner(this);
        private LivingEntity target;

        private MobHost(LivingEntity mob, UUID ownerId, float damage, boolean grantOnly) {
            this.mob = mob;
            this.ownerId = ownerId;
            this.damage = damage;
            this.grantOnly = grantOnly;
        }

        private boolean alive() {
            return mob.isAlive() && !mob.isRemoved();
        }

        private boolean expired() {
            return grantOnly && !caster.acting() && passives.idle() && grants().isEmpty();
        }

        private void tick() {
            if (!(mob.level() instanceof ServerLevel level) || owner() == null) {
                return;
            }
            passives.tick(level);
            if (caster.acting()) {
                run(level);
                return;
            }
            AbilityCadence.Recast recast = AbilityCadence.next(this,
                    id -> AbilityRegistry.get(id) instanceof ActiveAbility);
            if (recast != null) {
                retarget(recast.target());
                begin(recast.ability());
            }
        }

        private int begin(String ability) {
            AbilityConfig.Entry tuned = tuned(ability);
            int duration = Math.max(1, tuned.duration());
            caster.begin(ability, duration, tuned.cooldown(), mob.level().getGameTime(),
                    mob.position());
            return duration;
        }

        private void run(ServerLevel level) {
            String ability = caster.current();
            SummonAbility behaviour = AbilityRegistry.get(ability);
            if (behaviour != null) {
                AbilityConfig.Entry tuned = tuned(ability);
                float amount = tuned.damageFrom(owner(), damage);
                if (caster.elapsed() == 0) {
                    CombatDebug.note(owner(), AbilityCadence.SUMMON_CAST + " — " + ability
                            + " runs with damage " + amount);
                }
                guarded(() -> behaviour.run(level, this, caster.elapsed(), amount, tuned));
            }
            if (behaviour == null || caster.advance()) {
                caster.stop();
            }
        }

        private AbilityConfig.Entry tuned(String ability) {
            SummonProfile.Resolved resolved = AbilityCadence.profileOf(this);
            return resolved == null
                    ? caster.tuned(ability, AbilityConfig.get(ability))
                    : resolved.tuningFor(ability, caster);
        }

        @Override
        public List<AbilityGrant> grants() {
            return AbilityGrants.grantsFor(owner(), mob);
        }

        @Override
        public boolean grantOnly() {
            return grantOnly;
        }

        @Override
        public Entity body() {
            return mob;
        }

        @Override
        public void holdAt(Vec3 point) {
            mob.teleportTo(point.x, point.y, point.z);
            mob.setDeltaMovement(Vec3.ZERO);
            mob.resetFallDistance();
        }

        @Override
        public Player owner() {
            return mob.level().getPlayerByUUID(ownerId);
        }

        @Override
        public LivingEntity target() {
            if (mob instanceof Mob hunter && hunter.getTarget() != null
                    && hunter.getTarget().isAlive()) {
                return hunter.getTarget();
            }
            return target != null && target.isAlive() ? target : null;
        }

        @Override
        public void retarget(LivingEntity next) {
            target = next;
        }

        @Override
        public float baseDamage() {
            return damage;
        }

        @Override
        public AbilityTuning orderTuning() {
            return null;
        }

        @Override
        public AbilityCaster caster() {
            return caster;
        }
    }
}

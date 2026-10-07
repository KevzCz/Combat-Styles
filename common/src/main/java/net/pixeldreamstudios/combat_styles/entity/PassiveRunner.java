package net.pixeldreamstudios.combat_styles.entity;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.equipment.AbilityGrant;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.SummonPassive;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PassiveRunner {

    private static final long READY = 0L;
    private static final int MIN_DURATION = 1;
    private static final int NO_SLOT = 0;

    private final AbilityHost base;
    private final List<Run> runs = new ArrayList<>();
    private final Map<Integer, Long> readyAt = new HashMap<>();
    private final AbilityCaster cooldowns = new AbilityCaster();

    PassiveRunner(AbilityHost base) {
        this.base = base;
    }

    boolean idle() {
        return runs.isEmpty();
    }

    boolean hasPassives() {
        return !passivesOf().isEmpty();
    }

    void react(PassiveTrigger on, LivingEntity other) {
        Entity body = base.body();
        if (body == null || base.owner() == null) {
            return;
        }
        List<SummonPassive> passives = passivesOf();
        long now = body.level().getGameTime();
        String entityId = AbilityCadence.entityIdOf(body);
        for (int index = 0; index < passives.size(); index++) {
            SummonPassive passive = passives.get(index);
            float odds = passive.odds();
            if (passive.trigger() != on || !AbilityRegistry.passive(passive.ability())
                    || !AbilityRegistry.usableBy(passive.ability(), entityId)
                    || now < readyAt.getOrDefault(index, READY)
                    || !cooldowns.ready(passive.ability(), now)
                    || (odds < SummonPassive.ALWAYS && body.level().getRandom().nextFloat() >= odds)) {
                continue;
            }
            start(index, passive, on, other, now);
        }
    }

    void tick(ServerLevel level) {
        if (runs.isEmpty()) {
            return;
        }
        List<Run> stepping = List.copyOf(runs);
        for (Run run : stepping) {
            if (run.step(level)) {
                runs.remove(run);
            }
        }
    }

    void stop() {
        for (Run run : runs) {
            run.caster.stop();
        }
        runs.clear();
    }

    private List<SummonPassive> passivesOf() {
        SummonProfile.Resolved resolved = AbilityCadence.profileOf(base);
        List<SummonPassive> passives = new ArrayList<>(resolved == null ? List.of() : resolved.passives());
        for (AbilityGrant grant : base.grants()) {
            passives.addAll(grant.passives());
        }
        return passives;
    }

    private void start(int index, SummonPassive passive, PassiveTrigger on, LivingEntity other, long now) {
        if (passive.cooldown() > SummonPassive.NO_COOLDOWN) {
            readyAt.put(index, now + passive.cooldown());
        }
        Run run = new Run(other, on, passive.options().orElse(null));
        AbilityCaster bound = base.caster();
        run.caster.bind(bound == null ? null : bound.profileKey(), bound == null ? NO_SLOT : bound.slot());
        run.caster.useEntry(passive.tuning(), passive.ability());
        AbilityConfig.Entry tuned = run.tuned(passive.ability());
        int duration = Math.max(MIN_DURATION, tuned.duration());
        run.caster.begin(passive.ability(), duration, tuned.cooldown(), now, base.body().position());
        if (tuned.cooldown() > 0) {
            cooldowns.holdUntil(passive.ability(), now + duration + tuned.cooldown());
        }
        runs.add(run);
    }

    private final class Run implements AbilityHost {

        private final AbilityCaster caster = new AbilityCaster();
        private final PassiveTrigger trigger;
        private final JsonObject options;
        private LivingEntity target;

        private Run(LivingEntity target, PassiveTrigger trigger, JsonObject options) {
            this.target = target;
            this.trigger = trigger;
            this.options = options;
        }

        private boolean step(ServerLevel level) {
            String ability = caster.current();
            SummonAbility behaviour = AbilityRegistry.get(ability);
            Player owner = owner();
            if (behaviour != null && owner != null) {
                AbilityConfig.Entry tuned = tuned(ability);
                float amount = tuned.damageFrom(owner, baseDamage());
                AbilityHosts.guarded(() -> behaviour.run(level, this, caster.elapsed(), amount, tuned));
            }
            boolean done = behaviour == null || owner == null || caster.advance();
            if (done) {
                caster.stop();
            }
            return done;
        }

        private AbilityConfig.Entry tuned(String ability) {
            SummonProfile.Resolved resolved = AbilityCadence.profileOf(base);
            AbilityConfig.Entry entry = resolved == null
                    ? caster.tuned(ability, AbilityConfig.get(ability))
                    : resolved.tuningFor(ability, caster);
            if (options == null) {
                return entry;
            }
            AbilityConfig.Entry merged = entry.copy();
            merged.mergeOptions(options);
            return merged;
        }

        @Override
        public Entity body() {
            return base.body();
        }

        @Override
        public Player owner() {
            return base.owner();
        }

        @Override
        public LivingEntity target() {
            boolean usable = target != null && (target.isAlive() || trigger == PassiveTrigger.KILL);
            return usable ? target : base.target();
        }

        @Override
        public void retarget(LivingEntity next) {
            target = next;
        }

        @Override
        public float baseDamage() {
            return base.baseDamage();
        }

        @Override
        public AbilityTuning orderTuning() {
            return null;
        }

        @Override
        public AbilityCaster caster() {
            return caster;
        }

        @Override
        public Vec3 origin() {
            return base.origin();
        }

        @Override
        public void holdAt(Vec3 point) {
            base.holdAt(point);
        }

        @Override
        public void showBeam(Vec3 end) {
            base.showBeam(end);
        }

        @Override
        public boolean drawsOwnBeam() {
            return base.drawsOwnBeam();
        }

        @Override
        public List<AbilityGrant> grants() {
            return base.grants();
        }

        @Override
        public boolean grantOnly() {
            return base.grantOnly();
        }
    }
}

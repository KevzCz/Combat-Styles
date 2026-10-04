package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class SummonOwnerTargetGoal extends TargetGoal {

    private record Lead(LivingEntity target, int age, Runnable seen) {
    }

    private static final int MAX_LEAD_AGE = 200;

    private int seenAssist = Integer.MIN_VALUE;
    private int seenDefend = Integer.MIN_VALUE;
    private int seenRetaliate = Integer.MIN_VALUE;
    private Lead chosen;

    public SummonOwnerTargetGoal(Mob mob) {
        super(mob, false);
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        chosen = freshest();
        return chosen != null;
    }

    @Override
    public void start() {
        follow(chosen);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        Lead fresh = freshest();
        if (fresh != null && fresh.target() != mob.getTarget()) {
            follow(fresh);
        }
        return super.canContinueToUse();
    }

    @Override
    public void stop() {
        LivingEntity chased = targetMob;
        super.stop();
        if (chased != null && mob.getTarget() == chased) {
            SummonBrains.setTarget(mob, null);
        }
    }

    private void follow(Lead lead) {
        if (lead == null) {
            return;
        }
        lead.seen().run();
        SummonBrains.setTarget(mob, lead.target());
        targetMob = lead.target();
    }

    private Lead freshest() {
        Player owner = SummonOwner.ownerPlayer(mob);
        if (owner == null) {
            return null;
        }
        Lead best = null;
        int assistStamp = owner.getLastHurtMobTimestamp();
        if (assistStamp != seenAssist) {
            best = newer(best, valid(owner.getLastHurtMob(), owner.tickCount - assistStamp,
                    () -> seenAssist = assistStamp));
        }
        int defendStamp = owner.getLastHurtByMobTimestamp();
        if (defendStamp != seenDefend) {
            best = newer(best, valid(owner.getLastHurtByMob(), owner.tickCount - defendStamp,
                    () -> seenDefend = defendStamp));
        }
        int retaliateStamp = mob.getLastHurtByMobTimestamp();
        if (retaliateStamp != seenRetaliate) {
            best = newer(best, valid(mob.getLastHurtByMob(), mob.tickCount - retaliateStamp,
                    () -> seenRetaliate = retaliateStamp));
        }
        return best;
    }

    private Lead valid(LivingEntity candidate, int age, Runnable seen) {
        if (candidate == null || age > MAX_LEAD_AGE || !SummonAi.huntable(mob, candidate)
                || !canAttack(candidate, TargetingConditions.DEFAULT)) {
            return null;
        }
        return new Lead(candidate, age, seen);
    }

    private static Lead newer(Lead current, Lead candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.age() < current.age() ? candidate : current;
    }
}

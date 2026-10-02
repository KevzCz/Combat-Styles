package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.EntityGetter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.pixeldreamstudios.combat_styles.entity.SummonAi;
import net.pixeldreamstudios.combat_styles.entity.SummonData;

import java.util.List;
import java.util.UUID;

@Mixin(Mob.class)
public abstract class SummonableMobMixin implements SummonData, OwnableEntity {

    @Unique
    private static final int SUMMON_TARGET_PRIORITY = 1;

    @Unique
    private static final int SUMMON_TARGET_INTERVAL = 10;

    @Shadow
    @Final
    protected GoalSelector targetSelector;

    @Override
    public EntityGetter level() {
        return ((Mob) (Object) this).level();
    }

    @Override
    public UUID getOwnerUUID() {
        return combat_styles$summonOwner();
    }

    @Override
    public LivingEntity getOwner() {
        Mob self = (Mob) (Object) this;
        UUID owner = combat_styles$summonOwner();
        return owner == null ? null : self.level().getPlayerByUUID(owner);
    }

    @Override
    public void combat_styles$applySummonGoals() {
        Mob self = (Mob) (Object) this;
        List<Goal> retaliation = targetSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(goal -> goal instanceof HurtByTargetGoal)
                .toList();
        for (Goal goal : retaliation) {
            targetSelector.removeGoal(goal);
        }
        targetSelector.addGoal(SUMMON_TARGET_PRIORITY,
                new NearestAttackableTargetGoal<>(self, LivingEntity.class,
                        SUMMON_TARGET_INTERVAL, true, false,
                        living -> SummonAi.huntable(self, living)));
    }
}

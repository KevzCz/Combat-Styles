package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public final class SummonBrains {

    private static final long DIG_HOLD_TICKS = 1200L;
    private static final int MAX_CALMED_PER_TICK = 4;

    private SummonBrains() {
    }

    private static boolean registered(Mob mob, MemoryModuleType<?> memory) {
        return mob.getBrain().checkMemory(memory, MemoryStatus.REGISTERED);
    }

    public static boolean targetsByBrain(Mob mob) {
        return registered(mob, MemoryModuleType.ATTACK_TARGET);
    }

    public static void setTarget(Mob mob, LivingEntity target) {
        mob.setTarget(target);
        if (!targetsByBrain(mob)) {
            return;
        }
        Brain<?> brain = mob.getBrain();
        if (target == null) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } else {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
            rile(mob, target);
        }
    }

    public static Optional<LivingEntity> chosenTarget(Mob mob) {
        if (!targetsByBrain(mob)) {
            return Optional.empty();
        }
        return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                .filter(target -> SummonAi.huntable(mob, target));
    }

    private static void rile(Mob mob, LivingEntity target) {
        if (mob instanceof Warden warden && !warden.getAngerLevel().isAngry()) {
            warden.increaseAngerAt(target, AngerLevel.ANGRY.getMinimumAnger(), false);
        }
    }

    private static void calm(Mob mob) {
        if (!(mob instanceof Warden warden)) {
            return;
        }
        for (int calmed = 0; calmed < MAX_CALMED_PER_TICK; calmed++) {
            Optional<LivingEntity> angry = warden.getEntityAngryAt();
            if (angry.isEmpty() || SummonAi.huntable(mob, angry.get())) {
                return;
            }
            warden.clearAnger(angry.get());
        }
    }

    public static void holdDigging(Mob mob) {
        if (registered(mob, MemoryModuleType.DIG_COOLDOWN)) {
            mob.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE,
                    DIG_HOLD_TICKS);
        }
    }

    public static void walkTo(Mob mob, Entity destination, double speed, int closeEnough) {
        if (registered(mob, MemoryModuleType.WALK_TARGET)) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(destination, (float) speed, closeEnough));
            return;
        }
        mob.getNavigation().moveTo(destination, speed);
    }

    public static void stopWalking(Mob mob) {
        if (registered(mob, MemoryModuleType.WALK_TARGET)) {
            mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
        mob.getNavigation().stop();
    }

    public static void forgetForbiddenTargets(Mob mob) {
        if (!targetsByBrain(mob)) {
            return;
        }
        holdDigging(mob);
        calm(mob);
        Brain<?> brain = mob.getBrain();
        Optional<LivingEntity> target = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
        if (target.isPresent() && !SummonAi.huntable(mob, target.get())) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } else {
            target.ifPresent(chased -> rile(mob, chased));
        }
        if (!registered(mob, MemoryModuleType.ANGRY_AT)) {
            return;
        }
        Optional<UUID> angry = brain.getMemory(MemoryModuleType.ANGRY_AT);
        if (angry.isEmpty()) {
            return;
        }
        Player owner = SummonOwner.ownerPlayer(mob);
        Entity grudge = owner == null ? null : findLiving(mob, angry.get());
        if (owner == null || angry.get().equals(owner.getUUID())
                || (grudge instanceof LivingEntity living && !SummonAi.huntable(mob, living))) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }

    private static Entity findLiving(Mob mob, UUID id) {
        Player player = mob.level().getPlayerByUUID(id);
        if (player != null) {
            return player;
        }
        return mob.level() instanceof ServerLevel server
                ? server.getEntity(id)
                : null;
    }
}

package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class SummonFollowOwnerGoal extends Goal {

    private static final double SPEED = 1.2D;
    private static final int ARRIVAL_SLACK = 1;
    private static final int REPATH_TICKS = 10;
    private static final int TELEPORT_TRIES = 10;
    private static final int TELEPORT_SPREAD = 3;
    private static final int TELEPORT_MIN = 2;
    private static final double BLOCK_CENTER = 0.5D;

    private final Mob mob;
    private Player owner;
    private int repath;

    public SummonFollowOwnerGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private SummonFollow follow() {
        return mob instanceof SummonData data ? data.combat_styles$follow() : SummonFollow.DEFAULTS;
    }

    private boolean pastTeleport(double distanceSqr) {
        SummonFollow follow = follow();
        return follow.teleports() && distanceSqr >= follow.teleport() * follow.teleport();
    }

    private boolean sitting() {
        return mob instanceof TamableAnimal tamable && tamable.isOrderedToSit();
    }

    @Override
    public boolean canUse() {
        Player found = SummonOwner.ownerPlayer(mob);
        if (found == null || found.isSpectator() || !found.isAlive() || sitting()) {
            return false;
        }
        double distance = mob.distanceToSqr(found);
        if (fighting() && !pastTeleport(distance)) {
            return false;
        }
        double start = follow().start();
        if (distance < start * start) {
            return false;
        }
        owner = found;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || !owner.isAlive() || sitting()) {
            return false;
        }
        double distance = mob.distanceToSqr(owner);
        if (pastTeleport(distance)) {
            return true;
        }
        double stop = follow().stop();
        return !fighting() && distance > stop * stop;
    }

    private boolean fighting() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        owner = null;
        SummonBrains.stopWalking(mob);
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }
        mob.getLookControl().setLookAt(owner, mob.getMaxHeadYRot(), mob.getMaxHeadXRot());
        if (--repath > 0) {
            return;
        }
        repath = adjustedTickDelay(REPATH_TICKS);
        if (pastTeleport(mob.distanceToSqr(owner))) {
            teleportNearOwner();
            return;
        }
        SummonBrains.walkTo(mob, owner, SPEED,
                (int) Math.ceil(follow().stop()) + ARRIVAL_SLACK);
    }

    private void teleportNearOwner() {
        SummonBrains.setTarget(mob, null);
        BlockPos origin = owner.blockPosition();
        for (int attempt = 0; attempt < TELEPORT_TRIES; attempt++) {
            int x = origin.getX() + offset();
            int y = origin.getY() + mob.getRandom().nextInt(TELEPORT_MIN + 1) - 1;
            int z = origin.getZ() + offset();
            if (Math.abs(x - origin.getX()) < TELEPORT_MIN
                    && Math.abs(z - origin.getZ()) < TELEPORT_MIN) {
                continue;
            }
            if (canStandAt(new BlockPos(x, y, z))) {
                mob.moveTo(x + BLOCK_CENTER, y, z + BLOCK_CENTER, mob.getYRot(), mob.getXRot());
                SummonBrains.stopWalking(mob);
                return;
            }
        }
    }

    private int offset() {
        return mob.getRandom().nextInt(TELEPORT_SPREAD * 2 + 1) - TELEPORT_SPREAD;
    }

    private boolean canStandAt(BlockPos pos) {
        if (!mob.level().getBlockState(pos.below()).isFaceSturdy(mob.level(), pos.below(),
                Direction.UP)) {
            return false;
        }
        return mob.level().noCollision(mob,
                mob.getBoundingBox().move(pos.getX() + BLOCK_CENTER - mob.getX(),
                        pos.getY() - mob.getY(), pos.getZ() + BLOCK_CENTER - mob.getZ()));
    }
}

package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;
import net.pixeldreamstudios.combat_styles.api.event.SummonEvents;
import net.pixeldreamstudios.combat_styles.config.SummonTuning;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;

import java.util.List;

public final class SummonOrders {

    public record Result(String ability, int commanded, int available) {

        public static final Result NONE = new Result("", 0, 0);

        public boolean reachedAny() {
            return commanded > 0;
        }

        public boolean reachedAll() {
            return available > 0 && commanded == available;
        }
    }

    private SummonOrders() {
    }

    public static String abilityFor(Player player, SummonProfile profile,
                                    String declared, int slot) {
        if (declared != null && !declared.isBlank()) {
            return declared;
        }
        if (profile != null) {
            String fromProfile = SummonProfiles.resolvedFor(player).abilityFor(slot);
            if (fromProfile != null) {
                return fromProfile;
            }
        }
        return null;
    }

    public static int command(Player player, LivingEntity target, String ability, float damage) {
        return command(player, target, ability, damage, SummonTargeting.ALL, null);
    }

    public static int command(Player player, LivingEntity target, String ability,
                              float damage, SummonTargeting targeting) {
        return command(player, target, ability, damage, targeting, null);
    }

    public static int command(Player player, LivingEntity target, String ability,
                              float damage, SummonTargeting targeting, AbilityTuning tuning) {
        return issue(player, target, ability, damage, targeting, tuning).commanded();
    }

    public static Result issue(Player player, LivingEntity target, String ability,
                               float damage, SummonTargeting targeting, AbilityTuning tuning) {
        if (player == null || ability == null || ability.isBlank()
                || player.level().isClientSide()) {
            return Result.NONE;
        }
        if (!AbilityRegistry.has(ability)) {
            return new Result(ability, 0, 0);
        }
        SummonTargeting mode = targeting == null ? SummonTargeting.ALL : targeting;
        List<ConfigurableSummon> free = SummonRoster.available(player, mode);
        if (free.isEmpty()) {
            return new Result(ability, 0, 0);
        }
        if (mode.commandsOne()) {
            boolean reached = commandOne(player, pick(player, target, free, mode),
                    target, ability, damage, tuning);
            return new Result(ability, reached ? 1 : 0, 1);
        }
        int limit = Math.max(1, SummonTuning.get().maxCommandedPerOrder);
        int reachable = Math.min(limit, free.size());
        int commanded = 0;
        int attempted = 0;
        for (ConfigurableSummon summon : free) {
            if (attempted >= limit) {
                break;
            }
            attempted++;
            if (commandOne(player, summon, target, ability, damage, tuning)) {
                commanded++;
            }
        }
        return new Result(ability, commanded, reachable);
    }

    public static int commandAll(Player player, LivingEntity target, String ability,
                                 float damage) {
        return command(player, target, ability, damage, SummonTargeting.ALL, null);
    }

    private static ConfigurableSummon pick(Player player, LivingEntity target,
                                           List<ConfigurableSummon> free, SummonTargeting mode) {
        if (mode == SummonTargeting.RANDOM) {
            return free.get(player.getRandom().nextInt(free.size()));
        }
        if (mode != SummonTargeting.NEAREST) {
            return free.getFirst();
        }
        Vec3 from = target == null ? player.position() : target.position();
        ConfigurableSummon closest = null;
        double best = Double.MAX_VALUE;
        for (ConfigurableSummon summon : free) {
            if (!(summon instanceof Entity entity)) {
                continue;
            }
            double distance = entity.position().distanceToSqr(from);
            if (distance < best || (distance == best && closest instanceof Entity held
                    && entity.getId() < held.getId())) {
                best = distance;
                closest = summon;
            }
        }
        return closest == null ? free.getFirst() : closest;
    }

    public static boolean commandOne(Player player, ConfigurableSummon summon,
                                     LivingEntity target, String ability, float damage,
                                     AbilityTuning tuning) {
        if (summon == null || ability == null || ability.isBlank()) {
            return false;
        }
        if (!SummonEvents.allowOrder(player, summon, target, ability, damage)) {
            return false;
        }
        summon.order(player, target, damage, ability, tuning);
        SummonEvents.fireActed(player, summon, ability, damage);
        return true;
    }
}

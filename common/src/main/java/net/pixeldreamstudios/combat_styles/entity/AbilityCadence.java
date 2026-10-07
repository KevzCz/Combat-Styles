package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.AbilityGrant;
import net.pixeldreamstudios.combat_styles.equipment.AbilityUse;
import net.pixeldreamstudios.combat_styles.equipment.SummonCadence;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AbilityCadence {

    public record Recast(String ability, LivingEntity target) {
    }

    public static final String SUMMON_CAST = "summon cast";

    private AbilityCadence() {
    }

    public static SummonProfile.Resolved profileOf(AbilityHost host) {
        if (host.grantOnly()) {
            return null;
        }
        AbilityCaster caster = host.caster();
        return SummonProfiles.resolvedFor(host.owner(), caster == null ? null : caster.profileKey(),
                entityIdOf(host.body()));
    }

    public static Recast next(AbilityHost host) {
        return next(host, ability -> true);
    }

    public static Recast next(AbilityHost host, Predicate<String> runnable) {
        AbilityCaster caster = host.caster();
        Player owner = host.owner();
        if (caster == null || owner == null || caster.acting()) {
            return null;
        }
        SummonProfile.Resolved resolved = profileOf(host);
        List<AbilityGrant> grants = host.grants();
        if (resolved == null && grants.isEmpty()) {
            return null;
        }
        int interval = recastOf(resolved, grants);
        long now = host.body().level().getGameTime();
        if (!caster.recastDue(now, interval)) {
            return null;
        }
        caster.scheduleRecast(now, interval);
        String entityId = entityIdOf(host.body());
        List<AbilityUse> pool = new ArrayList<>(resolved == null ? List.of() : resolved.abilityUses());
        for (AbilityGrant grant : grants) {
            pool.addAll(grant.actives());
        }
        List<String> ids = pool.stream().map(AbilityUse::ability).toList();
        int index = pickOf(resolved, grants).pickIndex(ids, caster.slot(), caster.nextStep(),
                host.body().level().getRandom(),
                candidate -> AbilityRegistry.has(candidate)
                        && runnable.test(candidate)
                        && AbilityRegistry.usableBy(candidate, entityId)
                        && caster.ready(candidate, now));
        if (index == AbilityPick.NONE_PICKED) {
            CombatDebug.skipped(owner, SUMMON_CAST, entityId + " has no ready ability in " + ids);
            return null;
        }
        AbilityUse picked = pool.get(index);
        String ability = picked.ability();
        SummonAim aim = aimOf(resolved, grants);
        LivingEntity target = aim.resolve(owner, host.body(), host.target());
        if (target == null && aim.targetsFoes()) {
            CombatDebug.skipped(owner, SUMMON_CAST, entityId + " " + ability
                    + " found no target for aim " + aim.getSerializedName());
            return null;
        }
        CombatDebug.fired(owner, SUMMON_CAST, entityId + " " + ability + " at "
                + (target == null ? "nothing" : entityIdOf(target)));
        caster.useEntry(picked.tuning(), ability, resolved == null ? ability : resolved.swap(ability));
        return new Recast(ability, target);
    }

    private static int recastOf(SummonProfile.Resolved resolved, List<AbilityGrant> grants) {
        int declared = resolved == null ? SummonCadence.NEVER_RECASTS : resolved.recastTicks();
        if (declared > SummonCadence.NEVER_RECASTS) {
            return declared;
        }
        for (AbilityGrant grant : grants) {
            int granted = grant.cadence().recastTicks().orElse(SummonCadence.NEVER_RECASTS);
            if (granted > SummonCadence.NEVER_RECASTS) {
                return granted;
            }
        }
        return declared;
    }

    private static AbilityPick pickOf(SummonProfile.Resolved resolved, List<AbilityGrant> grants) {
        if (resolved != null) {
            return resolved.abilityPick();
        }
        for (AbilityGrant grant : grants) {
            if (grant.cadence().abilityPick().isPresent()) {
                return grant.cadence().abilityPick().get();
            }
        }
        return AbilityPick.IN_ORDER;
    }

    private static SummonAim aimOf(SummonProfile.Resolved resolved, List<AbilityGrant> grants) {
        if (resolved != null) {
            return resolved.aim();
        }
        for (AbilityGrant grant : grants) {
            if (grant.cadence().aim().isPresent()) {
                return grant.cadence().aim().get();
            }
        }
        return SummonAim.HIT_TARGET;
    }

    public static String entityIdOf(Entity entity) {
        if (entity == null) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id == null ? "" : id.toString();
    }
}

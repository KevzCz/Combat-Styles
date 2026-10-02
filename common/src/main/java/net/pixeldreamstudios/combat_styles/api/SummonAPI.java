package net.pixeldreamstudios.combat_styles.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.entity.AbilityBehaviour;
import net.pixeldreamstudios.combat_styles.entity.AbilityParams;
import net.pixeldreamstudios.combat_styles.entity.AbilityRegistry;
import net.pixeldreamstudios.combat_styles.entity.ConfigurableSummon;
import net.pixeldreamstudios.combat_styles.entity.SummonOrders;
import net.pixeldreamstudios.combat_styles.entity.SummonRoster;
import net.pixeldreamstudios.combat_styles.entity.SummonTargeting;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;

import java.util.List;
import java.util.Map;

public final class SummonAPI {

    private SummonAPI() {
    }

    public static List<ConfigurableSummon> summonsOf(Player player) {
        return player == null ? List.of() : SummonRoster.of(player);
    }

    public static List<ConfigurableSummon> availableSummons(Player player, String targeting) {
        return player == null
                ? List.of()
                : SummonRoster.available(player, SummonTargeting.byName(targeting));
    }

    public static int summonCount(Player player) {
        return player == null ? 0 : SummonRoster.of(player).size();
    }

    public static int order(Player player, LivingEntity target, String ability, float damage) {
        return order(player, target, ability, damage, SummonTargeting.ALL.key());
    }

    public static int order(Player player, LivingEntity target, String ability,
                            float damage, String targeting) {
        return SummonOrders.command(player, target, ability, damage,
                SummonTargeting.byName(targeting));
    }

    public static int order(Player player, LivingEntity target, String ability,
                            float damage, String targeting, AbilityTuning tuning) {
        return SummonOrders.command(player, target, ability, damage,
                SummonTargeting.byName(targeting), tuning);
    }

    public static SummonOrders.Result issue(Player player, LivingEntity target,
                                            String ability, float damage, String targeting) {
        return SummonOrders.issue(player, target, ability, damage,
                SummonTargeting.byName(targeting), null);
    }

    public static void orderOne(Player player, ConfigurableSummon summon,
                                LivingEntity target, String ability, float damage) {
        SummonOrders.commandOne(player, summon, target, ability, damage, null);
    }

    public static int stopAll(Player player) {
        int stopped = 0;
        for (ConfigurableSummon summon : summonsOf(player)) {
            if (summon.acting()) {
                summon.stopActing();
                stopped++;
            }
        }
        return stopped;
    }

    public static int dismissAll(Player player) {
        int dismissed = 0;
        for (ConfigurableSummon summon : summonsOf(player)) {
            summon.dismiss();
            dismissed++;
        }
        return dismissed;
    }

    public static AbilityConfig.Entry abilityOf(Player player, String ability) {
        SummonProfile.Resolved resolved = player == null ? null : SummonProfiles.resolvedFor(player);
        return resolved == null ? AbilityConfig.get(ability) : resolved.tuningFor(ability);
    }

    public static AbilityConfig.Entry abilityDefault(String ability) {
        return AbilityConfig.get(ability);
    }

    public static void registerAbility(String ability, AbilityConfig.Entry defaults,
                                       AbilityBehaviour behaviour) {
        AbilityConfig.define(ability, defaults);
        AbilityRegistry.register(ability, behaviour);
    }

    public static void describeParam(String option, String display) {
        AbilityParams.display(option, display);
    }

    public static String formatParam(String option, double value) {
        return AbilityParams.format(option, value);
    }

    public static Map<String, Double> paramsOf(Player player, String ability) {
        return AbilityParams.of(abilityOf(player, ability));
    }

    public static List<String> registeredAbilities() {
        return AbilityRegistry.registered();
    }

    public static List<String> targetingModes() {
        return SummonTargeting.keys();
    }
}

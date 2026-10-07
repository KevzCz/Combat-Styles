package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.OptionField;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;

import java.util.List;
import java.util.Optional;

public final class RaiseDead {

    public static final String ABILITY = CombatStyles.MOD_ID + ":raise_dead";
    private static final int ONE = 1;
    private static final float AT_VICTIM = 0.0F;
    private static final float NO_DAMAGE = 0.0F;
    private static final List<String> TUNABLE = List.of(SummonAbility.TUNE_OPTIONS, "timing/cooldown_ticks");
    private static final int SAMPLE_MAX_ACTIVE = 3;
    private static final List<OptionField> OPTIONS = List.of(
            OptionField.of(MobFilter.KEY_ENTITIES, Codec.STRING.listOf(), List.of()),
            OptionField.of(MobFilter.KEY_ENTITY_TAGS, Codec.STRING.listOf(), List.of()),
            OptionField.of(MobFilter.KEY_EXEMPT_ENTITIES, Codec.STRING.listOf(), List.of()),
            OptionField.of(MobFilter.KEY_EXEMPT_TAGS, Codec.STRING.listOf(), List.of()),
            OptionField.of(RaiseRules.KEY_LIFESPAN_TICKS, Codec.INT, RaiseRules.DEFAULT_LIFESPAN),
            OptionField.of(RaiseRules.KEY_KEEP_GEAR, Codec.BOOL, RaiseRules.DEFAULT.keepGear()),
            OptionField.of(RaiseRules.KEY_SPECTRAL, Codec.BOOL, RaiseRules.DEFAULT.spectral()),
            OptionField.of(RaiseRules.KEY_MAX_ACTIVE, Codec.INT, SAMPLE_MAX_ACTIVE),
            OptionField.of(RaiseRules.KEY_CHANCE, Codec.FLOAT, RaiseRules.ALWAYS),
            OptionField.of(RaiseRules.KEY_PROFILE, RaiseProfile.CODEC, RaiseRules.DEFAULT.profile()),
            OptionField.of(RaiseRules.KEY_PROFILES, RaiseRules.PROFILES_CODEC, List.of(), SummonProfile.SCHEMA));

    private RaiseDead() {
    }

    public static boolean raise(ServerLevel level, Player owner, LivingEntity victim, RaiseRules rules) {
        if (level == null || owner == null || !(victim instanceof Mob) || !victim.isDeadOrDying()
                || SummonOwner.isSummon(victim) || !SummonSpawner.raisable(victim.getType())
                || !rules.filter().accepts(victim.getType())) {
            return false;
        }
        AffixEffects.Summon raised = new AffixEffects.Summon(AffixEffects.Summon.VICTIM, ONE,
                rules.lifespanTicks(), AT_VICTIM, false, NO_DAMAGE, Optional.empty(), Optional.empty(),
                rules.chance(), Optional.empty(), rules.maxActive(), SummonEquipment.NONE,
                SummonStats.NONE, rules.spectral(), rules.keepGear(), true);
        SummonSpawner.applySummons(level, owner, victim, List.of(raised), NO_DAMAGE, rules);
        return true;
    }

    public static void register() {
        AbilityRegistry.register(ABILITY, new Ability());
        AbilityConfig.define(ABILITY, defaults());
    }

    private static AbilityConfig.Entry defaults() {
        AbilityConfig.Entry entry = new AbilityConfig.Entry();
        entry.useOptions(RaiseRules.CODEC, RaiseRules.DEFAULT);
        return entry;
    }

    private static final class Ability implements PassiveAbility {

        @Override
        public PassiveTrigger on() {
            return PassiveTrigger.KILL;
        }

        @Override
        public void run(ServerLevel level, AbilityHost host, int elapsed, float damage,
                        AbilityConfig.Entry tuned) {
            raise(level, host.owner(), host.target(),
                    tuned.options(RaiseRules.CODEC, RaiseRules.DEFAULT));
        }

        @Override
        public List<String> tunable() {
            return TUNABLE;
        }

        @Override
        public List<OptionField> options() {
            return OPTIONS;
        }
    }
}

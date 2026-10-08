package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Entity;

import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.entity.MobFilter;

import java.util.List;

public record AbilityGrant(GrantTarget appliesTo, MobFilter filter, SummonCadence cadence) {

    public static final String KEY = "grant_abilities";
    public static final String KEY_APPLIES_TO = "applies_to";

    public static final Codec<AbilityGrant> CODEC = Customs.wrap(AbilityGrant.class, RecordCodecBuilder.create(instance -> instance.group(
            GrantTarget.CODEC.optionalFieldOf(KEY_APPLIES_TO, GrantTarget.BOTH).forGetter(AbilityGrant::appliesTo),
            MobFilter.MAP_CODEC.forGetter(AbilityGrant::filter),
            SummonCadence.MAP_CODEC.forGetter(AbilityGrant::cadence)
    ).apply(instance, AbilityGrant::new)));

    public static final AbilityGrant EMPTY = new AbilityGrant(GrantTarget.BOTH, MobFilter.ANY, SummonCadence.NONE);

    public AbilityGrant {
        appliesTo = appliesTo == null ? GrantTarget.BOTH : appliesTo;
        filter = filter == null ? MobFilter.ANY : filter;
        cadence = cadence == null ? SummonCadence.NONE : cadence;
    }

    public boolean reaches(Entity mob, boolean summon) {
        return mob != null && appliesTo.accepts(summon) && filter.accepts(mob.getType());
    }

    public List<AbilityUse> actives() {
        return cadence.uses();
    }

    public List<SummonPassive> passives() {
        return cadence.passives();
    }
}

package net.pixeldreamstudios.combat_styles.damage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.List;

public record DamageBonusList(List<DamageBonus> damageBonus) {

    public static final DamageBonusList EMPTY = new DamageBonusList(List.of());

    public static final Codec<DamageBonusList> CODEC = Customs.wrap(DamageBonusList.class, RecordCodecBuilder.create(
            instance -> instance.group(
                    DamageBonus.LIST_CODEC.optionalFieldOf(DamageBonus.KEY, List.of())
                            .forGetter(DamageBonusList::damageBonus)
            ).apply(instance, DamageBonusList::new)));

    public DamageBonusList {
        damageBonus = damageBonus == null ? List.of() : List.copyOf(damageBonus);
    }
}

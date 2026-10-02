package net.pixeldreamstudios.combat_styles.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.equipment.SpellTuning;

import java.util.Locale;
import java.util.Optional;

public record CastSpell(String spell, Target target, float chance, Optional<DamageSpec> damageSpec,
                        SpellTuning tuning, boolean ignoreIframes, String name,
                        int channelDuration) {

    public static final int SPELL_CHANNEL = 0;

    public enum Target implements StringRepresentable {
        AUTO,
        HIT,
        SELF,
        AIM;

        public static final Codec<Target> CODEC = StringRepresentable.fromEnum(Target::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<CastSpell> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("spell").forGetter(CastSpell::spell),
            Target.CODEC.optionalFieldOf("cast_target", Target.AUTO).forGetter(CastSpell::target),
            Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(CastSpell::chance),
            DamageSpec.CODEC.optionalFieldOf("damage_spec").forGetter(CastSpell::damageSpec),
            SpellTuning.CODEC.optionalFieldOf("tuning", SpellTuning.NONE).forGetter(CastSpell::tuning),
            Codec.BOOL.optionalFieldOf("ignore_iframes", false).forGetter(CastSpell::ignoreIframes),
            Codec.STRING.optionalFieldOf("name", "").forGetter(CastSpell::name),
            Codec.INT.optionalFieldOf("channel_duration", SPELL_CHANNEL)
                    .forGetter(CastSpell::channelDuration)
    ).apply(instance, CastSpell::new));

    public CastSpell {
        spell = spell == null ? "" : spell;
        target = target == null ? Target.AUTO : target;
        damageSpec = damageSpec == null ? Optional.empty() : damageSpec;
        tuning = tuning == null ? SpellTuning.NONE : tuning;
        name = name == null ? "" : name;
    }

    public Target resolvedTarget(boolean hasHit) {
        if (target != Target.AUTO) {
            return target == Target.HIT && !hasHit ? Target.AIM : target;
        }
        return hasHit ? Target.HIT : Target.AIM;
    }

    public boolean overridesChannel() {
        return channelDuration > SPELL_CHANNEL;
    }
}

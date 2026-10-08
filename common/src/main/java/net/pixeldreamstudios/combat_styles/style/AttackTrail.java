package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.Optional;

public record AttackTrail(Optional<String> color, boolean glows, boolean overridesAffixes) {

    public static final Codec<AttackTrail> CODEC = Customs.wrap(AttackTrail.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("color").forGetter(AttackTrail::color),
            Codec.BOOL.optionalFieldOf("glows", Boolean.FALSE).forGetter(AttackTrail::glows),
            Codec.BOOL.optionalFieldOf("overrides_affixes", Boolean.FALSE)
                    .forGetter(AttackTrail::overridesAffixes)
    ).apply(instance, AttackTrail::new)));

    public static final AttackTrail NONE =
            new AttackTrail(Optional.empty(), false, false);

    public boolean isEmpty() {
        return color.isEmpty() || color.get().isBlank();
    }

    public int rgba() {
        if (isEmpty()) {
            return 0;
        }
        String raw = color.get().trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        try {
            long parsed = Long.parseLong(raw, 16);
            return raw.length() <= 6
                    ? (int) (parsed << 8 | 0xFFL)
                    : (int) parsed;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}

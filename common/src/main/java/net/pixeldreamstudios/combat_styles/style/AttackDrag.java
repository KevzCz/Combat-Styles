package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

import java.util.List;

public record AttackDrag(boolean enabled, int ticks, int maxTargets, boolean trail) {

    public static final int DEFAULT_TICKS = 10;
    public static final int MAX_TICKS = 100;
    public static final int MAX_AUTHORED_TICKS = 40;
    public static final int DEFAULT_MAX_TARGETS = 8;
    public static final int MAX_TARGETS_LIMIT = 64;
    private static final String MOTION_DASH = "dash";
    private static final String MOTION_BLINK = "blink";

    public static final AttackDrag NONE =
            new AttackDrag(false, DEFAULT_TICKS, DEFAULT_MAX_TARGETS, true);

    public static final AttackDrag ENABLED =
            new AttackDrag(true, DEFAULT_TICKS, DEFAULT_MAX_TARGETS, true);

    public static final Codec<AttackDrag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE).forGetter(AttackDrag::enabled),
            Codec.INT.optionalFieldOf("ticks", DEFAULT_TICKS).forGetter(AttackDrag::ticks),
            Codec.INT.optionalFieldOf("max_targets", DEFAULT_MAX_TARGETS)
                    .forGetter(AttackDrag::maxTargets),
            Codec.BOOL.optionalFieldOf("trail", Boolean.TRUE).forGetter(AttackDrag::trail)
    ).apply(instance, AttackDrag::new));

    public AttackDrag {
        ticks = Math.clamp(ticks, 1, MAX_TICKS);
        maxTargets = Math.clamp(maxTargets, 1, MAX_TARGETS_LIMIT);
    }

    public static AttackDrag effective(AttackOverride override, List<StyleAffix> affixes) {
        AttackDrag declared = override == null ? NONE : override.dragOrDefault();
        int bonus = 0;
        if (override != null && grantsDrag(override.behaviourOrDefault().motionOrNone())) {
            bonus = CombatStylesConfig.get().motionDragWindowBonus();
        }
        if (bonus <= 0) {
            for (StyleAffix affix : affixes) {
                if (grantsDrag(affix.onSwing().mechanics().motion())
                        || grantsDrag(affix.onHit().mechanics().motion())) {
                    bonus = CombatStylesConfig.get().motionDragWindowBonus();
                    break;
                }
            }
        }
        if (!declared.enabled() && bonus <= 0) {
            return NONE;
        }
        AttackDrag base = declared.enabled() ? declared : ENABLED;
        return new AttackDrag(true, base.ticks() + bonus, base.maxTargets(), base.trail());
    }

    public static boolean grantsDrag(AffixEffects.Motion motion) {
        List<String> granting = CombatStylesConfig.get().dragGrantingMotions();
        return (motion.dash().isPresent() && granting.contains(MOTION_DASH))
                || (motion.blink().isPresent() && granting.contains(MOTION_BLINK));
    }

    public String signature() {
        return (enabled ? "1" : "0") + ticks + "x" + maxTargets + (trail ? "t" : "");
    }
}

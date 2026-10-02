package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;

import java.util.Collection;

public enum TooltipIcon {
    SWING(0xE020),
    HIT(0xE021),
    KILL(0xE022),
    MISS(0xE023),
    HURT(0xE024),
    SHOOT(0xE025),
    BLOCK(0xE026),
    PROJECTILE_HIT(0xE027),
    USE(0xE028),
    SUMMON_HIT(0xE029),
    SPRINTING(0xE02A),
    AIRBORNE(0xE02B),
    CROUCHING(0xE02C),
    NIGHT(0xE02D),
    DAY(0xE02E),
    BEHIND(0xE02F),
    DAMAGED(0xE030),
    CASTING(0xE031),
    HAS_SPELL(0xE032),
    IDLE(0xE033),
    HEALTH(0xE034);

    private static final ResourceLocation FONT =
            ResourceLocation.fromNamespaceAndPath(CombatStyles.MOD_ID, "icons");
    private static final String GAP = " ";

    private final int glyph;

    TooltipIcon(int glyph) {
        this.glyph = glyph;
    }

    public MutableComponent icon() {
        return Component.literal(Character.toString(glyph))
                .withStyle(style -> style.withFont(FONT).withColor(ChatFormatting.WHITE));
    }

    public static MutableComponent leading(Collection<TooltipIcon> icons, Component line) {
        if (icons.isEmpty() || !CombatStylesConfig.get().tooltipIcons()) {
            return line.copy();
        }
        MutableComponent shown = Component.empty();
        for (TooltipIcon icon : icons) {
            shown.append(icon.icon());
        }
        return shown.append(GAP).append(line);
    }
}

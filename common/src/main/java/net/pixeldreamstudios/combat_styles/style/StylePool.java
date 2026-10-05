package net.pixeldreamstudios.combat_styles.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StylePool {

    public static final String KEY_STYLE_PAGE = "style_page";

    private StylePool() {
    }

    public static boolean natural(String styleId) {
        AttackOverride override = AttackOverrides.get(styleId);
        if (override != null && override.stylePage().isPresent()) {
            return override.stylePage().get();
        }
        return definedNatural(styleId);
    }

    public static boolean definedNatural(String styleId) {
        return definedFlag(styleId).orElse(Boolean.TRUE);
    }

    private static Optional<Boolean> definedFlag(String styleId) {
        if (styleId == null) {
            return Optional.empty();
        }
        CustomAttack attack = CustomAttacks.get(styleId);
        if (attack != null) {
            return attack.stylePage();
        }
        CustomStyle style = CustomStyles.get(styleId);
        return style == null ? Optional.empty() : style.stylePage();
    }

    public static List<StylePage> natural(List<StylePage> pages) {
        List<StylePage> kept = new ArrayList<>(pages.size());
        for (StylePage page : pages) {
            if (page != null && natural(page.styleId())) {
                kept.add(page);
            }
        }
        return kept;
    }
}

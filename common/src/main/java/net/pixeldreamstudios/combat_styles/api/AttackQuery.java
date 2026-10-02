package net.pixeldreamstudios.combat_styles.api;

import net.bettercombat.api.WeaponAttributes;

import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

public final class AttackQuery {

    private AttackQuery() {
    }

    public static AttackInfo of(WeaponAttributes.Attack attack) {
        return AttackInfoOf.of(attack);
    }

    public static AttackInfo ofStyle(String styleId) {
        StylePage page = StyleRegistry.get(styleId);
        return page == null ? AttackInfo.UNKNOWN : AttackInfoOf.of(page.attack());
    }

}

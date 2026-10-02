package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.List;
import java.util.Optional;

public final class EquipmentAffix {

    private static final ResourceLocation FALLBACK_ID =
            ResourceLocation.fromNamespaceAndPath(CombatStyles.MOD_ID, "equipment");

    private EquipmentAffix() {
    }

    public static StyleAffix of(EquipmentRuntime.Match equipment) {
        ResourceLocation id = ResourceLocation.tryParse(equipment.id());
        return new StyleAffix(
                id == null ? FALLBACK_ID : id,
                0,
                equipment.rarity().orElse(StyleRarity.COMMON),
                Optional.empty(),
                equipment.effect().onHit().orElse(AffixEffects.OnHit.EMPTY),
                equipment.effect().onSwing().orElse(AffixEffects.OnHit.EMPTY),
                StyleAffix.AppliesTo.ANY,
                StyleAffix.DEFAULT_COST_MODIFIER,
                1.0D,
                Optional.empty(),
                List.of());
    }
}

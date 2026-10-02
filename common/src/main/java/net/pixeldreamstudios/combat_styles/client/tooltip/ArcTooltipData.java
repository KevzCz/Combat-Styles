package net.pixeldreamstudios.combat_styles.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import net.pixeldreamstudios.combat_styles.api.AttackShape;
import net.pixeldreamstudios.combat_styles.style.StylePage;

import java.util.Optional;

public record ArcTooltipData(AttackShape shape, double angle, double reach, double baseReach)
        implements TooltipComponent {

    private static final double BASE_REACH = 1.0D;

    public static Optional<TooltipComponent> of(StylePage page) {
        if (page == null) {
            return Optional.empty();
        }
        return Optional.of(new ArcTooltipData(
                AttackShape.of(page.hitbox(), page.angle()),
                page.angle(), page.rangeMultiplier(), BASE_REACH));
    }
}

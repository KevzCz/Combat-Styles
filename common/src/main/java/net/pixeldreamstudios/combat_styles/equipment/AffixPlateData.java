package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

public record AffixPlateData(Component name, List<Plate> plates,
                             List<Component> lines) implements TooltipComponent {

    public record Plate(Component label, int fillColor) {
    }

    public AffixPlateData {
        plates = plates == null ? List.of() : List.copyOf(plates);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}

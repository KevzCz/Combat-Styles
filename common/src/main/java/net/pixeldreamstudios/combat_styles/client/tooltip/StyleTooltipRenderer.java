package net.pixeldreamstudios.combat_styles.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.client.AffixPlateComponent;
import net.pixeldreamstudios.combat_styles.equipment.AffixPlateData;
import net.pixeldreamstudios.combat_styles.item.EquipmentItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StyleTooltipRenderer {

    private StyleTooltipRenderer() {
    }

    public static boolean handles(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getTooltipImage().orElse(null) instanceof ArcTooltipData) {
            return true;
        }
        return stack.getItem() instanceof EquipmentItem;
    }

    public static boolean hasPlates(List<Component> lines) {
        for (Component line : lines) {
            if (AffixPlates.of(line) != null) {
                return true;
            }
        }
        return false;
    }

    public static void render(GuiGraphics graphics, Font font, ItemStack stack,
                              List<Component> lines, int mouseX, int mouseY,
                              int screenWidth, int screenHeight) {
        renderLines(graphics, font, lines, stack.getTooltipImage(), mouseX, mouseY,
                screenWidth, screenHeight);
    }

    public static void renderLines(GuiGraphics graphics, Font font, List<Component> lines,
                                   Optional<TooltipComponent> image, int mouseX, int mouseY,
                                   int screenWidth, int screenHeight) {
        List<ClientTooltipComponent> components = new ArrayList<>(lines.size() + 1);
        for (Component line : lines) {
            AffixPlateData.Plate plate = AffixPlates.of(line);
            if (plate == null) {
                components.add(ClientTooltipComponent.create(line.getVisualOrderText()));
            } else {
                components.add(new AffixPlateComponent(new AffixPlateData(
                        line, List.of(plate), List.of())));
            }
        }

        if (image.orElse(null) instanceof ArcTooltipData arc) {
            components.add(new ArcTooltipComponent(
                    arc.shape(), arc.angle(), arc.reach(), arc.baseReach()));
        }

        TooltipPanel.render(graphics, font, components, mouseX, mouseY,
                screenWidth, screenHeight);
    }
}

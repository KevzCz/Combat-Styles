package net.pixeldreamstudios.combat_styles.client;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentContext;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EquipmentHints {

    public static final String MARKER_EQUIPMENT = "◇";
    private static final String KEY_AFFECTED_BY = "screen.combat_styles.affected_by";
    private static final String KEY_STANCE_SINGLE = "screen.combat_styles.stance_single";
    private static final String KEY_STANCE_DUAL = "screen.combat_styles.stance_dual";
    private static final String KEY_STANCE_DUAL_MAIN = "screen.combat_styles.stance_dual_main";
    private static final String KEY_STANCE_DUAL_OFF = "screen.combat_styles.stance_dual_off";
    private static final String DEFAULT_NAMESPACE = "minecraft:";
    private static final char NAMESPACE_SEPARATOR = ':';

    private enum Stance {
        SINGLE, DUAL_MAIN, DUAL_OFF
    }

    private EquipmentHints() {
    }

    private static List<EquipmentRuntime.Match> matchesFor(Player player, StyleEntry entry,
                                                           int index, int size) {
        if (player == null || entry == null || EquipmentRegistry.isEmpty()) {
            return List.of();
        }
        StylePage page = StyleRegistry.get(entry.styleId());
        EquipmentContext context = new EquipmentContext(player, entry,
                page == null ? null : page.attack(), index, size, 0);
        List<EquipmentRuntime.Match> found = new ArrayList<>();
        for (EquipmentRuntime.Match match : EquipmentRuntime.matching(player, context)) {
            if (match.trigger().everyNthSwing().isEmpty() && boundToSlot(match)) {
                found.add(match);
            }
        }
        return found;
    }

    public static void appendAffectingGear(Player player, List<StyleEntry> entries,
                                           StyleEntry entry, int index, List<Component> lines) {
        Map<Stance, Map<String, EquipmentRuntime.Match>> byStance = new LinkedHashMap<>();
        Set<String> shown = new LinkedHashSet<>();
        for (Stance stance : Stance.values()) {
            Map<String, EquipmentRuntime.Match> found = new LinkedHashMap<>();
            collectGear(player, entries, entry, index, stance, found);
            byStance.put(stance, found);
            shown.addAll(found.keySet());
        }
        if (shown.isEmpty()) {
            return;
        }
        lines.add(Component.empty());
        lines.add(Component.translatable(KEY_AFFECTED_BY).withStyle(ChatFormatting.GOLD));
        for (String id : shown) {
            EquipmentRuntime.Match match = null;
            List<Stance> matched = new ArrayList<>();
            for (Stance stance : Stance.values()) {
                EquipmentRuntime.Match found = byStance.get(stance).get(id);
                if (found != null) {
                    matched.add(stance);
                    if (match == null) {
                        match = found;
                    }
                }
            }
            MutableComponent line = Component.literal(MARKER_EQUIPMENT + " ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(gearName(match).withStyle(ChatFormatting.AQUA));
            Component gate = stanceLabel(matched);
            if (gate != null) {
                line.append(Component.literal(" ").append(gate)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            lines.add(line);
        }
    }

    public static boolean affectedInAnyStance(Player player, List<StyleEntry> entries,
                                              StyleEntry entry, int index) {
        for (Stance stance : Stance.values()) {
            Map<String, EquipmentRuntime.Match> found = new LinkedHashMap<>();
            collectGear(player, entries, entry, index, stance, found);
            if (!found.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void collectGear(Player player, List<StyleEntry> entries, StyleEntry entry,
                                    int index, Stance stance,
                                    Map<String, EquipmentRuntime.Match> out) {
        int usableIndex = usablePosition(entries, index, stance);
        if (usableIndex < 0) {
            return;
        }
        for (EquipmentRuntime.Match match : matchesFor(player, entry,
                usableIndex, usableTotal(entries, stance))) {
            out.putIfAbsent(match.id(), match);
        }
    }

    private static Component stanceLabel(List<Stance> matched) {
        if (matched.size() == Stance.values().length || matched.isEmpty()) {
            return null;
        }
        if (matched.size() == 1) {
            return switch (matched.getFirst()) {
                case SINGLE -> Component.translatable(KEY_STANCE_SINGLE);
                case DUAL_MAIN -> Component.translatable(KEY_STANCE_DUAL_MAIN);
                case DUAL_OFF -> Component.translatable(KEY_STANCE_DUAL_OFF);
            };
        }
        return matched.contains(Stance.SINGLE)
                ? null
                : Component.translatable(KEY_STANCE_DUAL);
    }

    private static boolean usableWhen(List<StyleEntry> entries, int index, Stance stance) {
        boolean dualWielding = stance != Stance.SINGLE;
        if (index < 0 || index >= entries.size()) {
            return false;
        }
        StyleEntry resolved = CustomStyles.resolve(entries.get(index));
        StylePage page = StyleRegistry.get(resolved.styleId());
        if (page != null && resolved.attackOverride().isPresent()) {
            page = AttackOverrides.applyTo(page, resolved.attackOverride().get());
        }
        if (page == null || !page.isConditional()) {
            return true;
        }
        for (WeaponAttributes.Condition condition : page.attack().conditions()) {
            if (condition == null) {
                continue;
            }
            switch (condition) {
                case NOT_DUAL_WIELDING -> {
                    if (dualWielding) {
                        return false;
                    }
                }
                case MAIN_HAND_ONLY -> {
                    if (stance == Stance.DUAL_OFF) {
                        return false;
                    }
                }
                case NO_OFFHAND_ITEM -> {
                    if (dualWielding) {
                        return false;
                    }
                }
                case DUAL_WIELDING_ANY, DUAL_WIELDING_SAME, DUAL_WIELDING_SAME_CATEGORY -> {
                    if (!dualWielding) {
                        return false;
                    }
                }
                case OFF_HAND_ONLY -> {
                    if (stance != Stance.DUAL_OFF) {
                        return false;
                    }
                }
                default -> {
                }
            }
        }
        return true;
    }

    private static int usableTotal(List<StyleEntry> entries, Stance stance) {
        int count = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (usableWhen(entries, i, stance)) {
                count++;
            }
        }
        return count;
    }

    private static int usablePosition(List<StyleEntry> entries, int index, Stance stance) {
        if (!usableWhen(entries, index, stance)) {
            return -1;
        }
        int position = 0;
        for (int i = 0; i < index; i++) {
            if (usableWhen(entries, i, stance)) {
                position++;
            }
        }
        return position;
    }

    private static MutableComponent gearName(EquipmentRuntime.Match match) {
        String id = match.id();
        int cut = id.indexOf(NAMESPACE_SEPARATOR);
        String path = cut >= 0 ? id.substring(cut + 1) : id;
        Item item = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.tryParse(cut >= 0 ? id : DEFAULT_NAMESPACE + path))
                .orElse(null);
        return item == null
                ? Component.literal(StyleText.prettify(path))
                : Component.translatable(item.getDescriptionId());
    }

    private static boolean boundToSlot(EquipmentRuntime.Match match) {
        return match.trigger().slotIndex().isPresent()
                || match.trigger().slots().isPresent()
                || match.trigger().slotBand().isPresent()
                || match.trigger().lastSlot().isPresent()
                || match.trigger().hitbox().isPresent()
                || match.trigger().styleId().isPresent()
                || match.trigger().affixId().isPresent()
                || match.trigger().requiresAffix().orElse(false);
    }
}

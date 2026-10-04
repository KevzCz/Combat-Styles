package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.pixeldreamstudios.combat_styles.equipment.EffectGroup;
import net.pixeldreamstudios.combat_styles.equipment.EffectKeys;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record HudRow(ItemStack stack, String itemId, List<Lane> lanes,
                     float itemFill, int itemRemaining, float procFlash, boolean armed) {

    private static final int HUE_STEPS = 360;
    private static final float HUE_SCALE = 1.0F / HUE_STEPS;
    private static final float SATURATION = 0.55F;
    private static final float BRIGHTNESS = 0.88F;
    private static final int OPAQUE = 0xFF000000;
    private static final int SAMPLE_SHORT = 40;
    private static final int SAMPLE_LONG = 100;

    public enum LaneState {
        READY,
        COOLING,
        PASSIVE
    }

    public record Lane(String key, int colour, LaneState state, float fill, int remaining) {

        public boolean cooling() {
            return state == LaneState.COOLING;
        }
    }

    public HudRow {
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
    }

    public static HudRow of(ItemStack stack, EquipmentDefinition definition, int copy) {
        String itemId = definition.id();
        String owner = EffectKeys.ownerOf(itemId, copy);
        List<Lane> lanes = new ArrayList<>();
        Set<String> claimed = new LinkedHashSet<>();
        for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
            EffectGroup group = named.getValue();
            boolean collapses = group.onCooldown() || group.windowed();
            for (EffectGroup.Entry entry : group.entries()) {
                String effect = entry.effect();
                if (effect.isBlank() || !definition.effects().containsKey(effect)) {
                    continue;
                }
                claimed.add(effect);
                if (!collapses) {
                    lanes.add(laneFor(owner, effect, definition.effects().get(effect)));
                }
            }
            if (collapses) {
                lanes.add(laneFor(owner, named.getKey(), null));
            }
        }
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            if (claimed.add(named.getKey())) {
                lanes.add(laneFor(owner, named.getKey(), named.getValue()));
            }
        }
        return new HudRow(stack, itemId, lanes,
                CooldownMirror.fill(itemId), CooldownMirror.remaining(itemId),
                CooldownMirror.procFlash(itemId), CooldownMirror.armed(itemId));
    }

    private static Lane laneFor(String owner, String name, TriggeredEffect entry) {
        String key = EffectKeys.of(owner, name);
        int remaining = CooldownMirror.remaining(key);
        LaneState state = remaining > 0
                ? LaneState.COOLING
                : passive(entry) ? LaneState.PASSIVE : LaneState.READY;
        float fill = remaining <= 0 ? 0.0F : 1.0F - CooldownMirror.fill(key);
        return new Lane(name, colourOf(name), state, fill, remaining);
    }

    private static boolean passive(TriggeredEffect entry) {
        return entry != null && entry.trigger().isAlways() && entry.cooldown().isEmpty();
    }

    private static int colourOf(String name) {
        int hue = Math.floorMod(name.hashCode(), HUE_STEPS);
        return OPAQUE | (Mth.hsvToRgb(hue * HUE_SCALE, SATURATION, BRIGHTNESS) & 0x00FFFFFF);
    }

    public boolean idle() {
        if (itemRemaining > 0 || procFlash > 0.0F || armed) {
            return false;
        }
        for (Lane lane : lanes) {
            if (lane.cooling()) {
                return false;
            }
        }
        return true;
    }

    public int longestRemaining() {
        int longest = itemRemaining;
        for (Lane lane : lanes) {
            longest = Math.max(longest, lane.remaining());
        }
        return longest;
    }

    public boolean anyCooling() {
        for (Lane lane : lanes) {
            if (lane.cooling()) {
                return true;
            }
        }
        return itemRemaining > 0;
    }

    public static List<HudRow> samples() {
        return List.of(
                new HudRow(new ItemStack(Items.GOLD_INGOT), "sample_a",
                        List.of(new Lane("passive", 0xFF7AA2D6, LaneState.PASSIVE, 0.0F, 0),
                                new Lane("entry", 0xFF6FD3E0, LaneState.READY, 0.0F, 0),
                                new Lane("exit", 0xFFE0A341, LaneState.COOLING, 0.45F,
                                        SAMPLE_SHORT)),
                        0.0F, 0, 0.0F, false),
                new HudRow(new ItemStack(Items.EMERALD), "sample_b",
                        List.of(new Lane("ward", 0xFF5FC294, LaneState.PASSIVE, 0.0F, 0),
                                new Lane("burst", 0xFFE0715C, LaneState.COOLING, 0.7F,
                                        SAMPLE_LONG)),
                        0.55F, SAMPLE_LONG, 0.0F, true));
    }
}

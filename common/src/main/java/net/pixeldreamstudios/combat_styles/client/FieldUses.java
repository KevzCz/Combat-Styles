package net.pixeldreamstudios.combat_styles.client;

import com.google.gson.JsonElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.config.ChoiceSource;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumber;
import net.pixeldreamstudios.combat_styles.config.OptionField;
import net.pixeldreamstudios.combat_styles.config.OptionSources;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FieldUses {

    private static final String KEY_USES = "screen.combat_styles.summon_field_uses";
    private static final String KEY_USED_BY = "screen.combat_styles.summon_field_used_by";
    private static final String KEY_IN_USE = "screen.combat_styles.field_uses_in_use";
    private static final String KEY_CHOICE = "screen.combat_styles.field_uses_choice";
    private static final String BULLET = "  • ";
    private static final String SEPARATOR = "/";
    private static final String ANY_OWNER = "";

    private static final String KEY_MODE = "mode";
    private static final String DAMAGE_OWNER = "damage";
    private static final String DAMAGES_OWNER = "damages";
    private static final String SCALING_OWNER = "scaling";
    private static final String USE_OWNER = "use";
    private static final String SPAWN_OWNER = "spawn";

    private record Rule(String owner, String selector, Map<String, List<String>> uses) {
    }

    private static final List<Rule> RULES = List.of(
            rule(DAMAGE_OWNER, KEY_MODE, Map.of(
                    "additive", List.of("ratio", "flat", "type", "school", "scaling", "ignores_armour", "multiplier"),
                    "amplify", List.of("ratio", "flat", "scaling", "multiplier"),
                    "convert", List.of("ratio", "type", "school", "scaling", "multiplier"))),
            rule(DAMAGES_OWNER, KEY_MODE, Map.of(
                    "additive", List.of("ratio", "flat", "type", "school", "scaling", "ignores_armour", "multiplier"),
                    "amplify", List.of("ratio", "flat", "scaling", "multiplier"),
                    "convert", List.of("ratio", "type", "school", "scaling", "multiplier"))),
            rule(ScaledNumber.KEY_BY, KEY_MODE, Map.of(
                    "per_point", List.of("source", "per_point", "cap"),
                    "multiplier", List.of("source", "per_point", "cap"),
                    "divisor", List.of("source", "baseline", "cap"))),
            rule(SCALING_OWNER, KEY_MODE, Map.of(
                    "per_point", List.of("source", "per_point", "cap"),
                    "multiplier", List.of("source", "per_point", "cap"),
                    "divisor", List.of("source", "baseline", "cap"))),
            rule(USE_OWNER, KEY_MODE, Map.of(
                    "press", List.of("pose", "cooldown"),
                    "charge", List.of("hold_ticks", "threshold_ticks", "pose",
                            "release_animation", "cooldown"),
                    "channel", List.of("hold_ticks", "channel_start", "channel_ticks", "pose",
                            "channel_animation", "release_animation", "cooldown"))),
            rule(SPAWN_OWNER, KEY_MODE, Map.of(
                    "random", List.of("anchor", "forward", "up", "right", "yaw", "pitch"),
                    "fan", List.of("anchor", "arc", "yaw", "pitch"),
                    "ring", List.of("anchor", "arc", "yaw", "pitch"),
                    "line", List.of("anchor", "arc", "forward", "right"))));

    private FieldUses() {
    }

    private static Rule rule(String owner, String selector, Map<String, List<String>> uses) {
        return new Rule(owner, selector, new LinkedHashMap<>(uses));
    }

    public static List<Component> lines(String schemaRoot, JsonTreeModel.Node node, JsonElement root) {
        List<Component> scripted = SummonAbilityFieldInfo.lines(schemaRoot, node, root);
        if (!scripted.isEmpty() || node == null || root == null) {
            return scripted;
        }
        String parent = parentOf(node.path());
        String value = stringOf(node.value());
        ChoiceSource source = OptionSources.get(node.key());
        if (source != null && value != null) {
            List<OptionField> fields = OptionSources.fieldsFor(node.key(), value);
            if (!fields.isEmpty()) {
                List<String> keys = new ArrayList<>();
                for (OptionField field : fields) {
                    keys.add(OptionSources.KEY_OPTIONS + SEPARATOR + field.key());
                }
                return uses(keys, parent, root);
            }
        }
        Rule selecting = ruleFor(parent, node.key());
        if (selecting != null && value != null) {
            List<String> keys = selecting.uses().get(value);
            return keys == null ? List.of() : uses(keys, parent, root);
        }
        return usedBy(node.key(), parent, root);
    }

    private static Rule ruleFor(String parent, String key) {
        String owner = ownerOf(parent);
        for (Rule rule : RULES) {
            if (rule.selector().equals(key)
                    && (rule.owner().equals(ANY_OWNER) || rule.owner().equals(owner))) {
                return rule;
            }
        }
        return null;
    }

    private static List<Component> uses(List<String> keys, String parent, JsonElement root) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY_USES).withStyle(ChatFormatting.GOLD));
        for (String key : keys) {
            boolean set = JsonTreeModel.resolve(root, join(parent, key)) != null;
            lines.add(entry(EffectLabels.labelFor(leafOf(key)), set));
        }
        lines.add(Component.translatable(KEY_IN_USE).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static List<Component> usedBy(String key, String parent, JsonElement root) {
        for (Rule rule : RULES) {
            if (!rule.owner().equals(ANY_OWNER) && !rule.owner().equals(ownerOf(parent))) {
                continue;
            }
            List<String> readers = new ArrayList<>();
            for (Map.Entry<String, List<String>> choice : rule.uses().entrySet()) {
                if (choice.getValue().contains(key)) {
                    readers.add(choice.getKey());
                }
            }
            if (readers.isEmpty() || readers.size() == rule.uses().size()) {
                continue;
            }
            String chosen = stringOf(JsonTreeModel.resolve(root, join(parent, rule.selector())));
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(KEY_USED_BY).withStyle(ChatFormatting.GOLD));
            for (String reader : readers) {
                lines.add(entry(Component.translatable(KEY_CHOICE,
                        EffectLabels.labelFor(rule.selector()), StyleText.prettify(reader)),
                        reader.equals(chosen)));
            }
            lines.add(Component.translatable(KEY_IN_USE).withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        return List.of();
    }

    private static String stringOf(JsonElement value) {
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static Component entry(Component label, boolean set) {
        return Component.literal(BULLET).append(label.copy())
                .withStyle(set ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    private static String join(String parent, String key) {
        return parent.isEmpty() ? key : parent + SEPARATOR + key;
    }

    private static String parentOf(String path) {
        int split = path == null ? -1 : path.lastIndexOf(SEPARATOR);
        return split < 0 ? "" : path.substring(0, split);
    }

    private static String leafOf(String path) {
        int split = path.lastIndexOf(SEPARATOR);
        return split < 0 ? path : path.substring(split + 1);
    }

    private static String ownerOf(String parent) {
        String[] parts = parent.split(SEPARATOR);
        for (int index = parts.length - 1; index >= 0; index--) {
            if (!EffectSchema.isIndex(parts[index]) && !parts[index].isEmpty()) {
                return parts[index];
            }
        }
        return ANY_OWNER;
    }
}

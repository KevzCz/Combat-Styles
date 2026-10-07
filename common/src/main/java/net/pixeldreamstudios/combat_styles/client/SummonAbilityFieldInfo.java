package net.pixeldreamstudios.combat_styles.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.pixeldreamstudios.combat_styles.entity.ScriptedAbility;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SummonAbilityFieldInfo {

    private static final String KEY_USES = "screen.combat_styles.summon_field_uses";
    private static final String KEY_USED_BY = "screen.combat_styles.summon_field_used_by";
    private static final String KEY_IN_USE = "screen.combat_styles.summon_field_in_use";
    private static final String BULLET = "  • ";
    private static final String SEPARATOR = "/";
    private static final ChatFormatting SET = ChatFormatting.GREEN;
    private static final ChatFormatting UNSET = ChatFormatting.DARK_GRAY;

    private SummonAbilityFieldInfo() {
    }

    public static List<Component> lines(String schemaRoot, JsonTreeModel.Node node, JsonElement root) {
        if (!EffectSchema.ROOT_SUMMON_ABILITY.equals(schemaRoot) || node == null || root == null) {
            return List.of();
        }
        String path = node.path();
        String parent = parentOf(path);
        boolean inAction = parent.endsWith(EffectSchema.KEY_ACTION);
        if (inAction && ScriptedAbility.KEY_TYPE.equals(node.key())) {
            ScriptedAbility.ActionType type = node.value() != null && node.value().isJsonPrimitive()
                    ? ScriptedAbility.ActionType.byName(node.value().getAsString())
                    : null;
            return type == null ? List.of() : uses(schemaRoot, type, parent, root);
        }
        List<ScriptedAbility.ActionType> readers = ScriptedAbility.ActionType.readersOf(
                inAction ? node.key() : path);
        return readers.isEmpty() ? List.of() : usedBy(readers, root);
    }

    private static List<Component> uses(String schemaRoot, ScriptedAbility.ActionType type,
                                        String actionPath, JsonElement root) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY_USES).withStyle(ChatFormatting.GOLD));
        for (String key : type.actionKeys()) {
            boolean set = JsonTreeModel.resolve(root, actionPath + SEPARATOR + key) != null;
            lines.add(entry(EffectLabels.labelFor(EffectSchema.join(schemaRoot, actionPath), key), set));
        }
        for (String tuning : type.tuningPaths()) {
            boolean set = JsonTreeModel.resolve(root, tuning) != null;
            lines.add(entry(EffectLabels.labelFor(EffectSchema.join(schemaRoot, parentOf(tuning)),
                    leafOf(tuning)), set));
        }
        lines.add(Component.translatable(KEY_IN_USE).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static List<Component> usedBy(List<ScriptedAbility.ActionType> readers, JsonElement root) {
        Set<String> present = typesIn(root);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(KEY_USED_BY).withStyle(ChatFormatting.GOLD));
        for (ScriptedAbility.ActionType type : readers) {
            lines.add(entry(Component.literal(StyleText.prettify(type.getSerializedName())),
                    present.contains(type.getSerializedName())));
        }
        lines.add(Component.translatable(KEY_IN_USE).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private static Set<String> typesIn(JsonElement root) {
        Set<String> found = new LinkedHashSet<>();
        JsonElement steps = root.isJsonObject()
                ? root.getAsJsonObject().get(EffectSchema.KEY_STEPS)
                : null;
        if (steps == null || !steps.isJsonArray()) {
            return found;
        }
        for (JsonElement step : (JsonArray) steps) {
            JsonElement action = step.isJsonObject()
                    ? step.getAsJsonObject().get(EffectSchema.KEY_ACTION)
                    : null;
            if (action != null && action.isJsonObject()) {
                JsonObject object = action.getAsJsonObject();
                JsonElement type = object.get(ScriptedAbility.KEY_TYPE);
                if (type != null && type.isJsonPrimitive()) {
                    found.add(type.getAsString());
                }
            }
        }
        return found;
    }

    private static Component entry(Component label, boolean set) {
        MutableComponent line = Component.literal(BULLET).append(label.copy());
        return line.withStyle(set ? SET : UNSET);
    }

    private static String parentOf(String path) {
        int split = path == null ? -1 : path.lastIndexOf(SEPARATOR);
        return split < 0 ? "" : path.substring(0, split);
    }

    private static String leafOf(String path) {
        int split = path.lastIndexOf(SEPARATOR);
        return split < 0 ? path : path.substring(split + 1);
    }
}

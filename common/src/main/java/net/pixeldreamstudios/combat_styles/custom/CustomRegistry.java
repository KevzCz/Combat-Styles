package net.pixeldreamstudios.combat_styles.custom;

import net.pixeldreamstudios.combat_styles.config.OptionField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CustomRegistry {

    private static final Map<String, CustomHook> HOOKS = new LinkedHashMap<>();

    private CustomRegistry() {
    }

    public static void register(String id, CustomHook hook) {
        if (id != null && !id.isBlank() && hook != null) {
            HOOKS.put(id, hook);
        }
    }

    public static CustomHook get(String id) {
        return id == null ? null : HOOKS.get(id);
    }

    public static List<String> ids() {
        return List.copyOf(HOOKS.keySet());
    }

    public static List<String> idsFor(String scope) {
        List<String> allowed = new ArrayList<>();
        for (Map.Entry<String, CustomHook> entry : HOOKS.entrySet()) {
            if (fits(entry.getValue(), scope)) {
                allowed.add(entry.getKey());
            }
        }
        return allowed;
    }

    public static boolean fits(CustomHook hook, String scope) {
        return hook != null && (hook.scopes().isEmpty() || scope == null || hook.scopes().contains(scope));
    }

    public static List<OptionField> optionsFor(String id) {
        CustomHook hook = get(id);
        return hook == null ? List.of() : hook.options();
    }
}

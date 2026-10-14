package net.pixeldreamstudios.combat_styles.kit;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Optional;

public record KitAction(Type type, String mode, String value) {

    public static final String MODE_GRANT = "grant";
    public static final String MODE_REVOKE = "revoke";
    public static final String MODE_ID = "id";
    public static final String MODE_COMMAND = "command";
    public static final String MODE_ADD = "add";
    public static final String MODE_REMOVE = "remove";
    public static final String MODE_UNLOCK = "unlock";
    public static final String MODE_LOCK = "lock";
    private static final String KEY_TYPE = "type";

    public enum Type {
        ADVANCEMENT("advancement", List.of(MODE_GRANT, MODE_REVOKE)),
        FUNCTION("function", List.of(MODE_ID)),
        COMMAND("command", List.of(MODE_COMMAND)),
        TAG("tag", List.of(MODE_ADD, MODE_REMOVE)),
        PUFFISH("puffish", List.of(MODE_UNLOCK, MODE_LOCK));

        private final String id;
        private final List<String> modes;

        Type(String id, List<String> modes) {
            this.id = id;
            this.modes = modes;
        }

        static Optional<Type> of(String id) {
            for (Type type : values()) {
                if (type.id.equals(id)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    public static Optional<KitAction> parse(JsonObject json) {
        if (!json.has(KEY_TYPE)) {
            return Optional.empty();
        }
        Optional<Type> type = Type.of(json.get(KEY_TYPE).getAsString());
        if (type.isEmpty()) {
            return Optional.empty();
        }
        for (String mode : type.get().modes) {
            if (json.has(mode) && json.get(mode).isJsonPrimitive()) {
                return Optional.of(new KitAction(type.get(), mode, json.get(mode).getAsString()));
            }
        }
        return Optional.empty();
    }
}

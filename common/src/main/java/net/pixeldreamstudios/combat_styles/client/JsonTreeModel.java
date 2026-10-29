package net.pixeldreamstudios.combat_styles.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JsonTreeModel {

    private static final String PATH_SEPARATOR = "/";
    private static final String ESCAPED_SEPARATOR = "\uE000";

    public enum Kind {
        OBJECT,
        ARRAY,
        NUMBER,
        BOOLEAN,
        STRING
    }

    public record Node(String key, String path, int depth, Kind kind, JsonElement value,
                       boolean expandable) {
    }

    private JsonTreeModel() {
    }

    public static Kind kindOf(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Kind.STRING;
        }
        if (element.isJsonObject()) {
            return Kind.OBJECT;
        }
        if (element.isJsonArray()) {
            return Kind.ARRAY;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return Kind.BOOLEAN;
        }
        if (primitive.isNumber()) {
            return Kind.NUMBER;
        }
        return Kind.STRING;
    }

    public static List<Node> flatten(JsonElement root, Set<String> collapsed) {
        List<Node> nodes = new ArrayList<>();
        walk(root, "", "", 0, nodes, collapsed);
        return nodes;
    }

    private static void walk(JsonElement element, String key, String path, int depth,
                             List<Node> nodes, Set<String> collapsed) {
        Kind kind = kindOf(element);
        boolean expandable = kind == Kind.OBJECT || kind == Kind.ARRAY;
        if (!path.isEmpty()) {
            nodes.add(new Node(key, path, depth, kind, element, expandable));
        }
        if (!expandable || (!path.isEmpty() && collapsed.contains(path))) {
            return;
        }
        int childDepth = path.isEmpty() ? 0 : depth + 1;
        if (kind == Kind.OBJECT) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                walk(entry.getValue(), entry.getKey(), join(path, entry.getKey()),
                        childDepth, nodes, collapsed);
            }
            return;
        }
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            walk(array.get(i), "[" + i + "]", join(path, String.valueOf(i)),
                    childDepth, nodes, collapsed);
        }
    }

    private static String join(String path, String key) {
        String segment = escape(key);
        return path.isEmpty() ? segment : path + PATH_SEPARATOR + segment;
    }

    public static JsonElement resolve(JsonElement root, String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        JsonElement current = root;
        for (String part : path.split(PATH_SEPARATOR)) {
            if (current == null) {
                return null;
            }
            if (current.isJsonObject()) {
                current = current.getAsJsonObject().get(unescape(part));
            } else if (current.isJsonArray()) {
                int index = parseIndex(part);
                JsonArray array = current.getAsJsonArray();
                current = index >= 0 && index < array.size() ? array.get(index) : null;
            } else {
                return null;
            }
        }
        return current;
    }

    public static void set(JsonElement root, String path, JsonElement value) {
        int split = path.lastIndexOf(PATH_SEPARATOR);
        String parentPath = split < 0 ? "" : path.substring(0, split);
        String leaf = unescape(split < 0 ? path : path.substring(split + 1));
        JsonElement parent = resolve(root, parentPath);
        if (parent == null) {
            return;
        }
        if (parent.isJsonObject()) {
            parent.getAsJsonObject().add(leaf, value);
            return;
        }
        if (parent.isJsonArray()) {
            int index = parseIndex(leaf);
            JsonArray array = parent.getAsJsonArray();
            if (index >= 0 && index < array.size()) {
                array.set(index, value);
            }
        }
    }

    public static void remove(JsonElement root, String path) {
        int split = path.lastIndexOf(PATH_SEPARATOR);
        String parentPath = split < 0 ? "" : path.substring(0, split);
        String leaf = unescape(split < 0 ? path : path.substring(split + 1));
        JsonElement parent = resolve(root, parentPath);
        if (parent == null) {
            return;
        }
        if (parent.isJsonObject()) {
            parent.getAsJsonObject().remove(leaf);
            return;
        }
        if (parent.isJsonArray()) {
            int index = parseIndex(leaf);
            JsonArray array = parent.getAsJsonArray();
            if (index >= 0 && index < array.size()) {
                array.remove(index);
            }
        }
    }

    public static String escape(String key) {
        return key.replace(PATH_SEPARATOR, ESCAPED_SEPARATOR);
    }

    public static String unescape(String segment) {
        return segment.replace(ESCAPED_SEPARATOR, PATH_SEPARATOR);
    }

    private static int parseIndex(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static JsonObject objectAt(JsonElement root, String path) {
        JsonElement element = resolve(root, path);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}

package net.pixeldreamstudios.combat_styles.damage;

import com.google.common.collect.MapMaker;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.item.TooltipContextAccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ScaledNumbers {

    private static final String PATH_SEPARATOR = "/";
    private static final String ROOT = "";

    private record Attached(Map<String, ScaledNumber> numbers, Codec<?> plain) {
    }

    private static final Map<Object, Attached> ATTACHED = new MapMaker().weakKeys().makeMap();
    private static final ThreadLocal<Player> OWNER = new ThreadLocal<>();

    private ScaledNumbers() {
    }

    private record Wrapped<T>(Codec<T> delegate) implements Codec<T> {

        @Override
        public <I> DataResult<Pair<T, I>> decode(DynamicOps<I> ops, I input) {
            JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
            Map<String, ScaledNumber> found = new LinkedHashMap<>();
            JsonElement stripped = strip(json, ROOT, found);
            if (found.isEmpty()) {
                return delegate.decode(ops, input);
            }
            return delegate.decode(ops, JsonOps.INSTANCE.convertTo(ops, stripped)).map(pair -> {
                ATTACHED.put(pair.getFirst(), new Attached(Map.copyOf(found), delegate));
                return Pair.of(pair.getFirst(), ops.empty());
            });
        }

        @Override
        public <I> DataResult<I> encode(T input, DynamicOps<I> ops, I prefix) {
            Attached attached = ATTACHED.get(input);
            if (attached == null) {
                return delegate.encode(input, ops, prefix);
            }
            return delegate.encodeStart(JsonOps.INSTANCE, input).flatMap(json -> {
                for (Map.Entry<String, ScaledNumber> number : attached.numbers().entrySet()) {
                    ScaledNumber.CODEC.encodeStart(JsonOps.INSTANCE, number.getValue()).result()
                            .ifPresent(encoded -> put(json, number.getKey(), encoded));
                }
                I converted = JsonOps.INSTANCE.convertTo(ops, json);
                return ops.getMap(prefix).result().isPresent()
                        ? ops.getMap(converted).flatMap(map -> ops.mergeToMap(prefix, map))
                        : DataResult.success(converted);
            });
        }
    }

    public static <T> Codec<T> wrap(Codec<T> codec) {
        return new Wrapped<>(codec);
    }

    public static <T> T carry(Object from, T to, Predicate<String> dropped) {
        Attached attached = from == null ? null : ATTACHED.get(from);
        if (attached == null || to == null || from == to) {
            return to;
        }
        Map<String, ScaledNumber> kept = new LinkedHashMap<>();
        attached.numbers().forEach((path, number) -> {
            if (!dropped.test(path)) {
                kept.put(path, number);
            }
        });
        if (!kept.isEmpty()) {
            ATTACHED.put(to, new Attached(Map.copyOf(kept), attached.plain()));
        }
        return to;
    }

    public static Optional<ScaledNumber> at(Object value, String path) {
        Attached attached = value == null ? null : ATTACHED.get(value);
        return attached == null ? Optional.empty() : Optional.ofNullable(attached.numbers().get(path));
    }

    public static boolean has(Object value) {
        return value != null && ATTACHED.containsKey(value);
    }

    public static List<ScaledNumber> of(Object value) {
        Attached attached = value == null ? null : ATTACHED.get(value);
        return attached == null ? List.of() : List.copyOf(attached.numbers().values());
    }

    public static Map<String, ScaledNumber> entriesOf(Object value) {
        Attached attached = value == null ? null : ATTACHED.get(value);
        return attached == null ? Map.of() : attached.numbers();
    }

    public static List<String> sourcesOf(Object value) {
        List<String> sources = new ArrayList<>();
        for (ScaledNumber number : of(value)) {
            for (String source : number.sources()) {
                if (!sources.contains(source)) {
                    sources.add(source);
                }
            }
        }
        return sources;
    }

    @SuppressWarnings("unchecked")
    public static <T> T resolved(T value, Player player) {
        Attached attached = value == null ? null : ATTACHED.get(value);
        if (attached == null || player == null) {
            return value;
        }
        Codec<T> plain = (Codec<T>) attached.plain();
        Optional<JsonElement> json = plain.encodeStart(JsonOps.INSTANCE, value).result();
        if (json.isEmpty()) {
            return value;
        }
        for (Map.Entry<String, ScaledNumber> number : attached.numbers().entrySet()) {
            String path = number.getKey();
            put(json.get(), path, valueAt(json.get(), path, valueOf(number.getValue(), player)));
        }
        return plain.parse(JsonOps.INSTANCE, json.get()).result()
                .map(result -> Customs.copy(value, result))
                .orElse(value);
    }

    public static JsonObject baked(JsonObject options, Player owner) {
        if (options == null || owner == null || owner.level().isClientSide()) {
            return options;
        }
        Map<String, ScaledNumber> found = new LinkedHashMap<>();
        JsonElement stripped = strip(options, ROOT, found);
        if (found.isEmpty() || !stripped.isJsonObject()) {
            return options;
        }
        for (Map.Entry<String, ScaledNumber> number : found.entrySet()) {
            put(stripped, number.getKey(), new JsonPrimitive(valueOf(number.getValue(), owner)));
        }
        return stripped.getAsJsonObject();
    }

    private static float valueOf(ScaledNumber number, Player player) {
        return player != null && !player.level().isClientSide()
                ? number.roll(player, player.getRandom())
                : number.resolve(player);
    }

    private static JsonPrimitive valueAt(JsonElement root, String path, float value) {
        JsonElement existing = at(root, path);
        boolean whole = existing != null && existing.isJsonPrimitive()
                && existing.getAsJsonPrimitive().isNumber()
                && isWholeType(existing.getAsJsonPrimitive().getAsNumber());
        return whole ? new JsonPrimitive(Math.round(value)) : new JsonPrimitive(value);
    }

    private static boolean isWholeType(Number number) {
        return number instanceof Integer || number instanceof Long
                || number instanceof Short || number instanceof Byte;
    }

    private static JsonElement at(JsonElement root, String path) {
        JsonElement node = root;
        for (String part : path.split(PATH_SEPARATOR)) {
            node = node == null ? null : child(node, part);
        }
        return node;
    }

    public static <T> T withOwner(Player owner, Supplier<T> body) {
        Player previous = OWNER.get();
        OWNER.set(owner);
        try {
            return body.get();
        } finally {
            if (previous == null) {
                OWNER.remove();
            } else {
                OWNER.set(previous);
            }
        }
    }

    public static Player owner() {
        Player scoped = OWNER.get();
        return scoped != null ? scoped : TooltipContextAccess.viewer();
    }

    public static float resolve(JsonElement element, float fallback) {
        if (element == null) {
            return fallback;
        }
        if (!ScaledNumber.isScaled(element)) {
            return element.isJsonPrimitive() ? element.getAsFloat() : fallback;
        }
        return ScaledNumber.parse(element).map(number -> number.resolve(owner())).orElse(fallback);
    }

    private static JsonElement strip(JsonElement node, String path, Map<String, ScaledNumber> found) {
        if (ScaledNumber.isScaled(node)) {
            Optional<ScaledNumber> number = ScaledNumber.parse(node);
            if (number.isPresent()) {
                found.put(path, number.get());
                return new JsonPrimitive(number.get().base());
            }
            return node;
        }
        if (node.isJsonArray()) {
            JsonArray copy = new JsonArray();
            JsonArray array = node.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                copy.add(strip(array.get(index), join(path, String.valueOf(index)), found));
            }
            return copy;
        }
        if (node.isJsonObject()) {
            JsonObject copy = new JsonObject();
            for (Map.Entry<String, JsonElement> field : node.getAsJsonObject().entrySet()) {
                copy.add(field.getKey(), strip(field.getValue(), join(path, field.getKey()), found));
            }
            return copy;
        }
        return node;
    }

    private static void put(JsonElement root, String path, JsonElement value) {
        String[] parts = path.split(PATH_SEPARATOR);
        JsonElement node = root;
        for (int index = 0; index < parts.length - 1; index++) {
            node = child(node, parts[index]);
            if (node == null) {
                return;
            }
        }
        String last = parts[parts.length - 1];
        if (node.isJsonObject()) {
            node.getAsJsonObject().add(last, value);
        } else if (node.isJsonArray() && isIndex(last)
                && Integer.parseInt(last) < node.getAsJsonArray().size()) {
            node.getAsJsonArray().set(Integer.parseInt(last), value);
        }
    }

    private static JsonElement child(JsonElement node, String part) {
        if (node.isJsonObject()) {
            return node.getAsJsonObject().get(part);
        }
        if (node.isJsonArray() && isIndex(part)) {
            int index = Integer.parseInt(part);
            return index < node.getAsJsonArray().size() ? node.getAsJsonArray().get(index) : null;
        }
        return null;
    }

    private static boolean isIndex(String part) {
        return !part.isEmpty() && part.chars().allMatch(Character::isDigit);
    }

    private static String join(String path, String key) {
        return path.isEmpty() ? key : path + PATH_SEPARATOR + key;
    }
}

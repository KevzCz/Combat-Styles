package net.pixeldreamstudios.combat_styles.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.LibraryRef;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class EntityLibraries {

    public static final String PROJECTILES = "projectiles";
    public static final String SUMMON_PROFILES = "summon_profiles";

    public static final Codec<Map<String, AffixEffects.Projectile>> PROJECTILES_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.lazyInitialized(() -> AffixEffects.Projectile.CODEC));
    public static final Codec<Map<String, SummonProfile>> SUMMON_PROFILES_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.lazyInitialized(() -> SummonProfile.CODEC));

    private record Library(String key, String refKey, Codec<?> codec) {
    }

    private static final List<Library> LIBRARIES = List.of(
            new Library(PROJECTILES, LibraryRef.PROJECTILE_KEY,
                    Codec.lazyInitialized(() -> AffixEffects.Projectile.CODEC)),
            new Library(SUMMON_PROFILES, LibraryRef.SUMMON_KEY,
                    Codec.lazyInitialized(() -> SummonProfile.CODEC)));

    private static final Set<String> LIBRARY_KEYS = Set.of(PROJECTILES, SUMMON_PROFILES);

    private EntityLibraries() {
    }

    private record Wrapped(Codec<EquipmentDefinition> delegate) implements Codec<EquipmentDefinition> {

        @Override
        public <I> DataResult<Pair<EquipmentDefinition, I>> decode(DynamicOps<I> ops, I input) {
            boolean hasLibrary = false;
            for (String key : LIBRARY_KEYS) {
                hasLibrary |= ops.get(input, key).result().isPresent();
            }
            if (!hasLibrary) {
                return delegate.decode(ops, input);
            }
            JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
            I expanded = JsonOps.INSTANCE.convertTo(ops, expand(json));
            return delegate.decode(ops, expanded)
                    .map(pair -> Pair.of(pair.getFirst(), ops.empty()));
        }

        @Override
        public <I> DataResult<I> encode(EquipmentDefinition input, DynamicOps<I> ops, I prefix) {
            if (input.projectiles().isEmpty() && input.summonProfiles().isEmpty()) {
                return delegate.encode(input, ops, prefix);
            }
            return delegate.encodeStart(JsonOps.INSTANCE, input)
                    .map(EntityLibraries::compact)
                    .flatMap(json -> {
                        I converted = JsonOps.INSTANCE.convertTo(ops, json);
                        if (Objects.equals(prefix, ops.empty())) {
                            return DataResult.success(converted);
                        }
                        return ops.getMap(converted).flatMap(map -> ops.mergeToMap(prefix, map));
                    });
        }
    }

    public static Codec<EquipmentDefinition> wrap(Codec<EquipmentDefinition> codec) {
        return new Wrapped(codec);
    }

    public static JsonElement expand(JsonElement root) {
        return transform(root, false);
    }

    public static JsonElement compact(JsonElement root) {
        return transform(root, true);
    }

    private static JsonElement transform(JsonElement root, boolean compacting) {
        if (root == null || !root.isJsonObject()) {
            return root;
        }
        JsonObject copy = root.getAsJsonObject().deepCopy();
        for (Library library : LIBRARIES) {
            JsonObject entries = libraryOf(copy, library);
            if (entries == null) {
                continue;
            }
            for (Map.Entry<String, JsonElement> field : copy.entrySet()) {
                if (!LIBRARY_KEYS.contains(field.getKey())) {
                    field.setValue(walk(field.getValue(), library, entries, compacting));
                }
            }
        }
        return copy;
    }

    private static JsonObject libraryOf(JsonObject root, Library library) {
        JsonElement entries = root.get(library.key());
        return entries != null && entries.isJsonObject() && !entries.getAsJsonObject().isEmpty()
                ? entries.getAsJsonObject()
                : null;
    }

    private static Optional<JsonObject> entryFor(JsonObject node, Library library, JsonObject entries) {
        JsonElement ref = node.get(library.refKey());
        if (ref == null || !ref.isJsonPrimitive() || !ref.getAsJsonPrimitive().isString()
                || !LibraryRef.isRef(ref.getAsString())) {
            return Optional.empty();
        }
        JsonElement entry = entries.get(LibraryRef.nameOf(ref.getAsString()));
        if (entry == null || !entry.isJsonObject()) {
            CombatStyles.LOGGER.warn("Reference {} has no entry in this equipment's {}",
                    ref.getAsString(), library.key());
            return Optional.empty();
        }
        return Optional.of(entry.getAsJsonObject());
    }

    private static JsonElement walk(JsonElement node, Library library, JsonObject entries,
                                    boolean compacting) {
        if (node.isJsonArray()) {
            JsonArray array = node.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                array.set(index, walk(array.get(index), library, entries, compacting));
            }
            return array;
        }
        if (!node.isJsonObject()) {
            return node;
        }
        JsonObject object = node.getAsJsonObject();
        Optional<JsonObject> entry = entryFor(object, library, entries);
        if (entry.isPresent()) {
            if (!compacting) {
                return merged(entry.get(), object);
            }
            JsonObject sparse = sparse(object, entry.get(), library.refKey());
            return sameValue(library, object, merged(entry.get(), sparse)) ? sparse : object;
        }
        for (Map.Entry<String, JsonElement> field : object.entrySet()) {
            field.setValue(walk(field.getValue(), library, entries, compacting));
        }
        return object;
    }

    private static JsonObject merged(JsonObject base, JsonObject over) {
        JsonObject result = base.deepCopy();
        for (Map.Entry<String, JsonElement> field : over.entrySet()) {
            JsonElement existing = result.get(field.getKey());
            if (existing != null && existing.isJsonObject() && field.getValue().isJsonObject()) {
                result.add(field.getKey(), merged(existing.getAsJsonObject(),
                        field.getValue().getAsJsonObject()));
            } else {
                result.add(field.getKey(), field.getValue().deepCopy());
            }
        }
        return result;
    }

    private static JsonObject sparse(JsonObject full, JsonObject base, String refKey) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> field : full.entrySet()) {
            JsonElement inherited = base.get(field.getKey());
            JsonElement value = field.getValue();
            if (field.getKey().equals(refKey) || inherited == null) {
                result.add(field.getKey(), value.deepCopy());
            } else if (inherited.isJsonObject() && value.isJsonObject()) {
                JsonObject nested = sparse(value.getAsJsonObject(), inherited.getAsJsonObject(), refKey);
                if (!nested.isEmpty()) {
                    result.add(field.getKey(), nested);
                }
            } else if (!inherited.equals(value)) {
                result.add(field.getKey(), value.deepCopy());
            }
        }
        return result;
    }

    private static boolean sameValue(Library library, JsonObject expected, JsonObject rebuilt) {
        Optional<?> want = library.codec().parse(JsonOps.INSTANCE, expected).result();
        Optional<?> got = library.codec().parse(JsonOps.INSTANCE, rebuilt).result();
        return want.isPresent() && want.equals(got);
    }
}

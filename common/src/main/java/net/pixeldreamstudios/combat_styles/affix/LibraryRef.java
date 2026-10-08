package net.pixeldreamstudios.combat_styles.affix;

import com.google.common.collect.MapMaker;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.Map;
import java.util.Optional;

public final class LibraryRef {

    public static final String PROJECTILE_KEY = "projectile";
    public static final String SUMMON_KEY = "base_profile";
    public static final String PREFIX = "@";

    private static final Map<Object, String> ATTACHED = new MapMaker().weakKeys().makeMap();

    private LibraryRef() {
    }

    private record Wrapped<T>(Codec<T> delegate, String key) implements Codec<T> {

        @Override
        public <I> DataResult<Pair<T, I>> decode(DynamicOps<I> ops, I input) {
            return delegate.decode(ops, input).map(pair -> {
                ops.get(input, key).flatMap(ops::getStringValue).result()
                        .filter(LibraryRef::isRef)
                        .ifPresentOrElse(ref -> ATTACHED.put(pair.getFirst(), ref),
                                () -> ATTACHED.remove(pair.getFirst()));
                return pair;
            });
        }

        @Override
        public <I> DataResult<I> encode(T input, DynamicOps<I> ops, I prefix) {
            DataResult<I> encoded = delegate.encode(input, ops, prefix);
            String ref = ATTACHED.get(input);
            if (ref == null) {
                return encoded;
            }
            return encoded.flatMap(result -> ops.getMap(result).result().isEmpty()
                    ? DataResult.success(result)
                    : ops.mergeToMap(result, ops.createString(key), ops.createString(ref)));
        }
    }

    public static <T> Codec<T> wrap(Codec<T> codec, String key) {
        return new Wrapped<>(codec, key);
    }

    public static boolean isRef(String raw) {
        return raw != null && raw.startsWith(PREFIX) && raw.length() > PREFIX.length();
    }

    public static String nameOf(String raw) {
        return isRef(raw) ? raw.substring(PREFIX.length()) : raw;
    }

    public static String refTo(String name) {
        return PREFIX + name;
    }

    public static Optional<String> of(Object value) {
        return Optional.ofNullable(value == null ? null : ATTACHED.get(value));
    }
}

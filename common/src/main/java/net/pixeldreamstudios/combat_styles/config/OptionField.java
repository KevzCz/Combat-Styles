package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

public record OptionField(String key, JsonElement sample, String mount) {

    public static <T> OptionField of(String key, Codec<T> codec, T sample) {
        return of(key, codec, sample, null);
    }

    public static <T> OptionField of(String key, Codec<T> codec, T sample, String mount) {
        return new OptionField(key, codec.encodeStart(JsonOps.INSTANCE, sample).result()
                .orElse(JsonNull.INSTANCE), mount);
    }
}

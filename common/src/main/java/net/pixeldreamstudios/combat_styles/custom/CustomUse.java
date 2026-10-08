package net.pixeldreamstudios.combat_styles.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

public record CustomUse(String hook, Optional<JsonObject> options) {

    public static final String KEY_HOOK = "hook";
    public static final String KEY_OPTIONS = "options";
    private static final String NOT_AN_OBJECT = "Custom options must be an object";

    public static final Codec<JsonObject> OPTIONS_CODEC = ExtraCodecs.JSON.comapFlatMap(
            element -> element.isJsonObject()
                    ? DataResult.success(element.getAsJsonObject().deepCopy())
                    : DataResult.error(() -> NOT_AN_OBJECT),
            object -> (JsonElement) object.deepCopy());

    private static final Codec<CustomUse> FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf(KEY_HOOK).forGetter(CustomUse::hook),
            OPTIONS_CODEC.optionalFieldOf(KEY_OPTIONS).forGetter(CustomUse::options)
    ).apply(instance, CustomUse::new));

    public static final Codec<CustomUse> CODEC = Codec.either(Codec.STRING, FULL).xmap(
            either -> either.map(CustomUse::of, full -> full),
            Either::right);

    public static final CustomUse SAMPLE = of("");

    public CustomUse {
        hook = hook == null ? "" : hook;
        options = options == null ? Optional.empty() : options;
    }

    public static CustomUse of(String hook) {
        return new CustomUse(hook, Optional.empty());
    }

    public JsonObject optionsOrEmpty() {
        return options.map(JsonObject::deepCopy).orElseGet(JsonObject::new);
    }
}

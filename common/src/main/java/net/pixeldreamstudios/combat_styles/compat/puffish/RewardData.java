package net.pixeldreamstudios.combat_styles.compat.puffish;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

final class RewardData {

    private RewardData() {
    }

    static <R extends Reward> Result<R, Problem> create(RewardConfigContext context,
                                                       BiFunction<JsonElement, RewardConfigContext, R> factory) {
        return context.getData().andThen(data -> {
            try {
                return Result.<R, Problem>success(factory.apply(data, context));
            } catch (RuntimeException exception) {
                return Result.<R, Problem>failure(data.getPath().createProblem(String.valueOf(exception.getMessage())));
            }
        });
    }

    static JsonObject object(JsonElement data) {
        if (!data.getJson().isJsonObject()) {
            throw new IllegalArgumentException("Expected an object");
        }
        return data.getJson().getAsJsonObject();
    }

    static List<String> strings(JsonObject object, String key) {
        if (!object.has(key)) {
            return List.of();
        }
        if (!object.get(key).isJsonArray()) {
            return List.of(object.get(key).getAsString());
        }
        JsonArray array = object.getAsJsonArray(key);
        List<String> out = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            out.add(array.get(index).getAsString());
        }
        return out;
    }

    static List<JsonObject> objects(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return List.of();
        }
        JsonArray array = object.getAsJsonArray(key);
        List<JsonObject> out = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            out.add(array.get(index).getAsJsonObject());
        }
        return out;
    }

    static JsonElement child(JsonElement data, String key) {
        return data.getAsObject().andThen(object -> object.get(key)).getSuccess()
                .orElseThrow(() -> new IllegalArgumentException("Missing \"" + key + "\""));
    }
}

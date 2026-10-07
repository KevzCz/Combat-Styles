package net.pixeldreamstudios.combat_styles.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

public interface ChoiceSource {

    String selector();

    List<String> values();

    default List<OptionField> options(String value) {
        return List.of();
    }

    default JsonObject optionDefaults(String value) {
        return null;
    }

    default JsonElement template(String value, String scope, String key) {
        return null;
    }

    default List<String> allowed(String value, String scope) {
        return null;
    }
}

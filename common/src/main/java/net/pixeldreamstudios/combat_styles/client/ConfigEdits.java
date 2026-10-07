package net.pixeldreamstudios.combat_styles.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.config.EquipmentDropConfig;
import net.pixeldreamstudios.combat_styles.config.StatusEffectConfig;
import net.pixeldreamstudios.combat_styles.config.SummonTuning;
import net.pixeldreamstudios.combat_styles.network.ConfigEditHandler;
import net.pixeldreamstudios.combat_styles.network.EditConfigPayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ConfigEdits {

    private static final String KEY_OPERATOR_ONLY = "combat_styles.edit.operator_only";
    private static final String KEY_TOO_LARGE = "combat_styles.edit.too_large";

    private ConfigEdits() {
    }

    public static boolean connected() {
        return Minecraft.getInstance().getConnection() != null;
    }

    public static boolean permitted() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.isLocalServer() || (minecraft.player != null
                && minecraft.player.hasPermissions(ConfigEditHandler.OPERATOR_LEVEL));
    }

    public static Component operatorOnly() {
        return Component.translatable(KEY_OPERATOR_ONLY);
    }

    public static Component tooLarge() {
        return Component.translatable(KEY_TOO_LARGE);
    }

    public static void tell(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    public static <T> boolean save(EditConfigPayload.Kind kind, String id, Codec<T> codec, T value) {
        Optional<JsonElement> json = codec.encodeStart(JsonOps.INSTANCE, value)
                .resultOrPartial(error -> CombatStyles.LOGGER.error(
                        "Failed to encode {} {}: {}", kind, id, error));
        return json.isPresent() && send(new EditConfigPayload(kind, id, json.get().toString()));
    }

    public static boolean remove(EditConfigPayload.Kind kind, String id) {
        return send(new EditConfigPayload(kind, id, EditConfigPayload.NONE));
    }

    public static boolean send(EditConfigPayload payload) {
        if (payload.json().getBytes(StandardCharsets.UTF_8).length
                > EditConfigPayload.MAX_JSON_LENGTH) {
            CombatStyles.LOGGER.error("{} {} is too large to send to the server",
                    payload.kind(), payload.id());
            return false;
        }
        NetworkManager.sendEditConfig(payload);
        return true;
    }

    public static Set<EditConfigPayload> settings() {
        Set<EditConfigPayload> edits = new LinkedHashSet<>();
        edits.add(new EditConfigPayload(EditConfigPayload.Kind.MAIN_CONFIG,
                EditConfigPayload.NONE, CombatStylesConfig.rulesJson().toString()));
        addEntries(edits, EditConfigPayload.Kind.STATUS_EFFECT, StatusEffectConfig.toJson());
        addEntries(edits, EditConfigPayload.Kind.EQUIPMENT_DROP, EquipmentDropConfig.toJson());
        edits.add(new EditConfigPayload(EditConfigPayload.Kind.SUMMON_TUNING,
                EditConfigPayload.NONE, SummonTuning.toJson().toString()));
        return edits;
    }

    public static Set<EditConfigPayload> changedSince(Set<EditConfigPayload> baseline) {
        Set<EditConfigPayload> changed = new LinkedHashSet<>(settings());
        changed.removeAll(baseline);
        return changed;
    }

    private static void addEntries(Set<EditConfigPayload> edits, EditConfigPayload.Kind kind,
                                   JsonObject entries) {
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            edits.add(new EditConfigPayload(kind, entry.getKey(), entry.getValue().toString()));
        }
    }
}

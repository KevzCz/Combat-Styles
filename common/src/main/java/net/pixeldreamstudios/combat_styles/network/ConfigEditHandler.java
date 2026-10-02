package net.pixeldreamstudios.combat_styles.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.CustomAffixConfig;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.config.EquipmentDropConfig;
import net.pixeldreamstudios.combat_styles.config.StatusEffectConfig;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentOverrides;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.CustomAttack;
import net.pixeldreamstudios.combat_styles.style.CustomAttackConfig;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

public final class ConfigEditHandler {

    public static final int OPERATOR_LEVEL = 2;

    private static volatile boolean settingsChanged;
    private static volatile boolean dataChanged;

    private ConfigEditHandler() {
    }

    public static boolean permitted(ServerPlayer player) {
        return player != null && (player.hasPermissions(OPERATOR_LEVEL)
                || player.server.isSingleplayerOwner(player.getGameProfile()));
    }

    public static void handle(ServerPlayer player, EditConfigPayload payload) {
        if (!permitted(player)) {
            CombatStyles.LOGGER.warn("{} tried to edit {} {} without operator permission",
                    player.getGameProfile().getName(), payload.kind(), payload.id());
            return;
        }
        JsonElement json = parse(payload.json());
        boolean applied = switch (payload.kind()) {
            case MAIN_CONFIG -> CombatStylesConfig.applyEdit(json);
            case STATUS_EFFECT -> StatusEffectConfig.applyEntry(payload.id(), json);
            case ABILITY -> AbilityConfig.applyEntry(payload.id(), json);
            case EQUIPMENT_DROP -> EquipmentDropConfig.applyRarity(payload.id(), json);
            case EQUIPMENT -> saveEquipment(json);
            case EQUIPMENT_CLEAR -> clearEquipment(payload.id());
            case AFFIX -> saveAffix(json);
            case AFFIX_DELETE -> CustomAffixConfig.delete(payload.id());
            case ATTACK -> saveAttack(json);
            case ATTACK_DELETE -> deleteAttack(payload.id());
            case ATTACK_OVERRIDE -> saveAttackOverride(payload.id(), json);
        };
        if (!applied) {
            CombatStyles.LOGGER.warn("Edit {} {} from {} was not applied",
                    payload.kind(), payload.id(), player.getGameProfile().getName());
            return;
        }
        CombatStyles.LOGGER.info("{} applied {} {}", player.getGameProfile().getName(),
                payload.kind(), payload.id());
        if (isSetting(payload.kind())) {
            settingsChanged = true;
        } else {
            dataChanged = true;
        }
    }

    public static void flush(MinecraftServer server) {
        if (settingsChanged) {
            settingsChanged = false;
            NetworkManager.broadcastConfig(server);
        }
        if (dataChanged) {
            dataChanged = false;
            NetworkManager.broadcastDefaultStyles(server);
        }
    }

    private static boolean isSetting(EditConfigPayload.Kind kind) {
        return switch (kind) {
            case MAIN_CONFIG, STATUS_EFFECT, ABILITY, EQUIPMENT_DROP -> true;
            default -> false;
        };
    }

    private static JsonElement parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return JsonParser.parseString(raw);
        } catch (RuntimeException exception) {
            CombatStyles.LOGGER.warn("Rejected malformed edit payload", exception);
            return null;
        }
    }

    private static <T> T decode(Codec<T> codec, JsonElement json) {
        if (json == null) {
            return null;
        }
        return codec.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> CombatStyles.LOGGER.warn(
                        "Rejected edit payload: {}", error))
                .orElse(null);
    }

    private static boolean saveEquipment(JsonElement json) {
        EquipmentDefinition definition = decode(EquipmentDefinition.CODEC, json);
        if (definition == null || !EquipmentOverrides.save(definition)) {
            return false;
        }
        EquipmentRegistry.acceptLocal(definition);
        return true;
    }

    private static boolean clearEquipment(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        EquipmentOverrides.clear(id);
        EquipmentRegistry.restoreLocal(id);
        return true;
    }

    private static boolean saveAffix(JsonElement json) {
        StyleAffix affix = decode(StyleAffix.CODEC, json);
        return affix != null && CustomAffixConfig.save(affix);
    }

    private static boolean saveAttack(JsonElement json) {
        CustomAttack attack = decode(CustomAttack.CODEC, json);
        if (attack == null || !CustomAttackConfig.save(attack)) {
            return false;
        }
        StyleRegistry.rebuild();
        return true;
    }

    private static boolean deleteAttack(String id) {
        if (!CustomAttackConfig.delete(id)) {
            return false;
        }
        StyleRegistry.rebuild();
        return true;
    }

    private static boolean saveAttackOverride(String styleId, JsonElement json) {
        AttackOverride override = decode(AttackOverride.CODEC, json);
        return override != null && AttackOverrides.isEditable(styleId)
                && AttackOverrides.save(styleId, override);
    }
}

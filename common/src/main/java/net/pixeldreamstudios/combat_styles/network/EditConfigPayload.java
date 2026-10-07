package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.function.IntFunction;

public record EditConfigPayload(Kind kind, String id, String json)
        implements CustomPacketPayload {

    public enum Kind {
        MAIN_CONFIG,
        STATUS_EFFECT,
        ABILITY,
        EQUIPMENT_DROP,
        EQUIPMENT,
        EQUIPMENT_CLEAR,
        AFFIX,
        AFFIX_DELETE,
        ATTACK,
        ATTACK_DELETE,
        ATTACK_OVERRIDE,
        ATTACK_OVERRIDE_CLEAR,
        CUSTOM_STYLE,
        CUSTOM_STYLE_DELETE,
        DEFAULT_STYLE,
        DEFAULT_STYLE_CLEAR,
        STYLE_BOOK,
        STYLE_BOOK_CLEAR,
        WEAPON_ROLL,
        WEAPON_ROLL_CLEAR,
        SUMMON_ABILITY,
        SUMMON_ABILITY_CLEAR,
        SUMMON_TUNING;

        private static final IntFunction<Kind> BY_ID =
                ByIdMap.continuous(Kind::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    }

    public static final String NONE = "";
    public static final int MAX_JSON_LENGTH = 32000;

    public static final CustomPacketPayload.Type<EditConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("edit_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.idMapper(Kind.BY_ID, Kind::ordinal), EditConfigPayload::kind,
                    ByteBufCodecs.STRING_UTF8, EditConfigPayload::id,
                    ByteBufCodecs.stringUtf8(MAX_JSON_LENGTH), EditConfigPayload::json,
                    EditConfigPayload::new);

    @Override
    public Type<EditConfigPayload> type() {
        return TYPE;
    }
}

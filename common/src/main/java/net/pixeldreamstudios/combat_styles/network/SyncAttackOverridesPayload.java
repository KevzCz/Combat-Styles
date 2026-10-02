package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;

import java.util.Map;

public record SyncAttackOverridesPayload(Map<String, AttackOverride> overrides)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncAttackOverridesPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_attack_overrides"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAttackOverridesPayload>
            STREAM_CODEC = new StreamCodec<>() {

                @Override
                public SyncAttackOverridesPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new SyncAttackOverridesPayload(
                            AttackOverride.MAP_STREAM_CODEC.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer,
                                   SyncAttackOverridesPayload payload) {
                    AttackOverride.MAP_STREAM_CODEC.encode(buffer, payload.overrides());
                }
            };

    @Override
    public Type<SyncAttackOverridesPayload> type() {
        return TYPE;
    }
}

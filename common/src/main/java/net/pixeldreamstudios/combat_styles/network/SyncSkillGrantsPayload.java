package net.pixeldreamstudios.combat_styles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.skill.SkillGrants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SyncSkillGrantsPayload(List<String> equipment, List<String> styles, double budget,
                                     Map<String, String> icons)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSkillGrantsPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_skill_grants"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSkillGrantsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncSkillGrantsPayload::equipment,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncSkillGrantsPayload::styles,
                    ByteBufCodecs.DOUBLE, SyncSkillGrantsPayload::budget,
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
                    SyncSkillGrantsPayload::icons,
                    SyncSkillGrantsPayload::new);

    public static SyncSkillGrantsPayload of(SkillGrants.View view) {
        return new SyncSkillGrantsPayload(view.equipment(), view.styles(), view.budget(), view.icons());
    }

    public SkillGrants.View toView() {
        return new SkillGrants.View(equipment, styles, budget, icons);
    }

    @Override
    public Type<SyncSkillGrantsPayload> type() {
        return TYPE;
    }
}

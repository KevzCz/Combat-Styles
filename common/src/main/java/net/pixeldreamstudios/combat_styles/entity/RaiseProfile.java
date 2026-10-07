package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;
import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;

import java.util.List;
import java.util.Locale;

public enum RaiseProfile implements StringRepresentable {
    NONE,
    MATCHING,
    INHERIT;

    public static final Codec<RaiseProfile> CODEC = StringRepresentable.fromEnum(RaiseProfile::values);

    public SummonProfile profileFor(Player player, String entityId) {
        return profileFor(player, entityId, List.of());
    }

    public SummonProfile profileFor(Player player, String entityId, List<SummonProfile> own) {
        if (this == NONE || player == null) {
            return null;
        }
        for (SummonProfile profile : own) {
            if (profile.entity().map(id -> id.equals(entityId)).orElse(false)) {
                return profile;
            }
        }
        if (this == MATCHING) {
            return null;
        }
        SummonProfile matched = SummonProfiles.profileFor(player, entityId);
        return matched != null ? matched : SummonProfiles.profileFor(player);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}

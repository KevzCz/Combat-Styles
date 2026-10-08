package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;

public record SummonCompanion(int count, int respawnTicks, SummonEquipment equipment,
                              SummonStats attributes, boolean spectral, SummonFollow follow) {

    public static final int DEFAULT_COUNT = 1;
    public static final int DEFAULT_RESPAWN_TICKS = 100;

    public static final Codec<SummonCompanion> CODEC = Customs.wrap(SummonCompanion.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("count", DEFAULT_COUNT).forGetter(SummonCompanion::count),
            Codec.INT.optionalFieldOf("respawn_ticks", DEFAULT_RESPAWN_TICKS)
                    .forGetter(SummonCompanion::respawnTicks),
            SummonEquipment.CODEC.optionalFieldOf("equipment", SummonEquipment.NONE)
                    .forGetter(SummonCompanion::equipment),
            SummonStats.CODEC.optionalFieldOf("attributes", SummonStats.NONE)
                    .forGetter(SummonCompanion::attributes),
            Codec.BOOL.optionalFieldOf("spectral", Boolean.FALSE).forGetter(SummonCompanion::spectral),
            SummonFollow.CODEC.optionalFieldOf("follow", SummonFollow.DEFAULTS)
                    .forGetter(SummonCompanion::follow)
    ).apply(instance, SummonCompanion::new)));

    public static SummonCompanion ofCount(int count) {
        return new SummonCompanion(count, DEFAULT_RESPAWN_TICKS, SummonEquipment.NONE,
                SummonStats.NONE, false, SummonFollow.DEFAULTS);
    }

    public SummonCompanion {
        count = Math.max(0, count);
        respawnTicks = Math.max(0, respawnTicks);
        equipment = equipment == null ? SummonEquipment.NONE : equipment;
        attributes = attributes == null ? SummonStats.NONE : attributes;
        follow = follow == null ? SummonFollow.DEFAULTS : follow;
    }
}

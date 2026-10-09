package net.pixeldreamstudios.combat_styles.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record CustomUseList(List<CustomUse> uses) {

    public static final CustomUseList EMPTY = new CustomUseList(List.of());

    public static final Codec<CustomUseList> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    CustomUse.CODEC.listOf().optionalFieldOf(Customs.KEY, List.of())
                            .forGetter(CustomUseList::uses)
            ).apply(instance, CustomUseList::new));

    public CustomUseList {
        uses = uses == null ? List.of() : List.copyOf(uses);
    }
}

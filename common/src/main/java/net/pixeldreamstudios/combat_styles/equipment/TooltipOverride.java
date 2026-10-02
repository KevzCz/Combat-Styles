package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public record TooltipOverride(String header, List<String> lines) {

    public static final TooltipOverride NONE = new TooltipOverride("", List.of());

    public static final Codec<TooltipOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf(TriggeredEffect.TOOLTIP_HEADER, "")
                    .forGetter(TooltipOverride::header),
            Codec.STRING.listOf().optionalFieldOf(TriggeredEffect.TOOLTIP_LINES, List.of())
                    .forGetter(TooltipOverride::lines)
    ).apply(instance, TooltipOverride::new));

    public TooltipOverride {
        header = header == null ? "" : header.trim();
        lines = lines == null
                ? List.of()
                : lines.stream().filter(line -> line != null && !line.isBlank()).toList();
    }

    public boolean isEmpty() {
        return header.isEmpty() && lines.isEmpty();
    }

    public static MutableComponent text(String raw) {
        return Component.translatableWithFallback(raw, raw);
    }
}

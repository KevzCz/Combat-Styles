package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;

import net.pixeldreamstudios.combat_styles.api.SequenceInfo;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record StyleSequence(String name, List<StyleEntry> entries) implements SequenceInfo {

    public static final StyleSequence EMPTY = new StyleSequence("", List.of());

    public static final Codec<StyleSequence> CODEC = Customs.wrap(StyleSequence.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(StyleSequence::name),
            StyleEntry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(StyleSequence::entries)
    ).apply(instance, StyleSequence::new)));

    private static final int MAX_STREAM_ENTRIES = 64;

    public static final StreamCodec<ByteBuf, StyleSequence> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StyleSequence::name,
            StyleEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_STREAM_ENTRIES)), StyleSequence::entries,
            StyleSequence::new);

    public StyleSequence {
        entries = List.copyOf(entries);
    }

    public static StyleSequence ofIds(String name, List<String> styleIds) {
        List<StyleEntry> entries = new ArrayList<>(styleIds.size());
        for (String styleId : styleIds) {
            entries.add(StyleEntry.of(styleId));
        }
        return new StyleSequence(name, entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public List<String> styleIds() {
        List<String> ids = new ArrayList<>(entries.size());
        for (StyleEntry entry : entries) {
            ids.add(entry.styleId());
        }
        return ids;
    }

    public StyleEntry entry(int index) {
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    public List<StylePage> resolve() {
        List<StylePage> pages = new ArrayList<>(entries.size());
        for (StyleEntry entry : entries) {
            pages.add(StyleRegistry.get(CustomStyles.resolve(entry).styleId()));
        }
        return pages;
    }

    public StyleSequence with(String styleId) {
        return with(styleId, null);
    }

    public StyleSequence with(String styleId, String affixId) {
        List<StyleEntry> next = new ArrayList<>(entries);
        next.add(StyleEntry.of(styleId, affixId));
        return new StyleSequence(name, next);
    }

    public StyleSequence without(int index) {
        if (index < 0 || index >= entries.size()) {
            return this;
        }
        List<StyleEntry> next = new ArrayList<>(entries);
        next.remove(index);
        return new StyleSequence(name, next);
    }

    public StyleSequence withSound(int index, String soundId) {
        if (index < 0 || index >= entries.size()) {
            return this;
        }
        List<StyleEntry> next = new ArrayList<>(entries);
        next.set(index, soundId == null
                ? next.get(index).withoutSound()
                : next.get(index).withSound(soundId));
        return new StyleSequence(name, next);
    }

    public StyleSequence renamed(String newName) {
        return new StyleSequence(newName, entries);
    }
}

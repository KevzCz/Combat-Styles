package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.network.chat.Component;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class EffectLines extends AbstractList<Component> {

    public record Tagged(String tag, Component line) {
    }

    private static final String UNTAGGED = "";
    private static final String INDEX_SEPARATOR = "#";

    private static final String PHASE_SEPARATOR = "/";
    private static final String SECTION_SEPARATOR = ":";
    private static final String ORDINAL_SEPARATOR = "@";

    private final List<Tagged> entries = new ArrayList<>();
    private String section = UNTAGGED;
    private String phase = UNTAGGED;
    private String tag = UNTAGGED;

    public static EffectLines of(StyleAffix affix) {
        EffectLines lines = new EffectLines();
        AffixText.appendEffects(affix, lines, false);
        return lines;
    }

    public void section(String name) {
        section = name == null ? UNTAGGED : name;
        phase = UNTAGGED;
        tag = UNTAGGED;
    }

    public void addNested(List<Tagged> inner, UnaryOperator<Component> place) {
        String outer = currentTag();
        for (Tagged line : inner) {
            String combined = line.tag().isEmpty() ? outer
                    : outer.isEmpty() ? line.tag() : outer + SECTION_SEPARATOR + line.tag();
            entries.add(new Tagged(combined, place.apply(line.line())));
        }
    }

    public static String keyOf(String tag, int ordinal) {
        return tag + ORDINAL_SEPARATOR + ordinal;
    }

    public List<String> keys() {
        Map<String, Integer> seen = new HashMap<>();
        List<String> keys = new ArrayList<>(entries.size());
        for (Tagged entry : entries) {
            int ordinal = seen.merge(entry.tag(), 1, Integer::sum) - 1;
            keys.add(keyOf(entry.tag(), ordinal));
        }
        return keys;
    }

    public void phase(String name) {
        phase = name == null ? UNTAGGED : name;
        tag = UNTAGGED;
    }

    public void scope(String name) {
        tag = name == null ? UNTAGGED : name;
    }

    public void scope(String name, int index) {
        tag = name + INDEX_SEPARATOR + index;
    }

    private String currentTag() {
        String local = phase.isEmpty() ? tag : phase + PHASE_SEPARATOR + tag;
        if (section.isEmpty()) {
            return local;
        }
        return local.isEmpty() ? section : section + SECTION_SEPARATOR + local;
    }

    public String tagAt(int index) {
        return entries.get(index).tag();
    }

    public List<Tagged> tagged() {
        return List.copyOf(entries);
    }

    @Override
    public boolean add(Component line) {
        entries.add(new Tagged(currentTag(), line));
        return true;
    }

    public void addTagged(List<Tagged> lines) {
        entries.addAll(lines);
    }

    @Override
    public Component set(int index, Component line) {
        Tagged previous = entries.get(index);
        entries.set(index, new Tagged(previous.tag(), line));
        return previous.line();
    }

    @Override
    public Component get(int index) {
        return entries.get(index).line();
    }

    @Override
    public int size() {
        return entries.size();
    }
}

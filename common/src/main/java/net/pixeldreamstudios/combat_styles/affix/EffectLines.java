package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.network.chat.Component;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public final class EffectLines extends AbstractList<Component> {

    public record Tagged(String tag, Component line) {
    }

    private static final String UNTAGGED = "";
    private static final String INDEX_SEPARATOR = "#";

    private static final String PHASE_SEPARATOR = "/";

    private final List<Tagged> entries = new ArrayList<>();
    private String phase = UNTAGGED;
    private String tag = UNTAGGED;

    public static EffectLines of(StyleAffix affix) {
        EffectLines lines = new EffectLines();
        AffixText.appendEffects(affix, lines, false);
        return lines;
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
        return phase.isEmpty() ? tag : phase + PHASE_SEPARATOR + tag;
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
    public Component get(int index) {
        return entries.get(index).line();
    }

    @Override
    public int size() {
        return entries.size();
    }
}

package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.equipment.ComboCycle;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.ArrayList;
import java.util.List;

public record SwingContext(int slotIndex, int slotCount, String previousStyleId,
                           List<String> precedingStyleIds, String weaponCategory,
                           int hitsLanded, boolean sameTarget, int livePosition) {

    public static final SwingContext UNKNOWN = new SwingContext(-1, 0, null, List.of(),
            null, 0, false, -1);

    public static SwingContext of(List<StyleEntry> entries, int index) {
        return of(entries, index, null);
    }

    public static SwingContext of(List<StyleEntry> entries, int index, int[] live) {
        if (entries == null || entries.isEmpty() || index < 0 || index >= entries.size()) {
            return UNKNOWN;
        }
        int[] cycle = live == null || live.length == 0
                ? ComboCycle.all(entries.size())
                : live;
        int position = ComboCycle.positionOf(cycle, index);
        if (position == ComboCycle.NOT_LIVE) {
            cycle = ComboCycle.all(entries.size());
            position = index;
        }
        int previousSlot = cycle[Math.floorMod(position - 1, cycle.length)];
        List<String> preceding = new ArrayList<>();
        for (int i = 0; i < position; i++) {
            preceding.add(entries.get(cycle[i]).styleId());
        }
        return new SwingContext(index, cycle.length, entries.get(previousSlot).styleId(),
                List.copyOf(preceding), null, 0, false, position);
    }

    public SwingContext withWeaponCategory(String category) {
        return new SwingContext(slotIndex, slotCount, previousStyleId, precedingStyleIds,
                category, hitsLanded, sameTarget, livePosition);
    }

    public SwingContext withHistory(int hits, boolean repeatedTarget) {
        return new SwingContext(slotIndex, slotCount, previousStyleId, precedingStyleIds,
                weaponCategory, hits, repeatedTarget, livePosition);
    }

    public List<String> precedingCategories() {
        List<String> categories = new ArrayList<>();
        for (String styleId : precedingStyleIds) {
            StylePage page = StyleRegistry.get(styleId);
            if (page != null) {
                categories.add(page.category());
            }
        }
        return categories;
    }

    public boolean isKnown() {
        return slotIndex >= 0 && slotCount > 0;
    }

    public boolean isFirst() {
        return livePosition == 0;
    }

    public boolean isLast() {
        return slotCount > 0 && livePosition == slotCount - 1;
    }

    public String previousCategory() {
        StylePage page = StyleRegistry.get(previousStyleId);
        return page == null ? null : page.category();
    }
}

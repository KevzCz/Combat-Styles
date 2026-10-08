package net.pixeldreamstudios.combat_styles.style;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StyleIdRule {

    public static final String SLOT_SEPARATOR = "/";

    public record Candidate(String source, int slot, boolean defines, boolean preset) {

        public String id() {
            return idOf(source, slot);
        }
    }

    private static final Comparator<Candidate> ORDER = Comparator
            .comparing((Candidate candidate) -> !candidate.defines())
            .thenComparing(candidate -> !candidate.preset())
            .thenComparing(Candidate::source)
            .thenComparingInt(Candidate::slot);

    private StyleIdRule() {
    }

    public static String idOf(String source, int slot) {
        return source + SLOT_SEPARATOR + slot;
    }

    public static List<Candidate> ordered(List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<Candidate> sorted = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (candidate != null && seen.add(candidate.id())) {
                sorted.add(candidate);
            }
        }
        sorted.sort(ORDER);
        return sorted;
    }

    public static Candidate canonical(List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Candidate best = null;
        for (Candidate candidate : candidates) {
            if (candidate != null && (best == null || ORDER.compare(candidate, best) < 0)) {
                best = candidate;
            }
        }
        return best;
    }
}

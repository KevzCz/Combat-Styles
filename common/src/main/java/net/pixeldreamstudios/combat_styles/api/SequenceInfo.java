package net.pixeldreamstudios.combat_styles.api;

import java.util.List;

public interface SequenceInfo {

    String name();

    boolean isEmpty();

    int size();

    List<String> styleIds();
}

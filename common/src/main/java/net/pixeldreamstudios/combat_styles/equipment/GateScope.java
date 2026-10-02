package net.pixeldreamstudios.combat_styles.equipment;

public enum GateScope {

    EFFECT(""),
    GROUP("group:"),
    HELD("held:"),
    LOCKOUT("lockout:");

    private final String prefix;

    GateScope(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}

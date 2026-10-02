package net.pixeldreamstudios.combat_styles.style;

public final class ExtraAttackScope {

    public record Frame(AttackBehaviour behaviour, SlashAnchor anchor) {

        public Frame {
            anchor = anchor == null ? SlashAnchor.WEAPON : anchor;
        }
    }

    private static final ThreadLocal<Frame> ACTIVE = new ThreadLocal<>();

    private ExtraAttackScope() {
    }

    public static void push(AttackBehaviour behaviour) {
        push(behaviour, SlashAnchor.WEAPON);
    }

    public static void push(AttackBehaviour behaviour, SlashAnchor anchor) {
        ACTIVE.set(new Frame(behaviour, anchor));
    }

    public static void pop() {
        ACTIVE.remove();
    }

    public static AttackBehaviour active() {
        Frame frame = ACTIVE.get();
        return frame == null ? null : frame.behaviour();
    }

    public static SlashAnchor anchor() {
        Frame frame = ACTIVE.get();
        return frame == null ? SlashAnchor.WEAPON : frame.anchor();
    }
}

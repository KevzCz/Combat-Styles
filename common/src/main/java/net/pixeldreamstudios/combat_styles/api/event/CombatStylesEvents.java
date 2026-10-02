package net.pixeldreamstudios.combat_styles.api.event;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.ResolvedAffixes;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.affix.SwingContext;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class CombatStylesEvents {

    public static final Event<SwingListener> SWING = new Event<>();
    public static final Event<HitListener> HIT = new Event<>();
    public static final Event<WaveListener> WAVE = new Event<>();
    public static final Event<SequenceListener> SEQUENCE_CHANGED = new Event<>();
    public static final Event<MissListener> MISS = new Event<>();
    public static final Event<KillListener> KILL = new Event<>();
    public static final Event<EquipmentListener> EQUIPMENT = new Event<>();

    private CombatStylesEvents() {
    }

    public static final class Event<T> {

        private final List<T> listeners = new CopyOnWriteArrayList<>();

        public void register(T listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        public void unregister(T listener) {
            listeners.remove(listener);
        }

        public void forEach(Consumer<T> action) {
            for (T listener : listeners) {
                try {
                    action.accept(listener);
                } catch (Throwable throwable) {
                    CombatStyles.LOGGER.error("Combat Styles event listener failed", throwable);
                }
            }
        }

        public boolean allow(Predicate<T> test) {
            for (T listener : listeners) {
                try {
                    if (!test.test(listener)) {
                        return false;
                    }
                } catch (Throwable throwable) {
                    CombatStyles.LOGGER.error("Combat Styles event listener failed", throwable);
                }
            }
            return true;
        }

        public boolean isEmpty() {
            return listeners.isEmpty();
        }
    }

    @FunctionalInterface
    public interface SwingListener {

        boolean onSwing(SwingEvent event);
    }

    @FunctionalInterface
    public interface HitListener {

        boolean onHit(HitEvent event);
    }

    @FunctionalInterface
    public interface WaveListener {

        boolean onWave(WaveEvent event);
    }

    @FunctionalInterface
    public interface MissListener {

        boolean onMiss(MissEvent event);
    }

    @FunctionalInterface
    public interface KillListener {

        void onKill(KillEvent event);
    }

    @FunctionalInterface
    public interface EquipmentListener {

        boolean onEquipmentTrigger(EquipmentEvent event);
    }

    @FunctionalInterface
    public interface SequenceListener {

        void onSequenceChanged(ItemStack stack, StyleSequence previous, StyleSequence current);
    }

    public record MissEvent(Player player, ItemStack weapon, int comboCount,
                            SwingContext context) {
    }

    public record KillEvent(Player player, LivingEntity target, ItemStack weapon,
                           ResolvedAffixes affixes, int comboCount) {
    }

    public record EquipmentEvent(Player player, String equipmentId, ItemStack weapon,
                                 int slotIndex, boolean missPhase) {
    }

    public record SwingEvent(Player player, ItemStack weapon, int comboCount,
                             WeaponAttributes.Attack attack, ResolvedAffixes affixes,
                             SwingContext context, float baseDamage) {
    }

    public record HitEvent(Player player, LivingEntity target, ItemStack weapon,
                           StyleAffix affix, ResolvedAffixes affixes,
                           SwingContext context, float dealtDamage) {
    }

    public record WaveEvent(Player player, StyleAffix affix, float damage,
                            String texture, int frames, boolean thrusting) {
    }
}

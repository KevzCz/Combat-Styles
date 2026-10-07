package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.equipment.AbilityTuning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AbilityCaster {

    public static final String IDLE = "idle";
    private static final long UNSCHEDULED = -1L;
    private static final int UNBOUND_SLOT = 0;
    private static final String TAG_PROFILE = "AbilityProfile";
    private static final String TAG_SLOT = "AbilitySlot";
    private static final String TAG_STEP = "AbilityStep";
    private static final String TAG_LIFE = "AbilityLife";
    private static final String TAG_FINALE = "AbilityFinaleSpent";
    private static final int NO_LIFE = -1;

    private String current = IDLE;
    private int elapsed;
    private int remaining;
    private String queued;
    private Vec3 castStart;
    private final Map<String, Long> readyAt = new HashMap<>();
    private long nextRecastAt = UNSCHEDULED;
    private int step;
    private int slot = UNBOUND_SLOT;
    private String profileKey;
    private int life = NO_LIFE;
    private boolean finaleSpent;
    private AbilityTuning entryTuning;
    private final Set<String> entryAbilities = new HashSet<>();

    public boolean acting() {
        return !IDLE.equals(current);
    }

    public String current() {
        return current;
    }

    public int elapsed() {
        return elapsed;
    }

    public Vec3 castStart() {
        return castStart;
    }

    public void begin(String ability, int duration, int cooldown, long now, Vec3 start) {
        current = ability == null || ability.isBlank() ? IDLE : AbilityConfig.key(ability);
        elapsed = 0;
        remaining = Math.max(1, duration);
        castStart = start;
        if (cooldown > 0 && acting()) {
            readyAt.put(current, now + remaining + cooldown);
        }
    }

    public boolean advance() {
        elapsed++;
        return --remaining <= 0;
    }

    public void useEntry(Optional<AbilityTuning> tuning, String... abilities) {
        entryTuning = tuning == null ? null : tuning.orElse(null);
        entryAbilities.clear();
        for (String ability : abilities) {
            if (ability != null && !ability.isBlank()) {
                entryAbilities.add(AbilityConfig.key(ability));
            }
        }
    }

    public boolean holdsEntryFor(String ability) {
        return ability != null && entryAbilities.contains(AbilityConfig.key(ability));
    }

    public AbilityConfig.Entry tuned(String ability, AbilityConfig.Entry base) {
        return entryTuning == null || base == null || !holdsEntryFor(ability)
                ? base
                : entryTuning.over(base);
    }

    public void stop() {
        entryTuning = null;
        entryAbilities.clear();
        current = IDLE;
        elapsed = 0;
        remaining = 0;
        castStart = null;
    }

    public void queue(String ability) {
        queued = ability;
    }

    public String takeQueued() {
        String next = queued;
        queued = null;
        return next;
    }

    public void clearQueue() {
        queued = null;
    }

    public void holdUntil(String ability, long tick) {
        if (ability != null && !ability.isBlank()) {
            readyAt.merge(AbilityConfig.key(ability), tick, Math::max);
        }
    }

    public boolean ready(String ability, long now) {
        Long until = readyAt.get(AbilityConfig.key(ability));
        return until == null || now >= until;
    }

    public boolean recastDue(long now, int interval) {
        if (interval <= 0) {
            nextRecastAt = UNSCHEDULED;
            return false;
        }
        if (nextRecastAt == UNSCHEDULED) {
            nextRecastAt = now + interval;
            return false;
        }
        return now >= nextRecastAt;
    }

    public void scheduleRecast(long now, int interval) {
        nextRecastAt = interval <= 0 ? UNSCHEDULED : now + interval;
    }

    public int nextStep() {
        return step++;
    }

    public void bind(String key, int spawnSlot) {
        profileKey = key;
        slot = spawnSlot;
    }

    public String profileKey() {
        return profileKey;
    }

    public int slot() {
        return slot;
    }

    public void setLife(int ticks) {
        life = ticks > 0 ? ticks : NO_LIFE;
        finaleSpent = false;
    }

    public boolean hasLife() {
        return life != NO_LIFE;
    }

    public int life() {
        return life;
    }

    public boolean tickLife() {
        if (life == NO_LIFE) {
            return false;
        }
        if (life > 0) {
            life--;
        }
        return life <= 0;
    }

    public boolean finaleSpent() {
        return finaleSpent;
    }

    public void spendFinale() {
        finaleSpent = true;
    }

    public void save(CompoundTag tag) {
        if (profileKey != null) {
            tag.putString(TAG_PROFILE, profileKey);
        }
        tag.putInt(TAG_SLOT, slot);
        tag.putInt(TAG_STEP, step);
        if (life != NO_LIFE) {
            tag.putInt(TAG_LIFE, life);
        }
        if (finaleSpent) {
            tag.putBoolean(TAG_FINALE, true);
        }
    }

    public void load(CompoundTag tag) {
        profileKey = tag.contains(TAG_PROFILE) ? tag.getString(TAG_PROFILE) : null;
        slot = tag.getInt(TAG_SLOT);
        step = tag.getInt(TAG_STEP);
        life = tag.contains(TAG_LIFE) ? tag.getInt(TAG_LIFE) : NO_LIFE;
        finaleSpent = tag.getBoolean(TAG_FINALE);
    }
}

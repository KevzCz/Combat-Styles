package net.pixeldreamstudios.combat_styles.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.equipment.SpellLockState;
import net.pixeldreamstudios.combat_styles.equipment.SpellLocks;
import net.pixeldreamstudios.combat_styles.equipment.SpellTuning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SyncSpellLocksPayload(List<String> grants, List<Entry> locks,
                                    Map<String, SpellTuning> tunes, Map<String, String> inherits)
        implements CustomPacketPayload {

    public record Entry(String id, boolean school, SpellLocks.Mode mode, String item) {

        private static final StreamCodec<ByteBuf, SpellLocks.Mode> MODE_CODEC =
                ByteBufCodecs.BOOL.map(remove -> remove ? SpellLocks.Mode.REMOVE : SpellLocks.Mode.BLOCK,
                        mode -> mode == SpellLocks.Mode.REMOVE);

        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::id,
                ByteBufCodecs.BOOL, Entry::school,
                MODE_CODEC, Entry::mode,
                ByteBufCodecs.STRING_UTF8, Entry::item,
                Entry::new);
    }

    public static final CustomPacketPayload.Type<SyncSpellLocksPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_spell_locks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSpellLocksPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    SyncSpellLocksPayload::grants,
                    Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncSpellLocksPayload::locks,
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8,
                            ByteBufCodecs.fromCodec(SpellTuning.CODEC)), SyncSpellLocksPayload::tunes,
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
                    SyncSpellLocksPayload::inherits,
                    SyncSpellLocksPayload::new);

    public static SyncSpellLocksPayload of(SpellLockState.State state) {
        List<Entry> locks = new ArrayList<>();
        state.spells().forEach((id, lock) -> locks.add(new Entry(id, false, lock.mode(), lock.item())));
        state.schools().forEach((id, lock) -> locks.add(new Entry(id, true, lock.mode(), lock.item())));
        return new SyncSpellLocksPayload(state.grants(), locks, state.tunes(), state.inherits());
    }

    public SpellLockState.State toState() {
        Map<String, SpellLockState.Lock> spells = new LinkedHashMap<>();
        Map<String, SpellLockState.Lock> schools = new LinkedHashMap<>();
        for (Entry entry : locks) {
            SpellLockState.Lock lock = new SpellLockState.Lock(entry.mode(), entry.item());
            (entry.school() ? schools : spells).put(entry.id(), lock);
        }
        return new SpellLockState.State(grants, spells, schools, tunes, inherits);
    }

    @Override
    public Type<SyncSpellLocksPayload> type() {
        return TYPE;
    }
}

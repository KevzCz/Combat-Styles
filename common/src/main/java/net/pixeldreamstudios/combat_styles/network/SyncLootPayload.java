package net.pixeldreamstudios.combat_styles.network;
import net.pixeldreamstudios.combat_styles.entity.ScriptedAbilities;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.DefinitionLayer;
import net.pixeldreamstudios.combat_styles.damage.CritGroups;
import net.pixeldreamstudios.combat_styles.loot.BookRolls;
import net.pixeldreamstudios.combat_styles.loot.DropPools;
import net.pixeldreamstudios.combat_styles.loot.EquipmentDrops;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SyncLootPayload(Layer books, Layer rolls, Layer abilities, Layer crits, Layer drops,
                              Map<String, String> tagged)
        implements CustomPacketPayload {

    public record Layer(Map<String, String> definitions, Set<String> config, Set<String> data) {

        static Layer of(DefinitionLayer<?> layer) {
            return new Layer(layer.encodedAuthoritative(), layer.configIds(), layer.dataIds());
        }

        void applyTo(DefinitionLayer<?> layer) {
            layer.acceptEncodedSync(definitions, config, data);
        }
    }

    public static final CustomPacketPayload.Type<SyncLootPayload> TYPE =
            new CustomPacketPayload.Type<>(CombatStyles.id("sync_loot"));

    private static final int MAX_JSON_LENGTH = 1 << 20;
    private static final String ID_SEPARATOR = ",";

    private static final StreamCodec<ByteBuf, Map<String, String>> DEFINITIONS_CODEC =
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8,
                    ByteBufCodecs.stringUtf8(MAX_JSON_LENGTH));

    private static final StreamCodec<ByteBuf, List<String>> IDS_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    private static final StreamCodec<ByteBuf, Layer> LAYER_CODEC = new StreamCodec<>() {

        @Override
        public Layer decode(ByteBuf buffer) {
            Map<String, String> definitions = DEFINITIONS_CODEC.decode(buffer);
            List<String> config = IDS_CODEC.decode(buffer);
            List<String> data = IDS_CODEC.decode(buffer);
            return new Layer(definitions, Set.copyOf(config), Set.copyOf(data));
        }

        @Override
        public void encode(ByteBuf buffer, Layer layer) {
            DEFINITIONS_CODEC.encode(buffer, layer.definitions());
            IDS_CODEC.encode(buffer, List.copyOf(layer.config()));
            IDS_CODEC.encode(buffer, List.copyOf(layer.data()));
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLootPayload> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public SyncLootPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new SyncLootPayload(LAYER_CODEC.decode(buffer),
                            LAYER_CODEC.decode(buffer), LAYER_CODEC.decode(buffer),
                            LAYER_CODEC.decode(buffer), LAYER_CODEC.decode(buffer),
                            DEFINITIONS_CODEC.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, SyncLootPayload payload) {
                    LAYER_CODEC.encode(buffer, payload.books());
                    LAYER_CODEC.encode(buffer, payload.rolls());
                    LAYER_CODEC.encode(buffer, payload.abilities());
                    LAYER_CODEC.encode(buffer, payload.crits());
                    LAYER_CODEC.encode(buffer, payload.drops());
                    DEFINITIONS_CODEC.encode(buffer, payload.tagged());
                }
            };

    public static SyncLootPayload capture() {
        return new SyncLootPayload(Layer.of(BookRolls.DEFINITIONS),
                Layer.of(WeaponRolls.DEFINITIONS), Layer.of(ScriptedAbilities.DEFINITIONS),
                Layer.of(CritGroups.DEFINITIONS), Layer.of(DropPools.DEFINITIONS), encodeTagged());
    }

    public void applyToClient() {
        books.applyTo(BookRolls.DEFINITIONS);
        rolls.applyTo(WeaponRolls.DEFINITIONS);
        abilities.applyTo(ScriptedAbilities.DEFINITIONS);
        crits.applyTo(CritGroups.DEFINITIONS);
        drops.applyTo(DropPools.DEFINITIONS);
        Map<String, List<String>> decoded = new LinkedHashMap<>();
        tagged.forEach((rarity, ids) -> decoded.put(rarity, ids.isEmpty()
                ? List.of() : List.of(ids.split(ID_SEPARATOR))));
        EquipmentDrops.acceptSync(decoded);
        ScriptedAbilities.rebuildFromSync();
    }

    private static Map<String, String> encodeTagged() {
        Map<String, String> encoded = new LinkedHashMap<>();
        EquipmentDrops.taggedForSync().forEach((rarity, ids) ->
                encoded.put(rarity, String.join(ID_SEPARATOR, ids)));
        return encoded;
    }

    @Override
    public Type<SyncLootPayload> type() {
        return TYPE;
    }
}

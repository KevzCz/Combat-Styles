package net.pixeldreamstudios.combat_styles.custom;

import com.google.common.collect.MapMaker;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.pixeldreamstudios.combat_styles.damage.CritRolls;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumbers;
import net.pixeldreamstudios.combat_styles.equipment.UseScope;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class Customs {

    public static final String KEY = "custom";

    private static final Map<Object, List<CustomUse>> ATTACHED = new MapMaker().weakKeys().makeMap();
    private static final Map<Entity, Map<CustomUse, JsonObject>> BAKED = new MapMaker().weakKeys().makeMap();
    private static final Codec<List<CustomUse>> USES = Codec.lazyInitialized(() -> CustomUse.CODEC.listOf());

    private Customs() {
    }

    private record Wrapped<T>(String scope, Codec<T> delegate) implements Codec<T> {

        @Override
        public <I> DataResult<Pair<T, I>> decode(DynamicOps<I> ops, I input) {
            return delegate.decode(ops, input).map(pair -> {
                attach(pair.getFirst(), read(ops, input));
                return pair;
            });
        }

        @Override
        public <I> DataResult<I> encode(T input, DynamicOps<I> ops, I prefix) {
            DataResult<I> encoded = delegate.encode(input, ops, prefix);
            List<CustomUse> uses = of(input);
            if (uses.isEmpty()) {
                return encoded;
            }
            return encoded.flatMap(result -> {
                if (ops.getMap(result).result().isEmpty()) {
                    return DataResult.success(result);
                }
                return USES.encodeStart(ops, uses)
                        .flatMap(value -> ops.mergeToMap(result, ops.createString(KEY), value));
            });
        }

        @Override
        public String toString() {
            return "Custom[" + scope + "]" + delegate;
        }
    }

    public static <T> Codec<T> wrap(Class<T> type, Codec<T> codec) {
        return new Wrapped<>(CustomScopes.of(type), codec);
    }

    public static String scopeOf(Codec<?> codec) {
        if (codec instanceof Wrapped<?> wrapped) {
            return wrapped.scope();
        }
        if (codec instanceof ListCodec<?> list) {
            return scopeOf(list.elementCodec());
        }
        if (codec instanceof UnboundedMapCodec<?, ?> map) {
            return scopeOf(map.elementCodec());
        }
        return null;
    }

    private static <I> List<CustomUse> read(DynamicOps<I> ops, I input) {
        return ops.get(input, KEY).result()
                .flatMap(value -> USES.parse(ops, value).result())
                .orElse(List.of());
    }

    public static void attach(Object box, List<CustomUse> uses) {
        if (box == null) {
            return;
        }
        if (uses == null || uses.isEmpty()) {
            ATTACHED.remove(box);
            return;
        }
        ATTACHED.put(box, List.copyOf(uses));
    }

    public static List<CustomUse> of(Object box) {
        if (box == null) {
            return List.of();
        }
        List<CustomUse> uses = ATTACHED.get(box);
        return uses == null ? List.of() : uses;
    }

    public static boolean has(Object box) {
        return !of(box).isEmpty();
    }

    public static <T> T copy(Object from, T to) {
        List<CustomUse> uses = of(from);
        if (to != null && !uses.isEmpty() && from != to) {
            ATTACHED.put(to, uses);
        }
        return to;
    }

    public static <T> T merged(Object first, Object second, T to) {
        List<CustomUse> combined = new ArrayList<>(of(first));
        for (CustomUse use : of(second)) {
            if (!combined.contains(use)) {
                combined.add(use);
            }
        }
        if (to != null && !combined.isEmpty()) {
            ATTACHED.put(to, List.copyOf(combined));
        }
        return to;
    }

    private interface Call {
        void run(CustomHook hook, CustomContext context);
    }

    private static void each(String scope, Object box, Player owner, Entity actor, LivingEntity target,
                             Call call) {
        for (CustomUse use : of(box)) {
            CustomHook hook = CustomRegistry.get(use.hook());
            if (!CustomRegistry.fits(hook, scope)) {
                continue;
            }
            UseScope.within(actor, () -> ScaledNumbers.withOwner(owner, () -> {
                call.run(hook, new CustomContext(scope, box, bakedOptions(use, owner, actor),
                        owner, actor, target));
                return null;
            }));
        }
    }

    private static JsonObject bakedOptions(CustomUse use, Player owner, Entity actor) {
        JsonObject options = use.optionsOrEmpty();
        if (!(actor instanceof Projectile)) {
            return ScaledNumbers.baked(options, owner);
        }
        return BAKED.computeIfAbsent(actor, ignored -> new IdentityHashMap<>())
                .computeIfAbsent(use, ignored -> CritRolls.carriedBy(actor,
                        () -> ScaledNumbers.baked(options, owner)));
    }

    public static List<Component> tooltip(String scope, Object box) {
        List<Component> lines = new ArrayList<>();
        for (CustomUse use : of(box)) {
            CustomHook hook = CustomRegistry.get(use.hook());
            if (CustomRegistry.fits(hook, scope)) {
                lines.addAll(hook.tooltip(use.optionsOrEmpty()));
            }
        }
        return lines;
    }

    public static void apply(String scope, Object box, Player owner, Entity actor, LivingEntity target) {
        each(scope, box, owner, actor, target, CustomHook::onApply);
    }

    public static void spawned(String scope, Object box, Player owner, Entity actor, Entity spawned) {
        each(scope, box, owner, actor, null, (hook, context) -> hook.onSpawn(context, spawned));
    }

    public static void ticked(String scope, Object box, Player owner, Entity entity) {
        each(scope, box, owner, entity, null, (hook, context) -> hook.onTick(context, entity));
    }

    public static void hit(String scope, Object box, Player owner, Entity source, LivingEntity target) {
        each(scope, box, owner, source, target, (hook, context) -> hook.onHit(context, source, target));
    }

    public static void landed(String scope, Object box, Player owner, Entity entity) {
        each(scope, box, owner, entity, null, (hook, context) -> hook.onLand(context, entity));
    }

    public static boolean allows(String scope, Object box, Player owner, Entity actor, LivingEntity target) {
        for (CustomUse use : of(box)) {
            CustomHook hook = CustomRegistry.get(use.hook());
            if (CustomRegistry.fits(hook, scope) && !hook.test(new CustomContext(scope, box,
                    bakedOptions(use, owner, actor), owner, actor, target))) {
                return false;
            }
        }
        return true;
    }

    public static float modifyDamage(String scope, Object box, Player owner, Entity actor,
                                     LivingEntity target, float amount) {
        float modified = amount;
        for (CustomUse use : of(box)) {
            CustomHook hook = CustomRegistry.get(use.hook());
            if (CustomRegistry.fits(hook, scope)) {
                modified = hook.modifyDamage(new CustomContext(scope, box,
                        bakedOptions(use, owner, actor), owner, actor, target), modified);
            }
        }
        return modified;
    }
}

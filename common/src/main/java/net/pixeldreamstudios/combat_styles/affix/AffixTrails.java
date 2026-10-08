package net.pixeldreamstudios.combat_styles.affix;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.fx.TrailAppearance;
import net.bettercombat.api.fx.TrailAppearanceOverride;
import java.util.List;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.ChargeMirror;
import net.pixeldreamstudios.combat_styles.equipment.SequenceSlots;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.SwingHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

public final class AffixTrails {

    private AffixTrails() {
    }

    public static void register() {
        try {
            TrailAppearanceOverride.register(AffixTrails::resolve);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Unable to register the combat styles trail override", throwable);
        }
    }

    private static WeaponAttributes.Attack cachedAttack;
    private static ItemStack cachedStack;
    private static StyleAffix.TrailColor cachedColour;
    private static int cachedSwing = -1;
    private static int cachedSlot = -1;

    private static TrailAppearance resolve(Player player, ItemStack stack, TrailAppearance appearance) {
        WeaponAttributes.Attack attack = currentAttack(player);
        StyleAffix.TrailColor colour;
        int swing = EquipmentRuntime.swingCount(player);
        int slot = ChargeMirror.swungSlot();
        if (attack != null && attack == cachedAttack && stack == cachedStack
                && swing == cachedSwing && slot == cachedSlot) {
            colour = cachedColour;
        } else {
            colour = trailForCurrentAttack(player, stack);
            cachedAttack = attack;
            cachedStack = stack;
            cachedColour = colour;
            cachedSwing = swing;
            cachedSlot = slot;
        }
        if (colour == null) {
            return appearance;
        }
        TrailAppearance.Part part = new TrailAppearance.Part(colour.rgba(), colour.glows());
        return new TrailAppearance(part, part);
    }

    private static StyleAffix.TrailColor trailForCurrentAttack(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return null;
        }
        StyleSequence sequence;
        SwingHolder.push(player);
        try {
            sequence = SequenceService.sequenceOf(stack, player.getUUID());
        } finally {
            SwingHolder.pop();
        }
        if (sequence.isEmpty()) {
            return null;
        }

        WeaponAttributes.Attack attack = currentAttack(player);
        if (attack == null) {
            return null;
        }

        List<StyleEntry> entries = sequence.entries();
        int slot = SubstitutionScope.sameAttack(ChargeMirror.swungAttack(), attack)
                ? ChargeMirror.swungSlot()
                : SequenceSlots.indexOfAttack(entries, attack);
        if (slot < 0 || slot >= entries.size()) {
            slot = SequenceSlots.indexOfAttack(entries, attack);
        }
        if (slot < 0 || slot >= entries.size()) {
            return null;
        }
        StyleEntry entry = entries.get(slot);
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page != null && entry.attackOverride().isPresent()) {
            page = AttackOverrides.applyTo(page, entry.attackOverride().get());
        }
        if (page == null) {
            return null;
        }
        StyleAffix.TrailColor resolved = CombatEffectDealer.affixesForTrail(player,
                        entry.resolveAffixes(page).restrictedTo(page),
                        entry, slot, entries.size())
                .blendedTrail().orElse(null);
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "trail — slot " + CombatDebug.slot(slot, entries.size())
                    + " | declared " + CombatDebug.slot(ChargeMirror.swungSlot())
                    + " | swung " + animationOf(attack)
                    + " | live " + animationOf(liveAttack(player))
                    + " | swing " + EquipmentRuntime.swingCount(player)
                    + " => " + (resolved == null ? "none"
                            : String.format("#%08X", resolved.rgba())));
        }
        return resolved;
    }

    private static String animationOf(WeaponAttributes.Attack attack) {
        return attack == null ? "-" : attack.animation();
    }

    private static WeaponAttributes.Attack liveAttack(Player player) {
        if (!(player instanceof EntityPlayer_BetterCombat holder)) {
            return null;
        }
        AttackHand hand = holder.getCurrentAttack();
        return hand == null ? null : hand.attack();
    }

    private static WeaponAttributes.Attack currentAttack(Player player) {
        WeaponAttributes.Attack live = liveAttack(player);
        WeaponAttributes.Attack swung = ChargeMirror.swungAttack();
        if (swung == null || live == null) {
            return live;
        }
        return SubstitutionScope.sameAttack(swung, live) ? swung : live;
    }

}

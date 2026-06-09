package com.untitledmc.dungeons.combat;

import com.untitledmc.dungeons.stat.PlayerStatService;
import com.untitledmc.dungeons.stat.PlayerStats;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CombatListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatService playerStatService;
    private final DamageCalculator damageCalculator;
    private final CombatDebugService combatDebugService;

    public CombatListener(
            JavaPlugin plugin,
            PlayerStatService playerStatService,
            DamageCalculator damageCalculator,
            CombatDebugService combatDebugService
    ) {
        this.plugin = plugin;
        this.playerStatService = playerStatService;
        this.damageCalculator = damageCalculator;
        this.combatDebugService = combatDebugService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target) || shouldIgnoreTarget(target)) {
            return;
        }

        PlayerStats stats = playerStatService.recalculate(player);
        DamageResult result = damageCalculator.calculate(stats);
        event.setDamage(result.finalDamage());

        sendCombatFeedback(player, result);
        spawnDamageIndicator(target, result);
    }

    private boolean shouldIgnoreTarget(LivingEntity target) {
        if (target instanceof Player || target instanceof ArmorStand) {
            return true;
        }

        return target.isInvulnerable()
                || target.hasMetadata("NPC")
                || target.getScoreboardTags().contains("NPC");
    }

    private void sendCombatFeedback(Player player, DamageResult result) {
        Component message = Component.text(formatDamage(result.finalDamage()) + " damage", NamedTextColor.RED);
        if (result.wasCrit()) {
            message = Component.text("CRIT! ", NamedTextColor.YELLOW).append(message);
        }

        player.sendActionBar(message);

        if (!combatDebugService.isEnabled(player)) {
            return;
        }

        player.sendMessage(Component.text("Combat debug:", NamedTextColor.GOLD));
        player.sendMessage(Component.text(" - Base damage: " + formatStat(result.baseDamage()), NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Strength: " + formatStat(result.strength()), NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Crit chance: " + formatStat(result.critChance()) + "%", NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Crit damage: " + formatStat(result.critDamage()) + "%", NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Raw damage: " + formatStat(result.rawDamage()), NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Final damage: " + formatStat(result.finalDamage()), NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Crit: " + result.wasCrit(), NamedTextColor.GRAY));
    }

    private void spawnDamageIndicator(LivingEntity target, DamageResult result) {
        Location location = target.getLocation().add(0.0D, target.getHeight() + 0.35D, 0.0D);
        TextDisplay display = target.getWorld().spawn(location, TextDisplay.class, textDisplay -> {
            textDisplay.text(Component.text(
                    (result.wasCrit() ? "CRIT " : "") + formatDamage(result.finalDamage()),
                    result.wasCrit() ? NamedTextColor.YELLOW : NamedTextColor.RED
            ));
            textDisplay.setBillboard(Display.Billboard.CENTER);
            textDisplay.setShadowed(true);
            textDisplay.setSeeThrough(true);
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> removeIfValid(display), 20L);
    }

    private void removeIfValid(Entity entity) {
        if (entity.isValid()) {
            entity.remove();
        }
    }

    private String formatDamage(double value) {
        return String.valueOf((int) Math.round(value));
    }

    private String formatStat(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}

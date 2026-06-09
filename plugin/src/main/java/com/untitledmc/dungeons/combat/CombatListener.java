package com.untitledmc.dungeons.combat;

import com.untitledmc.dungeons.stat.PlayerStatService;
import com.untitledmc.dungeons.stat.PlayerStats;
import com.untitledmc.dungeons.mob.CustomMob;
import com.untitledmc.dungeons.mob.CustomMobService;
import com.untitledmc.dungeons.mob.MobRegistry;
import com.untitledmc.dungeons.stat.StatType;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class CombatListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatService playerStatService;
    private final DamageCalculator damageCalculator;
    private final CombatDebugService combatDebugService;
    private final CustomMobService customMobService;
    private final MobRegistry mobRegistry;
    private final NamespacedKey projectileMobIdKey;
    private final NamespacedKey projectileDamageKey;

    public CombatListener(
            JavaPlugin plugin,
            PlayerStatService playerStatService,
            DamageCalculator damageCalculator,
            CombatDebugService combatDebugService,
            CustomMobService customMobService,
            MobRegistry mobRegistry
    ) {
        this.plugin = plugin;
        this.playerStatService = playerStatService;
        this.damageCalculator = damageCalculator;
        this.combatDebugService = combatDebugService;
        this.customMobService = customMobService;
        this.mobRegistry = mobRegistry;
        this.projectileMobIdKey = new NamespacedKey(plugin, "custom_mob_projectile_id");
        this.projectileDamageKey = new NamespacedKey(plugin, "custom_mob_projectile_damage");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            handlePlayerDamage(event, player);
            return;
        }

        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Projectile projectile) {
            handleCustomMobProjectileDamage(event, projectile, player);
            return;
        }

        if (event.getEntity() instanceof Player player && event.getDamager() instanceof LivingEntity damager) {
            handleCustomMobDamage(event, damager, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof LivingEntity shooter) || !customMobService.isCustomMob(shooter)) {
            return;
        }

        if (!(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }

        String mobId = customMobService.getCustomMobId(shooter);
        CustomMob mob = mobRegistry.getMob(mobId).orElse(null);
        if (mob == null) {
            return;
        }

        PersistentDataContainer data = projectile.getPersistentDataContainer();
        data.set(projectileMobIdKey, PersistentDataType.STRING, mob.id());
        data.set(projectileDamageKey, PersistentDataType.DOUBLE, mob.damage());
    }

    private void handlePlayerDamage(EntityDamageByEntityEvent event, Player player) {
        if (!(event.getEntity() instanceof LivingEntity target) || shouldIgnoreTarget(target)) {
            return;
        }

        PlayerStats stats = playerStatService.recalculate(player);
        DamageResult result = damageCalculator.calculate(stats);
        if (customMobService.isCustomMob(target)) {
            event.setCancelled(true);
            customMobService.damageCustomMob(target, result.finalDamage());
            applyCustomMobKnockback(player, target);
            customMobService.updateMobDisplayName(target);

            sendCombatFeedback(player, result);
            spawnDamageIndicator(target, result);
            if (customMobService.getCurrentHealth(target) <= 0.0D) {
                defeatCustomMob(player, target);
            }
            return;
        }

        event.setDamage(result.finalDamage());

        sendCombatFeedback(player, result);
        spawnDamageIndicator(target, result);
    }

    private void handleCustomMobDamage(EntityDamageByEntityEvent event, LivingEntity damager, Player player) {
        if (!customMobService.isCustomMob(damager)) {
            return;
        }

        String mobId = customMobService.getCustomMobId(damager);
        CustomMob mob = mobRegistry.getMob(mobId).orElse(null);
        if (mob == null) {
            return;
        }

        PlayerStats stats = playerStatService.recalculate(player);
        double defense = Math.max(0.0D, stats.get(StatType.DEFENSE));
        double finalDamage = calculateIncomingMobDamage(mob.damage(), defense);

        event.setCancelled(true);
        applyDirectDamage(player, finalDamage);
        sendIncomingMobFeedback(player, mob, mob.damage(), defense, finalDamage);
    }

    private void handleCustomMobProjectileDamage(EntityDamageByEntityEvent event, Projectile projectile, Player player) {
        PersistentDataContainer data = projectile.getPersistentDataContainer();
        String mobId = data.get(projectileMobIdKey, PersistentDataType.STRING);
        if (mobId == null) {
            return;
        }

        CustomMob mob = mobRegistry.getMob(mobId).orElse(null);
        if (mob == null) {
            return;
        }

        double baseDamage = data.getOrDefault(projectileDamageKey, PersistentDataType.DOUBLE, mob.damage());
        PlayerStats stats = playerStatService.recalculate(player);
        double defense = Math.max(0.0D, stats.get(StatType.DEFENSE));
        double finalDamage = calculateIncomingMobDamage(baseDamage, defense);

        event.setCancelled(true);
        applyDirectDamage(player, finalDamage);
        sendIncomingMobFeedback(player, mob, baseDamage, defense, finalDamage);
    }

    private void defeatCustomMob(Player player, LivingEntity target) {
        String mobId = customMobService.getCustomMobId(target);
        String displayName = customMobService.getPlainDisplayName(mobId);
        target.remove();
        player.sendActionBar(Component.text("Defeated " + displayName + "!", NamedTextColor.GREEN));
    }

    private void applyDirectDamage(Player player, double finalDamage) {
        double health = player.getHealth();
        player.setHealth(Math.max(0.0D, health - finalDamage));
    }

    private double calculateIncomingMobDamage(double baseDamage, double defense) {
        double minimumDamage = Math.max(0.0D, plugin.getConfig().getDouble("combat.minimum_damage", 1.0D));
        return Math.max(minimumDamage, baseDamage * (100.0D / (100.0D + defense)));
    }

    private void applyCustomMobKnockback(Player attacker, LivingEntity target) {
        double strength = Math.max(0.0D, plugin.getConfig().getDouble("combat.mob_knockback", 0.3D));
        double vertical = Math.max(0.0D, plugin.getConfig().getDouble("combat.mob_knockback_vertical", 0.08D));
        String mobId = customMobService.getCustomMobId(target);
        if (mobId != null && mobId.toLowerCase(Locale.ROOT).contains("brute")) {
            strength *= 0.55D;
        }

        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        direction.setY(0.0D);
        if (direction.lengthSquared() <= 0.0001D) {
            direction = attacker.getLocation().getDirection().multiply(-1.0D);
            direction.setY(0.0D);
        }

        if (direction.lengthSquared() <= 0.0001D) {
            return;
        }

        Vector velocity = direction.normalize().multiply(strength);
        velocity.setY(vertical);
        target.setVelocity(target.getVelocity().add(velocity));
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

    private void sendIncomingMobFeedback(Player player, CustomMob mob, double baseDamage, double defense, double finalDamage) {
        player.sendActionBar(Component.text(
                "Hit by " + customMobService.getPlainDisplayName(mob.id()) + " for " + formatDamage(finalDamage) + " damage",
                NamedTextColor.RED
        ));

        if (!combatDebugService.isEnabled(player)) {
            return;
        }

        player.sendMessage(Component.text("Incoming mob damage:", NamedTextColor.GOLD));
        player.sendMessage(Component.text(" - Mob base damage: " + formatStat(baseDamage), NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Player defense: " + formatStat(defense), NamedTextColor.GRAY));
        player.sendMessage(Component.text(" - Final damage: " + formatStat(finalDamage), NamedTextColor.GRAY));
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

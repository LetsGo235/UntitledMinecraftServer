package com.untitledmc.dungeons.stat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ActionBarService {
    private final JavaPlugin plugin;
    private final PlayerStatService playerStatService;
    private final ManaService manaService;
    private BukkitTask task;

    public ActionBarService(JavaPlugin plugin, PlayerStatService playerStatService, ManaService manaService) {
        this.plugin = plugin;
        this.playerStatService = playerStatService;
        this.manaService = manaService;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::sendOnlinePlayerBars, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void sendOnlinePlayerBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats stats = playerStatService.getStats(player);
            int maxHealth = rounded(stats.get(StatType.HEALTH));
            int currentHealth = currentDisplayHealth(player, maxHealth);
            int currentMana = rounded(manaService.getCurrentMana(player));
            int maxMana = rounded(manaService.getMaxMana(player));

            player.sendActionBar(Component.text("❤ HP: " + currentHealth + "/" + maxHealth
                    + "   ✦ Mana: " + currentMana + "/" + maxMana));
        }
    }

    private int currentDisplayHealth(Player player, int maxHealth) {
        if (maxHealth <= 0) {
            return 0;
        }

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        double vanillaMaxHealth = maxHealthAttribute == null ? 20.0D : Math.max(1.0D, maxHealthAttribute.getValue());
        double ratio = Math.max(0.0D, Math.min(1.0D, player.getHealth() / vanillaMaxHealth));
        return (int) Math.ceil(maxHealth * ratio);
    }

    private int rounded(double value) {
        return (int) Math.round(value);
    }
}

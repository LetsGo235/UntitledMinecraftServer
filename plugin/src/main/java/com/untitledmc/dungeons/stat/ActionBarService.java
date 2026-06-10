package com.untitledmc.dungeons.stat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ActionBarService {
    private final JavaPlugin plugin;
    private final PlayerHealthService playerHealthService;
    private final ManaService manaService;
    private BukkitTask task;

    public ActionBarService(JavaPlugin plugin, PlayerHealthService playerHealthService, ManaService manaService) {
        this.plugin = plugin;
        this.playerHealthService = playerHealthService;
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
            int currentHealth = rounded(playerHealthService.getCurrentHealth(player));
            int maxHealth = rounded(playerHealthService.getMaxHealth(player));
            int currentMana = rounded(manaService.getCurrentMana(player));
            int maxMana = rounded(manaService.getMaxMana(player));

            player.sendActionBar(Component.text("\u2764 HP: " + currentHealth + "/" + maxHealth
                    + "   \u2726 Mana: " + currentMana + "/" + maxMana));
        }
    }

    private int rounded(double value) {
        return (int) Math.round(value);
    }
}

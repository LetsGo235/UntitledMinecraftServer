package com.untitledmc.dungeons.stat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ManaService {
    private final JavaPlugin plugin;
    private final PlayerStatService playerStatService;
    private final Map<UUID, Double> currentMana = new HashMap<>();
    private BukkitTask regenerationTask;

    public ManaService(JavaPlugin plugin, PlayerStatService playerStatService) {
        this.plugin = plugin;
        this.playerStatService = playerStatService;
    }

    public void start() {
        regenerationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::regenerateOnlinePlayers, 20L, 20L);
    }

    public void stop() {
        if (regenerationTask != null) {
            regenerationTask.cancel();
            regenerationTask = null;
        }
        currentMana.clear();
    }

    public void initialize(Player player) {
        currentMana.put(player.getUniqueId(), getMaxMana(player));
    }

    public void setToMax(Player player) {
        currentMana.put(player.getUniqueId(), getMaxMana(player));
    }

    public void clamp(Player player) {
        UUID playerId = player.getUniqueId();
        double maxMana = getMaxMana(player);
        double mana = currentMana.getOrDefault(playerId, maxMana);
        currentMana.put(playerId, clamp(mana, 0.0D, maxMana));
    }

    public void clear(Player player) {
        currentMana.remove(player.getUniqueId());
    }

    public double getCurrentMana(Player player) {
        clamp(player);
        return currentMana.getOrDefault(player.getUniqueId(), getMaxMana(player));
    }

    public double getMaxMana(Player player) {
        return Math.max(0.0D, playerStatService.getStats(player).get(StatType.MANA));
    }

    public boolean consumeMana(Player player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }

        double mana = getCurrentMana(player);
        if (mana < amount) {
            return false;
        }

        currentMana.put(player.getUniqueId(), mana - amount);
        return true;
    }

    public void addMana(Player player, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        UUID playerId = player.getUniqueId();
        double mana = currentMana.getOrDefault(playerId, getMaxMana(player));
        currentMana.put(playerId, clamp(mana + amount, 0.0D, getMaxMana(player)));
    }

    private void regenerateOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            double maxMana = getMaxMana(player);
            double mana = currentMana.getOrDefault(player.getUniqueId(), maxMana);
            currentMana.put(player.getUniqueId(), clamp(mana + Math.max(1.0D, maxMana * 0.02D), 0.0D, maxMana));
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

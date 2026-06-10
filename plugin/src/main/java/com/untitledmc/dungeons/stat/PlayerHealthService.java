package com.untitledmc.dungeons.stat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class PlayerHealthService {
    public static final double VANILLA_MAX_HEALTH = 20.0D;

    private final PlayerStatService playerStatService;
    private final Map<UUID, Double> currentHealth = new HashMap<>();

    public PlayerHealthService(PlayerStatService playerStatService) {
        this.playerStatService = playerStatService;
    }

    public double getCurrentHealth(Player player) {
        initialize(player);
        clampHealth(player);
        return currentHealth.getOrDefault(player.getUniqueId(), getMaxHealth(player));
    }

    public double getMaxHealth(Player player) {
        return Math.max(1.0D, playerStatService.getStats(player).get(StatType.HEALTH));
    }

    public void damage(Player player, double amount) {
        if (amount <= 0.0D || player.isDead()) {
            return;
        }

        initialize(player);
        UUID playerId = player.getUniqueId();
        currentHealth.put(playerId, clamp(currentHealth.getOrDefault(playerId, getMaxHealth(player)) - amount, 0.0D, getMaxHealth(player)));
        syncVanillaHealth(player);
    }

    public void heal(Player player, double amount) {
        if (amount <= 0.0D || player.isDead()) {
            return;
        }

        initialize(player);
        UUID playerId = player.getUniqueId();
        currentHealth.put(playerId, clamp(currentHealth.getOrDefault(playerId, getMaxHealth(player)) + amount, 0.0D, getMaxHealth(player)));
        syncVanillaHealth(player);
    }

    public void setToMax(Player player) {
        if (player.isDead()) {
            return;
        }

        currentHealth.put(player.getUniqueId(), getMaxHealth(player));
        syncVanillaHealth(player);
    }

    public void clampHealth(Player player) {
        if (player.isDead()) {
            return;
        }

        initialize(player);
        UUID playerId = player.getUniqueId();
        currentHealth.put(playerId, clamp(currentHealth.getOrDefault(playerId, getMaxHealth(player)), 0.0D, getMaxHealth(player)));
        syncVanillaHealth(player);
    }

    public void initialize(Player player) {
        currentHealth.putIfAbsent(player.getUniqueId(), getMaxHealth(player));
        syncVanillaHealth(player);
    }

    public void clear(Player player) {
        currentHealth.remove(player.getUniqueId());
    }

    public void syncVanillaHealth(Player player) {
        if (player.isDead()) {
            return;
        }

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null && maxHealthAttribute.getBaseValue() != VANILLA_MAX_HEALTH) {
            maxHealthAttribute.setBaseValue(VANILLA_MAX_HEALTH);
        }

        double maxHealth = getMaxHealth(player);
        double customHealth = currentHealth.getOrDefault(player.getUniqueId(), maxHealth);
        if (customHealth <= 0.0D) {
            player.setHealth(0.0D);
            return;
        }

        double visualHealth = Math.max(1.0D, Math.min(VANILLA_MAX_HEALTH, VANILLA_MAX_HEALTH * (customHealth / maxHealth)));
        player.setHealth(clamp(visualHealth, 0.0D, VANILLA_MAX_HEALTH));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

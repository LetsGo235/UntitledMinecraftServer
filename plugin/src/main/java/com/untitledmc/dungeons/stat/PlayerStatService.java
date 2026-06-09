package com.untitledmc.dungeons.stat;

import com.untitledmc.dungeons.item.CustomItem;
import com.untitledmc.dungeons.item.registry.ItemRegistry;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class PlayerStatService {
    private static final double MIN_PLAYER_MAX_HEALTH = 1.0D;
    private static final double MAX_PLAYER_MAX_HEALTH = 200.0D;

    private final ItemRegistry itemRegistry;
    private final NamespacedKey itemIdKey;
    private final EnumMap<StatType, Double> baseStats = new EnumMap<>(StatType.class);
    private final Map<UUID, PlayerStats> calculatedStats = new java.util.HashMap<>();

    public PlayerStatService(ItemRegistry itemRegistry, NamespacedKey itemIdKey) {
        this.itemRegistry = itemRegistry;
        this.itemIdKey = itemIdKey;
        seedBaseStats();
    }

    public PlayerStats recalculate(Player player) {
        PlayerStats stats = new PlayerStats();
        stats.reset();

        for (Map.Entry<StatType, Double> baseStat : baseStats.entrySet()) {
            stats.set(baseStat.getKey(), baseStat.getValue());
        }

        addCustomItemStats(player.getInventory().getItemInMainHand(), stats);
        addArmorStats(player.getInventory(), stats);

        PlayerStats snapshot = stats.copy();
        calculatedStats.put(player.getUniqueId(), snapshot);
        syncMaxHealth(player, snapshot);
        return snapshot.copy();
    }

    public PlayerStats getStats(Player player) {
        PlayerStats stats = calculatedStats.get(player.getUniqueId());
        if (stats == null) {
            return recalculate(player);
        }
        return stats.copy();
    }

    public void clear(Player player) {
        calculatedStats.remove(player.getUniqueId());
    }

    public void syncMaxHealth(Player player, PlayerStats stats) {
        if (player.isDead()) {
            return;
        }

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return;
        }

        double oldMaxHealth = Math.max(MIN_PLAYER_MAX_HEALTH, maxHealthAttribute.getValue());
        double oldHealthRatio = Math.max(0.0D, Math.min(1.0D, player.getHealth() / oldMaxHealth));
        double newMaxHealth = clamp(stats.get(StatType.HEALTH), MIN_PLAYER_MAX_HEALTH, MAX_PLAYER_MAX_HEALTH);

        maxHealthAttribute.setBaseValue(newMaxHealth);
        player.setHealth(clamp(newMaxHealth * oldHealthRatio, 0.0D, newMaxHealth));
    }

    private void addArmorStats(PlayerInventory inventory, PlayerStats stats) {
        for (ItemStack armorItem : inventory.getArmorContents()) {
            addCustomItemStats(armorItem, stats);
        }
    }

    private void addCustomItemStats(ItemStack stack, PlayerStats stats) {
        resolveCustomItem(stack).ifPresent(item -> {
            for (Map.Entry<String, Integer> stat : item.stats().entrySet()) {
                StatType.fromConfigKey(stat.getKey()).ifPresent(type -> stats.add(type, stat.getValue()));
            }
        });
    }

    private Optional<CustomItem> resolveCustomItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        String itemId = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return itemRegistry.get(itemId);
    }

    private void seedBaseStats() {
        for (StatType type : StatType.values()) {
            baseStats.put(type, 0.0D);
        }

        baseStats.put(StatType.HEALTH, 100.0D);
        baseStats.put(StatType.MANA, 100.0D);
        baseStats.put(StatType.SPEED, 100.0D);
        baseStats.put(StatType.CRIT_CHANCE, 30.0D);
        baseStats.put(StatType.CRIT_DAMAGE, 50.0D);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }
}

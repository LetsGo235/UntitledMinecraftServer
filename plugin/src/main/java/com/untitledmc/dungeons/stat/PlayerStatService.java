package com.untitledmc.dungeons.stat;

import com.untitledmc.dungeons.item.CustomItem;
import com.untitledmc.dungeons.item.registry.ItemRegistry;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class PlayerStatService {
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
}

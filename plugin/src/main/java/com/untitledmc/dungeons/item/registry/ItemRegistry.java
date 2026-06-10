package com.untitledmc.dungeons.item.registry;

import com.untitledmc.dungeons.item.CustomItem;
import com.untitledmc.dungeons.item.ItemType;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemRegistry {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<String, CustomItem> items = new LinkedHashMap<>();

    public ItemRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.exists()) {
            plugin.saveResource("items.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, CustomItem> loaded = new LinkedHashMap<>();

        for (String itemId : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(itemId);
            if (section == null) {
                continue;
            }

            try {
                CustomItem item = parseItem(itemId, section);
                loaded.put(item.id(), item);
            } catch (IllegalArgumentException exception) {
                logger.warning("Failed to load custom item '" + itemId + "': " + exception.getMessage());
            }
        }

        items.clear();
        items.putAll(loaded);
        logger.info("Loaded " + items.size() + " custom item(s).");
    }

    public Optional<CustomItem> get(String itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(normalizeId(itemId)));
    }

    public Collection<String> ids() {
        return List.copyOf(items.keySet());
    }

    public Collection<CustomItem> items() {
        return List.copyOf(items.values());
    }

    private CustomItem parseItem(String itemId, ConfigurationSection section) {
        String normalizedId = normalizeId(itemId);
        Material material = Material.matchMaterial(requireString(section, "material"));
        if (material == null) {
            throw new IllegalArgumentException("unknown material '" + section.getString("material") + "'");
        }

        String displayName = requireString(section, "display_name");
        String rarity = section.getString("rarity", "COMMON");
        Map<String, Integer> stats = parseStats(section.getConfigurationSection("stats"));
        CustomItem.Ability ability = parseAbility(section.getConfigurationSection("ability"));
        ItemType itemType = parseItemType(section.getString("item_type"), stats, ability);
        boolean stackable = section.getBoolean("stackable", false);
        int defaultMaxStackSize = stackable ? material.getMaxStackSize() : 1;
        int maxStackSize = section.getInt("max_stack_size", defaultMaxStackSize);
        if (!stackable) {
            maxStackSize = 1;
        } else if (maxStackSize < 1 || maxStackSize > 99) {
            throw new IllegalArgumentException("max_stack_size must be between 1 and 99");
        }
        List<String> lore = section.getStringList("lore");

        return new CustomItem(
                normalizedId,
                material,
                displayName,
                rarity,
                itemType,
                stackable,
                maxStackSize,
                stats,
                ability,
                lore
        );
    }

    private ItemType parseItemType(
            String configuredType,
            Map<String, Integer> stats,
            CustomItem.Ability ability
    ) {
        if (configuredType == null || configuredType.isBlank()) {
            return stats.containsKey("damage") || ability != null ? ItemType.WEAPON : ItemType.MISC;
        }

        try {
            return ItemType.valueOf(configuredType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown item_type '" + configuredType + "'");
        }
    }

    private Map<String, Integer> parseStats(ConfigurationSection section) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        if (section == null) {
            return stats;
        }

        for (String key : section.getKeys(false)) {
            stats.put(key, section.getInt(key));
        }
        return stats;
    }

    private CustomItem.Ability parseAbility(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String id = requireString(section, "id");
        String trigger = section.getString("trigger", "PASSIVE");
        int manaCost = section.getInt("mana_cost");
        int cooldownSeconds = section.getInt("cooldown_seconds");
        return new CustomItem.Ability(id, trigger, manaCost, cooldownSeconds);
    }

    private String requireString(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required field '" + path + "'");
        }
        return value;
    }

    private String normalizeId(String itemId) {
        return itemId.toLowerCase(Locale.ROOT);
    }
}

package com.untitledmc.dungeons.mob;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobRegistry {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<String, CustomMob> mobs = new LinkedHashMap<>();

    public MobRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "mobs.yml");
        if (!file.exists()) {
            plugin.saveResource("mobs.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, CustomMob> loaded = new LinkedHashMap<>();

        for (String mobId : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(mobId);
            if (section == null) {
                continue;
            }

            try {
                CustomMob mob = parseMob(mobId, section);
                loaded.put(mob.id(), mob);
            } catch (IllegalArgumentException exception) {
                logger.warning("Failed to load custom mob '" + mobId + "': " + exception.getMessage());
            }
        }

        mobs.clear();
        mobs.putAll(loaded);
        logger.info("Loaded " + mobs.size() + " custom mob(s).");
    }

    public Optional<CustomMob> getMob(String mobId) {
        if (mobId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mobs.get(normalizeId(mobId)));
    }

    public Collection<String> getMobIds() {
        return List.copyOf(mobs.keySet());
    }

    private CustomMob parseMob(String mobId, ConfigurationSection section) {
        String normalizedId = normalizeId(mobId);
        EntityType entityType = parseEntityType(requireString(section, "entity_type"));
        if (!entityType.isAlive() || !entityType.isSpawnable()) {
            throw new IllegalArgumentException("entity_type must be a spawnable living entity");
        }

        return new CustomMob(
                normalizedId,
                requireString(section, "display_name"),
                entityType,
                requirePositiveInt(section, "level"),
                requirePositiveDouble(section, "max_health"),
                requirePositiveDouble(section, "damage"),
                requireNonNegativeDouble(section, "defense"),
                requirePositiveDouble(section, "movement_speed"),
                requirePositiveDouble(section, "follow_range"),
                requireNonNegativeInt(section, "experience_reward"),
                requireNonNegativeInt(section, "coin_reward")
        );
    }

    private EntityType parseEntityType(String value) {
        try {
            return EntityType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown entity_type '" + value + "'");
        }
    }

    private String requireString(ConfigurationSection section, String path) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required field '" + path + "'");
        }
        return value;
    }

    private int requirePositiveInt(ConfigurationSection section, String path) {
        int value = section.getInt(path);
        if (value <= 0) {
            throw new IllegalArgumentException("'" + path + "' must be greater than 0");
        }
        return value;
    }

    private int requireNonNegativeInt(ConfigurationSection section, String path) {
        int value = section.getInt(path);
        if (value < 0) {
            throw new IllegalArgumentException("'" + path + "' must be 0 or greater");
        }
        return value;
    }

    private double requirePositiveDouble(ConfigurationSection section, String path) {
        double value = section.getDouble(path);
        if (value <= 0.0D) {
            throw new IllegalArgumentException("'" + path + "' must be greater than 0");
        }
        return value;
    }

    private double requireNonNegativeDouble(ConfigurationSection section, String path) {
        double value = section.getDouble(path);
        if (value < 0.0D) {
            throw new IllegalArgumentException("'" + path + "' must be 0 or greater");
        }
        return value;
    }

    private String normalizeId(String mobId) {
        return mobId.toLowerCase(Locale.ROOT);
    }
}

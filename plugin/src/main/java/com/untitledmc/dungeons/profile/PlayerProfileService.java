package com.untitledmc.dungeons.profile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerProfileService {
    private final JavaPlugin plugin;
    private final File profileFile;
    private final YamlConfiguration profileConfig;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();

    public PlayerProfileService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.profileFile = new File(plugin.getDataFolder(), "profiles.yml");
        this.profileConfig = YamlConfiguration.loadConfiguration(profileFile);
    }

    public PlayerProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    public PlayerProfile getProfile(UUID playerId) {
        return profiles.computeIfAbsent(playerId, this::loadProfile);
    }

    public void saveProfile(PlayerProfile profile) {
        String path = "players." + profile.getPlayerId();
        profileConfig.set(path + ".coins", profile.getCoins());
        profileConfig.set(path + ".dungeon-level", profile.getDungeonLevel());
        profileConfig.set(path + ".dungeon-xp", profile.getDungeonXp());
        saveFile();
    }

    public void saveAll() {
        for (PlayerProfile profile : profiles.values()) {
            String path = "players." + profile.getPlayerId();
            profileConfig.set(path + ".coins", profile.getCoins());
            profileConfig.set(path + ".dungeon-level", profile.getDungeonLevel());
            profileConfig.set(path + ".dungeon-xp", profile.getDungeonXp());
        }
        saveFile();
    }

    private PlayerProfile loadProfile(UUID playerId) {
        String path = "players." + playerId;
        return new PlayerProfile(
                playerId,
                profileConfig.getInt(path + ".coins", 0),
                profileConfig.getInt(path + ".dungeon-level", 1),
                profileConfig.getInt(path + ".dungeon-xp", 0)
        );
    }

    private void saveFile() {
        try {
            profileConfig.save(profileFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player profiles: " + exception.getMessage());
        }
    }
}

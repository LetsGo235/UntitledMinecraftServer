package com.untitledmc.dungeons.combat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class CombatDebugService {
    private final Set<UUID> enabledPlayers = new HashSet<>();

    public boolean toggle(Player player) {
        UUID playerId = player.getUniqueId();
        if (enabledPlayers.remove(playerId)) {
            return false;
        }

        enabledPlayers.add(playerId);
        return true;
    }

    public boolean isEnabled(Player player) {
        return enabledPlayers.contains(player.getUniqueId());
    }

    public void clear(Player player) {
        enabledPlayers.remove(player.getUniqueId());
    }
}

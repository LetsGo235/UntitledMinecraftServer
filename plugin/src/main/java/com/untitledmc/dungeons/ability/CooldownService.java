package com.untitledmc.dungeons.ability;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CooldownService {
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID playerId, String abilityId) {
        return getRemainingMillis(playerId, abilityId) > 0L;
    }

    public int getRemainingSeconds(UUID playerId, String abilityId) {
        long remainingMillis = getRemainingMillis(playerId, abilityId);
        return (int) Math.ceil(remainingMillis / 1000.0D);
    }

    public void setCooldown(UUID playerId, String abilityId, int seconds) {
        if (seconds <= 0) {
            return;
        }

        cooldowns.computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(normalize(abilityId), System.currentTimeMillis() + (seconds * 1000L));
    }

    public void clear(UUID playerId) {
        cooldowns.remove(playerId);
    }

    private long getRemainingMillis(UUID playerId, String abilityId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) {
            return 0L;
        }

        String normalizedId = normalize(abilityId);
        long expiresAt = playerCooldowns.getOrDefault(normalizedId, 0L);
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            playerCooldowns.remove(normalizedId);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(playerId);
            }
            return 0L;
        }
        return remaining;
    }

    private String normalize(String abilityId) {
        return abilityId.toLowerCase(Locale.ROOT);
    }
}

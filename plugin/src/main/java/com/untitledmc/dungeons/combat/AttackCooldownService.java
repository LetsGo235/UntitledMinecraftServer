package com.untitledmc.dungeons.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AttackCooldownService {
    private static final long DEFAULT_COOLDOWN_MS = 500L;

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastSuccessfulHitMillis = new HashMap<>();

    public AttackCooldownService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean canAttack(Player player) {
        return getRemainingCooldownMillis(player) <= 0L;
    }

    public void markSuccessfulHit(Player player) {
        lastSuccessfulHitMillis.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getRemainingCooldownMillis(Player player) {
        long cooldownMillis = getCooldownMillis(player);
        long lastHitMillis = lastSuccessfulHitMillis.getOrDefault(player.getUniqueId(), 0L);
        return Math.max(0L, cooldownMillis - (System.currentTimeMillis() - lastHitMillis));
    }

    public void clear(Player player) {
        lastSuccessfulHitMillis.remove(player.getUniqueId());
    }

    public long getCooldownMillis(Player player) {
        return Math.max(0L, plugin.getConfig().getLong("combat.player_attack_cooldown_ms", DEFAULT_COOLDOWN_MS));
    }
}

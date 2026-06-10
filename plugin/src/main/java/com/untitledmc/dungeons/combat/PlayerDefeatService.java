package com.untitledmc.dungeons.combat;

import com.untitledmc.dungeons.stat.ManaService;
import com.untitledmc.dungeons.stat.PlayerHealthService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDefeatService {
    private final JavaPlugin plugin;
    private final PlayerHealthService playerHealthService;
    private final ManaService manaService;
    private final Map<UUID, Long> invulnerableUntilMillis = new HashMap<>();

    public PlayerDefeatService(JavaPlugin plugin, PlayerHealthService playerHealthService, ManaService manaService) {
        this.plugin = plugin;
        this.playerHealthService = playerHealthService;
        this.manaService = manaService;
    }

    public boolean defeat(Player player) {
        if (playerHealthService.isDefeated(player)) {
            return false;
        }

        playerHealthService.setDefeated(player, true);
        int invulnerabilityTicks = Math.max(0, plugin.getConfig().getInt("combat.defeat_invulnerability_ticks", 60));
        invulnerableUntilMillis.put(
                player.getUniqueId(),
                System.currentTimeMillis() + (invulnerabilityTicks * 50L)
        );

        stabilizePlayer(player);
        playerHealthService.resetAfterDefeat(player);
        if (plugin.getConfig().getBoolean("combat.defeat_reset_mana", true)) {
            manaService.setToMax(player);
        }

        teleportAfterDefeat(player);
        playDefeatFeedback(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                clear(player);
                return;
            }

            stabilizePlayer(player);
            playerHealthService.resetAfterDefeat(player);
            if (plugin.getConfig().getBoolean("combat.defeat_reset_mana", true)) {
                manaService.setToMax(player);
            }
            playerHealthService.setDefeated(player, false);
        }, 1L);

        return true;
    }

    public boolean isInvulnerable(Player player) {
        Long until = invulnerableUntilMillis.get(player.getUniqueId());
        if (until == null) {
            return false;
        }

        if (System.currentTimeMillis() > until) {
            invulnerableUntilMillis.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    public void resetAfterRespawn(Player player) {
        stabilizePlayer(player);
        playerHealthService.resetAfterDefeat(player);
        if (plugin.getConfig().getBoolean("combat.defeat_reset_mana", true)) {
            manaService.setToMax(player);
        }
        playerHealthService.setDefeated(player, false);
    }

    public void ensureValidOnJoin(Player player) {
        stabilizePlayer(player);
        if (playerHealthService.getCurrentHealth(player) <= 0.0D) {
            playerHealthService.resetAfterDefeat(player);
        } else {
            playerHealthService.clampHealth(player);
        }
        playerHealthService.setDefeated(player, false);
    }

    public void clear(Player player) {
        invulnerableUntilMillis.remove(player.getUniqueId());
        playerHealthService.setDefeated(player, false);
    }

    private void stabilizePlayer(Player player) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null && maxHealthAttribute.getBaseValue() != PlayerHealthService.VANILLA_MAX_HEALTH) {
            maxHealthAttribute.setBaseValue(PlayerHealthService.VANILLA_MAX_HEALTH);
        }

        if (!player.isDead()) {
            player.setHealth(PlayerHealthService.VANILLA_MAX_HEALTH);
        }
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setNoDamageTicks(Math.max(player.getNoDamageTicks(), plugin.getConfig().getInt("combat.defeat_invulnerability_ticks", 60)));
    }

    private void teleportAfterDefeat(Player player) {
        if (!plugin.getConfig().getBoolean("combat.defeat_teleport_to_spawn", true)) {
            return;
        }

        Location respawnLocation = player.getRespawnLocation();
        if (respawnLocation == null) {
            respawnLocation = player.getWorld().getSpawnLocation();
        }
        player.teleport(respawnLocation);
    }

    private void playDefeatFeedback(Player player) {
        player.sendMessage(Component.text("You were defeated!", NamedTextColor.RED));
        player.sendActionBar(Component.text("You were defeated!", NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 0.7F, 1.4F);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0D, 1.0D, 0.0D), 30, 0.45D, 0.7D, 0.45D, 0.04D);
    }
}

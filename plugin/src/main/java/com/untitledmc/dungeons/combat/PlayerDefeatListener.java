package com.untitledmc.dungeons.combat;

import com.untitledmc.dungeons.stat.ManaService;
import com.untitledmc.dungeons.stat.PlayerHealthService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerDefeatListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerHealthService playerHealthService;
    private final ManaService manaService;
    private final PlayerDefeatService playerDefeatService;

    public PlayerDefeatListener(
            JavaPlugin plugin,
            PlayerHealthService playerHealthService,
            ManaService manaService,
            PlayerDefeatService playerDefeatService
    ) {
        this.plugin = plugin;
        this.playerHealthService = playerHealthService;
        this.manaService = manaService;
        this.playerDefeatService = playerDefeatService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (playerDefeatService.isInvulnerable(player) || playerHealthService.isDefeated(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> playerDefeatService.resetAfterRespawn(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            playerDefeatService.ensureValidOnJoin(player);
            manaService.clamp(player);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDefeatService.clear(event.getPlayer());
    }
}

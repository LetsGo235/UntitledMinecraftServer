package com.untitledmc.dungeons.stat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerStatListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStatService playerStatService;
    private final ManaService manaService;

    public PlayerStatListener(JavaPlugin plugin, PlayerStatService playerStatService, ManaService manaService) {
        this.plugin = plugin;
        this.playerStatService = playerStatService;
        this.manaService = manaService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerStatService.recalculate(event.getPlayer());
        manaService.initialize(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerStatService.clear(event.getPlayer());
        manaService.clear(event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        recalculateNextTick(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedMainHand(PlayerChangedMainHandEvent event) {
        recalculateNextTick(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            recalculateNextTick(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            recalculateNextTick(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            recalculateNow(player);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        recalculateNextTick(event.getPlayer());
    }

    private void recalculateNextTick(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> recalculateNow(player));
    }

    private void recalculateNow(Player player) {
        playerStatService.recalculate(player);
        manaService.clamp(player);
    }
}

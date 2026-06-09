package com.untitledmc.dungeons.ability;

import com.untitledmc.dungeons.item.CustomItem;
import com.untitledmc.dungeons.stat.ManaService;
import com.untitledmc.dungeons.stat.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class AbilityContext {
    private final Player player;
    private final ItemStack item;
    private final CustomItem customItem;
    private final PlayerStats stats;
    private final ManaService manaService;

    public AbilityContext(
            Player player,
            ItemStack item,
            CustomItem customItem,
            PlayerStats stats,
            ManaService manaService
    ) {
        this.player = player;
        this.item = item;
        this.customItem = customItem;
        this.stats = stats;
        this.manaService = manaService;
    }

    public Player player() {
        return player;
    }

    public ItemStack item() {
        return item;
    }

    public CustomItem customItem() {
        return customItem;
    }

    public PlayerStats stats() {
        return stats;
    }

    public ManaService manaService() {
        return manaService;
    }
}

package com.untitledmc.dungeons.ability;

import com.untitledmc.dungeons.item.CustomItem;
import com.untitledmc.dungeons.item.registry.ItemRegistry;
import com.untitledmc.dungeons.stat.ManaService;
import com.untitledmc.dungeons.stat.PlayerStatService;
import com.untitledmc.dungeons.stat.PlayerStats;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class AbilityListener implements Listener {
    private static final String RIGHT_CLICK_TRIGGER = "RIGHT_CLICK";

    private final NamespacedKey itemIdKey;
    private final ItemRegistry itemRegistry;
    private final PlayerStatService playerStatService;
    private final ManaService manaService;
    private final AbilityRegistry abilityRegistry;
    private final CooldownService cooldownService;

    public AbilityListener(
            NamespacedKey itemIdKey,
            ItemRegistry itemRegistry,
            PlayerStatService playerStatService,
            ManaService manaService,
            AbilityRegistry abilityRegistry,
            CooldownService cooldownService
    ) {
        this.itemIdKey = itemIdKey;
        this.itemRegistry = itemRegistry;
        this.playerStatService = playerStatService;
        this.manaService = manaService;
        this.abilityRegistry = abilityRegistry;
        this.cooldownService = cooldownService;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        CustomItem customItem = resolveCustomItem(item);
        if (customItem == null || customItem.ability() == null) {
            return;
        }

        CustomItem.Ability abilityData = customItem.ability();
        if (!RIGHT_CLICK_TRIGGER.equals(abilityData.trigger().toUpperCase(Locale.ROOT))) {
            return;
        }

        Ability ability = abilityRegistry.getAbility(abilityData.id()).orElse(null);
        if (ability == null) {
            return;
        }

        event.setCancelled(true);
        UUID playerId = player.getUniqueId();

        if (cooldownService.isOnCooldown(playerId, ability.getId())) {
            int seconds = cooldownService.getRemainingSeconds(playerId, ability.getId());
            player.sendMessage(Component.text("Ability on cooldown: " + seconds + "s remaining.", NamedTextColor.RED));
            return;
        }

        if (!manaService.consumeMana(player, abilityData.manaCost())) {
            int currentMana = (int) Math.floor(manaService.getCurrentMana(player));
            player.sendMessage(Component.text(
                    "Not enough mana. Need " + abilityData.manaCost() + ", have " + currentMana + ".",
                    NamedTextColor.AQUA
            ));
            return;
        }

        cooldownService.setCooldown(playerId, ability.getId(), abilityData.cooldownSeconds());
        PlayerStats stats = playerStatService.getStats(player);
        ability.execute(new AbilityContext(player, item, customItem, stats, manaService));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownService.clear(event.getPlayer().getUniqueId());
    }

    private boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private CustomItem resolveCustomItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }

        String itemId = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return itemRegistry.get(itemId).orElse(null);
    }
}

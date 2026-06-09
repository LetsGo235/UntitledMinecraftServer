package com.untitledmc.dungeons;

import com.untitledmc.dungeons.ability.AbilityListener;
import com.untitledmc.dungeons.ability.AbilityRegistry;
import com.untitledmc.dungeons.ability.CooldownService;
import com.untitledmc.dungeons.combat.CombatDebugService;
import com.untitledmc.dungeons.combat.CombatListener;
import com.untitledmc.dungeons.combat.DamageCalculator;
import com.untitledmc.dungeons.item.ItemBuilder;
import com.untitledmc.dungeons.item.ItemLoreRenderer;
import com.untitledmc.dungeons.item.command.DItemCommand;
import com.untitledmc.dungeons.item.registry.ItemRegistry;
import com.untitledmc.dungeons.stat.ActionBarService;
import com.untitledmc.dungeons.stat.ManaService;
import com.untitledmc.dungeons.stat.PlayerStatListener;
import com.untitledmc.dungeons.stat.PlayerStatService;
import com.untitledmc.dungeons.stat.command.UdsCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class UntitledDungeonsPlugin extends JavaPlugin {
    private ItemRegistry itemRegistry;
    private ManaService manaService;
    private ActionBarService actionBarService;
    private CooldownService cooldownService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        itemRegistry = new ItemRegistry(this);
        itemRegistry.load();

        NamespacedKey itemIdKey = new NamespacedKey(this, "item_id");
        ItemLoreRenderer loreRenderer = new ItemLoreRenderer();
        ItemBuilder itemBuilder = new ItemBuilder(itemIdKey, loreRenderer);
        DItemCommand dItemCommand = new DItemCommand(itemRegistry, itemBuilder);
        PluginCommand ditemCommand = getCommand("ditem");
        if (ditemCommand != null) {
            ditemCommand.setExecutor(dItemCommand);
            ditemCommand.setTabCompleter(dItemCommand);
        }

        PlayerStatService playerStatService = new PlayerStatService(itemRegistry, itemIdKey);
        manaService = new ManaService(this, playerStatService);
        actionBarService = new ActionBarService(this, playerStatService, manaService);
        AbilityRegistry abilityRegistry = new AbilityRegistry();
        abilityRegistry.registerDefaults();
        cooldownService = new CooldownService();
        CombatDebugService combatDebugService = new CombatDebugService();
        DamageCalculator damageCalculator = new DamageCalculator();

        UdsCommand udsCommand = new UdsCommand(this, itemRegistry, playerStatService, combatDebugService);
        PluginCommand command = getCommand("uds");
        if (command != null) {
            command.setExecutor(udsCommand);
            command.setTabCompleter(udsCommand);
        }

        getServer().getPluginManager().registerEvents(
                new PlayerStatListener(this, playerStatService, manaService, combatDebugService),
                this
        );
        getServer().getPluginManager().registerEvents(new AbilityListener(
                itemIdKey,
                itemRegistry,
                playerStatService,
                manaService,
                abilityRegistry,
                cooldownService
        ), this);
        getServer().getPluginManager().registerEvents(
                new CombatListener(this, playerStatService, damageCalculator, combatDebugService),
                this
        );
        manaService.start();
        actionBarService.start();

        getLogger().info("UntitledDungeons has enabled.");
    }

    @Override
    public void onDisable() {
        if (actionBarService != null) {
            actionBarService.stop();
        }
        if (manaService != null) {
            manaService.stop();
        }
        getLogger().info("UntitledDungeons has disabled.");
    }
}

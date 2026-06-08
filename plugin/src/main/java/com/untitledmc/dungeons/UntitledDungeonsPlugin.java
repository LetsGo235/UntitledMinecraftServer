package com.untitledmc.dungeons;

import com.untitledmc.dungeons.item.ItemBuilder;
import com.untitledmc.dungeons.item.ItemLoreRenderer;
import com.untitledmc.dungeons.item.command.DItemCommand;
import com.untitledmc.dungeons.item.registry.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class UntitledDungeonsPlugin extends JavaPlugin {
    private ItemRegistry itemRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        itemRegistry = new ItemRegistry(this);
        itemRegistry.load();

        ItemLoreRenderer loreRenderer = new ItemLoreRenderer();
        ItemBuilder itemBuilder = new ItemBuilder(new NamespacedKey(this, "item_id"), loreRenderer);
        DItemCommand dItemCommand = new DItemCommand(itemRegistry, itemBuilder);
        PluginCommand command = getCommand("ditem");
        if (command != null) {
            command.setExecutor(dItemCommand);
            command.setTabCompleter(dItemCommand);
        }

        getLogger().info("UntitledDungeons has enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("UntitledDungeons has disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("uds")) {
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("UntitledDungeons is running.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("UntitledDungeons config reloaded.");
            return true;
        }

        sender.sendMessage("Usage: /uds status or /uds reload");
        return true;
    }
}

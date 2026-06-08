package com.untitledmc.dungeons;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class UntitledDungeonsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
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

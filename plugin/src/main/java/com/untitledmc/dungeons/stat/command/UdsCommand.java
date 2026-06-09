package com.untitledmc.dungeons.stat.command;

import com.untitledmc.dungeons.combat.CombatDebugService;
import com.untitledmc.dungeons.item.registry.ItemRegistry;
import com.untitledmc.dungeons.mob.MobRegistry;
import com.untitledmc.dungeons.stat.PlayerStatService;
import com.untitledmc.dungeons.stat.PlayerStats;
import com.untitledmc.dungeons.stat.StatType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UdsCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ItemRegistry itemRegistry;
    private final MobRegistry mobRegistry;
    private final PlayerStatService playerStatService;
    private final CombatDebugService combatDebugService;

    public UdsCommand(
            JavaPlugin plugin,
            ItemRegistry itemRegistry,
            MobRegistry mobRegistry,
            PlayerStatService playerStatService,
            CombatDebugService combatDebugService
    ) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.mobRegistry = mobRegistry;
        this.playerStatService = playerStatService;
        this.combatDebugService = combatDebugService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("UntitledDungeons is running.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            itemRegistry.load();
            mobRegistry.load();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerStatService.recalculate(player);
            }
            sender.sendMessage("UntitledDungeons config, custom items, and custom mobs reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use /uds stats.");
                return true;
            }

            sendStats(sender, playerStatService.getStats(player));
            return true;
        }

        if (args[0].equalsIgnoreCase("combatdebug")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use /uds combatdebug.");
                return true;
            }

            boolean enabled = combatDebugService.toggle(player);
            sender.sendMessage("Combat debug " + (enabled ? "enabled" : "disabled") + ".");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("recalculatestats")) {
            if (!sender.hasPermission("untitleddungeons.admin")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player '" + args[1] + "' is not online.");
                return true;
            }

            playerStatService.recalculate(target);
            sender.sendMessage("Recalculated stats for " + target.getName() + ".");
            return true;
        }

        sender.sendMessage("Usage: /uds status, /uds reload, /uds stats, /uds combatdebug, or /uds recalculatestats <player>");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return matching(args[0], List.of("status", "reload", "stats", "combatdebug", "recalculatestats"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("recalculatestats")) {
            return matching(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }

        return List.of();
    }

    private void sendStats(CommandSender sender, PlayerStats stats) {
        sender.sendMessage("Calculated stats:");
        for (StatType type : StatType.values()) {
            sender.sendMessage(" - " + formatLabel(type) + ": " + formatStat(stats.get(type)));
        }
    }

    private String formatLabel(StatType type) {
        String[] parts = type.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private String formatStat(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private List<String> matching(String input, Iterable<String> candidates) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerInput)) {
                matches.add(candidate);
            }
        }
        return matches;
    }
}

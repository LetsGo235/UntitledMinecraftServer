package com.untitledmc.dungeons.item.command;

import com.untitledmc.dungeons.item.ItemBuilder;
import com.untitledmc.dungeons.item.registry.ItemRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DItemCommand implements CommandExecutor, TabCompleter {
    private final ItemRegistry itemRegistry;
    private final ItemBuilder itemBuilder;

    public DItemCommand(ItemRegistry itemRegistry, ItemBuilder itemBuilder) {
        this.itemRegistry = itemRegistry;
        this.itemBuilder = itemBuilder;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            itemRegistry.load();
            sender.sendMessage("Custom items reloaded.");
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player '" + args[1] + "' is not online.");
                return true;
            }

            return itemRegistry.get(args[2])
                    .map(item -> {
                        ItemStack stack = itemBuilder.build(item);
                        target.getInventory().addItem(stack).values()
                                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
                        sender.sendMessage("Gave " + item.id() + " to " + target.getName() + ".");
                        return true;
                    })
                    .orElseGet(() -> {
                        sender.sendMessage("Unknown custom item '" + args[2] + "'.");
                        return true;
                    });
        }

        sender.sendMessage("Usage: /ditem give <player> <item_id> or /ditem reload");
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
            return matching(args[0], List.of("give", "reload"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return matching(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return matching(args[2], itemRegistry.ids());
        }

        return List.of();
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

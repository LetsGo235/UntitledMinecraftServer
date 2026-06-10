package com.untitledmc.dungeons.item.command;

import com.untitledmc.dungeons.item.CustomItem;
import com.untitledmc.dungeons.item.ItemBuilder;
import com.untitledmc.dungeons.item.ItemType;
import com.untitledmc.dungeons.item.registry.ItemRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
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
    private static final int MAX_GIVE_AMOUNT = 2304;

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

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            sendItemList(sender);
            return true;
        }

        if ((args.length == 3 || args.length == 4) && args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player '" + args[1] + "' is not online.");
                return true;
            }

            int amount = 1;
            if (args.length == 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage("Amount must be a whole number.");
                    return true;
                }
                if (amount < 1) {
                    sender.sendMessage("Amount must be at least 1.");
                    return true;
                }
                if (amount > MAX_GIVE_AMOUNT) {
                    sender.sendMessage("Amount cannot exceed " + MAX_GIVE_AMOUNT + ".");
                    return true;
                }
            }

            int requestedAmount = amount;

            return itemRegistry.get(args[2])
                    .map(item -> {
                        giveItems(target, item, requestedAmount);
                        sender.sendMessage("Gave " + requestedAmount + "x " + item.id() + " to " + target.getName() + ".");
                        return true;
                    })
                    .orElseGet(() -> {
                        sender.sendMessage("Unknown custom item '" + args[2] + "'.");
                        return true;
                    });
        }

        sender.sendMessage("Usage: /ditem give <player> <item_id> [amount], /ditem list, or /ditem reload");
        return true;
    }

    private void giveItems(Player target, CustomItem item, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = item.stackable() ? Math.min(remaining, item.maxStackSize()) : 1;
            ItemStack stack = itemBuilder.build(item, stackAmount);
            target.getInventory().addItem(stack).values()
                    .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            remaining -= stackAmount;
        }
    }

    private void sendItemList(CommandSender sender) {
        Map<ItemType, List<String>> grouped = itemRegistry.items().stream()
                .collect(Collectors.groupingBy(
                        item -> item.itemType(),
                        () -> new java.util.EnumMap<>(ItemType.class),
                        Collectors.mapping(item -> item.id(), Collectors.toList())
                ));

        sender.sendMessage("Registered custom items:");
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(Enum::ordinal)))
                .forEach(entry -> sender.sendMessage(
                        "- " + formatType(entry.getKey()) + ": " + String.join(", ", entry.getValue())
                ));
    }

    private String formatType(ItemType itemType) {
        String name = itemType.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return matching(args[0], List.of("give", "list", "reload"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return matching(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return matching(args[2], itemRegistry.ids());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return matching(args[3], List.of("1", "5", "16", "32", "64"));
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

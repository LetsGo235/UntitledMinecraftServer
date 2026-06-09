package com.untitledmc.dungeons.mob.command;

import com.untitledmc.dungeons.mob.CustomMobService;
import com.untitledmc.dungeons.mob.MobRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DMobCommand implements CommandExecutor, TabCompleter {
    private final MobRegistry mobRegistry;
    private final CustomMobService customMobService;

    public DMobCommand(MobRegistry mobRegistry, CustomMobService customMobService) {
        this.mobRegistry = mobRegistry;
        this.customMobService = customMobService;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            mobRegistry.load();
            sender.sendMessage("Custom mobs reloaded.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use /dmob spawn.");
                return true;
            }

            return mobRegistry.getMob(args[1])
                    .map(mob -> {
                        customMobService.spawnCustomMob(player.getLocation(), mob.id());
                        sender.sendMessage("Spawned custom mob " + mob.id() + ".");
                        return true;
                    })
                    .orElseGet(() -> {
                        sender.sendMessage("Unknown custom mob '" + args[1] + "'.");
                        return true;
                    });
        }

        sender.sendMessage("Usage: /dmob spawn <mob_id> or /dmob reload");
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
            return matching(args[0], List.of("spawn", "reload"));
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return matching(args[1], mobRegistry.getMobIds());
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

package com.untitledmc.dungeons.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;

public final class ItemLoreRenderer {
    public List<String> render(CustomItem item) {
        List<String> lore = new ArrayList<>();

        lore.add(color("&8Type: &f" + formatLabel(item.itemType().name())));
        lore.add(color("&8Rarity: " + rarityColor(item.rarity()) + formatLabel(item.rarity())));

        if (showsStats(item) && !item.stats().isEmpty()) {
            lore.add("");
            lore.add(color("&7Stats"));
            for (Map.Entry<String, Integer> stat : item.stats().entrySet()) {
                lore.add(color("&7" + formatLabel(stat.getKey()) + ": &a+" + stat.getValue()));
            }
        }

        CustomItem.Ability ability = item.ability();
        if (item.itemType() == ItemType.WEAPON && ability != null) {
            lore.add("");
            lore.add(color("&6Ability: &e" + formatLabel(ability.id()) + " &8(" + ability.id() + ")"));
            lore.add(color("&7Trigger: &f" + formatLabel(ability.trigger())));
            lore.add(color("&7Mana Cost: &b" + ability.manaCost()));
            lore.add(color("&7Cooldown: &f" + ability.cooldownSeconds() + "s"));
        }

        if (!item.flavorLore().isEmpty()) {
            lore.add("");
            for (String line : item.flavorLore()) {
                lore.add(color(line));
            }
        }

        if (item.itemType() == ItemType.TALISMAN) {
            lore.add("");
            lore.add(color("&8Passive effects not active yet."));
        }

        lore.add("");
        lore.add(color("&l" + rarityColor(item.rarity()) + formatLabel(item.rarity())));
        return lore;
    }

    private boolean showsStats(CustomItem item) {
        return item.itemType() == ItemType.WEAPON
                || item.itemType() == ItemType.ARMOR
                || item.itemType() == ItemType.TALISMAN;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String formatLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown";
        }

        String[] parts = raw.toLowerCase(Locale.ROOT).split("[_\\s-]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private String rarityColor(String rarity) {
        if (rarity == null) {
            return "&f";
        }

        return switch (rarity.toUpperCase(Locale.ROOT)) {
            case "COMMON" -> "&f";
            case "UNCOMMON" -> "&a";
            case "RARE" -> "&9";
            case "EPIC" -> "&5";
            case "LEGENDARY" -> "&6";
            case "MYTHIC" -> "&d";
            default -> "&f";
        };
    }
}

package com.untitledmc.dungeons.item;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

public final class CustomItem {
    private final String id;
    private final Material material;
    private final String displayName;
    private final String rarity;
    private final Map<String, Integer> stats;
    private final Ability ability;
    private final List<String> flavorLore;

    public CustomItem(
            String id,
            Material material,
            String displayName,
            String rarity,
            Map<String, Integer> stats,
            Ability ability,
            List<String> flavorLore
    ) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.rarity = rarity;
        this.stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));
        this.ability = ability;
        this.flavorLore = List.copyOf(flavorLore);
    }

    public String id() {
        return id;
    }

    public Material material() {
        return material;
    }

    public String displayName() {
        return displayName;
    }

    public String rarity() {
        return rarity;
    }

    public Map<String, Integer> stats() {
        return stats;
    }

    public Ability ability() {
        return ability;
    }

    public List<String> flavorLore() {
        return flavorLore;
    }

    public record Ability(String id, String trigger, int manaCost, int cooldownSeconds) {
    }
}

package com.untitledmc.dungeons.stat;

import java.util.EnumMap;
import java.util.Map;

public final class PlayerStats {
    private final EnumMap<StatType, Double> values = new EnumMap<>(StatType.class);

    public PlayerStats() {
        reset();
    }

    public void add(StatType type, double amount) {
        values.put(type, get(type) + amount);
    }

    public void set(StatType type, double value) {
        values.put(type, value);
    }

    public double get(StatType type) {
        return values.getOrDefault(type, 0.0D);
    }

    public void reset() {
        values.clear();
        for (StatType type : StatType.values()) {
            values.put(type, 0.0D);
        }
    }

    public PlayerStats copy() {
        PlayerStats copy = new PlayerStats();
        copy.values.clear();
        copy.values.putAll(values);
        return copy;
    }

    public Map<StatType, Double> asMap() {
        return Map.copyOf(values);
    }
}

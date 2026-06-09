package com.untitledmc.dungeons.stat;

import java.util.Locale;
import java.util.Optional;

public enum StatType {
    HEALTH,
    DEFENSE,
    STRENGTH,
    CRIT_CHANCE,
    CRIT_DAMAGE,
    INTELLIGENCE,
    MANA,
    SPEED,
    ABILITY_DAMAGE,
    DAMAGE;

    public static Optional<StatType> fromConfigKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        String normalized = key.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return Optional.of(StatType.valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}

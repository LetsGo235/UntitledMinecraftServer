package com.untitledmc.dungeons.mob;

import org.bukkit.entity.EntityType;

public record CustomMob(
        String id,
        String displayName,
        EntityType entityType,
        int level,
        double maxHealth,
        double damage,
        double defense,
        double movementSpeed,
        double followRange,
        int experienceReward,
        int coinReward
) {
}

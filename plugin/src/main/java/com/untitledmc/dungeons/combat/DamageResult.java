package com.untitledmc.dungeons.combat;

public record DamageResult(
        double finalDamage,
        boolean wasCrit,
        double baseDamage,
        double rawDamage,
        double strength,
        double critChance,
        double critDamage
) {
}

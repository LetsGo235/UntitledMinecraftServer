package com.untitledmc.dungeons.combat;

import com.untitledmc.dungeons.stat.PlayerStats;
import com.untitledmc.dungeons.stat.StatType;
import java.util.concurrent.ThreadLocalRandom;

public final class DamageCalculator {
    public DamageResult calculate(PlayerStats stats) {
        double baseDamage = stats.get(StatType.DAMAGE);
        double strength = stats.get(StatType.STRENGTH);
        double critChance = Math.max(0.0D, stats.get(StatType.CRIT_CHANCE));
        double critDamage = stats.get(StatType.CRIT_DAMAGE);
        if (critDamage <= 0.0D) {
            critDamage = 50.0D;
        }

        double strengthBonus = 1.0D + (strength / 100.0D);
        double rawDamage = baseDamage * strengthBonus;
        boolean wasCrit = ThreadLocalRandom.current().nextDouble(100.0D) < critChance;
        double finalDamage = rawDamage;
        if (wasCrit) {
            finalDamage *= 1.0D + (critDamage / 100.0D);
        }

        return new DamageResult(
                Math.max(1.0D, finalDamage),
                wasCrit,
                baseDamage,
                rawDamage,
                strength,
                critChance,
                critDamage
        );
    }
}

package com.untitledmc.dungeons.profile;

import java.util.UUID;

public final class PlayerProfile {
    private final UUID playerId;
    private int coins;
    private int dungeonLevel;
    private int dungeonXp;

    public PlayerProfile(UUID playerId, int coins, int dungeonLevel, int dungeonXp) {
        this.playerId = playerId;
        this.coins = Math.max(0, coins);
        this.dungeonLevel = Math.max(1, dungeonLevel);
        this.dungeonXp = Math.max(0, dungeonXp);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getCoins() {
        return coins;
    }

    public void addCoins(int amount) {
        coins = Math.max(0, coins + amount);
    }

    public int getDungeonLevel() {
        return dungeonLevel;
    }

    public void increaseDungeonLevel() {
        dungeonLevel++;
    }

    public int getDungeonXp() {
        return dungeonXp;
    }

    public void setDungeonXp(int dungeonXp) {
        this.dungeonXp = Math.max(0, dungeonXp);
    }
}

package com.untitledmc.dungeons.reward;

import com.untitledmc.dungeons.combat.CombatDebugService;
import com.untitledmc.dungeons.mob.CustomMob;
import com.untitledmc.dungeons.profile.PlayerProfile;
import com.untitledmc.dungeons.profile.PlayerProfileService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class RewardService {
    private final PlayerProfileService profileService;
    private final CombatDebugService combatDebugService;

    public RewardService(PlayerProfileService profileService, CombatDebugService combatDebugService) {
        this.profileService = profileService;
        this.combatDebugService = combatDebugService;
    }

    public void grantCoins(Player player, int amount) {
        if (amount <= 0) {
            return;
        }

        PlayerProfile profile = profileService.getProfile(player);
        profile.addCoins(amount);
        profileService.saveProfile(profile);
    }

    public void grantDungeonXp(Player player, int amount) {
        if (amount <= 0) {
            return;
        }

        PlayerProfile profile = profileService.getProfile(player);
        profile.setDungeonXp(profile.getDungeonXp() + amount);
        while (profile.getDungeonXp() >= getRequiredXp(profile.getDungeonLevel())) {
            int requiredXp = getRequiredXp(profile.getDungeonLevel());
            profile.setDungeonXp(profile.getDungeonXp() - requiredXp);
            profile.increaseDungeonLevel();
            sendLevelUpFeedback(player, profile.getDungeonLevel());
        }
        profileService.saveProfile(profile);
    }

    public void handleMobKillReward(Player player, CustomMob mob) {
        grantCoins(player, mob.coinReward());
        grantDungeonXp(player, mob.experienceReward());

        player.sendActionBar(Component.text("+" + mob.coinReward() + " coins", NamedTextColor.GOLD)
                .append(Component.text("   +" + mob.experienceReward() + " Dungeon XP", NamedTextColor.AQUA)));

        if (combatDebugService.isEnabled(player)) {
            PlayerProfile profile = profileService.getProfile(player);
            player.sendMessage(Component.text("Reward debug:", NamedTextColor.GOLD));
            player.sendMessage(Component.text(" - Mob: " + mob.id(), NamedTextColor.GRAY));
            player.sendMessage(Component.text(" - Coins granted: " + mob.coinReward(), NamedTextColor.GRAY));
            player.sendMessage(Component.text(" - Dungeon XP granted: " + mob.experienceReward(), NamedTextColor.GRAY));
            player.sendMessage(Component.text(
                    " - Profile: " + profile.getCoins() + " coins, level " + profile.getDungeonLevel()
                            + ", " + profile.getDungeonXp() + "/" + getRequiredXp(profile.getDungeonLevel()) + " XP",
                    NamedTextColor.GRAY
            ));
        }
    }

    public int getRequiredXp(int currentLevel) {
        return 100 * Math.max(1, currentLevel);
    }

    private void sendLevelUpFeedback(Player player, int newLevel) {
        player.sendMessage(Component.text(
                "Dungeon Level Up! You are now Level " + newLevel,
                NamedTextColor.LIGHT_PURPLE
        ));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.1F);
        player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.getLocation().add(0.0D, 1.0D, 0.0D),
                24,
                0.5D,
                0.75D,
                0.5D,
                0.05D
        );
    }
}

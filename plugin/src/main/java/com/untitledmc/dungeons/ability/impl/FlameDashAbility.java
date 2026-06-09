package com.untitledmc.dungeons.ability.impl;

import com.untitledmc.dungeons.ability.Ability;
import com.untitledmc.dungeons.ability.AbilityContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public final class FlameDashAbility implements Ability {
    private static final String ID = "flame_dash";
    private static final double DISTANCE = 8.0D;
    private static final double STEP = 0.5D;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Flame Dash";
    }

    @Override
    public String getDescription() {
        return "Dash forward in a burst of flame.";
    }

    @Override
    public void execute(AbilityContext context) {
        Player player = context.player();
        Location from = player.getLocation();
        Location target = findDashTarget(from);
        World world = player.getWorld();

        spawnTrail(world, from, target);
        player.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
        world.spawnParticle(Particle.FLAME, target.clone().add(0.0D, 1.0D, 0.0D), 24, 0.35D, 0.45D, 0.35D, 0.02D);
        world.spawnParticle(Particle.CRIT, target.clone().add(0.0D, 1.0D, 0.0D), 18, 0.35D, 0.45D, 0.35D, 0.15D);
        world.playSound(target, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8F, 1.3F);
        player.sendActionBar(Component.text("Flame Dash!", NamedTextColor.GOLD));
    }

    private Location findDashTarget(Location start) {
        Vector direction = start.getDirection();
        direction.setY(0.0D);
        if (direction.lengthSquared() <= 0.0D) {
            return start;
        }
        direction.normalize();

        Location lastSafe = start.clone();
        for (double traveled = STEP; traveled <= DISTANCE; traveled += STEP) {
            Location candidate = start.clone().add(direction.clone().multiply(traveled));
            candidate.setYaw(start.getYaw());
            candidate.setPitch(start.getPitch());

            if (!isSafe(candidate)) {
                break;
            }
            lastSafe = center(candidate);
        }

        return lastSafe;
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0.0D, 1.0D, 0.0D).getBlock();
        Block ground = location.clone().subtract(0.0D, 1.0D, 0.0D).getBlock();

        return feet.isPassable()
                && head.isPassable()
                && ground.getType().isSolid()
                && !isHazard(feet.getType())
                && !isHazard(head.getType())
                && !isHazard(ground.getType());
    }

    private boolean isHazard(Material material) {
        return material == Material.LAVA
                || material == Material.FIRE
                || material == Material.SOUL_FIRE
                || material == Material.CAMPFIRE
                || material == Material.SOUL_CAMPFIRE
                || material == Material.CACTUS
                || material == Material.MAGMA_BLOCK
                || material == Material.POWDER_SNOW
                || material == Material.SWEET_BERRY_BUSH;
    }

    private Location center(Location location) {
        Location centered = location.clone();
        centered.setX(location.getBlockX() + 0.5D);
        centered.setZ(location.getBlockZ() + 0.5D);
        return centered;
    }

    private void spawnTrail(World world, Location from, Location to) {
        Vector path = to.toVector().subtract(from.toVector());
        double length = path.length();
        if (length <= 0.0D) {
            return;
        }

        Vector step = path.normalize().multiply(0.75D);
        Location point = from.clone().add(0.0D, 1.0D, 0.0D);
        for (double traveled = 0.0D; traveled < length; traveled += 0.75D) {
            world.spawnParticle(Particle.FLAME, point, 4, 0.18D, 0.18D, 0.18D, 0.01D);
            world.spawnParticle(Particle.CRIT, point, 2, 0.12D, 0.12D, 0.12D, 0.05D);
            point.add(step);
        }
    }
}

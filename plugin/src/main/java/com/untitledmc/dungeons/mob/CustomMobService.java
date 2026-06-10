package com.untitledmc.dungeons.mob;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomMobService {
    private static final double VANILLA_HEALTH_BUFFER = 20.0D;

    private final JavaPlugin plugin;
    private final MobRegistry mobRegistry;
    private final NamespacedKey mobIdKey;
    private final NamespacedKey currentHealthKey;
    private final NamespacedKey maxHealthKey;
    private final NamespacedKey deadKey;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public CustomMobService(JavaPlugin plugin, MobRegistry mobRegistry) {
        this.plugin = plugin;
        this.mobRegistry = mobRegistry;
        this.mobIdKey = new NamespacedKey(plugin, "custom_mob_id");
        this.currentHealthKey = new NamespacedKey(plugin, "custom_mob_current_health");
        this.maxHealthKey = new NamespacedKey(plugin, "custom_mob_max_health");
        this.deadKey = new NamespacedKey(plugin, "custom_mob_dead");
    }

    public LivingEntity spawnCustomMob(Location location, String mobId) {
        CustomMob mob = mobRegistry.getMob(mobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown custom mob '" + mobId + "'."));
        Entity entity = location.getWorld().spawnEntity(location, mob.entityType());
        if (!(entity instanceof LivingEntity livingEntity)) {
            entity.remove();
            throw new IllegalStateException("Configured entity is not living: " + mob.entityType());
        }

        PersistentDataContainer data = livingEntity.getPersistentDataContainer();
        data.set(mobIdKey, PersistentDataType.STRING, mob.id());
        data.set(currentHealthKey, PersistentDataType.DOUBLE, mob.maxHealth());
        data.set(maxHealthKey, PersistentDataType.DOUBLE, mob.maxHealth());
        data.remove(deadKey);

        applyAttribute(livingEntity, Attribute.MAX_HEALTH, VANILLA_HEALTH_BUFFER);
        applyAttribute(livingEntity, Attribute.MOVEMENT_SPEED, mob.movementSpeed());
        applyAttribute(livingEntity, Attribute.FOLLOW_RANGE, mob.followRange());
        applyAttribute(livingEntity, Attribute.ATTACK_DAMAGE, mob.damage());
        AttributeInstance maxHealthAttribute = livingEntity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            livingEntity.setHealth(Math.min(VANILLA_HEALTH_BUFFER, maxHealthAttribute.getValue()));
        }
        livingEntity.setRemoveWhenFarAway(false);
        livingEntity.setCustomNameVisible(true);
        updateMobDisplayName(livingEntity);
        return livingEntity;
    }

    public boolean isCustomMob(LivingEntity entity) {
        if (shouldIgnore(entity)) {
            return false;
        }
        String mobId = getCustomMobId(entity);
        return mobId != null && mobRegistry.getMob(mobId).isPresent();
    }

    public String getCustomMobId(LivingEntity entity) {
        return entity.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
    }

    public double getCurrentHealth(LivingEntity entity) {
        Double currentHealth = entity.getPersistentDataContainer().get(currentHealthKey, PersistentDataType.DOUBLE);
        if (currentHealth != null) {
            return currentHealth;
        }
        return getMaxHealth(entity);
    }

    public void setCurrentHealth(LivingEntity entity, double health) {
        double clampedHealth = Math.max(0.0D, Math.min(health, getMaxHealth(entity)));
        entity.getPersistentDataContainer().set(currentHealthKey, PersistentDataType.DOUBLE, clampedHealth);
    }

    public void damageCustomMob(LivingEntity entity, double amount) {
        if (!isCustomMob(entity) || isDeadOrDying(entity)) {
            return;
        }

        CustomMob mob = mobRegistry.getMob(getCustomMobId(entity)).orElse(null);
        double reducedDamage = amount;
        if (mob != null && mob.defense() > 0.0D) {
            reducedDamage = amount * (100.0D / (100.0D + mob.defense()));
        }
        setCurrentHealth(entity, getCurrentHealth(entity) - Math.max(1.0D, reducedDamage));
    }

    public boolean markDeadIfNeeded(LivingEntity entity) {
        if (!isCustomMob(entity) || isDeadOrDying(entity)) {
            return false;
        }

        entity.getPersistentDataContainer().set(deadKey, PersistentDataType.BYTE, (byte) 1);
        return true;
    }

    public boolean isDeadOrDying(LivingEntity entity) {
        return entity.isDead()
                || !entity.isValid()
                || entity.getPersistentDataContainer().has(deadKey, PersistentDataType.BYTE);
    }

    public void playDeathFeedback(LivingEntity entity, CustomMob customMob) {
        if (entity == null || customMob == null || entity.isDead() || !entity.isValid()) {
            return;
        }

        if (!plugin.getConfig().getBoolean("combat.mob_death_feedback", true)) {
            return;
        }

        Location location = entity.getLocation().add(0.0D, Math.max(0.45D, entity.getHeight() * 0.45D), 0.0D);
        World world = entity.getWorld();
        Sound deathSound = getDeathSound(customMob.entityType());
        if (deathSound != null) {
            world.playSound(location, deathSound, 1.0F, 0.85F);
        }

        boolean cinderMob = customMob.id().toLowerCase(Locale.ROOT).contains("cinder");
        world.spawnParticle(Particle.SMOKE, location, 24, 0.35D, 0.35D, 0.35D, 0.04D);
        world.spawnParticle(Particle.SOUL, location, 12, 0.3D, 0.45D, 0.3D, 0.03D);
        world.spawnParticle(Particle.DAMAGE_INDICATOR, location, 8, 0.25D, 0.25D, 0.25D, 0.02D);
        world.spawnParticle(Particle.CRIT, location, 10, 0.25D, 0.4D, 0.25D, 0.08D);
        if (cinderMob) {
            world.spawnParticle(Particle.FLAME, location, 18, 0.3D, 0.45D, 0.3D, 0.04D);
        }
        world.spawnParticle(Particle.SMOKE, location.clone().add(0.0D, 0.6D, 0.0D), 10, 0.18D, 0.35D, 0.18D, 0.08D);
    }

    public void finishDeath(LivingEntity entity) {
        if (entity == null || entity.isDead() || !entity.isValid()) {
            return;
        }

        try {
            entity.setHealth(0.0D);
            scheduleRemove(entity);
        } catch (IllegalArgumentException exception) {
            scheduleRemove(entity);
        }
    }

    public void updateMobDisplayName(LivingEntity entity) {
        if (!isCustomMob(entity)) {
            return;
        }

        CustomMob mob = mobRegistry.getMob(getCustomMobId(entity)).orElse(null);
        if (mob == null) {
            return;
        }

        Component displayName = Component.text("Lv. " + mob.level() + " ")
                .append(legacySerializer.deserialize(mob.displayName()))
                .append(Component.text(" " + formatHealth(getCurrentHealth(entity)) + "/" + formatHealth(getMaxHealth(entity)) + "\u2764"));
        entity.customName(displayName);
        entity.setCustomNameVisible(true);
    }

    public double getMaxHealth(LivingEntity entity) {
        Double maxHealth = entity.getPersistentDataContainer().get(maxHealthKey, PersistentDataType.DOUBLE);
        return maxHealth == null ? 0.0D : maxHealth;
    }

    public String getPlainDisplayName(String mobId) {
        return mobRegistry.getMob(mobId)
                .map(CustomMob::displayName)
                .map(name -> legacySerializer.deserialize(name))
                .map(component -> LegacyComponentSerializer.legacySection().serialize(component))
                .map(this::stripSectionFormatting)
                .orElse(mobId);
    }

    private void scheduleRemove(LivingEntity entity) {
        long delayTicks = Math.max(0L, plugin.getConfig().getLong("combat.mob_death_remove_delay_ticks", 10L));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (entity.isValid()) {
                entity.remove();
            }
        }, delayTicks);
    }

    private Sound getDeathSound(EntityType entityType) {
        return switch (entityType) {
            case BLAZE -> Sound.ENTITY_BLAZE_DEATH;
            case CAVE_SPIDER, SPIDER -> Sound.ENTITY_SPIDER_DEATH;
            case CREEPER -> Sound.ENTITY_CREEPER_DEATH;
            case DROWNED -> Sound.ENTITY_DROWNED_DEATH;
            case ENDERMAN -> Sound.ENTITY_ENDERMAN_DEATH;
            case EVOKER -> Sound.ENTITY_EVOKER_DEATH;
            case HUSK -> Sound.ENTITY_HUSK_DEATH;
            case ILLUSIONER -> Sound.ENTITY_ILLUSIONER_DEATH;
            case PHANTOM -> Sound.ENTITY_PHANTOM_DEATH;
            case PILLAGER -> Sound.ENTITY_PILLAGER_DEATH;
            case SKELETON -> Sound.ENTITY_SKELETON_DEATH;
            case SLIME -> Sound.ENTITY_SLIME_DEATH;
            case STRAY -> Sound.ENTITY_STRAY_DEATH;
            case VEX -> Sound.ENTITY_VEX_DEATH;
            case VINDICATOR -> Sound.ENTITY_VINDICATOR_DEATH;
            case WITCH -> Sound.ENTITY_WITCH_DEATH;
            case WITHER_SKELETON -> Sound.ENTITY_WITHER_SKELETON_DEATH;
            case ZOMBIE -> Sound.ENTITY_ZOMBIE_DEATH;
            default -> Sound.ENTITY_GENERIC_DEATH;
        };
    }

    private boolean shouldIgnore(LivingEntity entity) {
        return entity instanceof Player
                || entity instanceof ArmorStand
                || entity.isInvulnerable()
                || entity.hasMetadata("NPC")
                || entity.getScoreboardTags().contains("NPC");
    }

    private void applyAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private String formatHealth(double value) {
        double rounded = Math.max(0.0D, Math.ceil(value));
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    private String stripSectionFormatting(String value) {
        return value.replaceAll("(?i)\u00a7[0-9A-FK-ORX]", "");
    }
}

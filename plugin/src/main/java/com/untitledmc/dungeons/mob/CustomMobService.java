package com.untitledmc.dungeons.mob;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomMobService {
    private static final double VANILLA_HEALTH_BUFFER = 20.0D;

    private final MobRegistry mobRegistry;
    private final NamespacedKey mobIdKey;
    private final NamespacedKey currentHealthKey;
    private final NamespacedKey maxHealthKey;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

    public CustomMobService(JavaPlugin plugin, MobRegistry mobRegistry) {
        this.mobRegistry = mobRegistry;
        this.mobIdKey = new NamespacedKey(plugin, "custom_mob_id");
        this.currentHealthKey = new NamespacedKey(plugin, "custom_mob_current_health");
        this.maxHealthKey = new NamespacedKey(plugin, "custom_mob_max_health");
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
        if (!isCustomMob(entity)) {
            return;
        }

        CustomMob mob = mobRegistry.getMob(getCustomMobId(entity)).orElse(null);
        double reducedDamage = amount;
        if (mob != null && mob.defense() > 0.0D) {
            reducedDamage = amount * (100.0D / (100.0D + mob.defense()));
        }
        setCurrentHealth(entity, getCurrentHealth(entity) - Math.max(1.0D, reducedDamage));
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

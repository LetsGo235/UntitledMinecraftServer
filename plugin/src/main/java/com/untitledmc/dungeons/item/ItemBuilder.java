package com.untitledmc.dungeons.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class ItemBuilder {
    private final NamespacedKey itemIdKey;
    private final ItemLoreRenderer loreRenderer;

    public ItemBuilder(NamespacedKey itemIdKey, ItemLoreRenderer loreRenderer) {
        this.itemIdKey = itemIdKey;
        this.loreRenderer = loreRenderer;
    }

    public ItemStack build(CustomItem item) {
        ItemStack stack = new ItemStack(item.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        meta.setDisplayName(loreRenderer.color(item.displayName()));
        meta.setLore(loreRenderer.render(item));
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, item.id());
        stack.setItemMeta(meta);
        return stack;
    }
}

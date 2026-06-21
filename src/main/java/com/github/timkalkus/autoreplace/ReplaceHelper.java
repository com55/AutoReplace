package com.github.timkalkus.autoreplace;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public class ReplaceHelper {

    private final Player player;
    private final Inventory inventory;
    private final ItemStack item;
    private final Integer itemSlot;

    private ShulkerBoxHelper shulkerBox = null;
    private int shulkerBoxLocation = -1;
    private int replacementItemSlot = -1;

    // Inventory slots in search-priority order: hotbar (0-8), off-hand (40), then the main inventory (9-35).
    // Armor slots (36-39) are intentionally not searched as a replacement source.
    private static final int OFF_HAND_SLOT = 40;
    private static final int[] SEARCH_ORDER = buildSearchOrder();

    private static int[] buildSearchOrder() {
        int[] order = new int[37];
        int idx = 0;
        for (int slot = 0; slot <= 8; slot++) {   // hotbar
            order[idx++] = slot;
        }
        order[idx++] = OFF_HAND_SLOT;             // off-hand
        for (int slot = 9; slot <= 35; slot++) {  // main inventory
            order[idx++] = slot;
        }
        return order;
    }

    public ReplaceHelper(Player player, ItemStack item, int itemSlot) {
        this.player = player;
        this.inventory = player.getInventory();
        this.item = item;
        this.itemSlot = itemSlot;
    }


    public void replace() {
        findReplacement();
        if (replacementItemSlot == -1) {
            return;
        }
        if (shulkerBox != null) {
            player.getInventory().setItem(itemSlot, shulkerBox.getInventory().getItem(replacementItemSlot));
            shulkerBox.getInventory().clear(replacementItemSlot);
            player.getInventory().setItem(shulkerBoxLocation, shulkerBox.getUpdatedShulkerItem());
        } else {
            player.getInventory().setItem(itemSlot, player.getInventory().getItem(replacementItemSlot));
            player.getInventory().clear(replacementItemSlot);
        }
        player.updateInventory();
    }

    public void swapTool() {
        findReplacement();
        if (replacementItemSlot == -1) {
            saveTool();
            return;
        }
        ItemStack replacementItem;
        if (shulkerBox != null) {
            replacementItem = shulkerBox.getInventory().getItem(replacementItemSlot);
            shulkerBox.getInventory().setItem(replacementItemSlot, item);
            player.getInventory().setItem(itemSlot, replacementItem);
            player.getInventory().setItem(shulkerBoxLocation, shulkerBox.getUpdatedShulkerItem());
        } else {
            replacementItem = player.getInventory().getItem(replacementItemSlot);
            player.getInventory().setItem(replacementItemSlot, item);
            player.getInventory().setItem(itemSlot, replacementItem); // here nullpoint exception occures for armour slots
        }
        player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
        player.updateInventory();
    }

    public void saveTool() {
        int saveSlot = inventory.firstEmpty();
        if (saveSlot != -1) {
            inventory.setItem(saveSlot, item);
            inventory.setItem(itemSlot, null);
        } else { // only works for in-hand items
            ItemStack offHand = player.getInventory().getItemInOffHand();
            player.getInventory().setItemInOffHand(inventory.getItem(itemSlot));
            player.getInventory().setItemInMainHand(offHand);
        }
        player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
        player.updateInventory();
    }

    private void findReplacement() {
        // Searches the inventory for the first possible replacement following SEARCH_ORDER
        // (hotbar, off-hand, main inventory): first the loose items, then inside shulker boxes.
        ItemStack[] invContent = inventory.getContents();
        // 1. loose items in the inventory
        for (int slot : SEARCH_ORDER) {
            if (slot >= invContent.length || (itemSlot != null && slot == itemSlot)) {
                continue;
            }
            if (isPossibleReplacement(invContent[slot])) {
                this.replacementItemSlot = slot;
                return;
            }
        }
        // 2. items inside shulker boxes, in the same location priority
        for (int slot : SEARCH_ORDER) {
            if (slot >= invContent.length || !isShulker(invContent[slot])) {
                continue;
            }
            ShulkerBoxHelper sbh = new ShulkerBoxHelper(invContent[slot]);
            ItemStack[] sbContent = sbh.getInventory().getContents();
            for (int j = 0; j < sbContent.length; j++) {
                if (isPossibleReplacement(sbContent[j])) {
                    this.shulkerBox = sbh;
                    this.shulkerBoxLocation = slot;
                    this.replacementItemSlot = j;
                    return;
                }
            }
        }
    }

    private boolean isShulker(ItemStack item) {
        if (item == null)
            return false;
        try {
            return item.getType().name().contains("SHULKER_BOX");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isPossibleReplacement(ItemStack item) {
        if (item == null)
            return false;
        try {
            if (!item.getType().equals(this.item.getType()))
                return false;
            // don't replace non-enchanted tools with enchanted ones
            if (item.getEnchantments().isEmpty() != this.item.getEnchantments().isEmpty())
                return false;
            if (item.hasItemMeta() && item.getItemMeta() instanceof Damageable && item.getType().getMaxDurability() != 0)
                return ((Damageable) item.getItemMeta()).getDamage() * 1.0 / item.getType().getMaxDurability() < .5;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

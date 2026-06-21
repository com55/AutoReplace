package com.github.timkalkus.autoreplace;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoReplaceTest {
    private ServerMock server;

    /**
     * Initialized Bukkit-Server-Mock and saves instance of AutoReplaceMain locally
     */
    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(AutoReplaceMain.class);
        server.getLogger().setLevel(Level.WARNING);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * Checks that non-enchanted tools will be replaced when broken
     */
    @Test
    public void replaceNonEnchantedTool() {
        // create player and add one damaged and one undamaged item
        Player player = server.addPlayer();
        ItemStack item0 = getDamageable(Material.IRON_PICKAXE, 1, false);
        ItemStack item1 = getDamageable(Material.IRON_PICKAXE, -1, false);
        player.getInventory().setItem(0, item0);
        item0 = player.getInventory().getItem(0);
        player.getInventory().setItem(1, item1);
        // execute event
        executeItemDamageEvent(player, item0);
        // check if item was replaced
        assertTrue(getRestDurability(player.getInventory().getItem(0)) > 5, "replacement item was not moved");
        assertEquals(1, Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).
                filter(items -> !AutoReplaceListener.isNullOrAir(items)).count(), "amount of items in inventory does not match expectations");
    }

    /**
     * Checks that non-enchanted tools won't be moved with low durability left
     */
    @Test
    public void keepNonEnchantedTool() {
        // create player and add one damaged and one undamaged item
        Player player = server.addPlayer();
        ItemStack item0 = getDamageable(Material.IRON_PICKAXE, 5, false);
        ItemStack item1 = getDamageable(Material.IRON_PICKAXE, -1, false);
        player.getInventory().setItem(0, item0);
        item0 = player.getInventory().getItem(0);
        player.getInventory().setItem(1, item1);
        // execute event
        executeItemDamageEvent(player, item0);
        // check if item was replaced
        assertTrue(getRestDurability(player.getInventory().getItem(0)) < 5, "damaged item was incorrectly moved");
        assertTrue(getRestDurability(player.getInventory().getItem(1)) > 5, "replacement item was incorrectly moved");
        assertEquals(2, Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).
                filter(items -> !AutoReplaceListener.isNullOrAir(items)).count(), "amount of items in inventory does not match expectations");
    }

    /**
     * Checks that enchanted tools will be replaced with low durability left
     */
    @Test
    public void swapEnchantedTool() {
        // create player and add one damaged and one undamaged item
        Player player = server.addPlayer();
        ItemStack item0 = getDamageable(Material.IRON_PICKAXE, 5, true);
        ItemStack item1 = getDamageable(Material.IRON_PICKAXE, -1, true);
        player.getInventory().setItem(0, item0);
        item0 = player.getInventory().getItem(0);
        player.getInventory().setItem(1, item1);
        // execute event
        executeItemDamageEvent(player, item0);
        // check if item was replaced
        assertNotSame(item0, player.getInventory().getItem(0), "damaged item was not moved in inventory");
        assertTrue(getRestDurability(player.getInventory().getItem(0)) > 5, "damaged item was not moved");
        assertTrue(getRestDurability(player.getInventory().getItem(1)) < 5, "replacement item was not moved");
        assertEquals(2, Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).
                filter(items -> !AutoReplaceListener.isNullOrAir(items)).count(), "amount of items in inventory does not match expectations");
    }

    /**
     * Checks that non-enchanted tools will be moved with low durability left, if no replacement is available
     */
    @Test
    public void saveEnchantedTool() {
        // create player and add single item
        Player player = server.addPlayer();
        ItemStack item = getDamageable(Material.IRON_PICKAXE, 5, true);
        // places clone in inventory
        player.getInventory().setItem(0, item);
        // redirects reference to cloned item in inventory
        item = player.getInventory().getItem(0);
        assertNotNull(item);
        // execute event
        executeItemDamageEvent(player, item);
        // check if item was moved
        assertTrue(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(0)), "Item was not moved in inventory");
        assertTrue(player.getInventory().contains(item), "Item was not found in inventory");
    }

    /**
     * Checks that non-enchanted elytras will be replaced with 1 durability left
     */
    @Test
    public void replaceUnenchantedElytra() {
        // create player and add one damaged and one undamaged item
        Player player = server.addPlayer();
        ItemStack item0 = getDamageable(Material.ELYTRA, 3, false);
        ItemStack item1 = getDamageable(Material.ELYTRA, -1, false);
        player.getInventory().setItem(38, item0);
        item0 = player.getInventory().getItem(38);
        player.getInventory().setItem(0, item1);
        // execute event
        executeItemDamageEvent(player, item0);
        // check if elytra is kept with 2 durability left
        assertEquals(2, getRestDurability(player.getInventory().getItem(38)), "elytra should not have been moved, but was");
        assertEquals(2, Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).
                filter(items -> !AutoReplaceListener.isNullOrAir(items)).count(), "amount of items in inventory does not match expectations");
        executeItemDamageEvent(player, player.getInventory().getItem(38));
        // check if elytra gets replaced with 1 durability left
        assertTrue(getRestDurability(player.getInventory().getItem(38)) > 5, "elytra should have been moved, but wasn't");
        assertEquals(2, Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).
                filter(items -> !AutoReplaceListener.isNullOrAir(items)).count(), "amount of items in inventory does not match expectations");
    }

    /**
     * Checks that enchanted elytras will be replaced with 1 durability left
     */
    @Test
    public void replaceEnchantedElytra() {
        // create player and add one damaged and one undamaged item
        Player player = server.addPlayer();
        ItemStack item0 = getDamageable(Material.ELYTRA, 3, true);
        ItemStack item1 = getDamageable(Material.ELYTRA, -1, true);
        player.getInventory().setItem(38, item0);
        item0 = player.getInventory().getItem(38);
        player.getInventory().setItem(0, item1);
        // execute event
        executeItemDamageEvent(player, item0);
        // check if elytra is kept with 2 durability left
        assertEquals(2, getRestDurability(player.getInventory().getItem(38)), "elytra should not have been moved, but was");
        assertEquals(2, Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).
                filter(items -> !AutoReplaceListener.isNullOrAir(items)).count(), "amount of items in inventory does not match expectations");
        executeItemDamageEvent(player, player.getInventory().getItem(38));
        // check if elytra gets replaced with 1 durability left
        assertTrue(getRestDurability(player.getInventory().getItem(38)) > 5, "elytra should have been moved, but wasn't");
        assertEquals(2, Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).
                filter(items -> !AutoReplaceListener.isNullOrAir(items)).count(), "amount of items in inventory does not match expectations");
    }

    @Test
    public void refillStack() {
        // create player and add single item
        Player player = server.addPlayer();
        ItemStack item = new ItemStack(Material.STONE, 1);
        // places clone in inventory
        player.getInventory().setItem(0, item);
        // redirects reference to cloned item in inventory
        item = player.getInventory().getItem(0);
        player.getInventory().setItem(1, new ItemStack(Material.STONE, 64));
        int itemCount = amountInInventory(player.getInventory(), Material.STONE);
        assertNotNull(item);
        // execute event
        executeItemUsedEvent(player, item);
        // check if item was moved
        assertFalse(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(0)), "Stack is empty, but should be refilled");
        assertEquals(itemCount - 1, amountInInventory(player.getInventory(), Material.STONE));
    }

    /**
     * Checks that a depleted stack is refilled from the plain inventory first and only falls back to a
     * shulker box when the inventory has no replacement (regression test for shulker-before-inventory order)
     */
    @Test
    public void refillFromInventoryBeforeShulker() {
        Player player = server.addPlayer();
        // held single stack that will be depleted
        ItemStack held = new ItemStack(Material.STONE, 1);
        player.getInventory().setItem(0, held);
        held = player.getInventory().getItem(0);
        assertNotNull(held);
        // a full replacement stack directly in the inventory
        player.getInventory().setItem(1, new ItemStack(Material.STONE, 64));
        // another full replacement stack inside a shulker box
        player.getInventory().setItem(2, shulkerContaining(new ItemStack(Material.STONE, 64)));
        // execute event
        executeItemUsedEvent(player, held);
        // refill must come from the plain inventory stack, leaving the shulker box untouched
        assertFalse(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(0)), "held stack should be refilled");
        assertTrue(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(1)), "inventory replacement should have been consumed first");
        assertEquals(64, amountInShulker(player.getInventory().getItem(2), Material.STONE), "shulker box should be untouched when inventory has a replacement");
    }

    /**
     * Checks that a depleted stack is refilled from a stack held in the off-hand
     */
    @Test
    public void refillFromOffHand() {
        Player player = server.addPlayer();
        // held single stack that will be depleted
        ItemStack held = new ItemStack(Material.STONE, 1);
        player.getInventory().setItem(0, held);
        held = player.getInventory().getItem(0);
        assertNotNull(held);
        // only replacement is a stack held in the off-hand
        player.getInventory().setItemInOffHand(new ItemStack(Material.STONE, 64));
        // execute event
        executeItemUsedEvent(player, held);
        // check that the held stack was refilled
        assertFalse(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(0)), "held stack should be refilled from the off-hand stack");
    }

    /**
     * Checks that a depleted stack is refilled from a shulker box held in the off-hand
     */
    @Test
    public void refillFromShulkerInOffHand() {
        Player player = server.addPlayer();
        // held single stack that will be depleted
        ItemStack held = new ItemStack(Material.STONE, 1);
        player.getInventory().setItem(0, held);
        held = player.getInventory().getItem(0);
        assertNotNull(held);
        // only replacement is inside a shulker box held in the off-hand
        player.getInventory().setItemInOffHand(shulkerContaining(new ItemStack(Material.STONE, 64)));
        // execute event
        executeItemUsedEvent(player, held);
        // check that the held stack was refilled
        assertFalse(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(0)), "held stack should be refilled from the shulker box in the off-hand");
    }

    /**
     * Checks that the off-hand is preferred over the main inventory for loose replacements
     */
    @Test
    public void refillPrefersOffHandOverInventory() {
        Player player = server.addPlayer();
        // held single stack that will be depleted
        ItemStack held = new ItemStack(Material.STONE, 1);
        player.getInventory().setItem(0, held);
        held = player.getInventory().getItem(0);
        assertNotNull(held);
        // replacement available both in the main inventory and in the off-hand
        player.getInventory().setItem(9, new ItemStack(Material.STONE, 64));
        player.getInventory().setItemInOffHand(new ItemStack(Material.STONE, 64));
        // execute event
        executeItemUsedEvent(player, held);
        // off-hand has priority, so it should be consumed and the main inventory left untouched
        assertFalse(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(0)), "held stack should be refilled");
        assertTrue(AutoReplaceListener.isNullOrAir(player.getInventory().getItemInOffHand()), "off-hand should be consumed before the main inventory");
        assertNotNull(player.getInventory().getItem(9));
        assertEquals(64, player.getInventory().getItem(9).getAmount(), "main inventory replacement should be untouched");
    }

    /**
     * Checks that a shulker box in the off-hand is preferred over a shulker box in the main inventory
     */
    @Test
    public void refillPrefersOffHandShulkerOverInventoryShulker() {
        Player player = server.addPlayer();
        // held single stack that will be depleted
        ItemStack held = new ItemStack(Material.STONE, 1);
        player.getInventory().setItem(0, held);
        held = player.getInventory().getItem(0);
        assertNotNull(held);
        // a matching shulker box both in the main inventory and in the off-hand
        player.getInventory().setItem(9, shulkerContaining(new ItemStack(Material.STONE, 64)));
        player.getInventory().setItemInOffHand(shulkerContaining(new ItemStack(Material.STONE, 64)));
        // execute event
        executeItemUsedEvent(player, held);
        // off-hand shulker has priority, so it should be drained and the main inventory shulker left untouched
        assertFalse(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(0)), "held stack should be refilled");
        assertEquals(0, amountInShulker(player.getInventory().getItemInOffHand(), Material.STONE), "off-hand shulker should be drained before the main inventory shulker");
        assertEquals(64, amountInShulker(player.getInventory().getItem(9), Material.STONE), "main inventory shulker should be untouched");
    }

    /**
     * Checks that eating the last food item refills the hand from the inventory
     */
    @Test
    public void refillFoodAfterEating() {
        Player player = server.addPlayer();
        // last food item held in the main hand
        ItemStack held = new ItemStack(Material.BREAD, 1);
        player.getInventory().setItem(0, held);
        player.getInventory().setHeldItemSlot(0);
        held = player.getInventory().getItem(0);
        assertNotNull(held);
        // a replacement stack in the inventory
        player.getInventory().setItem(9, new ItemStack(Material.BREAD, 64));
        // execute event
        executeItemConsumeEvent(player, held, EquipmentSlot.HAND);
        // the emptied hand should be refilled from the inventory stack
        assertFalse(AutoReplaceListener.isNullOrAir(player.getInventory().getItem(0)), "food should be refilled after eating the last one");
    }

    /**
     * Checks that eating the last food item from the off-hand refills it from the inventory
     */
    @Test
    public void refillFoodAfterEatingOffHand() {
        Player player = server.addPlayer();
        // last food item held in the off-hand
        ItemStack held = new ItemStack(Material.BREAD, 1);
        player.getInventory().setItemInOffHand(held);
        held = player.getInventory().getItemInOffHand();
        assertNotNull(held);
        // a replacement stack in the inventory
        player.getInventory().setItem(9, new ItemStack(Material.BREAD, 64));
        // execute event
        executeItemConsumeEvent(player, held, EquipmentSlot.OFF_HAND);
        // the emptied off-hand should be refilled from the inventory stack
        assertFalse(AutoReplaceListener.isNullOrAir(player.getInventory().getItemInOffHand()), "off-hand food should be refilled after eating the last one");
    }

    @Test
    public void stressTest() {
        int numberOfPlayers = 20;
        server.setPlayers(numberOfPlayers);
        for (int i = 0; i < numberOfPlayers; i++) {
            executeItemDamageEvent(server.getPlayer(i), setInventoryForDamageEvent(server.getPlayer(i), i), false);
        }
        server.getScheduler().performOneTick();
        for (int i = 0; i < numberOfPlayers; i++) {
            testInventoryAfterDamageEvent(server.getPlayer(i), i);
        }

    }

    public ItemStack setInventoryForDamageEvent(Player player, int i) {
        Material type;
        switch (i % 5) {
            case 0: // unenchanted, should break and be replaced
                type = Material.NETHERITE_PICKAXE;
                player.getInventory().setItem(0, getDamageable(type, 1, false));
                player.getInventory().setItem(ThreadLocalRandom.current().nextInt(1, 20), getDamageable(type, -1, false));
                return player.getInventory().getItem(0);
            case 1: // unenchanted, should stay
                type = Material.NETHERITE_PICKAXE;
                player.getInventory().setItem(0, getDamageable(type, 2, false));
                player.getInventory().setItem(ThreadLocalRandom.current().nextInt(1, 20), getDamageable(type, -1, false));
                return player.getInventory().getItem(0);
            case 2: // enchanted, should be swapped
                type = Material.NETHERITE_PICKAXE;
                player.getInventory().setItem(0, getDamageable(type, 3, true));
                player.getInventory().setItem(ThreadLocalRandom.current().nextInt(1, 20), getDamageable(type, -1, true));
                return player.getInventory().getItem(0);
            case 3: // enchanted, should be saved
                type = Material.NETHERITE_PICKAXE;
                player.getInventory().setItem(0, getDamageable(type, 3, true));
                return player.getInventory().getItem(0);
            case 4: // enchanted, should be swapped
                type = Material.ELYTRA;
                player.getInventory().setItem(38, getDamageable(type, 2, true));
                player.getInventory().setItem(ThreadLocalRandom.current().nextInt(0, 20), getDamageable(type, -1, true));
                return player.getInventory().getItem(38);
            default: // enchanted, should not be swapped
                type = Material.ELYTRA;
                player.getInventory().setItem(38, getDamageable(type, 3, true));
                player.getInventory().setItem(ThreadLocalRandom.current().nextInt(0, 20), getDamageable(type, -1, true));
                return player.getInventory().getItem(38);
        }
    }

    public void testInventoryAfterDamageEvent(Player player, int i) {
        Material type;
        ItemStack item;
        switch (i % 5) {
            case 0: // unenchanted, should break and be replaced
                type = Material.NETHERITE_PICKAXE;
                item = player.getInventory().getItem(0);
                assertNotNull(item);
                assertEquals(type, item.getType());
                assertTrue(item.getEnchantments().isEmpty());
                assertEquals(1, amountInInventory(player.getInventory(), type));
                assertTrue(getRestDurability(item) > 5);
                break;
            case 1: // unenchanted, should stay
                type = Material.NETHERITE_PICKAXE;
                item = player.getInventory().getItem(0);
                assertNotNull(item);
                assertEquals(type, item.getType());
                assertTrue(item.getEnchantments().isEmpty());
                assertEquals(2, amountInInventory(player.getInventory(), type));
                assertTrue(getRestDurability(item) < 5);
                break;
            case 2: // enchanted, should be swapped
                type = Material.NETHERITE_PICKAXE;
                item = player.getInventory().getItem(0);
                assertNotNull(item);
                assertEquals(type, item.getType());
                assertFalse(item.getEnchantments().isEmpty());
                assertEquals(2, amountInInventory(player.getInventory(), type));
                assertTrue(getRestDurability(item) > 5);
                break;
            case 3: // enchanted, should be saved
                type = Material.NETHERITE_PICKAXE;
                item = player.getInventory().getItem(0);
                assertTrue(AutoReplaceListener.isNullOrAir(item));
                assertEquals(1, amountInInventory(player.getInventory(), type));
                break;
            case 4: // enchanted, should be swapped
                type = Material.ELYTRA;
                item = player.getInventory().getItem(38);
                assertNotNull(item);
                assertEquals(type, item.getType());
                assertFalse(item.getEnchantments().isEmpty());
                assertEquals(2, amountInInventory(player.getInventory(), type));
                assertTrue(getRestDurability(item) > 5);
                break;
            default: // enchanted, should not be swapped
                type = Material.ELYTRA;
                item = player.getInventory().getItem(38);
                assertNotNull(item);
                assertEquals(type, item.getType());
                assertFalse(item.getEnchantments().isEmpty());
                assertEquals(2, amountInInventory(player.getInventory(), type));
                assertTrue(getRestDurability(item) < 5);
                break;
        }
    }

    /**
     * Imitates vanilla ItemDamageEvent behaviour by calling the event and increasing the damage if the event is not canceled
     *
     * @param player initiator of the event
     * @param item   item of the event, has to be damagable
     */
    private void executeItemDamageEvent(Player player, ItemStack item, boolean doTick) {
        assertNotNull(player);
        assertNotNull(item);
        assertTrue(item.hasItemMeta(), "Item missing ItemMeta");
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertTrue(meta instanceof Damageable);
        PlayerItemDamageEvent event = new PlayerItemDamageEvent(player, item, 1);
        server.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            Damageable damageable = (Damageable) meta;
            damageable.setDamage(damageable.getDamage() + 1);
            if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                item.setType(Material.AIR);
            }
        }
        item.setItemMeta(meta);
        if (doTick) {
            server.getScheduler().performOneTick();
        }
    }

    private void executeItemDamageEvent(Player player, ItemStack item) {
        executeItemDamageEvent(player, item, true);
    }

    /**
     * Imitates vanilla PlayerInteractEvent behaviour by calling the event and reducing the amount by 1 if the event is not canceled
     *
     * @param player initiator of the event
     * @param item   item of the event
     */
    private void executeItemUsedEvent(Player player, ItemStack item, boolean doTick) {
        assertNotNull(player);
        assertNotNull(item);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, null, BlockFace.EAST, null);
        server.getPluginManager().callEvent(event);
        if (!event.useItemInHand().equals(Event.Result.DENY) || !event.useInteractedBlock().equals(Event.Result.DENY)) {
            item.setAmount(item.getAmount() - 1);
            item.setType(Material.AIR);
        }
        if (doTick) {
            server.getScheduler().performOneTick();
        }
    }

    private void executeItemUsedEvent(Player player, ItemStack item) {
        executeItemUsedEvent(player, item, true);
    }

    /**
     * Imitates vanilla PlayerItemConsumeEvent behaviour by calling the event and reducing the amount by 1
     * (turning a depleted stack into air) if the event is not canceled
     *
     * @param player initiator of the event
     * @param item   item being consumed
     * @param hand   hand the item is consumed from
     */
    private void executeItemConsumeEvent(Player player, ItemStack item, EquipmentSlot hand) {
        assertNotNull(player);
        assertNotNull(item);
        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(player, item, hand);
        server.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            item.setAmount(item.getAmount() - 1);
            item.setType(Material.AIR);
        }
        server.getScheduler().performOneTick();
    }

    /**
     * Creates a ItemStack with the chosen material, remaining durability and optional enchantment
     *
     * @param material            chosen material, has to be damageable
     * @param remainingDurability how much durability should be left, -1 for no damage
     * @param isEnchanted         whether the tool should have an enchantment
     * @return modified ItemStack
     */
    @SuppressWarnings("SameParameterValue")
    private ItemStack getDamageable(Material material, int remainingDurability, boolean isEnchanted) {
        ItemStack item = new ItemStack(material);
        assertTrue(item.hasItemMeta(), "Item missing ItemMeta");
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta, "ItemMeta is null");
        if (isEnchanted) {
            meta.addEnchant(Enchantment.MENDING, 0, true);
        }
        assertTrue(meta instanceof Damageable);
        if (remainingDurability > 0) {
            ((Damageable) meta).setDamage(material.getMaxDurability() - remainingDurability);
        }
        item.setItemMeta(meta);
        return item;
    }

    private int getRestDurability(ItemStack item) {
        assertNotNull(item);
        assertTrue(item.hasItemMeta(), "Item missing ItemMeta");
        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta, "ItemMeta is null");
        assertTrue(meta instanceof Damageable);
        return item.getType().getMaxDurability() - ((Damageable) meta).getDamage();
    }

    /**
     * calculates the amount of items with the specified material
     */
    @SuppressWarnings("SameParameterValue")
    private int amountInInventory(Inventory inventory, Material material) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType().equals(material)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * builds a shulker box item that contains the given content in its first slot
     */
    private ItemStack shulkerContaining(ItemStack content) {
        ItemStack shulker = new ItemStack(Material.WHITE_SHULKER_BOX);
        BlockStateMeta bsm = (BlockStateMeta) shulker.getItemMeta();
        assertNotNull(bsm);
        ShulkerBox box = (ShulkerBox) bsm.getBlockState();
        box.getInventory().setItem(0, content);
        bsm.setBlockState(box);
        shulker.setItemMeta(bsm);
        return shulker;
    }

    /**
     * calculates the amount of items with the specified material inside a shulker box item
     */
    private int amountInShulker(ItemStack shulker, Material material) {
        assertNotNull(shulker);
        BlockStateMeta bsm = (BlockStateMeta) shulker.getItemMeta();
        assertNotNull(bsm);
        ShulkerBox box = (ShulkerBox) bsm.getBlockState();
        return amountInInventory(box.getInventory(), material);
    }

    /**
     * Print the input inventory e.g. as quick manual check
     */
    @SuppressWarnings("unused")
    private void printInventory(Inventory inventory) {
        List<String> invString = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            invString.add(itemStackToString(item));
        }
        System.out.println(invString);
    }

    /**
     * generate String with essential informations for quick manual checks
     *
     * @param item input item
     * @return essential information as String
     */
    private String itemStackToString(ItemStack item) {
        if (item == null) {
            return "null";
        }
        String returnString = item.getAmount() + "x" + item.getType().name();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            assertNotNull(meta);
            if (meta.hasEnchants()) {
                returnString += "*";
            }
            if (meta instanceof Damageable) {
                returnString += " " + (item.getType().getMaxDurability() - ((Damageable) meta).getDamage()) +
                        "/" + item.getType().getMaxDurability();
            }
        }
        returnString += " " + Integer.toHexString(item.hashCode());
        int r = item.hashCode() % 256;
        int g = item.hashCode() / 16 % 256;
        int b = item.hashCode() / 256 % 256;
        return Ansi.colorize(returnString, Attribute.TEXT_COLOR(Math.abs(r), Math.abs(g), Math.abs(b)));
        //return returnString;
    }
}

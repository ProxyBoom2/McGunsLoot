package com.loottables.mcgunsloot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiManager implements Listener {

    private final McGunsLoot plugin;
    private final LootManager lootManager;
    private final Map<Player, LootTable> editing = new HashMap<>();
    private final Map<Player, Integer> pageMap = new HashMap<>();

    public GuiManager(McGunsLoot plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    public void openEditor(Player player, LootTable table) {
        int page = pageMap.getOrDefault(player, 0);
        editing.put(player, table);

        String title = "Loot Editor: " + table.getName();
        if (page > 0) title += " (Page " + (page + 1) + ")";
        
        Inventory gui = Bukkit.createInventory(null, 54, title);

        List<LootEntry> entries = table.getEntries();
        int startIndex = page * 45;
        int slot = 0;

        for (int i = startIndex; i < entries.size() && slot < 45; i++) {
            LootEntry e = entries.get(i);
            ItemStack item = e.getItemStack().clone();
            ItemMeta meta = item.getItemMeta();

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add("§6--- Editor Stats ---");
            lore.add("§7Min: §f" + e.getMin());
            lore.add("§7Max: §f" + e.getMax());
            lore.add("§7Weight: §f" + e.getWeight());
            lore.add("§7Req Level: §b" + e.getMinLevel());
            lore.add("");
            lore.add("§cClick to remove");

            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }

        // --- Navigation & Action Buttons ---
        
        // Add Button (Slot 49 - Bottom Middle)
        ItemStack add = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta am = add.getItemMeta();
        am.setDisplayName("§aAdd New Loot Entry");
        am.setLore(Arrays.asList("§7(Adds a default Diamond)", "§7Use /loots additem for custom items"));
        add.setItemMeta(am);
        gui.setItem(49, add);

        // Previous Page (Slot 45)
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName("§ePrevious Page");
            prev.setItemMeta(pm);
            gui.setItem(45, prev);
        }

        // Next Page (Slot 53)
        if (entries.size() > startIndex + 45) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("§eNext Page");
            next.setItemMeta(nm);
            gui.setItem(53, next);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // 1. Handle Loot Chest Interaction
        if (title.equals("§6§lMCGUNS")) {
            if (event.getRawSlot() == CustomInventoryFactory.CLOCK_SLOT) {
                event.setCancelled(true);
                return;
            }

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || 
                event.getAction() == InventoryAction.HOTBAR_SWAP || 
                event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                
                ItemStack current = event.getCurrentItem();
                if (current != null && current.getType() == Material.CLOCK) {
                    event.setCancelled(true);
                }
            }
            return; 
        }

        // 2. Handle Loot Editor Interaction
        if (title.startsWith("Loot Editor: ")) {
            event.setCancelled(true);

            LootTable table = editing.get(player);
            if (table == null) return;

            int slot = event.getRawSlot();
            int currentPage = pageMap.getOrDefault(player, 0);

            // Add Item Logic
            if (slot == 49) {
                table.addEntry(new LootEntry(new ItemStack(Material.DIAMOND), 0, 1, 1, 10));
                lootManager.saveToConfig();
                openEditor(player, table);
                return;
            }

            // Previous Page
            if (slot == 45 && currentPage > 0) {
                pageMap.put(player, currentPage - 1);
                openEditor(player, table);
                return;
            }

            // Next Page
            if (slot == 53 && table.getEntries().size() > (currentPage + 1) * 45) {
                pageMap.put(player, currentPage + 1);
                openEditor(player, table);
                return;
            }

            // Remove Item Logic
            if (slot >= 0 && slot < 45) {
                int entryIndex = (currentPage * 45) + slot;
                if (entryIndex < table.getEntries().size()) {
                    table.removeEntry(entryIndex);
                    lootManager.saveToConfig();
                    openEditor(player, table);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("Loot Editor: ")) {
            Player p = (Player) event.getPlayer();
            editing.remove(p);
            pageMap.remove(p); // Reset page on close
        }
    }
}
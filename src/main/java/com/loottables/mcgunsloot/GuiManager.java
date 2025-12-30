package com.loottables.mcgunsloot;

import java.util.ArrayList; // FIXED: Import for image_3c4cca.png
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiManager implements Listener {

    private final McGunsLoot plugin;
    private final LootManager lootManager;
    private final Map<Player, LootTable> editing = new HashMap<>();

    public GuiManager(McGunsLoot plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    public void openEditor(Player player, LootTable table) {
        editing.put(player, table);

        Inventory gui = Bukkit.createInventory(null, 54, "Loot Editor: " + table.getName());

        int slot = 0;
        for (LootEntry e : table.getEntries()) {
            if (slot >= 53) break; // Leave last slot for the "Add" button
            
            ItemStack item = e.getItemStack().clone(); 
            ItemMeta meta = item.getItemMeta();

            // FIXED: Using ArrayList from the import above
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

        // Add New Entry Button
        ItemStack add = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta am = add.getItemMeta();
        am.setDisplayName("§aAdd New Loot Entry");
        am.setLore(Arrays.asList("§7(Adds a default Diamond)", "§7Use /loots additem for custom items"));
        add.setItemMeta(am);
        gui.setItem(53, add);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith("Loot Editor: ")) return;

        event.setCancelled(true);

        LootTable table = editing.get(player);
        if (table == null) return;

        int slot = event.getRawSlot();

        // Add Item Logic
        if (slot == 53) {
            table.addEntry(new LootEntry(new ItemStack(Material.DIAMOND), 1, 1, 10, 0));
            lootManager.saveToConfig();
            openEditor(player, table);
            return;
        }

        // Remove Item Logic (FIXED: Calls table.removeEntry for image_3bf271.png)
        if (slot >= 0 && slot < table.getEntries().size()) {
            table.removeEntry(slot);
            lootManager.saveToConfig();
            openEditor(player, table);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("Loot Editor: ")) {
            editing.remove((Player) event.getPlayer());
        }
    }
}
package com.loottables.mcgunsloot;

import java.util.Arrays;
import java.util.HashMap;
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

        Inventory gui = Bukkit.createInventory(null, 54,
                "Loot Editor: " + table.getName());

        int slot = 0;
        for (LootEntry e : table.getEntries()) {
            ItemStack item = new ItemStack(e.getMaterial());
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName("§e" + e.getMaterial().name());
            meta.setLore(Arrays.asList(
                    "§7Min: §f" + e.getMin(),
                    "§7Max: §f" + e.getMax(),
                    "§7Weight: §f" + e.getWeight(),
                    "",
                    "§cClick to remove"
            ));

            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }

        ItemStack add = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta am = add.getItemMeta();
        am.setDisplayName("§aAdd New Loot Entry");
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

        if (slot == 53) {
            table.addEntry(new LootEntry(Material.DIAMOND, 1, 1, 10));
            lootManager.saveToConfig();
            openEditor(player, table);
            return;
        }

        if (slot < table.getEntries().size()) {
            table.removeEntry(slot);
            lootManager.saveToConfig();
            openEditor(player, table);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("Loot Editor: ")) {
            editing.remove((Player) event.getPlayer());
            lootManager.saveToConfig();
        }
    }
}

package com.loottables.mcgunsloot;

import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public class ChestListener implements Listener {

    private final McGunsLoot plugin;
    private final LootManager lootManager;

    public ChestListener(McGunsLoot plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;

        Location loc = chest.getLocation();
        if (!lootManager.isLinked(loc)) return;

        // Cancel vanilla opening to show our custom UI
        event.setCancelled(true);

        int cd = lootManager.getRemainingCooldown(player, loc);
        Inventory inv;

        if (cd > 0) {
            // Player is on cooldown: Retrieve the inventory they were just using
            inv = lootManager.getOrCreateActiveInventory(player, loc);
        } else {
            // Cooldown is 0: Generate fresh loot and save it as their new active inventory
            inv = CustomInventoryFactory.createLootInventory(lootManager, loc, player);
            lootManager.saveActiveInventory(player, loc, inv);
        }

        player.openInventory(inv);

        // Update clock every second
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (player.getOpenInventory().getTopInventory() != inv) {
                    cancel();
                    return;
                }

                int currentCd = lootManager.getRemainingCooldown(player, loc);
                CustomInventoryFactory.updateClock(inv, currentCd);

                if (currentCd <= 0) cancel();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
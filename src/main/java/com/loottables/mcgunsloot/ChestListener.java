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

        event.setCancelled(true);

        Inventory inv = CustomInventoryFactory.createLootInventory(lootManager, loc, player);
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

                int cd = lootManager.getRemainingCooldown(player, loc);
                CustomInventoryFactory.updateClock(inv, cd);

                if (cd <= 0) cancel();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}

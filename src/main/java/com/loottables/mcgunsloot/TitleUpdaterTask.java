package com.loottables.mcgunsloot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public class TitleUpdaterTask extends BukkitRunnable {

    private final McGunsLoot plugin;
    private final Player player;
    private final Location loc;
    private final LootManager lootManager;

    public TitleUpdaterTask(McGunsLoot plugin, Player player, Location loc, LootManager lootManager) {
        this.plugin = plugin;
        this.player = player;
        this.loc = loc;
        this.lootManager = lootManager;
    }

    @Override
    public void run() {

        if (!player.isOnline()) {
            cancel();
            return;
        }

        Inventory open = player.getOpenInventory().getTopInventory();
        if (open == null) {
            cancel();
            return;
        }

        int cd = lootManager.getRemainingCooldown(player, loc);

        String title = "§6§lMCGUNS §c(" + cd + "s)";

        // Create the new updated inventory
        Inventory updated = Bukkit.createInventory(null, open.getSize(), title);
        updated.setContents(open.getContents());

        player.openInventory(updated);
    }
}

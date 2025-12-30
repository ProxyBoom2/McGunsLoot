package com.loottables.mcgunsloot;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public class CooldownClockTask extends BukkitRunnable {

    private final LootManager lootManager;
    private final Player player;
    private final Location loc;

    public CooldownClockTask(LootManager lootManager, Player player, Location loc) {
        this.lootManager = lootManager;
        this.player = player;
        this.loc = loc;
    }

    @Override
    public void run() {

        if (!player.isOnline()) {
            cancel();
            return;
        }

        Inventory inv = lootManager.getCachedInventory(loc);
        if (inv == null) {
            cancel();
            return;
        }

        int cd = lootManager.getRemainingCooldown(player, loc);
        inv.setItem(CustomInventoryFactory.TIMER_SLOT,
                CustomInventoryFactory.createClock(cd));

        if (cd <= 0) {
            lootManager.clearCachedInventory(loc);
            cancel();
        }
    }
}

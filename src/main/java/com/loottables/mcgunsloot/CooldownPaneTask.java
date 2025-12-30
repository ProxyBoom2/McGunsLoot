package com.loottables.mcgunsloot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class CooldownPaneTask extends BukkitRunnable {

    private final LootManager lootManager;
    private final Player player;
    private final Inventory inventory;
    private final Location chestLocation;

    public CooldownPaneTask(
            LootManager lootManager,
            Player player,
            Inventory inventory,
            Location chestLocation
    ) {
        this.lootManager = lootManager;
        this.player = player;
        this.inventory = inventory;
        this.chestLocation = chestLocation;
    }

    @Override
    public void run() {

        if (!player.isOnline()) {
            cancel();
            return;
        }

        // Player closed inventory
        if (player.getOpenInventory().getTopInventory() != inventory) {
            cancel();
            return;
        }

        int seconds = lootManager.getRemainingCooldown(player, chestLocation);

        // Cooldown finished → remove pane and stop
        if (seconds <= 0) {
            inventory.clear(0);
            cancel();
            return;
        }

        // Orange glass pane with timer
        ItemStack pane = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName("§6§lMCGUNS §c(" + seconds + "s)");
        pane.setItemMeta(meta);

        inventory.setItem(0, pane); // TOP LEFT SLOT
    }
}

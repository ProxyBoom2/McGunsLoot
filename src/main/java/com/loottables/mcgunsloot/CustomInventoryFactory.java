package com.loottables.mcgunsloot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomInventoryFactory {

    public static final int CLOCK_SLOT = 26; // bottom-right

    public static Inventory createLootInventory(LootManager lootManager, Location loc, Player player) {

        LootTable table = lootManager.getLootTable(loc);
        int cd = lootManager.getRemainingCooldown(player, loc);

        String title = "§6§lMCGUNS";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Add loot ONLY if not on cooldown
        if (cd == 0 && table != null) {
            LootEntry entry = table.getWeightedRandom();
            if (entry != null) {
                int amount = entry.getMin()
                        + lootManager.getRandom().nextInt(entry.getMax() - entry.getMin() + 1);

                inv.addItem(new ItemStack(entry.getMaterial(), amount));
                lootManager.setCooldown(loc, player, table.getCooldownSeconds());
            }
        }

        // Always add clock
        inv.setItem(CLOCK_SLOT, createClock(cd));
        return inv;
    }

    public static void updateClock(Inventory inv, int seconds) {
        inv.setItem(CLOCK_SLOT, createClock(seconds));
    }

    private static ItemStack createClock(int seconds) {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        meta.setDisplayName("§6§lCooldown");
        meta.setLore(java.util.List.of(
                "§7Next loot in:",
                "§c" + seconds + " seconds"
        ));
        clock.setItemMeta(meta);
        return clock;
    }
}

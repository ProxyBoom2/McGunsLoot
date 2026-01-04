package com.loottables.mcgunsloot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.phobia.levels.api.PlayerLevelUpEvent;

public class CustomInventoryFactory {

    public static final int CLOCK_SLOT = 26;

    public static Inventory createLootInventory(LootManager lootManager, Location loc, Player player) {
        LootTable table = lootManager.getLootTable(loc);
        int cd = lootManager.getRemainingCooldown(player, loc);

        String title = "§6§lMCGUNS";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        if (cd <= 0 && table != null) {
            int playerLevel = PlayerLevelUpEvent.getLevel(player);

            // Filter for level-appropriate loot
            List<LootEntry> accessibleLoot = table.getEntries().stream()
                .filter(entry -> playerLevel >= entry.getMinLevel())
                .collect(Collectors.toCollection(ArrayList::new));

            if (!accessibleLoot.isEmpty()) {
                // Shuffle the loot list so the "first 4" successes are random items
                Collections.shuffle(accessibleLoot);

                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < 26; i++) slots.add(i);
                Collections.shuffle(slots);
                
                int itemsFound = 0;
                int maxItems = 1 + lootManager.getRandom().nextInt(4); // Target: 1 to 4 items

                for (LootEntry entry : accessibleLoot) {
                    // Stop once we hit our random cap (1-4)
                    if (itemsFound >= maxItems) break;

                    double roll = lootManager.getRandom().nextDouble() * 100;
                    if (roll < entry.getWeight()) {
                        spawnItemInInv(inv, entry, slots.get(itemsFound), lootManager);
                        itemsFound++;
                    }
                }

                // PITY SYSTEM: If 0 items passed their % roll, force 1 random item
                if (itemsFound == 0 && !accessibleLoot.isEmpty()) {
                    spawnItemInInv(inv, accessibleLoot.get(0), slots.get(0), lootManager);
                }
                
                lootManager.setCooldown(loc, player, table.getCooldownSeconds());
            } else {
                player.sendMessage("§cYou aren't a high enough level to find anything here!");
            }
        }

        inv.setItem(CLOCK_SLOT, createClock(cd));
        return inv;
    }

    private static void spawnItemInInv(Inventory inv, LootEntry entry, int slot, LootManager lootManager) {
        ItemStack lootItem = entry.getItemStack().clone();
        int min = entry.getMin();
        int max = entry.getMax();
        int amount = (max <= min) ? min : min + lootManager.getRandom().nextInt(max - min + 1);
        lootItem.setAmount(amount);
        inv.setItem(slot, lootItem);
    }

    public static void updateClock(Inventory inv, int seconds) {
        inv.setItem(CLOCK_SLOT, createClock(seconds));
    }

    public static ItemStack createClock(int seconds) {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lCooldown");
            meta.setLore(List.of("§7Next loot in:", "§c" + (Math.max(seconds, 0)) + " seconds"));
            clock.setItemMeta(meta);
        }
        return clock;
    }
}
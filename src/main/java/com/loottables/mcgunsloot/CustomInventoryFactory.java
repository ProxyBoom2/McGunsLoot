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
        // FIXED: Simpler retrieval to avoid null errors if a chest is unlinked mid-open
        LootTable table = lootManager.getLootTable(loc);
        int cd = lootManager.getRemainingCooldown(player, loc);

        String title = "§6§lMCGUNS";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Only populate if cooldown is 0
        if (cd <= 0 && table != null) {
            int playerLevel = PlayerLevelUpEvent.getLevel(player);

            List<LootEntry> accessibleLoot = table.getEntries().stream()
                .filter(entry -> playerLevel >= entry.getMinLevel())
                .collect(Collectors.toList());

            if (!accessibleLoot.isEmpty()) {
                // Determine how many slots to fill (2 to 4)
                int rolls = 2 + lootManager.getRandom().nextInt(3); 
                
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < 26; i++) slots.add(i);
                Collections.shuffle(slots);

                for (int i = 0; i < rolls && i < slots.size(); i++) {
                    LootEntry entry = getWeightedRandomFromList(accessibleLoot, lootManager.getRandom());
                    if (entry != null) {
                        ItemStack lootItem = entry.getItemStack().clone();
                        int amount = entry.getMin() + lootManager.getRandom().nextInt(entry.getMax() - entry.getMin() + 1);
                        lootItem.setAmount(amount);
                        
                        inv.setItem(slots.get(i), lootItem);
                    }
                }
                // IMPORTANT: The cooldown is set here. 
                // In your ChestListener, we ensure this inventory is saved so it doesn't disappear.
                lootManager.setCooldown(loc, player, table.getCooldownSeconds());
            } else {
                player.sendMessage("§cYou aren't a high enough level to find anything here!");
            }
        }

        inv.setItem(CLOCK_SLOT, createClock(cd));
        return inv;
    }

    private static LootEntry getWeightedRandomFromList(List<LootEntry> entries, java.util.Random random) {
        int totalWeight = entries.stream().mapToInt(LootEntry::getWeight).sum();
        if (totalWeight <= 0) return null;
        
        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (LootEntry entry : entries) {
            current += entry.getWeight();
            if (roll < current) return entry;
        }
        return null;
    }

    public static void updateClock(Inventory inv, int seconds) {
        inv.setItem(CLOCK_SLOT, createClock(seconds));
    }

    private static ItemStack createClock(int seconds) {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        meta.setDisplayName("§6§lCooldown");
        // Simplified Lore formatting
        meta.setLore(List.of("§7Next loot in:", "§c" + (seconds < 0 ? 0 : seconds) + " seconds"));
        clock.setItemMeta(meta);
        return clock;
    }
}
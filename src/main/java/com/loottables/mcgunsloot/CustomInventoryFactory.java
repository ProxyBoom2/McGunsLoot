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

        boolean isLucky = lootManager.isLucky(player, loc);

        String title = isLucky ? "§6§lMCGUNS §e§l(LUCKY)" : "§6§lMCGUNS";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        if (cd <= 0 && table != null) {
            int playerLevel = PlayerLevelUpEvent.getLevel(player);

            List<LootEntry> accessibleLoot = table.getEntries().stream()
                .filter(entry -> playerLevel >= entry.getMinLevel())
                .collect(Collectors.toCollection(ArrayList::new));

            if (!accessibleLoot.isEmpty()) {
                Collections.shuffle(accessibleLoot);

                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < 26; i++) slots.add(i);
                Collections.shuffle(slots);

                int maxItems = 1 + lootManager.getRandom().nextInt(4); // 1 to 4 items

                // Only fetch multiplier when the chest is actually lucky
                double multiplier = 1.0;
                if (isLucky) {
                    multiplier = Bukkit.getPluginManager().getPlugin("McGunsLoot")
                            .getConfig().getDouble("lucky-chest.multiplier", 2.0);
                }

                // Phase 1: roll every item exactly once to build a candidate pool.
                // rarity is "1 in X", so chance = 100.0 / rarity.
                // Lucky multiplier divides the rarity, making items more likely.
                List<LootEntry> winners = new ArrayList<>();
                for (LootEntry entry : accessibleLoot) {
                    double chance = 100.0 / entry.getRarity();
                    double finalChance = isLucky ? (chance * multiplier) : chance;
                    double roll = lootManager.getRandom().nextDouble() * 100;
                    if (roll < finalChance) {
                        winners.add(entry);
                    }
                }

                // Phase 2: place up to maxItems winners into random slots
                int itemsFound = 0;
                for (LootEntry entry : winners) {
                    if (itemsFound >= maxItems) break;
                    spawnItemInInv(inv, entry, slots.get(itemsFound), lootManager);
                    itemsFound++;
                }

                // Pity system: guarantee at least 1 item if nothing passed its roll
                if (itemsFound == 0) {
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
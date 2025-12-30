package com.loottables.mcgunsloot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

public class LootManager {

    private final McGunsLoot plugin;
    private final Map<String, LootTable> tables = new HashMap<>();
    
    // PATCH: ConcurrentHashMap prevents crashes if linking while the background loader runs
    private final Map<Location, String> linkedChests = new ConcurrentHashMap<>();
    private final Map<Location, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();
    
    // Tracks current active loot sessions
    private final Map<UUID, Map<Location, Inventory>> activeInventories = new HashMap<>();
    
    private final Random random = new Random();

    public LootManager(McGunsLoot plugin) {
        this.plugin = plugin;
    }

    public Random getRandom() { return random; }

    // ================= SESSION MANAGEMENT =================

    public void saveActiveInventory(Player player, Location loc, Inventory inv) {
        activeInventories
            .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
            .put(loc, inv);
    }

    public Inventory getOrCreateActiveInventory(Player player, Location loc) {
        Map<Location, Inventory> playerMap = activeInventories.get(player.getUniqueId());
        if (playerMap != null && playerMap.containsKey(loc)) {
            return playerMap.get(loc);
        }
        Inventory inv = CustomInventoryFactory.createLootInventory(this, loc, player);
        saveActiveInventory(player, loc, inv);
        return inv;
    }

    // ================= CHEST PARTICLE SUPPORT =================

    public Set<Location> getLinkedChestLocations() {
        return linkedChests.keySet();
    }

    // ================= TABLES =================

    public LootTable createTable(String name) {
        LootTable table = new LootTable(name);
        tables.put(name.toLowerCase(), table);
        saveToConfig();
        return table;
    }

    public LootTable getTable(String name) {
        return tables.get(name.toLowerCase());
    }

    public boolean tableExists(String name) {
        return tables.containsKey(name.toLowerCase());
    }

    public Set<String> getAllTableNames() {
        return tables.keySet();
    }

    // ================= CHESTS =================

    public void linkChest(Location loc, String tableName) {
        linkedChests.put(loc, tableName.toLowerCase());
        saveToConfig(); 
    }

    public void unlinkChest(Location loc) {
        linkedChests.remove(loc);
        cooldowns.remove(loc);
        saveToConfig();
    }

    public boolean isLinked(Location loc) {
        return linkedChests.containsKey(loc);
    }

    public LootTable getLootTable(Location loc) {
        String name = linkedChests.get(loc);
        return name == null ? null : tables.get(name.toLowerCase());
    }

    // ================= COOLDOWNS =================

    public int getRemainingCooldown(Player player, Location loc) {
        Map<UUID, Long> map = cooldowns.get(loc);
        if (map == null) return 0;

        Long expires = map.get(player.getUniqueId());
        if (expires == null) return 0;

        long diff = expires - System.currentTimeMillis();
        return diff > 0 ? (int) (diff / 1000) : 0;
    }

    public void setCooldown(Location loc, Player player, int seconds) {
        cooldowns
            .computeIfAbsent(loc, k -> new ConcurrentHashMap<>())
            .put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    // ================= CONFIG & RETRY LOGIC =================

    public void loadFromConfig() {
        tables.clear();
        linkedChests.clear();
        cooldowns.clear();
        activeInventories.clear();

        FileConfiguration cfg = plugin.getConfig();
        
        // 1. Load Tables
        ConfigurationSection tSec = cfg.getConfigurationSection("tables");
        if (tSec != null) {
            for (String name : tSec.getKeys(false)) {
                ConfigurationSection tableData = tSec.getConfigurationSection(name);
                if (tableData != null) {
                    LootTable table = LootTable.loadFromConfig(name, tableData);
                    tables.put(name.toLowerCase(), table);
                }
            }
        }

        // 2. Load Chests via Retry Loop
        attemptChestLoad();
    }

    /**
     * Attempts to load chests. If a world isn't loaded yet (like Greenfield),
     * it will wait 5 seconds and try again.
     */
    private void attemptChestLoad() {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection cSec = cfg.getConfigurationSection("chests");
        if (cSec == null) return;

        boolean missingWorld = false;
        int loadedInThisPass = 0;

        for (String key : cSec.getKeys(false)) {
            ConfigurationSection sec = cSec.getConfigurationSection(key);
            if (sec == null) continue;

            String worldName = sec.getString("world");
            World world = (worldName != null) ? Bukkit.getWorld(worldName) : null;
            
            // If the world is null, Greenfield likely isn't loaded yet.
            if (world == null) {
                missingWorld = true;
                continue;
            }

            // Absolute precision using getInt
            Location loc = new Location(
                    world,
                    sec.getInt("x"),
                    sec.getInt("y"),
                    sec.getInt("z")
            );

            // Avoid duplicate links if the retry runs multiple times
            if (linkedChests.containsKey(loc)) continue;

            String tableName = sec.getString("table");
            if (tableName != null && tables.containsKey(tableName.toLowerCase())) {
                linkedChests.put(loc, tableName.toLowerCase());
                loadedInThisPass++;
            }
        }

        if (missingWorld) {
            plugin.getLogger().warning("[McGunsLoot] Some worlds are not yet loaded. Retrying chest-links in 5 seconds...");
            new BukkitRunnable() {
                @Override
                public void run() {
                    attemptChestLoad();
                }
            }.runTaskLater(plugin, 100L); // 100 ticks = 5 seconds
        } else {
            plugin.getLogger().info("[McGunsLoot] Successfully loaded " + linkedChests.size() + " total linked chests.");
        }
    }

    public void saveToConfig() {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("tables", null);
        cfg.set("chests", null);

        // Save Tables
        for (Map.Entry<String, LootTable> entry : tables.entrySet()) {
            entry.getValue().saveToConfig(cfg.createSection("tables." + entry.getKey()));
        }

        // Save Chests
        int index = 0;
        for (Map.Entry<Location, String> entry : linkedChests.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null) continue;

            String path = "chests." + index;
            cfg.set(path + ".world", loc.getWorld().getName());
            
            // PATCH: Save as Integers to ensure perfect block alignment
            cfg.set(path + ".x", loc.getBlockX());
            cfg.set(path + ".y", loc.getBlockY());
            cfg.set(path + ".z", loc.getBlockZ());
            cfg.set(path + ".table", entry.getValue());
            index++;
        }

        plugin.saveConfig();
    }
}
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
    
    private final Map<Location, String> linkedChests = new ConcurrentHashMap<>();
    private final Map<Location, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();
    
    private final Map<UUID, Map<Location, Inventory>> activeInventories = new HashMap<>();
    
    private final Random random = new Random();

    public LootManager(McGunsLoot plugin) {
        this.plugin = plugin;
    }

    public Random getRandom() { return random; }

    // ================= SPECIAL REWARDS (XP & TOKENS) =================

    public void applySpecialRewards(Player player) {
        FileConfiguration config = plugin.getConfig();

        // 1. XP Rewards
        double xpChance = config.getDouble("rewards.xp.chance", 1.0);
        double xpRoll = random.nextDouble() * 100;
        
        if (xpRoll < xpChance) {
            int min = config.getInt("rewards.xp.min", 10);
            int max = config.getInt("rewards.xp.max", 50);
            int amount = random.nextInt((max - min) + 1) + min;
            
            String rawXpCmd = config.getString("rewards.xp.command", "givexp %player% %amount%");
            String finalXpCmd = rawXpCmd.replace("%player%", player.getName())
                                        .replace("%amount%", String.valueOf(amount));
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalXpCmd);
        }

        // 2. Token Rewards
        double tokenChance = config.getDouble("rewards.tokens.chance", 1.0);
        double tokenRoll = random.nextDouble() * 100;

        if (tokenRoll < tokenChance) {
            int min = config.getInt("rewards.tokens.min", 1);
            int max = config.getInt("rewards.tokens.max", 5);
            int amount = random.nextInt((max - min) + 1) + min;

            String rawTokenCmd = config.getString("rewards.tokens.command", "tokens give %player% %amount%");
            String finalTokenCmd = rawTokenCmd.replace("%player%", player.getName())
                                             .replace("%amount%", String.valueOf(amount));
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalTokenCmd);
        }
    }

    // ================= SESSION MANAGEMENT =================

    public void saveActiveInventory(Player player, Location loc, Inventory inv) {
        activeInventories
            .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
            .put(loc, inv);
    }

    public Inventory getOrCreateActiveInventory(Player player, Location loc) {
        Map<Location, Inventory> playerMap = activeInventories.get(player.getUniqueId());
        
        if (playerMap != null && playerMap.containsKey(loc)) {
            if (getRemainingCooldown(player, loc) > 0) {
                return playerMap.get(loc);
            }
        }

        applySpecialRewards(player);

        Inventory inv = CustomInventoryFactory.createLootInventory(this, loc, player);
        saveActiveInventory(player, loc, inv);
        return inv;
    }

    // ================= CONFIG & DATA MANAGEMENT =================

    public void loadFromConfig() {
        // FORCE the plugin to read the file from disk, ignoring memory
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        tables.clear();
        linkedChests.clear();
        cooldowns.clear();
        activeInventories.clear();
        
        // Ensure Reward defaults are present in the config if they are missing
        boolean needsSave = false;
        if (!cfg.contains("rewards.xp.chance")) { cfg.set("rewards.xp.chance", 1.0); needsSave = true; }
        if (!cfg.contains("rewards.tokens.chance")) { cfg.set("rewards.tokens.chance", 1.0); needsSave = true; }
        if (needsSave) plugin.saveConfig();
        
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

        attemptChestLoad();
    }

    public void saveToConfig() {
        FileConfiguration cfg = plugin.getConfig();
        
        // Build tables and chests from scratch
        cfg.set("tables", null);
        cfg.set("chests", null);

        for (Map.Entry<String, LootTable> entry : tables.entrySet()) {
            entry.getValue().saveToConfig(cfg.createSection("tables." + entry.getKey()));
        }

        int index = 0;
        for (Map.Entry<Location, String> entry : linkedChests.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null) continue;

            String path = "chests." + index;
            cfg.set(path + ".world", loc.getWorld().getName());
            cfg.set(path + ".x", loc.getBlockX());
            cfg.set(path + ".y", loc.getBlockY());
            cfg.set(path + ".z", loc.getBlockZ());
            cfg.set(path + ".table", entry.getValue());
            index++;
        }
        
        // Save the file. Because we didn't touch the "rewards" key, it remains as is.
        plugin.saveConfig();
    }

    private void attemptChestLoad() {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection cSec = cfg.getConfigurationSection("chests");
        if (cSec == null) return;

        boolean missingWorld = false;

        for (String key : cSec.getKeys(false)) {
            ConfigurationSection sec = cSec.getConfigurationSection(key);
            if (sec == null) continue;

            String worldName = sec.getString("world");
            World world = (worldName != null) ? Bukkit.getWorld(worldName) : null;
            
            if (world == null) {
                missingWorld = true;
                continue;
            }

            Location loc = new Location(world, sec.getInt("x"), sec.getInt("y"), sec.getInt("z"));
            if (linkedChests.containsKey(loc)) continue;

            String tableName = sec.getString("table");
            if (tableName != null && tables.containsKey(tableName.toLowerCase())) {
                linkedChests.put(loc, tableName.toLowerCase());
            }
        }

        if (missingWorld) {
            new BukkitRunnable() {
                @Override
                public void run() { attemptChestLoad(); }
            }.runTaskLater(plugin, 100L);
        }
    }

    // ================= GETTERS & SETTERS =================

    public Set<Location> getLinkedChestLocations() { return linkedChests.keySet(); }

    public LootTable createTable(String name) {
        LootTable table = new LootTable(name);
        tables.put(name.toLowerCase(), table);
        saveToConfig();
        return table;
    }

    public LootTable getTable(String name) { return tables.get(name.toLowerCase()); }
    public boolean tableExists(String name) { return tables.containsKey(name.toLowerCase()); }
    public Set<String> getAllTableNames() { return tables.keySet(); }
    public void linkChest(Location loc, String tableName) { linkedChests.put(loc, tableName.toLowerCase()); saveToConfig(); }
    public void unlinkChest(Location loc) { linkedChests.remove(loc); cooldowns.remove(loc); saveToConfig(); }
    public boolean isLinked(Location loc) { return linkedChests.containsKey(loc); }
    public LootTable getLootTable(Location loc) { String name = linkedChests.get(loc); return name == null ? null : tables.get(name.toLowerCase()); }

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
}
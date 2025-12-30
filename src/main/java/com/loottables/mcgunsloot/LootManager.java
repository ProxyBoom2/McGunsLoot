package com.loottables.mcgunsloot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class LootManager {

    private final McGunsLoot plugin;

    private final Map<String, LootTable> tables = new HashMap<>();
    private final Map<Location, String> linkedChests = new HashMap<>();
    private final Map<Location, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    private final Random random = new Random();

    public LootManager(McGunsLoot plugin) {
        this.plugin = plugin;
    }

    public Random getRandom() {
        return random;
    }

    // ================= TABLES =================

    public LootTable createTable(String name) {
        LootTable table = new LootTable(name);
        tables.put(name.toLowerCase(), table);
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
    }

    public void unlinkChest(Location loc) {
        linkedChests.remove(loc);
        cooldowns.remove(loc);
    }

    public boolean isLinked(Location loc) {
        return linkedChests.containsKey(loc);
    }

    public LootTable getLootTable(Location loc) {
        String name = linkedChests.get(loc);
        return name == null ? null : tables.get(name);
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

    // ================= CONFIG =================

    public void loadFromConfig() {
        tables.clear();
        linkedChests.clear();
        cooldowns.clear();

        FileConfiguration cfg = plugin.getConfig();

        ConfigurationSection tSec = cfg.getConfigurationSection("tables");
        if (tSec != null) {
            for (String name : tSec.getKeys(false)) {
                LootTable table = LootTable.loadFromConfig(name, tSec.getConfigurationSection(name));
                tables.put(name.toLowerCase(), table);
            }
        }

        ConfigurationSection cSec = cfg.getConfigurationSection("chests");
        if (cSec != null) {
            for (String key : cSec.getKeys(false)) {
                ConfigurationSection sec = cSec.getConfigurationSection(key);

                Location loc = new Location(
                        Bukkit.getWorld(sec.getString("world")),
                        sec.getDouble("x"),
                        sec.getDouble("y"),
                        sec.getDouble("z")
                );

                String tableName = sec.getString("table");
                if (tableName != null && tables.containsKey(tableName.toLowerCase())) {
                    linkedChests.put(loc, tableName.toLowerCase());
                }
            }
        }
    }

    public void saveToConfig() {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("tables", null);
        cfg.set("chests", null);

        for (String name : tables.keySet()) {
            LootTable table = tables.get(name);
            table.saveToConfig(cfg.createSection("tables." + name));
        }

        int index = 0;
        for (Map.Entry<Location, String> entry : linkedChests.entrySet()) {
            Location loc = entry.getKey();
            String tableName = entry.getValue();

            String path = "chests." + index;
            cfg.set(path + ".world", loc.getWorld().getName());
            cfg.set(path + ".x", loc.getX());
            cfg.set(path + ".y", loc.getY());
            cfg.set(path + ".z", loc.getZ());
            cfg.set(path + ".table", tableName);
            index++;
        }

        plugin.saveConfig();
    }
}

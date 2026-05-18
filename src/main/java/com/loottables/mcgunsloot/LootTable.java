package com.loottables.mcgunsloot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class LootTable {
    private final String name;
    private int cooldownSeconds = 300;
    private final List<LootEntry> entries = new ArrayList<>();
    private final Random random = new Random();

    public LootTable(String name) { this.name = name; }
    public String getName() { return name; }
    public void setCooldownSeconds(int seconds) { this.cooldownSeconds = seconds; }
    public int getCooldownSeconds() { return cooldownSeconds; }

    public void addItem(ItemStack item, int minLevel, int min, int max, int rarity) {
        entries.add(new LootEntry(item, min, max, rarity, minLevel));
    }

    public void addEntry(LootEntry entry) { entries.add(entry); }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) entries.remove(index);
    }

    public List<LootEntry> getEntries() { return Collections.unmodifiableList(entries); }

    /**
     * Rolls loot using a 1-in-X rarity system.
     * e.g. rarity=5000 means a 1-in-5000 (0.02%) chance per roll.
     */
    public List<ItemStack> rollLoot() {
        List<ItemStack> results = new ArrayList<>();

        for (LootEntry entry : entries) {
            // Convert "1 in X" rarity to a percentage: 100.0 / rarity
            double chance = 100.0 / entry.getRarity();
            double roll = random.nextDouble() * 100;

            if (roll < chance) {
                ItemStack stack = entry.getItemStack().clone();
                int amount = entry.getMin();
                if (entry.getMax() > entry.getMin()) {
                    amount = random.nextInt((entry.getMax() - entry.getMin()) + 1) + entry.getMin();
                }
                stack.setAmount(amount);
                results.add(stack);
            }
        }
        return results;
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("cooldown", cooldownSeconds);
        section.set("items", null);
        for (int i = 0; i < entries.size(); i++) {
            LootEntry e = entries.get(i);
            ConfigurationSection cs = section.createSection("items." + i);
            cs.set("item", e.getItemStack());
            cs.set("min", e.getMin());
            cs.set("max", e.getMax());
            cs.set("rarity", e.getRarity());
            cs.set("minLevel", e.getMinLevel());
        }
    }

    public static LootTable loadFromConfig(String name, ConfigurationSection section) {
        LootTable table = new LootTable(name);
        if (section.contains("cooldown")) table.cooldownSeconds = section.getInt("cooldown");

        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection cs = items.getConfigurationSection(key);
                if (cs == null) continue;

                ItemStack item;
                if (cs.isItemStack("item")) {
                    item = cs.getItemStack("item");
                } else if (cs.contains("material")) {
                    String matName = cs.getString("material");
                    Material mat = Material.matchMaterial(matName != null ? matName : "");
                    item = (mat != null) ? new ItemStack(mat) : new ItemStack(Material.BARRIER);
                } else {
                    continue;
                }

                if (item == null) item = new ItemStack(Material.BARRIER);

                // Backwards compatibility: if old "weight" key exists and "rarity" does not,
                // treat the old weight value as a rarity by converting it (weight=10 -> rarity=10).
                int rarity;
                if (cs.contains("rarity")) {
                    rarity = cs.getInt("rarity", 1000);
                } else {
                    // Old weight was a percentage (e.g. 10 = 10%). Convert to 1-in-X.
                    int oldWeight = cs.getInt("weight", 10);
                    rarity = Math.max(1, (int) Math.round(100.0 / oldWeight));
                }

                table.addEntry(new LootEntry(
                        item,
                        cs.getInt("min", 1),
                        cs.getInt("max", 1),
                        rarity,
                        cs.getInt("minLevel", 0)
                ));
            }
        }
        return table;
    }
}
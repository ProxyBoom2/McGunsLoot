package com.loottables.mcgunsloot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class LootTable {

    private final String name;
    private int cooldownSeconds = 30;

    private final List<LootEntry> entries = new ArrayList<>();
    private final Random random = new Random();

    public LootTable(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void setCooldownSeconds(int seconds) { cooldownSeconds = seconds; }
    public int getCooldownSeconds() { return cooldownSeconds; }

    public void addEntry(LootEntry entry) { entries.add(entry); }
    public void removeEntry(int index) { if (index >= 0 && index < entries.size()) entries.remove(index); }
    public List<LootEntry> getEntries() { return Collections.unmodifiableList(entries); }

    public LootEntry getWeightedRandom() {
        int total = entries.stream().mapToInt(LootEntry::getWeight).sum();
        if (total == 0) return null;

        int roll = random.nextInt(total);
        int current = 0;

        for (LootEntry e : entries) {
            current += e.getWeight();
            if (roll < current) return e;
        }
        return null;
    }

    public List<GeneratedLoot> generateLoot() {
        List<GeneratedLoot> output = new ArrayList<>();
        if (entries.isEmpty()) return output;

        LootEntry pick = getWeightedRandom();
        if (pick == null) return output;

        int amount = pick.getMin() + random.nextInt((pick.getMax() - pick.getMin()) + 1);
        output.add(new GeneratedLoot(pick.getMaterial(), amount));

        return output;
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("cooldown", cooldownSeconds);

        int id = 0;
        for (LootEntry e : entries) {
            ConfigurationSection cs = section.createSection("items." + id);
            cs.set("material", e.getMaterial().name());
            cs.set("min", e.getMin());
            cs.set("max", e.getMax());
            cs.set("weight", e.getWeight());
            id++;
        }
    }

    public static LootTable loadFromConfig(String name, ConfigurationSection section) {
        LootTable table = new LootTable(name);

        if (section.contains("cooldown"))
            table.cooldownSeconds = section.getInt("cooldown");

        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection cs = items.getConfigurationSection(key);
                table.addEntry(new LootEntry(
                        Material.valueOf(cs.getString("material")),
                        cs.getInt("min"),
                        cs.getInt("max"),
                        cs.getInt("weight")
                ));
            }
        }
        return table;
    }

    public static class GeneratedLoot {
        public final Material material;
        public final int amount;
        public GeneratedLoot(Material material, int amount) { this.material = material; this.amount = amount; }
    }
}

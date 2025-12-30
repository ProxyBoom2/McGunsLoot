package com.loottables.mcgunsloot;

import org.bukkit.Material;

public class LootEntry {

    private final Material material;
    private final int min;
    private final int max;
    private final int weight;

    public LootEntry(Material material, int min, int max, int weight) {
        this.material = material;
        this.min = min;
        this.max = max;
        this.weight = weight;
    }

    // --- Required by older parts of your plugin ---
    public Material getMaterial() { 
        return material; 
    }

    public int getMin() { 
        return min; 
    }

    public int getMax() { 
        return max; 
    }

    public int getWeight() { 
        return weight; 
    }

    // --- Required by CustomInventoryFactory & ChestListener ---
    public int getMinAmount() {
        return min;
    }

    public int getMaxAmount() {
        return max;
    }
}

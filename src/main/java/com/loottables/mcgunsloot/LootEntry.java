package com.loottables.mcgunsloot;

import org.bukkit.inventory.ItemStack;

public class LootEntry {

    private final ItemStack itemStack; // Changed from Material to ItemStack
    private final int min;
    private final int max;
    private final int weight;
    private final int minLevel; // New field for tiered looting

    public LootEntry(ItemStack itemStack, int min, int max, int weight, int minLevel) {
        this.itemStack = itemStack;
        this.min = min;
        this.max = max;
        this.weight = weight;
        this.minLevel = minLevel;
    }

    public ItemStack getItemStack() { return itemStack; }
    public int getMin() { return min; }
    public int getMax() { return max; }
    public int getWeight() { return weight; }
    public int getMinLevel() { return minLevel; }

    // Compatibility for your existing UI/Listeners
    public int getMinAmount() { return min; }
    public int getMaxAmount() { return max; }
}
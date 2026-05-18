package com.loottables.mcgunsloot;

import org.bukkit.inventory.ItemStack;

public class LootEntry {
    private final ItemStack itemStack;
    private final int min;
    private final int max;
    private final int rarity; // "1 in X" — e.g. rarity=5000 means 1-in-5000 chance
    private final int minLevel;

    public LootEntry(ItemStack itemStack, int min, int max, int rarity, int minLevel) {
        this.itemStack = itemStack;
        this.min = min;
        this.max = max;
        this.rarity = rarity;
        this.minLevel = minLevel;
    }

    public ItemStack getItemStack() { return itemStack; }
    public int getMin() { return min; }
    public int getMax() { return max; }
    public int getRarity() { return rarity; }
    public int getMinLevel() { return minLevel; }

    // Compatibility for existing UI/Listeners
    public int getMinAmount() { return min; }
    public int getMaxAmount() { return max; }
}
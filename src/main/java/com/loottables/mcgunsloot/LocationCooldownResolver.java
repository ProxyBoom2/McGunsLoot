package com.loottables.mcgunsloot;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Helper class for resolving cooldowns.
 */
public class LocationCooldownResolver {

    private final LootManager lootManager;

    public LocationCooldownResolver(LootManager lootManager) {
        this.lootManager = lootManager;
    }

    public int getRemaining(Player player, Location loc) {
        return lootManager.getRemainingCooldown(player, loc);
    }
}

package com.loottables.mcgunsloot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class McGunsLoot extends JavaPlugin {

    private LootManager lootManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        lootManager = new LootManager(this);
        guiManager = new GuiManager(this, lootManager);
        
        // Load data
        lootManager.loadFromConfig();

        getServer().getPluginManager().registerEvents(new ChestListener(this, lootManager), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        LootCommand command = new LootCommand(lootManager, guiManager, this);
        getCommand("loots").setExecutor(command);
        getCommand("loots").setTabCompleter(command);

        // Particle Task
        new BukkitRunnable() {
            @Override
            public void run() {
                if (lootManager.getLinkedChestLocations().isEmpty()) return;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    for (Location loc : lootManager.getLinkedChestLocations()) {
                        if (loc.getWorld() == null || !p.getWorld().equals(loc.getWorld())) continue;
                        
                        if (p.getLocation().distanceSquared(loc) < 144) {
                            if (lootManager.getRemainingCooldown(p, loc) == 0) {
                                p.spawnParticle(
                                    Particle.VILLAGER_HAPPY, 
                                    loc.clone().add(0.5, 1.1, 0.5), 
                                    2, 0.15, 0.15, 0.15, 0
                                );
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);

        getLogger().info("McGunsLoot enabled. Data persistence fixed.");
    }

    /**
     * Helper method to reload the plugin entirely
     */
    public void reloadPlugin() {
        reloadConfig();
        lootManager.loadFromConfig();
    }

    @Override
    public void onDisable() {
        // REMOVED lootManager.saveToConfig() 
        // This prevents the plugin from overwriting your manual config edits on shutdown!
        getLogger().info("McGunsLoot disabled safely.");
    }
}
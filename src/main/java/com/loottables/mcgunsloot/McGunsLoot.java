package com.loottables.mcgunsloot;

import org.bukkit.plugin.java.JavaPlugin;

public class McGunsLoot extends JavaPlugin {

    private LootManager lootManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        lootManager = new LootManager(this);
        guiManager = new GuiManager(this, lootManager);

        lootManager.loadFromConfig();

        // ✔ FIXED: Required BOTH plugin + lootManager
        getServer().getPluginManager().registerEvents(new ChestListener(this, lootManager), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        LootCommand command = new LootCommand(lootManager, guiManager);
        getCommand("loots").setExecutor(command);
        getCommand("loots").setTabCompleter(command);

        getLogger().info("McGunsLoot enabled.");
        getServer().getPluginManager().registerEvents(
        new InventoryLockListener(), this
); 
    }

    @Override
    public void onDisable() {
        if (lootManager != null) {
            lootManager.saveToConfig();
        }
    }
}

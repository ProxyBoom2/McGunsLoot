package com.loottables.mcgunsloot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LootCommand implements CommandExecutor, TabCompleter {

    private final LootManager lootManager;
    private final GuiManager guiManager;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public LootCommand(LootManager lootManager, GuiManager guiManager, org.bukkit.plugin.java.JavaPlugin plugin) {
        this.lootManager = lootManager;
        this.guiManager = guiManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Global permission check for the base command
        if (!sender.hasPermission("mcgunsloot.use")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mcgunsloot.admin")) return true;
            
            plugin.reloadConfig();
            lootManager.loadFromConfig();
            
            sender.sendMessage("§a[McGunsLoot] Config and Loot Tables reloaded!");
            return true;
        }

        // /loots togglelucky - Handled for both console and players
        if (args.length > 0 && args[0].equalsIgnoreCase("togglelucky")) {
            if (!sender.hasPermission("mcgunsloot.admin")) return true;

            boolean current = plugin.getConfig().getBoolean("lucky-chest.enabled", true);
            plugin.getConfig().set("lucky-chest.enabled", !current);
            plugin.saveConfig();

            String status = !current ? "§aENABLED" : "§cDISABLED";
            sender.sendMessage("§6[McGunsLoot] §fLucky chests are now " + status);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6--- McGunsLoot Commands ---");
            player.sendMessage("§e/loots create <name> §7- Create a loot table");
            player.sendMessage("§e/loots additem <name> <lvl> <min> <max> <weight> §7- Add item");
            player.sendMessage("§e/loots edit <name> §7- Edit a loot table");
            player.sendMessage("§e/loots link <name> [radius/world] §7- Link chests");
            player.sendMessage("§e/loots unlink §7- Unlink looked-at chest");
            player.sendMessage("§e/loots setlucky §7- Force a chest to be lucky (Testing)");
            player.sendMessage("§e/loots togglelucky §7- Globally enable/disable lucky chests");
            return true;
        }

        // /loots setlucky
        if (args[0].equalsIgnoreCase("setlucky")) {
            if (!player.hasPermission("mcgunsloot.admin")) return true;
            Block target = player.getTargetBlockExact(5);
            if (target == null || !lootManager.isLinked(target.getLocation())) {
                player.sendMessage("§cYou must be looking at a linked chest!");
                return true;
            }
            lootManager.setLucky(player, target.getLocation(), true);
            player.sendMessage("§a[Testing] This chest is now §eLUCKY §afor you.");
            return true;
        }

        // /loots create <name>
        if (args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission("mcgunsloot.admin")) return true;
            if (args.length < 2) {
                player.sendMessage("§cUsage: /loots create <name>");
                return true;
            }
            String name = args[1];
            if (lootManager.tableExists(name)) {
                player.sendMessage("§cA loot table named §e" + name + " §calready exists.");
                return true;
            }
            lootManager.createTable(name);
            lootManager.saveToConfig();
            player.sendMessage("§aCreated loot table: §e" + name);
            return true;
        }

        // /loots additem
        if (args[0].equalsIgnoreCase("additem")) {
            if (!player.hasPermission("mcgunsloot.admin")) return true;
            if (args.length < 6) {
                player.sendMessage("§cUsage: /loots additem <tableName> <minLevel> <minQty> <maxQty> <weight>");
                return true;
            }

            String tableName = args[1];
            LootTable table = lootManager.getTable(tableName);

            if (table == null) {
                player.sendMessage("§cNo loot table found with name §e" + tableName);
                return true;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getType().isAir()) {
                player.sendMessage("§cYou must be holding an item to add it!");
                return true;
            }

            try {
                int minLevel = Integer.parseInt(args[2]);
                int minQty = Integer.parseInt(args[3]);
                int maxQty = Integer.parseInt(args[4]);
                int weight = Integer.parseInt(args[5]);
                
                table.addItem(itemInHand.clone(), minLevel, minQty, maxQty, weight); 
                lootManager.saveToConfig();

                player.sendMessage("§aAdded §e" + itemInHand.getType().name() + " §ato table §e" + tableName);
            } catch (NumberFormatException e) {
                player.sendMessage("§cAll values after the table name must be numbers!");
            }
            return true;
        }

        // /loots edit <name>
        if (args[0].equalsIgnoreCase("edit")) {
            if (!player.hasPermission("mcgunsloot.admin")) return true;
            if (args.length < 2) {
                player.sendMessage("§cUsage: /loots edit <name>");
                return true;
            }
            String name = args[1];
            LootTable table = lootManager.getTable(name);
            if (table == null) {
                player.sendMessage("§cNo loot table found with name §e" + name + "§c.");
                return true;
            }
            guiManager.openEditor(player, table);
            return true;
        }

        // /loots link <name> [radius/world]
        if (args[0].equalsIgnoreCase("link")) {
            if (!player.hasPermission("mcgunsloot.admin")) return true;
            if (args.length < 2) {
                player.sendMessage("§cUsage: /loots link <tableName> [radius/world]");
                return true;
            }
            String name = args[1];
            if (!lootManager.tableExists(name)) {
                player.sendMessage("§cNo loot table found with name §e" + name);
                return true;
            }

            if (args.length == 3) {
                if (args[2].equalsIgnoreCase("world")) {
                    int count = 0;
                    for (org.bukkit.Chunk chunk : player.getWorld().getLoadedChunks()) {
                        for (org.bukkit.block.BlockState state : chunk.getTileEntities()) {
                            if (state instanceof Chest) {
                                if (!lootManager.isLinked(state.getLocation())) {
                                    lootManager.linkChest(state.getLocation(), name);
                                    count++;
                                }
                            }
                        }
                    }
                    lootManager.saveToConfig();
                    player.sendMessage("§aLinked §e" + count + " §anew chests in world: " + player.getWorld().getName());
                } else {
                    try {
                        int radius = Integer.parseInt(args[2]);
                        int count = 0;
                        org.bukkit.Location origin = player.getLocation();
                        for (int x = -radius; x <= radius; x++) {
                            for (int y = -radius; y <= radius; y++) {
                                for (int z = -radius; z <= radius; z++) {
                                    Block b = origin.clone().add(x, y, z).getBlock();
                                    if (b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST) {
                                        if (!lootManager.isLinked(b.getLocation())) {
                                            lootManager.linkChest(b.getLocation(), name);
                                            count++;
                                        }
                                    }
                                }
                            }
                        }
                        lootManager.saveToConfig();
                        player.sendMessage("§aLinked §e" + count + " §anew chests within " + radius + " blocks.");
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cUse a number for radius or 'world'.");
                    }
                }
                return true;
            }

            Block target = player.getTargetBlockExact(5);
            if (target == null || !(target.getState() instanceof Chest chest)) {
                player.sendMessage("§cLook at a chest or specify a radius/world.");
                return true;
            }
            
            if (lootManager.isLinked(chest.getLocation())) {
                player.sendMessage("§cThis chest is already linked to a loot table!");
                return true;
            }

            lootManager.linkChest(chest.getLocation(), name);
            lootManager.saveToConfig();
            player.sendMessage("§aLinked single chest.");
            return true;
        }

        // /loots unlink
        if (args[0].equalsIgnoreCase("unlink")) {
            if (!player.hasPermission("mcgunsloot.admin")) return true;
            Block target = player.getTargetBlockExact(5);
            if (target == null || !(target.getState() instanceof Chest chest)) {
                player.sendMessage("§cYou must be looking at a chest within 5 blocks.");
                return true;
            }
            lootManager.unlinkChest(chest.getLocation());
            lootManager.saveToConfig();
            player.sendMessage("§aUnlinked chest.");
            return true;
        }

        player.sendMessage("§cUnknown subcommand.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("mcgunsloot.use")) return new ArrayList<>();
        
        List<String> out = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String s : List.of("create", "additem", "edit", "link", "unlink", "reload", "setlucky", "togglelucky")) {
                if (s.startsWith(prefix)) out.add(s);
            }
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || 
                                args[0].equalsIgnoreCase("link") || 
                                args[0].equalsIgnoreCase("additem"))) {
            String prefix = args[1].toLowerCase();
            for (String name : lootManager.getAllTableNames()) {
                if (name.toLowerCase().startsWith(prefix)) out.add(name);
            }
        }
        
        if (args[0].equalsIgnoreCase("additem")) {
            if (args.length == 3) out.add("<minLevel>");
            if (args.length == 4) out.add("<minQty>");
            if (args.length == 5) out.add("<maxQty>");
            if (args.length == 6) out.add("<weight>");
        }

        if (args[0].equalsIgnoreCase("link") && args.length == 3) {
            out.addAll(List.of("world", "5", "10", "20"));
        }

        return out;
    }
}
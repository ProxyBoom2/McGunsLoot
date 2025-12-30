package com.loottables.mcgunsloot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class LootCommand implements CommandExecutor, TabCompleter {

    private final LootManager lootManager;
    private final GuiManager guiManager;

    public LootCommand(LootManager lootManager, GuiManager guiManager) {
        this.lootManager = lootManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6McGunsLoot Commands:");
            player.sendMessage("§e/loots create <name> §7- Create a loot table");
            player.sendMessage("§e/loots edit <name> §7- Edit a loot table");
            player.sendMessage("§e/loots link <name> §7- Link looked-at chest to a table");
            player.sendMessage("§e/loots unlink §7- Unlink looked-at chest");
            return true;
        }

        // -----------------------------
        // /loots create <name>
        // -----------------------------
        if (args[0].equalsIgnoreCase("create")) {
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

        // -----------------------------
        // /loots edit <name>
        // -----------------------------
        if (args[0].equalsIgnoreCase("edit")) {
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

        // -----------------------------
        // /loots link <name>
        // -----------------------------
        if (args[0].equalsIgnoreCase("link")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /loots link <tableName>");
                return true;
            }

            String name = args[1];
            LootTable table = lootManager.getTable(name);

            if (table == null) {
                player.sendMessage("§cNo loot table found with name §e" + name + "§c.");
                return true;
            }

            Block target = player.getTargetBlockExact(5);
            if (target == null || !(target.getState() instanceof Chest chest)) {
                player.sendMessage("§cYou must be looking at a chest within 5 blocks.");
                return true;
            }

            lootManager.linkChest(chest.getLocation(), name);
            lootManager.saveToConfig();
            player.sendMessage("§aLinked chest at §e" +
                    chest.getLocation().getBlockX() + "," +
                    chest.getLocation().getBlockY() + "," +
                    chest.getLocation().getBlockZ() +
                    " §ato loot table §e" + name + "§a.");
            return true;
        }

        // -----------------------------
        // /loots unlink
        // -----------------------------
        if (args[0].equalsIgnoreCase("unlink")) {

            Block target = player.getTargetBlockExact(5);
            if (target == null || !(target.getState() instanceof Chest chest)) {
                player.sendMessage("§cYou must be looking at a chest within 5 blocks.");
                return true;
            }

            if (!lootManager.isLinked(chest.getLocation())) {
                player.sendMessage("§cThat chest is not linked to any loot table.");
                return true;
            }

            lootManager.unlinkChest(chest.getLocation());
            lootManager.saveToConfig();
            player.sendMessage("§aUnlinked chest at §e" +
                    chest.getLocation().getBlockX() + "," +
                    chest.getLocation().getBlockY() + "," +
                    chest.getLocation().getBlockZ() + "§a.");
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Use §e/loots§c for help.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        List<String> out = new ArrayList<>();

        if (args.length == 1) {
            out.add("create");
            out.add("edit");
            out.add("link");
            out.add("unlink");
            return out;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("link"))) {
            String prefix = args[1].toLowerCase();
            for (String name : lootManager.getAllTableNames()) {
                if (name.toLowerCase().startsWith(prefix)) {
                    out.add(name);
                }
            }
        }

        return out;
    }
}

package com.loottables.mcgunsloot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;

public class InventoryLockListener implements Listener {

    // Bottom-right slot (0-based)
    private static final int LOCKED_SLOT = 26;

    private boolean isMcGunsInventory(InventoryView view) {
        return view != null && "§6§lMCGUNS".equals(view.getTitle());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!isMcGunsInventory(event.getView())) return;

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        // Block ONLY slot 26 in the top inventory
        if (rawSlot == LOCKED_SLOT && rawSlot < topSize) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (!isMcGunsInventory(event.getView())) return;

        // If drag touches slot 26 → cancel
        if (event.getRawSlots().contains(LOCKED_SLOT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDoubleClick(InventoryClickEvent event) {

        if (!isMcGunsInventory(event.getView())) return;

        // Prevent double-click collect from pulling the clock
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
        }
    }
}

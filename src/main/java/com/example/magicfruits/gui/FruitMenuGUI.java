package com.example.magicfruits.gui;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class FruitMenuGUI implements Listener {
    
    private final MagicFruits plugin;
    
    public FruitMenuGUI(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public void openFruitsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lFRUITS MENU §8§l✦");
        
        // Border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        // Add all fruits
        int slot = 10;
        for (FruitType fruit : FruitType.values()) {
            gui.setItem(slot, fruit.getIcon());
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }
        
        // Back button
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to main dashboard");
        gui.setItem(49, back);
        
        player.openInventory(gui);
    }
    
    public void openGiveFruitMenu(Player player, FruitType fruit) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lGIVE FRUIT §8§l✦");
        
        // Border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        gui.setItem(22, fruit.getIcon());
        
        int slot = 28;
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.displayName(Component.text("§a§l" + online.getName()));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Click to give §e" + fruit.getDisplayName()));
            lore.add(Component.text("§7to this player!"));
            meta.lore(lore);
            
            head.setItemMeta(meta);
            gui.setItem(slot, head);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        // Back button
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to fruits menu");
        gui.setItem(49, back);
        
        player.openInventory(gui);
    }
    
    public void openPlayerMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lPLAYER MENU §8§l✦");
        
        // Border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        // Spin all button
        ItemStack spinAll = createMenuItem(Material.NETHER_STAR, "§c§l✦ SPIN ALL PLAYERS ✦",
            "§7Start fruit spin for all",
            "§7online players!");
        gui.setItem(22, spinAll);
        
        // Player list
        int slot = 28;
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.displayName(Component.text("§b§l" + online.getName()));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7• §eLeft Click: §fStart spin"));
            lore.add(Component.text("§7• §eRight Click: §fGive fruit menu"));
            meta.lore(lore);
            
            head.setItemMeta(meta);
            gui.setItem(slot, head);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        // Back button
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to main dashboard");
        gui.setItem(49, back);
        
        player.openInventory(gui);
    }
    
    public void openSpinControl(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8§l✦ §6§lSPIN CONTROL §8§l✦");
        
        gui.setItem(11, createMenuItem(Material.NETHER_STAR, "§6§l✦ SPIN YOURSELF ✦",
            "§7Start a fruit spin for yourself"));
        
        gui.setItem(13, createMenuItem(Material.PLAYER_HEAD, "§b§l✦ SPIN SPECIFIC ✦",
            "§7Start spin for a specific player"));
        
        gui.setItem(15, createMenuItem(Material.DRAGON_HEAD, "§c§l✦ SPIN ALL ✦",
            "§7Start spin for all online players"));
        
        // Back button
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to main dashboard");
        gui.setItem(22, back);
        
        player.openInventory(gui);
    }
    
    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(Component.text(line));
        }
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        
        if (title.equals("§8§l✦ §6§lFRUITS MENU §8§l✦")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String name = net.md_5.bungee.api.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            if (name.equals("BACK")) {
                plugin.getAdminGUI().openMainDashboard(player);
                return;
            }
            
            for (FruitType fruit : FruitType.values()) {
                if (fruit.getDisplayName().equals(name)) {
                    openGiveFruitMenu(player, fruit);
                    return;
                }
            }
        } else if (title.equals("§8§l✦ §6§lGIVE FRUIT §8§l✦")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;
            
            String name = net.md_5.bungee.api.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            if (name.equals("BACK")) {
                openFruitsMenu(player);
                return;
            }
            
            // Get the fruit from the center item
            ItemStack fruitItem = event.getInventory().getItem(22);
            if (fruitItem == null) return;
            
            FruitType fruit = null;
            for (FruitType f : FruitType.values()) {
                if (fruitItem.getItemMeta().getDisplayName().equals(f.getDisplayName())) {
                    fruit = f;
                    break;
                }
            }
            
            if (fruit != null && clicked.getType() == Material.PLAYER_HEAD) {
                Player target = Bukkit.getPlayer(name);
                if (target != null) {
                    target.getInventory().addItem(fruit.createItem());
                    player.sendMessage("§aGave §e" + fruit.getDisplayName() + " §ato §e" + target.getName());
                    player.closeInventory();
                }
            }
        } else if (title.equals("§8§l✦ §6§lPLAYER MENU §8§l✦")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String name = net.md_5.bungee.api.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            if (name.equals("BACK")) {
                plugin.getAdminGUI().openMainDashboard(player);
                return;
            }
            
            if (name.equals("SPIN ALL PLAYERS")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    plugin.getSpinManager().startFruitSpin(online);
                }
                player.sendMessage("§aStarted spin for all online players!");
                player.closeInventory();
            } else {
                Player target = Bukkit.getPlayer(name);
                if (target != null) {
                    // Left click = spin, Right click = give menu
                    if (event.isLeftClick()) {
                        plugin.getSpinManager().startFruitSpin(target);
                        player.sendMessage("§aStarted spin for §e" + target.getName());
                        player.closeInventory();
                    } else if (event.isRightClick()) {
                        player.closeInventory();
                        player.sendMessage("§ePlease use the fruits menu to give fruits!");
                    }
                }
            }
        } else if (title.equals("§8§l✦ §6§lSPIN CONTROL §8§l✦")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String name = net.md_5.bungee.api.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            if (name.equals("BACK")) {
                plugin.getAdminGUI().openMainDashboard(player);
                return;
            }
            
            if (name.equals("SPIN YOURSELF")) {
                plugin.getSpinManager().startFruitSpin(player);
                player.closeInventory();
            } else if (name.equals("SPIN SPECIFIC")) {
                player.sendMessage("§eUse: /magicfruits spin <player>");
                player.closeInventory();
            } else if (name.equals("SPIN ALL")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    plugin.getSpinManager().startFruitSpin(online);
                }
                player.sendMessage("§aStarted spin for all online players!");
                player.closeInventory();
            }
        }
    }
}

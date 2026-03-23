package com.example.magicfruits.gui;

import com.example.magicfruits.MagicFruits;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminGUI implements Listener {
    
    private final MagicFruits plugin;
    
    public AdminGUI(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public void openMainDashboard(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lMAGIC FRUITS ADMIN §8§l✦");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        gui.setItem(10, createMenuItem(Material.CHEST, "§6§l✦ FRUITS MANAGEMENT ✦",
            "§7Manage all magical fruits", "§7Give fruits to players"));
        
        gui.setItem(12, createMenuItem(Material.PLAYER_HEAD, "§b§l✦ PLAYER MANAGEMENT ✦",
            "§7Manage player spins", "§7Spin for specific players"));
        
        gui.setItem(14, createMenuItem(Material.NETHER_STAR, "§5§l✦ SPIN CONTROL ✦",
            "§7Control spin system", "§7Start mass spins"));
        
        gui.setItem(16, createMenuItem(Material.ANVIL, "§d§l✦ DATA MANAGEMENT ✦",
            "§7Reset player data", "§7Clear fruits and progress", "§c⚠ WARNING: Cannot be undone!"));
        
        gui.setItem(29, createMenuItem(Material.COMPARATOR, "§e§l✦ GLOBAL SETTINGS ✦",
            "§7Configure plugin settings", "§7First join reward, death drops", "§7Cooldown, particles, sounds"));
        
        gui.setItem(31, createMenuItem(Material.PAPER, "§a§l✦ STATISTICS ✦",
            "§7View plugin statistics", "§7Active players, settings"));
        
        gui.setItem(33, createMenuItem(Material.ENDER_CHEST, "§c§l✦ RELOAD CONFIG ✦",
            "§7Reload configuration", "§7Apply new settings"));
        
        player.openInventory(gui);
    }
    
    public void openSettingsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lSETTINGS §8§l✦");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        String firstJoinStatus = plugin.getDataManager().isFirstJoinReward() ? "§a✓ ENABLED" : "§c✗ DISABLED";
        gui.setItem(19, createMenuItem(Material.GOLDEN_APPLE, "§6§lFIRST JOIN REWARD: " + firstJoinStatus,
            "§7New players get a fruit spin", "§7Click to " + (plugin.getDataManager().isFirstJoinReward() ? "disable" : "enable")));
        
        String dropStatus = plugin.getDataManager().isDropOnDeath() ? "§a✓ ENABLED" : "§c✗ DISABLED";
        gui.setItem(20, createMenuItem(Material.SKELETON_SKULL, "§c§lDROP ON DEATH: " + dropStatus,
            "§7Fruits drop when player dies", "§7Click to " + (plugin.getDataManager().isDropOnDeath() ? "disable" : "enable")));
        
        gui.setItem(21, createMenuItem(Material.CLOCK, "§e§lCOOLDOWN: " + plugin.getDataManager().getCooldownTime() + "s",
            "§7Ability cooldown time", "§7Click to cycle: 30s → 20s → 15s → 10s"));
        
        gui.setItem(22, createMenuItem(Material.SUGAR, "§b§lSPIN DURATION: " + plugin.getDataManager().getSpinDuration() + "s",
            "§7Duration of fruit spin", "§7Click to cycle: 15s → 10s → 20s"));
        
        String particlesStatus = plugin.getDataManager().isParticlesEnabled() ? "§a✓ ENABLED" : "§c✗ DISABLED";
        gui.setItem(23, createMenuItem(Material.FIREWORK_STAR, "§d§lPARTICLES: " + particlesStatus,
            "§7Visual particle effects", "§7Click to " + (plugin.getDataManager().isParticlesEnabled() ? "disable" : "enable")));
        
        String soundsStatus = plugin.getDataManager().isSoundsEnabled() ? "§a✓ ENABLED" : "§c✗ DISABLED";
        gui.setItem(24, createMenuItem(Material.NOTE_BLOCK, "§a§lSOUNDS: " + soundsStatus,
            "§7Sound effects", "§7Click to " + (plugin.getDataManager().isSoundsEnabled() ? "disable" : "enable")));
        
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK", "§7Return to main dashboard");
        gui.setItem(49, back);
        
        player.openInventory(gui);
    }
    
    public void openStatistics(Player player) {
        Inventory stats = Bukkit.createInventory(null, 27, "§8§l✦ §6§lSTATISTICS §8§l✦");
        
        stats.setItem(11, createMenuItem(Material.PLAYER_HEAD, "§a§lONLINE PLAYERS",
            "§7" + Bukkit.getOnlinePlayers().size() + " players online"));
        
        stats.setItem(13, createMenuItem(Material.CHEST, "§b§lPLUGIN SETTINGS",
            "§7First Join Reward: " + (plugin.getDataManager().isFirstJoinReward() ? "§aON" : "§cOFF"),
            "§7Drop on Death: " + (plugin.getDataManager().isDropOnDeath() ? "§aON" : "§cOFF"),
            "§7Cooldown: §f" + plugin.getDataManager().getCooldownTime() + "s",
            "§7Spin Duration: §f" + plugin.getDataManager().getSpinDuration() + "s",
            "§7Particles: " + (plugin.getDataManager().isParticlesEnabled() ? "§aON" : "§cOFF"),
            "§7Sounds: " + (plugin.getDataManager().isSoundsEnabled() ? "§aON" : "§cOFF"),
            "§7Reset Players: §f" + plugin.getDataManager().getResetPlayersCount()));
        
        stats.setItem(15, createMenuItem(Material.NETHER_STAR, "§6§lACTIVE SPINS",
            "§7" + plugin.getSpinManager().getActiveSpinCount() + " active spins"));
        
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK", "§7Return to main dashboard");
        stats.setItem(22, back);
        
        player.openInventory(stats);
    }
    
    public void openDataManagement(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8§l✦ §6§lDATA MANAGEMENT §8§l✦");
        
        gui.setItem(11, createMenuItem(Material.REDSTONE, "§c§lRESET YOUR DATA",
            "§7Reset your own magical fruit data", "§7You will get a new spin on next join", "§c⚠ WARNING: This cannot be undone!"));
        
        gui.setItem(13, createMenuItem(Material.PLAYER_HEAD, "§e§lRESET PLAYER DATA",
            "§7Reset data for a specific player", "§7They will get a new spin on next join", "§c⚠ WARNING: This cannot be undone!"));
        
        gui.setItem(15, createMenuItem(Material.DRAGON_HEAD, "§4§lRESET ALL DATA",
            "§7Reset data for ALL players", "§7Everyone will get a new spin on next join", "§c⚠ WARNING: This cannot be undone!"));
        
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK", "§7Return to main dashboard");
        gui.setItem(22, back);
        
        player.openInventory(gui);
    }
    
    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        
        if (title.equals("§8§l✦ §6§lMAGIC FRUITS ADMIN §8§l✦")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            switch (name) {
                case "FRUITS MANAGEMENT":
                    new FruitMenuGUI(plugin).openFruitsMenu(player);
                    break;
                case "PLAYER MANAGEMENT":
                    new FruitMenuGUI(plugin).openPlayerMenu(player);
                    break;
                case "SPIN CONTROL":
                    new FruitMenuGUI(plugin).openSpinControl(player);
                    break;
                case "DATA MANAGEMENT":
                    openDataManagement(player);
                    break;
                case "GLOBAL SETTINGS":
                    openSettingsMenu(player);
                    break;
                case "STATISTICS":
                    openStatistics(player);
                    break;
                case "RELOAD CONFIG":
                    plugin.reloadConfig();
                    plugin.getDataManager().loadSettings();
                    player.sendMessage("§aConfig reloaded successfully!");
                    player.closeInventory();
                    break;
            }
        } else if (title.equals("§8§l✦ §6§lSETTINGS §8§l✦")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            if (name.equals("BACK")) {
                openMainDashboard(player);
                return;
            }
            
            if (name.contains("FIRST JOIN REWARD")) {
                plugin.getDataManager().setFirstJoinReward(!plugin.getDataManager().isFirstJoinReward());
                player.sendMessage(plugin.getDataManager().isFirstJoinReward() ? 
                    "§aFirst join reward has been §lENABLED" : "§cFirst join reward has been §lDISABLED");
                openSettingsMenu(player);
            } else if (name.contains("DROP ON DEATH")) {
                plugin.getDataManager().setDropOnDeath(!plugin.getDataManager().isDropOnDeath());
                player.sendMessage(plugin.getDataManager().isDropOnDeath() ? 
                    "§aFruits will §lDROP §aon death" : "§cFruits will §lNOT §cdrop on death anymore");
                openSettingsMenu(player);
            } else if (name.contains("COOLDOWN")) {
                int current = plugin.getDataManager().getCooldownTime();
                int newCooldown = current == 30 ? 20 : current == 20 ? 15 : current == 15 ? 10 : 30;
                plugin.getDataManager().setCooldownTime(newCooldown);
                player.sendMessage("§eCooldown set to: §f" + newCooldown + " seconds");
                openSettingsMenu(player);
            } else if (name.contains("SPIN DURATION")) {
                int current = plugin.getDataManager().getSpinDuration();
                int newDuration = current == 15 ? 10 : current == 10 ? 20 : 15;
                plugin.getDataManager().setSpinDuration(newDuration);
                player.sendMessage("§eSpin duration set to: §f" + newDuration + " seconds");
                openSettingsMenu(player);
            } else if (name.contains("PARTICLES")) {
                plugin.getDataManager().setParticlesEnabled(!plugin.getDataManager().isParticlesEnabled());
                player.sendMessage(plugin.getDataManager().isParticlesEnabled() ? 
                    "§aParticles have been §lENABLED" : "§cParticles have been §lDISABLED");
                openSettingsMenu(player);
            } else if (name.contains("SOUNDS")) {
                plugin.getDataManager().setSoundsEnabled(!plugin.getDataManager().isSoundsEnabled());
                player.sendMessage(plugin.getDataManager().isSoundsEnabled() ? 
                    "§aSounds have been §lENABLED" : "§cSounds have been §lDISABLED");
                openSettingsMenu(player);
            }
        } else if (title.equals("§8§l✦ §6§lSTATISTICS §8§l✦")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (name.equals("BACK")) {
                openMainDashboard(player);
            }
        } else if (title.equals("§8§l✦ §6§lDATA MANAGEMENT §8§l✦")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String name = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            
            switch (name) {
                case "RESET YOUR DATA":
                    plugin.getDataManager().resetPlayerData(player);
                    player.sendMessage("§cYour magical fruit data has been reset!");
                    player.closeInventory();
                    break;
                case "RESET PLAYER DATA":
                    player.sendMessage("§ePlease use: /magicfruits reset <player>");
                    player.closeInventory();
                    break;
                case "RESET ALL DATA":
                    plugin.getDataManager().resetAllPlayersData();
                    player.sendMessage("§cAll player data has been reset!");
                    player.closeInventory();
                    break;
                case "BACK":
                    openMainDashboard(player);
                    break;
            }
        }
    }
        }

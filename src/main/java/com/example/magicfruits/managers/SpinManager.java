package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SpinManager {
    
    private final MagicFruits plugin;
    private final Map<UUID, Boolean> spinning = new HashMap<>();
    private final Map<UUID, Integer> spinTaskIds = new HashMap<>();
    
    public SpinManager(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public void handlePlayerJoin(Player player) {
        if (plugin.getDataManager().isFirstJoinReward() && !player.hasPlayedBefore()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    startFruitSpin(player);
                }
            }.runTaskLater(plugin, 20L);
        }
        
        if (plugin.getDataManager().isPlayerReset(player.getUniqueId())) {
            plugin.getDataManager().removeResetPlayer(player.getUniqueId());
            new BukkitRunnable() {
                @Override
                public void run() {
                    startFruitSpin(player);
                }
            }.runTaskLater(plugin, 20L);
        }
    }
    
    public void startFruitSpin(Player player) {
        if (spinning.getOrDefault(player.getUniqueId(), false)) return;
        
        spinning.put(player.getUniqueId(), true);
        List<FruitType> fruits = Arrays.asList(FruitType.values());
        int duration = plugin.getDataManager().getSpinDuration() * 20;
        
        // Show spin GUI with all fruits
        showSpinGUI(player, fruits);
        
        player.sendTitle("§6§l✨ FRUIT SPIN! ✨", "§eGet ready to spin!", 10, 20, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int ticks = 0;
            int index = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !spinning.get(player.getUniqueId())) {
                    FruitType selected = fruits.get(index % fruits.size());
                    player.getInventory().addItem(selected.createItem());
                    
                    // Celebration effects
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        for (int i = 0; i < 360; i += 10) {
                            double rad = Math.toRadians(i);
                            double x = Math.cos(rad) * 3;
                            double z = Math.sin(rad) * 3;
                            player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().clone().add(x, 2, z), 0, 0, 0, 0, 1);
                        }
                    }
                    
                    if (plugin.getDataManager().isSoundsEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    }
                    
                    player.sendTitle("§6§l🎉 YOU GOT! 🎉", selected.getDisplayName(), 10, 40, 10);
                    player.sendMessage("§a§l✦ You received: §6" + selected.getDisplayName());
                    
                    // Close spin GUI
                    if (player.getOpenInventory().getTitle().equals("§6§l✦ FRUIT SPIN ✦")) {
                        player.closeInventory();
                    }
                    
                    spinning.put(player.getUniqueId(), false);
                    this.cancel();
                    return;
                }
                
                if (ticks % 5 == 0) {
                    index++;
                    FruitType current = fruits.get(index % fruits.size());
                    player.sendActionBar("§6§l⟳ §eSpinning: §f" + current.getDisplayName());
                    
                    // Update GUI to highlight current fruit
                    updateSpinGUI(player, fruits, current);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void showSpinGUI(Player player, List<FruitType> fruits) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§l✦ FRUIT SPIN ✦");
        
        // Add all fruits as items
        int slot = 10;
        for (FruitType fruit : fruits) {
            ItemStack displayItem = fruit.createDisplayItem();
            ItemMeta meta = displayItem.getItemMeta();
            meta.setDisplayName("§6§l" + fruit.getDisplayName());
            displayItem.setItemMeta(meta);
            gui.setItem(slot, displayItem);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }
        
        // Add decorative border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                    gui.setItem(i, border);
                }
            }
        }
        
        player.openInventory(gui);
    }
    
    private void updateSpinGUI(Player player, List<FruitType> fruits, FruitType current) {
        Inventory gui = player.getOpenInventory().getTopInventory();
        if (!gui.getTitle().equals("§6§l✦ FRUIT SPIN ✦")) return;
        
        int slot = 10;
        for (FruitType fruit : fruits) {
            ItemStack displayItem = fruit.createDisplayItem();
            ItemMeta meta = displayItem.getItemMeta();
            
            if (fruit == current) {
                meta.setDisplayName("§e§l➡ " + fruit.getDisplayName() + " ⬅");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§6§l✦ CURRENT FRUIT ✦");
                lore.add("§eSpinning...");
                meta.setLore(lore);
            } else {
                meta.setDisplayName("§7§l" + fruit.getDisplayName());
            }
            
            displayItem.setItemMeta(meta);
            gui.setItem(slot, displayItem);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }
    }
    
    public int getActiveSpinCount() {
        return spinning.size();
    }
}

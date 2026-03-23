package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpinManager {
    
    private final MagicFruits plugin;
    private final Map<UUID, Boolean> spinning = new ConcurrentHashMap<>();
    private final Map<UUID, List<ArmorStand>> activeArmorStands = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> spinTasks = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
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
        
        player.sendTitle("§6§l✨ FRUIT SPIN! ✨", "§eGet ready to spin!", 10, 20, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        
        // Create armor stands around player
        createFloatingItems(player, fruits);
        
        new BukkitRunnable() {
            int ticks = 0;
            int currentIndex = 0;
            
            @Override
            public void run() {
                if (ticks >= duration || !spinning.get(player.getUniqueId())) {
                    // Random selection instead of sequential
                    int randomIndex = random.nextInt(fruits.size());
                    FruitType selected = fruits.get(randomIndex);
                    
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
                    
                    // Remove armor stands
                    removeFloatingItems(player);
                    
                    spinning.put(player.getUniqueId(), false);
                    this.cancel();
                    return;
                }
                
                // Update highlighted fruit every 5 ticks
                if (ticks % 5 == 0) {
                    currentIndex = (currentIndex + 1) % fruits.size();
                    FruitType current = fruits.get(currentIndex);
                    player.sendActionBar("§6§l⟳ §eSpinning: §f" + current.getDisplayName() + " §6§l⟳");
                    
                    // Highlight current fruit in armor stands
                    highlightFruit(player, current);
                }
                
                // Rotate armor stands around player
                rotateFloatingItems(player, ticks);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void createFloatingItems(Player player, List<FruitType> fruits) {
        List<ArmorStand> stands = new ArrayList<>();
        int radius = 3;
        int count = fruits.size();
        
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = 1.5;
            
            Location loc = player.getLocation().clone().add(x, y, z);
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            
            // Set item in hand
            ItemStack item = fruits.get(i).createDisplayItem();
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§6§l" + fruits.get(i).getDisplayName());
            item.setItemMeta(meta);
            stand.setItemInHand(item);
            
            // Store data
            stand.setCustomName("§6§l" + fruits.get(i).getDisplayName());
            stand.setCustomNameVisible(true);
            
            stands.add(stand);
        }
        
        activeArmorStands.put(player.getUniqueId(), stands);
    }
    
    private void rotateFloatingItems(Player player, int tick) {
        List<ArmorStand> stands = activeArmorStands.get(player.getUniqueId());
        if (stands == null) return;
        
        Location center = player.getLocation();
        int count = stands.size();
        float rotationSpeed = 0.1f;
        
        for (int i = 0; i < count; i++) {
            ArmorStand stand = stands.get(i);
            double angle = (2 * Math.PI * i) / count + (tick * rotationSpeed);
            double radius = 3;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = 1.5 + Math.sin(angle * 2) * 0.2;
            
            Location newLoc = center.clone().add(x, y, z);
            stand.teleport(newLoc);
            stand.setRotation((float) Math.toDegrees(angle), 0);
        }
    }
    
    private void highlightFruit(Player player, FruitType current) {
        List<ArmorStand> stands = activeArmorStands.get(player.getUniqueId());
        if (stands == null) return;
        
        for (ArmorStand stand : stands) {
            String name = stand.getCustomName();
            if (name != null && name.contains(current.getDisplayName())) {
                stand.setCustomName("§e§l⭐ " + current.getDisplayName() + " ⭐");
                stand.setGlowing(true);
            } else {
                stand.setCustomName("§6§l" + (name != null ? name.replaceAll("§e§l⭐ | ⭐", "") : ""));
                stand.setGlowing(false);
            }
        }
    }
    
    private void removeFloatingItems(Player player) {
        List<ArmorStand> stands = activeArmorStands.remove(player.getUniqueId());
        if (stands != null) {
            for (ArmorStand stand : stands) {
                stand.remove();
            }
        }
    }
    
    public int getActiveSpinCount() {
        return spinning.size();
    }
}

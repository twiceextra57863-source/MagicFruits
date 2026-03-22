package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpinManager {
    
    private final MagicFruits plugin;
    private final Map<UUID, Boolean> spinActive = new ConcurrentHashMap<>();
    private final Map<UUID, FruitType> lastSelectedFruit = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> spinTaskIds = new ConcurrentHashMap<>();
    
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
        
        // Check if player was reset
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
        if (spinActive.getOrDefault(player.getUniqueId(), false)) return;
        
        spinActive.put(player.getUniqueId(), true);
        int durationTicks = plugin.getDataManager().getSpinDuration() * 20;
        List<FruitType> fruits = Arrays.asList(FruitType.values());
        
        // Show the spin GUI using action bar and inventory preview
        new BukkitRunnable() {
            int ticks = 0;
            int currentIndex = 0;
            
            @Override
            public void run() {
                if (ticks >= durationTicks || !spinActive.get(player.getUniqueId())) {
                    FruitType selected = fruits.get(currentIndex % fruits.size());
                    lastSelectedFruit.put(player.getUniqueId(), selected);
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
                    
                    player.showTitle(Title.title(
                        Component.text("§6§l🎉 YOU GOT! 🎉"),
                        Component.text(selected.getDisplayName()),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
                    ));
                    
                    // Show item in chat
                    player.sendMessage("§a§l✦ You received: §6" + selected.getDisplayName() + " §a§l✦");
                    
                    spinActive.put(player.getUniqueId(), false);
                    spinTaskIds.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                if (ticks % 5 == 0) {
                    currentIndex++;
                    FruitType current = fruits.get(currentIndex % fruits.size());
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        for (int i = 0; i < 36; i++) {
                            double angle = Math.toRadians(i * 10 + ticks * 3);
                            double radius = 3;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().clone().add(x, 2, z), 0, 0, 0, 0, 1);
                        }
                    }
                    
                    // Show fruit name and item in action bar with custom model
                    String displayName = current.getDisplayName();
                    player.sendActionBar(Component.text("§6§l⟳ §eSpinning: §f" + displayName + " §6§l⟳"));
                    
                    // Preview item in inventory temporarily (optional)
                    ItemStack previewItem = current.createDisplayItem();
                    player.getInventory().setItem(8, previewItem);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    public int getActiveSpinCount() {
        return spinActive.size();
    }
    
    public boolean isSpinning(UUID playerId) {
        return spinActive.getOrDefault(playerId, false);
    }
}

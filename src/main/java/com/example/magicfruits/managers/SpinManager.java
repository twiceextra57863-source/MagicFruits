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
        
        // Show spin start message
        player.sendTitle("§6§l✨ FRUIT SPIN! ✨", "§eGet ready to spin!", 10, 40, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int ticks = 0;
            int currentIndex = 0;
            int lastDisplayIndex = -1;
            
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
                    
                    // Show final result
                    player.showTitle(Title.title(
                        Component.text("§6§l🎉 YOU GOT! 🎉"),
                        Component.text(selected.getDisplayName()),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
                    ));
                    
                    player.sendMessage("§a§l✦ You received: §6" + selected.getDisplayName() + " §a§l✦");
                    
                    spinActive.put(player.getUniqueId(), false);
                    spinTaskIds.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                // Update fruit every 5 ticks for smooth animation
                if (ticks % 5 == 0) {
                    currentIndex++;
                    FruitType current = fruits.get(currentIndex % fruits.size());
                    
                    // Show fruit name as title (popup on screen)
                    if (currentIndex != lastDisplayIndex) {
                        lastDisplayIndex = currentIndex;
                        
                        // Display fruit in title with animation
                        String titleMessage = "§6§l⟳ " + current.getDisplayName() + " §6§l⟳";
                        String subtitleMessage = "§eSpinning... §7" + ((durationTicks - ticks) / 20) + "s remaining";
                        
                        player.sendTitle(titleMessage, subtitleMessage, 0, 10, 0);
                        
                        // Also show in action bar
                        player.sendActionBar(Component.text("§6§l⟳ §eSpinning: §f" + current.getDisplayName() + " §6§l⟳"));
                        
                        // Particle effects around player
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            for (int i = 0; i < 36; i++) {
                                double angle = Math.toRadians(i * 10 + ticks * 3);
                                double radius = 3;
                                double x = Math.cos(angle) * radius;
                                double z = Math.sin(angle) * radius;
                                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().clone().add(x, 2, z), 0, 0, 0, 0, 1);
                            }
                        }
                        
                        // Play spin sound
                        if (plugin.getDataManager().isSoundsEnabled() && ticks % 20 == 0) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f + (ticks / 100.0f));
                        }
                    }
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

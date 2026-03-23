package com.example.magicfruits.managers;

import com.example.magicfruits.MagicFruits;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GracePeriodManager {
    
    private final MagicFruits plugin;
    private boolean isActive = false;
    private int durationMinutes = 0;
    private int remainingSeconds = 0;
    private BossBar bossBar;
    private BukkitRunnable countdownTask;
    private Map<UUID, Boolean> hadFruitsBeforeGrace = new HashMap<>();
    
    public GracePeriodManager(MagicFruits plugin) {
        this.plugin = plugin;
        bossBar = Bukkit.createBossBar("§6§l✨ GRACE PERIOD INACTIVE ✨", BarColor.GREEN, BarStyle.SEGMENTED_20);
    }
    
    public void startGrace(int minutes) {
        if (isActive) {
            Bukkit.broadcastMessage("§c§l⚠ §fA grace period is already active!");
            return;
        }
        
        isActive = true;
        durationMinutes = minutes;
        remainingSeconds = minutes * 60;
        
        bossBar.setTitle("§a§l🛡️ GRACE PERIOD ACTIVE - " + formatTime(remainingSeconds) + " §a§l🛡️");
        bossBar.setColor(BarColor.GREEN);
        bossBar.setProgress(1.0);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean hasFruit = false;
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && com.example.magicfruits.FruitType.fromItem(item) != null) {
                    hasFruit = true;
                    break;
                }
            }
            hadFruitsBeforeGrace.put(player.getUniqueId(), hasFruit);
        }
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l╔══════════════════════════════════════════════════╗");
        Bukkit.broadcastMessage("§6§l║           §e§l✨ GRACE PERIOD STARTED! ✨           §6§l║");
        Bukkit.broadcastMessage("§6§l╠══════════════════════════════════════════════════╣");
        Bukkit.broadcastMessage("§6§l║  §fDuration: §e" + minutes + " minute" + (minutes > 1 ? "s" : "") + "                      §6§l║");
        Bukkit.broadcastMessage("§6§l║  §fNo fruits will be lost on death!              §6§l║");
        Bukkit.broadcastMessage("§6§l║  §fNew players will §aNOT §freceive fruits!      §6§l║");
        Bukkit.broadcastMessage("§6§l╚══════════════════════════════════════════════════╝");
        Bukkit.broadcastMessage("");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        startCountdown();
    }
    
    public void stopGrace() {
        if (!isActive) {
            Bukkit.broadcastMessage("§c§l⚠ §fNo active grace period to stop!");
            return;
        }
        
        isActive = false;
        
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        
        bossBar.setTitle("§c§l✨ GRACE PERIOD ENDED ✨");
        bossBar.setColor(BarColor.RED);
        bossBar.setProgress(1.0);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    bossBar.removePlayer(player);
                }
                bossBar.setTitle("§6§l✨ GRACE PERIOD INACTIVE ✨");
                bossBar.setColor(BarColor.GREEN);
            }
        }.runTaskLater(plugin, 100L);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c§l╔══════════════════════════════════════════════════╗");
        Bukkit.broadcastMessage("§c§l║           §e§l✨ GRACE PERIOD ENDED! ✨           §c§l║");
        Bukkit.broadcastMessage("§c§l╠══════════════════════════════════════════════════╣");
        Bukkit.broadcastMessage("§c§l║  §fFruit protection has been removed!            §c§l║");
        Bukkit.broadcastMessage("§c§l║  §fNew players will now receive fruits!          §c§l║");
        Bukkit.broadcastMessage("§c§l╚══════════════════════════════════════════════════╝");
        Bukkit.broadcastMessage("");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 1.0f, 0.8f);
        }
        
        hadFruitsBeforeGrace.clear();
    }
    
    private void startCountdown() {
        countdownTask = new BukkitRunnable() {
            int seconds = remainingSeconds;
            
            @Override
            public void run() {
                if (!isActive) {
                    this.cancel();
                    return;
                }
                
                if (seconds <= 0) {
                    stopGrace();
                    this.cancel();
                    return;
                }
                
                double progress = (double) seconds / (durationMinutes * 60);
                bossBar.setProgress(progress);
                bossBar.setTitle("§a§l🛡️ GRACE PERIOD - " + formatTime(seconds) + " remaining §a§l🛡️");
                
                if (seconds <= 60) {
                    bossBar.setColor(BarColor.RED);
                } else if (seconds <= 180) {
                    bossBar.setColor(BarColor.YELLOW);
                } else {
                    bossBar.setColor(BarColor.GREEN);
                }
                
                if (seconds == 60) {
                    Bukkit.broadcastMessage("§e§l⚠ §f1 minute remaining in grace period! §e§l⚠");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                } else if (seconds == 30) {
                    Bukkit.broadcastMessage("§e§l⚠ §f30 seconds remaining in grace period! §e§l⚠");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    }
                } else if (seconds == 10) {
                    Bukkit.broadcastMessage("§c§l⚠ §f10 seconds remaining! §c§l⚠");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                } else if (seconds <= 5 && seconds > 0) {
                    Bukkit.broadcastMessage("§c§l⚠ §f" + seconds + "... §c§l⚠");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                }
                
                seconds--;
            }
        };
        
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public boolean shouldPreventDeathDrop(Player player) {
        if (!isActive) return false;
        return hadFruitsBeforeGrace.getOrDefault(player.getUniqueId(), false);
    }
    
    public boolean shouldPreventFirstJoinReward() {
        return isActive;
    }
    
    public int getRemainingSeconds() {
        return remainingSeconds;
    }
}

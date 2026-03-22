package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    
    private final MagicFruits plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> activeTasks = new ConcurrentHashMap<>();
    
    public CooldownManager(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public boolean isOnCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) return false;
        return cooldowns.get(playerId) > System.currentTimeMillis();
    }
    
    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis() + (plugin.getDataManager().getCooldownTime() * 1000L));
    }
    
    public void startCooldown(UUID playerId, FruitType fruit) {
        setCooldown(playerId);
        startCooldownDisplay(playerId, fruit);
    }
    
    public void startCooldownDisplay(UUID playerId, FruitType fruit) {
        if (activeTasks.containsKey(playerId)) {
            plugin.getServer().getScheduler().cancelTask(activeTasks.get(playerId));
        }
        
        int taskId = new BukkitRunnable() {
            int secondsLeft = plugin.getDataManager().getCooldownTime();
            
            @Override
            public void run() {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player == null) {
                    this.cancel();
                    return;
                }
                
                if (secondsLeft <= 0) {
                    player.setExp(0);
                    player.setLevel(0);
                    if (plugin.getDataManager().isSoundsEnabled()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                    player.sendActionBar(Component.text("§a§l✓ §f" + fruit.getDisplayName() + " §a§lREADY!"));
                    activeTasks.remove(playerId);
                    this.cancel();
                    return;
                }
                
                showCooldownOnXPBar(player, secondsLeft, fruit);
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        
        activeTasks.put(playerId, taskId);
    }
    
    public void showCooldownOnXPBar(Player player, int secondsLeft, FruitType fruit) {
        float progress = (float) secondsLeft / (float) plugin.getDataManager().getCooldownTime();
        player.setExp(progress);
        player.setLevel(secondsLeft);
        
        String message = "§c§l⏳ §f" + fruit.getDisplayName() + " §c§lCOOLDOWN: §e" + secondsLeft + "s";
        
        if (secondsLeft <= 5) {
            message = "§c§l⚠ §f" + fruit.getDisplayName() + " §c§lREADY IN: §e" + secondsLeft + "s §c§l⚠";
        } else if (secondsLeft <= 10) {
            message = "§6§l⌛ §f" + fruit.getDisplayName() + " §6§lCOOLDOWN: §e" + secondsLeft + "s";
        }
        
        player.sendActionBar(Component.text(message));
    }
    
    public void showCooldownMessage(Player player, FruitType fruit) {
        long timeLeft = cooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        long secondsLeft = timeLeft / 1000;
        player.sendMessage("§c§l⚠ §fAbility on cooldown! §7(" + secondsLeft + " seconds remaining)");
    }
    
    public int getRemainingSeconds(UUID playerId) {
        long timeLeft = cooldowns.getOrDefault(playerId, 0L) - System.currentTimeMillis();
        return (int) Math.max(0, timeLeft / 1000);
    }
    
    public void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cooldowns.entrySet().removeIf(entry -> entry.getValue() <= System.currentTimeMillis());
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }
}

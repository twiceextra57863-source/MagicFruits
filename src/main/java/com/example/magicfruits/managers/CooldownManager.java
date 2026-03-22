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
    private final Map<UUID, Map<FruitType, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<FruitType, Integer>> activeTasks = new ConcurrentHashMap<>();
    
    public CooldownManager(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public boolean isOnCooldown(UUID playerId, FruitType fruit) {
        Map<FruitType, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Long cooldownEnd = playerCooldowns.get(fruit);
        if (cooldownEnd == null) return false;
        return cooldownEnd > System.currentTimeMillis();
    }
    
    public void setCooldown(UUID playerId, FruitType fruit) {
        Map<FruitType, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        playerCooldowns.put(fruit, System.currentTimeMillis() + (plugin.getDataManager().getCooldownTime() * 1000L));
    }
    
    public void startCooldown(UUID playerId, FruitType fruit) {
        setCooldown(playerId, fruit);
        startCooldownDisplay(playerId, fruit);
    }
    
    public void startCooldownDisplay(UUID playerId, FruitType fruit) {
        Map<FruitType, Integer> playerTasks = activeTasks.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        
        if (playerTasks.containsKey(fruit)) {
            plugin.getServer().getScheduler().cancelTask(playerTasks.get(fruit));
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
                
                if (secondsLeft <= 0 || !isOnCooldown(playerId, fruit)) {
                    player.setExp(0);
                    player.setLevel(0);
                    if (plugin.getDataManager().isSoundsEnabled()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                    player.sendActionBar(Component.text("§a§l✓ §f" + fruit.getDisplayName() + " §a§lREADY!"));
                    
                    Map<FruitType, Integer> tasks = activeTasks.get(playerId);
                    if (tasks != null) tasks.remove(fruit);
                    this.cancel();
                    return;
                }
                
                showCooldownOnXPBar(player, secondsLeft, fruit);
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        
        playerTasks.put(fruit, taskId);
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
        Map<FruitType, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return;
        
        Long cooldownEnd = playerCooldowns.get(fruit);
        if (cooldownEnd == null) return;
        
        long timeLeft = cooldownEnd - System.currentTimeMillis();
        long secondsLeft = timeLeft / 1000;
        player.sendMessage("§c§l⚠ §f" + fruit.getDisplayName() + " on cooldown! §7(" + secondsLeft + " seconds remaining)");
    }
    
    public int getRemainingSeconds(UUID playerId, FruitType fruit) {
        Map<FruitType, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        Long cooldownEnd = playerCooldowns.get(fruit);
        if (cooldownEnd == null) return 0;
        long timeLeft = cooldownEnd - System.currentTimeMillis();
        return (int) Math.max(0, timeLeft / 1000);
    }
    
    public void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                cooldowns.forEach((playerId, fruitCooldowns) -> {
                    fruitCooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
                });
                cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }
}

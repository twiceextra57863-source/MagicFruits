package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    
    private final MagicFruits plugin;
    private final Map<UUID, Map<FruitType, Map<String, Long>>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<FruitType, Map<String, Integer>>> activeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isFirstClick = new ConcurrentHashMap<>();
    
    public CooldownManager(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public boolean isOnCooldown(UUID playerId, FruitType fruit, String abilityType) {
        if (fruit == FruitType.PORTAL_FRUIT && abilityType.equals("primary")) {
            if (isFirstClick.getOrDefault(playerId, true)) return false;
        }
        
        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Map<String, Long> abilityCooldowns = playerCooldowns.get(fruit);
        if (abilityCooldowns == null) return false;
        Long cooldownEnd = abilityCooldowns.get(abilityType);
        if (cooldownEnd == null) return false;
        
        boolean onCooldown = cooldownEnd > System.currentTimeMillis();
        if (!onCooldown && fruit == FruitType.PORTAL_FRUIT) isFirstClick.put(playerId, true);
        return onCooldown;
    }
    
    public void startCooldown(UUID playerId, FruitType fruit, String abilityType) {
        if (fruit == FruitType.PORTAL_FRUIT && abilityType.equals("primary")) {
            if (isFirstClick.getOrDefault(playerId, true)) {
                isFirstClick.put(playerId, false);
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) player.sendActionBar(Component.text("§b§l⚡ PORTAL READY!"));
                return;
            }
        }
        setCooldown(playerId, fruit, abilityType);
        startCooldownDisplay(playerId, fruit, abilityType);
    }

    // CRITICAL FIX: Made this PUBLIC so StealGUI and ThiefAbility can see it
    public void setCooldown(UUID playerId, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Map<String, Long> abilityCooldowns = playerCooldowns.computeIfAbsent(fruit, k -> new ConcurrentHashMap<>());
        long timeInSeconds = plugin.getDataManager().getCooldownTime();
        abilityCooldowns.put(abilityType, System.currentTimeMillis() + (timeInSeconds * 1000L));
    }
    
    public void startCooldownDisplay(UUID playerId, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Integer>> playerTasks = activeTasks.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Map<String, Integer> abilityTasks = playerTasks.computeIfAbsent(fruit, k -> new ConcurrentHashMap<>());
        
        if (abilityTasks.containsKey(abilityType)) {
            plugin.getServer().getScheduler().cancelTask(abilityTasks.get(abilityType));
        }
        
        int taskId = new BukkitRunnable() {
            int totalTime = plugin.getDataManager().getCooldownTime();
            int secondsLeft = totalTime;
            
            @Override
            public void run() {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player == null) { this.cancel(); return; }
                
                if (secondsLeft <= 0) {
                    player.setExp(0);
                    player.setLevel(0);
                    if (plugin.getDataManager().isSoundsEnabled()) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    if (fruit == FruitType.PORTAL_FRUIT) isFirstClick.put(playerId, true);
                    player.sendActionBar(Component.text("§a§l✓ §f" + fruit.getDisplayName() + " READY!"));
                    this.cancel();
                    return;
                }
                // CRITICAL FIX: Matched parameters with the call from MagicFruits.java
                showCooldownOnXPBar(player, secondsLeft, fruit, abilityType);
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        abilityTasks.put(abilityType, taskId);
    }
    
    // CRITICAL FIX: Removed 'totalTime' parameter to match MagicFruits.java
    public void showCooldownOnXPBar(Player player, int secondsLeft, FruitType fruit, String abilityType) {
        int totalTime = plugin.getDataManager().getCooldownTime();
        float progress = (float) secondsLeft / (float) totalTime;
        player.setExp(Math.max(0, Math.min(1.0f, progress)));
        player.setLevel(secondsLeft);
        
        String color = (secondsLeft <= 5) ? "§c§l" : "§e§l";
        player.sendActionBar(Component.text("§7" + fruit.getDisplayName() + " " + color + "COOLDOWN: " + secondsLeft + "s"));
    }

    // CRITICAL FIX: Added missing cleanup task
    public void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                cooldowns.forEach((playerId, fruitMap) -> {
                    fruitMap.forEach((fruit, abilityMap) -> abilityMap.entrySet().removeIf(entry -> entry.getValue() <= currentTime));
                });
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

    public int getRemainingSeconds(UUID playerId, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        Map<String, Long> abilityCooldowns = playerCooldowns.get(fruit);
        if (abilityCooldowns == null) return 0;
        Long cooldownEnd = abilityCooldowns.get(abilityType);
        return (cooldownEnd == null) ? 0 : (int) Math.max(0, (cooldownEnd - System.currentTimeMillis()) / 1000);
    }

    public void showCooldownMessage(Player player, FruitType fruit, String abilityType) {
        int seconds = getRemainingSeconds(player.getUniqueId(), fruit, abilityType);
        if (seconds > 0) player.sendMessage(Component.text("§c§l⚠ §fWait §e" + seconds + "s!"));
    }
}

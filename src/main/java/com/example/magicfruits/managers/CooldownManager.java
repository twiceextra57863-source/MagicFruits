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
    
    // Flag to track if the next click is the "First Click" (True = Free, False = Trigger Cooldown)
    private final Map<UUID, Boolean> isFirstClick = new ConcurrentHashMap<>();
    
    public CooldownManager(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if the ability is on cooldown.
     * Special logic for Portal Fruit: First click is always allowed.
     */
    public boolean isOnCooldown(UUID playerId, FruitType fruit, String abilityType) {
        // Portal Fruit Special Case: Check if it's the first click
        if (fruit == FruitType.PORTAL_FRUIT && abilityType.equals("primary")) {
            if (isFirstClick.getOrDefault(playerId, true)) {
                return false; // Pehla click hai, cooldown check bypass kar do
            }
        }
        
        // Normal Cooldown Check
        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        
        Map<String, Long> abilityCooldowns = playerCooldowns.get(fruit);
        if (abilityCooldowns == null) return false;
        
        Long cooldownEnd = abilityCooldowns.get(abilityType);
        if (cooldownEnd == null) return false;
        
        boolean onCooldown = cooldownEnd > System.currentTimeMillis();
        
        // Reset Portal Flag: Agar cooldown khatam ho chuka hai, toh wapas "First Click" enable kar do
        if (!onCooldown && fruit == FruitType.PORTAL_FRUIT) {
            isFirstClick.put(playerId, true);
        }
        
        return onCooldown;
    }
    
    /**
     * Set or trigger the cooldown. 
     */
    public void startCooldown(UUID playerId, FruitType fruit, String abilityType) {
        // Portal Fruit Logic: Pehle click par cooldown start nahi hoga, sirf flag badlega
        if (fruit == FruitType.PORTAL_FRUIT && abilityType.equals("primary")) {
            if (isFirstClick.getOrDefault(playerId, true)) {
                isFirstClick.put(playerId, false); // Agla click cooldown trigger karega
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) {
                    player.sendActionBar(Component.text("§b§l⚡ PORTAL READY: §fNext click starts cooldown!"));
                }
                return; // Cooldown start mat karo abhi
            }
        }
        
        // Agar dusra click hai ya koi aur fruit hai, toh cooldown start karo
        setCooldown(playerId, fruit, abilityType);
        startCooldownDisplay(playerId, fruit, abilityType);
        
        // Reset flag for portal so it starts fresh after cooldown ends
        if (fruit == FruitType.PORTAL_FRUIT) {
            isFirstClick.put(playerId, false); 
        }
    }
    
    private void setCooldown(UUID playerId, FruitType fruit, String abilityType) {
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
                if (player == null) {
                    this.cancel();
                    return;
                }
                
                if (secondsLeft <= 0) {
                    player.setExp(0);
                    player.setLevel(0);
                    if (plugin.getDataManager().isSoundsEnabled()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                    
                    // Reset Portal to first click mode
                    if (fruit == FruitType.PORTAL_FRUIT) {
                        isFirstClick.put(playerId, true);
                    }
                    
                    String abilityName = abilityType.equals("primary") ? "Primary" : "Secondary";
                    player.sendActionBar(Component.text("§a§l✓ §f" + fruit.getDisplayName() + " READY!"));
                    
                    Map<String, Integer> tasks = activeTasks.get(playerId).get(fruit);
                    if (tasks != null) tasks.remove(abilityType);
                    this.cancel();
                    return;
                }
                
                showCooldownOnXPBar(player, secondsLeft, totalTime, fruit, abilityType);
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        
        abilityTasks.put(abilityType, taskId);
    }
    
    public void showCooldownOnXPBar(Player player, int secondsLeft, int totalTime, FruitType fruit, String abilityType) {
        float progress = (float) secondsLeft / (float) totalTime;
        player.setExp(Math.max(0, Math.min(1.0f, progress)));
        player.setLevel(secondsLeft);
        
        String abilityName = abilityType.equals("primary") ? "Primary" : "Secondary";
        String color = (secondsLeft <= 5) ? "§c§l" : (secondsLeft <= 10) ? "§6§l" : "§e§l";
        
        player.sendActionBar(Component.text("§7" + fruit.getDisplayName() + " (" + abilityName + ") " + color + "COOLDOWN: " + secondsLeft + "s"));
    }

    public void showCooldownMessage(Player player, FruitType fruit, String abilityType) {
        int seconds = getRemainingSeconds(player.getUniqueId(), fruit, abilityType);
        if (seconds > 0) {
            player.sendMessage(Component.text("§c§l⚠ §fWait §e" + seconds + "s §fbefore using " + fruit.getDisplayName() + " again!"));
        }
    }
    
    public int getRemainingSeconds(UUID playerId, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        Map<String, Long> abilityCooldowns = playerCooldowns.get(fruit);
        if (abilityCooldowns == null) return 0;
        Long cooldownEnd = abilityCooldowns.get(abilityType);
        if (cooldownEnd == null) return 0;
        long timeLeft = cooldownEnd - System.currentTimeMillis();
        return (int) Math.max(0, timeLeft / 1000);
    }
}

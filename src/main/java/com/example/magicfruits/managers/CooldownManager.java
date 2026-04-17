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
    private final Map<UUID, Map<FruitType, Map<String, Long>>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<FruitType, Map<String, Integer>>> activeTasks = new ConcurrentHashMap<>();
    
    // Naya feature: Portal fruit ka state track karne ke liye
    private final Map<UUID, Boolean> portalFirstClick = new ConcurrentHashMap<>();
    
    public CooldownManager(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public boolean isOnCooldown(UUID playerId, FruitType fruit, String abilityType) {
        // PORTAL FRUIT SPECIAL HANDLING: Pehla click kabhi cooldown mein nahi hoga
        if (fruit == FruitType.PORTAL_FRUIT && abilityType.equals("primary")) {
            if (portalFirstClick.getOrDefault(playerId, true)) {
                return false; 
            }
        }

        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Map<String, Long> abilityCooldowns = playerCooldowns.get(fruit);
        if (abilityCooldowns == null) return false;
        Long cooldownEnd = abilityCooldowns.get(abilityType);
        if (cooldownEnd == null) return false;

        boolean onCooldown = cooldownEnd > System.currentTimeMillis();
        
        // Agar cooldown khatam ho gaya, toh portal flag reset kar do
        if (!onCooldown && fruit == FruitType.PORTAL_FRUIT) {
            portalFirstClick.put(playerId, true);
        }
        
        return onCooldown;
    }
    
    // PUBLIC rakha hai taaki StealGUI/ThiefAbility se error na aaye
    public void setCooldown(UUID playerId, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Map<String, Long> abilityCooldowns = playerCooldowns.computeIfAbsent(fruit, k -> new ConcurrentHashMap<>());
        abilityCooldowns.put(abilityType, System.currentTimeMillis() + (plugin.getDataManager().getCooldownTime() * 1000L));
    }
    
    public void startCooldown(UUID playerId, FruitType fruit, String abilityType) {
        // Portal fruit logic: Pehle click pe cooldown display start nahi hoga
        if (fruit == FruitType.PORTAL_FRUIT && abilityType.equals("primary")) {
            if (portalFirstClick.getOrDefault(playerId, true)) {
                portalFirstClick.put(playerId, false); // Agle click ke liye flag set kiya
                Player p = plugin.getServer().getPlayer(playerId);
                if (p != null) p.sendActionBar(Component.text("§b§l⚡ PORTAL READY!"));
                return; // Cooldown timer start nahi hoga
            }
        }

        setCooldown(playerId, fruit, abilityType);
        startCooldownDisplay(playerId, fruit, abilityType);
    }
    
    public void startCooldownDisplay(UUID playerId, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Integer>> playerTasks = activeTasks.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Map<String, Integer> abilityTasks = playerTasks.computeIfAbsent(fruit, k -> new ConcurrentHashMap<>());
        
        if (abilityTasks.containsKey(abilityType)) {
            plugin.getServer().getScheduler().cancelTask(abilityTasks.get(abilityType));
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
                
                if (secondsLeft <= 0 || !isOnCooldown(playerId, fruit, abilityType)) {
                    player.setExp(0);
                    player.setLevel(0);
                    if (plugin.getDataManager().isSoundsEnabled()) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                    
                    // Portal reset
                    if (fruit == FruitType.PORTAL_FRUIT) {
                        portalFirstClick.put(playerId, true);
                    }

                    String abilityName = abilityType.equals("primary") ? "Primary" : "Secondary";
                    player.sendActionBar(Component.text("§a§l✓ §f" + fruit.getDisplayName() + " §a§l" + abilityName + " READY!"));
                    
                    Map<String, Integer> tasks = activeTasks.get(playerId).get(fruit);
                    if (tasks != null) tasks.remove(abilityType);
                    this.cancel();
                    return;
                }
                
                showCooldownOnXPBar(player, secondsLeft, fruit, abilityType);
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        
        abilityTasks.put(abilityType, taskId);
    }
    
    public void showCooldownOnXPBar(Player player, int secondsLeft, FruitType fruit, String abilityType) {
        float progress = (float) secondsLeft / (float) plugin.getDataManager().getCooldownTime();
        player.setExp(progress);
        player.setLevel(secondsLeft);
        
        String abilityName = abilityType.equals("primary") ? "Primary" : "Secondary";
        String message = "§c§l⏳ §f" + fruit.getDisplayName() + " (" + abilityName + ") §c§lCOOLDOWN: §e" + secondsLeft + "s";
        
        if (secondsLeft <= 5) {
            message = "§c§l⚠ §f" + fruit.getDisplayName() + " (" + abilityName + ") §c§lREADY IN: §e" + secondsLeft + "s §c§l⚠";
        } else if (secondsLeft <= 10) {
            message = "§6§l⌛ §f" + fruit.getDisplayName() + " (" + abilityName + ") §6§lCOOLDOWN: §e" + secondsLeft + "s";
        }
        
        player.sendActionBar(Component.text(message));
    }
    
    public void showCooldownMessage(Player player, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return;
        Map<String, Long> abilityCooldowns = playerCooldowns.get(fruit);
        if (abilityCooldowns == null) return;
        Long cooldownEnd = abilityCooldowns.get(abilityType);
        if (cooldownEnd == null) return;
        
        long timeLeft = cooldownEnd - System.currentTimeMillis();
        long secondsLeft = timeLeft / 1000;
        String abilityName = abilityType.equals("primary") ? "Primary" : "Secondary";
        player.sendMessage("§c§l⚠ §f" + fruit.getDisplayName() + " (" + abilityName + ") on cooldown! §7(" + secondsLeft + " seconds remaining)");
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
    
    public void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                cooldowns.forEach((playerId, fruitMap) -> {
                    fruitMap.forEach((fruit, abilityMap) -> {
                        abilityMap.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
                    });
                    fruitMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
                });
                cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }
}

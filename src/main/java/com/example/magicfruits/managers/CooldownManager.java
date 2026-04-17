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
    private final Map<UUID, Boolean> portalFirstClick = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    
    public CooldownManager(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public boolean isOnCooldown(UUID playerId, FruitType fruit, String abilityType) {
        if (fruit == FruitType.PORTAL_FRUIT && abilityType.equals("primary")) {
            if (portalFirstClick.getOrDefault(playerId, true)) return false;
        }

        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Map<String, Long> abilityCooldowns = playerCooldowns.get(fruit);
        if (abilityCooldowns == null) return false;
        Long cooldownEnd = abilityCooldowns.get(abilityType);
        
        if (cooldownEnd == null) return false;
        boolean onCooldown = cooldownEnd > System.currentTimeMillis();
        
        if (!onCooldown && fruit == FruitType.PORTAL_FRUIT) portalFirstClick.put(playerId, true);
        return onCooldown;
    }
    
    public void setCooldown(UUID playerId, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Long>> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Map<String, Long> abilityCooldowns = playerCooldowns.computeIfAbsent(fruit, k -> new ConcurrentHashMap<>());
        abilityCooldowns.put(abilityType, System.currentTimeMillis() + (plugin.getDataManager().getCooldownTime() * 1000L));
    }
    
    public void startCooldown(UUID playerId, FruitType fruit, String abilityType) {
        if (isOnCooldown(playerId, fruit, abilityType)) return;

        if (fruit == FruitType.PORTAL_FRUIT && abilityType.equals("primary")) {
            if (portalFirstClick.getOrDefault(playerId, true)) {
                portalFirstClick.put(playerId, false);
                Player p = plugin.getServer().getPlayer(playerId);
                if (p != null) p.sendActionBar(Component.text("§b§l⚡ PORTAL READY!"));
                return;
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
            final int totalSeconds = plugin.getDataManager().getCooldownTime();
            int secondsLeft = totalSeconds;
            
            @Override
            public void run() {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                if (secondsLeft <= 0) {
                    player.setExp(0);
                    player.setLevel(0);
                    if (plugin.getDataManager().isSoundsEnabled()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                    if (fruit == FruitType.PORTAL_FRUIT) portalFirstClick.put(playerId, true);
                    player.sendActionBar(Component.text("§a§l✓ §f" + fruit.getDisplayName() + " READY!"));
                    this.cancel();
                    return;
                }
                
                // Call the method
                showCooldownOnXPBar(player, secondsLeft, fruit, abilityType);
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        
        abilityTasks.put(abilityType, taskId);
    }
    
    // FIX: Method ko PUBLIC rakha hai taaki MagicFruits.java isse access kar sake
    public void showCooldownOnXPBar(Player player, int secondsLeft, FruitType fruit, String abilityType) {
        int totalSeconds = plugin.getDataManager().getCooldownTime();
        float progress = (float) secondsLeft / totalSeconds;
        
        player.setExp(Math.max(0, Math.min(1.0f, progress)));
        player.setLevel(secondsLeft);
        
        String abilityName = abilityType.equals("primary") ? "Primary" : "Secondary";
        String color = (secondsLeft <= 5) ? "§c§l" : (secondsLeft <= 10 ? "§6§l" : "§e§l");
        
        player.sendActionBar(Component.text("§8[ " + color + "⏳ §f" + fruit.getDisplayName() + " §7- " + secondsLeft + "s " + color + " ]"));
    }
    
    public void showCooldownMessage(Player player, FruitType fruit, String abilityType) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - lastMessageTime.getOrDefault(uuid, 0L) < 2500) return;

        int secondsLeft = getRemainingSeconds(uuid, fruit, abilityType);
        if (secondsLeft > 0) {
            player.sendMessage(Component.text("§c§l⚠ §f" + fruit.getDisplayName() + " is on cooldown! §7(" + secondsLeft + "s)"));
            lastMessageTime.put(uuid, now);
        }
    }
    
    public int getRemainingSeconds(UUID playerId, FruitType fruit, String abilityType) {
        Map<FruitType, Map<String, Long>> pCd = cooldowns.get(playerId);
        if (pCd == null) return 0;
        Map<String, Long> aCd = pCd.get(fruit);
        if (aCd == null) return 0;
        Long end = aCd.get(abilityType);
        if (end == null) return 0;
        return (int) Math.max(0, Math.ceil((end - System.currentTimeMillis()) / 1000.0));
    }
    
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
}

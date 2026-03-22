package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SpinManager {
    
    private final MagicFruits plugin;
    private final Map<UUID, Boolean> spinning = new HashMap<>();
    
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
        
        player.sendTitle("§6§l✨ FRUIT SPIN! ✨", "§eGet ready!", 10, 20, 10);
        
        new BukkitRunnable() {
            int ticks = 0;
            int index = 0;
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    FruitType selected = fruits.get(index % fruits.size());
                    player.getInventory().addItem(selected.createItem());
                    player.sendTitle("§6§l🎉 YOU GOT! 🎉", selected.getDisplayName(), 10, 40, 10);
                    player.sendMessage("§a§l✦ You received: §6" + selected.getDisplayName());
                    spinning.put(player.getUniqueId(), false);
                    this.cancel();
                    return;
                }
                
                if (ticks % 5 == 0) {
                    index++;
                    FruitType current = fruits.get(index % fruits.size());
                    player.sendActionBar("§6§l⟳ §eSpinning: §f" + current.getDisplayName());
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    public int getActiveSpinCount() {
        return spinning.size();
    }
}

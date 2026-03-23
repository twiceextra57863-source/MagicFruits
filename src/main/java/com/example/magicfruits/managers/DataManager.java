package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class DataManager {
    
    private final MagicFruits plugin;
    private Set<UUID> resetPlayers = new HashSet<>();
    
    // Settings
    private boolean firstJoinReward = true;
    private boolean dropOnDeath = true;
    private int cooldownTime = 30;
    private int spinDuration = 15;
    private boolean particlesEnabled = true;
    private boolean soundsEnabled = true;
    
    public DataManager(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public void loadSettings() {
        FileConfiguration config = plugin.getConfig();
        firstJoinReward = config.getBoolean("settings.first-join-reward", true);
        dropOnDeath = config.getBoolean("settings.drop-on-death", true);
        cooldownTime = config.getInt("settings.cooldown-seconds", 30);
        spinDuration = config.getInt("settings.spin-duration-seconds", 15);
        particlesEnabled = config.getBoolean("settings.particles-enabled", true);
        soundsEnabled = config.getBoolean("settings.sounds-enabled", true);
    }
    
    public void saveSettings() {
        FileConfiguration config = plugin.getConfig();
        config.set("settings.first-join-reward", firstJoinReward);
        config.set("settings.drop-on-death", dropOnDeath);
        config.set("settings.cooldown-seconds", cooldownTime);
        config.set("settings.spin-duration-seconds", spinDuration);
        config.set("settings.particles-enabled", particlesEnabled);
        config.set("settings.sounds-enabled", soundsEnabled);
        plugin.saveConfig();
    }
    
    public void loadResetData() {
        FileConfiguration config = plugin.getConfig();
        List<String> resetUUIDs = config.getStringList("reset-players");
        resetPlayers.clear();
        for (String uuid : resetUUIDs) {
            try {
                resetPlayers.add(UUID.fromString(uuid));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in config: " + uuid);
            }
        }
    }
    
    public void saveResetData() {
        FileConfiguration config = plugin.getConfig();
        List<String> resetUUIDs = new ArrayList<>();
        for (UUID uuid : resetPlayers) {
            resetUUIDs.add(uuid.toString());
        }
        config.set("reset-players", resetUUIDs);
        plugin.saveConfig();
    }
    
    public void resetPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && FruitType.fromItem(item) != null) {
                player.getInventory().remove(item);
            }
        }
        
        resetPlayers.add(uuid);
        saveResetData();
        
        player.sendMessage("§c§l⚠ §fYour magical fruit data has been reset by an admin!");
        player.sendMessage("§aYou will receive a new fruit spin on your next join!");
    }
    
    public void resetAllPlayersData() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            resetPlayerData(player);
        }
        
        resetPlayers.clear();
        saveResetData();
        
        plugin.getServer().broadcastMessage("§c§l⚠ §6MagicFruits §f- All player data has been reset by an admin!");
        plugin.getServer().broadcastMessage("§aAll players will receive a new fruit spin on their next join!");
    }
    
    public boolean isPlayerReset(UUID uuid) {
        return resetPlayers.contains(uuid);
    }
    
    public void removeResetPlayer(UUID uuid) {
        resetPlayers.remove(uuid);
        saveResetData();
    }
    
    public int getResetPlayersCount() {
        return resetPlayers.size();
    }
    
    public boolean isFirstJoinReward() { return firstJoinReward; }
    public void setFirstJoinReward(boolean value) { this.firstJoinReward = value; saveSettings(); }
    
    public boolean isDropOnDeath() { return dropOnDeath; }
    public void setDropOnDeath(boolean value) { this.dropOnDeath = value; saveSettings(); }
    
    public int getCooldownTime() { return cooldownTime; }
    public void setCooldownTime(int value) { this.cooldownTime = value; saveSettings(); }
    
    public int getSpinDuration() { return spinDuration; }
    public void setSpinDuration(int value) { this.spinDuration = value; saveSettings(); }
    
    public boolean isParticlesEnabled() { return particlesEnabled; }
    public void setParticlesEnabled(boolean value) { this.particlesEnabled = value; saveSettings(); }
    
    public boolean isSoundsEnabled() { return soundsEnabled; }
    public void setSoundsEnabled(boolean value) { this.soundsEnabled = value; saveSettings(); }
}

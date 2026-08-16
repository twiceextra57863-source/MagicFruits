package com.example.magicfruits;

import com.example.magicfruits.abilities.Ability;
import com.example.magicfruits.gui.AdminGUI;
import com.example.magicfruits.managers.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MagicFruits extends JavaPlugin implements Listener {
    
    private static MagicFruits instance;
    private DataManager dataManager;
    private CooldownManager cooldownManager;
    private SpinManager spinManager;
    private AdminGUI adminGUI;
    private CommandHandler commandHandler;
    private GracePeriodManager gracePeriodManager;
    private Map<UUID, Ability> stolenAbilities = new HashMap<>();
    private Map<UUID, Long> stolenAbilityExpiry = new HashMap<>();
    
    // MagicWand region protection system
    private Map<UUID, Location> wandFirstSelection = new HashMap<>();
    private Map<UUID, Location> wandSecondSelection = new HashMap<>();
    private Set<String> protectedRegions = new HashSet<>(); // Store region IDs as "x1,y1,z1-x2,y2,z2"
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        dataManager = new DataManager(this);
        cooldownManager = new CooldownManager(this);
        spinManager = new SpinManager(this);
        adminGUI = new AdminGUI(this);
        commandHandler = new CommandHandler(this);
        gracePeriodManager = new GracePeriodManager(this);
        
        // Load data
        dataManager.loadSettings();
        dataManager.loadResetData();
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(adminGUI, this);
        getServer().getPluginManager().registerEvents(new com.example.magicfruits.gui.StealGUI(this), this);
        getServer().getPluginManager().registerEvents(new com.example.magicfruits.gui.FruitMenuGUI(this), this);
        
        // Register commands
        getCommand("magicfruits").setExecutor(commandHandler);
        getCommand("magicfruits").setTabCompleter(commandHandler);
        getCommand("magicwand").setExecutor(commandHandler);
        getCommand("magicwand").setTabCompleter(commandHandler);
        
        // Start cleanup tasks
        cooldownManager.startCleanupTask();
        startStolenAbilityCleanup();
        
        getLogger().info("§aMagicFruits plugin has been enabled!");
        getLogger().info("§eFirst Join Reward: " + (dataManager.isFirstJoinReward() ? "§aENABLED" : "§cDISABLED"));
        getLogger().info("§eDrop on Death: " + (dataManager.isDropOnDeath() ? "§aENABLED" : "§cDISABLED"));
        getLogger().info("§dAll " + FruitType.values().length + " fruits loaded successfully!");
    }
    
    @Override
    public void onDisable() {
        dataManager.saveSettings();
        dataManager.saveResetData();
        getLogger().info("§cMagicFruits plugin has been disabled!");
    }
    
    private void startStolenAbilityCleanup() {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                stolenAbilityExpiry.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
                stolenAbilities.keySet().removeIf(uuid -> !stolenAbilityExpiry.containsKey(uuid));
            }
        }.runTaskTimer(this, 1200L, 1200L);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if grace period is active
        if (gracePeriodManager != null && gracePeriodManager.shouldPreventFirstJoinReward()) {
            player.sendMessage("§e§l⚠ §fGrace period is active! You will not receive a fruit on first join.");
            return;
        }
        
        spinManager.handlePlayerJoin(player);
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!dataManager.isDropOnDeath()) return;
        
        Player player = event.getEntity();
        
        // Check if grace period protects this player
        if (gracePeriodManager != null && gracePeriodManager.shouldPreventDeathDrop(player)) {
            event.getDrops().removeIf(item -> FruitType.fromItem(item) != null);
            player.sendMessage("§a§l🛡️ §fYour fruits were protected during grace period!");
            return;
        }
        
        // Normal death handling
        for (ItemStack item : event.getDrops()) {
            if (FruitType.fromItem(item) != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        event.getDrops().removeIf(item -> FruitType.fromItem(item) != null);
        player.sendMessage("§c§l💀 §fYour magical fruits have been dropped on death!");
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check for Magic Wand clicks
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName != null && displayName.contains("MAGIC WAND")) {
                if (!player.hasPermission("magicfruits.admin")) {
                    return;
                }
                
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    setWandFirstSelection(player.getUniqueId(), player.getLocation());
                    player.sendMessage("§d§l✨ §fFirst selection set at §e" + 
                        player.getLocation().getBlockX() + ", " + 
                        player.getLocation().getBlockY() + ", " + 
                        player.getLocation().getBlockZ());
                    return;
                }
                
                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    setWandSecondSelection(player.getUniqueId(), player.getLocation());
                    player.sendMessage("§d§l✨ §fSecond selection set at §e" + 
                        player.getLocation().getBlockX() + ", " + 
                        player.getLocation().getBlockY() + ", " + 
                        player.getLocation().getBlockZ());
                    
                    if (hasCompleteSelection(player.getUniqueId())) {
                        Location loc1 = getWandFirstSelection(player.getUniqueId());
                        Location loc2 = getWandSecondSelection(player.getUniqueId());
                        if (loc1 != null && loc2 != null && loc1.getWorld().equals(loc2.getWorld())) {
                            player.sendMessage("§a§l✓ §fSelection complete! Use §e/magicwand protect §for §e/magicwand break");
                        }
                    }
                    return;
                }
            }
        }
        
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            FruitType fruit = FruitType.fromItem(item);
            
            if (fruit != null) {
                // Check if location is protected
                if (isLocationProtected(player.getLocation())) {
                    player.sendMessage("§c§l⚠ §fFruits are disabled in this protected area!");
                    event.setCancelled(true);
                    return;
                }
                
                event.setCancelled(true);
                
                // Check stolen ability
                if (stolenAbilities.containsKey(player.getUniqueId())) {
                    Ability stolen = stolenAbilities.get(player.getUniqueId());
                    if (stolenAbilityExpiry.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) {
                        stolen.execute(player, player.isSneaking());
                        return;
                    } else {
                        stolenAbilities.remove(player.getUniqueId());
                        stolenAbilityExpiry.remove(player.getUniqueId());
                    }
                }
                
                // Determine which ability is being used
                String abilityType = player.isSneaking() ? "secondary" : "primary";
                
                // Check cooldown for this specific ability
                if (cooldownManager.isOnCooldown(player.getUniqueId(), fruit, abilityType)) {
                    cooldownManager.showCooldownMessage(player, fruit, abilityType);
                    cooldownManager.showCooldownOnXPBar(player, 
                        cooldownManager.getRemainingSeconds(player.getUniqueId(), fruit, abilityType), fruit, abilityType);
                    return;
                }
                
                // Execute ability
                fruit.getAbility().execute(player, player.isSneaking());
                cooldownManager.startCooldown(player.getUniqueId(), fruit, abilityType);
            }
        }
    }
    
    public void setStolenAbility(UUID playerId, Ability ability, int durationSeconds) {
        stolenAbilities.put(playerId, ability);
        stolenAbilityExpiry.put(playerId, System.currentTimeMillis() + (durationSeconds * 1000L));
    }
    
    public void removeStolenAbility(UUID playerId) {
        stolenAbilities.remove(playerId);
        stolenAbilityExpiry.remove(playerId);
    }
    
    // MagicWand region protection methods
    public Location getWandFirstSelection(UUID playerId) {
        return wandFirstSelection.get(playerId);
    }
    
    public void setWandFirstSelection(UUID playerId, Location loc) {
        wandFirstSelection.put(playerId, loc);
    }
    
    public Location getWandSecondSelection(UUID playerId) {
        return wandSecondSelection.get(playerId);
    }
    
    public void setWandSecondSelection(UUID playerId, Location loc) {
        wandSecondSelection.put(playerId, loc);
    }
    
    public void clearWandSelections(UUID playerId) {
        wandFirstSelection.remove(playerId);
        wandSecondSelection.remove(playerId);
    }
    
    public boolean hasCompleteSelection(UUID playerId) {
        return wandFirstSelection.containsKey(playerId) && wandSecondSelection.containsKey(playerId);
    }
    
    public String createRegionKey(Location loc1, Location loc2) {
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        return x1 + "," + y1 + "," + z1 + "-" + x2 + "," + y2 + "," + z2;
    }
    
    public void protectRegion(Location loc1, Location loc2) {
        String regionKey = createRegionKey(loc1, loc2);
        protectedRegions.add(regionKey);
    }
    
    public void unprotectRegion(Location loc1, Location loc2) {
        String regionKey = createRegionKey(loc1, loc2);
        protectedRegions.remove(regionKey);
    }
    
    public boolean isLocationProtected(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        for (String region : protectedRegions) {
            String[] parts = region.split("-");
            String[] min = parts[0].split(",");
            String[] max = parts[1].split(",");
            
            int x1 = Integer.parseInt(min[0]);
            int y1 = Integer.parseInt(min[1]);
            int z1 = Integer.parseInt(min[2]);
            int x2 = Integer.parseInt(max[0]);
            int y2 = Integer.parseInt(max[1]);
            int z2 = Integer.parseInt(max[2]);
            
            if (x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2) {
                return true;
            }
        }
        return false;
    }
    
    public DataManager getDataManager() { return dataManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public SpinManager getSpinManager() { return spinManager; }
    public AdminGUI getAdminGUI() { return adminGUI; }
    public GracePeriodManager getGracePeriodManager() { return gracePeriodManager; }
    public static MagicFruits getInstance() { return instance; }
}

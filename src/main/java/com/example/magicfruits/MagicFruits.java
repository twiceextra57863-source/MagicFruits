package com.example.magicfruits;

import com.example.magicfruits.abilities.Ability;
import com.example.magicfruits.gui.AdminGUI;
import com.example.magicfruits.managers.*;
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
import java.util.Map;
import java.util.UUID;

public final class MagicFruits extends JavaPlugin implements Listener {
    
    private static MagicFruits instance;
    private DataManager dataManager;
    private CooldownManager cooldownManager;
    private SpinManager spinManager;
    private AdminGUI adminGUI;
    private CommandHandler commandHandler;
    private Map<UUID, Ability> stolenAbilities = new HashMap<>();
    private Map<UUID, Long> stolenAbilityExpiry = new HashMap<>();
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        dataManager = new DataManager(this);
        cooldownManager = new CooldownManager(this);
        spinManager = new SpinManager(this);
        adminGUI = new AdminGUI(this);
        commandHandler = new CommandHandler(this);
        
        // Load data
        dataManager.loadSettings();
        dataManager.loadResetData();
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(adminGUI, this);
        getServer().getPluginManager().registerEvents(new com.example.magicfruits.gui.StealGUI(this), this);
        getServer().getPluginManager().registerEvents(new com.example.magicfruits.gui.FruitMenuGUI(this), this);
        
        // Register PortalAbility events
        getServer().getPluginManager().registerEvents(new PortalAbility(), this);
        
        // Register commands
        getCommand("magicfruits").setExecutor(commandHandler);
        getCommand("magicfruits").setTabCompleter(commandHandler);
        
        // Start cleanup tasks
        cooldownManager.startCleanupTask();
        startStolenAbilityCleanup();
        
        getLogger().info("§aMagicFruits plugin has been enabled!");
        getLogger().info("§eFirst Join Reward: " + (dataManager.isFirstJoinReward() ? "§aENABLED" : "§cDISABLED"));
        getLogger().info("§eDrop on Death: " + (dataManager.isDropOnDeath() ? "§aENABLED" : "§cDISABLED"));
        getLogger().info("§dPortal Fruit added - Master of dimensions!");
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
        spinManager.handlePlayerJoin(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!dataManager.isDropOnDeath()) return;
        
        Player player = event.getEntity();
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
        
        // Right click check (air or block)
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            FruitType fruit = FruitType.fromItem(item);
            
            if (fruit != null) {
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
                
                // Check cooldown for this specific fruit
                if (cooldownManager.isOnCooldown(player.getUniqueId(), fruit)) {
                    cooldownManager.showCooldownMessage(player, fruit);
                    cooldownManager.showCooldownOnXPBar(player, 
                        cooldownManager.getRemainingSeconds(player.getUniqueId(), fruit), fruit);
                    return;
                }
                
                // Execute ability
                Ability ability = fruit.getAbility();
                ability.execute(player, player.isSneaking());
                cooldownManager.startCooldown(player.getUniqueId(), fruit);
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
    
    public DataManager getDataManager() { return dataManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public SpinManager getSpinManager() { return spinManager; }
    public AdminGUI getAdminGUI() { return adminGUI; }
    public static MagicFruits getInstance() { return instance; }
}

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
    private GracePeriodManager gracePeriodManager;
    private ProtectionManager protectionManager;
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
        gracePeriodManager = new GracePeriodManager(this);
        protectionManager = new ProtectionManager(this);
        
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
        if (getCommand("magicwand") != null) {
            getCommand("magicwand").setExecutor(commandHandler);
            getCommand("magicwand").setTabCompleter(commandHandler);
        }
        
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
        Player player = event.getEntity();
        
        // Check if grace period protects this player
        if (gracePeriodManager != null && gracePeriodManager.shouldPreventDeathDrop(player)) {
            for (ItemStack item : new java.util.ArrayList<>(event.getDrops())) {
                if (FruitType.fromItem(item) != null) {
                    event.getDrops().remove(item);
                    event.getItemsToKeep().add(item);
                }
            }
            player.sendMessage("§a§l🛡️ §fYour fruits were protected during grace period!");
            return;
        }

        // Global drop-on-death check
        if (!dataManager.isDropOnDeath()) {
            for (ItemStack item : new java.util.ArrayList<>(event.getDrops())) {
                if (FruitType.fromItem(item) != null) {
                    event.getDrops().remove(item);
                    event.getItemsToKeep().add(item);
                }
            }
            return;
        }

        boolean isPvP = (player.getKiller() != null);
        String mode = dataManager.getDeathDropMode();
        boolean shouldDrop = false;

        switch (mode.toUpperCase()) {
            case "BOTH":
            case "ALL":
                shouldDrop = true;
                break;
            case "PVP_ONLY":
                shouldDrop = isPvP;
                break;
            case "NATURAL_ONLY":
                shouldDrop = !isPvP;
                break;
            case "NEVER":
            case "NONE":
                shouldDrop = false;
                break;
            default:
                shouldDrop = isPvP;
                break;
        }

        if (shouldDrop) {
            for (ItemStack item : new java.util.ArrayList<>(event.getDrops())) {
                if (FruitType.fromItem(item) != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    event.getDrops().remove(item);
                }
            }
            player.sendMessage("§c§l💀 §fYour magical fruits have been dropped on death!");
        } else {
            // Protect fruits on death by keeping them in inventory
            for (ItemStack item : new java.util.ArrayList<>(event.getDrops())) {
                if (FruitType.fromItem(item) != null) {
                    event.getDrops().remove(item);
                    event.getItemsToKeep().add(item);
                }
            }
            player.sendMessage("§a§l🛡️ §fYour magical fruits were protected on death!");
        }
    }

    @EventHandler
    public void onPlayerDropItem(org.bukkit.event.player.PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (FruitType.fromItem(item) != null && !dataManager.isAllowDrop()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c§l⚠ §fDropping magical fruits is DISABLED in server config!");
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (dataManager.isAllowStorage()) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean currentIsFruit = (FruitType.fromItem(current) != null);
        boolean cursorIsFruit = (FruitType.fromItem(cursor) != null);

        if (!currentIsFruit && !cursorIsFruit) return;

        org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        // If clicking inside a non-player inventory (e.g. Chest, Barrel, EnderChest, Hopper, Shulker, etc.)
        if (clickedInv.getType() != org.bukkit.event.inventory.InventoryType.PLAYER) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                ((Player) event.getWhoClicked()).sendMessage("§c§l⚠ §fStoring magical fruits in containers is DISABLED in server config!");
            }
        } else if (event.isShiftClick() && currentIsFruit) {
            // Shift-clicking from player inventory into a top open container inventory
            if (event.getInventory().getType() != org.bukkit.event.inventory.InventoryType.PLAYER) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    ((Player) event.getWhoClicked()).sendMessage("§c§l⚠ §fStoring magical fruits in containers is DISABLED in server config!");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            FruitType fruit = FruitType.fromItem(item);
            
            if (fruit != null) {
                event.setCancelled(true);

                // Check region protection
                if (protectionManager != null && protectionManager.isLocationProtected(player.getLocation())) {
                    player.sendMessage("§c§l⚠ §fMagical fruits are BANNED in this protected region!");
                    return;
                }
                
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
    
    public DataManager getDataManager() { return dataManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public SpinManager getSpinManager() { return spinManager; }
    public AdminGUI getAdminGUI() { return adminGUI; }
    public GracePeriodManager getGracePeriodManager() { return gracePeriodManager; }
    public ProtectionManager getProtectionManager() { return protectionManager; }
    public static MagicFruits getInstance() { return instance; }
}

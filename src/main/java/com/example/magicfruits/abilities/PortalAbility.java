package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PortalAbility implements Ability, Listener {
    
    private final Map<UUID, PortalData> activePortals = new HashMap<>();
    private final Map<UUID, Long> summonCooldown = new HashMap<>();
    private final Map<UUID, PortalCreationData> creatingPortal = new HashMap<>();
    
    private static class PortalData {
        Location portal1;
        Location portal2;
        long expiryTime;
        
        PortalData(Location p1, Location p2, long expiry) {
            this.portal1 = p1;
            this.portal2 = p2;
            this.expiryTime = expiry;
        }
    }
    
    private static class PortalCreationData {
        Location firstPortal;
        long creationTime;
        
        PortalCreationData(Location loc, long time) {
            this.firstPortal = loc;
            this.creationTime = time;
        }
    }
    
    public PortalAbility() {
        // Start cleanup task for expired portals
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activePortals.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 1200L, 1200L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary Ability: Summon portal
            executeSummonPortal(player, plugin);
        } else {
            // Primary Ability: Teleport portal
            executeTeleportPortal(player, plugin);
        }
    }
    
    private void executeTeleportPortal(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check if already creating portal
        if (creatingPortal.containsKey(uuid)) {
            PortalCreationData data = creatingPortal.get(uuid);
            
            // Check if 20 seconds passed
            if (System.currentTimeMillis() - data.creationTime > 20000) {
                player.sendMessage("§c§l⚠ §fTime expired! Start again!");
                creatingPortal.remove(uuid);
                return;
            }
            
            // Create second portal at target block
            Block targetBlock = player.getTargetBlock(null, 50);
            if (targetBlock == null) {
                player.sendMessage("§c§l⚠ §fNo block in sight!");
                return;
            }
            Location secondPortal = targetBlock.getLocation().add(0.5, 1, 0.5);
            
            // Create portal visuals
            createPortalVisual(data.firstPortal);
            createPortalVisual(secondPortal);
            
            // Store portals
            activePortals.put(uuid, new PortalData(data.firstPortal, secondPortal, 
                System.currentTimeMillis() + 60000));
            
            player.sendTitle("§5§l✨ PORTALS CONNECTED! ✨", 
                "§eYou can now teleport between them!", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
            player.sendMessage("§5§l🔮 §fPortals connected! They will last for 60 seconds!");
            
            creatingPortal.remove(uuid);
            return;
        }
        
        // Create first portal at target block
        Block targetBlock = player.getTargetBlock(null, 50);
        if (targetBlock == null) {
            player.sendMessage("§c§l⚠ §fNo block in sight!");
            return;
        }
        Location firstPortal = targetBlock.getLocation().add(0.5, 1, 0.5);
        createPortalVisual(firstPortal);
        
        creatingPortal.put(uuid, new PortalCreationData(firstPortal, System.currentTimeMillis()));
        player.sendTitle("§5§l🔮 FIRST PORTAL PLACED!", 
            "§ePlace second portal within 20 seconds!", 10, 40, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
        player.sendMessage("§5§l🔮 §fFirst portal placed! You have 20 seconds to place the second portal!");
    }
    
    private void executeSummonPortal(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (20 minutes)
        long lastUse = summonCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 1200000) {
            long remaining = (1200000 - (System.currentTimeMillis() - lastUse)) / 1000;
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            player.sendMessage("§c§l⚠ §fSummon portal on cooldown! §7(" + minutes + "m " + seconds + "s remaining)");
            return;
        }
        
        // Get target block where player is looking
        Block targetBlock = player.getTargetBlock(null, 50);
        if (targetBlock == null) {
            player.sendMessage("§c§l⚠ §fNo block in sight!");
            return;
        }
        
        Location portalLoc = targetBlock.getLocation().add(0.5, 1, 0.5);
        
        // Create summon portal visual
        createSummonPortalVisual(portalLoc);
        
        // Store portal for this player
        activePortals.put(uuid, new PortalData(portalLoc, null, System.currentTimeMillis() + 120000));
        
        summonCooldown.put(uuid, System.currentTimeMillis());
        
        player.sendTitle("§5§l🌀 SUMMON PORTAL CREATED!", 
            "§eLeft click the portal to summon any player!", 10, 40, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);
        player.sendMessage("§5§l🌀 §fSummon portal created! Click it to summon players!");
        player.sendMessage("§eYou have 2 minutes to use this portal!");
        
        // Register click listener for this portal
        new PortalClickListener(portalLoc, player);
    }
    
    private void createPortalVisual(Location loc) {
        World world = loc.getWorld();
        
        // Particle effects
        for (int i = 0; i < 100; i++) {
            double radius = 1.5;
            double angle = Math.random() * 2 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(Particle.PORTAL, loc.clone().add(x, Math.random() * 2, z), 0, 0, 0, 0, 1);
        }
        
        // End rod beam
        for (int y = 0; y < 3; y++) {
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 5, 0.2, 0.2, 0.2, 0.1);
        }
        
        world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
    }
    
    private void createSummonPortalVisual(Location loc) {
        World world = loc.getWorld();
        
        // Circle of particles
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double radius = 2;
            double x = Math.cos(rad) * radius;
            double z = Math.sin(rad) * radius;
            world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(x, 0, z), 0, 0, 0, 0, 1);
            world.spawnParticle(Particle.PORTAL, loc.clone().add(x, 1, z), 0, 0, 0, 0, 1);
            world.spawnParticle(Particle.END_ROD, loc.clone().add(x, 2, z), 0, 0, 0, 0, 1);
        }
        
        // Center beam
        for (int y = 0; y < 4; y++) {
            world.spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, y, 0), 10, 0.2, 0.2, 0.2, 0.1);
        }
        
        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        
        // Check if player moved into a portal
        for (Map.Entry<UUID, PortalData> entry : activePortals.entrySet()) {
            PortalData data = entry.getValue();
            if (data.portal1 != null && data.portal2 != null) {
                if (to.distance(data.portal1) < 1.5 && from.distance(data.portal1) >= 1.5) {
                    player.teleport(data.portal2);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    player.spawnParticle(Particle.PORTAL, data.portal2, 50, 0.5, 1, 0.5, 0.1);
                    break;
                } else if (to.distance(data.portal2) < 1.5 && from.distance(data.portal2) >= 1.5) {
                    player.teleport(data.portal1);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    player.spawnParticle(Particle.PORTAL, data.portal1, 50, 0.5, 1, 0.5, 0.1);
                    break;
                }
            }
        }
    }
    
    private class PortalClickListener implements Listener {
        private final Location portalLoc;
        private final Player owner;
        
        PortalClickListener(Location loc, Player owner) {
            this.portalLoc = loc;
            this.owner = owner;
            MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
            
            // Auto unregister after 2 minutes
            new BukkitRunnable() {
                @Override
                public void run() {
                    org.bukkit.event.HandlerList.unregisterAll(PortalClickListener.this);
                }
            }.runTaskLater(MagicFruits.getInstance(), 2400L);
        }
        
        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            if (event.getClickedBlock() == null) return;
            
            Location clickedLoc = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);
            if (clickedLoc.distance(portalLoc) < 1.5 && event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
                
                if (!player.equals(owner)) {
                    player.sendMessage("§c§l⚠ §fThis portal belongs to " + owner.getName() + "!");
                    return;
                }
                
                // Open GUI to select player
                Inventory gui = Bukkit.createInventory(null, 54, "§5§l🌀 SUMMON PLAYER §5§l🌀");
                
                int slot = 19;
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (target.equals(owner)) continue;
                    
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(target);
                    meta.setDisplayName("§e§l" + target.getName());
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Click to summon §e" + target.getName());
                    lore.add("§7to your location!");
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                    gui.setItem(slot, head);
                    slot++;
                    if ((slot + 1) % 9 == 0) slot += 2;
                    if (slot > 43) break;
                }
                
                owner.openInventory(gui);
                
                // Handle GUI click
                new SummonGUIHandler(owner, portalLoc);
            }
        }
    }
    
    private class SummonGUIHandler implements Listener {
        private final Player owner;
        private final Location portalLoc;
        
        SummonGUIHandler(Player owner, Location portalLoc) {
            this.owner = owner;
            this.portalLoc = portalLoc;
            MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        }
        
        @EventHandler
        public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            if (!event.getView().getTitle().equals("§5§l🌀 SUMMON PLAYER §5§l🌀")) return;
            if (!event.getWhoClicked().equals(owner)) return;
            
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
            
            String targetName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(targetName);
            
            if (target != null) {
                target.teleport(owner.getLocation());
                target.sendTitle("§5§l🌀 SUMMONED! §5§l🌀", 
                    "§eYou have been summoned by " + owner.getName(), 10, 40, 10);
                target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                target.spawnParticle(Particle.PORTAL, target.getLocation(), 100, 1, 1, 1, 0.5);
                
                owner.sendMessage("§a§l✓ §fYou have summoned §e" + target.getName());
                owner.playSound(owner.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                
                // Remove portal after use
                activePortals.remove(owner.getUniqueId());
                owner.closeInventory();
                
                // Unregister handlers
                org.bukkit.event.HandlerList.unregisterAll(this);
            }
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Create teleport portals (2 portals, 20s to connect, 60s duration)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Create summon portal (20 min cooldown, click to summon any player)";
    }
                }

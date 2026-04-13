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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PortalAbility implements Ability, Listener {
    
    private final Map<UUID, PortalCreationData> creatingPortal = new ConcurrentHashMap<>();
    private final Map<UUID, PortalData> activePortals = new ConcurrentHashMap<>();
    private final Map<UUID, SummonPortalData> activeSummonPortals = new ConcurrentHashMap<>();
    private final Map<UUID, Long> summonCooldown = new ConcurrentHashMap<>();
    
    private static class PortalCreationData {
        Location firstPortal;
        long creationTime;
        boolean isWaiting;
        int taskId;
        
        PortalCreationData(Location loc, long time) {
            this.firstPortal = loc;
            this.creationTime = time;
            this.isWaiting = true;
            this.taskId = -1;
        }
    }
    
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
    
    private static class SummonPortalData {
        Location portalLocation;
        Player owner;
        long expiryTime;
        
        SummonPortalData(Location loc, Player owner, long expiry) {
            this.portalLocation = loc;
            this.owner = owner;
            this.expiryTime = expiry;
        }
    }
    
    public PortalAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activePortals.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                activeSummonPortals.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeSummonPortal(player, plugin);
        } else {
            executeTeleportPortal(player, plugin);
        }
    }
    
    private void executeTeleportPortal(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Case 1: Player is waiting to place second portal
        if (creatingPortal.containsKey(uuid) && creatingPortal.get(uuid).isWaiting) {
            PortalCreationData data = creatingPortal.get(uuid);
            
            // Check if 15 seconds have passed
            if (System.currentTimeMillis() - data.creationTime > 15000) {
                creatingPortal.remove(uuid);
                player.sendMessage("§c§l⚠ §fTime expired! Start again.");
                return;
            }
            
            // Get second portal location
            Block targetBlock = player.getTargetBlock(null, 30);
            if (targetBlock == null) {
                player.sendMessage("§c§l⚠ §fNo block in sight!");
                return;
            }
            
            Location secondPortal = targetBlock.getLocation().add(0.5, 1, 0.5);
            
            // Create portal effects
            createPortalRings(data.firstPortal, plugin);
            createPortalRings(secondPortal, plugin);
            
            // Store connected portals
            activePortals.put(uuid, new PortalData(data.firstPortal, secondPortal, 
                System.currentTimeMillis() + 60000));
            
            // Remove creation data
            creatingPortal.remove(uuid);
            
            // Play sounds
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.getWorld().playSound(data.firstPortal, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
                player.getWorld().playSound(secondPortal, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
            }
            
            player.sendTitle("§5§l✨ PORTALS CONNECTED! ✨", 
                "§eWalk through to teleport!", 10, 50, 10);
            player.sendMessage("§5§l🔮 §fPortals connected! They will last for 60 seconds!");
            return;
        }
        
        // Case 2: First portal placement - NO COOLDOWN CHECK
        Block targetBlock = player.getTargetBlock(null, 30);
        if (targetBlock == null) {
            player.sendMessage("§c§l⚠ §fNo block in sight!");
            return;
        }
        
        Location firstPortal = targetBlock.getLocation().add(0.5, 1, 0.5);
        
        // Create portal effect
        createPortalRings(firstPortal, plugin);
        
        // Store creation data
        PortalCreationData data = new PortalCreationData(firstPortal, System.currentTimeMillis());
        creatingPortal.put(uuid, data);
        
        // Start countdown timer
        data.taskId = new BukkitRunnable() {
            int seconds = 15;
            
            @Override
            public void run() {
                PortalCreationData current = creatingPortal.get(uuid);
                if (current == null || !current.isWaiting) {
                    this.cancel();
                    return;
                }
                
                if (seconds <= 0) {
                    current.isWaiting = false;
                    creatingPortal.remove(uuid);
                    player.sendMessage("§c§l⚠ §fTime expired! Portal creation cancelled.");
                    player.sendTitle("§c§l⚠ TIME EXPIRED! ⚠", 
                        "§eYou took too long!", 10, 30, 10);
                    this.cancel();
                    return;
                }
                
                player.sendActionBar("§5§l⏳ Place second portal: §e" + seconds + "s remaining");
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        
        // Play sound
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(firstPortal, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
        }
        
        player.sendTitle("§5§l🔮 FIRST PORTAL PLACED! 🔮", 
            "§eRight click again within 15 seconds!", 10, 40, 10);
        player.sendMessage("§5§l🔮 §fFirst portal placed! You have 15 seconds to place the second!");
    }
    
    private void createPortalRings(Location center, MagicFruits plugin) {
        World world = center.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 60) {
                    this.cancel();
                    return;
                }
                
                double radius = 1.8;
                for (int i = 0; i < 360; i += 15) {
                    double theta = Math.toRadians(i + ticks * 12);
                    double x = Math.cos(theta) * radius;
                    double z = Math.sin(theta) * radius;
                    double y = 1.0 + Math.sin(theta * 2) * 0.3;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFF6600), 0.7f));
                        world.spawnParticle(Particle.PORTAL, center.clone().add(x, y + 0.2, z), 0, 0, 0, 0, 1);
                    }
                }
                
                double radius2 = 1.2;
                for (int i = 0; i < 360; i += 15) {
                    double theta = Math.toRadians(i - ticks * 10);
                    double x = Math.cos(theta) * radius2;
                    double z = Math.sin(theta) * radius2;
                    double y = 1.2 + Math.cos(theta * 2) * 0.2;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.END_ROD, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;
        
        for (Map.Entry<UUID, PortalData> entry : activePortals.entrySet()) {
            PortalData data = entry.getValue();
            
            if (to.distance(data.portal1) < 1.2) {
                player.teleport(data.portal2);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                createTeleportFlash(data.portal2, MagicFruits.getInstance());
                break;
            } else if (to.distance(data.portal2) < 1.2) {
                player.teleport(data.portal1);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                createTeleportFlash(data.portal1, MagicFruits.getInstance());
                break;
            }
        }
    }
    
    private void createTeleportFlash(Location loc, MagicFruits plugin) {
        World world = loc.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 10) {
                    this.cancel();
                    return;
                }
                
                double radius = 1.5;
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i + ticks * 30);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.PORTAL, loc.clone().add(x, 0.5, z), 0, 0, 0, 0, 1);
                        world.spawnParticle(Particle.END_ROD, loc.clone().add(x, 1.2, z), 0, 0, 0, 0, 1);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void executeSummonPortal(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = summonCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 1200000) {
            long remaining = (1200000 - (System.currentTimeMillis() - lastUse)) / 1000;
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            player.sendMessage("§c§l⚠ §fSummon Portal on cooldown! §7(" + minutes + "m " + seconds + "s remaining)");
            return;
        }
        
        Block targetBlock = player.getTargetBlock(null, 30);
        if (targetBlock == null) {
            player.sendMessage("§c§l⚠ §fNo block in sight!");
            return;
        }
        
        Location portalLoc = targetBlock.getLocation().add(0.5, 1, 0.5);
        createSummonPortalVortex(portalLoc, plugin);
        
        activeSummonPortals.put(uuid, new SummonPortalData(portalLoc, player, System.currentTimeMillis() + 120000));
        summonCooldown.put(uuid, System.currentTimeMillis());
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(portalLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);
            player.getWorld().playSound(portalLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);
        }
        
        player.sendTitle("§5§l🌀 SUMMON PORTAL! 🌀", "§eLeft click to summon any player!", 10, 40, 10);
        player.sendMessage("§5§l🌀 §fSummon portal created for 2 minutes!");
    }
    
    private void createSummonPortalVortex(Location center, MagicFruits plugin) {
        World world = center.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 100) {
                    this.cancel();
                    return;
                }
                
                double radius = 2.0;
                for (int i = 0; i < 360; i += 15) {
                    double theta = Math.toRadians(i + ticks * 8);
                    double x = Math.cos(theta) * radius;
                    double z = Math.sin(theta) * radius;
                    double y = 1.0 + (Math.sin(theta * 2) * 0.3) + (ticks * 0.02);
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x6600CC), 0.8f));
                        world.spawnParticle(Particle.PORTAL, center.clone().add(x, y + 0.2, z), 0, 0, 0, 0, 1);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        
        if (clickedBlock == null) return;
        Location clickedLoc = clickedBlock.getLocation().add(0.5, 1, 0.5);
        
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            for (Map.Entry<UUID, SummonPortalData> entry : activeSummonPortals.entrySet()) {
                SummonPortalData data = entry.getValue();
                if (clickedLoc.distance(data.portalLocation) < 1.5 && data.owner.equals(player)) {
                    event.setCancelled(true);
                    openSummonGUI(player);
                    break;
                }
            }
        }
    }
    
    private void openSummonGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§5§l🌀 SUMMON PLAYER §5§l🌀");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        int slot = 19;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;
            
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName("§e§l" + target.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§8§m-----------------------------");
            lore.add("§7Click to summon §e" + target.getName());
            lore.add("§7to your location!");
            lore.add("§8§m-----------------------------");
            meta.setLore(lore);
            
            head.setItemMeta(meta);
            gui.setItem(slot, head);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§5§l🌀 SUMMON PLAYER §5§l🌀")) return;
        
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
        
        String targetName = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        Player target = Bukkit.getServer().getPlayer(targetName);
        
        if (target != null) {
            MagicFruits plugin = MagicFruits.getInstance();
            Location summonLoc = player.getLocation();
            
            target.teleport(summonLoc);
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 100, 1, 1, 1, 0.5);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
            
            target.sendTitle("§5§l🌀 SUMMONED! 🌀", "§eYou were summoned by " + player.getName(), 10, 40, 10);
            player.sendMessage("§a§l✓ §fYou summoned §e" + target.getName());
            
            player.closeInventory();
            activeSummonPortals.remove(player.getUniqueId());
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Create teleport portals (2 portals, 15s to connect, 60s duration)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Create summon portal (20 min cooldown, left click to summon players)";
    }
             }

package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
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
    private final Map<UUID, Long> teleportCooldown = new ConcurrentHashMap<>();
    
    private static class PortalCreationData {
        Location firstPortal;
        long creationTime;
        
        PortalCreationData(Location loc, long time) {
            this.firstPortal = loc;
            this.creationTime = time;
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
        
        // Cleanup task for expired portals
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activePortals.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                activeSummonPortals.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                creatingPortal.entrySet().removeIf(entry -> entry.getValue().creationTime + 15000 <= now);
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
        
        // Check if player is already creating a portal (no cooldown on first click)
        if (creatingPortal.containsKey(uuid)) {
            PortalCreationData data = creatingPortal.get(uuid);
            
            // Check if 15 seconds have passed
            if (System.currentTimeMillis() - data.creationTime > 15000) {
                player.sendMessage("§c§l⚠ §fTime expired! Start again.");
                creatingPortal.remove(uuid);
                return;
            }
            
            // Get target block for second portal
            Block targetBlock = player.getTargetBlock(null, 30);
            if (targetBlock == null) {
                player.sendMessage("§c§l⚠ §fNo block in sight!");
                return;
            }
            
            Location secondPortal = targetBlock.getLocation().add(0.5, 1, 0.5);
            
            // Create full 3D portal rings with mathematical calculations
            createFullPortalRings(data.firstPortal, plugin);
            createFullPortalRings(secondPortal, plugin);
            
            // Store connected portals
            activePortals.put(uuid, new PortalData(data.firstPortal, secondPortal, 
                System.currentTimeMillis() + 60000));
            
            // Play epic sound
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.getWorld().playSound(data.firstPortal, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
                player.getWorld().playSound(secondPortal, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
            }
            
            player.sendTitle("§5§l✨ PORTALS CONNECTED! ✨", 
                "§eStep through to teleport between dimensions!", 10, 50, 10);
            player.sendMessage("§5§l🔮 §fPortals connected! They will last for 60 seconds!");
            
            creatingPortal.remove(uuid);
            return;
        }
        
        // First portal placement - NO COOLDOWN CHECK HERE
        Block targetBlock = player.getTargetBlock(null, 30);
        if (targetBlock == null) {
            player.sendMessage("§c§l⚠ §fNo block in sight!");
            return;
        }
        
        Location firstPortal = targetBlock.getLocation().add(0.5, 1, 0.5);
        
        // Create full 3D portal ring with mathematical spiral
        createPortalRingWithSpiral(firstPortal, plugin);
        
        // Store creation data
        creatingPortal.put(uuid, new PortalCreationData(firstPortal, System.currentTimeMillis()));
        
        // Play sound
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(firstPortal, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
        }
        
        player.sendTitle("§5§l🔮 FIRST PORTAL PLACED! 🔮", 
            "§ePlace second portal within 15 seconds!", 10, 40, 10);
        player.sendMessage("§5§l🔮 §fFirst portal placed! You have 15 seconds to place the second portal!");
        
        // Show countdown in action bar
        new BukkitRunnable() {
            int seconds = 15;
            
            @Override
            public void run() {
                if (!creatingPortal.containsKey(uuid)) {
                    this.cancel();
                    return;
                }
                
                if (seconds <= 0) {
                    creatingPortal.remove(uuid);
                    player.sendMessage("§c§l⚠ §fTime expired! Portal creation cancelled.");
                    this.cancel();
                    return;
                }
                
                player.sendActionBar("§5§l⏳ Time to place second portal: §e" + seconds + "s");
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void createPortalRingWithSpiral(Location center, MagicFruits plugin) {
        World world = center.getWorld();
        
        // Create a 3D spiral ring using mathematical calculations
        new BukkitRunnable() {
            int ticks = 0;
            double radius = 1.5;
            
            @Override
            public void run() {
                if (ticks >= 80) {
                    this.cancel();
                    return;
                }
                
                // Spiral ring - parametric equations
                for (int i = 0; i < 360; i += 8) {
                    double theta = Math.toRadians(i + ticks * 5);
                    double x = Math.cos(theta) * radius;
                    double z = Math.sin(theta) * radius;
                    
                    // Height variation using sine wave
                    double y = 1.0 + Math.sin(theta * 2) * 0.5;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        // Inner ring - purple portal particles
                        world.spawnParticle(Particle.PORTAL, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                        world.spawnParticle(Particle.END_ROD, center.clone().add(x, y + 0.3, z), 0, 0, 0, 0, 1);
                        
                        // Outer ring - golden dust
                        double xOuter = Math.cos(theta) * (radius + 0.3);
                        double zOuter = Math.sin(theta) * (radius + 0.3);
                        world.spawnParticle(Particle.DUST, center.clone().add(xOuter, y, zOuter), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFAA44), 0.6f));
                    }
                }
                
                // Add floating particles inside the portal
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = Math.random() * radius;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    double y = 1.0 + (Math.random() - 0.5) * 1.0;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.SPELL_WITCH, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void createFullPortalRings(Location center, MagicFruits plugin) {
        World world = center.getWorld();
        
        // Create full 3D portal with multiple rotating rings
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 100) {
                    this.cancel();
                    return;
                }
                
                // Ring 1 - Outer ring (rotating clockwise)
                double radius1 = 2.0;
                for (int i = 0; i < 360; i += 12) {
                    double theta = Math.toRadians(i + ticks * 8);
                    double x = Math.cos(theta) * radius1;
                    double z = Math.sin(theta) * radius1;
                    double y = 1.0 + Math.sin(theta * 2) * 0.3;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFF6600), 0.8f));
                        world.spawnParticle(Particle.FLAME, center.clone().add(x, y, z), 0, 0, 0, 0, 0.3);
                    }
                }
                
                // Ring 2 - Middle ring (rotating counter-clockwise)
                double radius2 = 1.5;
                for (int i = 0; i < 360; i += 10) {
                    double theta = Math.toRadians(i - ticks * 6);
                    double x = Math.cos(theta) * radius2;
                    double z = Math.sin(theta) * radius2;
                    double y = 1.2 + Math.cos(theta * 1.5) * 0.2;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFF3300), 0.7f));
                        world.spawnParticle(Particle.PORTAL, center.clone().add(x, y + 0.2, z), 0, 0, 0, 0, 1);
                    }
                }
                
                // Ring 3 - Inner ring (rotating opposite)
                double radius3 = 1.0;
                for (int i = 0; i < 360; i += 8) {
                    double theta = Math.toRadians(i + ticks * 10);
                    double x = Math.cos(theta) * radius3;
                    double z = Math.sin(theta) * radius3;
                    double y = 1.5 + Math.sin(theta * 3) * 0.2;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.END_ROD, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                        world.spawnParticle(Particle.DUST, center.clone().add(x, y - 0.2, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFAA44), 0.6f));
                    }
                }
                
                // Central vortex particles
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(ticks * 15 + i * 18);
                    double r = Math.sin(ticks * 0.1) * 0.8;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    double y = 1.0 + Math.sin(angle * 2) * 0.5;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.PORTAL, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void executeSummonPortal(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (20 minutes)
        long lastUse = summonCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 1200000) {
            long remaining = (1200000 - (System.currentTimeMillis() - lastUse)) / 1000;
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            player.sendMessage("§c§l⚠ §fSummon Portal on cooldown! §7(" + minutes + "m " + seconds + "s remaining)");
            return;
        }
        
        // Get target block where player is looking
        Block targetBlock = player.getTargetBlock(null, 30);
        if (targetBlock == null) {
            player.sendMessage("§c§l⚠ §fNo block in sight!");
            return;
        }
        
        Location portalLoc = targetBlock.getLocation().add(0.5, 1, 0.5);
        
        // Create full summon portal with mathematical vortex
        createSummonPortalVortex(portalLoc, plugin);
        
        // Store summon portal
        activeSummonPortals.put(uuid, new SummonPortalData(portalLoc, player, System.currentTimeMillis() + 120000));
        summonCooldown.put(uuid, System.currentTimeMillis());
        
        // Play sound
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(portalLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);
            player.getWorld().playSound(portalLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);
        }
        
        player.sendTitle("§5§l🌀 SUMMON PORTAL CREATED! 🌀", 
            "§eLeft click the portal to summon any player!", 10, 40, 10);
        player.sendMessage("§5§l🌀 §fSummon portal created! Click it to summon players!");
        player.sendMessage("§eYou have 2 minutes to use this portal!");
    }
    
    private void createSummonPortalVortex(Location center, MagicFruits plugin) {
        World world = center.getWorld();
        
        // Create a beautiful vortex portal effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 120) {
                    this.cancel();
                    return;
                }
                
                double radius = 2.2;
                double height = 2.8;
                
                // Spiral vortex - parametric equations
                for (int i = 0; i < 360; i += 12) {
                    double theta = Math.toRadians(i + ticks * 10);
                    double r = radius * (1 - Math.sin(ticks * 0.03) * 0.2);
                    double x = Math.cos(theta) * r;
                    double z = Math.sin(theta) * r;
                    double y = 1.0 + (Math.sin(theta * 2) * 0.3) + (ticks * 0.02);
                    
                    if (y < height && plugin.getDataManager().isParticlesEnabled()) {
                        // Purple vortex particles
                        world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x6600CC), 0.9f));
                        world.spawnParticle(Particle.PORTAL, center.clone().add(x, y + 0.2, z), 0, 0, 0, 0, 1);
                        
                        // Outer ring - dark purple
                        double xOuter = Math.cos(theta + Math.PI) * (r + 0.3);
                        double zOuter = Math.sin(theta + Math.PI) * (r + 0.3);
                        world.spawnParticle(Particle.DUST, center.clone().add(xOuter, y, zOuter), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x440088), 0.7f));
                    }
                }
                
                // Rising energy particles
                for (int i = 0; i < 40; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = Math.random() * 2.0;
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    double y = Math.random() * height;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.PORTAL, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                        world.spawnParticle(Particle.END_ROD, center.clone().add(x, y, z), 0, 0, 0, 0, 0.5);
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
        
        // Check for summon portal (LEFT CLICK)
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
        
        // Check for teleport portals (RIGHT CLICK to teleport)
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            for (Map.Entry<UUID, PortalData> entry : activePortals.entrySet()) {
                PortalData data = entry.getValue();
                if (clickedLoc.distance(data.portal1) < 1.5) {
                    event.setCancelled(true);
                    player.teleport(data.portal2);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    
                    // Create teleport effect
                    createTeleportEffect(data.portal2, MagicFruits.getInstance());
                    break;
                } else if (clickedLoc.distance(data.portal2) < 1.5) {
                    event.setCancelled(true);
                    player.teleport(data.portal1);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    
                    createTeleportEffect(data.portal1, MagicFruits.getInstance());
                    break;
                }
            }
        }
    }
    
    private void createTeleportEffect(Location loc, MagicFruits plugin) {
        World world = loc.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 20) {
                    this.cancel();
                    return;
                }
                
                double radius = 1.5;
                for (int i = 0; i < 360; i += 20) {
                    double rad = Math.toRadians(i + ticks * 20);
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
    
    private void openSummonGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§5§l🌀 SUMMON PLAYER §5§l🌀");
        
        // Add decorative border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        // Add all online players
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
            lore.add("§7to your location through the portal!");
            lore.add("§8§m-----------------------------");
            meta.setLore(lore);
            
            head.setItemMeta(meta);
            gui.setItem(slot, head);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        // Add info item
        ItemStack info = new ItemStack(Material.ENDER_EYE);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§5§l🌀 SUMMON PORTAL 🌀");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8§m-----------------------------");
        infoLore.add("§7Click any player to summon them");
        infoLore.add("§7through the portal to your location!");
        infoLore.add("§7Portal lasts for §e2 minutes");
        infoLore.add("§7Cooldown: §c20 minutes");
        infoLore.add("§8§m-----------------------------");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        gui.setItem(49, info);
        
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
            
            // Create portal effect at summon location
            Location summonLoc = player.getLocation();
            createTeleportEffect(summonLoc, plugin);
            
            // Teleport target to player
            target.teleport(summonLoc);
            
            // Effects
            if (plugin.getDataManager().isParticlesEnabled()) {
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 100, 1, 1, 1, 0.5);
                target.getWorld().spawnParticle(Particle.DRAGON_BREATH, target.getLocation(), 50, 1, 1, 1, 0.3);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                target.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
            
            target.sendTitle("§5§l🌀 SUMMONED! 🌀", 
                "§eYou have been summoned by " + player.getName(), 10, 40, 10);
            player.sendMessage("§a§l✓ §fYou have summoned §e" + target.getName() + "§f to your location!");
            
            player.closeInventory();
            
            // Remove the summon portal after use
            activeSummonPortals.remove(player.getUniqueId());
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Create teleport portals (2 portals, 15s to connect, 60s duration)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Create summon portal (20 min cooldown, click to summon any player)";
    }
}

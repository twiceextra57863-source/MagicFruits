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
        
        // Check if player is already creating a portal
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
            
            // Create Doctor Strange style portal effects
            createDoctorStrangePortal(data.firstPortal);
            createDoctorStrangePortal(secondPortal);
            
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
        
        // First portal placement
        Block targetBlock = player.getTargetBlock(null, 30);
        if (targetBlock == null) {
            player.sendMessage("§c§l⚠ §fNo block in sight!");
            return;
        }
        
        Location firstPortal = targetBlock.getLocation().add(0.5, 1, 0.5);
        
        // Create first portal effect
        createPortalEffect(firstPortal, plugin);
        
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
    
    private void createPortalEffect(Location loc, MagicFruits plugin) {
        World world = loc.getWorld();
        
        // Create rotating ring effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40) {
                    this.cancel();
                    return;
                }
                
                double radius = 1.2;
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i + ticks * 10);
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
    
    private void createDoctorStrangePortal(Location loc) {
        MagicFruits plugin = MagicFruits.getInstance();
        World world = loc.getWorld();
        
        // Create Doctor Strange style portal with multiple rings
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 60) {
                    this.cancel();
                    return;
                }
                
                // Outer ring
                double outerRadius = 2.0;
                // Middle ring
                double middleRadius = 1.5;
                // Inner ring
                double innerRadius = 1.0;
                
                // Create multiple rotating rings
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i + ticks * 15);
                    
                    // Outer ring - orange/gold
                    double x1 = Math.cos(rad) * outerRadius;
                    double z1 = Math.sin(rad) * outerRadius;
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.DUST, loc.clone().add(x1, 0.5, z1), 0, 0, 0, 0, 
                            new Particle.DustOptions(Color.fromRGB(0xFF6600), 0.8f));
                        world.spawnParticle(Particle.FLAME, loc.clone().add(x1, 1, z1), 0, 0, 0, 0, 0.5);
                    }
                    
                    // Middle ring - orange/red
                    double rad2 = Math.toRadians(i + ticks * 12);
                    double x2 = Math.cos(rad2) * middleRadius;
                    double z2 = Math.sin(rad2) * middleRadius;
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.DUST, loc.clone().add(x2, 1, z2), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFF3300), 0.7f));
                        world.spawnParticle(Particle.PORTAL, loc.clone().add(x2, 1.2, z2), 0, 0, 0, 0, 1);
                    }
                    
                    // Inner ring - golden
                    double rad3 = Math.toRadians(i + ticks * 10);
                    double x3 = Math.cos(rad3) * innerRadius;
                    double z3 = Math.sin(rad3) * innerRadius;
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.END_ROD, loc.clone().add(x3, 1.5, z3), 0, 0, 0, 0, 1);
                        world.spawnParticle(Particle.DUST, loc.clone().add(x3, 0.8, z3), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFAA44), 0.6f));
                    }
                }
                
                // Central glow
                for (int i = 0; i < 20; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random() * 1.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(x, 1 + Math.random(), z), 0, 0, 0, 0, 1);
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
        
        // Create unique summon portal effect
        createSummonPortalEffect(portalLoc, plugin);
        
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
    
    private void createSummonPortalEffect(Location loc, MagicFruits plugin) {
        World world = loc.getWorld();
        
        // Create unique purple-black vortex effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 80) {
                    this.cancel();
                    return;
                }
                
                double radius = 2.0;
                double height = 2.5;
                
                // Spiral effect
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i + ticks * 8);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    double y = Math.sin(rad * 2) * 0.5 + 1;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.DUST, loc.clone().add(x, y, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x6600CC), 0.8f));
                        world.spawnParticle(Particle.PORTAL, loc.clone().add(x, y + 0.5, z), 0, 0, 0, 0, 1);
                    }
                }
                
                // Rising particles
                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radiusR = Math.random() * 2;
                    double x = Math.cos(angle) * radiusR;
                    double z = Math.sin(angle) * radiusR;
                    double y = Math.random() * height;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.SPELL_WITCH, loc.clone().add(x, y, z), 0, 0, 0, 0, 1);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;
            
            Location clickedLoc = clickedBlock.getLocation().add(0.5, 1, 0.5);
            
            // Check for summon portal
            for (Map.Entry<UUID, SummonPortalData> entry : activeSummonPortals.entrySet()) {
                SummonPortalData data = entry.getValue();
                if (clickedLoc.distance(data.portalLocation) < 1.5 && data.owner.equals(player)) {
                    event.setCancelled(true);
                    openSummonGUI(player, data.portalLocation);
                    break;
                }
            }
        }
        
        // Also check for teleport portals (right click to teleport)
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;
            
            Location clickedLoc = clickedBlock.getLocation().add(0.5, 1, 0.5);
            
            for (Map.Entry<UUID, PortalData> entry : activePortals.entrySet()) {
                PortalData data = entry.getValue();
                if (clickedLoc.distance(data.portal1) < 1.5) {
                    event.setCancelled(true);
                    player.teleport(data.portal2);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    break;
                } else if (clickedLoc.distance(data.portal2) < 1.5) {
                    event.setCancelled(true);
                    player.teleport(data.portal1);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    break;
                }
            }
        }
    }
    
    private void openSummonGUI(Player player, Location portalLoc) {
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
        
        // Handle GUI click
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTitle().equals("§5§l🌀 SUMMON PLAYER §5§l🌀")) {
                    handleSummonGUI(player, portalLoc);
                }
            }
        }.runTaskLater(MagicFruits.getInstance(), 1L);
    }
    
    private void handleSummonGUI(Player player, Location portalLoc) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.getOpenInventory().getTitle().equals("§5§l🌀 SUMMON PLAYER §5§l🌀")) {
                    this.cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
            // Create portal effect at summon location
            Location summonLoc = player.getLocation();
            createSummonEffect(summonLoc, MagicFruits.getInstance());
            
            // Teleport target to player
            target.teleport(summonLoc);
            
            // Effects
            if (MagicFruits.getInstance().getDataManager().isParticlesEnabled()) {
                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 100, 1, 1, 1, 0.5);
                target.getWorld().spawnParticle(Particle.DRAGON_BREATH, target.getLocation(), 50, 1, 1, 1, 0.3);
            }
            
            if (MagicFruits.getInstance().getDataManager().isSoundsEnabled()) {
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
    
    private void createSummonEffect(Location loc, MagicFruits plugin) {
        World world = loc.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 30) {
                    this.cancel();
                    return;
                }
                
                double radius = 2.0;
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i + ticks * 15);
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
    
    @Override
    public String getPrimaryDescription() {
        return "Create teleport portals (2 portals, 15s to connect, 60s duration)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Create summon portal (20 min cooldown, click to summon any player)";
    }
}

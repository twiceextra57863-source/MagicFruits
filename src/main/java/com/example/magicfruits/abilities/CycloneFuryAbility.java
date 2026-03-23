package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class CycloneFuryAbility implements Ability, Listener {
    
    private final Map<UUID, TornadoData> activeTornadoes = new HashMap<>();
    private final Map<UUID, Long> tornadoCooldown = new HashMap<>();
    private final Map<UUID, Long> cycloneCooldown = new HashMap<>();
    private final Map<UUID, Integer> blockCount = new HashMap<>();
    
    private static class TornadoData {
        Location center;
        List<Block> floatingBlocks;
        List<Entity> trappedEntities;
        int duration;
        int ticks;
        long expiryTime;
        
        TornadoData(Location center, long expiry) {
            this.center = center;
            this.floatingBlocks = new ArrayList<>();
            this.trappedEntities = new ArrayList<>();
            this.duration = 200; // 10 seconds
            this.ticks = 0;
            this.expiryTime = expiry;
        }
    }
    
    public CycloneFuryAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        // Cleanup task
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activeTornadoes.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeTornadoSummon(player, plugin);
        } else {
            executeCycloneDash(player, plugin);
        }
    }
    
    private void executeCycloneDash(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (40 seconds)
        long lastUse = cycloneCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 40000) {
            long remaining = (40000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fCyclone Dash on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        // Apply speed effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 9)); // 10x speed (speed 10)
        
        // Create tornado effect around player
        cycloneCooldown.put(uuid, System.currentTimeMillis());
        
        // Visual and sound effects
        if (plugin.getDataManager().isParticlesEnabled()) {
            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                double radius = 3;
                double x = Math.cos(rad) * radius;
                double z = Math.sin(rad) * radius;
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().clone().add(x, 1, z), 0, 0, 0, 0, 1);
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().clone().add(x, 2, z), 0, 0, 0, 0, 1);
            }
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        }
        
        player.sendTitle("§b§l🌀 CYCLONE FURY! 🌀", 
            "§eYou become a deadly tornado!", 10, 40, 10);
        player.sendMessage("§b§l🌀 §fYou transform into a cyclone for 10 seconds!");
        player.sendMessage("§eNearby enemies will be launched into the air!");
        
        // Create tornado effect that follows player
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 200) { // 10 seconds
                    this.cancel();
                    return;
                }
                
                // Tornado particle effect around player
                if (plugin.getDataManager().isParticlesEnabled()) {
                    for (int i = 0; i < 50; i++) {
                        double angle = Math.toRadians(ticks * 10 + i * 7);
                        double radius = 2.5;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().clone().add(x, ticks * 0.1, z), 0, 0, 0, 0, 1);
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().clone().add(x, ticks * 0.1 + 1, z), 0, 0, 0, 0, 1);
                    }
                }
                
                // Launch nearby entities
                for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        // Rotate around player
                        double angle = Math.toRadians(ticks * 15);
                        double radius = 3;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        
                        // Launch upward and rotate
                        Vector velocity = new Vector(x, 1.5, z);
                        target.setVelocity(velocity);
                        
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void executeTornadoSummon(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (60 seconds)
        long lastUse = tornadoCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fTornado Summon on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        // Create tornado at player's location
        Location center = player.getLocation();
        TornadoData tornado = new TornadoData(center, System.currentTimeMillis() + 10000);
        activeTornadoes.put(uuid, tornado);
        tornadoCooldown.put(uuid, System.currentTimeMillis());
        blockCount.put(uuid, 0);
        
        if (plugin.getDataManager().isParticlesEnabled()) {
            // Initial tornado particles
            for (int i = 0; i < 360; i += 5) {
                double rad = Math.toRadians(i);
                double radius = 4;
                double x = Math.cos(rad) * radius;
                double z = Math.sin(rad) * radius;
                for (int y = 0; y < 8; y++) {
                    player.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                }
            }
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 1.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }
        
        player.sendTitle("§b§l🌪️ TORNADO SUMMONED! 🌪️", 
            "§eLeft click to slam blocks at enemies!", 10, 40, 10);
        player.sendMessage("§b§l🌪️ §fA devastating tornado appears for 10 seconds!");
        player.sendMessage("§eBlocks will rise and rotate around the tornado!");
        player.sendMessage("§eLeft click during the tornado to slam blocks at your cursor!");
        
        // Tornado animation and block collection
        new BukkitRunnable() {
            int ticks = 0;
            int blockCollectTick = 0;
            
            @Override
            public void run() {
                if (!activeTornadoes.containsKey(uuid) || ticks >= 200) {
                    // Cleanup floating blocks
                    for (Block block : tornado.floatingBlocks) {
                        if (block != null && block.getType() != Material.AIR) {
                            block.setType(Material.AIR);
                        }
                    }
                    activeTornadoes.remove(uuid);
                    blockCount.remove(uuid);
                    player.sendMessage("§c§l⚠ §fThe tornado has dissipated!");
                    this.cancel();
                    return;
                }
                
                // Update tornado center to player's location
                tornado.center = player.getLocation();
                
                // Collect blocks every 10 ticks (0.5 seconds)
                if (blockCollectTick >= 10 && tornado.floatingBlocks.size() < 30) {
                    collectNearbyBlocks(tornado, plugin);
                    blockCollectTick = 0;
                }
                blockCollectTick++;
                
                // Rotate and animate floating blocks
                if (plugin.getDataManager().isParticlesEnabled()) {
                    for (int i = 0; i < tornado.floatingBlocks.size(); i++) {
                        Block block = tornado.floatingBlocks.get(i);
                        if (block != null && block.getType() != Material.AIR) {
                            double angle = Math.toRadians(ticks * 5 + i * 15);
                            double radius = 3 + (i * 0.1);
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            double y = 1 + (i * 0.2) + (Math.sin(ticks * 0.1) * 0.5);
                            
                            Location blockLoc = tornado.center.clone().add(x, y, z);
                            block.getWorld().spawnParticle(Particle.CLOUD, blockLoc, 5, 0.1, 0.1, 0.1, 0.05);
                            block.getWorld().spawnParticle(Particle.SWEEP_ATTACK, blockLoc, 3, 0.1, 0.1, 0.1, 0.05);
                        }
                    }
                }
                
                // Suck in nearby entities
                for (Entity entity : tornado.center.getWorld().getNearbyEntities(tornado.center, 8, 8, 8)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        // Pull towards tornado center and rotate
                        Vector direction = tornado.center.toVector().subtract(target.getLocation().toVector()).normalize();
                        double angle = Math.toRadians(ticks * 10);
                        Vector rotation = new Vector(Math.cos(angle), 0.3, Math.sin(angle));
                        
                        target.setVelocity(direction.multiply(0.5).add(rotation));
                        target.damage(1, player);
                        
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);
                        }
                        
                        if (!tornado.trappedEntities.contains(target)) {
                            tornado.trappedEntities.add(target);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void collectNearbyBlocks(TornadoData tornado, MagicFruits plugin) {
        int radius = 5;
        int collected = 0;
        
        for (int x = -radius; x <= radius && collected < 3; x++) {
            for (int z = -radius; z <= radius && collected < 3; z++) {
                for (int y = -2; y <= 3 && collected < 3; y++) {
                    Block block = tornado.center.clone().add(x, y, z).getBlock();
                    Material type = block.getType();
                    
                    // Collect non-air, non-bedrock blocks
                    if (type != Material.AIR && type != Material.BEDROCK && type != Material.WATER && type != Material.LAVA) {
                        if (!tornado.floatingBlocks.contains(block)) {
                            tornado.floatingBlocks.add(block);
                            
                            if (plugin.getDataManager().isParticlesEnabled()) {
                                block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.1);
                            }
                            
                            collected++;
                            break;
                        }
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check if player has active tornado and left clicks
        if (activeTornadoes.containsKey(uuid) && 
            (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR || 
             event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK)) {
            event.setCancelled(true);
            
            TornadoData tornado = activeTornadoes.get(uuid);
            if (tornado.floatingBlocks.isEmpty()) {
                player.sendMessage("§c§l⚠ §fNo blocks to slam!");
                return;
            }
            
            // Get target location at cursor
            Block targetBlock = player.getTargetBlock(null, 30);
            if (targetBlock == null) {
                player.sendMessage("§c§l⚠ §fNo target in sight!");
                return;
            }
            
            Location targetLoc = targetBlock.getLocation();
            MagicFruits plugin = MagicFruits.getInstance();
            
            // Slam the first block
            Block block = tornado.floatingBlocks.remove(0);
            if (block != null && block.getType() != Material.AIR) {
                // Create slamming effect
                for (int i = 0; i < 10; i++) {
                    double x = targetLoc.getX() + (Math.random() - 0.5) * 2;
                    double z = targetLoc.getZ() + (Math.random() - 0.5) * 2;
                    targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, x, targetLoc.getY() + 1, z, 1, 0, 0, 0, 0);
                    targetLoc.getWorld().spawnParticle(Particle.CLOUD, x, targetLoc.getY() + 1, z, 5, 0.2, 0.2, 0.2, 0.05);
                }
                
                // Damage entities in the area
                for (Entity entity : targetLoc.getWorld().getNearbyEntities(targetLoc, 3, 3, 3)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(8, player);
                        target.setVelocity(new Vector(0, 0.5, 0));
                        
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                }
                
                if (plugin.getDataManager().isSoundsEnabled()) {
                    targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                }
                
                player.sendMessage("§b§l🌪️ §fYou slammed a block at the target!");
                
                // Remove block from world
                block.setType(Material.AIR);
            }
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Cyclone Dash (10s, 10x speed, launches enemies, 40s cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Summon Tornado (10s, collects blocks, slam with left click, 60s cooldown)";
    }
                }

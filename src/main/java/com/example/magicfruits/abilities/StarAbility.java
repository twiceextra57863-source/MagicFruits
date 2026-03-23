package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StarAbility implements Ability, Listener {
    
    private final Map<UUID, Long> supernovaCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> blackHoleCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, SupernovaData> activeSupernova = new ConcurrentHashMap<>();
    private final Map<UUID, BlackHoleData> activeBlackHole = new ConcurrentHashMap<>();
    
    private static class SupernovaData {
        Player player;
        long expiryTime;
        int ticks;
        List<Location> starLocations;
        
        SupernovaData(Player player, long expiry) {
            this.player = player;
            this.expiryTime = expiry;
            this.ticks = 0;
            this.starLocations = new ArrayList<>();
        }
    }
    
    private static class BlackHoleData {
        Location center;
        Player owner;
        long expiryTime;
        int ticks;
        List<Entity> trappedEntities;
        
        BlackHoleData(Location center, Player owner, long expiry) {
            this.center = center;
            this.owner = owner;
            this.expiryTime = expiry;
            this.ticks = 0;
            this.trappedEntities = new ArrayList<>();
        }
    }
    
    public StarAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activeSupernova.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                activeBlackHole.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                supernovaCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
                blackHoleCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeBlackHole(player, plugin);
        } else {
            executeSupernova(player, plugin);
        }
    }
    
    private void executeSupernova(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = supernovaCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fSupernova on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        supernovaCooldown.put(uuid, System.currentTimeMillis());
        
        // Create Supernova effect
        SupernovaData supernova = new SupernovaData(player, System.currentTimeMillis() + 8000);
        activeSupernova.put(uuid, supernova);
        
        // Cosmic power effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 2, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 2, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 160, 1, false, false));
        
        // Epic sound
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.8f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
        }
        
        player.sendTitle("§6§l🌟 SUPERNOVA ACTIVATED! 🌟", 
            "§eYou become a cosmic star!", 10, 40, 10);
        player.sendMessage("§6§l🌟 §fYou harness the power of a supernova for 8 seconds!");
        
        // Create orbiting stars
        createOrbitingStars(player, plugin);
        
        // Star explosion animation
        createStarExplosion(player.getLocation(), plugin);
        
        // Auto attack
        startSupernovaAttacks(player, plugin);
        
        // Auto deactivate
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeSupernova.containsKey(uuid)) {
                    deactivateSupernova(player, plugin);
                }
            }
        }.runTaskLater(plugin, 160L);
    }
    
    private void createOrbitingStars(Player player, MagicFruits plugin) {
        new BukkitRunnable() {
            int ticks = 0;
            List<Location> stars = new ArrayList<>();
            
            @Override
            public void run() {
                if (!activeSupernova.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }
                
                Location center = player.getLocation();
                
                // Create 8 orbiting stars
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(ticks * 15 + i * 45);
                    double radius = 2.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = Math.sin(ticks * 0.2) * 0.8 + 1;
                    
                    Location starLoc = center.clone().add(x, y, z);
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        // Core star
                        starLoc.getWorld().spawnParticle(Particle.END_ROD, starLoc, 2, 0.1, 0.1, 0.1, 0.05);
                        starLoc.getWorld().spawnParticle(Particle.FLAME, starLoc, 1, 0.1, 0.1, 0.1, 0.02);
                        starLoc.getWorld().spawnParticle(Particle.DUST, starLoc, 3, 0.1, 0.1, 0.1, 
                            new Particle.DustOptions(Color.fromRGB(0xFFD700), 0.8f));
                        
                        // Trail
                        starLoc.getWorld().spawnParticle(Particle.SPELL_WITCH, starLoc, 5, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                
                // Central star aura
                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = 1.2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 1 + Math.random(), z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFAA00), 0.6f));
                        center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, 1 + Math.random(), z), 0, 0, 0, 0, 0.1);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void createStarExplosion(Location loc, MagicFruits plugin) {
        new BukkitRunnable() {
            int radius = 0;
            
            @Override
            public void run() {
                if (radius > 8) {
                    this.cancel();
                    return;
                }
                
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, 0.5, z), 0, 0, 0, 0, 1);
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x, 1, z), 0, 0, 0, 0, 1);
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 1.5, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFF6600), 0.7f));
                    }
                }
                
                radius++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void startSupernovaAttacks(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        new BukkitRunnable() {
            int attackTicks = 0;
            
            @Override
            public void run() {
                if (!activeSupernova.containsKey(uuid)) {
                    this.cancel();
                    return;
                }
                
                // Star beam every 10 ticks (0.5 seconds)
                if (attackTicks % 10 == 0) {
                    fireStarBeam(player, plugin);
                }
                
                // Damage nearby entities
                for (Entity entity : player.getNearbyEntities(6, 5, 6)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(4, player);
                        
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
                            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);
                        }
                        
                        // Launch away
                        Vector away = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                        away.setY(0.5);
                        target.setVelocity(away.multiply(1.2));
                    }
                }
                
                attackTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void fireStarBeam(Player player, MagicFruits plugin) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();
        
        new BukkitRunnable() {
            int distance = 0;
            
            @Override
            public void run() {
                if (distance > 15) {
                    this.cancel();
                    return;
                }
                
                Location current = start.clone().add(direction.clone().multiply(distance));
                
                // Beam particles
                if (plugin.getDataManager().isParticlesEnabled()) {
                    for (int i = 0; i < 10; i++) {
                        double offset = (Math.random() - 0.5) * 0.3;
                        current.getWorld().spawnParticle(Particle.END_ROD, current.clone().add(offset, offset, offset), 0, 0, 0, 0, 1);
                        current.getWorld().spawnParticle(Particle.FLAME, current.clone().add(offset, offset, offset), 0, 0, 0, 0, 1);
                        current.getWorld().spawnParticle(Particle.DUST, current.clone().add(offset, offset, offset), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFAA44), 0.5f));
                    }
                }
                
                // Damage entities in beam
                for (Entity entity : current.getWorld().getNearbyEntities(current, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        ((LivingEntity) entity).damage(6, player);
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            entity.getWorld().spawnParticle(Particle.EXPLOSION, entity.getLocation(), 1, 0, 0, 0, 0);
                        }
                    }
                }
                
                distance++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void deactivateSupernova(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        activeSupernova.remove(uuid);
        
        // Final explosion
        if (plugin.getDataManager().isParticlesEnabled()) {
            for (int i = 0; i < 200; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 4;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation().clone().add(x, 1, z), 0, 0, 0, 0, 1);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().clone().add(x, 1, z), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        }
        
        player.sendMessage("§c§l💥 §fThe supernova has faded!");
    }
    
    private void executeBlackHole(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = blackHoleCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fBlack Hole on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        blackHoleCooldown.put(uuid, System.currentTimeMillis());
        
        // Create black hole at target location
        Block targetBlock = player.getTargetBlock(null, 30);
        Location blackHoleLoc = targetBlock != null ? targetBlock.getLocation().add(0.5, 1, 0.5) : player.getLocation().add(0, 2, 0);
        
        BlackHoleData blackHole = new BlackHoleData(blackHoleLoc, player, System.currentTimeMillis() + 8000);
        activeBlackHole.put(uuid, blackHole);
        
        // Epic sound
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(blackHoleLoc, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.5f);
            player.getWorld().playSound(blackHoleLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
        }
        
        player.sendTitle("§8§l🌀 BLACK HOLE! 🌀", 
            "§eA cosmic void consumes all!", 10, 40, 10);
        player.sendMessage("§8§l🌀 §fYou summon a devastating black hole for 8 seconds!");
        
        // Black hole animation
        createBlackHoleAnimation(blackHoleLoc, plugin);
        
        // Black hole pull effect
        startBlackHolePull(blackHole, plugin);
        
        // Auto deactivate
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeBlackHole.containsKey(uuid)) {
                    deactivateBlackHole(blackHole, plugin);
                }
            }
        }.runTaskLater(plugin, 160L);
    }
    
    private void createBlackHoleAnimation(Location center, MagicFruits plugin) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!activeBlackHole.values().stream().anyMatch(bh -> bh.center.equals(center))) {
                    this.cancel();
                    return;
                }
                
                // Spiral particles
                for (int ring = 0; ring < 5; ring++) {
                    double angle = Math.toRadians(ticks * 10 + ring * 72);
                    double radius = 1.5 + ring * 0.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.5, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x330033), 1.0f));
                        center.getWorld().spawnParticle(Particle.SPELL_WITCH, center.clone().add(x, 1, z), 0, 0, 0, 0, 1);
                        center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(x, 1.5, z), 0, 0, 0, 0, 1);
                    }
                }
                
                // Central void
                for (int i = 0; i < 20; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random() * 2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.5 + Math.random(), z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x000000), 1.2f));
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void startBlackHolePull(BlackHoleData blackHole, MagicFruits plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeBlackHole.containsValue(blackHole)) {
                    this.cancel();
                    return;
                }
                
                // Pull all entities within 12 blocks
                for (Entity entity : blackHole.center.getWorld().getNearbyEntities(blackHole.center, 12, 8, 12)) {
                    if (entity instanceof LivingEntity && !entity.equals(blackHole.owner)) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        // Calculate pull force
                        Vector direction = blackHole.center.toVector().subtract(target.getLocation().toVector());
                        double distance = direction.length();
                        double force = Math.max(0.5, 2.0 - (distance / 8.0));
                        
                        direction.normalize();
                        target.setVelocity(direction.multiply(force));
                        
                        // Damage based on distance
                        if (distance < 3) {
                            target.damage(5, blackHole.owner);
                            if (plugin.getDataManager().isParticlesEnabled()) {
                                target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.3, 0.3, 0.3, 0.1);
                            }
                        } else if (distance < 6) {
                            target.damage(3, blackHole.owner);
                        }
                        
                        // Pull particles
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            Vector particleDir = direction.clone().multiply(0.3);
                            for (int i = 0; i < 5; i++) {
                                target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 0, 
                                    particleDir.getX(), particleDir.getY(), particleDir.getZ(), 1);
                            }
                        }
                        
                        if (!blackHole.trappedEntities.contains(target)) {
                            blackHole.trappedEntities.add(target);
                        }
                    }
                }
                
                // Absorb particles effect
                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random() * 8;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        blackHole.center.getWorld().spawnParticle(Particle.PORTAL, 
                            blackHole.center.clone().add(x, Math.random() * 3, z), 0, 
                            -x * 0.1, (Math.random() - 0.5) * 0.2, -z * 0.1, 1);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void deactivateBlackHole(BlackHoleData blackHole, MagicFruits plugin) {
        activeBlackHole.values().remove(blackHole);
        
        // Implosion effect
        if (plugin.getDataManager().isParticlesEnabled()) {
            for (int i = 0; i < 300; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                blackHole.center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, blackHole.center.clone().add(x, 1, z), 0, 0, 0, 0, 1);
                blackHole.center.getWorld().spawnParticle(Particle.PORTAL, blackHole.center.clone().add(x, 1, z), 10, 0.2, 0.2, 0.2, 0.05);
            }
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            blackHole.center.getWorld().playSound(blackHole.center, Sound.ENTITY_WITHER_DEATH, 2.0f, 0.6f);
        }
        
        if (blackHole.owner.isOnline()) {
            blackHole.owner.sendMessage("§8§l🌀 §fThe black hole collapses!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Supernova (8s, orbiting stars, star beams, 60s cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Black Hole (8s, massive pull, crushing damage, 60s cooldown)";
    }
}

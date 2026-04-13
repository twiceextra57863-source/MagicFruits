package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DraculaBitesAbility implements Ability, Listener {
    
    private final Map<UUID, Long> batTransformCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, BatTransformData> activeTransform = new ConcurrentHashMap<>();
    
    private static class BatTransformData {
        Player player;
        long expiryTime;
        Location originalLocation;
        
        BatTransformData(Player player, long expiry) {
            this.player = player;
            this.expiryTime = expiry;
            this.originalLocation = player.getLocation().clone();
        }
    }
    
    public DraculaBitesAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, BatTransformData> entry : activeTransform.entrySet()) {
                    if (entry.getValue().expiryTime <= now) {
                        revertFromBat(entry.getValue().player, entry.getValue());
                        activeTransform.remove(entry.getKey());
                    }
                }
                batTransformCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeBatTransform(player, plugin);
        } else {
            executeBloodPhase(player, plugin);
        }
    }
    
    private void executeBloodPhase(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = batTransformCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 45000) {
            long remaining = (45000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fBlood Phase on cooldown! §7(" + remaining + "s)");
            return;
        }
        
        batTransformCooldown.put(uuid, System.currentTimeMillis());
        
        // Blood phase effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
        
        if (plugin.getDataManager().isParticlesEnabled()) {
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= 100) {
                        this.cancel();
                        return;
                    }
                    for (int i = 0; i < 10; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double radius = 1.5;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(x, 1 + Math.random(), z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x8B0000), 0.8f));
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
        }
        
        player.sendTitle("§4§l🩸 BLOOD PHASE! 🩸", "§eStrength and Speed boosted!", 10, 40, 10);
        player.sendMessage("§4§l🩸 §fBlood Phase active for 15 seconds!");
    }
    
    private void executeBatTransform(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = batTransformCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fBat Transform on cooldown! §7(" + remaining + "s)");
            return;
        }
        
        // Already transformed?
        if (activeTransform.containsKey(uuid)) {
            player.sendMessage("§c§l⚠ §fYou are already a bat!");
            return;
        }
        
        batTransformCooldown.put(uuid, System.currentTimeMillis());
        
        // Store transform data
        BatTransformData data = new BatTransformData(player, System.currentTimeMillis() + 10000);
        activeTransform.put(uuid, data);
        
        // Transform player into bat
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 10, false, false));
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0.3f);
        
        // Bat particles around player
        createBatParticles(player, plugin);
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.0f, 0.8f);
        }
        
        player.sendTitle("§8§l🦇 BAT TRANSFORMATION! 🦇", "§eYou are now a bat for 10 seconds!", 10, 40, 10);
        player.sendMessage("§8§l🦇 §fYou transformed into a bat! Fly away!");
        player.sendMessage("§eUse WASD to fly, look up/down to change altitude");
        
        // Start bat flight control
        startBatFlight(player, plugin);
        
        // Spiral particles on expiry
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeTransform.containsKey(uuid)) {
                    revertFromBat(player, data);
                    activeTransform.remove(uuid);
                }
            }
        }.runTaskLater(plugin, 200L);
    }
    
    private void createBatParticles(Player player, MagicFruits plugin) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!activeTransform.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }
                
                // Bat wings effect
                for (int i = -1; i <= 1; i++) {
                    double offset = i * 0.8;
                    double angle = Math.toRadians(ticks * 20);
                    double x = Math.cos(angle) * 0.5 + offset;
                    double z = Math.sin(angle) * 0.3;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(x, 0.5, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x333333), 0.5f));
                        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().clone().add(x, 0.3, z), 0, 0, 0, 0, 1);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void startBatFlight(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeTransform.containsKey(uuid)) {
                    this.cancel();
                    return;
                }
                
                // Flight control based on looking direction
                float yaw = player.getLocation().getYaw();
                float pitch = player.getLocation().getPitch();
                
                double speed = 1.2;
                Vector velocity = new Vector();
                
                // Move forward in looking direction
                velocity.setX(-Math.sin(Math.toRadians(yaw)) * speed);
                velocity.setZ(Math.cos(Math.toRadians(yaw)) * speed);
                
                // Up/Down based on pitch
                velocity.setY(-Math.sin(Math.toRadians(pitch)) * 0.8);
                
                player.setVelocity(velocity);
                
                // Bat wing flap sound
                if (Math.random() < 0.05 && plugin.getDataManager().isSoundsEnabled()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BAT_LOOP, 0.5f, 1.0f);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void revertFromBat(Player player, BatTransformData data) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        // Remove effects
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);
        
        // Create spiral particles
        createSpiralParticles(player.getLocation(), plugin);
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        }
        
        player.sendTitle("§8§l🦇 TRANSFORMATION ENDED! 🦇", "§eYou are human again!", 10, 40, 10);
        player.sendMessage("§8§l🦇 §fYou have reverted from bat form!");
    }
    
    private void createSpiralParticles(Location center, MagicFruits plugin) {
        World world = center.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            int maxTicks = 30;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                double radius = 1.5;
                double height = 2.0;
                
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i + ticks * 15);
                    double x = Math.cos(rad) * radius * (1 - ticks / (double)maxTicks);
                    double z = Math.sin(rad) * radius * (1 - ticks / (double)maxTicks);
                    double y = height * (ticks / (double)maxTicks);
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.PORTAL, center.clone().add(x, y, z), 0, 0, 0, 0, 1);
                        world.spawnParticle(Particle.DUST, center.clone().add(x, y, z), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0x8B0000), 0.6f));
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Blood Phase (15s, Strength + Speed boost, 45s cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Bat Transform (10s, turn into a bat and fly freely, 60s cooldown)";
    }
                        }

package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TsunamiAbility implements Ability, Listener {
    
    private final Map<UUID, Long> geyserCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tsunamiCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, TsunamiData> activeTsunamis = new ConcurrentHashMap<>();
    
    private static class TsunamiData {
        Player player;
        Location startLocation;
        Vector direction;
        int duration;
        int ticks;
        
        TsunamiData(Player player, Location start, Vector dir) {
            this.player = player;
            this.startLocation = start;
            this.direction = dir;
            this.duration = 60;
            this.ticks = 0;
        }
    }
    
    public TsunamiAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                geyserCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
                tsunamiCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
                activeTsunamis.entrySet().removeIf(entry -> entry.getValue().ticks >= entry.getValue().duration);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeTsunamiWave(player, plugin);
        } else {
            executeWaterGeyser(player, plugin);
        }
    }
    
    private void executeWaterGeyser(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = geyserCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 45000) {
            long remaining = (45000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fWater Geyser on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        geyserCooldown.put(uuid, System.currentTimeMillis());
        
        List<Entity> targets = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(15, 10, 15)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                targets.add(entity);
            }
        }
        
        if (targets.isEmpty()) {
            player.sendMessage("§c§l⚠ §fNo enemies nearby!");
            return;
        }
        
        for (int i = 0; i < targets.size(); i++) {
            final Entity target = targets.get(i);
            final int delay = i * 5;
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    createGeyserAtEntity(target, player, plugin);
                }
            }.runTaskLater(plugin, delay);
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 0.8f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.2f);
        }
        
        player.sendTitle("§b§l💧 WATER GEYSER! 💧", 
            "§eFountains erupt beneath your enemies!", 10, 40, 10);
        player.sendMessage("§b§l💧 §fWater geysers erupt from beneath your enemies!");
    }
    
    private void createGeyserAtEntity(Entity target, Player player, MagicFruits plugin) {
        Location loc = target.getLocation();
        World world = loc.getWorld();
        Location originalLoc = target.getLocation().clone();
        
        new BukkitRunnable() {
            int height = 0;
            
            @Override
            public void run() {
                if (height > 12 || !target.isValid()) {
                    if (target instanceof LivingEntity) {
                        Vector launch = new Vector(
                            (Math.random() - 0.5) * 1.2,
                            1.8 + Math.random() * 0.5,
                            (Math.random() - 0.5) * 1.2
                        );
                        target.setVelocity(launch);
                        
                        if (target instanceof Player) {
                            ((Player) target).sendTitle("§b§l💦 WHOOSH! 💦", 
                                "§eA geyser launched you skyward!", 5, 20, 5);
                        }
                        
                        ((LivingEntity) target).damage(4, player);
                    }
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        for (int i = 0; i < 200; i++) {
                            double angle = Math.random() * 2 * Math.PI;
                            double radius = Math.random() * 2;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            world.spawnParticle(Particle.SPLASH, originalLoc.clone().add(x, 0.5, z), 0, 0, 0, 0, 1);
                            world.spawnParticle(Particle.BUBBLE_POP, originalLoc.clone().add(x, 0.5, z), 0, 0, 0, 0, 1);
                        }
                    }
                    
                    this.cancel();
                    return;
                }
                
                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = 0.8 + Math.random() * 0.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = height + Math.random() * 0.5;
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        world.spawnParticle(Particle.BUBBLE, originalLoc.clone().add(x, y, z), 0, 0, 0, 0, 1);
                        world.spawnParticle(Particle.BUBBLE_POP, originalLoc.clone().add(x, y, z), 0, 0, 0, 0, 1);
                        world.spawnParticle(Particle.SPLASH, originalLoc.clone().add(x, y, z), 0, 0, 0, 0, 1);
                    }
                }
                
                if (height % 3 == 0 && plugin.getDataManager().isSoundsEnabled()) {
                    world.playSound(originalLoc, Sound.BLOCK_WATER_AMBIENT, 1.0f, 0.8f + (height * 0.05f));
                }
                
                height++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        createRippleEffect(loc, plugin);
    }
    
    private void executeTsunamiWave(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = tsunamiCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fTsunami Wave on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        tsunamiCooldown.put(uuid, System.currentTimeMillis());
        
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location startLoc = player.getLocation().clone();
        
        TsunamiData tsunami = new TsunamiData(player, startLoc, direction);
        activeTsunamis.put(uuid, tsunami);
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 80, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 80, 1, false, false));
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.5f);
        }
        
        player.sendTitle("§b§l🌊 TSUNAMI WAVE! 🌊", 
            "§eBecome the ocean's fury!", 10, 40, 10);
        player.sendMessage("§b§l🌊 §fYou transform into a devastating tsunami wave!");
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 60 || !activeTsunamis.containsKey(uuid)) {
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    player.removePotionEffect(PotionEffectType.WATER_BREATHING);
                    activeTsunamis.remove(uuid);
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        for (int i = 0; i < 300; i++) {
                            double angle = Math.random() * 2 * Math.PI;
                            double radius = Math.random() * 5;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().clone().add(x, 1, z), 0, 0, 0, 0, 1);
                        }
                    }
                    
                    player.sendMessage("§c§l⚠ §fThe tsunami wave subsides!");
                    this.cancel();
                    return;
                }
                
                double speed = 1.2;
                Vector move = direction.clone().multiply(speed);
                player.setVelocity(move);
                
                double waveHeight = Math.sin(ticks * 0.3) * 1.5 + 2;
                double waveWidth = 4;
                
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * waveWidth;
                    double z = Math.sin(rad) * waveWidth;
                    
                    for (int y = 0; y < waveHeight; y++) {
                        Location waveLoc = player.getLocation().clone().add(x, y - 1, z);
                        
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            waveLoc.getWorld().spawnParticle(Particle.BUBBLE, waveLoc, 0, 0, 0, 0, 1);
                            waveLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, waveLoc, 0, 0, 0, 0, 1);
                            waveLoc.getWorld().spawnParticle(Particle.SPLASH, waveLoc, 0, 0, 0, 0, 1);
                            
                            if (y > waveHeight - 1) {
                                waveLoc.getWorld().spawnParticle(Particle.CLOUD, waveLoc, 0, 0, 0, 0, 1);
                            }
                        }
                    }
                }
                
                for (int angle = -60; angle <= 60; angle += 5) {
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * 3.5;
                    double z = Math.sin(rad) * 3.5;
                    
                    Vector forward = direction.clone();
                    Vector right = new Vector(-direction.getZ(), 0, direction.getX());
                    Location frontLoc = player.getLocation().clone()
                        .add(forward.multiply(2))
                        .add(right.multiply(x / 3.5))
                        .add(0, 1.5 + Math.sin(angle * 0.1) * 0.5, 0);
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        frontLoc.getWorld().spawnParticle(Particle.SPLASH, frontLoc, 2, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                
                for (Entity entity : player.getNearbyEntities(6, 4, 6)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        Vector knockback = entity.getLocation().toVector()
                            .subtract(player.getLocation().toVector())
                            .normalize()
                            .multiply(1.8);
                        knockback.setY(0.6);
                        
                        target.setVelocity(knockback);
                        target.damage(3, player);
                        
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                        }
                        
                        if (ticks % 10 == 0 && plugin.getDataManager().isSoundsEnabled()) {
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
                        }
                        
                        if (target instanceof Player) {
                            ((Player) target).sendMessage("§b§l🌊 §fYou were hit by §e" + player.getName() + "§f's tsunami wave!");
                            ((Player) target).playSound(target.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.0f);
                        }
                    }
                }
                
                for (int i = 0; i < 5; i++) {
                    double back = -i * 0.8;
                    Vector backDir = direction.clone().multiply(back);
                    Location trailLoc = player.getLocation().clone().add(backDir).add(0, 0.5, 0);
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        trailLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, trailLoc, 3, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                
                if (ticks % 10 == 0 && plugin.getDataManager().isSoundsEnabled()) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.5f, 0.6f);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void createRippleEffect(Location center, MagicFruits plugin) {
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
                        center.getWorld().spawnParticle(Particle.SPLASH, center.clone().add(x, 0.1, z), 0, 0, 0, 0, 1);
                        center.getWorld().spawnParticle(Particle.BUBBLE, center.clone().add(x, 0.1, z), 0, 0, 0, 0, 1);
                    }
                }
                
                if (plugin.getDataManager().isSoundsEnabled() && radius % 3 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_WATER_AMBIENT, 0.5f, 0.8f - (radius * 0.05f));
                }
                
                radius++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (activeTsunamis.containsKey(player.getUniqueId())) {
            // Allow free movement during tsunami
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Water Geyser (45s cooldown, launches nearby entities skyward with water pillars)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Tsunami Wave (60s cooldown, become a devastating water wave that damages and pushes enemies)";
    }
                        }

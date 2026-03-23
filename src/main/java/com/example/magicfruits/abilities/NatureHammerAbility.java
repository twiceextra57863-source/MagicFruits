package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class NatureHammerAbility implements Ability, Listener {
    
    private final Map<UUID, HookData> activeHooks = new HashMap<>();
    private final Map<UUID, Long> hookCooldown = new HashMap<>();
    private final Map<UUID, Long> hammerCooldown = new HashMap<>();
    
    private static class HookData {
        Player target;
        long expiryTime;
        boolean launched;
        
        HookData(Player target, long expiry) {
            this.target = target;
            this.expiryTime = expiry;
            this.launched = false;
        }
    }
    
    public NatureHammerAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        // Cleanup task
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activeHooks.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeOakHammer(player, plugin);
        } else {
            executeNatureHook(player, plugin);
        }
    }
    
    private void executeNatureHook(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (35 seconds)
        long lastUse = hookCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 35000) {
            long remaining = (35000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fNature Hook on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        // Get target player in line of sight
        Player target = getTargetPlayer(player, 20);
        if (target == null) {
            player.sendMessage("§c§l⚠ §fNo player in sight!");
            return;
        }
        
        // Create hook effect
        activeHooks.put(uuid, new HookData(target, System.currentTimeMillis() + 15000));
        hookCooldown.put(uuid, System.currentTimeMillis());
        
        // Visual effect - Vine tendrils reaching out
        if (plugin.getDataManager().isParticlesEnabled()) {
            new BukkitRunnable() {
                int ticks = 0;
                Location start = player.getEyeLocation();
                Location end = target.getLocation().add(0, 1, 0);
                Vector direction = end.toVector().subtract(start.toVector()).normalize();
                double distance = start.distance(end);
                
                @Override
                public void run() {
                    if (ticks >= 20 || !activeHooks.containsKey(uuid)) {
                        this.cancel();
                        return;
                    }
                    
                    double progress = ticks / 20.0;
                    Location current = start.clone().add(direction.clone().multiply(distance * progress));
                    
                    // Vine particles
                    for (int i = 0; i < 10; i++) {
                        double angle = Math.toRadians(ticks * 36 + i * 36);
                        double radius = 0.3;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        current.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, current.clone().add(x, 0, z), 0, 0, 0, 0, 1);
                        current.getWorld().spawnParticle(Particle.COMPOSTER, current.clone().add(x, 0.3, z), 0, 0, 0, 0, 1);
                    }
                    
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0f, 0.8f);
            player.playSound(target.getLocation(), Sound.ENTITY_VILLAGER_WORK_FARMER, 1.0f, 1.0f);
        }
        
        player.sendTitle("§2§l🌿 NATURE HOOK! 🌿", 
            "§eTarget locked for 15 seconds!", 10, 40, 10);
        player.sendMessage("§2§l🌿 §fYou have hooked §e" + target.getName() + "§f!");
        player.sendMessage("§eLeft click to launch them as a cannonball!");
        
        target.sendMessage("§c§l⚠ §fYou have been hooked by §e" + player.getName() + "§f!");
        target.sendMessage("§eYou will be controlled for 15 seconds!");
        
        // Hook movement control
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeHooks.containsKey(uuid) || activeHooks.get(uuid).launched) {
                    this.cancel();
                    return;
                }
                
                HookData data = activeHooks.get(uuid);
                if (data == null || data.target == null || !data.target.isOnline()) {
                    this.cancel();
                    return;
                }
                
                // Move target with player's cursor
                Location playerLoc = player.getEyeLocation();
                Vector direction = playerLoc.getDirection();
                Location targetLoc = data.target.getLocation();
                
                // Calculate new position 3 blocks in front of player
                Location newPos = playerLoc.clone().add(direction.multiply(2));
                newPos.setY(newPos.getY() - 1);
                
                // Smooth movement
                Vector velocity = newPos.toVector().subtract(targetLoc.toVector()).multiply(0.3);
                data.target.setVelocity(velocity);
                
                // Particle trail
                if (plugin.getDataManager().isParticlesEnabled()) {
                    data.target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, data.target.getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
                    data.target.getWorld().spawnParticle(Particle.COMPOSTER, data.target.getLocation(), 3, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void executeOakHammer(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (50 seconds)
        long lastUse = hammerCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 50000) {
            long remaining = (50000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fOak Hammer on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        hammerCooldown.put(uuid, System.currentTimeMillis());
        
        // Find nearest enemy
        Entity target = getNearestEnemy(player, 8);
        if (target == null) {
            player.sendMessage("§c§l⚠ §fNo enemy in range!");
            return;
        }
        
        // Create Oak Hammer visual
        Location hammerStart = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1));
        Location hammerEnd = target.getLocation().add(0, 1, 0);
        
        // Animate hammer swinging
        new BukkitRunnable() {
            int ticks = 0;
            List<Location> hammerPath = new ArrayList<>();
            
            @Override
            public void run() {
                if (ticks >= 15) {
                    // Smash impact
                    smashTarget(player, target, plugin);
                    this.cancel();
                    return;
                }
                
                double progress = ticks / 15.0;
                Location current = hammerStart.clone().add(hammerEnd.toVector().subtract(hammerStart.toVector()).multiply(progress));
                
                // Create oak hammer particles in a 3D shape
                for (int i = 0; i < 50; i++) {
                    double angle = Math.toRadians(ticks * 30 + i * 7.2);
                    double radius = 0.6;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = Math.sin(angle * 2) * 0.3;
                    
                    // Hammer head (oak wood color)
                    current.getWorld().spawnParticle(Particle.DUST, current.clone().add(x, y + 0.3, z), 0, 0, 0, 0, 
                        new Particle.DustOptions(Color.fromRGB(0x8B5A2B), 0.8f));
                    
                    // Handle (darker oak)
                    current.getWorld().spawnParticle(Particle.DUST, current.clone().add(x * 0.5, y - 0.5, z * 0.5), 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(0x5D3A1A), 0.6f));
                    
                    // Golden accents
                    if (i % 3 == 0) {
                        current.getWorld().spawnParticle(Particle.DUST, current.clone().add(x * 0.8, y + 0.1, z * 0.8), 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(0xFFD700), 0.4f));
                    }
                }
                
                // Swing sound
                if (plugin.getDataManager().isSoundsEnabled() && ticks % 3 == 0) {
                    current.getWorld().playSound(current, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f + (ticks * 0.03f));
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        player.sendTitle("§6§l🔨 OAK HAMMER! 🔨", 
            "§eSmashing your enemy!", 10, 40, 10);
        player.sendMessage("§6§l🔨 §fYou summon an ancient oak hammer!");
    }
    
    private void smashTarget(Player player, Entity target, MagicFruits plugin) {
        Location impactLoc = target.getLocation().add(0, 0.5, 0);
        
        // Ground smash effect - shockwave rings
        if (plugin.getDataManager().isParticlesEnabled()) {
            for (int ring = 0; ring < 5; ring++) {
                final int ringRadius = ring;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 360; i += 10) {
                            double rad = Math.toRadians(i);
                            double radius = 1.5 + (ringRadius * 0.5);
                            double x = Math.cos(rad) * radius;
                            double z = Math.sin(rad) * radius;
                            
                            // Spark particles on ground
                            impactLoc.getWorld().spawnParticle(Particle.DUST, impactLoc.clone().add(x, 0.1, z), 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(0xFFA500), 0.5f));
                            impactLoc.getWorld().spawnParticle(Particle.LAVA, impactLoc.clone().add(x, 0.1, z), 0, 0, 0, 0, 0.1);
                        }
                    }
                }.runTaskLater(plugin, ring * 2L);
            }
            
            // Crushed particle explosion
            for (int i = 0; i < 100; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 2;
                double height = Math.random() * 2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                impactLoc.getWorld().spawnParticle(Particle.DUST, impactLoc.clone().add(x, height, z), 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(0x8B5A2B), 0.5f));
                impactLoc.getWorld().spawnParticle(Particle.CRIT, impactLoc.clone().add(x, height, z), 0, 0, 0, 0, 1);
            }
        }
        
        // Damage and knockback
        if (target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            living.damage(12, player);
            
            // Launch upward with spin
            Vector launch = new Vector(
                (Math.random() - 0.5) * 1.5,
                1.5 + Math.random(),
                (Math.random() - 0.5) * 1.5
            );
            living.setVelocity(launch);
            
            // Add slowness effect
            living.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 2));
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
                impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_WOOD_BREAK, 1.0f, 1.0f);
            }
        }
        
        // Camera shake effect (visual only)
        player.sendMessage("§6§l🔨 §fYour oak hammer smashed the target!");
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check if player has active hook and left clicks
        if (activeHooks.containsKey(uuid) && 
            (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR || 
             event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK)) {
            event.setCancelled(true);
            
            HookData data = activeHooks.get(uuid);
            if (data == null || data.launched) return;
            
            data.launched = true;
            Player target = data.target;
            
            if (target != null && target.isOnline()) {
                MagicFruits plugin = MagicFruits.getInstance();
                
                // Launch target as cannonball
                Vector direction = player.getEyeLocation().getDirection().normalize();
                double power = 2.5;
                target.setVelocity(direction.multiply(power));
                
                // Visual effects
                if (plugin.getDataManager().isParticlesEnabled()) {
                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            if (ticks >= 20) {
                                this.cancel();
                                return;
                            }
                            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10, 0.3, 0.3, 0.3, 0.1);
                            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
                            ticks++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
                
                if (plugin.getDataManager().isSoundsEnabled()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                }
                
                player.sendMessage("§2§l🌿 §fYou launched §e" + target.getName() + "§f like a cannonball!");
                target.sendMessage("§c§l⚠ §fYou were launched by §e" + player.getName() + "§f!");
                
                // Remove hook
                activeHooks.remove(uuid);
            }
        }
    }
    
    private Player getTargetPlayer(Player player, int range) {
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && !entity.equals(player)) {
                Player target = (Player) entity;
                if (player.getEyeLocation().getDirection().angle(
                    target.getLocation().toVector().subtract(player.getEyeLocation().toVector())) < 0.5) {
                    return target;
                }
            }
        }
        return null;
    }
    
    private Entity getNearestEnemy(Player player, double range) {
        Entity nearest = null;
        double nearestDistance = range;
        
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                double distance = entity.getLocation().distance(player.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = entity;
                }
            }
        }
        
        // Also check players
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Player && !entity.equals(player)) {
                double distance = entity.getLocation().distance(player.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = entity;
                }
            }
        }
        
        return nearest;
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Nature Hook (15s control, left click to launch, 35s cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Oak Hammer (Summon hammer, smash enemy, 50s cooldown)";
    }
            }

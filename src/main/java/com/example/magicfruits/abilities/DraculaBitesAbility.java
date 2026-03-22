package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class DraculaBitesAbility implements Ability, Listener {
    
    private final Map<UUID, PhaseData> activePhase = new HashMap<>();
    private final Map<UUID, BatData> activeBat = new HashMap<>();
    private final Map<UUID, Integer> hitCounter = new HashMap<>();
    private final Map<UUID, Long> phaseCooldown = new HashMap<>();
    private final Map<UUID, Long> batCooldown = new HashMap<>();
    private final Map<UUID, Long> biteCooldown = new HashMap<>();
    
    private static class PhaseData {
        long expiryTime;
        
        PhaseData(long expiry) {
            this.expiryTime = expiry;
        }
    }
    
    private static class BatData {
        Bat bat;
        long expiryTime;
        
        BatData(Bat bat, long expiry) {
            this.bat = bat;
            this.expiryTime = expiry;
        }
    }
    
    public DraculaBitesAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        // Cleanup task for expired phases and bats
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                
                // Cleanup phases
                activePhase.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                
                // Cleanup bats
                for (Map.Entry<UUID, BatData> entry : activeBat.entrySet()) {
                    if (entry.getValue().expiryTime <= now) {
                        Bat bat = entry.getValue().bat;
                        if (bat != null && bat.isValid()) {
                            bat.remove();
                        }
                        activeBat.remove(entry.getKey());
                    }
                }
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeBatSummon(player, plugin);
        } else {
            executeBloodPhase(player, plugin);
        }
    }
    
    private void executeBloodPhase(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (45 seconds)
        long lastUse = phaseCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 45000) {
            long remaining = (45000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fBlood Phase on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        // Activate blood phase
        activePhase.put(uuid, new PhaseData(System.currentTimeMillis() + 15000));
        phaseCooldown.put(uuid, System.currentTimeMillis());
        hitCounter.put(uuid, 0);
        
        // Visual effects
        if (plugin.getDataManager().isParticlesEnabled()) {
            for (int i = 0; i < 100; i++) {
                double radius = 1.5;
                double angle = Math.random() * 2 * Math.PI;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation().clone().add(x, Math.random() * 2, z), 0, 0, 0, 0, 1);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().clone().add(x, Math.random() * 2, z), 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0x8B0000), 1.0f));
            }
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        }
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 300, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
        
        player.sendTitle("§4§l🩸 BLOOD PHASE ACTIVATED! 🫸", 
            "§eEvery 3rd hit heals you for 1 heart!", 10, 40, 10);
        player.sendMessage("§4§l🩸 §fYou enter a bloodthirsty phase for 15 seconds!");
        player.sendMessage("§eEvery 3rd hit on enemies will heal you!");
    }
    
    private void executeBatSummon(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (60 seconds)
        long lastUse = batCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fBat Summon on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        // Remove existing bat if any
        if (activeBat.containsKey(uuid)) {
            Bat oldBat = activeBat.get(uuid).bat;
            if (oldBat != null && oldBat.isValid()) {
                oldBat.remove();
            }
            activeBat.remove(uuid);
        }
        
        // Summon bat
        Location spawnLoc = player.getLocation().add(0, 1, 0);
        Bat bat = (Bat) player.getWorld().spawnEntity(spawnLoc, EntityType.BAT);
        bat.setAI(false);
        bat.setSilent(true);
        bat.setInvulnerable(true);
        bat.setGravity(false);
        
        // Set bat as passenger
        bat.addPassenger(player);
        
        activeBat.put(uuid, new BatData(bat, System.currentTimeMillis() + 20000));
        batCooldown.put(uuid, System.currentTimeMillis());
        
        if (plugin.getDataManager().isParticlesEnabled()) {
            for (int i = 0; i < 50; i++) {
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 0, 0, 0, 0, 1);
            }
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.0f, 0.8f);
        }
        
        player.sendTitle("§8§l🦇 BAT SUMMONED! 🦇", 
            "§eUse WASD to fly, Left Click to bite!", 10, 40, 10);
        player.sendMessage("§8§l🦇 §fYou summon a bat to ride for 20 seconds!");
        player.sendMessage("§eUse WASD to control flight, Left Click to bite enemies below!");
        
        // Bat movement control
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeBat.containsKey(uuid) || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                Bat currentBat = activeBat.get(uuid).bat;
                if (currentBat == null || !currentBat.isValid()) {
                    this.cancel();
                    return;
                }
                
                // Get player's movement direction
                Vector direction = new Vector();
                float yaw = player.getLocation().getYaw();
                float pitch = player.getLocation().getPitch();
                
                // Forward/Backward (W/S)
                if (player.isSneaking()) {
                    // S key - move backward
                    direction.setX(-Math.sin(Math.toRadians(yaw)) * 0.5);
                    direction.setZ(Math.cos(Math.toRadians(yaw)) * 0.5);
                } else if (player.isSprinting()) {
                    // W key - move forward
                    direction.setX(-Math.sin(Math.toRadians(yaw)) * 0.8);
                    direction.setZ(Math.cos(Math.toRadians(yaw)) * 0.8);
                } else {
                    direction.setX(0);
                    direction.setZ(0);
                }
                
                // Left/Right (A/D) - handled by player looking direction
                // Up/Down based on pitch
                direction.setY(-Math.sin(Math.toRadians(pitch)) * 0.5);
                
                if (direction.length() > 0) {
                    currentBat.setVelocity(direction);
                }
                
                // Rotate bat to face direction
                if (direction.getX() != 0 || direction.getZ() != 0) {
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
                    currentBat.setRotation(targetYaw, pitch);
                }
            }
        }.runTaskTimer(MagicFruits.getInstance(), 1L, 1L);
        
        // Auto-dismount after 20 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeBat.containsKey(uuid)) {
                    player.leaveVehicle();
                    Bat batData = activeBat.get(uuid).bat;
                    if (batData != null && batData.isValid()) {
                        batData.remove();
                    }
                    activeBat.remove(uuid);
                    player.sendMessage("§c§l⚠ §fYour bat has disappeared!");
                    player.playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(MagicFruits.getInstance(), 400L); // 20 seconds
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        UUID uuid = player.getUniqueId();
        
        // Check if blood phase is active
        if (activePhase.containsKey(uuid)) {
            int hits = hitCounter.getOrDefault(uuid, 0);
            hits++;
            
            if (hits >= 3) {
                // Heal 1 heart (2 health)
                double newHealth = Math.min(player.getHealth() + 2, player.getMaxHealth());
                player.setHealth(newHealth);
                
                // Visual effects
                if (MagicFruits.getInstance().getDataManager().isParticlesEnabled()) {
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
                    player.getWorld().spawnParticle(Particle.SPELL_WITCH, event.getEntity().getLocation(), 20, 0.3, 0.3, 0.3, 0.1);
                }
                
                if (MagicFruits.getInstance().getDataManager().isSoundsEnabled()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                }
                
                player.sendMessage("§4§l🩸 §fYou absorbed blood and healed §c1 heart§f!");
                
                hits = 0;
            }
            
            hitCounter.put(uuid, hits);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check if player is riding bat and left clicks
        if (activeBat.containsKey(uuid) && event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR) {
            event.setCancelled(true);
            
            // Check bite cooldown (2 seconds)
            long lastBite = biteCooldown.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - lastBite < 2000) {
                return; // Silent cooldown - no message
            }
            
            // Get target location below player
            Location eyeLoc = player.getEyeLocation();
            Vector direction = eyeLoc.getDirection();
            Location targetLoc = eyeLoc.clone();
            
            for (int i = 0; i < 30; i++) {
                targetLoc = targetLoc.add(direction);
                
                // Check for entities
                for (Entity entity : player.getWorld().getNearbyEntities(targetLoc, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity && !entity.equals(player) && !(entity instanceof Bat)) {
                        LivingEntity target = (LivingEntity) entity;
                        
                        // Deal damage
                        target.damage(1, player);
                        
                        // Heal player
                        double newHealth = Math.min(player.getHealth() + 1, player.getMaxHealth());
                        player.setHealth(newHealth);
                        
                        // Visual effects
                        if (MagicFruits.getInstance().getDataManager().isParticlesEnabled()) {
                            target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                            target.getWorld().spawnParticle(Particle.DUST, target.getLocation(), 20, 0.3, 0.3, 0.3, new Particle.DustOptions(Color.fromRGB(0x8B0000), 1.0f));
                            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.1);
                        }
                        
                        if (MagicFruits.getInstance().getDataManager().isSoundsEnabled()) {
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 0.8f);
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
                        }
                        
                        player.sendMessage("§8§l🦇 §fYou bit §e" + (target instanceof Player ? target.getName() : "enemy") + "§f and healed!");
                        
                        biteCooldown.put(uuid, System.currentTimeMillis());
                        return;
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // If player is riding bat, prevent normal movement
        if (activeBat.containsKey(player.getUniqueId()) && player.getVehicle() instanceof Bat) {
            event.setCancelled(true);
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Blood Phase (15s, every 3 hits heals 1 heart, 45s cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Summon Bat (20s ride, bite to heal, 60s cooldown)";
    }
}

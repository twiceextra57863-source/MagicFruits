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
import java.util.concurrent.ConcurrentHashMap;

public class DraculaBitesAbility implements Ability, Listener {
    
    private final Map<UUID, PhaseData> activePhase = new ConcurrentHashMap<>();
    private final Map<UUID, BatData> activeBat = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> hitCounter = new ConcurrentHashMap<>();
    private final Map<UUID, Long> phaseCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> batCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> biteCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> isRiding = new ConcurrentHashMap<>();
    
    private static class PhaseData {
        long expiryTime;
        
        PhaseData(long expiry) {
            this.expiryTime = expiry;
        }
    }
    
    private static class BatData {
        Bat bat;
        Player rider;
        long expiryTime;
        
        BatData(Bat bat, Player rider, long expiry) {
            this.bat = bat;
            this.rider = rider;
            this.expiryTime = expiry;
        }
    }
    
    public DraculaBitesAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activePhase.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                
                for (Map.Entry<UUID, BatData> entry : activeBat.entrySet()) {
                    if (entry.getValue().expiryTime <= now) {
                        Bat bat = entry.getValue().bat;
                        if (bat != null && bat.isValid()) {
                            bat.eject();
                            bat.remove();
                        }
                        activeBat.remove(entry.getKey());
                        isRiding.remove(entry.getKey());
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
        
        long lastUse = phaseCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 45000) {
            long remaining = (45000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fBlood Phase on cooldown! §7(" + remaining + "s)");
            return;
        }
        
        activePhase.put(uuid, new PhaseData(System.currentTimeMillis() + 15000));
        phaseCooldown.put(uuid, System.currentTimeMillis());
        hitCounter.put(uuid, 0);
        
        if (plugin.getDataManager().isParticlesEnabled()) {
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (!activePhase.containsKey(uuid)) {
                        this.cancel();
                        return;
                    }
                    if (ticks >= 150) {
                        this.cancel();
                        return;
                    }
                    for (int i = 0; i < 20; i++) {
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
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
        
        player.sendTitle("§4§l🩸 BLOOD PHASE! 🩸", "§eEvery 3rd hit heals you!", 10, 40, 10);
        player.sendMessage("§4§l🩸 §fBlood Phase active for 15 seconds!");
    }
    
    private void executeBatSummon(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = batCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fBat Summon on cooldown! §7(" + remaining + "s)");
            return;
        }
        
        // Remove existing bat
        if (activeBat.containsKey(uuid)) {
            Bat oldBat = activeBat.get(uuid).bat;
            if (oldBat != null && oldBat.isValid()) {
                oldBat.eject();
                oldBat.remove();
            }
            activeBat.remove(uuid);
            isRiding.remove(uuid);
        }
        
        // Summon bat
        Location spawnLoc = player.getLocation().add(0, 1, 0);
        Bat bat = (Bat) player.getWorld().spawnEntity(spawnLoc, EntityType.BAT);
        bat.setAI(false);
        bat.setSilent(true);
        bat.setInvulnerable(true);
        bat.setGravity(false);
        
        activeBat.put(uuid, new BatData(bat, player, System.currentTimeMillis() + 20000));
        batCooldown.put(uuid, System.currentTimeMillis());
        
        // Mount player on bat
        bat.addPassenger(player);
        isRiding.put(uuid, true);
        
        // Start bat flight control
        startBatFlightControl(player, bat, plugin);
        
        if (plugin.getDataManager().isParticlesEnabled()) {
            for (int i = 0; i < 30; i++) {
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 0, 0, 0, 0, 1);
            }
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        }
        
        player.sendTitle("§8§l🦇 BAT RIDE! 🦇", "§eUse WASD to fly, Left Click to bite!", 10, 40, 10);
        player.sendMessage("§8§l🦇 §fYou are riding a bat for 20 seconds!");
        player.sendMessage("§eCrouch to dismount | Crouch again to remount | Left Click to bite");
        
        // Auto dismount after 20 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeBat.containsKey(uuid)) {
                    BatData data = activeBat.get(uuid);
                    if (data.bat != null && data.bat.isValid()) {
                        data.bat.eject();
                        data.bat.remove();
                    }
                    activeBat.remove(uuid);
                    isRiding.remove(uuid);
                    player.sendMessage("§c§l⚠ §fYour bat has flown away!");
                    player.playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, 400L);
    }
    
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Crouch to toggle mount/dismount
        if (event.isSneaking() && activeBat.containsKey(uuid)) {
            BatData data = activeBat.get(uuid);
            if (data != null && data.bat != null && data.bat.isValid()) {
                if (isRiding.getOrDefault(uuid, false)) {
                    // Dismount
                    data.bat.eject();
                    isRiding.put(uuid, false);
                    player.sendMessage("§8§l🦇 §fYou dismounted the bat!");
                    player.playSound(player.getLocation(), Sound.ENTITY_BAT_HURT, 1.0f, 0.8f);
                } else {
                    // Mount
                    data.bat.addPassenger(player);
                    isRiding.put(uuid, true);
                    player.sendMessage("§8§l🦇 §fYou mounted the bat again!");
                    player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
                }
            }
        }
    }
    
    private void startBatFlightControl(Player player, Bat bat, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                BatData data = activeBat.get(uuid);
                if (data == null || data.bat == null || !data.bat.isValid() || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                // Only control if riding
                if (!isRiding.getOrDefault(uuid, false)) {
                    return;
                }
                
                // Get player's input
                boolean forward = player.isSprinting();      // W key
                float yaw = player.getLocation().getYaw();
                
                double speed = 0.6;
                Vector velocity = new Vector();
                
                if (forward) {
                    velocity.setX(-Math.sin(Math.toRadians(yaw)) * speed);
                    velocity.setZ(Math.cos(Math.toRadians(yaw)) * speed);
                }
                
                float pitch = player.getLocation().getPitch();
                velocity.setY(-Math.sin(Math.toRadians(pitch)) * 0.5);
                
                if (velocity.length() > 0) {
                    data.bat.setVelocity(velocity);
                } else {
                    data.bat.setVelocity(new Vector(0, 0, 0));
                }
                
                if (velocity.getX() != 0 || velocity.getZ() != 0) {
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-velocity.getX(), velocity.getZ()));
                    data.bat.setRotation(targetYaw, pitch);
                }
                
                if (plugin.getDataManager().isParticlesEnabled()) {
                    for (int i = 0; i < 2; i++) {
                        double offsetX = Math.sin(Math.toRadians(yaw)) * 0.5;
                        double offsetZ = -Math.cos(Math.toRadians(yaw)) * 0.5;
                        player.getWorld().spawnParticle(Particle.CLOUD, 
                            player.getLocation().clone().add(offsetX, 0.5, offsetZ), 0, 0, 0, 0, 1);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        UUID uuid = player.getUniqueId();
        
        if (activePhase.containsKey(uuid)) {
            int hits = hitCounter.getOrDefault(uuid, 0) + 1;
            
            if (hits >= 3) {
                double newHealth = Math.min(player.getHealth() + 2, player.getMaxHealth());
                player.setHealth(newHealth);
                
                if (MagicFruits.getInstance().getDataManager().isParticlesEnabled()) {
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
                }
                
                player.sendMessage("§4§l🩸 §fYou healed 1 heart!");
                hits = 0;
            }
            hitCounter.put(uuid, hits);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Left click while riding bat
        if (activeBat.containsKey(uuid) && isRiding.getOrDefault(uuid, false) &&
            (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR || 
             event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK)) {
            event.setCancelled(true);
            
            long lastBite = biteCooldown.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - lastBite < 1500) {
                return;
            }
            
            for (Entity entity : player.getNearbyEntities(5, 10, 5)) {
                if (entity instanceof LivingEntity && !entity.equals(player) && !(entity instanceof Bat)) {
                    LivingEntity target = (LivingEntity) entity;
                    
                    target.damage(2, player);
                    double newHealth = Math.min(player.getHealth() + 1, player.getMaxHealth());
                    player.setHealth(newHealth);
                    
                    if (MagicFruits.getInstance().getDataManager().isParticlesEnabled()) {
                        target.getWorld().spawnParticle(Particle.DUST, target.getLocation(), 20, 0.5, 0.5, 0.5,
                            new Particle.DustOptions(Color.fromRGB(0x8B0000), 0.8f));
                        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.1);
                    }
                    
                    if (MagicFruits.getInstance().getDataManager().isSoundsEnabled()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 0.8f);
                    }
                    
                    player.sendMessage("§8§l🦇 §fYou bit §e" + (target instanceof Player ? target.getName() : "enemy") + "§f!");
                    
                    biteCooldown.put(uuid, System.currentTimeMillis());
                    break;
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // Prevent normal movement while riding bat
        if (activeBat.containsKey(player.getUniqueId()) && isRiding.getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Blood Phase (15s, every 3 hits heals 1 heart, 45s cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Summon Bat (20s ride, WASD to fly, crouch to mount/dismount, left click to bite, 60s cooldown)";
    }
            }

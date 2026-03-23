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

public class WindMonsterAbility implements Ability, Listener {
    
    private final Map<UUID, Long> windCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> stormCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveForm> activeForms = new ConcurrentHashMap<>();
    
    private static class ActiveForm {
        Player player;
        String type; // "wind" or "storm"
        long expiryTime;
        int ticks;
        
        ActiveForm(Player player, String type, long expiry) {
            this.player = player;
            this.type = type;
            this.expiryTime = expiry;
            this.ticks = 0;
        }
    }
    
    public WindMonsterAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activeForms.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                windCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
                stormCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeStormMonster(player, plugin);
        } else {
            executeWindMonster(player, plugin);
        }
    }
    
    private void executeWindMonster(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = windCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fWind Monster on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        windCooldown.put(uuid, System.currentTimeMillis());
        activateForm(player, "wind", plugin);
    }
    
    private void executeStormMonster(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        long lastUse = stormCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 60000) {
            long remaining = (60000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fStorm Monster on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        stormCooldown.put(uuid, System.currentTimeMillis());
        activateForm(player, "storm", plugin);
    }
    
    private void activateForm(Player player, String type, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Remove existing form if any
        if (activeForms.containsKey(uuid)) {
            deactivateForm(player);
        }
        
        // Launch player 6 blocks up
        player.setVelocity(new Vector(0, 1.2, 0));
        
        // Create form data
        ActiveForm form = new ActiveForm(player, type, System.currentTimeMillis() + 10000);
        activeForms.put(uuid, form);
        
        // Apply effects - using RESISTANCE instead of DAMAGE_RESISTANCE
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 2, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1, false, false));
        
        // Play epic sounds
        if (plugin.getDataManager().isSoundsEnabled()) {
            if (type.equals("wind")) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 0.8f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.6f);
            } else {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.8f);
            }
        }
        
        // Send title
        if (type.equals("wind")) {
            player.sendTitle("§f§l🌪️ WIND MONSTER! 🌪️", 
                "§eYou become the fury of the winds!", 10, 40, 10);
            player.sendMessage("§f§l🌪️ §fYou transform into a Wind Monster for 10 seconds!");
        } else {
            player.sendTitle("§3§l⚡ STORM MONSTER! ⚡", 
                "§eYou become the wrath of the storm!", 10, 40, 10);
            player.sendMessage("§3§l⚡ §fYou transform into a Storm Monster for 10 seconds!");
        }
        
        // Create particle effects around player
        createAuraEffect(player, type, plugin);
        
        // Start attack animation
        startAttackAnimation(player, type, plugin);
        
        // Auto deactivate after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeForms.containsKey(uuid)) {
                    deactivateForm(player);
                }
            }
        }.runTaskLater(plugin, 200L);
    }
    
    private void createAuraEffect(Player player, String type, MagicFruits plugin) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!activeForms.containsKey(player.getUniqueId())) {
                    this.cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                double yOffset = Math.sin(ticks * 0.2) * 0.3;
                
                // Back wings/cloud effect
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(ticks * 15 + i * 18);
                    double radius = 1.2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    if (type.equals("wind")) {
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(x - 0.8, 1 + yOffset, z), 0, 0, 0, 0, 1);
                            loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x - 0.5, 1.2 + yOffset, z), 0, 0, 0, 0, 1);
                        }
                    } else {
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(x - 0.8, 1 + yOffset, z), 0, 0, 0, 0, 1);
                            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x - 0.5, 1.2 + yOffset, z), 0, 0, 0, 0, 
                                new Particle.DustOptions(Color.fromRGB(0x00AAFF), 0.8f));
                        }
                    }
                }
                
                // Hand aura
                for (int i = 0; i < 15; i++) {
                    double angle = Math.toRadians(ticks * 20 + i * 24);
                    double radius = 0.8;
                    double x = Math.cos(angle) * radius;
                    double y = Math.sin(angle) * radius;
                    
                    if (type.equals("wind")) {
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(x, 0.5 + y, 1.2), 0, 0, 0, 0, 1);
                        }
                    } else {
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(x, 0.5 + y, 1.2), 0, 0, 0, 0, 1);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void startAttackAnimation(Player player, String type, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        new BukkitRunnable() {
            int attackTicks = 0;
            int lastAttack = 0;
            
            @Override
            public void run() {
                if (!activeForms.containsKey(uuid) || attackTicks >= 200) {
                    this.cancel();
                    return;
                }
                
                // Auto attack every 20 ticks (1 second)
                if (attackTicks - lastAttack >= 20) {
                    performGroundSlam(player, type, plugin);
                    lastAttack = attackTicks;
                }
                
                // Keep player floating
                if (player.getLocation().getY() < player.getWorld().getHighestBlockYAt(player.getLocation()) + 4) {
                    player.setVelocity(new Vector(0, 0.2, 0));
                }
                
                attackTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void performGroundSlam(Player player, String type, MagicFruits plugin) {
        Location playerLoc = player.getLocation();
        Location groundLoc = playerLoc.clone().subtract(0, 3, 0);
        
        // Create massive fist effect
        createFistEffect(groundLoc, type, plugin);
        
        // Damage all entities in 5x5 area
        for (Entity entity : player.getNearbyEntities(5, 8, 5)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity target = (LivingEntity) entity;
                
                double damage = type.equals("wind") ? 6 : 8;
                double knockback = type.equals("wind") ? 1.5 : 2.0;
                
                target.damage(damage, player);
                
                // Knockback away from center
                Vector direction = target.getLocation().toVector().subtract(playerLoc.toVector()).normalize();
                direction.setY(0.5);
                target.setVelocity(direction.multiply(knockback));
                
                // Apply effects based on type
                if (type.equals("wind")) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
                    }
                } else {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation(), 40, 0.5, 0.5, 0.5, 0.1);
                    }
                    if (plugin.getDataManager().isSoundsEnabled()) {
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.8f);
                    }
                }
                
                // Cinematic hit effect
                if (plugin.getDataManager().isParticlesEnabled()) {
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 20, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
        
        // Shockwave effect
        createShockwave(playerLoc, type, plugin);
        
        // Play sound
        if (plugin.getDataManager().isSoundsEnabled()) {
            if (type.equals("wind")) {
                player.getWorld().playSound(playerLoc, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.2f, 0.7f);
            } else {
                player.getWorld().playSound(playerLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.6f);
            }
        }
    }
    
    private void createFistEffect(Location loc, String type, MagicFruits plugin) {
        // Create massive fist particles (5x5 area)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 2; y++) {
                    Location fistLoc = loc.clone().add(x, y, z);
                    double distance = Math.sqrt(x*x + z*z);
                    
                    if (distance <= 2.5) {
                        if (type.equals("wind")) {
                            if (plugin.getDataManager().isParticlesEnabled()) {
                                fistLoc.getWorld().spawnParticle(Particle.CLOUD, fistLoc, 3, 0.2, 0.2, 0.2, 0.05);
                                fistLoc.getWorld().spawnParticle(Particle.END_ROD, fistLoc, 2, 0.1, 0.1, 0.1, 0.05);
                            }
                        } else {
                            if (plugin.getDataManager().isParticlesEnabled()) {
                                fistLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, fistLoc, 5, 0.2, 0.2, 0.2, 0.05);
                                fistLoc.getWorld().spawnParticle(Particle.DUST, fistLoc, 3, 0.1, 0.1, 0.1, 
                                    new Particle.DustOptions(Color.fromRGB(0x00AAFF), 0.6f));
                            }
                        }
                    }
                }
            }
        }
        
        // Create giant fist outline
        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            double radius = 2.5;
            double x = Math.cos(rad) * radius;
            double z = Math.sin(rad) * radius;
            
            for (int y = 0; y <= 2; y++) {
                Location edgeLoc = loc.clone().add(x, y, z);
                if (type.equals("wind")) {
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        edgeLoc.getWorld().spawnParticle(Particle.CLOUD, edgeLoc, 2, 0.1, 0.1, 0.1, 0.02);
                    }
                } else {
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        edgeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, edgeLoc, 3, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }
        }
    }
    
    private void createShockwave(Location loc, String type, MagicFruits plugin) {
        new BukkitRunnable() {
            int radius = 0;
            
            @Override
            public void run() {
                if (radius > 5) {
                    this.cancel();
                    return;
                }
                
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    
                    if (type.equals("wind")) {
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(x, 0.1, z), 0, 0, 0, 0, 1);
                        }
                    } else {
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(x, 0.1, z), 0, 0, 0, 0, 1);
                            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 0, 0, 0, 0, 
                                new Particle.DustOptions(Color.fromRGB(0x00AAFF), 0.5f));
                        }
                    }
                }
                
                radius++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void deactivateForm(Player player) {
        UUID uuid = player.getUniqueId();
        ActiveForm form = activeForms.remove(uuid);
        
        if (form != null) {
            // Remove potion effects
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            
            // Final explosion effect
            MagicFruits plugin = MagicFruits.getInstance();
            if (plugin.getDataManager().isParticlesEnabled()) {
                for (int i = 0; i < 100; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random() * 3;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().clone().add(x, 1, z), 0, 0, 0, 0, 1);
                }
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0f, 0.5f);
            }
            
            player.sendMessage("§c§l⚠ §fYour monster form has ended!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Wind Monster (10s, floating, ground slam attacks, 60s cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Storm Monster (10s, floating, electric ground slam, 60s cooldown)";
    }
                                    }

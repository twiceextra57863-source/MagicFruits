package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DraculaBitesAbility implements Ability, Listener {
    
    private final Map<UUID, Long> bloodPhaseCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> batTransformCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, BloodPhaseData> activeBloodPhase = new ConcurrentHashMap<>();
    private final Map<UUID, BatTransformData> activeBatTransform = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> hitCounter = new ConcurrentHashMap<>();
    private final Map<UUID, Long> biteCooldown = new ConcurrentHashMap<>();
    
    private static class BloodPhaseData {
        long expiryTime;
        BloodPhaseData(long expiry) { this.expiryTime = expiry; }
    }
    
    private static class BatTransformData {
        Player player;
        Bat batEntity;
        long expiryTime;
        
        BatTransformData(Player player, Bat bat, long expiry) {
            this.player = player;
            this.batEntity = bat;
            this.expiryTime = expiry;
        }
    }
    
    public DraculaBitesAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activeBloodPhase.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                
                activeBatTransform.forEach((uuid, data) -> {
                    if (data.expiryTime <= now) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) revertFromBat(p, MagicFruits.getInstance());
                    }
                });
                
                bloodPhaseCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
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
        if (bloodPhaseCooldown.getOrDefault(uuid, 0L) > System.currentTimeMillis()) {
            long remaining = (bloodPhaseCooldown.get(uuid) - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c§l⚠ §fBlood Phase on cooldown! §7(" + remaining + "s)");
            return;
        }
        
        activeBloodPhase.put(uuid, new BloodPhaseData(System.currentTimeMillis() + 15000));
        bloodPhaseCooldown.put(uuid, System.currentTimeMillis() + 45000);
        hitCounter.put(uuid, 0);
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
        
        createBloodParticles(player, plugin);
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
        }
        
        player.sendTitle("§4§l🩸 BLOOD PHASE! 🩸", "§eStrength boost & Life steal!", 10, 40, 10);
    }

    private void executeBatTransform(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        if (batTransformCooldown.getOrDefault(uuid, 0L) > System.currentTimeMillis()) {
            long remaining = (batTransformCooldown.get(uuid) - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c§l⚠ §fBat Transform on cooldown! §7(" + remaining + "s)");
            return;
        }

        if (activeBatTransform.containsKey(uuid)) return;

        Bat bat = (Bat) player.getWorld().spawnEntity(player.getLocation(), EntityType.BAT);
        bat.setAwake(true);
        bat.setInvulnerable(true);
        bat.setSilent(true);

        BatTransformData data = new BatTransformData(player, bat, System.currentTimeMillis() + 15000);
        activeBatTransform.put(uuid, data);
        batTransformCooldown.put(uuid, System.currentTimeMillis() + 60000);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 310, 1, false, false));
        player.setAllowFlight(true);
        player.setFlying(true);

        player.sendTitle("§8§l🦇 BAT FORM 🦇", "§eFly and Bite enemies!", 10, 40, 10);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeBatTransform.containsKey(uuid)) {
                    this.cancel();
                    return;
                }
                
                bat.teleport(player.getLocation());
                player.setVelocity(player.getLocation().getDirection().multiply(0.7));

                if (plugin.getDataManager().isParticlesEnabled()) {
                    player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void revertFromBat(Player player, MagicFruits plugin) {
        BatTransformData data = activeBatTransform.remove(player.getUniqueId());
        if (data != null) {
            if (data.batEntity != null) data.batEntity.remove();
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setFlying(false);
            if (player.getGameMode() != GameMode.CREATIVE) player.setAllowFlight(false);
            
            createSpiralParticles(player.getLocation(), plugin);
            player.sendMessage("§8§l🦇 §fReverted to Human form!");
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);
        }
    }

    private void createBloodParticles(Player player, MagicFruits plugin) {
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t++ > 75 || !activeBloodPhase.containsKey(player.getUniqueId())) { this.cancel(); return; }
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 
                    new Particle.DustOptions(Color.fromRGB(0x8B0000), 1.0f));
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void createSpiralParticles(Location center, MagicFruits plugin) {
        for (int i = 0; i < 360; i += 20) {
            double rad = Math.toRadians(i);
            center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(Math.cos(rad), 1, Math.sin(rad)), 5, 0, 0, 0, 0.1);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player p = (Player) event.getDamager();
        UUID uuid = p.getUniqueId();

        if (activeBloodPhase.containsKey(uuid)) {
            int hits = hitCounter.getOrDefault(uuid, 0) + 1;
            if (hits >= 3) {
                // FIXED: GENERIC_MAX_HEALTH changed to MAX_HEALTH for 1.21+
                double maxHealth = p.getAttribute(Attribute.MAX_HEALTH).getValue();
                p.setHealth(Math.min(p.getHealth() + 2, maxHealth));
                p.sendMessage("§4§l🩸 §fLife Steal! +1 Heart");
                hits = 0;
            }
            hitCounter.put(uuid, hits);
        }
    }

    @EventHandler
    public void onBite(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!activeBatTransform.containsKey(p.getUniqueId())) return;
        if (!event.getAction().name().contains("LEFT_CLICK")) return;

        long now = System.currentTimeMillis();
        if (now - biteCooldown.getOrDefault(p.getUniqueId(), 0L) < 1000) return;

        for (Entity e : p.getNearbyEntities(4, 4, 4)) {
            if (e instanceof LivingEntity && !e.equals(p)) {
                ((LivingEntity) e).damage(4.0, p);
                // FIXED: GENERIC_MAX_HEALTH changed to MAX_HEALTH for 1.21+
                double maxHealth = p.getAttribute(Attribute.MAX_HEALTH).getValue();
                p.setHealth(Math.min(p.getHealth() + 1, maxHealth));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.2f);
                p.sendMessage("§8§l🦇 §fBite! Healed 0.5 Heart.");
                biteCooldown.put(p.getUniqueId(), now);
                break;
            }
        }
    }

    @Override
    public String getPrimaryDescription() { return "Blood Phase (15s, Heal on every 3rd hit, 45s CD)"; }
    @Override
    public String getSecondaryDescription() { return "Bat Form (15s, Fly and Bite to heal, 60s CD)"; }
}

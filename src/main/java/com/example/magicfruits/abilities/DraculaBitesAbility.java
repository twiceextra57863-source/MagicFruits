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

public class DraculaBitesAbility implements Ability, Listener {
    
    private final Map<UUID, Long> batTransformCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, BatTransformData> activeTransform = new ConcurrentHashMap<>();
    
    private static class BatTransformData {
        Player player;
        Bat batEntity; // Actual bat entity reference
        long expiryTime;
        
        BatTransformData(Player player, Bat bat, long expiry) {
            this.player = player;
            this.batEntity = bat;
            this.expiryTime = expiry;
        }
    }
    
    public DraculaBitesAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        // Cleanup task
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                activeTransform.forEach((uuid, data) -> {
                    if (data.expiryTime <= now) {
                        revertFromBat(data.player);
                    }
                });
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

    private void executeBatTransform(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Cooldown check
        if (batTransformCooldown.getOrDefault(uuid, 0L) > System.currentTimeMillis()) {
            long remaining = (batTransformCooldown.get(uuid) - System.currentTimeMillis()) / 1000;
            player.sendMessage("§c§l⚠ §fBat Transform on cooldown! §7(" + remaining + "s)");
            return;
        }
        
        if (activeTransform.containsKey(uuid)) return;

        // 1. Spawn the Bat Entity
        Bat bat = (Bat) player.getWorld().spawnEntity(player.getLocation(), EntityType.BAT);
        bat.setAwake(true);
        bat.setInvulnerable(true);
        bat.setSilent(true);

        // 2. Set Data (15 Seconds)
        long durationMillis = 15000;
        BatTransformData data = new BatTransformData(player, bat, System.currentTimeMillis() + durationMillis);
        activeTransform.put(uuid, data);
        batTransformCooldown.put(uuid, System.currentTimeMillis() + 60000); // 60s cooldown

        // 3. Transform Effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 310, 1, false, false));
        player.setAllowFlight(true);
        player.setFlying(true);
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        }

        player.sendTitle("§8§l🦇 BAT FORM 🦇", "§eControl the bat for 15s!", 10, 40, 10);

        // 4. Movement Logic (Syncing Bat to Player)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!activeTransform.containsKey(uuid) || ticks > 300) { // 300 ticks = 15s
                    revertFromBat(player);
                    this.cancel();
                    return;
                }

                // Make the Bat stay at the player's position
                bat.teleport(player.getLocation());
                
                // Flight Control: Increase velocity in looking direction
                Vector direction = player.getLocation().getDirection();
                player.setVelocity(direction.multiply(0.6)); // Bat-like speed control

                if (ticks % 5 == 0 && plugin.getDataManager().isParticlesEnabled()) {
                    player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void revertFromBat(Player player) {
        UUID uuid = player.getUniqueId();
        BatTransformData data = activeTransform.remove(uuid);
        
        if (data != null) {
            if (data.batEntity != null) data.batEntity.remove();
            
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.setFlying(false);
            if (player.getGameMode() != GameMode.CREATIVE) player.setAllowFlight(false);
            
            player.sendMessage("§8§l🦇 §fTransformation ended!");
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);
        }
    }

    // Prevents player from taking damage while in bat form
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (activeTransform.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // Rest of your Blood Phase and Particle methods stay the same...
    // (Ensure they are present in your final class)

    @Override
    public String getPrimaryDescription() {
        return "Blood Phase (15s, Strength + Speed boost, 45s cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Bat Transform (15s, become a bat and fly, 60s cooldown)";
    }
}

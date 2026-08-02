package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.block.Block;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;

public class ThunderAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Lightning strike
            Block targetBlock = player.getTargetBlock(null, 50);
            if (targetBlock != null) {
                org.bukkit.Location strikeLoc = targetBlock.getLocation();
                player.getWorld().strikeLightning(strikeLoc);

                if (plugin.getDataManager().isParticlesEnabled()) {
                    // Epic customized lightning blast particles
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc, 150, 2, 2, 2, 0.5);
                    player.getWorld().spawnParticle(Particle.CLOUD, strikeLoc, 50, 1, 2, 1, 0.1);
                    player.getWorld().spawnParticle(Particle.EXPLOSION, strikeLoc, 5, 0.5, 0.5, 0.5, 0);
                    player.getWorld().spawnParticle(Particle.FLASH, strikeLoc, 10, 1, 1, 1, 0);
                }

                if (plugin.getDataManager().isSoundsEnabled()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
                player.sendMessage("§3§l⚡ §fYou call down lightning from the heavens!");
            }
        } else {
            // Primary ability: Conduit Power + Dolphin's Grace
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 300, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 400, 1));
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§3§l⚡ THUNDER FRUIT §3§l⚡"),
                Component.text("§eYou command the storms!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§3§l⚡ §fThe power of storms flows through your veins!");

            // Run storm aura animation
            if (plugin.getDataManager().isParticlesEnabled()) {
                new org.bukkit.scheduler.BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 100 || !player.isOnline()) { // 5 seconds
                            this.cancel();
                            return;
                        }

                        org.bukkit.Location loc = player.getLocation();
                        // Crackling ring on the ground
                        double radius = 1.2;
                        double angle = ticks * 0.4;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(x, 0.2, z), 2, 0, 0, 0, 0.05);

                        // Spiral electric helix ascending
                        double y = (ticks % 20) * 0.1;
                        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(z, y, x), 2, 0.1, 0.1, 0.1, 0.05);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(x, y, z), 1, 0, 0, 0, 0.02);

                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Channel the power of storms (15s conduit power, 20s dolphin's grace)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Strike down lightning from the sky";
    }
}

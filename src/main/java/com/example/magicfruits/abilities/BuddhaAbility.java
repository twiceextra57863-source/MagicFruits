package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;

public class BuddhaAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Resistance IV for 5 seconds
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4));
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            }
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation(), 100, 1, 1, 1, 0.5);
            }
            player.sendMessage("§a§l✔ §fBuddha's protection surrounds you!");
        } else {
            // Primary ability: Strength II + Resistance I for 10 seconds
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                Location loc = player.getLocation();
                for (int i = 0; i < 50; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random() * 2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    player.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(x, 1 + Math.random(), z), 0, 0, 0, 0, 1);
                }
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§6§l✦ BUDDHA FRUIT §6§l✦"),
                Component.text("§eDivine strength flows through you!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§a§l✔ §fYou have been blessed with divine power!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Grants divine strength and resilience (10s)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Grants temporary invulnerability (5s)";
    }
}

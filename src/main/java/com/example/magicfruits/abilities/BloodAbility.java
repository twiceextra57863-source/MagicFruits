package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;

public class BloodAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Life drain
            player.getNearbyEntities(5, 5, 5).forEach(entity -> {
                if (entity instanceof Player) {
                    ((Player) entity).damage(4);
                    player.setHealth(Math.min(20, player.getHealth() + 2));
                }
            });
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
            }
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 100, 2, 2, 2, 0.1);
            }
            player.sendMessage("§4§l🩸 §fYou drain life from nearby enemies!");
        } else {
            // Primary ability: Wither effect
            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 2));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 100, 1, 1, 1, 0.2);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§4§l🩸 BLOOD FRUIT §4§l🩸"),
                Component.text("§eYou sacrifice for power!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§4§l🩸 §fYou feel the power of blood flowing through you!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Sacrifice vitality for power (5s wither, instant heal)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Drain life from nearby entities";
    }
}

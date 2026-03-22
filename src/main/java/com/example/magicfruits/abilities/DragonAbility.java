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

public class DragonAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Fire explosion
            player.getWorld().createExplosion(player.getLocation(), 3, false, false, player);
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            }
            player.sendMessage("§c§l🐉 §fYou unleash the dragon's wrath!");
        } else {
            // Primary ability: Fire Resistance + Strength
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 400, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100, 1, 1, 1, 0.2);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§c§l🐉 DRAGON FRUIT §c§l🐉"),
                Component.text("§eYou embrace the dragon's power!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§c§l🐉 §fYou feel the ancient dragon's power within you!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Imbues you with dragon's fire resistance and strength (20s)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Unleashes a fiery explosion";
    }
}

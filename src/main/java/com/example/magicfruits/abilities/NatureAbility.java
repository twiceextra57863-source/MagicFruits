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

public class NatureAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Nature's blessing
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 100, 2, 2, 2);
            }
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0f, 1.0f);
            }
            player.sendMessage("§2§l🌿 §fNature's blessing surrounds you!");
        } else {
            // Primary ability: Saturation + Luck
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 400, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 600, 1));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 150, 2, 2, 2, 0.2);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§2§l🌿 NATURE FRUIT §2§l🌿"),
                Component.text("§eYou become one with nature!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§2§l🌿 §fNature's energy flows through you!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Sustain yourself with nature's bounty (20s saturation, 30s luck)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Summon nature's blessing";
    }
}

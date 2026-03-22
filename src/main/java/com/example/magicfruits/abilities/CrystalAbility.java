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

public class CrystalAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Crystal shield with particles
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 1, 1, 1, 0.5);
            }
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
            }
            player.sendMessage("§b§l💎 §fCrystal shield surrounds you!");
        } else {
            // Primary ability: Speed III + Jump Boost II
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 300, 1));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 150, 1, 1, 1, 0.3);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
            
            player.showTitle(Title.title(
                Component.text("§b§l💎 CRYSTAL FRUIT §b§l💎"),
                Component.text("§eCrystalline speed flows through you!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§b§l💎 §fYou feel the power of crystals flowing through you!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Increases movement and jumping capabilities (15s)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Creates a protective crystal shield";
    }
}

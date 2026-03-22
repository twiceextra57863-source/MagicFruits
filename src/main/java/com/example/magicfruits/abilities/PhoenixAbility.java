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

public class PhoenixAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Full heal and fire extinguish
            player.setHealth(20);
            player.setFireTicks(0);
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 200, 1, 1, 1, 0.5);
            }
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
            }
            player.sendMessage("§6§l🔥 §fYou rise from the ashes, fully healed!");
        } else {
            // Primary ability: Regeneration III + Absorption II
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 2));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100, 1, 1, 1, 0.1);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§6§l🔥 PHOENIX FRUIT §6§l🔥"),
                Component.text("§eThe phoenix's flames heal you!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§6§l🔥 §fThe phoenix's flames begin to heal your wounds!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Regenerates health rapidly (15s) and grants absorption hearts";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Rise from the ashes with full health";
    }
}

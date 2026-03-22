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

public class VoidAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Random teleport
            Location randomLoc = player.getLocation().add(Math.random() * 20 - 10, 0, Math.random() * 20 - 10);
            randomLoc.setY(player.getWorld().getHighestBlockYAt(randomLoc));
            player.teleport(randomLoc);
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
            player.sendMessage("§5§l🌑 §fYou vanish into the void and reappear elsewhere!");
        } else {
            // Primary ability: Invisibility + Night Vision
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 1));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 50, 1, 1, 1, 0.1);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§5§l🌑 VOID FRUIT §5§l🌑"),
                Component.text("§eYou embrace the darkness!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§5§l🌑 §fYou become one with the void!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Embrace the shadows themselves (10s invisibility, 30s night vision)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Teleport through shadows";
    }
}

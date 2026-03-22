package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

public class StarAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Temporary flight
            player.setAllowFlight(true);
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
            }
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 1, 1, 1, 0.2);
            }
            player.sendMessage("§d§l⭐ §fYou gain the power of flight for 5 seconds!");
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage("§c§l⚠ §fYour flight power has ended!");
                }
            }.runTaskLater(plugin, 100);
        } else {
            // Primary ability: Glowing + Levitation
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 400, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 1));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 150, 1, 1, 1, 0.3);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
            
            player.showTitle(Title.title(
                Component.text("§d§l⭐ STAR FRUIT §d§l⭐"),
                Component.text("§eYou harness cosmic energy!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§d§l⭐ §fCosmic energy flows through your body!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Glow with celestial energy (20s glowing, 5s levitation)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Grant temporary flight (5s)";
    }
}

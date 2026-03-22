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
                player.getWorld().strikeLightning(targetBlock.getLocation());
                if (plugin.getDataManager().isSoundsEnabled()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
                player.sendMessage("§3§l⚡ §fYou call down lightning from the heavens!");
            }
        } else {
            // Primary ability: Conduit Power + Dolphin's Grace
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 300, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 400, 1));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 100, 1, 1, 1, 0.2);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§3§l⚡ THUNDER FRUIT §3§l⚡"),
                Component.text("§eYou command the storms!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§3§l⚡ §fThe power of storms flows through your veins!");
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

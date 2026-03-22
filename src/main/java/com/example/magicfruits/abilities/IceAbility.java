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

public class IceAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Freeze aura
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, player.getLocation(), 200, 3, 1, 3);
            }
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            }
            player.sendMessage("§b§l❄️ §fIce spreads from your body!");
        } else {
            // Primary ability: Slowness + Water Breathing
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 600, 1));
            
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 150, 1, 1, 1, 0.1);
            }
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            }
            
            player.showTitle(Title.title(
                Component.text("§b§l❄️ ICE FRUIT §b§l❄️"),
                Component.text("§eIce courses through your veins!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            
            player.sendMessage("§b§l❄️ §fYou feel the cold embrace of ice!");
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Chill your foes to the bone (10s slowness, 30s water breathing)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Freeze the surrounding area";
    }
}

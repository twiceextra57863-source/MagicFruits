package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import com.example.magicfruits.gui.StealGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;

public class ThiefAbility implements Ability {
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            // Secondary ability: Invisibility for 10 seconds
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 1));
            
            if (plugin.getDataManager().isSoundsEnabled()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
            if (plugin.getDataManager().isParticlesEnabled()) {
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation(), 50, 0.5, 1, 0.5, 0.1);
            }
            player.sendMessage("§8§l🎭 §fYou have become invisible for 10 seconds!");
        } else {
            // Primary ability: Open steal GUI
            new StealGUI(plugin).open(player);
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Open ability steal menu (2min cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Become invisible for 10 seconds";
    }
}

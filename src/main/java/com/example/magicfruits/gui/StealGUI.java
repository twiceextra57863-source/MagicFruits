package com.example.magicfruits.gui;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class StealGUI implements Listener {
    
    private final MagicFruits plugin;
    private final Map<UUID, Long> stealCooldown = new HashMap<>();
    
    public StealGUI(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player thief) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l🎭 §6§lSTEAL ABILITY §8§l🎭");
        
        // Border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        // Add online players
        int slot = 19;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(thief)) continue;
            
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(Component.text("§e§l" + target.getName()));
            
            // Check if target has any fruit
            boolean hasFruit = false;
            FruitType targetFruit = null;
            for (ItemStack item : target.getInventory().getContents()) {
                if (item != null) {
                    targetFruit = FruitType.fromItem(item);
                    if (targetFruit != null) {
                        hasFruit = true;
                        break;
                    }
                }
            }
            
            List<Component> lore = new ArrayList<>();
            if (hasFruit && targetFruit != null) {
                lore.add(Component.text("§a✓ Has: " + targetFruit.getDisplayName()));
                lore.add(Component.empty());
                lore.add(Component.text("§eClick to steal their ability!"));
                lore.add(Component.text("§c⚠ This will put their ability on cooldown!"));
            } else {
                lore.add(Component.text("§c✗ No magical fruit found!"));
                lore.add(Component.empty());
                lore.add(Component.text("§7Cannot steal from this player"));
            }
            meta.lore(lore);
            head.setItemMeta(meta);
            gui.setItem(slot, head);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("§6§l✦ ABILITY STEALER ✦"));
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("§7Steal a player's fruit ability!"));
        infoLore.add(Component.text("§7The victim will be frozen for 5 seconds"));
        infoLore.add(Component.text("§7You keep the ability for 20 seconds"));
        infoLore.add(Component.text("§7Cooldown: 2 minutes"));
        infoMeta.lore(infoLore);
        info.setItemMeta(infoMeta);
        gui.setItem(49, info);
        
        thief.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player thief)) return;
        if (!event.getView().getTitle().equals("§8§l🎭 §6§lSTEAL ABILITY §8§l🎭")) return;
        
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
        
        String targetName = net.md_5.bungee.api.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            thief.closeInventory();
            executeSteal(thief, target);
        }
    }
    
    private void executeSteal(Player thief, Player target) {
        UUID thiefId = thief.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        // Check cooldown (2 minutes)
        long lastSteal = stealCooldown.getOrDefault(thiefId, 0L);
        if (System.currentTimeMillis() - lastSteal < 120000) {
            long remaining = (120000 - (System.currentTimeMillis() - lastSteal)) / 1000;
            thief.sendMessage("§c§l⚠ §fSteal ability on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        // Find target's fruit and ability
        FruitType targetFruit = null;
        for (ItemStack item : target.getInventory().getContents()) {
            if (item != null) {
                targetFruit = FruitType.fromItem(item);
                if (targetFruit != null) break;
            }
        }
        
        if (targetFruit == null) {
            thief.sendMessage("§c§l⚠ §fThis player doesn't have any magical fruit!");
            return;
        }
        
        // Freeze all players within 30 blocks except thief and target
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(thief) || player.equals(target)) continue;
            if (player.getLocation().distance(thief.getLocation()) <= 30) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 128));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
                player.sendMessage("§c§l❄️ §fYou have been frozen by §e" + thief.getName() + "§f's thief ability!");
                if (plugin.getDataManager().isParticlesEnabled()) {
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 50, 1, 1, 1, 0.1);
                }
            }
        }
        
        // Put target's ability on cooldown
        plugin.getCooldownManager().setCooldown(targetId);
        target.sendMessage("§c§l⚠ §e" + thief.getName() + " §chas stolen your ability! It is now on cooldown!");
        
        // Give thief the stolen ability for 20 seconds
        plugin.setStolenAbility(thiefId, targetFruit.getAbility(), 20);
        thief.sendMessage("§a§l✓ §fYou have stolen §6" + targetFruit.getDisplayName() + "§f's ability!");
        thief.sendMessage("§eYou can use it for the next 20 seconds!");
        
        // Visual effects
        if (plugin.getDataManager().isParticlesEnabled()) {
            thief.getWorld().spawnParticle(Particle.PORTAL, thief.getLocation(), 100, 1, 1, 1, 0.5);
            target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 100, 1, 1, 1, 0.5);
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            thief.playSound(thief.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);
            target.playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.0f, 0.5f);
        }
        
        // Set cooldown for thief
        stealCooldown.put(thiefId, System.currentTimeMillis());
        
        // Freeze target for 5 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 128));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
        target.sendMessage("§c§l❄️ §fYou have been frozen for 5 seconds!");
    }
}

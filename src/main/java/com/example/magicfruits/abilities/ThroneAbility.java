package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ThroneAbility implements Ability, Listener {
    
    private final Map<UUID, ShieldData> activeShields = new HashMap<>();
    private final Map<UUID, WallData> activeWalls = new HashMap<>();
    private final Map<UUID, Long> shieldCooldown = new HashMap<>();
    private final Map<UUID, Long> wallCooldown = new HashMap<>();
    
    private static class ShieldData {
        long expiryTime;
        
        ShieldData(long expiry) {
            this.expiryTime = expiry;
        }
    }
    
    private static class WallData {
        List<Block> wallBlocks;
        List<Material> originalMaterials;
        long expiryTime;
        
        WallData(List<Block> blocks, List<Material> materials, long expiry) {
            this.wallBlocks = blocks;
            this.originalMaterials = materials;
            this.expiryTime = expiry;
        }
    }
    
    public ThroneAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        // Cleanup task for expired shields and walls
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                
                // Cleanup shields
                activeShields.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                
                // Cleanup walls
                for (Map.Entry<UUID, WallData> entry : activeWalls.entrySet()) {
                    if (entry.getValue().expiryTime <= now) {
                        restoreWall(entry.getValue());
                        activeWalls.remove(entry.getKey());
                    }
                }
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        if (isSecondary) {
            executeWallAbility(player, plugin);
        } else {
            executeShieldAbility(player, plugin);
        }
    }
    
    private void executeShieldAbility(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (30 seconds)
        long lastUse = shieldCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 30000) {
            long remaining = (30000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fShield ability on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        // Create shield
        activeShields.put(uuid, new ShieldData(System.currentTimeMillis() + 15000));
        shieldCooldown.put(uuid, System.currentTimeMillis());
        
        // Visual effects
        if (plugin.getDataManager().isParticlesEnabled()) {
            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                double radius = 1.5;
                double x = Math.cos(rad) * radius;
                double z = Math.sin(rad) * radius;
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().clone().add(x, 1, z), 0, 0, 0, 0, 1);
                player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().clone().add(x, 1.5, z), 0, 0, 0, 0, 1);
            }
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.0f);
        }
        
        player.sendTitle("§6§l🛡️ THRONE SHIELD ACTIVATED! 🛡️", 
            "§eDamage reflected for 15 seconds!", 10, 40, 10);
        player.sendMessage("§6§l🛡️ §fA royal shield surrounds you for 15 seconds!");
        player.sendMessage("§eAttacks will be reflected back to attackers!");
    }
    
    private void executeWallAbility(Player player, MagicFruits plugin) {
        UUID uuid = player.getUniqueId();
        
        // Check cooldown (45 seconds)
        long lastUse = wallCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastUse < 45000) {
            long remaining = (45000 - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c§l⚠ §fWall ability on cooldown! §7(" + remaining + " seconds remaining)");
            return;
        }
        
        // Get direction player is facing
        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock == null) {
            player.sendMessage("§c§l⚠ §fNo block in sight!");
            return;
        }
        
        // Create wall 3 blocks ahead
        List<Block> wallBlocks = new ArrayList<>();
        List<Material> originalMaterials = new ArrayList<>();
        
        BlockFace facing = getPlayerFacing(player);
        Block startBlock = targetBlock.getRelative(facing, 1);
        
        // Create a 3x3 wall
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                Block block;
                if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                    block = startBlock.getRelative(BlockFace.EAST, x).getRelative(BlockFace.UP, y);
                } else {
                    block = startBlock.getRelative(BlockFace.NORTH, x).getRelative(BlockFace.UP, y);
                }
                
                if (block.getType() != Material.AIR) {
                    wallBlocks.add(block);
                    originalMaterials.add(block.getType());
                    block.setType(Material.OBSIDIAN);
                    
                    // Particle effects
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.1);
                        block.getWorld().spawnParticle(Particle.LAVA, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        }
        
        activeWalls.put(uuid, new WallData(wallBlocks, originalMaterials, System.currentTimeMillis() + 15000));
        wallCooldown.put(uuid, System.currentTimeMillis());
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
        }
        
        player.sendTitle("§6§l🏰 THRONE WALL SUMMONED! 🏰", 
            "§eA mighty wall rises before you!", 10, 40, 10);
        player.sendMessage("§6§l🏰 §fA royal wall has been summoned!");
        player.sendMessage("§eIt will disappear after 15 seconds!");
        
        // Knockback and damage nearby entities
        for (Entity entity : player.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity living = (LivingEntity) entity;
                
                // Calculate knockback direction away from player
                Vector direction = living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                direction.setY(0.5);
                living.setVelocity(direction.multiply(2.0));
                
                // Damage
                living.damage(6, player);
                
                if (plugin.getDataManager().isParticlesEnabled()) {
                    living.getWorld().spawnParticle(Particle.CRIT, living.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                }
                
                if (plugin.getDataManager().isSoundsEnabled()) {
                    living.getWorld().playSound(living.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
                }
            }
        }
    }
    
    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        
        if (yaw >= 315 || yaw < 45) return BlockFace.NORTH;
        if (yaw >= 45 && yaw < 135) return BlockFace.EAST;
        if (yaw >= 135 && yaw < 225) return BlockFace.SOUTH;
        if (yaw >= 225 && yaw < 315) return BlockFace.WEST;
        
        return BlockFace.NORTH;
    }
    
    private void restoreWall(WallData wallData) {
        for (int i = 0; i < wallData.wallBlocks.size(); i++) {
            Block block = wallData.wallBlocks.get(i);
            Material originalMaterial = wallData.originalMaterials.get(i);
            block.setType(originalMaterial);
            
            // Particle effects on removal
            block.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.1);
        }
        
        if (MagicFruits.getInstance().getDataManager().isSoundsEnabled()) {
            if (!wallData.wallBlocks.isEmpty()) {
                wallData.wallBlocks.get(0).getWorld().playSound(wallData.wallBlocks.get(0).getLocation(), 
                    Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if damage is from a player
        if (!(event.getDamager() instanceof Player)) return;
        
        Player attacker = (Player) event.getDamager();
        Entity victim = event.getEntity();
        
        // Check if victim has active shield
        if (victim instanceof Player) {
            Player defender = (Player) victim;
            UUID uuid = defender.getUniqueId();
            
            if (activeShields.containsKey(uuid)) {
                // Calculate reflected damage (full damage back to attacker)
                double originalDamage = event.getDamage();
                double reflectedDamage = originalDamage;
                double reducedDamageToDefender = originalDamage * 0.5;
                
                // Apply reduced damage to defender
                event.setDamage(reducedDamageToDefender);
                
                // Apply reflected damage to attacker
                attacker.damage(reflectedDamage, defender);
                
                // Visual effects
                if (MagicFruits.getInstance().getDataManager().isParticlesEnabled()) {
                    defender.getWorld().spawnParticle(Particle.CRIT, defender.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                    defender.getWorld().spawnParticle(Particle.ENCHANT, attacker.getLocation(), 30, 0.5, 1, 0.5, 0.1);
                }
                
                if (MagicFruits.getInstance().getDataManager().isSoundsEnabled()) {
                    defender.getWorld().playSound(defender.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
                }
                
                // Send message
                defender.sendMessage("§6§l🛡️ §fYou reflected §e" + (int)reflectedDamage + "§f damage to §e" + attacker.getName() + "§f!");
                attacker.sendMessage("§c§l⚔️ §fYour attack was reflected by §e" + defender.getName() + "§f's throne shield!");
            }
        }
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Create throne shield (15s duration, reflects damage, 50% damage reduction)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Summon royal wall (3x3 wall, 15s duration, knockback + damage)";
    }
}

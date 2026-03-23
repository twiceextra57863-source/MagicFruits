package com.example.magicfruits.abilities;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThiefAbility implements Ability, Listener {
    
    private final Map<UUID, Long> stealCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, StolenAbility> stolenAbilities = new ConcurrentHashMap<>();
    private final Map<UUID, FreezeData> frozenPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> victimCooldown = new ConcurrentHashMap<>();
    
    private static class StolenAbility {
        Ability ability;
        FruitType fruit;
        long expiryTime;
        
        StolenAbility(Ability ability, FruitType fruit, long expiry) {
            this.ability = ability;
            this.fruit = fruit;
            this.expiryTime = expiry;
        }
    }
    
    private static class FreezeData {
        Location originalLocation;
        long freezeTime;
        int freezeDuration;
        
        FreezeData(Location loc, long time, int duration) {
            this.originalLocation = loc;
            this.freezeTime = time;
            this.freezeDuration = duration;
        }
    }
    
    public ThiefAbility() {
        MagicFruits.getInstance().getServer().getPluginManager().registerEvents(this, MagicFruits.getInstance());
        
        // Cleanup tasks
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                stolenAbilities.entrySet().removeIf(entry -> entry.getValue().expiryTime <= now);
                frozenPlayers.entrySet().removeIf(entry -> entry.getValue().freezeTime + (entry.getValue().freezeDuration * 1000L) <= now);
                victimCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
            }
        }.runTaskTimer(MagicFruits.getInstance(), 20L, 20L);
    }
    
    @Override
    public void execute(Player player, boolean isSecondary) {
        if (isSecondary) {
            executeStealAbility(player);
        } else {
            openStealGUI(player);
        }
    }
    
    private void openStealGUI(Player thief) {
        MagicFruits plugin = MagicFruits.getInstance();
        UUID uuid = thief.getUniqueId();
        
        // Check cooldown (2 minutes = 120 seconds)
        long lastSteal = stealCooldown.getOrDefault(uuid, 0L);
        long remainingTime = (120000 - (System.currentTimeMillis() - lastSteal)) / 1000;
        
        if (System.currentTimeMillis() - lastSteal < 120000) {
            thief.sendMessage("§c§l⚠ §fSteal ability on cooldown! §7(" + remainingTime + " seconds remaining)");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l🎭 §6§lSTEAL ABILITY §8§l🎭");
        
        // Create decorative border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        // Title decoration
        ItemStack titleDeco = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta titleMeta = titleDeco.getItemMeta();
        titleMeta.setDisplayName("§8§l══════════ §6§l✦ §f§lABILITY THIEVES §6§l✦ §8§l══════════");
        titleDeco.setItemMeta(titleMeta);
        gui.setItem(4, titleDeco);
        
        // Add online players with advanced scanning
        int slot = 19;
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);
        
        for (Player target : players) {
            if (target.equals(thief)) continue;
            
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName("§e§l" + target.getName());
            
            // Advanced fruit detection with priority scanning
            FruitType targetFruit = null;
            int priority = Integer.MAX_VALUE;
            
            // Scan main hand first (highest priority)
            ItemStack mainHand = target.getInventory().getItemInMainHand();
            if (mainHand != null) {
                FruitType fruit = FruitType.fromItem(mainHand);
                if (fruit != null) {
                    targetFruit = fruit;
                    priority = 1;
                }
            }
            
            // Scan off hand (second priority)
            if (targetFruit == null) {
                ItemStack offHand = target.getInventory().getItemInOffHand();
                if (offHand != null) {
                    FruitType fruit = FruitType.fromItem(offHand);
                    if (fruit != null) {
                        targetFruit = fruit;
                        priority = 2;
                    }
                }
            }
            
            // Scan hotbar slots (priority 3-12)
            if (targetFruit == null) {
                for (int i = 0; i < 9; i++) {
                    ItemStack item = target.getInventory().getItem(i);
                    if (item != null) {
                        FruitType fruit = FruitType.fromItem(item);
                        if (fruit != null) {
                            targetFruit = fruit;
                            priority = 3 + i;
                            break;
                        }
                    }
                }
            }
            
            // Scan full inventory
            if (targetFruit == null) {
                for (ItemStack item : target.getInventory().getContents()) {
                    if (item != null) {
                        FruitType fruit = FruitType.fromItem(item);
                        if (fruit != null) {
                            targetFruit = fruit;
                            break;
                        }
                    }
                }
            }
            
            List<String> lore = new ArrayList<>();
            lore.add("§8§m-----------------------------");
            
            if (targetFruit != null) {
                boolean recentlyStolen = victimCooldown.containsKey(target.getUniqueId()) && 
                    victimCooldown.get(target.getUniqueId()) > System.currentTimeMillis();
                
                if (recentlyStolen) {
                    lore.add("§c⚠ ABILITY ON COOLDOWN ⚠");
                    lore.add("§7This player was recently stolen from!");
                    long remaining = (victimCooldown.get(target.getUniqueId()) - System.currentTimeMillis()) / 1000;
                    lore.add("§7Remaining: §e" + remaining + "s");
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                    gui.setItem(slot, head);
                    slot++;
                    if ((slot + 1) % 9 == 0) slot += 2;
                    if (slot > 43) break;
                    continue;
                }
                
                lore.add("§a✓ HAS: §6" + targetFruit.getDisplayName());
                lore.add("§7Priority: §e" + (priority <= 9 ? "Hotbar" : "Inventory"));
                lore.add("");
                lore.add("§e◆ CLICK TO STEAL ◆");
                lore.add("§7Steal their ability for 20 seconds");
                lore.add("§7Victim will be frozen for 5 seconds");
                lore.add("");
                lore.add("§c⚠ All nearby players will be frozen!");
                lore.add("§8§m-----------------------------");
            } else {
                lore.add("§c✗ NO MAGICAL FRUIT DETECTED");
                lore.add("");
                lore.add("§7This player doesn't have");
                lore.add("§7any magical fruit to steal!");
                lore.add("");
                lore.add("§8§m-----------------------------");
            }
            
            meta.setLore(lore);
            head.setItemMeta(meta);
            gui.setItem(slot, head);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        // Add info item
        ItemStack info = new ItemStack(Material.ENDER_EYE);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§l✦ STEAL MECHANICS ✦");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8§m-----------------------------");
        infoLore.add("§7• Steal any player's fruit ability");
        infoLore.add("§7• Victim's ability goes on cooldown");
        infoLore.add("§7• You keep the ability for §e20 seconds");
        infoLore.add("§7• All nearby players §cfrozen §7for §e5 seconds");
        infoLore.add("§7• §c2 minute §7cooldown on steal");
        infoLore.add("§8§m-----------------------------");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        gui.setItem(49, info);
        
        thief.openInventory(gui);
    }
    
    private void executeStealAbility(Player thief) {
        thief.sendMessage("§eUse the GUI to steal abilities!");
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("§8§l🎭 §6§lSTEAL ABILITY §8§l🎭")) return;
        
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
        
        Player thief = (Player) event.getWhoClicked();
        UUID thiefId = thief.getUniqueId();
        
        // Check cooldown again
        long lastSteal = stealCooldown.getOrDefault(thiefId, 0L);
        if (System.currentTimeMillis() - lastSteal < 120000) {
            long remaining = (120000 - (System.currentTimeMillis() - lastSteal)) / 1000;
            thief.sendMessage("§c§l⚠ §fSteal ability on cooldown! §7(" + remaining + " seconds remaining)");
            thief.closeInventory();
            return;
        }
        
        String targetName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            thief.sendMessage("§c§l⚠ §fTarget player is no longer online!");
            thief.closeInventory();
            return;
        }
        
        // Check if target was recently stolen from (30 second protection)
        if (victimCooldown.containsKey(target.getUniqueId()) && 
            victimCooldown.get(target.getUniqueId()) > System.currentTimeMillis()) {
            long remaining = (victimCooldown.get(target.getUniqueId()) - System.currentTimeMillis()) / 1000;
            thief.sendMessage("§c§l⚠ §fThis player was recently stolen from! §7(" + remaining + "s protection remaining)");
            thief.closeInventory();
            return;
        }
        
        // Find target's fruit
        FruitType targetFruit = null;
        for (ItemStack item : target.getInventory().getContents()) {
            if (item != null) {
                targetFruit = FruitType.fromItem(item);
                if (targetFruit != null) break;
            }
        }
        
        if (targetFruit == null) {
            thief.sendMessage("§c§l⚠ §fThis player doesn't have any magical fruit!");
            thief.closeInventory();
            return;
        }
        
        MagicFruits plugin = MagicFruits.getInstance();
        
        // Create advanced freeze effect for all nearby players
        List<Player> frozenList = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(thief) || player.equals(target)) continue;
            if (player.getLocation().distance(thief.getLocation()) <= 30) {
                freezePlayer(player, 5);
                frozenList.add(player);
                player.sendTitle("§b§l❄️ FROZEN! ❄️", 
                    "§e" + thief.getName() + " stole an ability!", 10, 60, 10);
            }
        }
        
        // Freeze victim for 5 seconds with advanced effects
        freezePlayer(target, 5);
        target.sendTitle("§c§l⚠ ABILITY STOLEN! ⚠", 
            "§eYour ability was stolen by " + thief.getName(), 10, 80, 10);
        
        // Create stunning particle vortex around thief
        createStealVortex(thief, target);
        
        // Put target's ability on cooldown (30 seconds)
        plugin.getCooldownManager().setCooldown(target.getUniqueId(), targetFruit);
        
        // Give thief the stolen ability for 20 seconds
        stolenAbilities.put(thiefId, new StolenAbility(
            targetFruit.getAbility(), 
            targetFruit, 
            System.currentTimeMillis() + 20000
        ));
        
        // Set cooldown for thief (2 minutes)
        stealCooldown.put(thiefId, System.currentTimeMillis());
        
        // Set victim protection (30 seconds)
        victimCooldown.put(target.getUniqueId(), System.currentTimeMillis() + 30000);
        
        // Visual and sound effects
        if (plugin.getDataManager().isParticlesEnabled()) {
            createStealParticles(thief, target);
        }
        
        if (plugin.getDataManager().isSoundsEnabled()) {
            thief.playSound(thief.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);
            target.playSound(target.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1.0f, 0.5f);
            for (Player frozen : frozenList) {
                frozen.playSound(frozen.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
            }
        }
        
        // Send messages
        thief.sendTitle("§a§l✓ ABILITY STOLEN! ✓", 
            "§eYou stole " + targetFruit.getDisplayName(), 10, 60, 10);
        thief.sendMessage("§a§l✓ §fYou have stolen §6" + targetFruit.getDisplayName() + "§f's ability!");
        thief.sendMessage("§eYou can use it for the next 20 seconds!");
        thief.sendMessage("§7Use it with §eRight Click §7or §eCrouch + Right Click");
        
        target.sendMessage("§c§l⚠ §e" + thief.getName() + " §chas stolen your ability!");
        target.sendMessage("§7Your ability is on cooldown for 30 seconds!");
        
        for (Player frozen : frozenList) {
            frozen.sendMessage("§b§l❄️ §fYou were frozen by §e" + thief.getName() + "§f's thief ability!");
        }
        
        // Schedule ability removal after 20 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (stolenAbilities.containsKey(thiefId)) {
                    StolenAbility stolen = stolenAbilities.get(thiefId);
                    if (stolen.expiryTime <= System.currentTimeMillis()) {
                        stolenAbilities.remove(thiefId);
                        if (thief.isOnline()) {
                            thief.sendMessage("§c§l⚠ §fYour stolen ability has expired!");
                            if (plugin.getDataManager().isSoundsEnabled()) {
                                thief.playSound(thief.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                            }
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 400L);
        
        thief.closeInventory();
    }
    
    private void freezePlayer(Player player, int seconds) {
        Location originalLoc = player.getLocation();
        frozenPlayers.put(player.getUniqueId(), new FreezeData(originalLoc, System.currentTimeMillis(), seconds));
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20, 255, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, seconds * 20, 128, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, seconds * 20, 1, false, false));
        
        MagicFruits plugin = MagicFruits.getInstance();
        if (plugin.getDataManager().isParticlesEnabled()) {
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= seconds * 20 || !frozenPlayers.containsKey(player.getUniqueId())) {
                        this.cancel();
                        return;
                    }
                    
                    Location loc = player.getLocation();
                    for (int i = 0; i < 360; i += 15) {
                        double rad = Math.toRadians(i);
                        double radius = 1.2;
                        double x = Math.cos(rad) * radius;
                        double z = Math.sin(rad) * radius;
                        loc.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, loc.clone().add(x, 0.5, z), 0, 0, 0, 0, 1);
                        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(x, 1.2, z), 0, 0, 0, 0, 1);
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }
    
    private void createStealVortex(Player thief, Player target) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) {
                    this.cancel();
                    return;
                }
                
                double angle = Math.toRadians(ticks * 15);
                double radius = 2.5;
                
                for (int i = 0; i < 3; i++) {
                    double offset = i * Math.PI * 2 / 3;
                    double x = Math.cos(angle + offset) * radius;
                    double z = Math.sin(angle + offset) * radius;
                    
                    Location vortexLoc = thief.getLocation().clone().add(x, 1 + ticks * 0.05, z);
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        vortexLoc.getWorld().spawnParticle(Particle.PORTAL, vortexLoc, 0, 0, 0, 0, 1);
                        vortexLoc.getWorld().spawnParticle(Particle.END_ROD, vortexLoc, 0, 0, 0, 0, 1);
                    }
                }
                
                // Create particle beam from target to thief
                if (ticks < 30) {
                    double progress = ticks / 30.0;
                    Location beamLoc = target.getLocation().add(0, 1, 0).clone()
                        .add(thief.getLocation().add(0, 1, 0).toVector()
                        .subtract(target.getLocation().add(0, 1, 0).toVector())
                        .multiply(progress));
                    
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        beamLoc.getWorld().spawnParticle(Particle.DRAGON_BREATH, beamLoc, 5, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void createStealParticles(Player thief, Player target) {
        MagicFruits plugin = MagicFruits.getInstance();
        
        // Explosion at thief
        thief.getWorld().spawnParticle(Particle.EXPLOSION, thief.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        
        // Ring around thief
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double radius = 2;
            double x = Math.cos(rad) * radius;
            double z = Math.sin(rad) * radius;
            thief.getWorld().spawnParticle(Particle.PORTAL, thief.getLocation().clone().add(x, 1, z), 0, 0, 0, 0, 1);
            thief.getWorld().spawnParticle(Particle.END_ROD, thief.getLocation().clone().add(x, 1.5, z), 0, 0, 0, 0, 1);
        }
        
        // Ring around target
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double radius = 1.5;
            double x = Math.cos(rad) * radius;
            double z = Math.sin(rad) * radius;
            target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().clone().add(x, 1, z), 0, 0, 0, 0, 1);
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().clone().add(x, 1.5, z), 0, 0, 0, 0, 1);
        }
    }
    
    public Ability getStolenAbility(Player player) {
        StolenAbility stolen = stolenAbilities.get(player.getUniqueId());
        if (stolen != null && stolen.expiryTime > System.currentTimeMillis()) {
            return stolen.ability;
        }
        return null;
    }
    
    @Override
    public String getPrimaryDescription() {
        return "Open steal GUI (steal any player's ability, 2min cooldown)";
    }
    
    @Override
    public String getSecondaryDescription() {
        return "Execute steal (use after selecting target from GUI)";
    }
}

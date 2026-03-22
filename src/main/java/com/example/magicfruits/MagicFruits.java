package com.example.magicfruits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MagicFruits extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    
    private static MagicFruits instance;
    private Map<UUID, Boolean> spinActive = new ConcurrentHashMap<>();
    private Map<UUID, Integer> spinTaskIds = new ConcurrentHashMap<>();
    private Map<UUID, FruitType> lastSelectedFruit = new ConcurrentHashMap<>();
    private Map<UUID, Long> abilityCooldown = new ConcurrentHashMap<>();
    private Map<UUID, Integer> cooldownTasks = new ConcurrentHashMap<>();
    private Map<UUID, FruitType> lastUsedFruit = new ConcurrentHashMap<>();
    
    private enum FruitType {
        BUDDHA_FRUIT("§e§l✨ §6§lBUDDHA FRUIT §e§l✨", 
                    "§7§oEnlightened by ancient wisdom",
                    PotionEffectType.STRENGTH,
                    PotionEffectType.RESISTANCE,
                    Color.fromRGB(0xFFD700),
                    "YELLOW"),
        
        CRYSTAL_FRUIT("§b§l💎 §3§lCRYSTAL FRUIT §b§l💎",
                     "§7§oPure crystalline power",
                     PotionEffectType.SPEED,
                     PotionEffectType.JUMP_BOOST,
                     Color.fromRGB(0x00FFFF),
                     "LIGHT_BLUE"),
        
        DRAGON_FRUIT("§c§l🐉 §4§lDRAGON FRUIT §c§l🐉",
                    "§7§oWrath of the ancient dragons",
                    PotionEffectType.FIRE_RESISTANCE,
                    PotionEffectType.STRENGTH,
                    Color.fromRGB(0xFF4444),
                    "RED"),
        
        PHOENIX_FRUIT("§6§l🔥 §e§lPHOENIX FRUIT §6§l🔥",
                     "§7§oReborn from eternal flames",
                     PotionEffectType.REGENERATION,
                     PotionEffectType.ABSORPTION,
                     Color.fromRGB(0xFFA500),
                     "ORANGE"),
        
        VOID_FRUIT("§5§l🌑 §8§lVOID FRUIT §5§l🌑",
                  "§7§oEmbrace the darkness within",
                  PotionEffectType.INVISIBILITY,
                  PotionEffectType.NIGHT_VISION,
                  Color.fromRGB(0xAA00AA),
                  "PURPLE"),
        
        THUNDER_FRUIT("§3§l⚡ §b§lTHUNDER FRUIT §3§l⚡",
                     "§7§oCommand the storm itself",
                     PotionEffectType.CONDUIT_POWER,
                     PotionEffectType.DOLPHINS_GRACE,
                     Color.fromRGB(0x44AAFF),
                     "LIGHT_BLUE"),
        
        NATURE_FRUIT("§2§l🌿 §a§lNATURE FRUIT §2§l🌿",
                    "§7§oOne with the natural world",
                    PotionEffectType.SATURATION,
                    PotionEffectType.LUCK,
                    Color.fromRGB(0x44FF44),
                    "GREEN"),
        
        ICE_FRUIT("§b§l❄️ §f§lICE FRUIT §b§l❄️",
                 "§7§oFreeze your enemies solid",
                 PotionEffectType.SLOWNESS,
                 PotionEffectType.WATER_BREATHING,
                 Color.fromRGB(0x88FFFF),
                 "LIGHT_BLUE"),
        
        STAR_FRUIT("§d§l⭐ §5§lSTAR FRUIT §d§l⭐",
                  "§7§oHarness cosmic energy",
                  PotionEffectType.GLOWING,
                  PotionEffectType.LEVITATION,
                  Color.fromRGB(0xFF88FF),
                  "MAGENTA"),
        
        BLOOD_FRUIT("§4§l🩸 §c§lBLOOD FRUIT §4§l🩸",
                   "§7§oSacrifice for ultimate power",
                   PotionEffectType.WITHER,
                   PotionEffectType.INSTANT_HEALTH,
                   Color.fromRGB(0xFF4444),
                   "RED");
        
        private final String displayName;
        private final String description;
        private final PotionEffectType primaryEffect;
        private final PotionEffectType secondaryEffect;
        private final Color dyeColor;
        private final String dyeMaterial;
        
        FruitType(String displayName, String description, PotionEffectType primary, PotionEffectType secondary, Color dyeColor, String dyeMaterial) {
            this.displayName = displayName;
            this.description = description;
            this.primaryEffect = primary;
            this.secondaryEffect = secondary;
            this.dyeColor = dyeColor;
            this.dyeMaterial = dyeMaterial;
        }
        
        public ItemStack createItem() {
            ItemStack item = new ItemStack(Material.valueOf(dyeMaterial + "_DYE"));
            ItemMeta meta = item.getItemMeta();
            
            meta.displayName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("§8§m----------------------------------------"));
            lore.add(Component.text("§7§l✦ §f§lMYSTICAL ARTIFACT §7§l✦"));
            lore.add(Component.text("§8§m----------------------------------------"));
            lore.add(Component.empty());
            lore.add(Component.text("§7§o" + description).decoration(TextDecoration.ITALIC, true));
            lore.add(Component.empty());
            lore.add(Component.text("§6§l✨ ABILITIES ✨"));
            lore.add(Component.text(" §e▶ §fRight Click: §7" + getAbilityDescription(true)));
            lore.add(Component.text(" §e▶ §fCrouch + Right Click: §7" + getAbilityDescription(false)));
            lore.add(Component.empty());
            lore.add(Component.text("§5§l⚡ KEYBINDS ⚡"));
            lore.add(Component.text(" §d• §fUse: §7Right Click"));
            lore.add(Component.text(" §d• §fSpecial: §7Sneak + Right Click"));
            lore.add(Component.empty());
            lore.add(Component.text("§8§m----------------------------------------"));
            lore.add(Component.text("§7§l✦ §f§lLEGENDARY FRUIT §7§l✦"));
            lore.add(Component.text("§8§m----------------------------------------"));
            
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }
        
        private String getAbilityDescription(boolean primary) {
            if (primary) {
                return switch (this) {
                    case BUDDHA_FRUIT -> "Grants divine strength and resilience";
                    case CRYSTAL_FRUIT -> "Increases movement and jumping capabilities";
                    case DRAGON_FRUIT -> "Imbues you with dragon's fire resistance and strength";
                    case PHOENIX_FRUIT -> "Regenerates health rapidly";
                    case VOID_FRUIT -> "Embrace the shadows themselves";
                    case THUNDER_FRUIT -> "Channel the power of storms";
                    case NATURE_FRUIT -> "Sustain yourself with nature's bounty";
                    case ICE_FRUIT -> "Chill your foes to the bone";
                    case STAR_FRUIT -> "Glow with celestial energy";
                    case BLOOD_FRUIT -> "Sacrifice vitality for power";
                };
            } else {
                return switch (this) {
                    case BUDDHA_FRUIT -> "Grants temporary invulnerability";
                    case CRYSTAL_FRUIT -> "Creates a protective crystal shield";
                    case DRAGON_FRUIT -> "Unleashes a fiery explosion";
                    case PHOENIX_FRUIT -> "Rise from the ashes with full health";
                    case VOID_FRUIT -> "Teleport through shadows";
                    case THUNDER_FRUIT -> "Strike down lightning from the sky";
                    case NATURE_FRUIT -> "Summon nature's wrath";
                    case ICE_FRUIT -> "Freeze the surrounding area";
                    case STAR_FRUIT -> "Grant temporary flight";
                    case BLOOD_FRUIT -> "Drain life from nearby entities";
                };
            }
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getShortName() {
            return ChatColor.stripColor(displayName).replaceAll("[^a-zA-Z0-9]", "");
        }
    }
    
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("magicfruits")).setExecutor(this);
        Objects.requireNonNull(getCommand("magicfruits")).setTabCompleter(this);
        getLogger().info("§aMagicFruits plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("§cMagicFruits plugin has been disabled!");
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    startFruitSpin(player);
                }
            }.runTaskLater(this, 20L);
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) return;
        
        for (FruitType fruit : FruitType.values()) {
            if (item.getItemMeta() != null && item.getItemMeta().displayName() != null &&
                item.getItemMeta().displayName().equals(Component.text(fruit.displayName))) {
                
                event.setCancelled(true);
                long currentTime = System.currentTimeMillis();
                long lastUse = abilityCooldown.getOrDefault(player.getUniqueId(), 0L);
                long timeLeft = 30000 - (currentTime - lastUse);
                
                if (currentTime - lastUse < 30000) {
                    long remaining = timeLeft / 1000;
                    player.sendMessage("§c§l⚠ §fAbility on cooldown! §7(" + remaining + " seconds remaining)");
                    showCooldownOnXPBar(player, (int) (timeLeft / 1000), fruit);
                    return;
                }
                
                if (player.isSneaking()) {
                    executeSecondaryAbility(player, fruit);
                } else {
                    executePrimaryAbility(player, fruit);
                }
                
                abilityCooldown.put(player.getUniqueId(), currentTime);
                lastUsedFruit.put(player.getUniqueId(), fruit);
                startCooldownDisplay(player, fruit);
                break;
            }
        }
    }
    
    private void startCooldownDisplay(Player player, FruitType fruit) {
        // Cancel existing cooldown task
        if (cooldownTasks.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(cooldownTasks.get(player.getUniqueId()));
        }
        
        new BukkitRunnable() {
            int secondsLeft = 30;
            
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    // Cooldown finished - show ready status
                    player.setExp(0);
                    player.setLevel(0);
                    player.sendActionBar(Component.text("§a§l✓ §f" + fruit.getDisplayName() + " §a§lREADY!"));
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                showCooldownOnXPBar(player, secondsLeft, fruit);
                secondsLeft--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }
    
    private void showCooldownOnXPBar(Player player, int secondsLeft, FruitType fruit) {
        // Set XP bar progress (0.0 to 1.0)
        float progress = (float) secondsLeft / 30.0f;
        player.setExp(progress);
        
        // Set level to show seconds remaining
        player.setLevel(secondsLeft);
        
        // Show action bar with cooldown info
        String message = "§c§l⏳ §f" + fruit.getDisplayName() + " §c§lCOOLDOWN: §e" + secondsLeft + "s";
        
        // Add visual indicator based on time remaining
        if (secondsLeft <= 5) {
            message = "§c§l⚠ §f" + fruit.getDisplayName() + " §c§lREADY IN: §e" + secondsLeft + "s §c§l⚠";
        } else if (secondsLeft <= 10) {
            message = "§6§l⌛ §f" + fruit.getDisplayName() + " §6§lCOOLDOWN: §e" + secondsLeft + "s";
        }
        
        player.sendActionBar(Component.text(message));
    }
    
    private void executePrimaryAbility(Player player, FruitType fruit) {
        player.addPotionEffect(new PotionEffect(fruit.primaryEffect, 200, 2));
        
        Location loc = player.getLocation();
        for (int i = 0; i < 50; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = Math.random() * 2;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            player.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(x, 1 + Math.random(), z), 0, 0, 0, 0, 1);
        }
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        player.showTitle(Title.title(
            Component.text("§6§l✦ " + fruit.displayName + " §6§l✦"),
            Component.text("§ePrimary ability activated!"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
        ));
        
        player.sendMessage("§a§l✔ §fYou have activated §6" + fruit.displayName + "§f's primary ability!");
    }
    
    private void executeSecondaryAbility(Player player, FruitType fruit) {
        switch (fruit) {
            case BUDDHA_FRUIT:
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4));
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
                break;
            case CRYSTAL_FRUIT:
                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 1, 1, 1, 0.5);
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                break;
            case DRAGON_FRUIT:
                player.getWorld().createExplosion(player.getLocation(), 3, false, false, player);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                break;
            case PHOENIX_FRUIT:
                player.setHealth(20);
                player.setFireTicks(0);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 200, 1, 1, 1, 0.5);
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
                break;
            case VOID_FRUIT:
                Location randomLoc = player.getLocation().add(Math.random() * 20 - 10, 0, Math.random() * 20 - 10);
                randomLoc.setY(player.getWorld().getHighestBlockYAt(randomLoc));
                player.teleport(randomLoc);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                break;
            case THUNDER_FRUIT:
                Block targetBlock = player.getTargetBlock(null, 50);
                if (targetBlock != null) {
                    player.getWorld().strikeLightning(targetBlock.getLocation());
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
                break;
            case NATURE_FRUIT:
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 100, 2, 2, 2);
                player.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0f, 1.0f);
                break;
            case ICE_FRUIT:
                player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, player.getLocation(), 200, 3, 1, 3);
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                break;
            case STAR_FRUIT:
                player.setAllowFlight(true);
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                }.runTaskLater(this, 100);
                break;
            case BLOOD_FRUIT:
                player.getNearbyEntities(5, 5, 5).forEach(entity -> {
                    if (entity instanceof Player) {
                        ((Player) entity).damage(4);
                        player.setHealth(Math.min(20, player.getHealth() + 2));
                    }
                });
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
                break;
        }
        
        player.showTitle(Title.title(
            Component.text("§c§l⚡ " + fruit.displayName + " §c§l⚡"),
            Component.text("§eSecondary ability unleashed!"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
        ));
    }
    
    private void startFruitSpin(Player player) {
        if (spinActive.getOrDefault(player.getUniqueId(), false)) return;
        
        spinActive.put(player.getUniqueId(), true);
        
        new BukkitRunnable() {
            int ticks = 0;
            int currentIndex = 0;
            List<FruitType> fruits = Arrays.asList(FruitType.values());
            
            @Override
            public void run() {
                if (ticks >= 300 || !spinActive.get(player.getUniqueId())) {
                    FruitType selected = fruits.get(currentIndex % fruits.size());
                    lastSelectedFruit.put(player.getUniqueId(), selected);
                    player.getInventory().addItem(selected.createItem());
                    
                    for (int i = 0; i < 360; i += 10) {
                        double rad = Math.toRadians(i);
                        double x = Math.cos(rad) * 3;
                        double z = Math.sin(rad) * 3;
                        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().clone().add(x, 2, z), 0, 0, 0, 0, 1);
                    }
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    
                    player.showTitle(Title.title(
                        Component.text("§6§l🎉 YOU GOT! 🎉"),
                        Component.text(selected.displayName),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
                    ));
                    
                    spinActive.put(player.getUniqueId(), false);
                    spinTaskIds.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                if (ticks % 5 == 0) {
                    currentIndex++;
                    FruitType current = fruits.get(currentIndex % fruits.size());
                    
                    for (int i = 0; i < 36; i++) {
                        double angle = Math.toRadians(i * 10 + ticks * 3);
                        double radius = 3;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().clone().add(x, 2, z), 0, 0, 0, 0, 1);
                    }
                    
                    player.sendActionBar(Component.text("§6§l⟳ §eSpinning: §f" + current.displayName + " §6§l⟳"));
                }
                
                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                openAdminGUI((Player) sender);
            } else {
                sender.sendMessage("§cThis command can only be used by players!");
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("spin")) {
            if (args.length == 1) {
                if (sender instanceof Player) {
                    startFruitSpin((Player) sender);
                } else {
                    sender.sendMessage("§cPlease specify a player!");
                }
            } else if (args.length == 2) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    startFruitSpin(target);
                    sender.sendMessage("§aStarted fruit spin for §e" + target.getName());
                } else {
                    sender.sendMessage("§cPlayer not found!");
                }
            } else if (args.length == 3 && args[2].equalsIgnoreCase("all")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    startFruitSpin(player);
                }
                sender.sendMessage("§aStarted fruit spin for §eALL §aplayers!");
            }
        } else if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /magicfruits give <player> <fruit>");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
            
            try {
                FruitType fruit = FruitType.valueOf(args[2].toUpperCase());
                target.getInventory().addItem(fruit.createItem());
                sender.sendMessage("§aGave §e" + fruit.displayName + " §ato §e" + target.getName());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cInvalid fruit! Available: " + Arrays.toString(FruitType.values()));
            }
        }
        
        return true;
    }
    
    private void openAdminGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lMAGIC FRUITS ADMIN §8§l✦");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        ItemStack spinSelf = new ItemStack(Material.NETHER_STAR);
        ItemMeta spinSelfMeta = spinSelf.getItemMeta();
        spinSelfMeta.displayName(Component.text("§6§l✦ SPIN FOR YOURSELF ✦"));
        spinSelfMeta.lore(Arrays.asList(
            Component.text("§7Click to start a fruit spin!"),
            Component.text("§8You will receive a random magical fruit")
        ));
        spinSelf.setItemMeta(spinSelfMeta);
        gui.setItem(22, spinSelf);
        
        ItemStack spinPlayer = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta spinPlayerMeta = spinPlayer.getItemMeta();
        spinPlayerMeta.displayName(Component.text("§b§l✦ SPIN FOR PLAYER ✦"));
        spinPlayerMeta.lore(Arrays.asList(
            Component.text("§7Click to select a player"),
            Component.text("§8They will receive a random fruit!")
        ));
        spinPlayer.setItemMeta(spinPlayerMeta);
        gui.setItem(31, spinPlayer);
        
        ItemStack spinAll = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta spinAllMeta = spinAll.getItemMeta();
        spinAllMeta.displayName(Component.text("§c§l✦ SPIN FOR ALL ✦"));
        spinAllMeta.lore(Arrays.asList(
            Component.text("§7Start a spin for all"),
            Component.text("§8online players!")
        ));
        spinAll.setItemMeta(spinAllMeta);
        gui.setItem(40, spinAll);
        
        player.openInventory(gui);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spin", "give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return null;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(FruitType.values()).map(Enum::name).map(String::toLowerCase).toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spin")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return suggestions;
        }
        return Collections.emptyList();
    }
}

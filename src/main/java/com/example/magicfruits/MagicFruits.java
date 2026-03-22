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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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
    private Map<UUID, Integer> adminPage = new ConcurrentHashMap<>();
    
    // Settings
    private boolean firstJoinReward = true;
    private boolean dropOnDeath = true;
    private int cooldownTime = 30;
    private int spinDuration = 15;
    private boolean particlesEnabled = true;
    private boolean soundsEnabled = true;
    
    private enum FruitType {
        BUDDHA_FRUIT("§e§l✨ §6§lBUDDHA FRUIT §e§l✨", 
                    "§7§oEnlightened by ancient wisdom",
                    PotionEffectType.STRENGTH,
                    PotionEffectType.RESISTANCE,
                    Color.fromRGB(0xFFD700),
                    "YELLOW",
                    Material.GOLDEN_APPLE),
        
        CRYSTAL_FRUIT("§b§l💎 §3§lCRYSTAL FRUIT §b§l💎",
                     "§7§oPure crystalline power",
                     PotionEffectType.SPEED,
                     PotionEffectType.JUMP_BOOST,
                     Color.fromRGB(0x00FFFF),
                     "LIGHT_BLUE",
                     Material.DIAMOND),
        
        DRAGON_FRUIT("§c§l🐉 §4§lDRAGON FRUIT §c§l🐉",
                    "§7§oWrath of the ancient dragons",
                    PotionEffectType.FIRE_RESISTANCE,
                    PotionEffectType.STRENGTH,
                    Color.fromRGB(0xFF4444),
                    "RED",
                    Material.DRAGON_BREATH),
        
        PHOENIX_FRUIT("§6§l🔥 §e§lPHOENIX FRUIT §6§l🔥",
                     "§7§oReborn from eternal flames",
                     PotionEffectType.REGENERATION,
                     PotionEffectType.ABSORPTION,
                     Color.fromRGB(0xFFA500),
                     "ORANGE",
                     Material.BLAZE_POWDER),
        
        VOID_FRUIT("§5§l🌑 §8§lVOID FRUIT §5§l🌑",
                  "§7§oEmbrace the darkness within",
                  PotionEffectType.INVISIBILITY,
                  PotionEffectType.NIGHT_VISION,
                  Color.fromRGB(0xAA00AA),
                  "PURPLE",
                  Material.ENDER_PEARL),
        
        THUNDER_FRUIT("§3§l⚡ §b§lTHUNDER FRUIT §3§l⚡",
                     "§7§oCommand the storm itself",
                     PotionEffectType.CONDUIT_POWER,
                     PotionEffectType.DOLPHINS_GRACE,
                     Color.fromRGB(0x44AAFF),
                     "LIGHT_BLUE",
                     Material.NETHER_STAR),
        
        NATURE_FRUIT("§2§l🌿 §a§lNATURE FRUIT §2§l🌿",
                    "§7§oOne with the natural world",
                    PotionEffectType.SATURATION,
                    PotionEffectType.LUCK,
                    Color.fromRGB(0x44FF44),
                    "GREEN",
                    Material.OAK_SAPLING),
        
        ICE_FRUIT("§b§l❄️ §f§lICE FRUIT §b§l❄️",
                 "§7§oFreeze your enemies solid",
                 PotionEffectType.SLOWNESS,
                 PotionEffectType.WATER_BREATHING,
                 Color.fromRGB(0x88FFFF),
                 "LIGHT_BLUE",
                 Material.PACKED_ICE),
        
        STAR_FRUIT("§d§l⭐ §5§lSTAR FRUIT §d§l⭐",
                  "§7§oHarness cosmic energy",
                  PotionEffectType.GLOWING,
                  PotionEffectType.LEVITATION,
                  Color.fromRGB(0xFF88FF),
                  "MAGENTA",
                  Material.NETHER_STAR),
        
        BLOOD_FRUIT("§4§l🩸 §c§lBLOOD FRUIT §4§l🩸",
                   "§7§oSacrifice for ultimate power",
                   PotionEffectType.WITHER,
                   PotionEffectType.INSTANT_HEALTH,
                   Color.fromRGB(0xFF4444),
                   "RED",
                   Material.REDSTONE);
        
        private final String displayName;
        private final String description;
        private final PotionEffectType primaryEffect;
        private final PotionEffectType secondaryEffect;
        private final Color dyeColor;
        private final String dyeMaterial;
        private final Material icon;
        
        FruitType(String displayName, String description, PotionEffectType primary, PotionEffectType secondary, Color dyeColor, String dyeMaterial, Material icon) {
            this.displayName = displayName;
            this.description = description;
            this.primaryEffect = primary;
            this.secondaryEffect = secondary;
            this.dyeColor = dyeColor;
            this.dyeMaterial = dyeMaterial;
            this.icon = icon;
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
        
        public boolean isFruitItem(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) return false;
            if (item.getItemMeta() == null || item.getItemMeta().displayName() == null) return false;
            return item.getItemMeta().displayName().equals(Component.text(displayName));
        }
        
        public ItemStack getIcon() {
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(displayName));
            meta.lore(Arrays.asList(
                Component.text("§7" + description),
                Component.empty(),
                Component.text("§eClick to give this fruit!")
            ));
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
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("magicfruits")).setExecutor(this);
        Objects.requireNonNull(getCommand("magicfruits")).setTabCompleter(this);
        getLogger().info("§aMagicFruits plugin has been enabled!");
        getLogger().info("§eFirst Join Reward: " + (firstJoinReward ? "§aENABLED" : "§cDISABLED"));
        getLogger().info("§eDrop on Death: " + (dropOnDeath ? "§aENABLED" : "§cDISABLED"));
    }
    
    @Override
    public void onDisable() {
        saveSettings();
        getLogger().info("§cMagicFruits plugin has been disabled!");
    }
    
    private void loadSettings() {
        FileConfiguration config = getConfig();
        firstJoinReward = config.getBoolean("settings.first-join-reward", true);
        dropOnDeath = config.getBoolean("settings.drop-on-death", true);
        cooldownTime = config.getInt("settings.cooldown-seconds", 30);
        spinDuration = config.getInt("settings.spin-duration-seconds", 15);
        particlesEnabled = config.getBoolean("settings.particles-enabled", true);
        soundsEnabled = config.getBoolean("settings.sounds-enabled", true);
    }
    
    private void saveSettings() {
        FileConfiguration config = getConfig();
        config.set("settings.first-join-reward", firstJoinReward);
        config.set("settings.drop-on-death", dropOnDeath);
        config.set("settings.cooldown-seconds", cooldownTime);
        config.set("settings.spin-duration-seconds", spinDuration);
        config.set("settings.particles-enabled", particlesEnabled);
        config.set("settings.sounds-enabled", soundsEnabled);
        saveConfig();
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!dropOnDeath) return;
        
        Player player = event.getEntity();
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        List<ItemStack> fruitsToDrop = new ArrayList<>();
        
        for (ItemStack item : drops) {
            for (FruitType fruit : FruitType.values()) {
                if (fruit.isFruitItem(item)) {
                    fruitsToDrop.add(item);
                    break;
                }
            }
        }
        
        if (!fruitsToDrop.isEmpty()) {
            event.getDrops().removeAll(fruitsToDrop);
            for (ItemStack fruit : fruitsToDrop) {
                player.getWorld().dropItemNaturally(player.getLocation(), fruit);
            }
            player.sendMessage("§c§l💀 §fYour magical fruits have been dropped on death!");
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (firstJoinReward && !player.hasPlayedBefore()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    startFruitSpin(player);
                }
            }.runTaskLater(this, 20L);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle().equals("§8§l✦ §6§lMAGIC FRUITS ADMIN §8§l✦") ||
            event.getView().getTitle().equals("§8§l✦ §6§lFRUITS MENU §8§l✦") ||
            event.getView().getTitle().equals("§8§l✦ §6§lPLAYER MENU §8§l✦") ||
            event.getView().getTitle().equals("§8§l✦ §6§lSETTINGS §8§l✦")) {
            
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String title = event.getView().getTitle();
            
            if (title.equals("§8§l✦ §6§lMAGIC FRUITS ADMIN §8§l✦")) {
                handleMainDashboard(player, clicked);
            } else if (title.equals("§8§l✦ §6§lFRUITS MENU §8§l✦")) {
                handleFruitsMenu(player, clicked);
            } else if (title.equals("§8§l✦ §6§lPLAYER MENU §8§l✦")) {
                handlePlayerMenu(player, clicked);
            } else if (title.equals("§8§l✦ §6§lSETTINGS §8§l✦")) {
                handleSettingsMenu(player, clicked);
            }
        }
    }
    
    private void handleMainDashboard(Player player, ItemStack clicked) {
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        switch (name) {
            case "FRUITS MANAGEMENT":
                openFruitsMenu(player);
                break;
            case "PLAYER MANAGEMENT":
                openPlayerMenu(player);
                break;
            case "SPIN CONTROL":
                openSpinControl(player);
                break;
            case "GLOBAL SETTINGS":
                openSettingsMenu(player);
                break;
            case "STATISTICS":
                showStatistics(player);
                break;
            case "RELOAD CONFIG":
                reloadConfig();
                loadSettings();
                player.sendMessage("§aConfig reloaded successfully!");
                player.closeInventory();
                break;
        }
    }
    
    private void handleFruitsMenu(Player player, ItemStack clicked) {
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        if (name.equals("BACK")) {
            openAdminDashboard(player);
            return;
        }
        
        for (FruitType fruit : FruitType.values()) {
            if (fruit.getDisplayName().equals(name)) {
                openGiveFruitMenu(player, fruit);
                return;
            }
        }
    }
    
    private void handlePlayerMenu(Player player, ItemStack clicked) {
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        if (name.equals("BACK")) {
            openAdminDashboard(player);
            return;
        }
        
        if (name.equals("SPIN ALL PLAYERS")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                startFruitSpin(online);
            }
            player.sendMessage("§aStarted spin for all online players!");
            player.closeInventory();
        } else if (name.startsWith("SPIN: ")) {
            String targetName = name.substring(6);
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                startFruitSpin(target);
                player.sendMessage("§aStarted spin for §e" + targetName);
                player.closeInventory();
            }
        }
    }
    
    private void handleSettingsMenu(Player player, ItemStack clicked) {
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        if (name.equals("BACK")) {
            openAdminDashboard(player);
            return;
        }
        
        switch (name) {
            case "FIRST JOIN REWARD: ✓ ENABLED":
                firstJoinReward = false;
                saveSettings();
                player.sendMessage("§cFirst join reward has been §lDISABLED");
                openSettingsMenu(player);
                break;
            case "FIRST JOIN REWARD: ✗ DISABLED":
                firstJoinReward = true;
                saveSettings();
                player.sendMessage("§aFirst join reward has been §lENABLED");
                openSettingsMenu(player);
                break;
            case "DROP ON DEATH: ✓ ENABLED":
                dropOnDeath = false;
                saveSettings();
                player.sendMessage("§cFruits will §lNOT §cdrop on death anymore");
                openSettingsMenu(player);
                break;
            case "DROP ON DEATH: ✗ DISABLED":
                dropOnDeath = true;
                saveSettings();
                player.sendMessage("§aFruits will §lDROP §aon death");
                openSettingsMenu(player);
                break;
            case "COOLDOWN: 30s":
                // Cycle through cooldown options
                if (cooldownTime == 30) cooldownTime = 20;
                else if (cooldownTime == 20) cooldownTime = 15;
                else if (cooldownTime == 15) cooldownTime = 10;
                else cooldownTime = 30;
                saveSettings();
                player.sendMessage("§eCooldown set to: §f" + cooldownTime + " seconds");
                openSettingsMenu(player);
                break;
            case "SPIN DURATION: 15s":
                if (spinDuration == 15) spinDuration = 10;
                else if (spinDuration == 10) spinDuration = 20;
                else spinDuration = 15;
                saveSettings();
                player.sendMessage("§eSpin duration set to: §f" + spinDuration + " seconds");
                openSettingsMenu(player);
                break;
            case "PARTICLES: ✓ ENABLED":
                particlesEnabled = false;
                saveSettings();
                player.sendMessage("§cParticles have been §lDISABLED");
                openSettingsMenu(player);
                break;
            case "PARTICLES: ✗ DISABLED":
                particlesEnabled = true;
                saveSettings();
                player.sendMessage("§aParticles have been §lENABLED");
                openSettingsMenu(player);
                break;
            case "SOUNDS: ✓ ENABLED":
                soundsEnabled = false;
                saveSettings();
                player.sendMessage("§cSounds have been §lDISABLED");
                openSettingsMenu(player);
                break;
            case "SOUNDS: ✗ DISABLED":
                soundsEnabled = true;
                saveSettings();
                player.sendMessage("§aSounds have been §lENABLED");
                openSettingsMenu(player);
                break;
        }
    }
    
    private void openAdminDashboard(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lMAGIC FRUITS ADMIN §8§l✦");
        
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
        
        // Dashboard Items
        gui.setItem(11, createMenuItem(Material.CHEST, "§6§l✦ FRUITS MANAGEMENT ✦",
            "§7Manage all magical fruits",
            "§7Give fruits to players",
            "§7View fruit statistics"));
        
        gui.setItem(13, createMenuItem(Material.PLAYER_HEAD, "§b§l✦ PLAYER MANAGEMENT ✦",
            "§7Manage player spins",
            "§7Spin for specific players",
            "§7Give fruits to players"));
        
        gui.setItem(15, createMenuItem(Material.NETHER_STAR, "§5§l✦ SPIN CONTROL ✦",
            "§7Control spin system",
            "§7Start mass spins",
            "§7Configure spin settings"));
        
        gui.setItem(29, createMenuItem(Material.REDSTONE_COMPARATOR, "§e§l✦ GLOBAL SETTINGS ✦",
            "§7Configure plugin settings",
            "§7First join reward, death drops",
            "§7Cooldown, particles, sounds"));
        
        gui.setItem(31, createMenuItem(Material.PAPER, "§a§l✦ STATISTICS ✦",
            "§7View plugin statistics",
            "§7Total fruits given",
            "§7Active players"));
        
        gui.setItem(33, createMenuItem(Material.ENDER_CHEST, "§c§l✦ RELOAD CONFIG ✦",
            "§7Reload configuration",
            "§7Apply new settings"));
        
        // Decorative items
        ItemStack titleDeco = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta decoMeta = titleDeco.getItemMeta();
        decoMeta.displayName(Component.text("§6§l╔══════════════════════════════╗"));
        titleDeco.setItemMeta(decoMeta);
        gui.setItem(4, titleDeco);
        
        player.openInventory(gui);
    }
    
    private void openFruitsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lFRUITS MENU §8§l✦");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        int slot = 10;
        for (FruitType fruit : FruitType.values()) {
            gui.setItem(slot, fruit.getIcon());
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }
        
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to main dashboard");
        gui.setItem(49, back);
        
        player.openInventory(gui);
    }
    
    private void openGiveFruitMenu(Player player, FruitType fruit) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lGIVE FRUIT §8§l✦");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        gui.setItem(22, fruit.getIcon());
        
        int slot = 28;
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.displayName(Component.text("§a§l" + online.getName()));
            meta.lore(Arrays.asList(
                Component.text("§7Click to give §e" + fruit.getDisplayName()),
                Component.text("§7to this player!")
            ));
            head.setItemMeta(meta);
            gui.setItem(slot, head);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to fruits menu");
        gui.setItem(49, back);
        
        // Handle click in this GUI
        player.openInventory(gui);
    }
    
    private void openPlayerMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lPLAYER MENU §8§l✦");
        
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, border);
            }
        }
        
        ItemStack spinAll = createMenuItem(Material.NETHER_STAR, "§c§l✦ SPIN ALL PLAYERS ✦",
            "§7Start fruit spin for all",
            "§7online players!");
        gui.setItem(22, spinAll);
        
        int slot = 28;
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.displayName(Component.text("§b§l" + online.getName()));
            meta.lore(Arrays.asList(
                Component.text("§7• §eLeft Click: §fStart spin"),
                Component.text("§7• §eRight Click: §fGive fruit menu")
            ));
            head.setItemMeta(meta);
            gui.setItem(slot, head);
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
            if (slot > 43) break;
        }
        
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to main dashboard");
        gui.setItem(49, back);
        
        player.openInventory(gui);
    }
    
    private void openSpinControl(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8§l✦ §6§lSPIN CONTROL §8§l✦");
        
        gui.setItem(11, createMenuItem(Material.NETHER_STAR, "§6§l✦ SPIN YOURSELF ✦",
            "§7Start a fruit spin for yourself"));
        
        gui.setItem(13, createMenuItem(Material.PLAYER_HEAD, "§b§l✦ SPIN SPECIFIC ✦",
            "§7Start spin for a specific player"));
        
        gui.setItem(15, createMenuItem(Material.DRAGON_HEAD, "§c§l✦ SPIN ALL ✦",
            "§7Start spin for all online players"));
        
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to main dashboard");
        gui.setItem(22, back);
        
        player.openInventory(gui);
    }
    
    private void openSettingsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§8§l✦ §6§lSETTINGS §8§l✦");
        
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
        
        // First Join Reward Toggle
        String firstJoinStatus = firstJoinReward ? "§a✓ ENABLED" : "§c✗ DISABLED";
        gui.setItem(19, createMenuItem(Material.GOLDEN_APPLE, "§6§lFIRST JOIN REWARD: " + firstJoinStatus,
            "§7New players get a fruit spin",
            "§7Click to " + (firstJoinReward ? "disable" : "enable")));
        
        // Drop on Death Toggle
        String dropStatus = dropOnDeath ? "§a✓ ENABLED" : "§c✗ DISABLED";
        gui.setItem(20, createMenuItem(Material.SKELETON_SKULL, "§c§lDROP ON DEATH: " + dropStatus,
            "§7Fruits drop when player dies",
            "§7Click to " + (dropOnDeath ? "disable" : "enable")));
        
        // Cooldown Setting
        gui.setItem(21, createMenuItem(Material.CLOCK, "§e§lCOOLDOWN: " + cooldownTime + "s",
            "§7Ability cooldown time",
            "§7Click to cycle: 30s → 20s → 15s → 10s"));
        
        // Spin Duration Setting
        gui.setItem(22, createMenuItem(Material.SUGAR, "§b§lSPIN DURATION: " + spinDuration + "s",
            "§7Duration of fruit spin",
            "§7Click to cycle: 15s → 10s → 20s"));
        
        // Particles Toggle
        String particlesStatus = particlesEnabled ? "§a✓ ENABLED" : "§c✗ DISABLED";
        gui.setItem(23, createMenuItem(Material.FIREWORK_STAR, "§d§lPARTICLES: " + particlesStatus,
            "§7Visual particle effects",
            "§7Click to " + (particlesEnabled ? "disable" : "enable")));
        
        // Sounds Toggle
        String soundsStatus = soundsEnabled ? "§a✓ ENABLED" : "§c✗ DISABLED";
        gui.setItem(24, createMenuItem(Material.NOTE_BLOCK, "§a§lSOUNDS: " + soundsStatus,
            "§7Sound effects",
            "§7Click to " + (soundsEnabled ? "disable" : "enable")));
        
        // Back button
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to main dashboard");
        gui.setItem(49, back);
        
        player.openInventory(gui);
    }
    
    private void showStatistics(Player player) {
        Inventory stats = Bukkit.createInventory(null, 27, "§8§l✦ §6§lSTATISTICS §8§l✦");
        
        stats.setItem(11, createMenuItem(Material.PLAYER_HEAD, "§a§lONLINE PLAYERS",
            "§7" + Bukkit.getOnlinePlayers().size() + " players online"));
        
        stats.setItem(13, createMenuItem(Material.CHEST, "§b§lPLUGIN SETTINGS",
            "§7First Join Reward: " + (firstJoinReward ? "§aON" : "§cOFF"),
            "§7Drop on Death: " + (dropOnDeath ? "§aON" : "§cOFF"),
            "§7Cooldown: §f" + cooldownTime + "s",
            "§7Spin Duration: §f" + spinDuration + "s",
            "§7Particles: " + (particlesEnabled ? "§aON" : "§cOFF"),
            "§7Sounds: " + (soundsEnabled ? "§aON" : "§cOFF")));
        
        stats.setItem(15, createMenuItem(Material.NETHER_STAR, "§6§lACTIVE SPINS",
            "§7" + spinActive.size() + " active spins"));
        
        ItemStack back = createMenuItem(Material.ARROW, "§c§l◀ BACK",
            "§7Return to main dashboard");
        stats.setItem(22, back);
        
        player.openInventory(stats);
    }
    
    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(Component.text(line));
        }
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) return;
        
        for (FruitType fruit : FruitType.values()) {
            if (fruit.isFruitItem(item)) {
                event.setCancelled(true);
                long currentTime = System.currentTimeMillis();
                long lastUse = abilityCooldown.getOrDefault(player.getUniqueId(), 0L);
                long timeLeft = (cooldownTime * 1000L) - (currentTime - lastUse);
                
                if (currentTime - lastUse < (cooldownTime * 1000L)) {
                    long remaining = timeLeft / 1000;
                    player.sendMessage("§c§l⚠ §fAbility on cooldown! §7(" + remaining + " seconds remaining)");
                    showCooldownOnXPBar(player, (int) remaining, fruit);
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
        if (cooldownTasks.containsKey(player.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(cooldownTasks.get(player.getUniqueId()));
        }
        
        int taskId = new BukkitRunnable() {
            int secondsLeft = cooldownTime;
            
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    player.setExp(0);
                    player.setLevel(0);
                    if (soundsEnabled) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    }
                    player.sendActionBar(Component.text("§a§l✓ §f" + fruit.getDisplayName() + " §a§lREADY!"));
                    cooldownTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                showCooldownOnXPBar(player, secondsLeft, fruit);
                secondsLeft--;
            }
        }.runTaskTimer(this, 0L, 20L).getTaskId();
        
        cooldownTasks.put(player.getUniqueId(), taskId);
    }
    
    private void showCooldownOnXPBar(Player player, int secondsLeft, FruitType fruit) {
        float progress = (float) secondsLeft / (float) cooldownTime;
        player.setExp(progress);
        player.setLevel(secondsLeft);
        
        String message = "§c§l⏳ §f" + fruit.getDisplayName() + " §c§lCOOLDOWN: §e" + secondsLeft + "s";
        
        if (secondsLeft <= 5) {
            message = "§c§l⚠ §f" + fruit.getDisplayName() + " §c§lREADY IN: §e" + secondsLeft + "s §c§l⚠";
        } else if (secondsLeft <= 10) {
            message = "§6§l⌛ §f" + fruit.getDisplayName() + " §6§lCOOLDOWN: §e" + secondsLeft + "s";
        }
        
        player.sendActionBar(Component.text(message));
    }
    
    private void executePrimaryAbility(Player player, FruitType fruit) {
        player.addPotionEffect(new PotionEffect(fruit.primaryEffect, 200, 2));
        
        if (particlesEnabled) {
            Location loc = player.getLocation();
            for (int i = 0; i < 50; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double radius = Math.random() * 2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                player.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(x, 1 + Math.random(), z), 0, 0, 0, 0, 1);
            }
        }
        
        if (soundsEnabled) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
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
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
                break;
            case CRYSTAL_FRUIT:
                if (particlesEnabled) player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 100, 1, 1, 1, 0.5);
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                break;
            case DRAGON_FRUIT:
                player.getWorld().createExplosion(player.getLocation(), 3, false, false, player);
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                break;
            case PHOENIX_FRUIT:
                player.setHealth(20);
                player.setFireTicks(0);
                if (particlesEnabled) player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 200, 1, 1, 1, 0.5);
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.0f);
                break;
            case VOID_FRUIT:
                Location randomLoc = player.getLocation().add(Math.random() * 20 - 10, 0, Math.random() * 20 - 10);
                randomLoc.setY(player.getWorld().getHighestBlockYAt(randomLoc));
                player.teleport(randomLoc);
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                break;
            case THUNDER_FRUIT:
                Block targetBlock = player.getTargetBlock(null, 50);
                if (targetBlock != null) {
                    player.getWorld().strikeLightning(targetBlock.getLocation());
                    if (soundsEnabled) player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
                break;
            case NATURE_FRUIT:
                if (particlesEnabled) player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 100, 2, 2, 2);
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0f, 1.0f);
                break;
            case ICE_FRUIT:
                if (particlesEnabled) player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, player.getLocation(), 200, 3, 1, 3);
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                break;
            case STAR_FRUIT:
                player.setAllowFlight(true);
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
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
                if (soundsEnabled) player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
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
        int durationTicks = spinDuration * 20;
        
        new BukkitRunnable() {
            int ticks = 0;
            int currentIndex = 0;
            List<FruitType> fruits = Arrays.asList(FruitType.values());
            
            @Override
            public void run() {
                if (ticks >= durationTicks || !spinActive.get(player.getUniqueId())) {
                    FruitType selected = fruits.get(currentIndex % fruits.size());
                    lastSelectedFruit.put(player.getUniqueId(), selected);
                    player.getInventory().addItem(selected.createItem());
                    
                    if (particlesEnabled) {
                        for (int i = 0; i < 360; i += 10) {
                            double rad = Math.toRadians(i);
                            double x = Math.cos(rad) * 3;
                            double z = Math.sin(rad) * 3;
                            player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().clone().add(x, 2, z), 0, 0, 0, 0, 1);
                        }
                    }
                    
                    if (soundsEnabled) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    }
                    
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
                    
                    if (particlesEnabled) {
                        for (int i = 0; i < 36; i++) {
                            double angle = Math.toRadians(i * 10 + ticks * 3);
                            double radius = 3;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().clone().add(x, 2, z), 0, 0, 0, 0, 1);
                        }
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
                openAdminDashboard((Player) sender);
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
        } else if (args[0].equalsIgnoreCase("dashboard") || args[0].equalsIgnoreCase("admin")) {
            if (sender instanceof Player) {
                openAdminDashboard((Player) sender);
            } else {
                sender.sendMessage("§cThis command can only be used by players!");
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadSettings();
            sender.sendMessage("§aMagicFruits configuration reloaded!");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spin", "give", "dashboard", "reload");
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

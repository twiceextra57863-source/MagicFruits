package com.example.magicfruits.managers;

import com.example.magicfruits.MagicFruits;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ProtectionManager implements Listener {

    private final MagicFruits plugin;
    private final List<ProtectedRegion> protectedRegions = new ArrayList<>();
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();
    private File regionsFile;
    private FileConfiguration regionsConfig;

    public static class ProtectedRegion {
        private final String worldName;
        private final int minX, maxX, minY, maxY, minZ, maxZ;

        public ProtectedRegion(Location p1, Location p2) {
            this.worldName = p1.getWorld().getName();
            this.minX = Math.min(p1.getBlockX(), p2.getBlockX());
            this.maxX = Math.max(p1.getBlockX(), p2.getBlockX());
            this.minY = Math.min(p1.getBlockY(), p2.getBlockY());
            this.maxY = Math.max(p1.getBlockY(), p2.getBlockY());
            this.minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
            this.maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        }

        public ProtectedRegion(String worldName, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.worldName = worldName;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        public boolean contains(Location loc) {
            if (loc == null || loc.getWorld() == null) return false;
            if (!loc.getWorld().getName().equalsIgnoreCase(worldName)) return false;
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        public String getWorldName() { return worldName; }
        public int getMinX() { return minX; }
        public int getMaxX() { return maxX; }
        public int getMinY() { return minY; }
        public int getMaxY() { return maxY; }
        public int getMinZ() { return minZ; }
        public int getMaxZ() { return maxZ; }
    }

    public ProtectionManager(MagicFruits plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadRegions();
    }

    public static ItemStack createWand() {
        ItemStack item = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lMAGIC WAND §7(Protection Wand)");
        meta.setUnbreakable(true);
        List<String> lore = new ArrayList<>();
        lore.add("§8§m----------------------------------------");
        lore.add("§7§oRight-click block: §eSet Position 1");
        lore.add("§7§oLeft-click block: §eSet Position 2");
        lore.add("§8§m----------------------------------------");
        lore.add("§d/magicwand protect §7- Ban fruits in region");
        lore.add("§d/magicwand break §7- Unban fruits in region");
        lore.add("§8§m----------------------------------------");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_AXE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().contains("MAGIC WAND");
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isWand(item)) return;

        if (event.getClickedBlock() == null) return;

        Action action = event.getAction();
        Location clickedLoc = event.getClickedBlock().getLocation();

        if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            pos1Map.put(player.getUniqueId(), clickedLoc);
            player.sendMessage("§d§lMAGIC WAND §8» §aSelection 1 set at ("
                + clickedLoc.getBlockX() + ", " + clickedLoc.getBlockY() + ", " + clickedLoc.getBlockZ() + ")");
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            pos2Map.put(player.getUniqueId(), clickedLoc);
            player.sendMessage("§d§lMAGIC WAND §8» §aSelection 2 set at ("
                + clickedLoc.getBlockX() + ", " + clickedLoc.getBlockY() + ", " + clickedLoc.getBlockZ() + ")");
        }
    }

    public boolean isLocationProtected(Location loc) {
        if (loc == null) return false;
        for (ProtectedRegion region : protectedRegions) {
            if (region.contains(loc)) return true;
        }
        return false;
    }

    public boolean protectSelection(Player player) {
        UUID uuid = player.getUniqueId();
        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);

        if (p1 == null || p2 == null) {
            player.sendMessage("§c§l⚠ §fPlease select both positions first using the Magic Wand!");
            return false;
        }

        if (!p1.getWorld().getName().equals(p2.getWorld().getName())) {
            player.sendMessage("§c§l⚠ §fBoth selections must be in the same world!");
            return false;
        }

        ProtectedRegion region = new ProtectedRegion(p1, p2);
        protectedRegions.add(region);
        saveRegions();

        pos1Map.remove(uuid);
        pos2Map.remove(uuid);

        player.sendMessage("§a§l🛡️ AREA PROTECTED! §fFruits are now §cBANNED §fin this selected region.");
        return true;
    }

    public boolean breakProtection(Player player) {
        Location loc = player.getLocation();
        UUID uuid = player.getUniqueId();
        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);

        boolean removed = false;

        // If player has a selection, check matching regions
        if (p1 != null && p2 != null) {
            removed = protectedRegions.removeIf(r -> r.contains(p1) || r.contains(p2));
        }

        // Also remove region standing in
        boolean standingRemoved = protectedRegions.removeIf(r -> r.contains(loc));
        if (standingRemoved) removed = true;

        if (removed) {
            saveRegions();
            pos1Map.remove(uuid);
            pos2Map.remove(uuid);
            player.sendMessage("§c§l💥 PROTECTION REMOVED! §fFruits are now §aENABLED §fin this region.");
            return true;
        } else {
            player.sendMessage("§c§l⚠ §fNo protected region found at your location or selection!");
            return false;
        }
    }

    public void loadRegions() {
        protectedRegions.clear();
        regionsFile = new File(plugin.getDataFolder(), "protected_regions.yml");
        if (!regionsFile.exists()) {
            try {
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
                regionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create protected_regions.yml!");
            }
        }
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);

        if (regionsConfig.contains("regions")) {
            List<Map<?, ?>> list = (List<Map<?, ?>>) regionsConfig.getMapList("regions");
            for (Map<?, ?> map : list) {
                String world = (String) map.get("world");
                int minX = ((Number) map.get("minX")).intValue();
                int maxX = ((Number) map.get("maxX")).intValue();
                int minY = ((Number) map.get("minY")).intValue();
                int maxY = ((Number) map.get("maxY")).intValue();
                int minZ = ((Number) map.get("minZ")).intValue();
                int maxZ = ((Number) map.get("maxZ")).intValue();

                protectedRegions.add(new ProtectedRegion(world, minX, maxX, minY, maxY, minZ, maxZ));
            }
        }
    }

    public void saveRegions() {
        if (regionsFile == null) {
            regionsFile = new File(plugin.getDataFolder(), "protected_regions.yml");
        }
        regionsConfig = new YamlConfiguration();

        List<Map<String, Object>> list = new ArrayList<>();
        for (ProtectedRegion region : protectedRegions) {
            Map<String, Object> map = new HashMap<>();
            map.put("world", region.getWorldName());
            map.put("minX", region.getMinX());
            map.put("maxX", region.getMaxX());
            map.put("minY", region.getMinY());
            map.put("maxY", region.getMaxY());
            map.put("minZ", region.getMinZ());
            map.put("maxZ", region.getMaxZ());
            list.add(map);
        }

        regionsConfig.set("regions", list);
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save protected_regions.yml!");
        }
    }
}

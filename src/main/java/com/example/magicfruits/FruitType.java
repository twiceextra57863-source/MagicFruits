package com.example.magicfruits;

import com.example.magicfruits.abilities.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public enum FruitType {
    BUDDHA_FRUIT("§e§l✨ §6§lBUDDHA FRUIT §e§l✨", 
                "§7§oEnlightened by ancient wisdom",
                Material.NETHER_STAR,
                1001,
                new BuddhaAbility()),
    
    CRYSTAL_FRUIT("§b§l💎 §3§lCRYSTAL FRUIT §b§l💎",
                 "§7§oPure crystalline power",
                 Material.NETHER_STAR,
                 1002,
                 new CrystalAbility()),
    
    DRAGON_FRUIT("§c§l🐉 §4§lDRAGON FRUIT §c§l🐉",
                "§7§oWrath of the ancient dragons",
                Material.NETHER_STAR,
                1003,
                new DragonAbility()),
    
    PHOENIX_FRUIT("§6§l🔥 §e§lPHOENIX FRUIT §6§l🔥",
                 "§7§oReborn from eternal flames",
                 Material.NETHER_STAR,
                 1004,
                 new PhoenixAbility()),
    
    VOID_FRUIT("§5§l🌑 §8§lVOID FRUIT §5§l🌑",
              "§7§oEmbrace the darkness within",
              Material.NETHER_STAR,
              1005,
              new VoidAbility()),
    
    THUNDER_FRUIT("§3§l⚡ §b§lTHUNDER FRUIT §3§l⚡",
                 "§7§oCommand the storm itself",
                 Material.NETHER_STAR,
package com.example.magicfruits;

import com.example.magicfruits.abilities.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public enum FruitType {
    BUDDHA_FRUIT("§e§l✨ §6§lBUDDHA FRUIT §e§l✨", 
                "§7§oEnlightened by ancient wisdom",
                Material.NETHER_STAR,
                1001,
                new BuddhaAbility()),
    
    CRYSTAL_FRUIT("§b§l💎 §3§lCRYSTAL FRUIT §b§l💎",
                 "§7§oPure crystalline power",
                 Material.NETHER_STAR,
                 1002,
                 new CrystalAbility()),
    
    DRAGON_FRUIT("§c§l🐉 §4§lDRAGON FRUIT §c§l🐉",
                "§7§oWrath of the ancient dragons",
                Material.NETHER_STAR,
                1003,
                new DragonAbility()),
    
    PHOENIX_FRUIT("§6§l🔥 §e§lPHOENIX FRUIT §6§l🔥",
                 "§7§oReborn from eternal flames",
                 Material.NETHER_STAR,
                 1004,
                 new PhoenixAbility()),
    
    VOID_FRUIT("§5§l🌑 §8§lVOID FRUIT §5§l🌑",
              "§7§oEmbrace the darkness within",
              Material.NETHER_STAR,
              1005,
              new VoidAbility()),
    
    THUNDER_FRUIT("§3§l⚡ §b§lTHUNDER FRUIT §3§l⚡",
                 "§7§oCommand the storm itself",
                 Material.NETHER_STAR,
                 1006,
                 new ThunderAbility()),
    
    NATURE_FRUIT("§2§l🌿 §a§lNATURE FRUIT §2§l🌿",
                "§7§oOne with the natural world",
                Material.NETHER_STAR,
                1007,
                new NatureAbility()),
    
package com.example.magicfruits;

import com.example.magicfruits.abilities.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public enum FruitType {
    BUDDHA_FRUIT("§e§l✨ §6§lBUDDHA FRUIT §e§l✨", 
                "§7§oEnlightened by ancient wisdom",
                Material.NETHER_STAR,
                1001,
                new BuddhaAbility()),
    
    CRYSTAL_FRUIT("§b§l💎 §3§lCRYSTAL FRUIT §b§l💎",
                 "§7§oPure crystalline power",
                 Material.NETHER_STAR,
                 1002,
                 new CrystalAbility()),
    
    DRAGON_FRUIT("§c§l🐉 §4§lDRAGON FRUIT §c§l🐉",
                "§7§oWrath of the ancient dragons",
                Material.NETHER_STAR,
                1003,
                new DragonAbility()),
    
    PHOENIX_FRUIT("§6§l🔥 §e§lPHOENIX FRUIT §6§l🔥",
                 "§7§oReborn from eternal flames",
                 Material.NETHER_STAR,
                 1004,
                 new PhoenixAbility()),
    
    VOID_FRUIT("§5§l🌑 §8§lVOID FRUIT §5§l🌑",
              "§7§oEmbrace the darkness within",
              Material.NETHER_STAR,
              1005,
              new VoidAbility()),
    
    THUNDER_FRUIT("§3§l⚡ §b§lTHUNDER FRUIT §3§l⚡",
                 "§7§oCommand the storm itself",
                 Material.NETHER_STAR,
                 1006,
                 new ThunderAbility()),
    
    NATURE_FRUIT("§2§l🌿 §a§lNATURE FRUIT §2§l🌿",
                "§7§oOne with the natural world",
                Material.NETHER_STAR,
                1007,
                new NatureAbility()),
    
    ICE_FRUIT("§b§l❄️ §f§lICE FRUIT §b§l❄️",
             "§7§oFreeze your enemies solid",
             Material.NETHER_STAR,
             1008,
             new IceAbility()),
    
    STAR_FRUIT("§d§l⭐ §5§lSTAR FRUIT §d§l⭐",
              "§7§oHarness cosmic energy",
              Material.NETHER_STAR,
              1009,
              new StarAbility()),
    
    THIEF_FRUIT("§8§l🎭 §0§lTHIEF FRUIT §8§l🎭",
               "§7§oSteal abilities from others",
               Material.NETHER_STAR,
               1010,
               new ThiefAbility()),
    
    PORTAL_FRUIT("§5§l🌀 §d§lPORTAL FRUIT §5§l🌀",
                "§7§oMaster of dimensions",
                Material.NETHER_STAR,
                1011,
                new PortalAbility());
    
    private final String displayName;
    private final String description;
    private final Material material;
    private final int customModelData;
    private final Ability ability;
    
    FruitType(String displayName, String description, Material material, 
              int customModelData, Ability ability) {
        this.displayName = displayName;
        this.description = description;
        this.material = material;
        this.customModelData = customModelData;
        this.ability = ability;
    }
    
    public ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setCustomModelData(customModelData);
        
        List<String> lore = new ArrayList<>();
        lore.add("§8§m----------------------------------------");
        lore.add("§7§l✦ §f§lMYSTICAL ARTIFACT §7§l✦");
        lore.add("§8§m----------------------------------------");
        lore.add("");
        lore.add("§7§o" + description);
        lore.add("");
        lore.add("§6§l✨ ABILITIES ✨");
        lore.add(" §e▶ §fRight Click: §7" + ability.getPrimaryDescription());
        lore.add(" §e▶ §fCrouch + Right Click: §7" + ability.getSecondaryDescription());
        lore.add("");
        lore.add("§5§l⚡ KEYBINDS ⚡");
        lore.add(" §d• §fUse: §7Right Click");
        lore.add(" §d• §fSpecial: §7Sneak + Right Click");
        lore.add("");
        lore.add("§8§m----------------------------------------");
        lore.add("§7§l✦ §f§lLEGENDARY FRUIT §7§l✦");
        lore.add("§8§m----------------------------------------");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    public ItemStack createDisplayItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }
    
    public static FruitType fromItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        String name = item.getItemMeta().getDisplayName();
        for (FruitType fruit : values()) {
            if (fruit.displayName.equals(name)) {
                return fruit;
            }
        }
        return null;
    }
    
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setCustomModelData(customModelData);
        List<String> lore = new ArrayList<>();
        lore.add("§7" + description);
        lore.add("");
        lore.add("§eClick to give this fruit!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    public String getDisplayName() { return displayName; }
    public Ability getAbility() { return ability; }
}

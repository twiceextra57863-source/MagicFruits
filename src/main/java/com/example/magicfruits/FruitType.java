package com.example.magicfruits;

import com.example.magicfruits.abilities.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public enum FruitType {
    BUDDHA_FRUIT("§e§l✨ §6§lBUDDHA FRUIT §e§l✨", 
                "§7§oEnlightened by ancient wisdom",
                Color.fromRGB(0xFFD700),
                "YELLOW",
                Material.GOLDEN_APPLE,
                1001,
                new BuddhaAbility()),
    
    CRYSTAL_FRUIT("§b§l💎 §3§lCRYSTAL FRUIT §b§l💎",
                 "§7§oPure crystalline power",
                 Color.fromRGB(0x00FFFF),
                 "LIGHT_BLUE",
                 Material.DIAMOND,
                 1002,
                 new CrystalAbility()),
    
    DRAGON_FRUIT("§c§l🐉 §4§lDRAGON FRUIT §c§l🐉",
                "§7§oWrath of the ancient dragons",
                Color.fromRGB(0xFF4444),
                "RED",
                Material.DRAGON_BREATH,
                1003,
                new DragonAbility()),
    
    PHOENIX_FRUIT("§6§l🔥 §e§lPHOENIX FRUIT §6§l🔥",
                 "§7§oReborn from eternal flames",
                 Color.fromRGB(0xFFA500),
                 "ORANGE",
                 Material.BLAZE_POWDER,
                 1004,
                 new PhoenixAbility()),
    
    VOID_FRUIT("§5§l🌑 §8§lVOID FRUIT §5§l🌑",
              "§7§oEmbrace the darkness within",
              Color.fromRGB(0xAA00AA),
              "PURPLE",
              Material.ENDER_PEARL,
              1005,
              new VoidAbility()),
    
    THUNDER_FRUIT("§3§l⚡ §b§lTHUNDER FRUIT §3§l⚡",
                 "§7§oCommand the storm itself",
                 Color.fromRGB(0x44AAFF),
                 "LIGHT_BLUE",
                 Material.NETHER_STAR,
                 1006,
                 new ThunderAbility()),
    
    NATURE_FRUIT("§2§l🌿 §a§lNATURE FRUIT §2§l🌿",
                "§7§oOne with the natural world",
                Color.fromRGB(0x44FF44),
                "GREEN",
                Material.OAK_SAPLING,
                1007,
                new NatureAbility()),
    
    ICE_FRUIT("§b§l❄️ §f§lICE FRUIT §b§l❄️",
             "§7§oFreeze your enemies solid",
             Color.fromRGB(0x88FFFF),
             "LIGHT_BLUE",
             Material.PACKED_ICE,
             1008,
             new IceAbility()),
    
    STAR_FRUIT("§d§l⭐ §5§lSTAR FRUIT §d§l⭐",
              "§7§oHarness cosmic energy",
              Color.fromRGB(0xFF88FF),
              "MAGENTA",
              Material.NETHER_STAR,
              1009,
              new StarAbility()),
    
    THIEF_FRUIT("§8§l🎭 §0§lTHIEF FRUIT §8§l🎭",
               "§7§oSteal abilities from others",
               Color.fromRGB(0x333333),
package com.example.magicfruits;

import com.example.magicfruits.abilities.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public enum FruitType {
    BUDDHA_FRUIT("§e§l✨ §6§lBUDDHA FRUIT §e§l✨", 
                "§7§oEnlightened by ancient wisdom",
                Color.fromRGB(0xFFD700),
                Material.NETHER_STAR,
                1001,
                new BuddhaAbility()),
    
    CRYSTAL_FRUIT("§b§l💎 §3§lCRYSTAL FRUIT §b§l💎",
                 "§7§oPure crystalline power",
                 Color.fromRGB(0x00FFFF),
                 Material.NETHER_STAR,
                 1002,
                 new CrystalAbility()),
    
    DRAGON_FRUIT("§c§l🐉 §4§lDRAGON FRUIT §c§l🐉",
                "§7§oWrath of the ancient dragons",
                Color.fromRGB(0xFF4444),
                Material.NETHER_STAR,
                1003,
                new DragonAbility()),
    
    PHOENIX_FRUIT("§6§l🔥 §e§lPHOENIX FRUIT §6§l🔥",
                 "§7§oReborn from eternal flames",
                 Color.fromRGB(0xFFA500),
                 Material.NETHER_STAR,
                 1004,
                 new PhoenixAbility()),
    
    VOID_FRUIT("§5§l🌑 §8§lVOID FRUIT §5§l🌑",
              "§7§oEmbrace the darkness within",
              Color.fromRGB(0xAA00AA),
              Material.NETHER_STAR,
              1005,
              new VoidAbility()),
    
    THUNDER_FRUIT("§3§l⚡ §b§lTHUNDER FRUIT §3§l⚡",
                 "§7§oCommand the storm itself",
                 Color.fromRGB(0x44AAFF),
                 Material.NETHER_STAR,
                 1006,
                 new ThunderAbility()),
    
    NATURE_FRUIT("§2§l🌿 §a§lNATURE FRUIT §2§l🌿",
                "§7§oOne with the natural world",
                Color.fromRGB(0x44FF44),
                Material.NETHER_STAR,
                1007,
                new NatureAbility()),
    
    ICE_FRUIT("§b§l❄️ §f§lICE FRUIT §b§l❄️",
             "§7§oFreeze your enemies solid",
             Color.fromRGB(0x88FFFF),
             Material.NETHER_STAR,
             1008,
             new IceAbility()),
    
    STAR_FRUIT("§d§l⭐ §5§lSTAR FRUIT §d§l⭐",
              "§7§oHarness cosmic energy",
              Color.fromRGB(0xFF88FF),
              Material.NETHER_STAR,
              1009,
              new StarAbility()),
    
    THIEF_FRUIT("§8§l🎭 §0§lTHIEF FRUIT §8§l🎭",
               "§7§oSteal abilities from others",
               Color.fromRGB(0x333333),
               Material.NETHER_STAR,
               1010,
               new ThiefAbility()),
    
    BLOOD_FRUIT("§4§l🩸 §c§lBLOOD FRUIT §4§l🩸",
               "§7§oSacrifice for ultimate power",
               Color.fromRGB(0xFF4444),
               Material.NETHER_STAR,
               1011,
               new BloodAbility());
    
    private final String displayName;
    private final String description;
    private final Color dyeColor;
    private final Material itemMaterial;
    private final int customModelData;
    private final Ability ability;
    
    FruitType(String displayName, String description, Color dyeColor, 
              Material itemMaterial, int customModelData, Ability ability) {
        this.displayName = displayName;
        this.description = description;
        this.dyeColor = dyeColor;
        this.itemMaterial = itemMaterial;
        this.customModelData = customModelData;
        this.ability = ability;
    }
    
    public ItemStack createItem() {
        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));
        meta.setCustomModelData(customModelData);
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("§8§m----------------------------------------"));
        lore.add(Component.text("§7§l✦ §f§lMYSTICAL ARTIFACT §7§l✦"));
        lore.add(Component.text("§8§m----------------------------------------"));
        lore.add(Component.empty());
        lore.add(Component.text("§7§o" + description).decoration(TextDecoration.ITALIC, true));
        lore.add(Component.empty());
        lore.add(Component.text("§6§l✨ ABILITIES ✨"));
        lore.add(Component.text(" §e▶ §fRight Click: §7" + ability.getPrimaryDescription()));
        lore.add(Component.text(" §e▶ §fCrouch + Right Click: §7" + ability.getSecondaryDescription()));
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
    
    public ItemStack createDisplayItem() {
        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName));
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }
    
    public boolean isFruitItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getItemMeta() == null || item.getItemMeta().displayName() == null) return false;
        return item.getItemMeta().displayName().equals(Component.text(displayName));
    }
    
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName));
        meta.setCustomModelData(customModelData);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7" + description));
        lore.add(Component.empty());
        lore.add(Component.text("§eClick to give this fruit!"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    public static FruitType fromItem(ItemStack item) {
        if (item == null) return null;
        for (FruitType fruit : values()) {
            if (fruit.isFruitItem(item)) return fruit;
        }
        return null;
    }
    
    public String getDisplayName() { return displayName; }
    public Ability getAbility() { return ability; }
    public int getCustomModelData() { return customModelData; }
    public Material getItemMaterial() { return itemMaterial; }
}conMaterial() { return icon; }
}

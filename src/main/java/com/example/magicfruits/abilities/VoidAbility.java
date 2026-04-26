package com.example.magicfruits.abilities;

import com.example.magicfruits.MagicFruits;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class VoidAbility implements Ability {

    @Override
    public void execute(Player player, boolean isSecondary) {
        MagicFruits plugin = MagicFruits.getInstance();

        if (isSecondary) {
            executeTeleport(player, plugin);
        } else {
            executeInvisibility(player, plugin);
        }
    }

    // ======================= PRIMARY =======================
    private void executeInvisibility(Player player, MagicFruits plugin) {
        // Apply effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 1));

        // Sound
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
        }

        // Advanced particle animation: dark ascending spiral + fading rings
        startVoidAscensionParticles(player, plugin);

        player.sendTitle("§5§l🌑 VOID WALKER", "§eYou embrace the darkness", 10, 60, 10);
        player.sendMessage("§5§l🌑 §fYou become one with the void – invisible and night‑vision for 10 seconds.");
    }

    // ======================= SECONDARY =======================
    private void executeTeleport(Player player, MagicFruits plugin) {
        // Store old location for the rift effect
        Location oldLoc = player.getLocation().clone();

        // Random teleport (20 block radius)
        double radius = 20;
        double angle = ThreadLocalRandom.current().nextDouble(2 * Math.PI);
        double distance = ThreadLocalRandom.current().nextDouble(5, radius);
        double x = Math.cos(angle) * distance;
        double z = Math.sin(angle) * distance;
        Location newLoc = oldLoc.clone().add(x, 0, z);
        newLoc.setY(player.getWorld().getHighestBlockYAt(newLoc) + 1);

        player.teleport(newLoc);

        // Rift opening at start location
        createRift(oldLoc, plugin);
        // Rift closing at new location
        createRift(newLoc, plugin);

        // Sound
        if (plugin.getDataManager().isSoundsEnabled()) {
            player.playSound(oldLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
            player.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.3f);
            player.playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 0.8f, 0.9f);
        }

        player.sendTitle("§5§l🌀 VOID SHIFT", "§eYou tear through space", 10, 50, 10);
        player.sendMessage("§5§l🌀 §fYou vanish into the void and reappear elsewhere.");
    }

    // ======================= PARTICLE EFFECTS =======================

    /**
     * Dark spiralling particles that rise from the player's feet, forming a void crown.
     */
    private void startVoidAscensionParticles(Player player, MagicFruits plugin) {
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 60; // 3 seconds (20 ticks per second)

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 0.2, 0);
                double t = ticks / 5.0; // progression

                // 1) Rising smoke rings (expanding)
                double ringRadius = 0.8 + t * 0.3;
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i + ticks * 10);
                    double x = Math.cos(rad) * ringRadius;
                    double z = Math.sin(rad) * ringRadius;
                    double yOffset = t * 0.5; // rises
                    Location pLoc = loc.clone().add(x, yOffset, z);
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 0, 0, 0, 0, 1,
                                new Particle.DustOptions(Color.fromRGB(0x2E1A47), 0.8f));
                        pLoc.getWorld().spawnParticle(Particle.PORTAL, pLoc, 0, 0, 0, 0, 1);
                    }
                }

                // 2) Helix / double helix of dark particles around the player
                for (int i = 0; i < 2; i++) {
                    double offset = (i == 0 ? 0 : Math.PI);
                    for (int angleStep = 0; angleStep < 360; angleStep += 20) {
                        double rad = Math.toRadians(angleStep + ticks * 12 + offset * 180);
                        double radius = 1.3;
                        double x = Math.cos(rad) * radius;
                        double z = Math.sin(rad) * radius;
                        double y = 0.8 + Math.sin(rad * 2) * 0.4;
                        Location hLoc = loc.clone().add(x, y, z);
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            hLoc.getWorld().spawnParticle(Particle.SPELL_WITCH, hLoc, 0, 0, 0, 0, 2);
                        }
                    }
                }

                // 3) Floating void embers (random sparks)
                for (int j = 0; j < 4; j++) {
                    double angleRand = Math.random() * 2 * Math.PI;
                    double radRand = Math.random() * 2.2;
                    double xr = Math.cos(angleRand) * radRand;
                    double zr = Math.sin(angleRand) * radRand;
                    double yr = Math.random() * 2.2;
                    Location spark = loc.clone().add(xr, yr, zr);
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        spark.getWorld().spawnParticle(Particle.DUST, spark, 0, 0, 0, 0, 1,
                                new Particle.DustOptions(Color.fromRGB(0x5A2E8C), 0.6f));
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Creates a dimensional rift at a location – rotating vortex with dark lightning.
     */
    private void createRift(Location center, MagicFruits plugin) {
        World world = center.getWorld();
        new BukkitRunnable() {
            int age = 0;
            final int maxAge = 25; // 1.25 seconds

            @Override
            public void run() {
                if (age >= maxAge) {
                    this.cancel();
                    return;
                }

                double radius = 1.2 + age * 0.08;
                double height = 1.0 + age * 0.1;

                // Horizontal rotating rings
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i + age * 20);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    Location ringLoc = center.clone().add(x, 0.2, z);
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        ringLoc.getWorld().spawnParticle(Particle.DUST, ringLoc, 0, 0, 0, 0, 1,
                                new Particle.DustOptions(Color.fromRGB(0x3B1E6B), 0.9f));
                        ringLoc.getWorld().spawnParticle(Particle.PORTAL, ringLoc, 0, 0, 0, 0, 1);
                    }
                }

                // Upward spiral energy
                for (int y = 0; y <= 2; y++) {
                    double ang = Math.toRadians(age * 25 + y * 60);
                    double offX = Math.cos(ang) * (radius - 0.3);
                    double offZ = Math.sin(ang) * (radius - 0.3);
                    Location spiral = center.clone().add(offX, y * 0.6, offZ);
                    if (plugin.getDataManager().isParticlesEnabled()) {
                        spiral.getWorld().spawnParticle(Particle.END_ROD, spiral, 0, 0, 0, 0, 1);
                    }
                }

                // Dark lightning streaks (random)
                if (age % 3 == 0) {
                    for (int k = 0; k < 3; k++) {
                        double angleLight = Math.random() * 2 * Math.PI;
                        double len = 0.8 + Math.random() * 1.2;
                        double xL = Math.cos(angleLight) * len;
                        double zL = Math.sin(angleLight) * len;
                        Location boltStart = center.clone().add(xL, 0.2, zL);
                        Location boltEnd = center.clone().add(xL * 1.5, 1.0 + Math.random(), zL * 1.5);
                        if (plugin.getDataManager().isParticlesEnabled()) {
                            boltStart.getWorld().spawnParticle(Particle.SPELL_WITCH, boltStart, 0, 0, 0, 0, 2);
                            boltEnd.getWorld().spawnParticle(Particle.SPELL_WITCH, boltEnd, 0, 0, 0, 0, 2);
                        }
                    }
                }

                age++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ======================= DESCRIPTIONS =======================
    @Override
    public String getPrimaryDescription() {
        return "Void Walker (10s invisibility + night vision, dark ascending spiral)";
    }

    @Override
    public String getSecondaryDescription() {
        return "Void Shift (random teleport with rift effects, 2min cooldown)";
    }
}

package com.example.magicfruits.abilities;

import org.bukkit.entity.Player;

public interface Ability {
    void execute(Player player, boolean isSecondary);
    String getPrimaryDescription();
    String getSecondaryDescription();
}

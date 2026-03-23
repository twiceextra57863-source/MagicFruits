package com.example.magicfruits.managers;

import com.example.magicfruits.FruitType;
import com.example.magicfruits.MagicFruits;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandHandler implements CommandExecutor, TabCompleter {
    
    private final MagicFruits plugin;
    
    public CommandHandler(MagicFruits plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getAdminGUI().openMainDashboard((Player) sender);
            } else {
                sender.sendMessage("§cThis command can only be used by players!");
            }
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "spin":
                handleSpinCommand(sender, args);
                break;
            case "give":
                handleGiveCommand(sender, args);
                break;
            case "dashboard":
            case "admin":
                if (sender instanceof Player) {
                    plugin.getAdminGUI().openMainDashboard((Player) sender);
                } else {
                    sender.sendMessage("§cThis command can only be used by players!");
                }
                break;
            case "reload":
                if (sender.hasPermission("magicfruits.admin")) {
                    plugin.reloadConfig();
                    plugin.getDataManager().loadSettings();
                    plugin.getDataManager().loadResetData();
                    sender.sendMessage("§aMagicFruits configuration reloaded!");
                } else {
                    sender.sendMessage("§cYou don't have permission!");
                }
                break;
            case "reset":
                handleResetCommand(sender, args);
                break;
            case "grace":
                handleGraceCommand(sender, args);
                break;
            default:
                sender.sendMessage("§cUnknown command! Use: /magicfruits [spin|give|dashboard|reload|reset|grace]");
                break;
        }
        
        return true;
    }
    
    private void handleSpinCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                plugin.getSpinManager().startFruitSpin((Player) sender);
            } else {
                sender.sendMessage("§cPlease specify a player!");
            }
        } else if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                plugin.getSpinManager().startFruitSpin(target);
                sender.sendMessage("§aStarted fruit spin for §e" + target.getName());
            } else {
                sender.sendMessage("§cPlayer not found!");
            }
        } else if (args.length == 3 && args[2].equalsIgnoreCase("all")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getSpinManager().startFruitSpin(player);
            }
            sender.sendMessage("§aStarted fruit spin for §eALL §aplayers!");
        }
    }
    
    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /magicfruits give <player> <fruit>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        try {
            FruitType fruit = FruitType.valueOf(args[2].toUpperCase());
            target.getInventory().addItem(fruit.createItem());
            sender.sendMessage("§aGave §e" + fruit.getDisplayName() + " §ato §e" + target.getName());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid fruit! Available: " + Arrays.toString(FruitType.values()));
        }
    }
    
    private void handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("magicfruits.admin")) {
            sender.sendMessage("§cYou don't have permission!");
            return;
        }
        
        if (args.length == 1) {
            sender.sendMessage("§cUsage: /magicfruits reset <player|all>");
            return;
        }
        
        if (args[1].equalsIgnoreCase("all")) {
            plugin.getDataManager().resetAllPlayersData();
            sender.sendMessage("§aAll player data has been reset!");
        } else {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                plugin.getDataManager().resetPlayerData(target);
                sender.sendMessage("§aReset data for §e" + target.getName());
            } else {
                sender.sendMessage("§cPlayer not found!");
            }
        }
    }
    
    private void handleGraceCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("magicfruits.admin")) {
            sender.sendMessage("§cYou don't have permission!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /magicfruits grace <start|stop> [minutes]");
            return;
        }
        
        if (args[1].equalsIgnoreCase("start")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /magicfruits grace start <minutes>");
                return;
            }
            
            try {
                int minutes = Integer.parseInt(args[2]);
                if (minutes <= 0) {
                    sender.sendMessage("§cMinutes must be positive!");
                    return;
                }
                if (minutes > 60) {
                    sender.sendMessage("§cMaximum grace period is 60 minutes!");
                    return;
                }
                plugin.getGracePeriodManager().startGrace(minutes);
                sender.sendMessage("§aGrace period started for " + minutes + " minutes!");
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number!");
            }
        } else if (args[1].equalsIgnoreCase("stop")) {
            plugin.getGracePeriodManager().stopGrace();
            sender.sendMessage("§aGrace period stopped!");
        } else {
            sender.sendMessage("§cUsage: /magicfruits grace <start|stop> [minutes]");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spin", "give", "dashboard", "reload", "reset", "grace");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return null;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.stream(FruitType.values()).map(Enum::name).map(String::toLowerCase).toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spin")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return suggestions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return suggestions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("grace")) {
            return Arrays.asList("start", "stop");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("grace") && args[1].equalsIgnoreCase("start")) {
            return Arrays.asList("5", "10", "15", "30", "60");
        }
        return Collections.emptyList();
    }
}

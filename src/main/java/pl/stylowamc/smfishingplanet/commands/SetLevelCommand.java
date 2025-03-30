package pl.stylowamc.smfishingplanet.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class SetLevelCommand implements CommandExecutor, TabCompleter {
    private final SMFishingPlanet plugin;
    
    public SetLevelCommand(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smfishing.setlevel")) {
            MessageUtils.sendMessage(sender, "no_permission");
            return true;
        }
        
        if (args.length != 2) {
            sender.sendMessage(MessageUtils.colorize("&cUżycie: /setlevel <gracz> <poziom>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(MessageUtils.colorize("&cGracz nie jest online!"));
            return true;
        }
        
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.colorize("&cPoziom musi być liczbą!"));
            return true;
        }
        
        if (level < 1 || level > plugin.getConfigManager().getConfig().getInt("levels.max_level", 100)) {
            sender.sendMessage(MessageUtils.colorize("&cPoziom musi być między 1 a " + 
                plugin.getConfigManager().getConfig().getInt("levels.max_level", 100) + "!"));
            return true;
        }
        
        // Ustaw poziom
        plugin.getPlayerDataManager().setLevel(target, level);
        
        // Przeładuj dane gracza
        plugin.getPlayerDataManager().loadPlayerData(target);
        
        // Wyślij wiadomości
        sender.sendMessage(MessageUtils.colorize("&aUstawiono poziom &e" + level + " &adla gracza &e" + target.getName()));
        target.sendMessage(MessageUtils.colorize("&aTwój poziom rybactwa został ustawiony na &e" + level));
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            completions.add("1");
            completions.add("10");
            completions.add("25");
            completions.add("50");
            completions.add("100");
        }
        
        return completions;
    }
} 
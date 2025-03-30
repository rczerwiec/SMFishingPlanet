package pl.stylowamc.smfishingplanet.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.items.FishingLine;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZylkaCommand implements CommandExecutor, TabCompleter {
    private final SMFishingPlanet plugin;
    private final FishingLine fishingLine;
    
    public ZylkaCommand(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.fishingLine = new FishingLine(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.colorize("&cTa komenda jest dostępna tylko dla graczy!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("smfishing.zylka")) {
            MessageUtils.sendMessage(player, "no_permission");
            return true;
        }
        
        if (args.length != 1) {
            player.sendMessage(MessageUtils.colorize("&cUżycie: /zylka <2mm/4mm/5mm/6mm>"));
            return true;
        }
        
        ItemStack zylka = null;
        switch (args[0].toLowerCase()) {
            case "2mm":
                zylka = fishingLine.createFishingLine();
                break;
            case "4mm":
                zylka = fishingLine.createFishingLine4mm();
                break;
            case "5mm":
                zylka = fishingLine.createFishingLine5mm();
                break;
            case "6mm":
                zylka = fishingLine.createFishingLine6mm();
                break;
            default:
                player.sendMessage(MessageUtils.colorize("&cNieprawidłowy rozmiar żyłki! Dostępne: 2mm, 4mm, 5mm, 6mm"));
                return true;
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            MessageUtils.sendMessage(player, "fishing.inventory_full");
            return true;
        }
        
        player.getInventory().addItem(zylka);
        player.sendMessage(MessageUtils.colorize("&aOtrzymałeś żyłkę " + args[0]));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("2mm", "4mm", "5mm", "6mm");
            List<String> suggestions = new ArrayList<>();
            for (String completion : completions) {
                if (completion.toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(completion);
                }
            }
            return suggestions;
        }
        return new ArrayList<>();
    }
} 
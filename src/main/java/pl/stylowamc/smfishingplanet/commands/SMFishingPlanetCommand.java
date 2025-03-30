package pl.stylowamc.smfishingplanet.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

public class SMFishingPlanetCommand implements CommandExecutor {
    private final SMFishingPlanet plugin;

    public SMFishingPlanetCommand(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("smfishing.admin")) {
                MessageUtils.sendMessage(sender, "no_permission");
                return true;
            }
            
            plugin.reloadPlugin();
            sender.sendMessage("§8[§bSMFishing§8] §aKonfiguracja została przeładowana!");
            return true;
        }
        
        sender.sendMessage("§8[§bSMFishing§8] §7Plugin SMFishingPlanet v" + plugin.getDescription().getVersion());
        sender.sendMessage("§8[§bSMFishing§8] §7Użyj §f/smfishingplanet reload §7aby przeładować konfigurację.");
        return true;
    }
} 
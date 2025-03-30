package pl.stylowamc.smfishingplanet.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

public class SprzedajRybyCommand implements CommandExecutor {
    private final SMFishingPlanet plugin;

    public SprzedajRybyCommand(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage((Player) sender, "player_only");
            return true;
        }

        Player player = (Player) sender;
        plugin.getSellMenu().openMainMenu(player);
        return true;
    }
} 
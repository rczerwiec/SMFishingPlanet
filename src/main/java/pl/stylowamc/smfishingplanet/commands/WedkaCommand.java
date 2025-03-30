package pl.stylowamc.smfishingplanet.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;

public class WedkaCommand implements CommandExecutor {
    private final SMFishingPlanet plugin;

    public WedkaCommand(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage((Player) sender, "player_only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            MessageUtils.sendMessage(player, "rod.invalid_type");
            return true;
        }

        String type = args[0].toLowerCase();
        int requiredLevel = plugin.getConfigManager().getConfig().getInt("fishing_rod." + type + ".required_level", -1);
        
        if (requiredLevel == -1) {
            MessageUtils.sendMessage(player, "rod.invalid_type");
            return true;
        }

        int playerLevel = plugin.getPlayerDataManager().getLevel(player);
        if (playerLevel < requiredLevel) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("level", String.valueOf(requiredLevel));
            MessageUtils.sendMessage(player, "rod.level_too_low", placeholders);
            return true;
        }

        ItemStack rod = plugin.getFishingRod().createFishingRod(type);
        if (rod != null) {
            player.getInventory().addItem(rod);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("rod_name", rod.getItemMeta().getDisplayName());
            MessageUtils.sendMessage(player, "rod.received", placeholders);
        }

        return true;
    }
} 
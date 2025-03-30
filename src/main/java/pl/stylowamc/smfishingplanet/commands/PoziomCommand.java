package pl.stylowamc.smfishingplanet.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;

public class PoziomCommand implements CommandExecutor {
    private final SMFishingPlanet plugin;

    public PoziomCommand(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, "player_only");
            return true;
        }

        Player player = (Player) sender;
        
        // Pobierz aktualny poziom i XP
        int level = plugin.getPlayerDataManager().getLevel(player);
        double currentXp = plugin.getPlayerDataManager().getCurrentXp(player);
        double requiredXp = plugin.getPlayerDataManager().getRequiredXp(level + 1);
        double progress = (currentXp / requiredXp) * 100;
        
        // Wyślij informacje o poziomie
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("level", String.valueOf(level));
        placeholders.put("current_xp", String.format("%.1f", currentXp));
        placeholders.put("required_xp", String.format("%.1f", requiredXp));
        placeholders.put("progress", String.format("%.1f", progress));
        
        MessageUtils.sendMessage(player, "level.current", placeholders);
        MessageUtils.sendMessage(player, "level.progress", placeholders);
        
        return true;
    }
} 
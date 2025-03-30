package pl.stylowamc.smfishingplanet.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;

public class StatystykiCommand implements CommandExecutor {
    private final SMFishingPlanet plugin;

    public StatystykiCommand(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, "player_only");
            return true;
        }

        Player player = (Player) sender;
        Map<String, Object> stats = plugin.getPlayerDataManager().getPlayerStats(player);
        
        if (stats.isEmpty()) {
            MessageUtils.sendMessage(player, "stats.not_found");
            return true;
        }
        
        // Wyświetl nagłówek
        player.sendMessage("§8§m---------------------§8[ §b§lSTATYSTYKI ŁOWIENIA §8]§m---------------------");
        
        // Główne statystyki
        int totalCatches = (int) stats.getOrDefault("total_catches", 0);
        int failedCatches = (int) stats.getOrDefault("failed_catches", 0);
        int trashCaught = (int) stats.getOrDefault("trash_caught", 0);
        double successRate = (double) stats.getOrDefault("success_rate", 0.0);
        
        player.sendMessage("§7• §fZłowione ryby: §a" + totalCatches);
        player.sendMessage("§7• §fZłowione śmieci: §e" + trashCaught);
        player.sendMessage("§7• §fNieudane próby: §c" + failedCatches);
        player.sendMessage("§7• §fCałkowite próby: §e" + (totalCatches + failedCatches + trashCaught));
        player.sendMessage("§7• §fWskaźnik sukcesu: §b" + String.format("%.1f", successRate) + "%");
        
        // Statystyki wagi
        int smallFish = (int) stats.getOrDefault("small_fish", 0);
        int mediumFish = (int) stats.getOrDefault("medium_fish", 0);
        int largeFish = (int) stats.getOrDefault("large_fish", 0);
        int hugeFish = (int) stats.getOrDefault("huge_fish", 0);
        
        player.sendMessage("");
        player.sendMessage("§7• §fMałe ryby (<1kg): §a" + smallFish);
        player.sendMessage("§7• §fŚrednie ryby (1-3kg): §a" + mediumFish);
        player.sendMessage("§7• §fDuże ryby (3-7kg): §a" + largeFish);
        player.sendMessage("§7• §fOgromne ryby (>7kg): §a" + hugeFish);
        
        // Rekord
        double heaviestFish = (double) stats.getOrDefault("heaviest_fish", 0.0);
        String heaviestFishName = (String) stats.getOrDefault("heaviest_fish_name", "Brak");
        String heaviestFishDate = (String) stats.getOrDefault("heaviest_fish_date", "");
        
        player.sendMessage("");
        player.sendMessage("§6☆ §lREKORD §6☆");
        player.sendMessage("§7• §fNajcięższa ryba: §e" + heaviestFishName + " §7(§e" + String.format("%.2f", heaviestFish) + "kg§7)");
        if (!heaviestFishDate.isEmpty()) {
            player.sendMessage("§7• §fData złowienia: §e" + heaviestFishDate);
        }
        
        // Ekonomia
        double totalEarnings = (double) stats.getOrDefault("total_earnings", 0.0);
        player.sendMessage("");
        player.sendMessage("§7• §fZarobki ze sprzedaży: §a$" + String.format("%.2f", totalEarnings));
        
        player.sendMessage("§8§m---------------------------------------------------------------");
        
        return true;
    }
} 
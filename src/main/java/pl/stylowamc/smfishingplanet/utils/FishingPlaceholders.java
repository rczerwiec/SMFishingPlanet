package pl.stylowamc.smfishingplanet.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.managers.PlayerDataManager.PlayerData;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.List;

public class FishingPlaceholders extends PlaceholderExpansion {
    private final SMFishingPlanet plugin;

    public FishingPlaceholders(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "smfishing";
    }

    @Override
    public String getAuthor() {
        return "StyloWAMC";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        PlayerData playerData = plugin.getPlayerDataManager().getOrCreatePlayerData(player.getUniqueId());
        if (playerData == null) {
            return "";
        }

        // Poziom rybactwa
        if (identifier.equals("level")) {
            return String.valueOf(playerData.getLevel());
        }

        // XP rybactwa
        if (identifier.equals("xp")) {
            return String.format("%.1f", playerData.getXp());
        }

        // Wymagane XP do następnego poziomu
        if (identifier.equals("required_xp")) {
            return String.format("%.1f", plugin.getPlayerDataManager().getRequiredXp(playerData.getLevel() + 1));
        }

        // Postęp do następnego poziomu (w procentach)
        if (identifier.equals("progress")) {
            double requiredXp = plugin.getPlayerDataManager().getRequiredXp(playerData.getLevel() + 1);
            double progress = (playerData.getXp() / requiredXp) * 100;
            return String.format("%.1f", progress);
        }

        // Łączna liczba złowionych ryb
        if (identifier.equals("total_catches")) {
            return String.valueOf(playerData.getTotalCatches());
        }

        // Liczba złowionych ryb według rzadkości
        if (identifier.startsWith("catches_")) {
            String rarity = identifier.substring(8);
            return String.valueOf(playerData.getRarityCaught().getOrDefault(rarity, 0));
        }

        // Top 10 graczy według poziomu
        if (identifier.equals("top_level")) {
            return getTopPlayersByLevel();
        }

        // Top 10 graczy według złowionych ryb
        if (identifier.equals("top_catches")) {
            return getTopPlayersByCatches();
        }
        
        // Pojedynczy gracz z top 10 według poziomu
        if (identifier.startsWith("top_level_")) {
            try {
                int position = Integer.parseInt(identifier.substring(10));
                if (position < 1 || position > 10) {
                    return "";
                }
                return getTopPlayerByLevel(position - 1);
            } catch (NumberFormatException e) {
                return "";
            }
        }
        
        // Pojedynczy gracz z top 10 według złowionych ryb
        if (identifier.startsWith("top_catches_")) {
            try {
                int position = Integer.parseInt(identifier.substring(12));
                if (position < 1 || position > 10) {
                    return "";
                }
                return getTopPlayerByCatches(position - 1);
            } catch (NumberFormatException e) {
                return "";
            }
        }

        return null;
    }

    private String getTopPlayersByLevel() {
        return plugin.getPlayerDataManager().getAllPlayerData().entrySet().stream()
                .sorted(Map.Entry.<UUID, PlayerData>comparingByValue(Comparator.comparingInt(PlayerData::getLevel)).reversed())
                .limit(10)
                .map(entry -> {
                    String playerName = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
                    return playerName + ": " + entry.getValue().getLevel();
                })
                .collect(Collectors.joining(", "));
    }

    private String getTopPlayersByCatches() {
        return plugin.getPlayerDataManager().getAllPlayerData().entrySet().stream()
                .sorted(Map.Entry.<UUID, PlayerData>comparingByValue(Comparator.comparingInt(PlayerData::getTotalCatches)).reversed())
                .limit(10)
                .map(entry -> {
                    String playerName = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
                    return playerName + ": " + entry.getValue().getTotalCatches();
                })
                .collect(Collectors.joining(", "));
    }
    
    private String getTopPlayerByLevel(int position) {
        List<Map.Entry<UUID, PlayerData>> topPlayers = plugin.getPlayerDataManager().getAllPlayerData().entrySet().stream()
                .sorted(Map.Entry.<UUID, PlayerData>comparingByValue(Comparator.comparingInt(PlayerData::getLevel)).reversed())
                .limit(10)
                .collect(Collectors.toList());
                
        if (position >= topPlayers.size()) {
            return "";
        }
        
        Map.Entry<UUID, PlayerData> entry = topPlayers.get(position);
        String playerName = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
        return playerName + ": " + entry.getValue().getLevel();
    }
    
    private String getTopPlayerByCatches(int position) {
        List<Map.Entry<UUID, PlayerData>> topPlayers = plugin.getPlayerDataManager().getAllPlayerData().entrySet().stream()
                .sorted(Map.Entry.<UUID, PlayerData>comparingByValue(Comparator.comparingInt(PlayerData::getTotalCatches)).reversed())
                .limit(10)
                .collect(Collectors.toList());
                
        if (position >= topPlayers.size()) {
            return "";
        }
        
        Map.Entry<UUID, PlayerData> entry = topPlayers.get(position);
        String playerName = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
        return playerName + ": " + entry.getValue().getTotalCatches();
    }
} 
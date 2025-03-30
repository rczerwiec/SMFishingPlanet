package pl.stylowamc.smfishingplanet.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {
    private final SMFishingPlanet plugin;
    private final Map<UUID, PlayerData> playerData;
    private final File dataFolder;

    public PlayerDataManager(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        
        if (!playerFile.exists()) {
            // Stwórz nowe dane dla gracza
            playerData.put(uuid, new PlayerData(
                1, // Początkowy poziom
                0.0 // XP
            ));
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        double balance = config.getDouble("balance", plugin.getConfigManager().getStartingBalance());
        int level = config.getInt("level", 1); // Domyślny poziom 1
        double xp = config.getDouble("xp", 0.0);

        playerData.put(uuid, new PlayerData(level, xp));
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerData.get(uuid);
        if (data == null) return;

        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        config.set("balance", getBalance(player));
        config.set("level", data.getLevel());
        config.set("xp", data.getXp());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie można zapisać danych gracza: " + player.getName(), e);
        }
    }

    public void saveAllPlayers() {
        for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
            File playerFile = new File(dataFolder, entry.getKey().toString() + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

            PlayerData data = entry.getValue();
            config.set("balance", getBalance(plugin.getServer().getPlayer(entry.getKey())));
            config.set("level", data.getLevel());
            config.set("xp", data.getXp());

            try {
                config.save(playerFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie można zapisać danych gracza: " + entry.getKey(), e);
            }
        }
    }

    public void addBalance(Player player, double amount) {
        PlayerData data = getPlayerData(player);
        if (data != null) {
            double newBalance = data.getBalance() + amount;
            data.setBalance(newBalance);
        }
    }

    public void removeBalance(Player player, double amount) {
        PlayerData data = getPlayerData(player);
        if (data != null) {
            double newBalance = Math.max(0, data.getBalance() - amount);
            data.setBalance(newBalance);
        }
    }

    public double getBalance(Player player) {
        PlayerData data = getPlayerData(player);
        return data != null ? data.getBalance() : 0.0;
    }

    public void addXp(Player player, double xp) {
        PlayerData data = playerData.getOrDefault(player.getUniqueId(), new PlayerData());
        data.addXp(xp);

        // Sprawdź czy gracz może awansować na wyższy poziom
        int currentLevel = data.getLevel();
        double requiredXp = getRequiredXp(currentLevel + 1);

        while (data.getCurrentXp() >= requiredXp && currentLevel < plugin.getConfigManager().getConfig().getInt("levels.max_level", 100)) {
            // Awansuj na następny poziom
            data.setLevel(currentLevel + 1);
            data.addXp(-requiredXp); // Odejmij wymagane XP
            
            // Wyślij wiadomość o awansie
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("level", String.valueOf(currentLevel + 1));
            MessageUtils.sendMessage(player, "level.up", placeholders);

            // Zaktualizuj zmienne do następnej iteracji
            currentLevel = data.getLevel();
            requiredXp = getRequiredXp(currentLevel + 1);
        }

        playerData.put(player.getUniqueId(), data);
    }

    public double getRequiredXp(int level) {
        return 100 * Math.pow(1.5, level - 1);
    }

    public int getLevel(Player player) {
        return playerData.getOrDefault(player.getUniqueId(), new PlayerData()).getLevel();
    }

    public double getXp(Player player) {
        PlayerData data = getPlayerData(player);
        return data != null ? data.getXp() : 0.0;
    }

    public double getCurrentXp(Player player) {
        return playerData.getOrDefault(player.getUniqueId(), new PlayerData()).getCurrentXp();
    }

    public void setLevel(Player player, int level) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerData.get(uuid);
        
        if (data == null) {
            data = new PlayerData(level, 0.0);
            playerData.put(uuid, data);
        } else {
            data.setLevel(level);
            data.setCurrentXp(0); // Reset XP przy zmianie poziomu
        }
        
        // Natychmiast zapisz dane
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        
        config.set("level", level);
        config.set("xp", 0.0);
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie można zapisać danych gracza: " + player.getName(), e);
        }
        
        // Wyślij wiadomość o aktualnym poziomie
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("level", String.valueOf(level));
        MessageUtils.sendMessage(player, "level.current", placeholders);
    }

    private PlayerData getPlayerData(Player player) {
        return playerData.get(player.getUniqueId());
    }

    private static class PlayerData {
        private double balance;
        private int level;
        private double xp;

        public PlayerData() {
            this.level = 1;
            this.xp = 0.0;
        }

        public PlayerData(int level, double xp) {
            this.level = level;
            this.xp = xp;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public double getXp() {
            return xp;
        }

        public void setXp(double xp) {
            this.xp = xp;
        }

        public void addXp(double amount) {
            this.xp += amount;
        }

        public double getCurrentXp() {
            return xp;
        }

        public void setCurrentXp(double xp) {
            this.xp = xp;
        }
    }
} 
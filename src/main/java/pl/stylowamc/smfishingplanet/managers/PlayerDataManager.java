package pl.stylowamc.smfishingplanet.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.models.Fish;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

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
            PlayerData data = new PlayerData();
            data.setLevel(1);
            data.setXp(0.0);
            playerData.put(uuid, data);
            plugin.getLogger().info("Utworzono nowe dane dla gracza: " + player.getName());
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
     
        // Wczytaj podstawowe dane
        int level = config.getInt("level", 1); // Domyślny poziom 1
        double xp = config.getDouble("xp", 0.0);
        double balance = config.getDouble("balance", plugin.getConfigManager().getConfig().getDouble("economy.starting_balance", 0.0));

        PlayerData data = new PlayerData();
        data.setLevel(level);
        data.setXp(xp);
        data.setBalance(balance);
        
        // Wczytaj statystyki łowienia
        if (config.contains("stats")) {
            // Proste statystyki
            if (config.contains("stats.total_catches")) {
                int totalCatches = config.getInt("stats.total_catches", 0);
                for (int i = 0; i < totalCatches; i++) {
                    data.incrementCatches();
                }
            }
            
            if (config.contains("stats.failed_catches")) {
                int failedCatches = config.getInt("stats.failed_catches", 0);
                for (int i = 0; i < failedCatches; i++) {
                    data.incrementFailedCatches();
                }
            }
            
            if (config.contains("stats.trash_caught")) {
                int trashCaught = config.getInt("stats.trash_caught", 0);
                for (int i = 0; i < trashCaught; i++) {
                    data.incrementTrashCaught();
                }
            }
            
            // Złowione ryby według nazwy
            if (config.contains("stats.fish_caught")) {
                ConfigurationSection fishCaughtSection = config.getConfigurationSection("stats.fish_caught");
                if (fishCaughtSection != null) {
                    for (String fishName : fishCaughtSection.getKeys(false)) {
                        int count = fishCaughtSection.getInt(fishName, 0);
                        for (int i = 0; i < count; i++) {
                            data.addFishCaught(fishName);
                        }
                    }
                }
            }
            
            // Złowione ryby według rzadkości
            if (config.contains("stats.rarity_caught")) {
                ConfigurationSection rarityCaughtSection = config.getConfigurationSection("stats.rarity_caught");
                if (rarityCaughtSection != null) {
                    for (String rarity : rarityCaughtSection.getKeys(false)) {
                        int count = rarityCaughtSection.getInt(rarity, 0);
                        for (int i = 0; i < count; i++) {
                            data.addRarityCaught(rarity);
                        }
                    }
                }
            }
            
            // Rekordy
            double heaviestFish = config.getDouble("stats.heaviest_fish", 0);
            String heaviestFishName = config.getString("stats.heaviest_fish_name", "");
            String heaviestFishDate = config.getString("stats.heaviest_fish_date", "");
            data.setHeaviestFish(heaviestFish, heaviestFishName, heaviestFishDate);
            
            // Statystyki ekonomiczne
            data.addEarnings(config.getDouble("stats.total_earnings", 0));
        }
        
        playerData.put(uuid, data);
        plugin.getLogger().info("Wczytano dane gracza: " + player.getName());
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerData.get(uuid);
        if (data == null) return;

        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        // Podstawowe dane
        config.set("balance", getBalance(player));
        config.set("level", data.getLevel());
        config.set("xp", data.getXp());
        
        // Statystyki łowienia
        config.set("stats.total_catches", data.getTotalCatches());
        config.set("stats.failed_catches", data.getFailedCatches());
        config.set("stats.trash_caught", data.getTrashCaught());
        
        // Zapisz ilość złowionych ryb według nazwy
        ConfigurationSection fishCaughtSection = config.createSection("stats.fish_caught");
        for (Map.Entry<String, Integer> fishEntry : data.getFishCaught().entrySet()) {
            fishCaughtSection.set(fishEntry.getKey(), fishEntry.getValue());
        }
        
        // Zapisz ilość złowionych ryb według rzadkości
        ConfigurationSection rarityCaughtSection = config.createSection("stats.rarity_caught");
        for (Map.Entry<String, Integer> rarityEntry : data.getRarityCaught().entrySet()) {
            rarityCaughtSection.set(rarityEntry.getKey(), rarityEntry.getValue());
        }
        
        // Statystyki wagi
        config.set("stats.small_fish", data.getSmallFishCaught());
        config.set("stats.medium_fish", data.getMediumFishCaught());
        config.set("stats.large_fish", data.getLargeFishCaught());
        config.set("stats.huge_fish", data.getHugeFishCaught());
        
        // Rekordy
        config.set("stats.heaviest_fish", data.getHeaviestFish());
        config.set("stats.heaviest_fish_name", data.getHeaviestFishName());
        config.set("stats.heaviest_fish_date", data.getHeaviestFishDate());
        
        // Statystyki ekonomiczne
        config.set("stats.total_earnings", data.getTotalEarnings());

        try {
            config.save(playerFile);
            plugin.getLogger().info("Zapisano dane gracza: " + player.getName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie można zapisać danych gracza: " + player.getName(), e);
        }
    }

    public void saveAllPlayers() {
        plugin.getLogger().info("Zapisywanie danych wszystkich graczy...");
        for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            
            File playerFile = new File(dataFolder, uuid.toString() + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

            // Podstawowe dane
            config.set("balance", data.getBalance());
            config.set("level", data.getLevel());
            config.set("xp", data.getXp());
            
            // Statystyki łowienia
            config.set("stats.total_catches", data.getTotalCatches());
            config.set("stats.failed_catches", data.getFailedCatches());
            config.set("stats.trash_caught", data.getTrashCaught());
            
            // Zapisz ilość złowionych ryb według nazwy
            ConfigurationSection fishCaughtSection = config.createSection("stats.fish_caught");
            for (Map.Entry<String, Integer> fishEntry : data.getFishCaught().entrySet()) {
                fishCaughtSection.set(fishEntry.getKey(), fishEntry.getValue());
            }
            
            // Zapisz ilość złowionych ryb według rzadkości
            ConfigurationSection rarityCaughtSection = config.createSection("stats.rarity_caught");
            for (Map.Entry<String, Integer> rarityEntry : data.getRarityCaught().entrySet()) {
                rarityCaughtSection.set(rarityEntry.getKey(), rarityEntry.getValue());
            }
            
            // Statystyki wagi
            config.set("stats.small_fish", data.getSmallFishCaught());
            config.set("stats.medium_fish", data.getMediumFishCaught());
            config.set("stats.large_fish", data.getLargeFishCaught());
            config.set("stats.huge_fish", data.getHugeFishCaught());
            
            // Rekordy
            config.set("stats.heaviest_fish", data.getHeaviestFish());
            config.set("stats.heaviest_fish_name", data.getHeaviestFishName());
            config.set("stats.heaviest_fish_date", data.getHeaviestFishDate());
            
            // Statystyki ekonomiczne
            config.set("stats.total_earnings", data.getTotalEarnings());

            try {
                config.save(playerFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie można zapisać danych gracza: " + entry.getKey(), e);
            }
        }
        plugin.getLogger().info("Zapisano dane wszystkich graczy!");
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

    public void addXp(Player player, double amount) {
        PlayerData data = getPlayerData(player);
        data.addXp(amount);
        
        // Sprawdz, czy gracz może awansować
        double requiredXp = plugin.getConfigManager().getXpRequired(data.getLevel() + 1);
        if (data.getXp() >= requiredXp) {
            // Awansuj gracza
            data.setLevel(data.getLevel() + 1);
            data.setXp(data.getXp() - requiredXp);
            
            // Wyślij wiadomość o awansie
            MessageUtils.sendMessage(player, "level_up", Collections.singletonMap("level", String.valueOf(data.getLevel())));
            
            // Sprawdź, czy gracz może awansować ponownie
            if (data.getXp() >= plugin.getConfigManager().getXpRequired(data.getLevel() + 1)) {
                addXp(player, 0); // Rekurencyjne sprawdzenie czy można awansować dalej
            }
        }
        
        // Zapisz dane gracza
        savePlayerData(player);
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
        return playerData.getOrDefault(player.getUniqueId(), new PlayerData()).getXp();
    }

    public void setLevel(Player player, int level) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerData.get(uuid);
        
        if (data == null) {
            data = new PlayerData();
            data.setLevel(level);
            data.setXp(0);
            playerData.put(uuid, data);
        } else {
            data.setLevel(level);
            data.setXp(0); // Reset XP przy zmianie poziomu
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
        MessageUtils.sendMessage(player, "level.set", placeholders);
    }

    public PlayerData getPlayerData(Player player) {
        if (!playerData.containsKey(player.getUniqueId())) {
            playerData.put(player.getUniqueId(), new PlayerData());
        }
        return playerData.get(player.getUniqueId());
    }

    public PlayerData getOrCreatePlayerData(UUID uuid) {
        if (!playerData.containsKey(uuid)) {
            playerData.put(uuid, new PlayerData());
        }
        return playerData.get(uuid);
    }

    public double giveXp(Player player, double xp) {
        PlayerData data = getPlayerData(player);
        data.addXp(xp);
        double requiredXp = plugin.getConfigManager().getXpRequired(data.getLevel());
        
        if (data.getXp() >= requiredXp) {
            levelUp(player);
            return data.getXp() - requiredXp;
        }
        
        savePlayer(player.getUniqueId());
        return 0;
    }

    public void levelUp(Player player) {
        PlayerData data = getPlayerData(player);
        double requiredXp = plugin.getConfigManager().getXpRequired(data.getLevel());
        
        if (data.getXp() >= requiredXp) {
            double remainingXp = data.getXp() - requiredXp;
            data.setLevel(data.getLevel() + 1);
            data.setXp(remainingXp);
            
            // Komunikaty i nagrody za poziom
            
            savePlayer(player.getUniqueId());
        }
    }

    public void savePlayers() {
        File dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            
            File playerFile = new File(dataFolder, uuid.toString() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            // Podstawowe dane
            config.set("balance", data.getBalance());
            config.set("level", data.getLevel());
            config.set("xp", data.getXp());
            
            // Statystyki łowienia
            config.set("stats.total_catches", data.getTotalCatches());
            config.set("stats.failed_catches", data.getFailedCatches());
            
            // Zapisz ilość złowionych ryb według nazwy
            ConfigurationSection fishCaughtSection = config.createSection("stats.fish_caught");
            for (Map.Entry<String, Integer> fishEntry : data.getFishCaught().entrySet()) {
                fishCaughtSection.set(fishEntry.getKey(), fishEntry.getValue());
            }
            
            // Zapisz ilość złowionych ryb według rzadkości
            ConfigurationSection rarityCaughtSection = config.createSection("stats.rarity_caught");
            for (Map.Entry<String, Integer> rarityEntry : data.getRarityCaught().entrySet()) {
                rarityCaughtSection.set(rarityEntry.getKey(), rarityEntry.getValue());
            }
            
            // Statystyki wagi
            config.set("stats.small_fish", data.getSmallFishCaught());
            config.set("stats.medium_fish", data.getMediumFishCaught());
            config.set("stats.large_fish", data.getLargeFishCaught());
            config.set("stats.huge_fish", data.getHugeFishCaught());
            
            // Rekordy
            config.set("stats.heaviest_fish", data.getHeaviestFish());
            config.set("stats.heaviest_fish_name", data.getHeaviestFishName());
            config.set("stats.heaviest_fish_date", data.getHeaviestFishDate());
            
            // Statystyki ekonomiczne
            config.set("stats.total_earnings", data.getTotalEarnings());
            
            try {
                config.save(playerFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Nie można zapisać danych gracza: " + uuid, e);
            }
        }
    }
    
    public void loadPlayers() {
        File dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
            return;
        }
        
        File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null) {
            return;
        }
        
        for (File file : playerFiles) {
            String fileName = file.getName();
            if (fileName.endsWith(".yml")) {
                try {
                    String uuidString = fileName.substring(0, fileName.length() - 4);
                    UUID uuid = UUID.fromString(uuidString);
                    
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    PlayerData data = new PlayerData();
                    
                    // Podstawowe dane
                    data.setBalance(config.getDouble("balance", 0.0));
                    data.setLevel(config.getInt("level", 1));
                    data.setXp(config.getDouble("xp", 0.0));
                    
                    // Statystyki łowienia
                    if (config.contains("stats")) {
                        // Proste statystyki
                        if (config.contains("stats.total_catches")) {
                            int totalCatches = config.getInt("stats.total_catches", 0);
                            for (int i = 0; i < totalCatches; i++) {
                                data.incrementCatches();
                            }
                        }
                        
                        if (config.contains("stats.failed_catches")) {
                            int failedCatches = config.getInt("stats.failed_catches", 0);
                            for (int i = 0; i < failedCatches; i++) {
                                data.incrementFailedCatches();
                            }
                        }
                        
                        // Złowione ryby według nazwy
                        if (config.contains("stats.fish_caught")) {
                            ConfigurationSection fishCaughtSection = config.getConfigurationSection("stats.fish_caught");
                            if (fishCaughtSection != null) {
                                for (String fishName : fishCaughtSection.getKeys(false)) {
                                    int count = fishCaughtSection.getInt(fishName, 0);
                                    for (int i = 0; i < count; i++) {
                                        data.addFishCaught(fishName);
                                    }
                                }
                            }
                        }
                        
                        // Złowione ryby według rzadkości
                        if (config.contains("stats.rarity_caught")) {
                            ConfigurationSection rarityCaughtSection = config.getConfigurationSection("stats.rarity_caught");
                            if (rarityCaughtSection != null) {
                                for (String rarity : rarityCaughtSection.getKeys(false)) {
                                    int count = rarityCaughtSection.getInt(rarity, 0);
                                    for (int i = 0; i < count; i++) {
                                        data.addRarityCaught(rarity);
                                    }
                                }
                            }
                        }
                        
                        // Statystyki wagi
                        data.addFishByWeight(config.getDouble("stats.small_fish", 0));
                        data.addFishByWeight(config.getDouble("stats.medium_fish", 0));
                        data.addFishByWeight(config.getDouble("stats.large_fish", 0));
                        data.addFishByWeight(config.getDouble("stats.huge_fish", 0));
                        
                        // Rekordy
                        double heaviestFish = config.getDouble("stats.heaviest_fish", 0);
                        String heaviestFishName = config.getString("stats.heaviest_fish_name", "");
                        String heaviestFishDate = config.getString("stats.heaviest_fish_date", "");
                        data.setHeaviestFish(heaviestFish, heaviestFishName, heaviestFishDate);
                        
                        // Statystyki ekonomiczne
                        data.addEarnings(config.getDouble("stats.total_earnings", 0));
                    }
                    
                    playerData.put(uuid, data);
                    plugin.getLogger().info("Wczytano dane gracza: " + uuid);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Nieprawidłowy format UUID w pliku: " + fileName, e);
                }
            }
        }
    }
    
    public void registerStatCatch(Player player, Fish fish) {
        PlayerData data = getPlayerData(player);
        if (data == null) return;
        
        data.incrementCatches();
        data.addFishCaught(fish.getName());
        data.addRarityCaught(fish.getRarity().getName().toLowerCase());
        data.addFishByWeight(fish.getWeight());
        
        // Aktualizacja rekordu
        data.setHeaviestFish(fish.getWeight(), fish.getName(), fish.getCatchDate());
        
        savePlayer(player.getUniqueId());
    }
    
    public void registerStatFailedCatch(Player player) {
        PlayerData data = getPlayerData(player);
        if (data == null) return;
        
        data.incrementFailedCatches();
        savePlayer(player.getUniqueId());
    }
    
    public void registerFishSold(Player player, String fishName, double value) {
        PlayerData data = getPlayerData(player);
        if (data == null) return;
        
        data.addEarnings(value);
        savePlayer(player.getUniqueId());
    }
    
    public Map<String, Object> getPlayerStats(Player player) {
        PlayerData data = getPlayerData(player);
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_catches", data.getTotalCatches());
        stats.put("failed_catches", data.getFailedCatches());
        stats.put("trash_caught", data.getTrashCaught());
        stats.put("success_rate", data.getSuccessRate());
        stats.put("fish_caught", data.getFishCaught());
        stats.put("rarity_caught", data.getRarityCaught());
        stats.put("small_fish", data.getSmallFishCaught());
        stats.put("medium_fish", data.getMediumFishCaught());
        stats.put("large_fish", data.getLargeFishCaught());
        stats.put("huge_fish", data.getHugeFishCaught());
        stats.put("heaviest_fish", data.getHeaviestFish());
        stats.put("heaviest_fish_name", data.getHeaviestFishName());
        stats.put("heaviest_fish_date", data.getHeaviestFishDate());
        stats.put("total_earnings", data.getTotalEarnings());
        
        return stats;
    }

    /**
     * Zapisuje dane gracza do pliku
     * @param uuid UUID gracza do zapisania
     */
    public void savePlayer(UUID uuid) {
        PlayerData data = playerData.get(uuid);
        if (data == null) return;

        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Podstawowe dane
        config.set("balance", data.getBalance());
        config.set("level", data.getLevel());
        config.set("xp", data.getXp());
        
        // Zapisz statystyki (opcjonalnie)
        // Możesz skopiować tutaj fragment z metody savePlayers(), który zapisuje statystyki
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie można zapisać danych gracza: " + uuid, e);
        }
    }

    // Metody do obsługi statystyk gracza
    public void incrementTotalCatches(Player player) {
        PlayerData data = getPlayerData(player);
        data.incrementCatches();
        savePlayerData(player);
    }

    public void incrementFailedCatches(Player player) {
        PlayerData data = getPlayerData(player);
        data.incrementFailedCatches();
        savePlayerData(player);
    }

    public void addFishCaught(Player player, String fishName) {
        PlayerData data = getPlayerData(player);
        data.addFishCaught(fishName);
        savePlayerData(player);
    }

    public void addRarityCaught(Player player, String rarity) {
        PlayerData data = getPlayerData(player);
        data.addRarityCaught(rarity);
        savePlayerData(player);
    }

    public void incrementSmallFishCount(Player player) {
        PlayerData data = getPlayerData(player);
        data.smallFishCaught++;
        savePlayerData(player);
    }

    public void incrementMediumFishCount(Player player) {
        PlayerData data = getPlayerData(player);
        data.mediumFishCaught++;
        savePlayerData(player);
    }

    public void incrementLargeFishCount(Player player) {
        PlayerData data = getPlayerData(player);
        data.largeFishCaught++;
        savePlayerData(player);
    }

    public void incrementHugeFishCount(Player player) {
        PlayerData data = getPlayerData(player);
        data.hugeFishCaught++;
        savePlayerData(player);
    }

    public void checkHeaviestFish(Player player, Fish fish) {
        PlayerData data = getPlayerData(player);
        double weight = fish.getWeight();
        
        if (weight > data.getHeaviestFish()) {
            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());
            data.setHeaviestFish(weight, fish.getName(), date);
            savePlayerData(player);
        }
    }

    /**
     * Dodaje zarobek do statystyk gracza
     * @param player Gracz, którego statystyki aktualizujemy
     * @param earnings Kwota zarobku
     */
    public void addEarnings(Player player, double earnings) {
        PlayerData data = getPlayerData(player);
        data.addEarnings(earnings);
        savePlayer(player.getUniqueId()); // Zapisz dane po aktualizacji
    }

    /**
     * Rejestruje złowienie śmieci w statystykach gracza
     */
    public void registerStatTrashCaught(Player player) {
        PlayerData data = getPlayerData(player);
        data.incrementTrashCaught();
        savePlayerData(player);
    }

    /**
     * Zwraca kopię wszystkich danych graczy
     * @return Mapa zawierająca dane wszystkich graczy
     */
    public Map<UUID, PlayerData> getAllPlayerData() {
        // Zwróć kopię mapy, aby uniknąć modyfikacji oryginalnych danych
        return new HashMap<>(playerData);
    }
    
    /**
     * Przywraca dane graczy z podanej mapy
     * @param data Mapa zawierająca dane graczy do przywrócenia
     */
    public void restorePlayerData(Map<UUID, PlayerData> data) {
        if (data == null || data.isEmpty()) {
            plugin.getLogger().warning("Próba przywrócenia pustych danych graczy!");
            return;
        }
        
        // Przywróć dane graczy - usuń najpierw starą mapę i zastąp ją nową
        playerData.clear();
        playerData.putAll(data);
        plugin.getLogger().info("Przywrócono dane dla " + data.size() + " graczy.");
    }

    public static class PlayerData {
        private double balance;
        private int level;
        private double xp;
        
        // Statystyki łowienia
        private int totalCatches;
        private int failedCatches;
        private Map<String, Integer> fishCaught;
        private Map<String, Integer> rarityCaught;
        private int trashCaught;
        
        // Statystyki wagi
        private int smallFishCaught;
        private int mediumFishCaught;
        private int largeFishCaught;
        private int hugeFishCaught;
        
        // Rekord
        private double heaviestFish;
        private String heaviestFishName;
        private String heaviestFishDate;
        
        // Statystyki ekonomiczne
        private double totalEarnings;
        
        public PlayerData() {
            this.balance = 0;
            this.level = 1;
            this.xp = 0;
            this.totalCatches = 0;
            this.failedCatches = 0;
            this.fishCaught = new HashMap<>();
            this.rarityCaught = new HashMap<>();
            this.trashCaught = 0;
            this.smallFishCaught = 0;
            this.mediumFishCaught = 0;
            this.largeFishCaught = 0;
            this.hugeFishCaught = 0;
            this.heaviestFish = 0;
            this.heaviestFishName = "";
            this.heaviestFishDate = "";
            this.totalEarnings = 0;
        }
        
        public double getBalance() {
            return balance;
        }
        
        public void setBalance(double balance) {
            this.balance = balance;
        }
        
        public void addBalance(double amount) {
            this.balance += amount;
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
        
        // Metody statystyk łowienia
        public int getTotalCatches() {
            return totalCatches;
        }
        
        public void incrementCatches() {
            this.totalCatches++;
        }
        
        public int getFailedCatches() {
            return failedCatches;
        }
        
        public void incrementFailedCatches() {
            this.failedCatches++;
        }
        
        public double getSuccessRate() {
            if (totalCatches + failedCatches == 0) return 0.0;
            return (double) totalCatches / (totalCatches + failedCatches) * 100.0;
        }
        
        public Map<String, Integer> getFishCaught() {
            return Collections.unmodifiableMap(fishCaught);
        }
        
        public void addFishCaught(String fishName) {
            fishCaught.put(fishName, fishCaught.getOrDefault(fishName, 0) + 1);
        }
        
        public Map<String, Integer> getRarityCaught() {
            return Collections.unmodifiableMap(rarityCaught);
        }
        
        public void addRarityCaught(String rarity) {
            rarityCaught.put(rarity, rarityCaught.getOrDefault(rarity, 0) + 1);
        }
        
        // Metody statystyk wagi
        public int getSmallFishCaught() {
            return smallFishCaught;
        }
        
        public int getMediumFishCaught() {
            return mediumFishCaught;
        }
        
        public int getLargeFishCaught() {
            return largeFishCaught;
        }
        
        public int getHugeFishCaught() {
            return hugeFishCaught;
        }
        
        public void addFishByWeight(double weight) {
            if (weight < 1.0) {
                smallFishCaught++;
            } else if (weight < 3.0) {
                mediumFishCaught++;
            } else if (weight < 7.0) {
                largeFishCaught++;
            } else {
                hugeFishCaught++;
            }
            
            // Aktualizacja rekordu jeśli to najcięższa ryba
            if (weight > heaviestFish) {
                setHeaviestFish(weight, heaviestFishName, new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()));
            }
        }
        
        // Metody rekordów
        public double getHeaviestFish() {
            return heaviestFish;
        }
        
        public String getHeaviestFishName() {
            return heaviestFishName;
        }
        
        public String getHeaviestFishDate() {
            return heaviestFishDate;
        }
        
        public void setHeaviestFish(double weight, String fishName, String date) {
            if (weight > heaviestFish) {
                this.heaviestFish = weight;
                this.heaviestFishName = fishName;
                this.heaviestFishDate = date;
            }
        }
        
        // Metody statystyk ekonomicznych
        public double getTotalEarnings() {
            return totalEarnings;
        }
        
        public void addEarnings(double amount) {
            this.totalEarnings += amount;
        }
        
        // Resetowanie statystyk
        public void resetStats() {
            totalCatches = 0;
            failedCatches = 0;
            fishCaught.clear();
            rarityCaught.clear();
            smallFishCaught = 0;
            mediumFishCaught = 0;
            largeFishCaught = 0;
            hugeFishCaught = 0;
            heaviestFish = 0.0;
            heaviestFishName = "";
            heaviestFishDate = "";
            totalEarnings = 0.0;
        }

        public int getTrashCaught() {
            return trashCaught;
        }
        
        public void incrementTrashCaught() {
            this.trashCaught++;
        }
    }
} 
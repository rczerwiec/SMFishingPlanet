package pl.stylowamc.smfishingplanet.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {
    private final SMFishingPlanet plugin;
    private FileConfiguration config;
    private FileConfiguration fishConfig;
    private File configFile;
    private File fishConfigFile;

    public ConfigManager(SMFishingPlanet plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        // Załaduj główny config
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        fishConfigFile = new File(plugin.getDataFolder(), "fish.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
            plugin.getLogger().info("Utworzono plik config.yml");
        }

        if (!fishConfigFile.exists()) {
            plugin.saveResource("fish.yml", false);
            plugin.getLogger().info("Utworzono plik fish.yml");
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        fishConfig = YamlConfiguration.loadConfiguration(fishConfigFile);

        // Dodaj debug info
        boolean debugEnabled = config.getBoolean("debug", false);
        plugin.getLogger().info("Debug mode: " + debugEnabled);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie można zapisać pliku config.yml", e);
        }
    }

    public void saveFishConfig() {
        try {
            fishConfig.save(fishConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie można zapisać pliku fish.yml", e);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getFishConfig() {
        return fishConfig;
    }

    public void reloadConfigs() {
        loadConfigs();
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        fishConfig = YamlConfiguration.loadConfiguration(fishConfigFile);
        
        // Dodaj debug info przy reloadzie
        boolean debugEnabled = config.getBoolean("debug", false);
        plugin.getLogger().info("==========================================");
        plugin.getLogger().info("Config reloaded!");
        plugin.getLogger().info("Debug mode: " + debugEnabled);
        plugin.getLogger().info("Debug value in config: " + config.get("debug"));
        plugin.getLogger().info("==========================================");
    }

    public boolean isDebugEnabled() {
        boolean debugEnabled = config.getBoolean("debug", false);
        plugin.getLogger().info("Checking debug status: " + debugEnabled);
        return debugEnabled;
    }

    // Metody pomocnicze do pobierania wartości z konfiguracji
    public int getFishingDuration() {
        return config.getInt("fishing.duration", 10);
    }

    public int getMinStopTime() {
        return config.getInt("fishing.min_stop_time", 3);
    }

    public int getMaxStopTime() {
        return config.getInt("fishing.max_stop_time", 8);
    }

    public double getErrorMargin() {
        return config.getDouble("fishing.error_margin", 0.5);
    }

    public String getCatchSound() {
        return config.getString("fishing.sound.catch", "ENTITY_FISHING_BOBBER_SPLASH");
    }

    public String getFailSound() {
        return config.getString("fishing.sound.fail", "ENTITY_VILLAGER_NO");
    }

    public int getGuiSize() {
        return config.getInt("gui.sell_menu.size", 54);
    }

    public int getSellAllSlot() {
        return config.getInt("gui.sell_menu.sell_all_slot", 49);
    }

    public String getSellAllItem() {
        return config.getString("gui.sell_menu.sell_all_item", "EMERALD");
    }

    public String getEmptySlotItem() {
        return config.getString("gui.sell_menu.empty_slot_item", "GRAY_STAINED_GLASS_PANE");
    }

    public double getStartingBalance() {
        return config.getDouble("economy.starting_balance", 0.0);
    }

    public double getMinSellValue() {
        return config.getDouble("economy.min_sell_value", 1.0);
    }

    public double getMaxSellValue() {
        return config.getDouble("economy.max_sell_value", 1000000.0);
    }

    public int getMaxLevel() {
        return config.getInt("levels.max_level", 100);
    }

    public double getXpMultiplier() {
        return config.getDouble("levels.xp_multiplier", 1.0);
    }

    public int getXpForRarity(String rarity) {
        String path = "levels.xp_per_fish." + rarity.toLowerCase();
        plugin.getLogger().info("==== DEBUG XP CONFIG ====");
        plugin.getLogger().info("Próbuję pobrać XP dla rzadkości: " + rarity);
        plugin.getLogger().info("Ścieżka w config: " + path);
        
        int xp = config.getInt(path, -1);
        plugin.getLogger().info("Wartość znaleziona: " + xp);
        
        // Sprawdź alternatywne ścieżki, jeśli nie znaleziono wartości
        if (xp == -1) {
            plugin.getLogger().info("Nie znaleziono wartości pod ścieżką " + path + ", szukam alternatyw...");
            
            // Mapowanie polskich nazw na angielskie
            String alternativePath = path;
            if (rarity.equalsIgnoreCase("pospolita")) {
                alternativePath = "levels.xp_per_fish.common";
            } else if (rarity.equalsIgnoreCase("niepospolita")) {
                alternativePath = "levels.xp_per_fish.uncommon";
            } else if (rarity.equalsIgnoreCase("rzadka")) {
                alternativePath = "levels.xp_per_fish.rare";
            } else if (rarity.equalsIgnoreCase("epicka")) {
                alternativePath = "levels.xp_per_fish.epic";
            } else if (rarity.equalsIgnoreCase("legendarna")) {
                alternativePath = "levels.xp_per_fish.legendary";
            }
            
            plugin.getLogger().info("Próbuję alternatywną ścieżkę: " + alternativePath);
            xp = config.getInt(alternativePath, 1);
            plugin.getLogger().info("Wartość alternatywna: " + xp);
        }
        
        plugin.getLogger().info("Ostateczna wartość XP: " + xp);
        return xp;
    }

    /**
     * Zwraca ilość XP wymagane do osiągnięcia następnego poziomu
     * @param level Aktualny poziom gracza
     * @return Wymagane XP do następnego poziomu
     */
    public double getXpRequired(int level) {
        String path = "levels.required_xp." + level;
        if (config.contains(path)) {
            double requiredXp = config.getDouble(path);
            plugin.getLogger().info("Pobieranie wymaganego XP dla poziomu " + level + ": " + requiredXp);
            return requiredXp;
        } else {
            // Wzór na wymagane XP gdy nie zdefiniowano konkretnej wartości
            double requiredXp = 100 * Math.pow(1.5, level - 1);
            plugin.getLogger().info("Obliczanie wymaganego XP dla poziomu " + level + ": " + requiredXp);
            return requiredXp;
        }
    }
} 
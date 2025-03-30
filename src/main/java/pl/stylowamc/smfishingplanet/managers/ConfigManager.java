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
    }

    public void loadConfigs() {
        // Główna konfiguracja
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Konfiguracja ryb
        fishConfigFile = new File(plugin.getDataFolder(), "fish.yml");
        if (!fishConfigFile.exists()) {
            plugin.saveResource("fish.yml", false);
        }
        fishConfig = YamlConfiguration.loadConfiguration(fishConfigFile);
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
        return config.getInt("levels.xp_per_fish." + rarity.toLowerCase(), 1);
    }
} 
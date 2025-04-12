package pl.stylowamc.smfishingplanet;

import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pl.stylowamc.smfishingplanet.commands.PoziomCommand;
import pl.stylowamc.smfishingplanet.commands.SprzedajRybyCommand;
import pl.stylowamc.smfishingplanet.commands.WedkaCommand;
import pl.stylowamc.smfishingplanet.commands.ZylkaCommand;
import pl.stylowamc.smfishingplanet.commands.SetLevelCommand;
import pl.stylowamc.smfishingplanet.commands.RybyCommand;
import pl.stylowamc.smfishingplanet.commands.SMFishingPlanetCommand;
import pl.stylowamc.smfishingplanet.commands.StatystykiCommand;
import pl.stylowamc.smfishingplanet.crafting.FishingRodRecipe;
import pl.stylowamc.smfishingplanet.items.FishingRod;
import pl.stylowamc.smfishingplanet.listeners.FishingListener;
import pl.stylowamc.smfishingplanet.listeners.GuiListener;
import pl.stylowamc.smfishingplanet.listeners.PlayerJoinListener;
import pl.stylowamc.smfishingplanet.listeners.VanillaFishingListener;
import pl.stylowamc.smfishingplanet.listeners.CraftingListener;
import pl.stylowamc.smfishingplanet.managers.ConfigManager;
import pl.stylowamc.smfishingplanet.managers.FishManager;
import pl.stylowamc.smfishingplanet.managers.PlayerDataManager;
import pl.stylowamc.smfishingplanet.menus.SellMenu;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;
import pl.stylowamc.smfishingplanet.utils.FishingPlaceholders;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.command.CommandExecutor;
import java.util.Map;
import java.util.UUID;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;

public class SMFishingPlanet extends JavaPlugin {
    private static SMFishingPlanet instance;
    private Economy economy;
    private ConfigManager configManager;
    private FishManager fishManager;
    private PlayerDataManager playerDataManager;
    private FishingRod fishingRod;
    private SellMenu sellMenu;
    private WorldGuard worldGuard;

    @Override
    public void onEnable() {
        instance = this;

        // Inicjalizacja konfiguracji
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // Inicjalizacja wiadomości
        MessageUtils.loadMessages();

        // Inicjalizacja ekonomii
        if (!setupEconomy()) {
            getLogger().severe("Nie znaleziono pluginu Vault! Wyłączanie...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicjalizacja managerów
        fishManager = new FishManager(this);
        playerDataManager = new PlayerDataManager(this);
        fishingRod = new FishingRod(this);
        sellMenu = new SellMenu(this);
        
        // Inicjalizacja śmieci i żyłek
        pl.stylowamc.smfishingplanet.models.Trash.init(this);
        pl.stylowamc.smfishingplanet.models.FishingLine.init(this);

        // Rejestracja listenerów
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new VanillaFishingListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);

        // Rejestracja komend
        registerCommands();

        // Rejestruj crafting
        FishingRodRecipe rodRecipe = new FishingRodRecipe(this);
        
        // Podstawowa wędka
        ShapedRecipe basicRodRecipe = rodRecipe.createBasicRodRecipe();
        if (basicRodRecipe != null) {
            Bukkit.removeRecipe(basicRodRecipe.getKey());
            getServer().addRecipe(basicRodRecipe);
        }
        
        // Zaawansowana wędka
        ShapedRecipe advancedRodRecipe = rodRecipe.createAdvancedRodRecipe();
        if (advancedRodRecipe != null) {
            Bukkit.removeRecipe(advancedRodRecipe.getKey());
            getServer().addRecipe(advancedRodRecipe);
        }
        
        // Profesjonalna wędka
        ShapedRecipe proRodRecipe = rodRecipe.createProfessionalRodRecipe();
        if (proRodRecipe != null) {
            Bukkit.removeRecipe(proRodRecipe.getKey());
            getServer().addRecipe(proRodRecipe);
        }
        
        // Wędka mistrza
        ShapedRecipe masterRodRecipe = rodRecipe.createMasterRodRecipe();
        if (masterRodRecipe != null) {
            Bukkit.removeRecipe(masterRodRecipe.getKey());
            getServer().addRecipe(masterRodRecipe);
        }

        // Inicjalizacja bStats
        int pluginId = 20947; // ID twojego pluginu na bStats
        new Metrics(this, pluginId);

        // Inicjalizacja WorldGuard
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuard = WorldGuard.getInstance();
            getLogger().info("WorldGuard został pomyślnie zainicjalizowany!");
        } else {
            getLogger().warning("WorldGuard nie został znaleziony! Niektóre funkcje mogą nie działać.");
        }

        // Rejestracja placeholderów
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FishingPlaceholders(this).register();
            getLogger().info("PlaceholderAPI został pomyślnie zainicjalizowany!");
        } else {
            getLogger().warning("PlaceholderAPI nie został znaleziony! Placeholdery nie będą działać.");
        }

        getLogger().info("Plugin został pomyślnie włączony!");
    }

    @Override
    public void onDisable() {
        // Zapisz dane graczy
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayers();
        }

        getLogger().info("Plugin został wyłączony!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static SMFishingPlanet getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public FishManager getFishManager() {
        return fishManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public FishingRod getFishingRod() {
        return fishingRod;
    }

    public SellMenu getSellMenu() {
        return sellMenu;
    }

    public FileConfiguration getFishConfig() {
        return configManager.getFishConfig();
    }

    public void reloadPlugin() {
        // Zapisz dane graczy przed przeładowaniem
        if (playerDataManager != null) {
            getLogger().info("Zapisywanie danych graczy przed przeładowaniem pluginu...");
            playerDataManager.saveAllPlayers();
        }
        
        // Przechowaj tymczasowo dane graczy
        Map<UUID, PlayerDataManager.PlayerData> tempPlayerData = null;
        if (playerDataManager != null) {
            tempPlayerData = playerDataManager.getAllPlayerData();
            getLogger().info("Przechowano tymczasowo dane " + tempPlayerData.size() + " graczy");
            
            // Wyświetl poziomy graczy dla debugowania
            for (Map.Entry<UUID, PlayerDataManager.PlayerData> entry : tempPlayerData.entrySet()) {
                getLogger().info("Gracz " + entry.getKey() + ": poziom = " + entry.getValue().getLevel());
            }
        }
        
        // Przeładuj konfiguracje
        getLogger().info("Przeładowywanie konfiguracji...");
        reloadConfig();
        configManager.loadConfigs();
        MessageUtils.reloadMessages();
        fishManager.reloadFish();
        
        // Przywróć dane graczy po przeładowaniu, jeśli były wcześniej zapisane
        if (tempPlayerData != null && playerDataManager != null) {
            getLogger().info("Przywracanie danych graczy po przeładowaniu...");
            playerDataManager.restorePlayerData(tempPlayerData);
            
            // Wyświetl poziomy graczy dla debugowania po przywróceniu
            for (Map.Entry<UUID, PlayerDataManager.PlayerData> entry : playerDataManager.getAllPlayerData().entrySet()) {
                getLogger().info("Gracz " + entry.getKey() + " po przeładowaniu: poziom = " + entry.getValue().getLevel());
            }
            
            getLogger().info("Przywrócono dane graczy po przeładowaniu pluginu.");
        }
    }

    private void registerCommands() {
        // Rejestracja komend z zabezpieczeniem przed nullami
        safeRegisterCommand("ryby", new RybyCommand(this));
        safeRegisterCommand("wedka", new WedkaCommand(this));
        safeRegisterCommand("sprzedajryby", new SprzedajRybyCommand(this));
        safeRegisterCommand("smfishingplanet", new SMFishingPlanetCommand(this));
        safeRegisterCommand("poziom", new PoziomCommand(this));
        safeRegisterCommand("zylka", new ZylkaCommand(this));
        safeRegisterCommand("setlevel", new SetLevelCommand(this));
        safeRegisterCommand("statystyki", new StatystykiCommand(this));
    }
    
    private void safeRegisterCommand(String name, CommandExecutor executor) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(executor);
        } else {
            getLogger().warning("Nie można zarejestrować komendy: " + name + " - brak definicji w plugin.yml");
        }
    }

    public WorldGuard getWorldGuard() {
        return worldGuard;
    }
}

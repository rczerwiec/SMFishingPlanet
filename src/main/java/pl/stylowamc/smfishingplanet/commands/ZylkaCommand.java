package pl.stylowamc.smfishingplanet.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ZylkaCommand implements CommandExecutor, TabCompleter {
    private final SMFishingPlanet plugin;
    
    public ZylkaCommand(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.colorize("&cTa komenda jest dostępna tylko dla graczy!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("smfishing.zylka")) {
            MessageUtils.sendMessage(player, "no_permission");
            return true;
        }
        
        if (args.length != 1) {
            player.sendMessage(MessageUtils.colorize("&cUżycie: /zylka <typ>"));
            List<String> availableTypes = getAvailableFishingLineTypes();
            player.sendMessage(MessageUtils.colorize("&cDostępne typy: " + String.join(", ", availableTypes)));
            return true;
        }
        
        String requestedType = args[0].toLowerCase();
        ItemStack zylka = null;
        
        // Pobierz sekcję z konfiguracją żyłek
        ConfigurationSection linesSection = plugin.getConfig().getConfigurationSection("fishing_lines");
        
        if (linesSection != null) {
            // Sprawdź czy żądany typ żyłki istnieje w konfiguracji
            boolean found = false;
            
            for (String key : linesSection.getKeys(false)) {
                String lineName = linesSection.getString(key + ".name", "").toLowerCase();
                
                if (key.equalsIgnoreCase(requestedType) || 
                    (lineName.contains("żyłka") && lineName.contains(requestedType))) {
                    // Znaleziono żyłkę w konfiguracji
                    found = true;
                    
                    // Użyj modelu FishingLine z pakietu models
                    pl.stylowamc.smfishingplanet.models.FishingLine fishingLine = 
                        pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType(key);
                    zylka = fishingLine.createItemStack();
                    break;
                }
            }
            
            if (!found) {
                player.sendMessage(MessageUtils.colorize("&cNieprawidłowy typ żyłki!"));
                List<String> availableTypes = getAvailableFishingLineTypes();
                player.sendMessage(MessageUtils.colorize("&cDostępne typy: " + String.join(", ", availableTypes)));
                return true;
            }
        } else {
            // Jeśli brak konfiguracji, używaj domyślnych typów
            pl.stylowamc.smfishingplanet.models.FishingLine fishingLine;
            
            switch (requestedType) {
                case "4mm":
                case "basic":
                    fishingLine = pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType("basic");
                    break;
                case "5mm":
                case "advanced":
                    fishingLine = pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType("advanced");
                    break;
                case "6mm":
                case "professional":
                    fishingLine = pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType("professional");
                    break;
                default:
                    player.sendMessage(MessageUtils.colorize("&cNieprawidłowy typ żyłki! Dostępne typy: basic (4mm), advanced (5mm), professional (6mm)"));
                    return true;
            }
            zylka = fishingLine.createItemStack();
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            MessageUtils.sendMessage(player, "fishing.inventory_full");
            return true;
        }
        
        player.getInventory().addItem(zylka);
        player.sendMessage(MessageUtils.colorize("&aOtrzymałeś żyłkę " + args[0]));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = getAvailableFishingLineTypes();
            return suggestions.stream()
                    .filter(type -> type.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    /**
     * Pobiera listę dostępnych typów żyłek z konfiguracji
     * @return Lista typów żyłek (nazwy sekcji z config.yml)
     */
    private List<String> getAvailableFishingLineTypes() {
        ConfigurationSection linesSection = plugin.getConfig().getConfigurationSection("fishing_lines");
        
        if (linesSection != null) {
            return new ArrayList<>(linesSection.getKeys(false));
        } else {
            // Jeśli brak konfiguracji, użyj wartości domyślnych
            return Arrays.asList("basic", "advanced", "professional");
        }
    }
} 
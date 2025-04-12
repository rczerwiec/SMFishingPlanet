package pl.stylowamc.smfishingplanet.items;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class FishingLine {
    private final SMFishingPlanet plugin;
    
    public FishingLine(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createFishingLine() {
        return getFishingLineFromConfig("basic");
    }
    
    public ItemStack createFishingLine4mm() {
        return getFishingLineFromConfig("advanced");
    }
    
    public ItemStack createFishingLine5mm() {
        return getFishingLineFromConfig("professional");
    }
    
    public ItemStack createFishingLine6mm() {
        return getFishingLineFromConfig("professional");
    }
    
    /**
     * Tworzy przedmiot żyłki na podstawie konfiguracji
     * @param rodType Typ wędki odpowiadający sekcji w konfiguracji (basic, advanced, professional)
     * @return ItemStack żyłki
     */
    private ItemStack getFishingLineFromConfig(String rodType) {
        ConfigurationSection linesSection = plugin.getConfig().getConfigurationSection("fishing_lines");
        if (linesSection == null) {
            plugin.getLogger().warning("Brak sekcji fishing_lines w config.yml. Używam domyślnych wartości.");
            return createDefaultFishingLine(rodType);
        }
        
        ConfigurationSection lineSection = linesSection.getConfigurationSection(rodType);
        if (lineSection == null) {
            plugin.getLogger().warning("Brak konfiguracji dla żyłki typu " + rodType + ". Używam domyślnych wartości.");
            return createDefaultFishingLine(rodType);
        }
        
        String name = lineSection.getString("name", "Żyłka");
        Material material;
        try {
            material = Material.valueOf(lineSection.getString("material", "STRING"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Nieprawidłowy materiał dla żyłki " + rodType + ". Używam STRING.");
            material = Material.STRING;
        }
        
        List<String> configLore = lineSection.getStringList("lore");
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.setDisplayName(MessageUtils.colorize("&f&l" + name));
        
        List<String> lore = new ArrayList<>();
        if (configLore.isEmpty()) {
            lore.add(MessageUtils.colorize("&7Używana do craftowania"));
            lore.add(MessageUtils.colorize("&7wędki dla poziomu wyższego"));
        } else {
            for (String line : configLore) {
                lore.add(MessageUtils.colorize(line));
            }
        }
        
        // Dodaj informację o możliwości użycia w craftingu
        lore.add("");
        lore.add(MessageUtils.colorize("&a✓ &7Można używać w craftingu wędek!"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Tworzy domyślną żyłkę w przypadku braku konfiguracji
     * @param rodType Typ wędki
     * @return ItemStack domyślnej żyłki
     */
    private ItemStack createDefaultFishingLine(String rodType) {
        ItemStack item = new ItemStack(Material.STRING);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        String size;
        String rodName;
        
        switch (rodType) {
            case "basic":
                size = "2mm";
                rodName = "Podstawowej";
                break;
            case "advanced":
                size = "4mm";
                rodName = "Zaawansowanej";
                break;
            case "professional":
                size = "5mm";
                rodName = "Profesjonalnej";
                break;
            default:
                size = "2mm";
                rodName = "Podstawowej";
                break;
        }
        
        meta.setDisplayName(MessageUtils.colorize("&f&lŻyłka " + size));
        
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Używana do craftowania"));
        lore.add(MessageUtils.colorize("&7Wędki " + rodName));
        
        // Dodaj informację o możliwości użycia w craftingu
        lore.add("");
        lore.add(MessageUtils.colorize("&a✓ &7Można używać w craftingu wędek!"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
} 
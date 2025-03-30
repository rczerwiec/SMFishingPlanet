package pl.stylowamc.smfishingplanet.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FishingLine {
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final double dropChance;
    private final String rodType;
    
    private static final Random random = new Random();
    private static SMFishingPlanet plugin;
    private static Map<String, FishingLine> fishingLinesByRodType = new HashMap<>();
    
    public FishingLine(String name, Material material, List<String> lore, double dropChance, String rodType) {
        this.name = name;
        this.material = material;
        this.lore = lore;
        this.dropChance = dropChance;
        this.rodType = rodType;
    }
    
    public static void init(SMFishingPlanet pluginInstance) {
        plugin = pluginInstance;
        loadFishingLinesFromConfig();
    }
    
    private static void loadFishingLinesFromConfig() {
        fishingLinesByRodType.clear();
        ConfigurationSection linesSection = plugin.getConfig().getConfigurationSection("fishing_lines");
        
        if (linesSection == null) {
            plugin.getLogger().warning("Nie znaleziono sekcji żyłek w konfiguracji. Używam domyślnych wartości.");
            createDefaultFishingLines();
            return;
        }
        
        for (String rodType : linesSection.getKeys(false)) {
            ConfigurationSection lineSection = linesSection.getConfigurationSection(rodType);
            
            if (lineSection != null) {
                String name = lineSection.getString("name", "Żyłka");
                Material material;
                try {
                    material = Material.valueOf(lineSection.getString("material", "STRING"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Nieprawidłowy materiał dla żyłki " + rodType + ". Używam STRING.");
                    material = Material.STRING;
                }
                
                List<String> lore = lineSection.getStringList("lore");
                double dropChance = lineSection.getDouble("drop_chance", 5.0);
                
                fishingLinesByRodType.put(rodType, new FishingLine(name, material, lore, dropChance, rodType));
            }
        }
        
        if (fishingLinesByRodType.isEmpty()) {
            plugin.getLogger().warning("Nie znaleziono żadnych skonfigurowanych żyłek. Używam domyślnych wartości.");
            createDefaultFishingLines();
        }
    }
    
    private static void createDefaultFishingLines() {
        fishingLinesByRodType.put("basic", new FishingLine("Żyłka 4mm", Material.STRING, 
                                  List.of("&7Podstawowa żyłka wędkarska.", 
                                         "&7Używana do &aWędki Podstawowej&7."), 5.0, "basic"));
        
        fishingLinesByRodType.put("advanced", new FishingLine("Żyłka 5mm", Material.STRING, 
                                   List.of("&7Wzmocniona żyłka wędkarska.", 
                                          "&7Używana do &bWędki Zaawansowanej&7."), 3.0, "advanced"));
        
        fishingLinesByRodType.put("professional", new FishingLine("Żyłka 6mm", Material.STRING, 
                                   List.of("&7Profesjonalna żyłka wędkarska.", 
                                          "&7Używana do &dWędki Profesjonalnej&7."), 2.0, "professional"));
    }
    
    public String getName() {
        return name;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public double getDropChance() {
        return dropChance;
    }
    
    public String getRodType() {
        return rodType;
    }
    
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.colorize("&b" + name));
            
            List<String> itemLore = new ArrayList<>();
            for (String line : lore) {
                itemLore.add(MessageUtils.colorize(line));
            }
            
            itemLore.add("");
            itemLore.add(MessageUtils.colorize("&a✓ &7Można używać w craftingu wędek!"));
            
            meta.setLore(itemLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static FishingLine getLineForRodType(String rodType) {
        if (fishingLinesByRodType.isEmpty()) {
            if (plugin != null) {
                loadFishingLinesFromConfig();
            } else {
                createDefaultFishingLines();
            }
        }
        
        return fishingLinesByRodType.getOrDefault(rodType, fishingLinesByRodType.getOrDefault("basic", 
               new FishingLine("Żyłka 4mm", Material.STRING, List.of("&7Podstawowa żyłka wędkarska."), 5.0, "basic")));
    }
    
    public static boolean shouldDropLine(String rodType) {
        FishingLine line = getLineForRodType(rodType);
        return random.nextDouble() * 100 <= line.getDropChance();
    }
} 
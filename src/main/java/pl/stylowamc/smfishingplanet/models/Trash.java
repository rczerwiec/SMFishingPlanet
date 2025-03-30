package pl.stylowamc.smfishingplanet.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Trash {
    private String name;
    private Material material;
    private List<String> lore;
    private double value;
    private static final Random random = new Random();
    private static SMFishingPlanet plugin;
    private static List<Trash> trashItems;
    
    public Trash(String name, Material material, List<String> lore, double value) {
        this.name = name;
        this.material = material;
        this.lore = lore;
        this.value = value;
    }
    
    public static void init(SMFishingPlanet pluginInstance) {
        plugin = pluginInstance;
        loadTrashFromConfig();
    }
    
    private static void loadTrashFromConfig() {
        trashItems = new ArrayList<>();
        ConfigurationSection trashSection = plugin.getConfig().getConfigurationSection("trash.items");
        
        if (trashSection == null) {
            plugin.getLogger().warning("Sekcja śmieci nie została znaleziona w konfiguracji. Używam domyślnych wartości.");
            return;
        }
        
        double defaultValue = plugin.getConfig().getDouble("trash.default_value", 0.1);
        
        for (String key : trashSection.getKeys(false)) {
            ConfigurationSection itemSection = trashSection.getConfigurationSection(key);
            
            if (itemSection != null) {
                String name = itemSection.getString("name", key);
                Material material;
                try {
                    material = Material.valueOf(itemSection.getString("material", "PAPER"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Nieprawidłowy materiał dla śmiecia " + key + ". Używam PAPER.");
                    material = Material.PAPER;
                }
                
                List<String> lore = itemSection.getStringList("lore");
                double value = itemSection.getDouble("value", defaultValue);
                
                trashItems.add(new Trash(name, material, lore, value));
            }
        }
        
        if (trashItems.isEmpty()) {
            plugin.getLogger().warning("Nie znaleziono żadnych skonfigurowanych śmieci. Używam domyślnych wartości.");
        }
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
    
    public double getValue() {
        return value;
    }
    
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtils.colorize("&7" + name));
            
            List<String> itemLore = new ArrayList<>();
            itemLore.add(MessageUtils.colorize("&8Śmieć"));
            for (String line : lore) {
                itemLore.add(MessageUtils.colorize(line));
            }
            itemLore.add("");
            itemLore.add(MessageUtils.colorize("&7Wartość: &e" + value + "$"));
            
            meta.setLore(itemLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static Trash getRandomTrash() {
        if (trashItems == null || trashItems.isEmpty()) {
            if (plugin != null) {
                loadTrashFromConfig();
            } else {
                // Wygeneruj domyślne śmieci, jeśli plugin nie jest jeszcze zainicjalizowany
                return createDefaultTrash();
            }
        }
        
        if (trashItems.isEmpty()) {
            return createDefaultTrash();
        }
        
        return trashItems.get(random.nextInt(trashItems.size()));
    }
    
    private static Trash createDefaultTrash() {
        return new Trash("Mokry papier", Material.PAPER, 
                      List.of("&7Rozpadający się w rękach."), 0.1);
    }
    
    public static double getTrashChance() {
        return plugin != null ? plugin.getConfig().getDouble("trash.chance", 20.0) : 20.0;
    }
} 
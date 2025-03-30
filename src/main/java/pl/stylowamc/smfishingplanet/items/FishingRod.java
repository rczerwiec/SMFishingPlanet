package pl.stylowamc.smfishingplanet.items;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class FishingRod {
    private final SMFishingPlanet plugin;
    private final NamespacedKey rodTypeKey;
    private final NamespacedKey durabilityKey;
    
    public FishingRod(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.rodTypeKey = new NamespacedKey(plugin, "rod_type");
        this.durabilityKey = new NamespacedKey(plugin, "durability");
    }
    
    public ItemStack createFishingRod(String type) {
        ConfigurationSection config = plugin.getConfigManager().getConfig().getConfigurationSection("fishing_rod." + type);
        if (config == null) return null;

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        if (meta == null) return rod;

        // Ustaw nazwę
        String name = config.getString("name");
        if (name != null) {
            meta.setDisplayName(MessageUtils.colorize(name));
        }

        // Ustaw wytrzymałość
        int maxDurability = config.getInt("durability", 100);
        if (meta instanceof org.bukkit.inventory.meta.Damageable) {
            ((org.bukkit.inventory.meta.Damageable) meta).setDamage(0);
        }

        // Dodaj enchantmenty
        List<String> enchants = config.getStringList("enchants");
        for (String enchantStr : enchants) {
            String[] parts = enchantStr.split(":");
            if (parts.length != 2) continue;

            try {
                Enchantment enchant = Enchantment.getByName(parts[0]);
                int level = Integer.parseInt(parts[1]);
                if (enchant != null) {
                    meta.addEnchant(enchant, level, true);
                } else {
                    plugin.getLogger().log(Level.WARNING, "Nieprawidłowy enchant: " + parts[0]);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().log(Level.WARNING, "Nieprawidłowy poziom enchantu: " + parts[1]);
            }
        }

        // Ustaw lore
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Wymagany poziom: &e" + config.getInt("required_level", 1)));
        lore.add(MessageUtils.colorize("&7Wytrzymałość: &e" + maxDurability + "&7/&a" + maxDurability));
        
        // Dodaj informacje o bonusach
        ConfigurationSection bonuses = config.getConfigurationSection("bonuses");
        if (bonuses != null) {
            lore.add(MessageUtils.colorize("&7&m-------------------"));
            lore.add(MessageUtils.colorize("&6Bonusy:"));
            
            double catchChance = bonuses.getDouble("catch_chance", 1.0);
            if (catchChance > 1.0) {
                int bonus = (int)((catchChance - 1.0) * 100);
                lore.add(MessageUtils.colorize("&7➤ &a+" + bonus + "% &7szansy na złowienie"));
            }
            
            double rareFishChance = bonuses.getDouble("rare_fish_chance", 1.0);
            if (rareFishChance > 1.0) {
                int bonus = (int)((rareFishChance - 1.0) * 100);
                lore.add(MessageUtils.colorize("&7➤ &a+" + bonus + "% &7szansy na rzadkie ryby"));
            }
            
            double doubleCatchChance = bonuses.getDouble("double_catch_chance", 0.0);
            if (doubleCatchChance > 0.0) {
                int chance = (int)(doubleCatchChance * 100);
                lore.add(MessageUtils.colorize("&7➤ &a" + chance + "% &7szansy na podwójne złowienie"));
            }
        }
        
        meta.setLore(lore);

        // Zapisz dane w PersistentDataContainer
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(rodTypeKey, PersistentDataType.STRING, type);
        container.set(durabilityKey, PersistentDataType.INTEGER, maxDurability);

        rod.setItemMeta(meta);
        return rod;
    }
    
    public boolean isFishingRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD) {
            return false;
        }

        // Jeśli przedmiot nie ma metadanych, to jest to zwykła wędka
        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        String displayName = meta.getDisplayName();
        ConfigurationSection rodsSection = plugin.getConfigManager().getConfig().getConfigurationSection("fishing_rod");
        if (rodsSection == null) return false;

        // Sprawdź czy to jest jedna z naszych wędek
        for (String key : rodsSection.getKeys(false)) {
            String rodName = rodsSection.getString(key + ".name");
            if (rodName != null && displayName.equals(MessageUtils.colorize(rodName))) {
                return true;
            }
        }

        return false; // Jeśli to nie jest wędka z pluginu, nie pozwalamy na łowienie nią
    }
    
    public String getRodType(ItemStack item) {
        if (!isFishingRod(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        // Sprawdź PersistentDataContainer najpierw
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(rodTypeKey, PersistentDataType.STRING)) {
            return container.get(rodTypeKey, PersistentDataType.STRING);
        }
        
        // Jeśli nie ma danych w PDC, spróbuj rozpoznać po nazwie
        String displayName = meta.getDisplayName();
        String cleanName = org.bukkit.ChatColor.stripColor(displayName).toLowerCase();
        
        if (cleanName.contains("podstawowa")) return "basic";
        if (cleanName.contains("zaawansowana")) return "advanced";
        if (cleanName.contains("profesjonalna")) return "professional";
        if (cleanName.contains("mistrza")) return "master";
        
        // Sprawdź również angielskie nazwy
        if (cleanName.contains("basic")) return "basic";
        if (cleanName.contains("advanced")) return "advanced";
        if (cleanName.contains("professional")) return "professional";
        if (cleanName.contains("master")) return "master";
        
        return null;
    }

    public void damageFishingRod(ItemStack rod) {
        if (!isFishingRod(rod)) {
            plugin.getLogger().info("DEBUG: Przedmiot nie jest wędką z pluginu");
            return;
        }
        
        ItemMeta meta = rod.getItemMeta();
        if (meta == null) {
            plugin.getLogger().info("DEBUG: ItemMeta jest null");
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String type = container.has(rodTypeKey, PersistentDataType.STRING) ? 
                      container.get(rodTypeKey, PersistentDataType.STRING) : null;
        
        Integer currentDurability = container.has(durabilityKey, PersistentDataType.INTEGER) ? 
                                   container.get(durabilityKey, PersistentDataType.INTEGER) : null;

        // Jeśli wędka nie ma zapisanej wytrzymałości, ustaw domyślną
        if (type == null || currentDurability == null) {
            type = getRodType(rod);
            plugin.getLogger().info("DEBUG: Typ wędki odczytany z nazwy: " + type);
            
            if (type == null) {
                type = "basic"; // Domyślny typ wędki
                plugin.getLogger().info("DEBUG: Ustawiam domyślny typ wędki: basic");
            }
            
            currentDurability = plugin.getConfigManager().getConfig().getInt("fishing_rod." + type + ".durability", 100);
            plugin.getLogger().info("DEBUG: Ustawiam domyślną wytrzymałość: " + currentDurability);
            
            container.set(rodTypeKey, PersistentDataType.STRING, type);
            container.set(durabilityKey, PersistentDataType.INTEGER, currentDurability);
        }

        // Pobierz maksymalną wytrzymałość z konfiguracji
        int maxDurability = plugin.getConfigManager().getConfig().getInt("fishing_rod." + type + ".durability", 100);
        
        // Zmniejsz wytrzymałość
        currentDurability--;
        plugin.getLogger().info("DEBUG: Zmniejszam wytrzymałość wędki typu " + type + " z " + (currentDurability + 1) + " do " + currentDurability);
        
        container.set(durabilityKey, PersistentDataType.INTEGER, currentDurability);

        // Aktualizuj lore
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Wymagany poziom: &e" + plugin.getConfigManager().getConfig().getInt("fishing_rod." + type + ".required_level", 1)));
        lore.add(MessageUtils.colorize("&7Wytrzymałość: &e" + currentDurability + "&7/&a" + maxDurability));
        
        // Dodaj informacje o bonusach
        ConfigurationSection bonuses = plugin.getConfigManager().getConfig().getConfigurationSection("fishing_rod." + type + ".bonuses");
        if (bonuses != null) {
            lore.add(MessageUtils.colorize("&7&m-------------------"));
            lore.add(MessageUtils.colorize("&6Bonusy:"));
            
            double rareFishChance = bonuses.getDouble("rare_fish_chance", 1.0);
            if (rareFishChance > 1.0) {
                int bonus = (int)((rareFishChance - 1.0) * 100);
                lore.add(MessageUtils.colorize("&7➤ &a+" + bonus + "% &7szansy na rzadkie ryby"));
            }
            
            double doubleCatchChance = bonuses.getDouble("double_catch_chance", 0.0);
            if (doubleCatchChance > 0.0) {
                int chance = (int)(doubleCatchChance * 100);
                lore.add(MessageUtils.colorize("&7➤ &a" + chance + "% &7szansy na podwójne złowienie"));
            }
        }

        meta.setLore(lore);
        rod.setItemMeta(meta);
    }

    public int getDurability(ItemStack rod) {
        if (!isFishingRod(rod)) return 0;
        ItemMeta meta = rod.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);
    }

    public double getBonus(ItemStack rod, String bonusType) {
        if (!isFishingRod(rod)) return 1.0;
        String type = getRodType(rod);
        if (type == null) return 1.0;
        
        ConfigurationSection config = plugin.getConfigManager().getConfig().getConfigurationSection("fishing_rod." + type + ".bonuses");
        if (config == null) return 1.0;
        
        return config.getDouble(bonusType, 1.0);
    }
} 
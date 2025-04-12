package pl.stylowamc.smfishingplanet.items;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FishingRod {
    private final SMFishingPlanet plugin;
    private final NamespacedKey rodTypeKey;
    private final NamespacedKey durabilityKey;
    private final Map<String, FishingRod> rodTypes;
    private final String name;
    private final int durability;
    private final int requiredLevel;
    private final boolean availableEverywhere;
    private final List<String> cantUseAtRegions;
    private final double rareFishChance;
    private final double doubleCatchChance;

    public FishingRod(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.rodTypeKey = new NamespacedKey(plugin, "rod_type");
        this.durabilityKey = new NamespacedKey(plugin, "durability");
        this.rodTypes = new HashMap<>();
        this.name = "";
        this.durability = 0;
        this.requiredLevel = 0;
        this.availableEverywhere = true;
        this.cantUseAtRegions = new ArrayList<>();
        this.rareFishChance = 1.0;
        this.doubleCatchChance = 0.0;
        loadRodTypes();
    }

    public FishingRod(SMFishingPlanet plugin, String type) {
        this.plugin = plugin;
        this.rodTypeKey = new NamespacedKey(plugin, "rod_type");
        this.durabilityKey = new NamespacedKey(plugin, "durability");
        this.rodTypes = new HashMap<>();

        ConfigurationSection config = plugin.getConfig().getConfigurationSection("fishing_rod." + type);
        if (config == null) {
            throw new IllegalArgumentException("Nie znaleziono konfiguracji dla wędki typu: " + type);
        }

        this.name = MessageUtils.colorize(config.getString("name", "&aWędka"));
        this.durability = config.getInt("durability", 100);
        this.requiredLevel = config.getInt("required_level", 1);
        this.availableEverywhere = config.getBoolean("available_everywhere", true);
        this.cantUseAtRegions = config.getStringList("cantUseAtRegions");
        this.rareFishChance = config.getDouble("bonuses.rare_fish_chance", 1.0);
        this.doubleCatchChance = config.getDouble("bonuses.double_catch_chance", 0.0);
    }

    private void loadRodTypes() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("fishing_rod");
        if (config == null) return;

        for (String type : config.getKeys(false)) {
            rodTypes.put(type, new FishingRod(plugin, type));
        }
    }

    public boolean isFishingRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(rodTypeKey, PersistentDataType.STRING);
    }

    public FishingRod getRodFromItem(ItemStack item) {
        if (!isFishingRod(item)) {
            return null;
        }
        String type = item.getItemMeta().getPersistentDataContainer().get(rodTypeKey, PersistentDataType.STRING);
        return rodTypes.get(type);
    }

    public String getRodType(ItemStack item) {
        if (!isFishingRod(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(rodTypeKey, PersistentDataType.STRING);
    }

    public int getDurability(ItemStack item) {
        if (!isFishingRod(item)) {
            return 0;
        }
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);
    }

    public void setDurability(ItemStack item, int durability) {
        if (!isFishingRod(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, durability);
        updateRodLore(item, meta, durability);
        item.setItemMeta(meta);
    }

    private void updateRodLore(ItemStack item, ItemMeta meta, int currentDurability) {
        String type = getRodType(item);
        if (type == null) return;

        ConfigurationSection config = plugin.getConfig().getConfigurationSection("fishing_rod." + type);
        if (config == null) return;

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Wytrzymałość: &e" + currentDurability + "/" + config.getInt("durability", 100)));
        lore.add(MessageUtils.colorize("&7Wymagany poziom: &e" + config.getInt("required_level", 1)));
        
        // Dodaj informacje o bonusach
        ConfigurationSection bonuses = config.getConfigurationSection("bonuses");
        if (bonuses != null) {
            lore.add("");
            lore.add(MessageUtils.colorize("&7&lBonusy:"));
            if (bonuses.contains("rare_fish_chance")) {
                double chance = bonuses.getDouble("rare_fish_chance");
                String formattedChance = String.format("%.1f", (chance - 1.0) * 100);
                lore.add(MessageUtils.colorize("&7Szansa na rzadkie ryby: &e+" + formattedChance + "%"));
            }
            if (bonuses.contains("double_catch_chance")) {
                double chance = bonuses.getDouble("double_catch_chance");
                String formattedChance = String.format("%.1f", chance * 100);
                lore.add(MessageUtils.colorize("&7Szansa na podwójne złowienie: &e" + formattedChance + "%"));
            }
        }
        
        meta.setLore(lore);
    }

    public void damageRod(ItemStack item, int amount) {
        if (!isFishingRod(item)) {
            return;
        }
        int currentDurability = getDurability(item);
        setDurability(item, Math.max(0, currentDurability - amount));
    }

    public boolean canUseInRegion(Player player) {
        if (availableEverywhere) {
            return true;
        }

        // Sprawdź czy gracz jest w regionie, w którym nie można używać wędki
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        
        // Konwertuj lokalizację Bukkit na WorldEdit
        com.sk89q.worldedit.util.Location location = BukkitAdapter.adapt(player.getLocation());

        for (String region : cantUseAtRegions) {
            if (query.getApplicableRegions(location).getRegions().stream()
                    .anyMatch(r -> r.getId().equals(region))) {
                MessageUtils.sendMessage(player, "fishing.region_not_allowed", 
                    java.util.Collections.singletonMap("region", region));
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public double getRareFishChance() {
        return rareFishChance;
    }

    public double getDoubleCatchChance() {
        return doubleCatchChance;
    }

    public double getBonus(ItemStack item, String bonusType) {
        if (!isFishingRod(item)) {
            return 1.0;
        }
        String type = getRodType(item);
        if (type == null) {
            return 1.0;
        }
        return plugin.getConfig().getDouble("fishing_rod." + type + ".bonuses." + bonusType, 1.0);
    }

    public void damageFishingRod(ItemStack item) {
        if (!isFishingRod(item)) {
            return;
        }
        int currentDurability = getDurability(item);
        setDurability(item, Math.max(0, currentDurability - 1));
    }

    public ItemStack createFishingRod(String type) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("fishing_rod." + type);
        if (config == null) {
            return null;
        }

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        
        // Ustaw nazwę
        meta.setDisplayName(MessageUtils.colorize(config.getString("name", "&aWędka")));
        
        // Ustaw lore
        List<String> lore = new ArrayList<>();
        int maxDurability = config.getInt("durability", 100);
        lore.add(MessageUtils.colorize("&7Wytrzymałość: &e" + maxDurability + "/" + maxDurability));
        lore.add(MessageUtils.colorize("&7Wymagany poziom: &e" + config.getInt("required_level", 1)));
        
        // Dodaj informacje o bonusach
        ConfigurationSection bonuses = config.getConfigurationSection("bonuses");
        if (bonuses != null) {
            lore.add("");
            lore.add(MessageUtils.colorize("&7&lBonusy:"));
            if (bonuses.contains("rare_fish_chance")) {
                double chance = bonuses.getDouble("rare_fish_chance");
                String formattedChance = String.format("%.1f", (chance - 1.0) * 100);
                lore.add(MessageUtils.colorize("&7Szansa na rzadkie ryby: &e+" + formattedChance + "%"));
            }
            if (bonuses.contains("double_catch_chance")) {
                double chance = bonuses.getDouble("double_catch_chance");
                String formattedChance = String.format("%.1f", chance * 100);
                lore.add(MessageUtils.colorize("&7Szansa na podwójne złowienie: &e" + formattedChance + "%"));
            }
        }
        
        meta.setLore(lore);
        
        // Dodaj NBT data
        meta.getPersistentDataContainer().set(rodTypeKey, PersistentDataType.STRING, type);
        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, maxDurability);
        
        rod.setItemMeta(meta);
        return rod;
    }

    public static ItemStack createFishingRod(SMFishingPlanet plugin, String type) {
        FishingRod rod = new FishingRod(plugin, type);
        return rod.createFishingRod(type);
    }
} 
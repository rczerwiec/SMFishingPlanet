package pl.stylowamc.smfishingplanet.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.models.Fish;
import pl.stylowamc.smfishingplanet.models.Rarity;
import pl.stylowamc.smfishingplanet.models.Fish.WeightRange;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

public class FishManager {
    private final SMFishingPlanet plugin;
    private final Map<String, Rarity> rarities;
    private final Map<String, List<Fish>> fishByCategory;
    private final Map<String, List<Fish>> fishByRegion;
    private final Random random;
    private final List<Fish> playerFish;
    
    public FishManager(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.rarities = new HashMap<>();
        this.fishByCategory = new HashMap<>();
        this.fishByRegion = new HashMap<>();
        this.random = new Random();
        this.playerFish = new ArrayList<>();
        
        loadFish();
    }
    
    public void loadFish() {
        FileConfiguration config = plugin.getConfigManager().getFishConfig();
        
        // Ładowanie rzadkości
        ConfigurationSection raritiesSection = config.getConfigurationSection("rarities");
        if (raritiesSection != null) {
            for (String key : raritiesSection.getKeys(false)) {
                String name = raritiesSection.getString(key + ".name");
                String color = raritiesSection.getString(key + ".color");
                int chance = raritiesSection.getInt(key + ".chance");
                rarities.put(key, new Rarity(name, color, chance));
            }
        }

        // Ładowanie ryb
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryKey : categoriesSection.getKeys(false)) {
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryKey);
                if (categorySection == null) continue;

                String categoryName = categorySection.getString("name");
                Material icon = Material.valueOf(categorySection.getString("icon", "COD"));
                double valueMultiplier = categorySection.getDouble("value_multiplier", 1.0);
                List<Fish> fishList = new ArrayList<>();

                ConfigurationSection fishSection = categorySection.getConfigurationSection("fish");
                if (fishSection != null) {
                    for (String fishKey : fishSection.getKeys(false)) {
                        String name = fishSection.getString(fishKey + ".name");
                        double baseValue = fishSection.getDouble(fishKey + ".base_value");
                        boolean availableEverywhere = fishSection.getBoolean(fishKey + ".available_everywhere", false);

                        // Tworzymy rybę używając poprawnego konstruktora
                        Fish fish = new Fish(name, categoryName, baseValue, valueMultiplier, availableEverywhere);
                        
                        // Dodajemy zakresy wag do listy w obiekcie Fish
                        ConfigurationSection rangesSection = fishSection.getConfigurationSection(fishKey + ".weight_ranges");
                        if (rangesSection != null) {
                            for (String rangeKey : rangesSection.getKeys(false)) {
                                double min = rangesSection.getDouble(rangeKey + ".min");
                                double max = rangesSection.getDouble(rangeKey + ".max");
                                String rarityKey = rangesSection.getString(rangeKey + ".rarity");
                                Rarity rarity = rarities.get(rarityKey.toLowerCase());
                                if (rarity != null) {
                                    fish.getWeightRanges().add(new WeightRange(min, max, rarity));
                                }
                            }
                        }

                        fishList.add(fish);

                        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                            plugin.getLogger().info("Załadowano rybę: " + name + " (kategoria: " + categoryName + ", dostępna wszędzie: " + availableEverywhere + ")");
                        }

                        // Jeśli ryba nie jest dostępna wszędzie, dodaj ją do mapy regionów
                        if (!availableEverywhere) {
                            List<String> regions = fishSection.getStringList(fishKey + ".regions");
                            for (String region : regions) {
                                fishByRegion.computeIfAbsent(region, k -> new ArrayList<>()).add(fish);
                            }
                        }
                    }
                }

                fishByCategory.put(categoryKey, fishList);
            }
        }
    }
    
    public Fish getRandomFish(Player player) {
        List<Fish> availableFish = new ArrayList<>();
        
        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            player.sendMessage("§8[DEBUG] §7Szukam dostępnych ryb...");
        }
        
        // Sprawdź region gracza
        Location loc = player.getLocation();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        Set<String> playerRegions = query.getApplicableRegions(BukkitAdapter.adapt(loc))
            .getRegions().stream()
            .map(region -> region.getId())
            .collect(Collectors.toSet());
            
        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            if (playerRegions.isEmpty()) {
                player.sendMessage("§8[DEBUG] §7Brak regionów w tej lokacji - sprawdzam tylko ryby dostępne wszędzie");
            } else {
                player.sendMessage("§8[DEBUG] §7Znalezione regiony: §f" + String.join(", ", playerRegions));
            }
        }
        
        // Iteruj przez wszystkie kategorie ryb
        for (Map.Entry<String, List<Fish>> entry : fishByCategory.entrySet()) {
            String categoryKey = entry.getKey();
            double categoryMultiplier = plugin.getConfigManager().getFishConfig().getDouble("categories." + categoryKey + ".value_multiplier", 1.0);
            
            for (Fish fish : entry.getValue()) {
                boolean shouldAdd = false;
                
                // Jeśli gracz jest poza regionem, dodaj TYLKO ryby dostępne wszędzie
                if (playerRegions.isEmpty()) {
                    shouldAdd = fish.isAvailableEverywhere();
                    if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                        player.sendMessage("§8[DEBUG] §7Ryba §f" + fish.getName() + 
                            " §7dostępna wszędzie: §f" + fish.isAvailableEverywhere());
                    }
                } else {
                    // Jeśli gracz jest w regionie
                    if (fish.isAvailableEverywhere()) {
                        shouldAdd = true;
                    } else {
                        // Pobierz regiony dla ryby z konfiguracji
                        String fishKey = fish.getName().toLowerCase().replace(" ", "_");
                        List<String> fishRegions = plugin.getConfigManager().getFishConfig()
                            .getStringList("categories." + categoryKey + ".fish." + fishKey + ".regions");
                        
                        // Sprawdź czy gracz jest w którymkolwiek z regionów ryby
                        shouldAdd = playerRegions.stream().anyMatch(fishRegions::contains);
                        
                        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                            player.sendMessage("§8[DEBUG] §7Ryba §f" + fish.getName() + 
                                " §7jest dostępna w regionach: §f" + String.join(", ", fishRegions) +
                                " §7(dodana: §f" + shouldAdd + "§7)");
                        }
                    }
                }
                
                if (shouldAdd) {
                    Fish fishWithMultiplier = new Fish(
                        fish.getName(),
                        fish.getCategory(),
                        fish.getBaseValue(),
                        categoryMultiplier,
                        fish.getRarity(),
                        fish.getWeight(),
                        fish.isAvailableEverywhere()
                    );
                    fishWithMultiplier.getWeightRanges().addAll(fish.getWeightRanges());
                    availableFish.add(fishWithMultiplier);
                    
                    if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                        player.sendMessage("§8[DEBUG] §7Dodano rybę: §f" + fish.getName());
                    }
                }
            }
        }
        
        if (availableFish.isEmpty()) {
            if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                player.sendMessage("§8[DEBUG] §7Nie znaleziono dostępnych ryb!");
            }
            return null;
        }
        
        // Wybierz losową rybę z dostępnych
        Fish selectedFish = availableFish.get(random.nextInt(availableFish.size()));
        
        // Pobierz bonus do rzadkości z wędki
        ItemStack rod = player.getInventory().getItemInMainHand();
        double rareFishChance = plugin.getFishingRod().getBonus(rod, "rare_fish_chance");
        
        // Wybierz losowy zakres wagi dla wybranej ryby z uwzględnieniem bonusu do rzadkości
        WeightRange weightRange = getRandomWeightRange(selectedFish.getWeightRanges(), rareFishChance);
        if (weightRange != null) {
            selectedFish.setWeight(weightRange.getRandomWeight(random));
            selectedFish.setRarity(weightRange.getRarity());
        }
        
        return selectedFish;
    }
    
    private WeightRange getRandomWeightRange(List<WeightRange> ranges, double rarityBonus) {
        if (ranges == null || ranges.isEmpty()) {
            return null;
        }
        
        // Oblicz sumę szans z uwzględnieniem bonusu
        double totalChance = 0;
        for (WeightRange range : ranges) {
            if (range.getRarity() != null) {
                if (rarityBonus > 1.0) {
                    String rarityName = range.getRarity().getName().toLowerCase();
                    switch (rarityName) {
                        case "common":
                        case "pospolita":
                            // Drastycznie zmniejsz szansę na pospolite ryby dla lepszych wędek
                            totalChance += range.getRarity().getChance() / (rarityBonus * rarityBonus);
                            break;
                        case "uncommon":
                        case "niepospolita":
                            totalChance += range.getRarity().getChance() * rarityBonus;
                            break;
                        case "rare":
                        case "rzadka":
                            totalChance += range.getRarity().getChance() * (rarityBonus * 1.5);
                            break;
                        case "epic":
                        case "epicka":
                            totalChance += range.getRarity().getChance() * (rarityBonus * 2);
                            break;
                        case "legendary":
                        case "legendarna":
                            totalChance += range.getRarity().getChance() * (rarityBonus * 2.5);
                            break;
                        default:
                            totalChance += range.getRarity().getChance();
                    }
                } else {
                    totalChance += range.getRarity().getChance();
                }
            }
        }
            
        if (totalChance <= 0) {
            return ranges.get(random.nextInt(ranges.size()));
        }
        
        // Losuj zakres
        double roll = random.nextDouble() * totalChance;
        double currentSum = 0;
        
        for (WeightRange range : ranges) {
            if (range.getRarity() != null) {
                double chance = range.getRarity().getChance();
                if (rarityBonus > 1.0) {
                    String rarityName = range.getRarity().getName().toLowerCase();
                    switch (rarityName) {
                        case "common":
                        case "pospolita":
                            chance /= (rarityBonus * rarityBonus);
                            break;
                        case "uncommon":
                        case "niepospolita":
                            chance *= rarityBonus;
                            break;
                        case "rare":
                        case "rzadka":
                            chance *= (rarityBonus * 1.5);
                            break;
                        case "epic":
                        case "epicka":
                            chance *= (rarityBonus * 2);
                            break;
                        case "legendary":
                        case "legendarna":
                            chance *= (rarityBonus * 2.5);
                            break;
                    }
                }
                currentSum += chance;
                if (roll <= currentSum) {
                    return range;
                }
            }
        }
        
        // Jeśli nie wylosowano żadnego zakresu, zwróć losowy
        return ranges.get(random.nextInt(ranges.size()));
    }
    
    public ItemStack createFishItem(Fish fish) {
        ItemStack item = new ItemStack(Material.COD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Rarity rarity = fish.getRarity();
        String rarityColor = rarity.getColor();
        String rarityPrefix = getRarityPrefix(rarity.getName(), fish.getName());
        
        meta.setDisplayName(rarityColor + rarityPrefix + " " + fish.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.getMessage("fish.stats.weight").replace("%weight%", String.format("%.1f", fish.getWeight())));
        lore.add(MessageUtils.getMessage("fish.stats.value").replace("%value%", String.format("%.0f", fish.getValue())));
        lore.add(MessageUtils.getMessage("fish.stats.rarity").replace("%rarity%", rarity.getColor() + rarityPrefix));
        lore.add(MessageUtils.colorize("&7Kategoria: &f" + fish.getCategory()));
        lore.add("");
        
        Location loc = fish.getCatchLocation();
        if (loc != null) {
            lore.add("§7Złowiono na: §f" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()));
            
            // Sprawdź region
            String regionName = "Świat";
            if (!fish.isAvailableEverywhere()) {
                RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                RegionQuery query = container.createQuery();
                Set<String> regions = query.getApplicableRegions(BukkitAdapter.adapt(loc))
                    .getRegions().stream()
                    .map(region -> region.getId())
                    .collect(Collectors.toSet());
                
                // Znajdź pierwszy region z listy regionów ryby
                for (String region : regions) {
                    if (fishByRegion.containsKey(region)) {
                        regionName = region;
                        break;
                    }
                }
            }
            
            lore.add("§7Łowisko: §f" + regionName);
        }
        
        if (fish.getCatchDate() != null) {
            lore.add("§7Data złowienia: §f" + fish.getCatchDate());
        }
        
        if (fish.getCatcherName() != null) {
            lore.add("§7Złowił: §f" + fish.getCatcherName());
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private String getRarityPrefix(String rarity, String fishName) {
        // Sprawdź czy nazwa ryby kończy się na 'a'
        boolean isFeminine = fishName.toLowerCase().endsWith("a");
        
        // Dostosuj końcówkę w zależności od rodzaju
        switch (rarity.toLowerCase()) {
            case "pospolita":
                return isFeminine ? "Pospolita" : "Pospolity";
            case "niepospolita":
                return isFeminine ? "Niepospolita" : "Niepospolity";
            case "rzadka":
                return isFeminine ? "Rzadka" : "Rzadki";
            case "epicka":
                return isFeminine ? "Epicka" : "Epicki";
            case "legendarna":
                return isFeminine ? "Legendarna" : "Legendarny";
            default:
                return rarity;
        }
    }
    
    public void addFish(Player player, Fish fish) {
        // Ustaw datę i lokalizację złowienia
        fish.setCatchLocation(player.getLocation());
        fish.setCatchDate(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
        fish.setCatcherName(player.getName());
        
        // Stwórz ItemStack ryby
        ItemStack fishItem = createFishItem(fish);
        
        if (fishItem == null) {
            MessageUtils.sendMessage(player, "fishing.error");
            return;
        }
        
        // Dodaj XP w zależności od rzadkości ryby
        String rarityName = fish.getRarity().getName().toLowerCase();
        int baseXp = plugin.getConfigManager().getXpForRarity(rarityName);
        double xpMultiplier = plugin.getConfigManager().getXpMultiplier();
        double finalXp = baseXp * xpMultiplier;
        
        // Dodaj XP graczowi
        plugin.getPlayerDataManager().addXp(player, finalXp);
        
        // Daj rybę graczowi
        if (player.getInventory().firstEmpty() == -1) {
            MessageUtils.sendMessage(player, "fishing.inventory_full");
            // Upuść rybę na ziemię jeśli ekwipunek jest pełny
            player.getWorld().dropItemNaturally(player.getLocation(), fishItem);
        } else {
            player.getInventory().addItem(fishItem);
            player.updateInventory();
        }
        
        // Aktualizuj poziom i wyświetl informację
        int currentLevel = plugin.getPlayerDataManager().getLevel(player);
        double currentXp = plugin.getPlayerDataManager().getCurrentXp(player);
        double requiredXp = plugin.getPlayerDataManager().getRequiredXp(currentLevel + 1);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("fish_name", fish.getName());
        placeholders.put("rarity_color", fish.getRarity().getColor());
        placeholders.put("weight", String.format("%.1f", fish.getWeight()));
        placeholders.put("value", String.format("%.0f", fish.getValue()));
        placeholders.put("level", String.valueOf(currentLevel));
        placeholders.put("current_xp", String.format("%.1f", currentXp));
        placeholders.put("required_xp", String.format("%.1f", requiredXp));
        placeholders.put("progress", String.format("%.1f", (currentXp / requiredXp) * 100));
        placeholders.put("gained_xp", String.format("%.1f", finalXp));
        
        MessageUtils.sendMessage(player, "fishing.success_with_xp", placeholders);
    }
    
    public void removeFish(UUID playerUUID, Fish fish) {
        playerFish.removeIf(f -> f.getPlayerUUID().equals(playerUUID) && 
            f.getName().equals(fish.getName()) && 
            f.getWeight() == fish.getWeight());
    }
    
    public List<Fish> getPlayerFish(UUID playerUUID) {
        return playerFish.stream()
            .filter(fish -> fish.getPlayerUUID().equals(playerUUID))
            .collect(Collectors.toList());
    }
    
    public void clearPlayerFish(UUID playerUUID) {
        playerFish.removeIf(fish -> fish.getPlayerUUID().equals(playerUUID));
    }
    
    public Rarity getRarity(String name) {
        return rarities.get(name.toLowerCase());
    }
    
    public Map<String, Rarity> getRarities() {
        return Collections.unmodifiableMap(rarities);
    }
    
    public Map<String, List<Fish>> getFishByCategory() {
        return Collections.unmodifiableMap(fishByCategory);
    }
    
    public void giveRandomFish(Player player) {
        Fish fish = getRandomFish(player);
        if (fish == null) return;

        fish.setCatchLocation(player.getLocation());
        fish.setCatchDate(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
        fish.setCatcherName(player.getName());

        ItemStack fishItem = createFishItem(fish);

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(fishItem);
        } else {
            MessageUtils.sendMessage(player, "fishing.inventory_full");
        }
    }
    
    public void reloadFish() {
        rarities.clear();
        fishByCategory.clear();
        fishByRegion.clear();
        loadFish();
    }
    
    public void giveFish(Player player, Fish fish) {
        if (fish == null) return;
        
        // Dodaj informacje o złowieniu
        fish.setCatchLocation(player.getLocation());
        fish.setCatchDate(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()));
        fish.setCatcherName(player.getName());
        
        // Stwórz przedmiot ryby
        ItemStack fishItem = createFishItem(fish);
        
        // Dodaj do ekwipunku gracza
        if (fishItem != null) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(fishItem);
            } else {
                MessageUtils.sendMessage(player, "fishing.inventory_full");
            }
        }
    }

    public Fish getRandomFish(double rarityBonus) {
        // Pobierz wszystkie dostępne ryby
        List<Fish> availableFish = new ArrayList<>(fishByCategory.values().stream()
            .flatMap(List::stream)
            .filter(fish -> !fish.getWeightRanges().isEmpty()) // Filtruj tylko ryby z zakresami wag
            .collect(Collectors.toList()));
            
        if (availableFish.isEmpty()) {
            plugin.getLogger().warning("Nie znaleziono dostępnych ryb!");
            return null;
        }
        
        // Wybierz losową rybę
        Fish selectedFish = availableFish.get(random.nextInt(availableFish.size()));
        
        // Wybierz losowy zakres wagi z uwzględnieniem bonusu do rzadkości
        WeightRange selectedRange = getRandomWeightRange(selectedFish.getWeightRanges(), rarityBonus);
        if (selectedRange == null) {
            plugin.getLogger().warning("Nie można wylosować zakresu wagi dla ryby: " + selectedFish.getName());
            return null;
        }
        
        // Wygeneruj losową wagę z wybranego zakresu
        double weight = selectedRange.getRandomWeight(random);
        
        // Stwórz nową instancję ryby z wylosowaną wagą i rzadkością
        double categoryMultiplier = plugin.getConfigManager().getFishConfig().getDouble("categories." + selectedFish.getCategory() + ".value_multiplier", 1.0);
        
        return new Fish(
            selectedFish.getName(),
            selectedFish.getCategory(),
            selectedFish.getBaseValue(),
            categoryMultiplier,
            selectedRange.getRarity(),
            weight,
            selectedFish.isAvailableEverywhere()
        );
    }
} 
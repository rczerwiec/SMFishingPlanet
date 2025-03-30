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
    private boolean debug;
    
    public FishManager(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.rarities = new HashMap<>();
        this.fishByCategory = new HashMap<>();
        this.fishByRegion = new HashMap<>();
        this.random = new Random();
        this.playerFish = new ArrayList<>();
        this.debug = plugin.getConfigManager().getConfig().getBoolean("debug", false);
        
        loadFish();
    }
    
    private void sendDebug(Player player, String message) {
        // Zawsze wysyłaj do konsoli, niezależnie od ustawienia debug
        plugin.getLogger().info("[DEBUG] " + message.replace("§8", "").replace("§7", "").replace("§f", "").replace("§a", "").replace("§c", ""));
        
        // Do gracza wysyłaj tylko jeśli debug jest włączony
        if (debug) {
            player.sendMessage("§8[DEBUG] §7" + message);
        }
    }
    
    public void loadFish() {
        FileConfiguration config = plugin.getConfigManager().getFishConfig();
        
        // Wyczyść istniejące mapy
        rarities.clear();
        fishByCategory.clear();
        fishByRegion.clear();
        
        if (debug) {
            plugin.getLogger().info("=== Rozpoczynam ładowanie ryb ===");
        }
        
        // Ładowanie rzadkości
        ConfigurationSection raritiesSection = config.getConfigurationSection("rarities");
        if (raritiesSection != null) {
            for (String key : raritiesSection.getKeys(false)) {
                String name = raritiesSection.getString(key + ".name");
                String color = raritiesSection.getString(key + ".color");
                int chance = raritiesSection.getInt(key + ".chance");
                rarities.put(key, new Rarity(name, color, chance));
                if (debug) {
                    plugin.getLogger().info("Załadowano rzadkość: " + key + " (" + name + ")");
                }
            }
        }

        // Ładowanie ryb
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryKey : categoriesSection.getKeys(false)) {
                if (debug) {
                    plugin.getLogger().info("=== Ładowanie kategorii: " + categoryKey + " ===");
                }
                
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryKey);
                if (categorySection == null) {
                    if (debug) {
                        plugin.getLogger().warning("Nie znaleziono sekcji dla kategorii: " + categoryKey);
                    }
                    continue;
                }

                String categoryName = categorySection.getString("name");
                Material icon = Material.valueOf(categorySection.getString("icon", "COD"));
                double valueMultiplier = categorySection.getDouble("value_multiplier", 1.0);
                List<Fish> fishList = new ArrayList<>();

                ConfigurationSection fishSection = categorySection.getConfigurationSection("fish");
                if (fishSection != null) {
                    if (debug) {
                        plugin.getLogger().info("Znaleziono sekcję 'fish' dla kategorii " + categoryKey);
                        plugin.getLogger().info("Dostępne ryby w kategorii: " + String.join(", ", fishSection.getKeys(false)));
                    }
                    
                    for (String fishKey : fishSection.getKeys(false)) {
                        String name = fishSection.getString(fishKey + ".name");
                        double baseValue = fishSection.getDouble(fishKey + ".base_value");
                        boolean availableEverywhere = fishSection.getBoolean(fishKey + ".available_everywhere", false);
                        List<String> regions = fishSection.getStringList(fishKey + ".regions");

                        if (debug) {
                            plugin.getLogger().info("--- Ładowanie ryby: " + name + " ---");
                            plugin.getLogger().info("• Klucz w configu: " + fishKey);
                            plugin.getLogger().info("• Ścieżka do regionów: categories." + categoryKey + ".fish." + fishKey + ".regions");
                            plugin.getLogger().info("• Base Value: " + baseValue);
                            plugin.getLogger().info("• Available Everywhere: " + availableEverywhere);
                            plugin.getLogger().info("• Regiony: " + String.join(", ", regions));
                        }

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
                                    if (debug) {
                                        plugin.getLogger().info("  Dodano zakres wagi: " + rangeKey + " (" + min + "-" + max + "kg, " + rarityKey + ")");
                                    }
                                }
                            }
                        }

                        fishList.add(fish);

                        // Jeśli ryba nie jest dostępna wszędzie, dodaj ją do mapy regionów
                        if (!availableEverywhere && !regions.isEmpty()) {
                            for (String region : regions) {
                                fishByRegion.computeIfAbsent(region, k -> new ArrayList<>()).add(fish);
                                if (debug) {
                                    plugin.getLogger().info("§a✔ Dodano rybę " + name + " do regionu " + region);
                                }
                            }
                        } else if (!availableEverywhere && regions.isEmpty()) {
                            plugin.getLogger().warning("§c✖ Ryba " + name + " nie jest dostępna wszędzie, ale nie ma zdefiniowanych regionów!");
                        }
                    }
                } else {
                    if (debug) {
                        plugin.getLogger().warning("Nie znaleziono sekcji 'fish' dla kategorii " + categoryKey);
                    }
                }

                fishByCategory.put(categoryKey, fishList);
            }
        }
        
        if (debug) {
            plugin.getLogger().info("=== Podsumowanie załadowanych ryb ===");
            plugin.getLogger().info("Liczba kategorii: " + fishByCategory.size());
            plugin.getLogger().info("Liczba regionów: " + fishByRegion.size());
            for (Map.Entry<String, List<Fish>> entry : fishByRegion.entrySet()) {
                plugin.getLogger().info("Region " + entry.getKey() + ": " + entry.getValue().size() + " ryb");
                for (Fish fish : entry.getValue()) {
                    plugin.getLogger().info("- " + fish.getName());
                }
            }
        }
    }
    
    public Fish getRandomFish(Player player) {
        // Wymuszamy włączenie debug na potrzeby diagnostyki
        boolean originalDebugValue = this.debug;
        this.debug = true;
        
        // Zbierz informacje diagnostyczne
        String debugMessage = "\n\n===== DIAGNOSTYKA DEBUGOWANIA =====\n";
        debugMessage += "• Wartość debug w Managerze (oryginalna): " + originalDebugValue + "\n";
        debugMessage += "• Wartość debug w Managerze (po wymuszeniu): " + this.debug + "\n";
        debugMessage += "• Wartość w configu: " + plugin.getConfigManager().getConfig().getBoolean("debug", false) + "\n";
        debugMessage += "• Klasa: " + this.getClass().getSimpleName() + "\n";
        debugMessage += "• Metoda: getRandomFish(Player)\n";
        debugMessage += "• Gracz: " + player.getName() + "\n";
        debugMessage += "• Lokalizacja: " + player.getLocation() + "\n";
        debugMessage += "• Czas: " + new java.util.Date() + "\n";
        
        // Wymuś wypisanie do konsoli
        plugin.getLogger().info(debugMessage);
        
        try {
            // Wyślij wiadomość do gracza
            player.sendMessage("§8[DEBUG] §7Diagnostyka wypisana do konsoli - sprawdź logi serwera");
            
            sendDebug(player, "=== ROZPOCZYNAM TEST DEBUGOWANIA ===");
            sendDebug(player, "Debug jest włączony: " + debug);
            sendDebug(player, "Wartość w configu: " + plugin.getConfigManager().getConfig().getBoolean("debug", false));

            // Pobierz bonus do rzadkości z wędki
            ItemStack rod = player.getInventory().getItemInMainHand();
            double rareFishChance = plugin.getFishingRod().getBonus(rod, "rare_fish_chance");
            
            sendDebug(player, "Bonus do rzadkości z wędki: " + rareFishChance);

            List<Fish> availableFish = new ArrayList<>();
            
            sendDebug(player, "=== Start procesu losowania ryby ===");
            sendDebug(player, "Szukam dostępnych ryb...");
            
            // Sprawdź region gracza
            Location loc = player.getLocation();
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            Set<String> playerRegions = query.getApplicableRegions(BukkitAdapter.adapt(loc))
                .getRegions().stream()
                .map(region -> region.getId())
                .collect(Collectors.toSet());
                
            sendDebug(player, "=== Informacje o regionach ===");
            if (playerRegions.isEmpty()) {
                sendDebug(player, "§c✖ Brak regionów w tej lokacji - będą dostępne tylko ryby z available_everywhere: true");
            } else {
                sendDebug(player, "§a✔ Znalezione regiony gracza: §f" + String.join(", ", playerRegions));
                // Dodaj szczegółowe debugowanie regionów
                for (String region : playerRegions) {
                    sendDebug(player, "§7• Region: §f" + region);
                    List<Fish> fishInRegion = fishByRegion.get(region);
                    if (fishInRegion != null && !fishInRegion.isEmpty()) {
                        sendDebug(player, "  §7Ryby w tym regionie:");
                        for (Fish fish : fishInRegion) {
                            sendDebug(player, "    §7- §f" + fish.getName());
                        }
                    } else {
                        sendDebug(player, "  §c✖ Brak zdefiniowanych ryb dla tego regionu");
                    }
                }
            }
            
            // Iteruj przez wszystkie kategorie ryb
            for (Map.Entry<String, List<Fish>> entry : fishByCategory.entrySet()) {
                String categoryKey = entry.getKey();
                double categoryMultiplier = plugin.getConfigManager().getFishConfig().getDouble("categories." + categoryKey + ".value_multiplier", 1.0);
                
                sendDebug(player, "=== Sprawdzam kategorię: §f" + categoryKey + " §7===");
                
                for (Fish fish : entry.getValue()) {
                    boolean shouldAdd = false;
                    
                    // Debugowanie ścieżki do regionów
                    String fishKey = fish.getName().toLowerCase().replace(" ", "_");
                    
                    // Znajdź odpowiednią sekcję ryby w konfiguracji
                    ConfigurationSection fishSection = null;
                    String fishPath = null;
                    
                    // Szukaj ryby w konfiguracji
                    ConfigurationSection categoryFishSection = plugin.getConfigManager().getFishConfig()
                        .getConfigurationSection("categories." + categoryKey + ".fish");
                        
                    if (categoryFishSection != null) {
                        for (String key : categoryFishSection.getKeys(false)) {
                            String fishName = categoryFishSection.getString(key + ".name");
                            if (fishName != null && fishName.equals(fish.getName())) {
                                fishSection = categoryFishSection.getConfigurationSection(key);
                                fishPath = "categories." + categoryKey + ".fish." + key;
                                break;
                            }
                        }
                    }
                    
                    if (fishSection == null) {
                        sendDebug(player, "§c✖ Nie znaleziono sekcji w configu dla ryby: " + fish.getName());
                        continue;
                    }
                    
                    // Pobierz informacje o rybie
                    boolean availableEverywhere = fishSection.getBoolean("available_everywhere", false);
                    List<String> fishRegions = fishSection.getStringList("regions");
                    
                    sendDebug(player, "--- Sprawdzam rybę: §f" + fish.getName() + " §7---");
                    sendDebug(player, "• Ścieżka w configu: §f" + fishPath);
                    sendDebug(player, "• Available Everywhere: §f" + availableEverywhere);
                    sendDebug(player, "• Regiony ryby: §f" + (fishRegions.isEmpty() ? "brak" : String.join(", ", fishRegions)));
                    
                    // Jeśli ryba jest dostępna wszędzie, dodaj ją
                    if (availableEverywhere) {
                        shouldAdd = true;
                        sendDebug(player, "§a✔ Ryba dostępna wszędzie - dodaję do puli");
                    } 
                    // Jeśli gracz jest w regionie i ryba ma przypisane regiony, sprawdź czy gracz jest w odpowiednim regionie
                    else if (!fishRegions.isEmpty()) {
                        // Sprawdź czy którykolwiek z regionów gracza pokrywa się z regionami ryby
                        boolean regionMatch = false;
                        for (String playerRegion : playerRegions) {
                            if (fishRegions.contains(playerRegion)) {
                                regionMatch = true;
                                sendDebug(player, "§a✔ Znaleziono pasujący region §f" + playerRegion + "§7 dla ryby");
                                break;
                            }
                        }
                        
                        if (regionMatch) {
                            shouldAdd = true;
                            sendDebug(player, "§a✔ Dodaję rybę do puli");
                        } else {
                            sendDebug(player, "§c✖ Brak pasującego regionu dla ryby");
                        }
                    } else {
                        sendDebug(player, "§c✖ Ryba nie ma zdefiniowanych regionów - pomijam");
                    }
                    
                    if (shouldAdd) {
                        Fish fishWithMultiplier = new Fish(
                            fish.getName(),
                            fish.getCategory(),
                            fish.getBaseValue(),
                            categoryMultiplier,
                            fish.getRarity(),
                            fish.getWeight(),
                            availableEverywhere
                        );
                        fishWithMultiplier.getWeightRanges().addAll(fish.getWeightRanges());
                        availableFish.add(fishWithMultiplier);
                        sendDebug(player, "§a✔ Dodano rybę §f" + fish.getName() + "§7 do puli dostępnych ryb");
                    }
                }
            }
            
            if (availableFish.isEmpty()) {
                sendDebug(player, "§c✖ Nie znaleziono żadnych dostępnych ryb!");
                return null;
            }
            
            sendDebug(player, "=== Dostępne ryby ===");
            for (Fish fish : availableFish) {
                sendDebug(player, "• §f" + fish.getName() + " §7(wszędzie: §f" + fish.isAvailableEverywhere() + "§7)");
            }
            
            // Wybierz losową rybę z dostępnych
            Fish selectedFish = availableFish.get(random.nextInt(availableFish.size()));
            
            // Wybierz losowy zakres wagi dla wybranej ryby z uwzględnieniem bonusu do rzadkości
            WeightRange weightRange = getRandomWeightRange(selectedFish.getWeightRanges(), rareFishChance);
            if (weightRange != null) {
                selectedFish.setWeight(weightRange.getRandomWeight(random));
                selectedFish.setRarity(weightRange.getRarity());
            }
            
            sendDebug(player, "=== Wylosowana ryba ===");
            sendDebug(player, "• Nazwa: §f" + selectedFish.getName());
            sendDebug(player, "• Waga: §f" + selectedFish.getWeight());
            sendDebug(player, "• Rzadkość: §f" + selectedFish.getRarity().getName());
            sendDebug(player, "=== Koniec procesu losowania ===");
            
            // Przywróć oryginalną wartość debug
            this.debug = originalDebugValue;
            
            return selectedFish;
        } catch (Exception e) {
            // Zapisz wyjątek do logów
            plugin.getLogger().severe("Wystąpił błąd podczas losowania ryby:");
            e.printStackTrace();
            
            // Przywróć oryginalną wartość debug
            this.debug = originalDebugValue;
            
            return null;
        }
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
        this.debug = plugin.getConfigManager().getConfig().getBoolean("debug", false);
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
    
    public List<Fish> getFishByRegion(String region) {
        return fishByRegion.get(region);
    }
    
    public Map<String, List<Fish>> getAllFishByRegion() {
        return Collections.unmodifiableMap(fishByRegion);
    }
} 
package pl.stylowamc.smfishingplanet.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
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
import java.util.Calendar;

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
                // Dla wędki podstawowej (rarityBonus = 1.0) nie modyfikujemy szans
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
                    // Dla wędki podstawowej używamy normalnych szans
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
                // Dla wędki podstawowej (rarityBonus = 1.0) nie modyfikujemy szans
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
        lore.add(MessageUtils.colorize("&7Waga: &f" + String.format("%.1f", fish.getWeight()) + " kg"));
        lore.add(MessageUtils.colorize("&7Wartość: &e" + String.format("%.2f", fish.getValue()) + "$"));
        lore.add(MessageUtils.colorize("&7Rzadkość: " + rarity.getColor() + rarityPrefix));
        lore.add(MessageUtils.colorize("&7Kategoria: &f" + fish.getCategory()));
        lore.add("");
        
        Location loc = fish.getCatchLocation();
        if (loc != null) {
            lore.add("§7Złowiono na: §f" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()));
            
            // Sprawdź region
            String regionName = "Świat";
            
            // Pobierz regiony w lokalizacji złowienia
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            Set<String> regions = query.getApplicableRegions(BukkitAdapter.adapt(loc))
                .getRegions().stream()
                .map(region -> region.getId())
                .collect(Collectors.toSet());
            
            if (!regions.isEmpty()) {
                // Jeśli są jakieś regiony, użyj pierwszego z nich
                regionName = regions.iterator().next();
                plugin.getLogger().info("Znaleziono region dla ryby: " + regionName);
            }
            
            lore.add("§7Łowisko: §f" + regionName);
        }
        
        if (fish.getCatchDate() != null) {
            // Wyodrębnij tylko datę z pełnego formatu daty i godziny (format: dd/MM/yyyy HH:mm)
            String catchDate = fish.getCatchDate();
            if (catchDate.contains(" ")) {
                catchDate = catchDate.split(" ")[0]; // Pobierz tylko część z datą (dd/MM/yyyy)
            }
            lore.add("§7Data złowienia: §f" + catchDate);
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
        
        // Utwórz datę dodając tylko datę bez godziny
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
        fish.setCatchDate(dateFormat.format(new java.util.Date()));
        
        fish.setCatcherName(player.getName());
        
        // Stwórz ItemStack ryby
        ItemStack fishItem = createFishItem(fish);
        
        if (fishItem == null) {
            MessageUtils.sendMessage(player, "fishing.error");
            return;
        }
        
        // Dodaj XP w zależności od rzadkości ryby
        String rarityName = fish.getRarity().getName().toLowerCase();
        
        // Debug info przy przyznawaniu XP
        plugin.getLogger().info("=== DEBUG XP ===");
        plugin.getLogger().info("Rzadkość ryby: " + rarityName);
        
        // Pobierz bazowe XP dla rzadkości - teraz ConfigManager obsługuje polskie nazwy
        int baseXp = plugin.getConfigManager().getXpForRarity(rarityName);
        plugin.getLogger().info("Bazowe XP z konfiguracji: " + baseXp);
        
        // Pobierz mnożnik XP
        double xpMultiplier = plugin.getConfigManager().getXpMultiplier();
        plugin.getLogger().info("Mnożnik XP z konfiguracji: " + xpMultiplier);
        
        // Oblicz finalne XP
        int finalXp = (int)(baseXp * xpMultiplier);
        plugin.getLogger().info("Finalne XP (base * multiplier): " + finalXp);
        
        // Dodaj XP do postępu gracza
        plugin.getPlayerDataManager().addXp(player, finalXp);
        
        // Dodaj rybę do ekwipunku gracza
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(fishItem);
        
        if (!leftover.isEmpty()) {
            // Jeśli ekwipunek jest pełny, upuść rybę na ziemię
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            MessageUtils.sendMessage(player, "inventory_full");
        }
        
        // Dodaj rybę do listy złowionych przez gracza
        if (fish.getCatcherName() != null) {
            playerFish.add(fish);
        }
        
        // Aktualizuj statystyki gracza
        plugin.getPlayerDataManager().incrementTotalCatches(player);
        plugin.getPlayerDataManager().addFishCaught(player, fish.getName());
        plugin.getPlayerDataManager().addRarityCaught(player, rarityName);
        
        // Aktualizuj statystyki wagi
        double weight = fish.getWeight();
        if (weight < 1.0) {
            plugin.getPlayerDataManager().incrementSmallFishCount(player);
        } else if (weight < 3.0) {
            plugin.getPlayerDataManager().incrementMediumFishCount(player);
        } else if (weight < 5.0) {
            plugin.getPlayerDataManager().incrementLargeFishCount(player);
        } else {
            plugin.getPlayerDataManager().incrementHugeFishCount(player);
        }
        
        // Sprawdź czy to najcięższa ryba gracza
        plugin.getPlayerDataManager().checkHeaviestFish(player, fish);
        
        // Odtwórz dźwięk złowienia
        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfigManager().getCatchSound()), 1.0f, 1.0f);
        
        // Wyślij wiadomość o złowieniu
        String message = MessageUtils.getMessage("fish_caught")
            .replace("%rarity_color%", fish.getRarity().getColor())
            .replace("%fish_name%", fish.getName())
            .replace("%weight%", String.format("%.1f", fish.getWeight()))
            .replace("%value%", String.format("%.2f", fish.calculateValue()))
            .replace("%xp%", String.valueOf(finalXp));
        
        player.sendMessage(MessageUtils.colorize(message));
    }
    
    public void removeFish(UUID playerUUID, Fish fish) {
        playerFish.removeIf(f -> f.getPlayerUUID().equals(playerUUID) && 
            f.getName().equals(fish.getName()) && 
            f.getWeight() == fish.getWeight());
    }
    
    /**
     * Sprzedaje rybę gracza i aktualizuje statystyki
     * @param player Gracz sprzedający rybę
     * @param fish Ryba do sprzedaży
     * @return Wartość sprzedanej ryby
     */
    public double sellFish(Player player, Fish fish) {
        // Oblicz wartość ryby
        double value = fish.calculateValue();
        
        // Usuń rybę z inventory gracza
        removeFish(player.getUniqueId(), fish);
        
        // Dodaj balans do konta gracza
        plugin.getPlayerDataManager().addBalance(player, value);
        
        // Zaktualizuj statystyki sprzedaży
        plugin.getPlayerDataManager().registerFishSold(player, fish.getName(), value);
        
        return value;
    }
    
    /**
     * Sprzedaje wszystkie ryby gracza
     * @param player Gracz sprzedający ryby
     * @return Łączna wartość sprzedanych ryb
     */
    public double sellAllFish(Player player) {
        // Pobierz wszystkie ryby gracza
        List<Fish> playerFishes = getPlayerFish(player.getUniqueId());
        
        if (playerFishes.isEmpty()) {
            return 0;
        }
        
        double totalValue = 0;
        
        // Sprzedaj każdą rybę
        for (Fish fish : playerFishes) {
            double value = fish.calculateValue();
            totalValue += value;
            
            // Zaktualizuj statystyki sprzedaży
            plugin.getPlayerDataManager().registerFishSold(player, fish.getName(), value);
        }
        
        // Dodaj balans do konta gracza
        plugin.getPlayerDataManager().addBalance(player, totalValue);
        
        // Usuń wszystkie ryby
        clearPlayerFish(player.getUniqueId());
        
        return totalValue;
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
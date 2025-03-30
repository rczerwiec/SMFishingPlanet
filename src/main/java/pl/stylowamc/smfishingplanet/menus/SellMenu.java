package pl.stylowamc.smfishingplanet.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.models.Fish;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SellMenu {
    private final SMFishingPlanet plugin;

    public SellMenu(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.getMessage("sell.menu.title"));
        
        // Lista kategorii w odpowiedniej kolejności
        List<String> orderedCategories = Arrays.asList("small", "medium", "large", "special");
        
        // Dodaj kategorie w lepszym układzie - środek górnej części menu
        int[] slots = {11, 13, 15, 22, 29, 31, 33};
        int categoryIndex = 0;
        
        for (String categoryKey : orderedCategories) {
            if (plugin.getFishManager().getFishByCategory().containsKey(categoryKey) && categoryIndex < slots.length) {
                ItemStack categoryItem = createCategoryItem(categoryKey);
                inv.setItem(slots[categoryIndex], categoryItem);
                categoryIndex++;
            }
        }

        // Przycisk sprzedaj wszystko
        ItemStack sellAllItem = new ItemStack(Material.EMERALD);
        ItemMeta sellAllMeta = sellAllItem.getItemMeta();
        sellAllMeta.setDisplayName(MessageUtils.colorize("&a&lSprzedaj wszystkie ryby"));
        sellAllItem.setItemMeta(sellAllMeta);
        inv.setItem(49, sellAllItem);

        // Wypełnij puste sloty szybą
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta glassMeta = glass.getItemMeta();
                glassMeta.setDisplayName(" ");
                glass.setItemMeta(glassMeta);
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
    }

    public void openFishTypeMenu(Player player, String categoryKey) {
        List<Fish> categoryFish = plugin.getFishManager().getFishByCategory().get(categoryKey);
        if (categoryFish == null) return;

        // Pobierz nazwę kategorii z konfiguracji
        String categoryName = plugin.getConfigManager().getFishConfig().getString("categories." + categoryKey + ".name", categoryKey);

        // Zbierz unikalne nazwy ryb z kategorii
        Set<String> uniqueFishNames = categoryFish.stream()
            .map(Fish::getName)
            .collect(Collectors.toSet());

        Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.colorize("&8» &7Kategoria: &f" + categoryName));

        // Dodaj przyciski dla każdego gatunku ryby w lepszym układzie
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int fishIndex = 0;
        
        for (String fishName : uniqueFishNames) {
            if (fishIndex < slots.length) {
                ItemStack fishTypeItem = createFishTypeButton(fishName, player);
                inv.setItem(slots[fishIndex], fishTypeItem);
                fishIndex++;
            }
        }

        // Przycisk powrotu
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(MessageUtils.colorize("&c&lPowrót"));
        backItem.setItemMeta(backMeta);
        inv.setItem(49, backItem);

        // Przycisk sprzedaj wszystkie
        ItemStack sellAllItem = new ItemStack(Material.EMERALD);
        ItemMeta sellAllMeta = sellAllItem.getItemMeta();
        sellAllMeta.setDisplayName(MessageUtils.colorize("&a&lSprzedaj wszystkie"));
        List<String> sellAllLore = new ArrayList<>();
        sellAllLore.add(MessageUtils.colorize("&7Kliknij aby sprzedać wszystkie"));
        sellAllLore.add(MessageUtils.colorize("&7ryby z tej kategorii"));
        sellAllLore.add("category:" + categoryKey);
        sellAllMeta.setLore(sellAllLore);
        sellAllItem.setItemMeta(sellAllMeta);
        inv.setItem(53, sellAllItem);

        // Wypełnij puste sloty szybą
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta glassMeta = glass.getItemMeta();
                glassMeta.setDisplayName(" ");
                glass.setItemMeta(glassMeta);
                inv.setItem(i, glass);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createCategoryItem(String categoryKey) {
        Material icon = Material.COD;
        String categoryName = "";
        
        // Pobierz nazwę i ikonę kategorii z konfiguracji
        String configPath = "categories." + categoryKey;
        if (plugin.getConfigManager().getFishConfig().contains(configPath)) {
            categoryName = plugin.getConfigManager().getFishConfig().getString(configPath + ".name", categoryKey);
            String iconName = plugin.getConfigManager().getFishConfig().getString(configPath + ".icon", "COD");
            try {
                icon = Material.valueOf(iconName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Nieprawidłowy materiał dla kategorii " + categoryKey + ": " + iconName);
            }
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.colorize("&8» &f" + categoryName));
        
        // Dodaj klucz kategorii do lore (ukryty)
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Kliknij aby zobaczyć dostępne"));
        lore.add(MessageUtils.colorize("&7gatunki ryb w tej kategorii"));
        lore.add("category:" + categoryKey); // Dodaj klucz kategorii w odpowiednim formacie
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFishTypeButton(String fishName, Player player) {
        ItemStack item = new ItemStack(Material.COD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.colorize("&8» &f" + fishName));
        
        // Policz ile ryb danego gatunku ma gracz
        long count = Arrays.stream(player.getInventory().getContents())
            .filter(Objects::nonNull)
            .filter(itemStack -> {
                if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                    String displayName = itemStack.getItemMeta().getDisplayName();
                    return displayName.contains(fishName);
                }
                return false;
            })
            .count();
            
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Posiadasz: &f" + count));
        lore.add(MessageUtils.colorize("&7Kliknij aby sprzedać wszystkie"));
        lore.add(MessageUtils.colorize("&7ryby tego gatunku"));
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }

    public void sellAllFish(Player player) {
        // Używamy nowej metody z FishManager która obsługuje również statystyki
        double totalValue = plugin.getFishManager().sellAllFish(player);
        
        if (totalValue > 0) {
            // Zaktualizuj ekwipunek gracza
            player.updateInventory();
            
            // Wyślij wiadomość o sprzedaży
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("value", String.format("%.2f", totalValue));
            MessageUtils.sendMessage(player, "sell.success", placeholders);
        } else {
            MessageUtils.sendMessage(player, "sell.nothing");
        }
    }
    
    public void sellFishByType(Player player, String fishName) {
        double totalValue = 0;
        int soldCount = 0;
        
        List<Fish> playerFish = plugin.getFishManager().getPlayerFish(player.getUniqueId());
        // Znajdź wszystkie ryby danego typu
        List<Fish> fishToSell = playerFish.stream()
            .filter(fish -> fish.getName().equals(fishName))
            .collect(Collectors.toList());
        
        // Sprzedaj każdą rybę
        for (Fish fish : fishToSell) {
            double value = plugin.getFishManager().sellFish(player, fish);
            totalValue += value;
            soldCount++;
        }
        
        if (soldCount > 0) {
            // Zaktualizuj ekwipunek gracza
            player.updateInventory();
            
            // Wyślij wiadomość o sprzedaży
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("fish_name", fishName);
            placeholders.put("amount", String.valueOf(soldCount));
            placeholders.put("value", String.format("%.2f", totalValue));
            MessageUtils.sendMessage(player, "messages.fish_sold", placeholders);
        } else {
            MessageUtils.sendMessage(player, "sell.nothing");
        }
    }
} 
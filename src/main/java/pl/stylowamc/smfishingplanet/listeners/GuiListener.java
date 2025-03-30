package pl.stylowamc.smfishingplanet.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.managers.ConfigManager;
import pl.stylowamc.smfishingplanet.managers.FishManager;
import pl.stylowamc.smfishingplanet.models.Fish;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiListener implements Listener {
    private final SMFishingPlanet plugin;
    private final ConfigManager configManager;
    private final FishManager fishManager;

    public GuiListener(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.fishManager = plugin.getFishManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Główne menu sprzedaży
        if (title.equals(MessageUtils.getMessage("sell.menu.title"))) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;

            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.equals(MessageUtils.colorize("&a&lSprzedaj wszystkie ryby"))) {
                sellAllFish(player);
                player.closeInventory();
            } else {
                // Sprawdź czy kliknięto kategorię
                List<String> lore = clickedItem.getItemMeta().getLore();
                if (lore != null) {
                    for (String line : lore) {
                        if (line.startsWith("category:")) {
                            String category = line.substring(9);
                            plugin.getSellMenu().openFishTypeMenu(player, category);
                            break;
                        }
                    }
                }
            }
        }
        // Menu kategorii
        else if (title.startsWith(MessageUtils.colorize("&8» &7Kategoria:"))) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) return;

            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.equals(MessageUtils.colorize("&c&lPowrót"))) {
                plugin.getSellMenu().openMainMenu(player);
            } else if (displayName.equals(MessageUtils.colorize("&a&lSprzedaj wszystkie"))) {
                sellCategoryFish(player, event.getInventory());
                player.closeInventory();
            } else {
                // Sprzedaj ryby danego typu
                String fishName = MessageUtils.stripColor(displayName.replace("» ", "").trim());
                if (!fishName.isEmpty()) {
                    sellFishByType(player, fishName);
                    // Odśwież menu
                    String category = MessageUtils.stripColor(title.split(": ")[1].trim());
                    plugin.getSellMenu().openFishTypeMenu(player, category);
                }
            }
        }
    }

    private void handleSellMenuClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot == 22) { // Slot przycisku "Sprzedaj wszystko"
            sellAllFish(player);
        } else if (slot >= 10 && slot <= 16 && (slot % 2 == 0)) { // Sloty kategorii
            String categoryKey = getCategoryFromSlot(slot);
            if (categoryKey != null) {
                plugin.getSellMenu().openFishTypeMenu(player, categoryKey);
            }
        }
    }

    private void handleCategoryMenuClick(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot == 49) { // Sprzedaj wszystko z kategorii
            sellCategoryFish(player, event.getInventory());
        } else if (slot == 45) { // Powrót
            plugin.getSellMenu().openMainMenu(player);
        } else if (slot < 45 && event.getCurrentItem() != null) { // Kliknięcie w rybę
            String fishName = MessageUtils.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            sellFishByType(player, fishName);
        }
    }

    private String getCategoryFromSlot(int slot) {
        int index = (slot - 10) / 2;
        List<String> categories = new ArrayList<>(plugin.getFishManager().getFishByCategory().keySet());
        return index < categories.size() ? categories.get(index) : null;
    }

    private void sellAllFish(Player player) {
        double totalValue = 0.0;
        int soldCount = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COD && item.hasItemMeta() && item.getItemMeta().hasLore()) {
                List<String> lore = item.getItemMeta().getLore();
                for (String line : lore) {
                    if (line.contains("Wartość:")) {
                        try {
                            String valueStr = line.split(": ")[1].replaceAll("[^0-9.]", "");
                            double value = Double.parseDouble(valueStr);
                            totalValue += value;
                            soldCount++;
                            player.getInventory().remove(item);
                        } catch (Exception ignored) {}
                        break;
                    }
                }
            }
        }
        
        if (soldCount > 0) {
            plugin.getEconomy().depositPlayer(player, totalValue);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(soldCount));
            placeholders.put("value", String.format("%.2f", totalValue));
            MessageUtils.sendMessage(player, "sell.success", placeholders);
        } else {
            MessageUtils.sendMessage(player, "sell.no_fish");
        }
    }

    private void sellCategoryFish(Player player, Inventory categoryMenu) {
        String category = "";
        for (ItemStack item : categoryMenu.getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
                List<String> lore = item.getItemMeta().getLore();
                for (String line : lore) {
                    if (line.startsWith("category:")) {
                        category = line.substring(9);
                        break;
                    }
                }
                if (!category.isEmpty()) break;
            }
        }
        
        if (category.isEmpty()) {
            MessageUtils.sendMessage(player, "sell.error");
            return;
        }

        double totalValue = 0.0;
        int soldCount = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COD && item.hasItemMeta() && 
                item.getItemMeta().hasLore() && isFishInCategory(item, category)) {
                List<String> lore = item.getItemMeta().getLore();
                for (String line : lore) {
                    if (line.contains("Wartość:")) {
                        try {
                            String valueStr = line.split(": ")[1].replaceAll("[^0-9.]", "");
                            double value = Double.parseDouble(valueStr);
                            totalValue += value;
                            soldCount++;
                            player.getInventory().remove(item);
                        } catch (Exception ignored) {}
                        break;
                    }
                }
            }
        }

        if (soldCount > 0) {
            plugin.getEconomy().depositPlayer(player, totalValue);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(soldCount));
            placeholders.put("value", String.format("%.2f", totalValue));
            MessageUtils.sendMessage(player, "sell.success", placeholders);
        } else {
            MessageUtils.sendMessage(player, "sell.no_fish");
        }
    }

    private void sellFishByType(Player player, String fishName) {
        double totalValue = 0.0;
        int soldCount = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COD && item.hasItemMeta() && 
                item.getItemMeta().hasDisplayName() && 
                MessageUtils.stripColor(item.getItemMeta().getDisplayName()).contains(fishName)) {
                
                List<String> lore = item.getItemMeta().getLore();
                if (lore != null) {
                    for (String line : lore) {
                        if (line.contains("Wartość:")) {
                            try {
                                String valueStr = line.split(": ")[1].replaceAll("[^0-9.]", "");
                                double value = Double.parseDouble(valueStr);
                                totalValue += value;
                                soldCount++;
                                player.getInventory().remove(item);
                            } catch (Exception ignored) {}
                            break;
                        }
                    }
                }
            }
        }
        
        if (soldCount > 0) {
            plugin.getEconomy().depositPlayer(player, totalValue);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(soldCount));
            placeholders.put("value", String.format("%.2f", totalValue));
            placeholders.put("fish_name", fishName);
            MessageUtils.sendMessage(player, "sell.success", placeholders);
        } else {
            MessageUtils.sendMessage(player, "sell.no_fish");
        }
    }

    private boolean isFishInCategory(ItemStack item, String category) {
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            for (String line : lore) {
                if (line.startsWith("category:") && line.substring(9).equals(category)) {
                    return true;
                }
            }
        }
        return false;
    }
} 
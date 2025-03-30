package pl.stylowamc.smfishingplanet.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class Fish {
    private final String name;
    private final String category;
    private final double baseValue;
    private final double categoryMultiplier;
    private Rarity rarity;
    private double weight;
    private final UUID playerUUID;
    private final boolean availableEverywhere;
    private final List<WeightRange> weightRanges;
    
    // Nowe pola
    private Location catchLocation;
    private String catchDate;
    private String catcherName;
    
    // Konstruktor dla szablonu ryby
    public Fish(String name, String category, double baseValue, double categoryMultiplier, boolean availableEverywhere) {
        this.name = name;
        this.category = category;
        this.baseValue = baseValue;
        this.categoryMultiplier = categoryMultiplier;
        this.weightRanges = new ArrayList<>();
        this.rarity = null;
        this.weight = 0.0;
        this.playerUUID = null;
        this.availableEverywhere = availableEverywhere;
        this.catchLocation = null;
        this.catchDate = null;
        this.catcherName = null;
    }
    
    // Konstruktor dla złowionej ryby
    public Fish(String name, String category, double baseValue, double categoryMultiplier, Rarity rarity, double weight, boolean availableEverywhere) {
        this.name = name;
        this.category = category;
        this.baseValue = baseValue;
        this.categoryMultiplier = categoryMultiplier;
        this.rarity = rarity;
        this.weight = weight;
        this.playerUUID = null;
        this.availableEverywhere = availableEverywhere;
        this.weightRanges = new ArrayList<>();
        this.catchLocation = null;
        this.catchDate = null;
        this.catcherName = null;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public double getBaseValue() {
        return baseValue;
    }
    
    public List<WeightRange> getWeightRanges() {
        return weightRanges;
    }
    
    public Rarity getRarity() {
        return rarity;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public boolean isAvailableEverywhere() {
        return availableEverywhere;
    }
    
    public double getCategoryMultiplier() {
        return categoryMultiplier;
    }
    
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        
        // Ustaw nazwę z rzadkością
        meta.setDisplayName(rarity.getFormattedName() + " " + name);
        
        // Ustaw opis
        List<String> lore = new ArrayList<>();
        lore.add("§7Waga: §f" + String.format("%.2f", weight) + " kg");
        lore.add("§7Wartość: §f" + String.format("%.0f", calculateValue()) + " monet");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    public double calculateValue() {
        // Bazowa wartość pomnożona przez mnożnik kategorii
        double value = baseValue * categoryMultiplier;
        
        // Bonus za rzadkość (do +50%)
        double rarityBonus = switch (rarity.getName().toLowerCase()) {
            case "common" -> 1.0;     // Brak bonusu
            case "uncommon" -> 1.1;   // +10%
            case "rare" -> 1.25;      // +25%
            case "epic" -> 1.35;      // +35%
            case "legendary" -> 1.5;   // +50%
            default -> 1.0;           // Domyślnie brak bonusu
        };
        value *= rarityBonus;
        
        // Bonus za wagę (do +30%)
        double weightBonus = Math.min(1.3, 1 + (weight / 10));
        value *= weightBonus;
        
        return value;
    }
    
    public double getValue() {
        return calculateValue();
    }
    
    public double calculateXp() {
        // Podstawowe XP zależne od wartości ryby
        double xp = calculateValue() * 0.1;
        
        // Bonus za rzadkość
        if (rarity != null) {
            switch (rarity.getName().toLowerCase()) {
                case "common":
                    xp *= 1.0;
                    break;
                case "uncommon":
                    xp *= 1.2;
                    break;
                case "rare":
                    xp *= 1.5;
                    break;
                case "epic":
                    xp *= 2.0;
                    break;
                case "legendary":
                    xp *= 3.0;
                    break;
            }
        }
        
        // Bonus za wagę (maksymalnie +50%)
        double minWeight = 0.0;
        if (weightRanges != null && !weightRanges.isEmpty()) {
            minWeight = weightRanges.get(0).getMin();
        }
        double weightBonus = Math.min(0.5, (weight - minWeight) / 5.0);
        xp *= (1 + weightBonus);
        
        return Math.round(xp);
    }
    
    public static class WeightRange {
        private final double min;
        private final double max;
        private final Rarity rarity;
        
        public WeightRange(double min, double max, Rarity rarity) {
            this.min = min;
            this.max = max;
            this.rarity = rarity;
        }
        
        public double getMin() {
            return min;
        }
        
        public double getMax() {
            return max;
        }
        
        public Rarity getRarity() {
            return rarity;
        }
        
        public double getRandomWeight(Random random) {
            double weight = min + (random.nextDouble() * (max - min));
            return Math.round(weight * 10.0) / 10.0; // Zaokrąglij do 1 miejsca po przecinku
        }
    }
    
    // Gettery i settery dla nowych pól
    public Location getCatchLocation() {
        return catchLocation;
    }
    
    public void setCatchLocation(Location catchLocation) {
        this.catchLocation = catchLocation;
    }
    
    public String getCatchDate() {
        return catchDate;
    }
    
    public void setCatchDate(String catchDate) {
        this.catchDate = catchDate;
    }
    
    public String getCatcherName() {
        return catcherName;
    }
    
    public void setCatcherName(String catcherName) {
        this.catcherName = catcherName;
    }
    
    @Override
    public Fish clone() {
        Fish clonedFish = new Fish(
            this.name,
            this.category,
            this.baseValue,
            this.categoryMultiplier,
            this.rarity,
            this.weight,
            this.availableEverywhere
        );
        
        clonedFish.weightRanges.addAll(this.weightRanges);
        clonedFish.catchLocation = this.catchLocation;
        clonedFish.catchDate = this.catchDate;
        clonedFish.catcherName = this.catcherName;
        
        return clonedFish;
    }
    
    public void setRarity(Rarity rarity) {
        this.rarity = rarity;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
} 
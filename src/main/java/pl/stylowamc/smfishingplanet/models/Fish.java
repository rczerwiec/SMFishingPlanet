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
        lore.add("§7Wartość: §f" + String.format("%.2f", calculateValue()) + "$");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    public double calculateValue() {
        System.out.println("DEBUG: Kalkulacja wartości dla ryby: " + name);
        
        // Bazowa wartość z pola klasy
        System.out.println("DEBUG: Bazowa wartość: " + baseValue);
        
        // Mnożnik kategorii z pola klasy
        System.out.println("DEBUG: Mnożnik kategorii: " + categoryMultiplier);
        
        // Upewnij się, że wartość bazowa nie jest zerowa ani ujemna
        double safeBaseValue = Math.max(0.01, baseValue);
        if (safeBaseValue != baseValue) {
            System.out.println("DEBUG: Skorygowana wartość bazowa: " + safeBaseValue);
        }
        
        // Upewnij się, że mnożnik kategorii nie jest zerowy ani ujemny
        double safeCategoryMultiplier = Math.max(0.01, categoryMultiplier);
        if (safeCategoryMultiplier != categoryMultiplier) {
            System.out.println("DEBUG: Skorygowany mnożnik kategorii: " + safeCategoryMultiplier);
        }
        
        double calculatedValue = safeBaseValue * safeCategoryMultiplier;
        System.out.println("DEBUG: Wartość po mnożniku kategorii: " + calculatedValue);
        
        // Upewnij się, że wartość nie jest zbyt niska
        if (calculatedValue < 0.01) {
            calculatedValue = 0.01;
            System.out.println("DEBUG: Wartość poniżej minimalnej, ustawiam na: " + calculatedValue);
        }
        
        // Bonus za rzadkość
        String rarityName = rarity.getName().toLowerCase();
        double rarityBonus = 1.0;
        
        switch (rarityName) {
            case "pospolita":
                rarityBonus = 1.0;
                break;
            case "niepospolita":
                rarityBonus = 1.5;
                break;
            case "rzadka":
                rarityBonus = 2.0;
                break;
            case "epicka":
                rarityBonus = 3.0;
                break;
            case "legendarna":
                rarityBonus = 5.0;
                break;
            default:
                rarityBonus = 1.0;
                break;
        }
        
        System.out.println("DEBUG: Rzadkość: " + rarityName + ", bonus: " + rarityBonus);
        calculatedValue *= rarityBonus;
        System.out.println("DEBUG: Wartość po bonusie za rzadkość: " + calculatedValue);
        
        // Bonus za wagę (do 30% więcej dla cięższych egzemplarzy)
        double weightBonus = Math.max(1.0, 1.0 + (weight / 20.0)); // Max bonus wynosi +30% dla 6kg ryby
        if (weightBonus > 1.3) weightBonus = 1.3; // Ogranicz do +30%
        
        System.out.println("DEBUG: Waga: " + weight + "kg, bonus: " + weightBonus);
        calculatedValue *= weightBonus;
        System.out.println("DEBUG: Finalna wartość po bonusie za wagę: " + calculatedValue);
        
        // Upewnij się, że finalna wartość nie jest mniejsza niż 0.01
        if (calculatedValue < 0.01) {
            calculatedValue = 0.01;
            System.out.println("DEBUG: Finalna wartość poniżej minimalnej, ustawiam na: " + calculatedValue);
        }
        
        // Zaokrąglenie do 2 miejsc po przecinku
        double roundedValue = Math.round(calculatedValue * 100.0) / 100.0;
        System.out.println("DEBUG: Finalna wartość po zaokrągleniu: " + roundedValue);
        
        return Math.max(0.01, roundedValue); // Dodatkowe zabezpieczenie przed wartościami ujemnymi lub zerowymi
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
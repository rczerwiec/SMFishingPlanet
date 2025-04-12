package pl.stylowamc.smfishingplanet.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private double balance;
    private int level;
    private double xp;
    private UUID playerUUID;
    
    // Statystyki łowienia
    private int totalCatches;       // Łącznie złowionych ryb
    private int failedCatches;      // Nieudane próby łowienia
    private Map<String, Integer> fishCaught;  // Liczba złowionych ryb według nazwy
    private Map<String, Integer> rarityCaught; // Liczba złowionych ryb według rzadkości
    private int trashCaught;       // Liczba złowionych śmieci
    
    // Statystyki wagi
    private int smallFishCaught;    // Małe ryby (< 1kg)
    private int mediumFishCaught;   // Średnie ryby (1-5kg)
    private int largeFishCaught;    // Duże ryby (5-15kg)
    private int hugeFishCaught;     // Ogromne ryby (>15kg)
    
    // Rekordy
    private double heaviestFish;     // Najcięższa złowiona ryba
    private String heaviestFishName; // Nazwa najcięższej ryby
    private String heaviestFishDate; // Data złowienia najcięższej ryby
    
    // Statystyki ekonomiczne
    private double totalEarnings;    // Łączny zarobek ze sprzedaży ryb
    
    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.balance = 0;
        this.level = 1;
        this.xp = 0;
        this.totalCatches = 0;
        this.failedCatches = 0;
        this.fishCaught = new HashMap<>();
        this.rarityCaught = new HashMap<>();
        this.trashCaught = 0;
        this.smallFishCaught = 0;
        this.mediumFishCaught = 0;
        this.largeFishCaught = 0;
        this.hugeFishCaught = 0;
        this.heaviestFish = 0;
        this.heaviestFishName = "";
        this.heaviestFishDate = "";
        this.totalEarnings = 0;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public double getXp() {
        return xp;
    }
    
    public void setXp(double xp) {
        this.xp = xp;
    }
    
    public void addXp(double xp) {
        this.xp += xp;
    }
    
    public double getCurrentXp() {
        return xp;
    }
    
    // Nowe metody
    
    public void incrementCatches() {
        totalCatches++;
    }
    
    public void incrementFailedCatches() {
        failedCatches++;
    }
    
    public void incrementTrashCaught() {
        trashCaught++;
    }
    
    public void addFishCaught(String fishName) {
        fishCaught.put(fishName, fishCaught.getOrDefault(fishName, 0) + 1);
    }
    
    public void addRarityCaught(String rarity) {
        rarityCaught.put(rarity, rarityCaught.getOrDefault(rarity, 0) + 1);
    }
    
    public void addFishByWeight(double weight) {
        if (weight < 1.0) {
            smallFishCaught++;
        } else if (weight < 5.0) {
            mediumFishCaught++;
        } else if (weight < 15.0) {
            largeFishCaught++;
        } else {
            hugeFishCaught++;
        }
        
        // Aktualizuj rekord
        if (weight > heaviestFish) {
            heaviestFish = weight;
        }
    }
    
    public void setHeaviestFish(double weight, String fishName, String date) {
        if (weight > heaviestFish) {
            heaviestFish = weight;
            heaviestFishName = fishName;
            heaviestFishDate = date;
        }
    }
    
    public void addEarnings(double amount) {
        totalEarnings += amount;
    }
    
    // Gettery dla statystyk
    
    public int getTotalCatches() {
        return totalCatches;
    }
    
    public int getFailedCatches() {
        return failedCatches;
    }
    
    public int getTrashCaught() {
        return trashCaught;
    }
    
    public Map<String, Integer> getFishCaught() {
        return fishCaught;
    }
    
    public Map<String, Integer> getRarityCaught() {
        return rarityCaught;
    }
    
    public int getSmallFishCaught() {
        return smallFishCaught;
    }
    
    public int getMediumFishCaught() {
        return mediumFishCaught;
    }
    
    public int getLargeFishCaught() {
        return largeFishCaught;
    }
    
    public int getHugeFishCaught() {
        return hugeFishCaught;
    }
    
    public double getHeaviestFish() {
        return heaviestFish;
    }
    
    public String getHeaviestFishName() {
        return heaviestFishName;
    }
    
    public String getHeaviestFishDate() {
        return heaviestFishDate;
    }
    
    public double getTotalEarnings() {
        return totalEarnings;
    }
    
    // Metody utility
    
    public void resetStats() {
        totalCatches = 0;
        failedCatches = 0;
        trashCaught = 0;
        fishCaught.clear();
        rarityCaught.clear();
        smallFishCaught = 0;
        mediumFishCaught = 0;
        largeFishCaught = 0;
        hugeFishCaught = 0;
        heaviestFish = 0;
        heaviestFishName = "";
        heaviestFishDate = "";
        totalEarnings = 0;
    }
    
    public int getTotalFishCaught() {
        return smallFishCaught + mediumFishCaught + largeFishCaught + hugeFishCaught;
    }
    
    public double getSuccessRate() {
        if (totalCatches + failedCatches == 0) return 0;
        return (double) totalCatches / (totalCatches + failedCatches) * 100;
    }

    public double getRequiredXp() {
        // Wzór na wymagane XP do następnego poziomu
        return 15 + (level - 1) * 15 + Math.max(0, (level - 3) * 10) + Math.max(0, (level - 8) * 10) + Math.max(0, (level - 12) * 10);
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }
} 
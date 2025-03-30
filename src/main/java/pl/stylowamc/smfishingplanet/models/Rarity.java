package pl.stylowamc.smfishingplanet.models;

public class Rarity {
    private final String name;
    private final String color;
    private final int chance;

    public Rarity(String name, String color, int chance) {
        this.name = name;
        this.color = color;
        this.chance = chance;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public int getChance() {
        return chance;
    }

    public String getFormattedName() {
        return color + name;
    }
} 
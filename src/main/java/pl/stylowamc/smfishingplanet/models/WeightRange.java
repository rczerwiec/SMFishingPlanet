package pl.stylowamc.smfishingplanet.models;

public class WeightRange {
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

    public boolean isInRange(double weight) {
        return weight >= min && weight <= max;
    }
} 
package pl.stylowamc.smfishingplanet.crafting;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.items.FishingLine;
import org.bukkit.inventory.meta.ItemMeta;

public class FishingRodRecipe {
    private final SMFishingPlanet plugin;
    private final FishingLine fishingLine;
    
    public FishingRodRecipe(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.fishingLine = new FishingLine(plugin);
    }
    
    private boolean isFishingLine(ItemStack item, String size) {
        if (item == null || item.getType() != Material.STRING || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = meta.getDisplayName();
        return displayName.contains("Żyłka " + size);
    }
    
    private RecipeChoice.ExactChoice createFishingLineChoice(ItemStack fishingLineItem) {
        return new RecipeChoice.ExactChoice(fishingLineItem) {
            @Override
            public boolean test(ItemStack item) {
                if (item == null || !item.hasItemMeta()) return false;
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) return false;
                
                ItemStack original = this.getChoices().get(0);
                ItemMeta originalMeta = original.getItemMeta();
                if (originalMeta == null || !originalMeta.hasDisplayName()) return false;
                
                return meta.getDisplayName().equals(originalMeta.getDisplayName());
            }
        };
    }
    
    public ShapedRecipe createBasicRodRecipe() {
        // Stwórz podstawową wędkę
        ItemStack basicRod = plugin.getFishingRod().createFishingRod("basic");
        if (basicRod == null) return null;
        
        // Stwórz klucz dla receptury
        NamespacedKey key = new NamespacedKey(plugin, "basic_fishing_rod");
        
        // Stwórz recepturę
        ShapedRecipe recipe = new ShapedRecipe(key, basicRod);
        
        // Ustaw kształt (0 - puste, 1 - patyk, 2 - żyłka)
        recipe.shape("  I", " I2", "I 2");
        
        // Stwórz żyłkę jako składnik
        ItemStack fishingLineItem = fishingLine.createFishingLine();
        
        // Ustaw składniki
        recipe.setIngredient('I', Material.STICK); // patyk
        recipe.setIngredient('2', createFishingLineChoice(fishingLineItem)); // dokładnie nasza żyłka
        
        return recipe;
    }
    
    public ShapedRecipe createAdvancedRodRecipe() {
        ItemStack advancedRod = plugin.getFishingRod().createFishingRod("advanced");
        if (advancedRod == null) return null;
        
        NamespacedKey key = new NamespacedKey(plugin, "advanced_fishing_rod");
        ShapedRecipe recipe = new ShapedRecipe(key, advancedRod);
        
        recipe.shape("  I", " I4", "I 4");
        
        ItemStack fishingLineItem = fishingLine.createFishingLine4mm();
        
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('4', createFishingLineChoice(fishingLineItem));
        
        return recipe;
    }
    
    public ShapedRecipe createProRodRecipe() {
        ItemStack proRod = plugin.getFishingRod().createFishingRod("pro");
        if (proRod == null) return null;
        
        NamespacedKey key = new NamespacedKey(plugin, "pro_fishing_rod");
        ShapedRecipe recipe = new ShapedRecipe(key, proRod);
        
        recipe.shape("  I", " I5", "I 5");
        
        ItemStack fishingLineItem = fishingLine.createFishingLine5mm();
        
        recipe.setIngredient('I', Material.GOLD_INGOT);
        recipe.setIngredient('5', createFishingLineChoice(fishingLineItem));
        
        return recipe;
    }
    
    public ShapedRecipe createMasterRodRecipe() {
        ItemStack masterRod = plugin.getFishingRod().createFishingRod("master");
        if (masterRod == null) return null;
        
        NamespacedKey key = new NamespacedKey(plugin, "master_fishing_rod");
        ShapedRecipe recipe = new ShapedRecipe(key, masterRod);
        
        recipe.shape("  I", " I6", "I 6");
        
        ItemStack fishingLineItem = fishingLine.createFishingLine6mm();
        
        recipe.setIngredient('I', Material.DIAMOND);
        recipe.setIngredient('6', createFishingLineChoice(fishingLineItem));
        
        return recipe;
    }
} 
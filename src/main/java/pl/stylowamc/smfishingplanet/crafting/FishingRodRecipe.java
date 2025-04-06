package pl.stylowamc.smfishingplanet.crafting;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

public class FishingRodRecipe {
    private final SMFishingPlanet plugin;
    
    public FishingRodRecipe(SMFishingPlanet plugin) {
        this.plugin = plugin;
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
                
                // Wyciągamy tylko nazwę i porównujemy ją, ignorując inne atrybuty
                String itemName = ChatColor.stripColor(meta.getDisplayName());
                String originalName = ChatColor.stripColor(originalMeta.getDisplayName());
                
                // Sprawdzamy czy żyłka ma odpowiednią wielkość (np. "Żyłka 4mm")
                if (originalName.contains("4mm") && itemName.contains("4mm")) return true;
                if (originalName.contains("5mm") && itemName.contains("5mm")) return true;
                if (originalName.contains("6mm") && itemName.contains("6mm")) return true;
                
                return false;
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
        
        // Ustaw kształt (0 - puste, I - składnik (patyk/ingot), L - żyłka)
        recipe.shape("  I", " IL", "I L");
        
        // Stwórz żyłkę jako składnik - używamy żyłki z pl.stylowamc.smfishingplanet.models.FishingLine
        pl.stylowamc.smfishingplanet.models.FishingLine fishingLineModel = 
            pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType("basic");
        ItemStack fishingLineItem = fishingLineModel.createItemStack();
        
        // Ustaw składniki
        recipe.setIngredient('I', Material.STICK); // patyk
        recipe.setIngredient('L', createFishingLineChoice(fishingLineItem)); // dokładnie nasza żyłka
        
        return recipe;
    }
    
    public ShapedRecipe createAdvancedRodRecipe() {
        ItemStack advancedRod = plugin.getFishingRod().createFishingRod("advanced");
        if (advancedRod == null) return null;
        
        NamespacedKey key = new NamespacedKey(plugin, "advanced_fishing_rod");
        ShapedRecipe recipe = new ShapedRecipe(key, advancedRod);
        
        recipe.shape("  I", " IL", "I L");
        
        // Używamy żyłki z modelu zamiast z items
        pl.stylowamc.smfishingplanet.models.FishingLine fishingLineModel = 
            pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType("basic");
        ItemStack fishingLineItem = fishingLineModel.createItemStack();
        
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('L', createFishingLineChoice(fishingLineItem));
        
        return recipe;
    }
    
    public ShapedRecipe createProfessionalRodRecipe() {
        ItemStack proRod = plugin.getFishingRod().createFishingRod("professional");
        if (proRod == null) return null;
        
        NamespacedKey key = new NamespacedKey(plugin, "professional_fishing_rod");
        ShapedRecipe recipe = new ShapedRecipe(key, proRod);
        
        recipe.shape("  I", " IL", "I L");
        
        // Używamy żyłki z modelu zamiast z items
        pl.stylowamc.smfishingplanet.models.FishingLine fishingLineModel = 
            pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType("advanced");
        ItemStack fishingLineItem = fishingLineModel.createItemStack();
        
        recipe.setIngredient('I', Material.GOLD_INGOT);
        recipe.setIngredient('L', createFishingLineChoice(fishingLineItem));
        
        return recipe;
    }
    
    public ShapedRecipe createMasterRodRecipe() {
        ItemStack masterRod = plugin.getFishingRod().createFishingRod("master");
        if (masterRod == null) return null;
        
        NamespacedKey key = new NamespacedKey(plugin, "master_fishing_rod");
        ShapedRecipe recipe = new ShapedRecipe(key, masterRod);
        
        recipe.shape("  I", " IL", "I L");
        
        // Używamy żyłki z modelu zamiast z items
        pl.stylowamc.smfishingplanet.models.FishingLine fishingLineModel = 
            pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType("professional");
        ItemStack fishingLineItem = fishingLineModel.createItemStack();
        
        recipe.setIngredient('I', Material.DIAMOND);
        recipe.setIngredient('L', createFishingLineChoice(fishingLineItem));
        
        return recipe;
    }
} 
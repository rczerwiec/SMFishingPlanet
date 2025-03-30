package pl.stylowamc.smfishingplanet.items;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class FishingLine {
    private final SMFishingPlanet plugin;
    
    public FishingLine(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createFishingLine() {
        return createFishingLine("2mm", "Początkowej");
    }
    
    public ItemStack createFishingLine4mm() {
        return createFishingLine("4mm", "Zaawansowanej");
    }
    
    public ItemStack createFishingLine5mm() {
        return createFishingLine("5mm", "Profesjonalnej");
    }
    
    public ItemStack createFishingLine6mm() {
        return createFishingLine("6mm", "Mistrza");
    }
    
    private ItemStack createFishingLine(String size, String rodType) {
        ItemStack item = new ItemStack(Material.STRING);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        meta.setDisplayName(MessageUtils.colorize("&f&lŻyłka " + size));
        
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Używana do craftowania"));
        lore.add(MessageUtils.colorize("&7Wędki " + rodType));
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
} 
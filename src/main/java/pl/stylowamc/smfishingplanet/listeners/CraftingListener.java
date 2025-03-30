package pl.stylowamc.smfishingplanet.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

public class CraftingListener implements Listener {
    private final SMFishingPlanet plugin;
    
    public CraftingListener(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        ItemStack result = event.getRecipe().getResult();
        if (!plugin.getFishingRod().isFishingRod(result)) return;
        
        String rodType = plugin.getFishingRod().getRodType(result);
        if (rodType == null) return;
        
        int requiredLevel = plugin.getConfigManager().getConfig().getInt("fishing_rod." + rodType + ".required_level", 1);
        int playerLevel = plugin.getPlayerDataManager().getLevel(player);
        
        if (playerLevel < requiredLevel) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, "rod_not_allowed", "level", String.valueOf(requiredLevel));
        }
    }
} 
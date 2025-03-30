package pl.stylowamc.smfishingplanet.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.items.FishingLine;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.Random;

public class VanillaFishingListener implements Listener {
    private final SMFishingPlanet plugin;
    private final Random random;
    private final FishingLine fishingLine;
    
    public VanillaFishingListener(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.fishingLine = new FishingLine(plugin);
    }
    
    @EventHandler
    public void onVanillaFishing(PlayerFishEvent event) {
        // Sprawdź czy to jest zwykła wędka
        if (plugin.getFishingRod().isFishingRod(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }
        
        // Sprawdź czy złowiono rybę
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Pobierz szansę z konfiguracji
            double dropChance = plugin.getConfig().getDouble("fishing_line.drop_chance", 0.05);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                event.getPlayer().sendMessage("§8[DEBUG] §7Szansa na żyłkę: §f" + String.format("%.1f%%", dropChance * 100));
            }
            
            // Sprawdź czy wylosowano żyłkę
            if (random.nextDouble() < dropChance) {
                ItemStack line = fishingLine.createFishingLine();
                event.getPlayer().getWorld().dropItemNaturally(
                    event.getHook().getLocation(),
                    line
                );
                
                // Wyślij wiadomość jeśli jest włączona
                if (plugin.getConfig().getBoolean("fishing_line.show_message", true)) {
                    String message = plugin.getConfig().getString("fishing_line.message", "&aZnalazłeś żyłkę 2mm!");
                    event.getPlayer().sendMessage(MessageUtils.colorize(message));
                }
                
                if (plugin.getConfig().getBoolean("debug", false)) {
                    event.getPlayer().sendMessage("§8[DEBUG] §7Wylosowano drop żyłki!");
                }
            }
        }
    }
} 
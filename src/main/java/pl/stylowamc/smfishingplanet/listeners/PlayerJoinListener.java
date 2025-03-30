package pl.stylowamc.smfishingplanet.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;

public class PlayerJoinListener implements Listener {
    private final SMFishingPlanet plugin;

    public PlayerJoinListener(SMFishingPlanet plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().loadPlayerData(player);
    }
} 
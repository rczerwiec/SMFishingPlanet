package pl.stylowamc.smfishingplanet.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.items.FishingRod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class FishingBar implements Listener {
    private final SMFishingPlanet plugin;
    private final Player player;
    private final BossBar bossBar;
    private final Random random;
    
    private double progress;
    private double direction;
    private double successZoneStart;
    private double successZoneEnd;
    private boolean active;
    
    // Mapa przechowująca aktywne paski łowienia dla graczy
    private static final Map<UUID, FishingBar> activeBars = new HashMap<>();
    
    public FishingBar(SMFishingPlanet plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.random = new Random();
        
        // Stwórz pasek bossa
        this.bossBar = Bukkit.createBossBar(
            MessageUtils.getMessage("fishing.bar_title"),
            BarColor.BLUE,
            BarStyle.SOLID
        );
        
        // Dodaj gracza do paska
        this.bossBar.addPlayer(player);
        
        // Ustaw początkowe wartości
        this.progress = 0.0;
        this.direction = 0.02; // Prędkość poruszania się wskaźnika
        this.active = true;
        
        // Losowo ustaw strefę sukcesu (między 0.3 a 0.7)
        this.successZoneStart = 0.3 + (random.nextDouble() * 0.2); // 0.3-0.5
        this.successZoneEnd = successZoneStart + 0.2; // +0.2 szerokości
        
        // Aktualizuj pasek
        updateBar();
        
        // Zapisz pasek w mapie
        activeBars.put(player.getUniqueId(), this);
    }

    public void update() {
        if (!active) return;
        
        // Przesuń wskaźnik
        progress += direction;
        
        // Odbij od krawędzi
        if (progress >= 1.0 || progress <= 0.0) {
            direction *= -1;
            progress = Math.max(0.0, Math.min(1.0, progress));
        }
        
        // Debug
        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            player.sendMessage("§8[DEBUG] §7Progress: §f" + String.format("%.2f", progress) + 
                             " §7Success Zone: §f" + String.format("%.2f", successZoneStart) + 
                             " - " + String.format("%.2f", successZoneEnd));
        }
        
        // Aktualizuj pasek
        updateBar();
    }
    
    private void updateBar() {
        bossBar.setProgress(progress);
        
        // Zmień kolor paska w zależności od pozycji wskaźnika
        if (isInSuccessZone()) {
            bossBar.setColor(BarColor.GREEN);
        } else {
            bossBar.setColor(BarColor.BLUE);
        }
    }
    
    public boolean isInSuccessZone() {
        return progress >= successZoneStart && progress <= successZoneEnd;
    }
    
    public void remove() {
        active = false;
        bossBar.removeAll();
        
        // Usuń z mapy aktywnych pasków
        activeBars.remove(player.getUniqueId());
        
        // Wyrejestruj listener
        PlayerInteractEvent.getHandlerList().unregister(this);
    }
    
    public static FishingBar getActiveFishingBar(Player player) {
        return activeBars.get(player.getUniqueId());
    }
} 
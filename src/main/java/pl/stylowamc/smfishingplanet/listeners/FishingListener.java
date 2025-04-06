package pl.stylowamc.smfishingplanet.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.FireworkEffect;
import org.bukkit.scheduler.BukkitRunnable;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;
import pl.stylowamc.smfishingplanet.models.Fish;
import pl.stylowamc.smfishingplanet.utils.FishingBar;
import pl.stylowamc.smfishingplanet.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FishingListener implements Listener {
    private final SMFishingPlanet plugin;
    private final Map<Player, BossBar> activeBossBars;
    private final Map<Player, BukkitRunnable> activeRunnables;
    private final Random random;
    
    public FishingListener(SMFishingPlanet plugin) {
        this.plugin = plugin;
        this.activeBossBars = new HashMap<>();
        this.activeRunnables = new HashMap<>();
        this.random = new Random();
    }
    
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();

        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            player.sendMessage("§8[DEBUG] §7Event: §f" + event.getState().name());
            player.sendMessage("§8[DEBUG] §7Rod type: §f" + (plugin.getFishingRod().isFishingRod(rod) ? "Plugin Rod" : "Vanilla Rod"));
        }

        // Sprawdź czy to jest nasza wędka czy vanilla
        boolean isPluginRod = plugin.getFishingRod().isFishingRod(rod);

        // Jeśli to zwykła wędka, pozwól na normalną mechanikę
        if (!isPluginRod) {
            return;
        }

        // Sprawdź poziom gracza
        String rodType = plugin.getFishingRod().getRodType(rod);
        if (rodType == null) return;
        
        int requiredLevel = plugin.getConfigManager().getConfig().getInt("fishing_rod." + rodType + ".required_level", 1);
        int playerLevel = plugin.getPlayerDataManager().getLevel(player);
        
        if (playerLevel < requiredLevel) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, "rod_not_allowed", "level", String.valueOf(requiredLevel));
            return;
        }

        // Od tego momentu obsługujemy tylko specjalną wędkę
        switch (event.getState()) {
            case FISHING:
                // Gracz zarzuca wędkę - NIE anulujemy tego eventu
                if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                    player.sendMessage("§8[DEBUG] §7Rozpoczynam łowienie specjalną wędką");
                }
                startFishing(player);
                break;

            case BITE:
                // Ryba złapała przynętę - rozpocznij minigre
                event.setCancelled(true);
                if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                    player.sendMessage("§8[DEBUG] §7Ryba złapała przynętę - rozpoczynam minigre");
                }
                startMinigame(player, event.getHook().getLocation());
                break;

            case CAUGHT_FISH:
            case REEL_IN:
                // Gracz złapał rybę - sprawdź czy wygrał minigre
                event.setCancelled(true);
                if (activeBossBars.containsKey(player)) {
                    BossBar bossBar = activeBossBars.get(player);
                    if (bossBar.getTitle().equals(MessageUtils.colorize("&e&l⚡ KLIKNIJ TERAZ! &e&l⚡"))) {
                        // Gracz kliknął w odpowiednim momencie
                        handleFishingSuccess(player, event.getHook().getLocation());
                    } else {
                        // Gracz kliknął za wcześnie lub za późno
                        handleFishingFail(player);
                    }
                    cleanupFishingState(player);
                }
                // Wyciągnij przynętę
                if (event.getHook() != null) {
                    event.getHook().remove();
                }
                break;

            case FAILED_ATTEMPT:
                // Gracz przerwał łowienie
                event.setCancelled(true);
                if (activeBossBars.containsKey(player)) {
                    if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                        player.sendMessage("§8[DEBUG] §7Przerwano łowienie");
                    }
                    handleFishingFail(player);
                    cleanupFishingState(player);
                }
                break;
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Inicjalizacja danych gracza
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Zapisz dane gracza
        plugin.getPlayerDataManager().savePlayerData(event.getPlayer());
    }

    // Dodaj nowy event handler do śledzenia zmiany przedmiotu w ręce
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        cleanupFishingState(player);
    }

    // Dodaj event handler do śledzenia upuszczenia przedmiotu
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        cleanupFishingState(player);
    }

    private void startFishing(Player player) {
        // Usuń wszystkie aktywne bossbary gracza
        Bukkit.getBossBars().forEachRemaining(bossBar -> {
            if (bossBar.getPlayers().contains(player)) {
                bossBar.removePlayer(player);
            }
        });
        
        // Usuń poprzedni bossbar i runnable jeśli istnieją
        if (activeBossBars.containsKey(player)) {
            activeBossBars.get(player).removeAll();
            activeBossBars.remove(player);
        }
        if (activeRunnables.containsKey(player)) {
            activeRunnables.get(player).cancel();
            activeRunnables.remove(player);
        }

        // Stwórz nowy bossbar
        BossBar bossBar = plugin.getServer().createBossBar(
            MessageUtils.colorize("&3&l⚓ Łowienie... &3&l⚓"),
            BarColor.BLUE,
            BarStyle.SEGMENTED_20
        );
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        activeBossBars.put(player, bossBar);

        // Uruchom animację
        BukkitRunnable runnable = new BukkitRunnable() {
            private int ticks = 0;
            private final int totalTicks = 100; // 5 sekund

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    this.cancel();
                    return;
                }

                bossBar.setProgress((double) ticks / totalTicks);
                ticks++;
            }
        };

        runnable.runTaskTimer(plugin, 0L, 1L);
        activeRunnables.put(player, runnable);
    }

    private void startMinigame(Player player, Location hookLocation) {
        // Wyczyść poprzedni stan jeśli istnieje
        cleanupFishingState(player);

        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            player.sendMessage("§8[DEBUG] §7Rozpoczynam minigre łowienia");
        }

        // Stwórz nowy bossbar
        BossBar bossBar = Bukkit.createBossBar(
            MessageUtils.colorize("&b&lŁowienie..."),
            BarColor.BLUE,
            BarStyle.SOLID
        );
        
        // Dodaj tylko gracza łowiącego
        bossBar.addPlayer(player);
        activeBossBars.put(player, bossBar);

        // Stwórz nowy runnable
        BukkitRunnable runnable = new BukkitRunnable() {
            private double progress = 0.0;
            private double direction = 0.02; // Prędkość paska
            private boolean waitingForClick = false;
            private int ticksWaiting = 0;
            private final int maxTicksWaiting = 10; // Czas na kliknięcie (w tickach)

            @Override
            public void run() {
                // Sprawdź czy gracz nadal trzyma wędkę
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                if (!player.isOnline() || !activeBossBars.containsKey(player) || !plugin.getFishingRod().isFishingRod(currentItem)) {
                    if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().warning("Przerwano minigre - gracz offline/zmienił przedmiot/brak bossbara");
                    }
                    cleanupFishingState(player);
                    cancel();
                    return;
                }

                if (waitingForClick) {
                    ticksWaiting++;
                    if (ticksWaiting >= maxTicksWaiting) {
                        // Gracz nie kliknął na czas
                        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                            player.sendMessage("§8[DEBUG] §cGracz nie kliknął na czas!");
                        }
                        bossBar.setTitle(MessageUtils.colorize("&c&lZa późno!"));
                        bossBar.setColor(BarColor.RED);
                        handleFishingFail(player);
                        
                        // Opóźnione czyszczenie stanu
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            cleanupFishingState(player);
                        }, 20L); // 1 sekunda opóźnienia
                        
                        cancel();
                    }
                    return;
                }

                progress += direction;
                // Zapobiegaj przekroczeniu wartości granicznych
                if (progress >= 0.99) {
                    progress = 0.99;
                    direction = -direction;
                } else if (progress <= 0.01) {
                    progress = 0.01;
                    direction = -direction;
                }

                if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                    player.sendMessage("§8[DEBUG] §7Progress: §f" + String.format("%.2f", progress));
                }

                bossBar.setProgress(progress);

                // Losowa szansa na "złapanie"
                if (Math.random() < 0.02) { // 2% szansa na tick
                    waitingForClick = true;
                    bossBar.setTitle(MessageUtils.colorize("&e&l⚡ KLIKNIJ TERAZ! &e&l⚡"));
                    bossBar.setColor(BarColor.YELLOW);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    
                    // Wyślij wiadomość na środku ekranu
                    player.sendTitle(
                        MessageUtils.colorize("&e&l⚡ KLIKNIJ! &e&l⚡"),
                        MessageUtils.colorize("&7aby złowić rybę"),
                        0, 20, 10
                    );

                    if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                        player.sendMessage("§8[DEBUG] §aPojawiła się możliwość złowienia!");
                    }
                }
            }
        };

        runnable.runTaskTimer(plugin, 0L, 1L);
        activeRunnables.put(player, runnable);

        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            player.sendMessage("§8[DEBUG] §7Minigra rozpoczęta pomyślnie");
        }
    }

    private boolean isInSweetSpot(double progress, double sweetSpotStart, double sweetSpotSize) {
        return progress >= sweetSpotStart && progress <= sweetSpotStart + sweetSpotSize;
    }

    public void handleFishingSuccess(Player player, Location hookLocation) {
        plugin.getLogger().info("\n\n===== ROZPOCZYNAM HANDLEFISHINGSUCCESS =====");
        
        // Pobierz bonusy z wędki
        ItemStack rod = player.getInventory().getItemInMainHand();
        double rareFishChance = plugin.getFishingRod().getBonus(rod, "rare_fish_chance");
        double doubleCatchChance = plugin.getFishingRod().getBonus(rod, "double_catch_chance");
        String rodType = plugin.getFishingRod().getRodType(rod);
        plugin.getLogger().info("DEBUG: Gracz " + player.getName() + " próbuje złowić rybę z bonusem rzadkości: " + rareFishChance);
        plugin.getLogger().info("DEBUG: Szansa na podwójne złowienie: " + doubleCatchChance);
        
        // Zmniejsz wytrzymałość wędki
        if (plugin.getFishingRod().isFishingRod(rod)) {
            plugin.getFishingRod().damageFishingRod(rod);
            
            // Sprawdź czy wędka się złamała
            if (plugin.getFishingRod().getDurability(rod) <= 0) {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                MessageUtils.sendMessage(player, "rod.broken");
                return;
            }
        }
        
        // Sprawdź, czy gracz powinien otrzymać żyłkę
        if (rodType != null && pl.stylowamc.smfishingplanet.models.FishingLine.shouldDropLine(rodType)) {
            plugin.getLogger().info("DEBUG: Gracz " + player.getName() + " złowił żyłkę dla wędki typu: " + rodType);
            
            // Utwórz odpowiednią żyłkę
            pl.stylowamc.smfishingplanet.models.FishingLine fishingLine = 
                pl.stylowamc.smfishingplanet.models.FishingLine.getLineForRodType(rodType);
            ItemStack lineItem = fishingLine.createItemStack();
            
            // Dodaj żyłkę do ekwipunku gracza
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(lineItem);
                player.sendMessage(MessageUtils.colorize("&a&lGratulacje! &7Znalazłeś &b" + fishingLine.getName() + "&7!"));
            } else {
                // Jeśli ekwipunek jest pełny, upuść przedmiot na ziemię
                player.getWorld().dropItemNaturally(player.getLocation(), lineItem);
                MessageUtils.sendMessage(player, "inventory_full");
            }
            
            return;
        }
        
        // Pobierz szansę na złowienie śmieci
        double trashChance = pl.stylowamc.smfishingplanet.models.Trash.getTrashChance();
        
        // Losowa szansa na złowienie śmieci
        if (Math.random() * 100 < trashChance) {
            plugin.getLogger().info("DEBUG: Gracz " + player.getName() + " złowił śmieć");
            
            // Utwórz losowy śmieć
            pl.stylowamc.smfishingplanet.models.Trash trash = pl.stylowamc.smfishingplanet.models.Trash.getRandomTrash();
            ItemStack trashItem = trash.createItemStack();
            
            // Dodaj śmieć do ekwipunku gracza
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(trashItem);
                player.sendMessage(MessageUtils.colorize("&7Złowiłeś śmieć: &f" + trash.getName()));
            } else {
                // Jeśli ekwipunek jest pełny, upuść przedmiot na ziemię
                player.getWorld().dropItemNaturally(player.getLocation(), trashItem);
                MessageUtils.sendMessage(player, "inventory_full");
            }
            
            // Dodaj efekty
            playFishingTrashEffects(player, hookLocation);
            
            // Zarejestruj w statystykach złowienie śmieci
            plugin.getPlayerDataManager().registerStatTrashCaught(player);
        } else {
            // Spróbuj złowić rybę
            Fish fish = plugin.getFishManager().getRandomFish(player);
            if (fish == null) {
                plugin.getLogger().warning("Nie można wylosować ryby dla gracza: " + player.getName());
                handleFishingFail(player);
                plugin.getPlayerDataManager().registerStatFailedCatch(player);
                return;
            }
            
            // Zarejestruj złowienie ryby w statystykach
            plugin.getPlayerDataManager().registerStatCatch(player, fish);
            
            // Złów drugą rybę (jeśli mamy szczęście)
            if (Math.random() < doubleCatchChance) {  // Używamy szansy z wędki zamiast stałej wartości
                plugin.getLogger().info("DEBUG: Gracz " + player.getName() + " ma szczęście i łowi drugą rybę! (Szansa: " + doubleCatchChance + ")");
                Fish secondFish = plugin.getFishManager().getRandomFish(player);
                if (secondFish != null) {
                    plugin.getFishManager().addFish(player, secondFish);
                    plugin.getPlayerDataManager().registerStatCatch(player, secondFish);
                }
            }
            
            // Dodaj złowioną rybę
            plugin.getFishManager().addFish(player, fish);
            
            // Dodaj efekty
            playFishingSuccessEffects(player, hookLocation);
        }
    }

    private void handleFishingFail(Player player) {
        // Usuń bossbar i runnable
        if (activeBossBars.containsKey(player)) {
            activeBossBars.get(player).removeAll();
            activeBossBars.remove(player);
        }
        if (activeRunnables.containsKey(player)) {
            activeRunnables.get(player).cancel();
            activeRunnables.remove(player);
        }

        // Wyślij komunikat "Za późno"
        player.sendTitle(
            MessageUtils.colorize("&c&lZa późno!"),
            MessageUtils.colorize("&7Spróbuj ponownie..."),
            5, 40, 5
        );
        
        // Efekty dla nieudanego łowienia
        Location loc = player.getLocation();
        World world = loc.getWorld();
        
        // Efekty cząsteczkowe
        world.spawnParticle(Particle.SMOKE_NORMAL, loc, 30, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.WATER_SPLASH, loc, 50, 0.5, 0.5, 0.5, 0.1);
        
        // Dźwięki
        world.playSound(loc, Sound.valueOf(plugin.getConfigManager().getFailSound()), 1.0f, 1.0f);
        world.playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        
        // Zmniejsz wytrzymałość wędki
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (plugin.getFishingRod().isFishingRod(rod)) {
            plugin.getFishingRod().damageFishingRod(rod);
            int durability = plugin.getFishingRod().getDurability(rod);
            if (durability <= 0) {
                player.getInventory().setItemInMainHand(null);
                MessageUtils.sendMessage(player, "rod.broken");
            }
        }
        
        // Wyciągnij przynętę
        player.getWorld().getEntities().stream()
            .filter(entity -> entity.getType() == EntityType.FISHING_HOOK)
            .filter(entity -> entity.getCustomName() != null && entity.getCustomName().equals(player.getName()))
            .forEach(entity -> entity.remove());
            
        // Zarejestruj nieudane łowienie w statystykach
        plugin.getPlayerDataManager().registerStatFailedCatch(player);
        
        // Wyślij wiadomość o porażce
        MessageUtils.sendMessage(player, "fishing.fail");
    }

    private void cleanupFishingState(Player player) {
        // Usuń bossbar jeśli istnieje
        if (activeBossBars.containsKey(player)) {
            BossBar bossBar = activeBossBars.get(player);
            bossBar.removeAll();
            activeBossBars.remove(player);
        }

        // Anuluj runnable jeśli istnieje
        if (activeRunnables.containsKey(player)) {
            activeRunnables.get(player).cancel();
            activeRunnables.remove(player);
        }
    }

    private void playFishingSuccessEffects(Player player, Location location) {
        // Efekty dla pomyślnego złowienia
        World world = location.getWorld();
        
        // Fajerwerka
        Firework firework = (Firework) world.spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        
        // Konfiguracja fajerwerki
        FireworkEffect effect = FireworkEffect.builder()
            .withColor(Color.AQUA, Color.BLUE)
            .with(FireworkEffect.Type.BALL_LARGE)
            .trail(true)
            .build();
        
        meta.addEffect(effect);
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        
        // Dodatkowe efekty
        world.spawnParticle(Particle.WATER_SPLASH, location, 50, 0.5, 0.5, 0.5, 0.1);
        world.playSound(location, Sound.ENTITY_PLAYER_SPLASH, 1.0f, 1.0f);
        
        // Wyślij wiadomość o sukcesie na środku ekranu
        player.sendTitle(
            MessageUtils.colorize("&a&l✔ ZŁOWIONO! &a&l✔"),
            MessageUtils.colorize("&7Sprawdź swój ekwipunek!"),
            10, 40, 20
        );
    }

    private void playFishingTrashEffects(Player player, Location location) {
        // Efekty dla złowienia śmieci
        World world = location.getWorld();
        
        // Efekty cząsteczkowe
        world.spawnParticle(Particle.WATER_SPLASH, location, 30, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.SMOKE_NORMAL, location, 10, 0.3, 0.3, 0.3, 0.05);
        
        // Dźwięki
        world.playSound(location, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.8f, 0.8f);
        world.playSound(location, Sound.BLOCK_WET_GRASS_STEP, 1.0f, 0.5f);
        
        // Wyświetl wiadomość na środku ekranu
        player.sendTitle(
            MessageUtils.colorize("&7&lŚmieć!"),
            MessageUtils.colorize("&8Udało ci się złowić jakiś śmieć..."),
            10, 40, 20
        );
    }
} 
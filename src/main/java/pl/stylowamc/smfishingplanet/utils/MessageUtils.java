package pl.stylowamc.smfishingplanet.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.stylowamc.smfishingplanet.SMFishingPlanet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageUtils {
    private static final SMFishingPlanet plugin = SMFishingPlanet.getInstance();
    private static FileConfiguration messages;
    private static final Map<String, String> messageCache = new HashMap<>();

    public static void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        messageCache.clear();
    }

    public static String getMessage(String path) {
        return messageCache.computeIfAbsent(path, key -> {
            String message = messages.getString(key);
            return message != null ? colorize(message) : "§cBrak wiadomości dla klucza: " + key;
        });
    }

    public static void sendMessage(Player player, String path) {
        player.sendMessage(getMessage("prefix") + getMessage(path));
    }

    public static void sendMessage(Player player, String path, Map<String, String> placeholders) {
        String message = getMessage("prefix") + getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        player.sendMessage(message);
    }

    public static void sendMessage(Player player, String key, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        sendMessage(player, key, placeholders);
    }

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String getPrefix() {
        return getMessage("prefix");
    }

    public static void reloadMessages() {
        loadMessages();
    }

    public static String stripColor(String text) {
        return ChatColor.stripColor(text);
    }

    public static void sendMessage(CommandSender sender, String key) {
        String message = messages.getString(key);
        if (message == null) {
            sender.sendMessage(colorize("&cBrak wiadomości o kluczu: " + key));
            return;
        }
        sender.sendMessage(colorize(message));
    }
} 
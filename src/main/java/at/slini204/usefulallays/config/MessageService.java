package at.slini204.usefulallays.config;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class MessageService {

    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);

        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                loaded.setDefaults(defaults);
                loaded.options().copyDefaults(true);
                loaded.save(file);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not update messages.yml defaults: " + exception.getMessage());
        }

        messages = loaded;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = messages.getString("prefix", "");
        String raw = messages.getString(path, path);
        sender.sendMessage(color(apply(prefix + raw, placeholders)));
    }

    public String raw(String path, Map<String, String> placeholders) {
        return color(apply(messages.getString(path, path), placeholders));
    }

    public static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    private String apply(String input, Map<String, String> placeholders) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}

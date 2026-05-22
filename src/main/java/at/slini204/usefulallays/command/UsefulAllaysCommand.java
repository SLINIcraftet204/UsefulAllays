package at.slini204.usefulallays.command;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UsefulAllaysCommand implements CommandExecutor, TabCompleter {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;

    public UsefulAllaysCommand(UsefulAllaysPlugin plugin, AllayRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§bUsefulAllays §8- §7Make Allays more useful and more like pets.");
            sender.sendMessage("§7Use §e/" + label + " list §7or §e/" + label + " reload§7.");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("usefulallays.admin.reload")) {
                    plugin.messages().send(sender, "plugin.noPermission");
                    return true;
                }
                plugin.reloadPlugin();
                plugin.messages().send(sender, "plugin.reloaded");
                return true;
            }
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                int count = repository.findLoadedOwnedAllays(player.getUniqueId()).size();
                plugin.messages().send(player, "allay.list", Map.of("count", String.valueOf(count)));
                return true;
            }
            default -> {
                plugin.messages().send(sender, "plugin.unknownCommand");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>();
        if ("list".startsWith(args[0].toLowerCase())) {
            suggestions.add("list");
        }
        if (sender.hasPermission("usefulallays.admin.reload") && "reload".startsWith(args[0].toLowerCase())) {
            suggestions.add("reload");
        }
        return suggestions;
    }
}

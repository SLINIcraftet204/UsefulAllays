package at.slini204.usefulallays.command;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.service.AllayDisplayService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class UsefulAllaysCommand implements CommandExecutor, TabCompleter {

    private static final double RENAME_RADIUS = 6.0;
    private static final int MAX_CUSTOM_NAME_LENGTH = 32;

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayDisplayService displayService;

    public UsefulAllaysCommand(UsefulAllaysPlugin plugin, AllayRepository repository, AllayDisplayService displayService) {
        this.plugin = plugin;
        this.repository = repository;
        this.displayService = displayService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§bUsefulAllays §8- §7Make Allays more useful and more like pets.");
            sender.sendMessage("§7Use §e/" + label + " list§7, §e/" + label + " rename <name>§7 or §e/" + label + " reload§7.");
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
            case "rename" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                if (!player.hasPermission("usefulallays.rename")) {
                    plugin.messages().send(player, "plugin.noPermission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.messages().send(player, "rename.usage", Map.of("label", label));
                    return true;
                }

                Optional<Allay> nearest = nearestOwnedAllay(player);
                if (nearest.isEmpty()) {
                    plugin.messages().send(player, "rename.noOwnedAllayNearby");
                    return true;
                }

                Allay allay = nearest.get();
                String customName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
                if (customName.equalsIgnoreCase("reset") || customName.equalsIgnoreCase("clear")) {
                    repository.setCustomName(allay, null);
                    displayService.applyDisplayName(player, allay);
                    plugin.messages().send(player, "rename.reset");
                    return true;
                }

                if (customName.length() > MAX_CUSTOM_NAME_LENGTH) {
                    plugin.messages().send(player, "rename.tooLong", Map.of("max", String.valueOf(MAX_CUSTOM_NAME_LENGTH)));
                    return true;
                }

                repository.setCustomName(allay, customName);
                displayService.applyDisplayName(player, allay);
                plugin.messages().send(player, "rename.success", Map.of("name", customName));
                return true;
            }
            default -> {
                plugin.messages().send(sender, "plugin.unknownCommand");
                return true;
            }
        }
    }

    private Optional<Allay> nearestOwnedAllay(Player player) {
        return player.getNearbyEntities(RENAME_RADIUS, RENAME_RADIUS, RENAME_RADIUS).stream()
                .filter(entity -> entity instanceof Allay)
                .map(entity -> (Allay) entity)
                .filter(repository::isClaimed)
                .filter(allay -> repository.ownerOf(allay).filter(player.getUniqueId()::equals).isPresent())
                .min(Comparator.comparingDouble(allay -> allay.getLocation().distanceSquared(player.getLocation())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String input = args[0].toLowerCase();
            if ("list".startsWith(input)) {
                suggestions.add("list");
            }
            if (sender.hasPermission("usefulallays.rename") && "rename".startsWith(input)) {
                suggestions.add("rename");
            }
            if (sender.hasPermission("usefulallays.admin.reload") && "reload".startsWith(input)) {
                suggestions.add("reload");
            }
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("rename")) {
            String input = args[1].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            if ("reset".startsWith(input)) {
                suggestions.add("reset");
            }
            return suggestions;
        }

        return List.of();
    }
}

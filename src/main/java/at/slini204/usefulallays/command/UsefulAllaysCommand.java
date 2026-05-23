package at.slini204.usefulallays.command;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.service.AllayDisplayService;
import at.slini204.usefulallays.service.AllayHomeService;
import at.slini204.usefulallays.service.AllaySnapshotService;
import at.slini204.usefulallays.util.LocationCodec;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class UsefulAllaysCommand implements CommandExecutor, TabCompleter {

    private static final double NEARBY_RADIUS = 8.0;
    private static final int MAX_CUSTOM_NAME_LENGTH = 32;

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayDisplayService displayService;
    private final AllayHomeService homeService;
    private final AllaySnapshotService snapshotService;

    public UsefulAllaysCommand(UsefulAllaysPlugin plugin,
                               AllayRepository repository,
                               AllayDisplayService displayService,
                               AllayHomeService homeService,
                               AllaySnapshotService snapshotService) {
        this.plugin = plugin;
        this.repository = repository;
        this.displayService = displayService;
        this.homeService = homeService;
        this.snapshotService = snapshotService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§bUsefulAllays §8- §7Make Allays more useful and more like pets.");
            sender.sendMessage("§7Use §e/" + label + " list§7, §e/" + label + " info§7, §e/" + label + " sethome§7, §e/" + label + " rename <name>§7 or §e/" + label + " reload§7.");
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
            case "export" -> {
                if (!sender.hasPermission("usefulallays.admin.export")) {
                    plugin.messages().send(sender, "plugin.noPermission");
                    return true;
                }
                try {
                    AllaySnapshotService.SnapshotResult result = snapshotService.writeNow();
                    plugin.messages().send(sender, "storage.exported", Map.of(
                            "count", String.valueOf(result.allayCount()),
                            "file", result.file().getPath()
                    ));
                } catch (IOException exception) {
                    sender.sendMessage("§cUsefulAllays could not export the readable Allay mirror: §e" + exception.getMessage());
                }
                return true;
            }
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                List<Allay> allays = repository.findLoadedOwnedAllays(player.getUniqueId());
                plugin.messages().send(player, "allay.list", Map.of("count", String.valueOf(allays.size())));
                for (Allay allay : allays) {
                    player.sendMessage("§8- §e" + allay.getUniqueId().toString().substring(0, 8)
                            + " §7Lv. §e" + repository.levelOf(allay)
                            + " §7Mode: §e" + repository.modeOf(allay).name()
                            + " §7Loc: §e" + LocationCodec.readable(allay.getLocation()));
                }
                return true;
            }
            case "info" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                Optional<Allay> nearest = nearestOwnedAllay(player);
                if (nearest.isEmpty()) {
                    plugin.messages().send(player, "allay.noOwnedAllayNearby");
                    return true;
                }

                sendAllayInfo(player, nearest.get());
                return true;
            }
            case "sethome" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                if (!player.hasPermission("usefulallays.sethome")) {
                    plugin.messages().send(player, "plugin.noPermission");
                    return true;
                }

                Optional<Allay> nearest = nearestOwnedAllay(player);
                if (nearest.isEmpty()) {
                    plugin.messages().send(player, "allay.noOwnedAllayNearby");
                    return true;
                }

                Location home = homeService.setManualHome(player, nearest.get()).orElse(player.getLocation());
                plugin.messages().send(player, "allay.homeSaved", Map.of("location", LocationCodec.readable(home)));
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

    private void sendAllayInfo(Player player, Allay allay) {
        List<Material> filters = repository.filtersOf(allay);
        String filterText = filters.isEmpty()
                ? "-"
                : filters.stream().map(Material::name).collect(Collectors.joining(", "));

        player.sendMessage("§bUsefulAllays §8» §7Allay Info");
        player.sendMessage("§7UUID: §e" + allay.getUniqueId());
        player.sendMessage("§7Owner: §e" + repository.ownerNameOf(allay).orElse("-") + " §8(" + repository.ownerOf(allay).map(Object::toString).orElse("-") + "§8)");
        player.sendMessage("§7Custom name: §e" + repository.customNameOf(allay).orElse("-"));
        player.sendMessage("§7Level: §e" + repository.levelOf(allay));
        player.sendMessage("§7Mode: §e" + repository.modeOf(allay).name());
        player.sendMessage("§7Location: §e" + LocationCodec.readable(allay.getLocation()));
        player.sendMessage("§7Home: §e" + repository.homeLocationOf(allay).map(LocationCodec::readable).orElse("-"));
        player.sendMessage("§7Filters: §e" + filterText);
    }

    private Optional<Allay> nearestOwnedAllay(Player player) {
        return player.getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS).stream()
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
            addIfStartsWith(suggestions, "list", input);
            addIfStartsWith(suggestions, "info", input);
            if (sender.hasPermission("usefulallays.sethome")) {
                addIfStartsWith(suggestions, "sethome", input);
            }
            if (sender.hasPermission("usefulallays.rename")) {
                addIfStartsWith(suggestions, "rename", input);
            }
            if (sender.hasPermission("usefulallays.admin.reload")) {
                addIfStartsWith(suggestions, "reload", input);
            }
            if (sender.hasPermission("usefulallays.admin.export")) {
                addIfStartsWith(suggestions, "export", input);
            }
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("rename")) {
            String input = args[1].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            addIfStartsWith(suggestions, "reset", input);
            return suggestions;
        }

        return List.of();
    }

    private void addIfStartsWith(List<String> suggestions, String value, String input) {
        if (value.startsWith(input)) {
            suggestions.add(value);
        }
    }
}

package at.slini204.usefulallays.command;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.config.MessageService;
import at.slini204.usefulallays.gui.AllayGui;
import at.slini204.usefulallays.model.AllayMode;
import at.slini204.usefulallays.service.AllayDisplayService;
import at.slini204.usefulallays.service.AllayFollowService;
import at.slini204.usefulallays.service.AllayHomeService;
import at.slini204.usefulallays.service.AllaySnapshotService;
import at.slini204.usefulallays.util.LocationCodec;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class UsefulAllaysCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_CUSTOM_NAME_LENGTH = 32;

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayDisplayService displayService;
    private final AllayHomeService homeService;
    private final AllayFollowService followService;
    private final AllaySnapshotService snapshotService;
    private final AllayGui allayGui;

    public UsefulAllaysCommand(UsefulAllaysPlugin plugin,
                               AllayRepository repository,
                               AllayDisplayService displayService,
                               AllayHomeService homeService,
                               AllayFollowService followService,
                               AllaySnapshotService snapshotService,
                               AllayGui allayGui) {
        this.plugin = plugin;
        this.repository = repository;
        this.displayService = displayService;
        this.homeService = homeService;
        this.followService = followService;
        this.snapshotService = snapshotService;
        this.allayGui = allayGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
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
                sendAllayList(player, label);
                return true;
            }
            case "info" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                Optional<Allay> selected = selectOwnedAllay(player, selectorFromArgs(args, 1));
                if (selected.isEmpty()) {
                    plugin.messages().send(player, "allay.noOwnedAllayLoaded");
                    return true;
                }
                sendAllayInfo(player, selected.get());
                return true;
            }
            case "menu", "gui" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                if (!player.hasPermission("usefulallays.gui")) {
                    plugin.messages().send(player, "plugin.noPermission");
                    return true;
                }
                Optional<Allay> selected = selectOwnedAllay(player, selectorFromArgs(args, 1));
                if (selected.isEmpty()) {
                    plugin.messages().send(player, "allay.noOwnedAllayLoaded");
                    return true;
                }
                allayGui.open(player, selected.get());
                return true;
            }
            case "call", "recall", "ruf", "bring" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                if (!player.hasPermission("usefulallays.call")) {
                    plugin.messages().send(player, "plugin.noPermission");
                    return true;
                }
                handleCallCommand(player, args);
                return true;
            }
            case "sendhome", "gohome" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                if (!player.hasPermission("usefulallays.sethome")) {
                    plugin.messages().send(player, "plugin.noPermission");
                    return true;
                }
                handleSendHomeCommand(player, args);
                return true;
            }
            case "mode" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                if (!player.hasPermission("usefulallays.gui")) {
                    plugin.messages().send(player, "plugin.noPermission");
                    return true;
                }
                if (args.length < 2) {
                    plugin.messages().send(player, "mode.usage", Map.of("label", label));
                    return true;
                }
                Optional<AllayMode> mode = parseMode(args[1]);
                if (mode.isEmpty()) {
                    plugin.messages().send(player, "mode.invalid");
                    return true;
                }
                Optional<Allay> selected = selectOwnedAllay(player, selectorFromArgs(args, 2));
                if (selected.isEmpty()) {
                    plugin.messages().send(player, "allay.noOwnedAllayLoaded");
                    return true;
                }
                Allay allay = selected.get();
                repository.setMode(allay, mode.get());
                Optional<Location> home = homeService.prepareHomeForMode(player, allay, mode.get());
                plugin.messages().send(player, "allay.modeChanged", Map.of("mode", mode.get().name()));
                if (homeService.usesHome(mode.get()) && home.isPresent()) {
                    plugin.messages().send(player, "allay.homeSaved", Map.of("location", LocationCodec.readable(home.get())));
                }
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

                Optional<Allay> selected = selectOwnedAllay(player, selectorFromArgs(args, 1));
                if (selected.isEmpty()) {
                    plugin.messages().send(player, "allay.noOwnedAllayLoaded");
                    return true;
                }

                Location home = homeService.setManualHome(player, selected.get()).orElse(player.getLocation());
                plugin.messages().send(player, "allay.homeSaved", Map.of("location", LocationCodec.readable(home)));
                return true;
            }
            case "rename", "nick", "nickname" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "plugin.playerOnly");
                    return true;
                }
                if (!player.hasPermission("usefulallays.rename")) {
                    plugin.messages().send(player, "plugin.noPermission");
                    return true;
                }
                handleRenameCommand(player, label, args);
                return true;
            }
            default -> {
                plugin.messages().send(sender, "plugin.unknownCommand");
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§bUsefulAllays §8- §7Make Allays more useful and more like pets.");
        sender.sendMessage("§7Use §e/" + label + " list §8- §7shows loaded owned Allays with numbers.");
        sender.sendMessage("§7Use §e/" + label + " menu [number/id/nickname] §8- §7opens the GUI without looking at the Allay.");
        sender.sendMessage("§7Use §e/" + label + " call [all|number/id/nickname] §8- §7calls Allays to you.");
        sender.sendMessage("§7Use §e/" + label + " mode <mode> [number/id/nickname] §8- §7changes mode by command.");
        sender.sendMessage("§7Use §e/" + label + " sethome [number/id/nickname] §8- §7sets home for an Allay.");
        sender.sendMessage("§7Use §e/" + label + " rename [number/id/current-nickname] <nickname> §8- §7sets the Allay nickname.");
    }

    private void sendAllayList(Player player, String label) {
        List<Allay> allays = sortedOwnedAllays(player);
        plugin.messages().send(player, "allay.list", Map.of("count", String.valueOf(allays.size())));
        if (allays.isEmpty()) {
            player.sendMessage("§8- §7No loaded owned Allays found.");
            return;
        }

        for (int i = 0; i < allays.size(); i++) {
            Allay allay = allays.get(i);
            String displayName = displayNameForCommands(allay, player.getName());
            String distance = readableDistance(player, allay);
            player.sendMessage("§8" + (i + 1) + ". §e" + displayName
                    + " §8(" + allay.getUniqueId().toString().substring(0, 8) + ")"
                    + " §7Lv. §e" + repository.levelOf(allay)
                    + " §7Mode: §e" + repository.modeOf(allay).name()
                    + " §7Dist: §e" + distance);
        }
        player.sendMessage("§8Tip: §7Use §e/" + label + " menu 1§7, §e/" + label + " call Elly§7 or §e/" + label + " mode follow Elly§7.");
    }

    private void handleCallCommand(Player player, String[] args) {
        if (!plugin.settings().recallEnabled()) {
            plugin.messages().send(player, "allay.callDisabled");
            return;
        }

        if (args.length < 2) {
            boolean includeHomeModes = plugin.settings().recallIncludeHomeModesByDefault();
            AllayFollowService.RecallResult result = followService.recallLoadedAllaysToPlayer(player, includeHomeModes);
            plugin.messages().send(player, "allay.called", Map.of(
                    "count", String.valueOf(result.moved()),
                    "skipped", String.valueOf(result.skippedMode() + result.skippedWorld())
            ));
            return;
        }

        String selector = selectorFromArgs(args, 1);
        if (selector.equalsIgnoreCase("all")) {
            AllayFollowService.RecallResult result = followService.recallLoadedAllaysToPlayer(player, true);
            plugin.messages().send(player, "allay.called", Map.of(
                    "count", String.valueOf(result.moved()),
                    "skipped", String.valueOf(result.skippedMode() + result.skippedWorld())
            ));
            return;
        }

        Optional<Allay> selected = selectOwnedAllay(player, selector);
        if (selected.isEmpty()) {
            plugin.messages().send(player, "allay.noOwnedAllayLoaded");
            return;
        }

        AllayFollowService.SingleRecallResult result = followService.recallLoadedAllayToPlayer(player, selected.get(), true);
        if (result == AllayFollowService.SingleRecallResult.MOVED) {
            plugin.messages().send(player, "allay.calledOne");
        } else {
            plugin.messages().send(player, "allay.callFailed");
        }
    }

    private void handleSendHomeCommand(Player player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
            int moved = followService.sendLoadedAllaysHome(player);
            plugin.messages().send(player, "allay.sentHomeCount", Map.of("count", String.valueOf(moved)));
            return;
        }

        Optional<Allay> selected = selectOwnedAllay(player, selectorFromArgs(args, 1));
        if (selected.isEmpty()) {
            plugin.messages().send(player, "allay.noOwnedAllayLoaded");
            return;
        }

        if (followService.sendLoadedAllayHome(player, selected.get())) {
            plugin.messages().send(player, "allay.sentHome");
        } else {
            plugin.messages().send(player, "allay.noHome");
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
        player.sendMessage("§7Nickname: §e" + repository.customNameOf(allay).orElse("-"));
        player.sendMessage("§7Level: §e" + repository.levelOf(allay));
        player.sendMessage("§7Mode: §e" + repository.modeOf(allay).name());
        player.sendMessage("§7Location: §e" + LocationCodec.readable(allay.getLocation()));
        player.sendMessage("§7Home: §e" + repository.homeLocationOf(allay).map(LocationCodec::readable).orElse("-"));
        player.sendMessage("§7Filters: §e" + filterText);
    }

    private void handleRenameCommand(Player player, String label, String[] args) {
        if (args.length < 2) {
            plugin.messages().send(player, "rename.usage", Map.of("label", label));
            return;
        }

        Optional<Allay> target;
        String nickname;

        if (args.length >= 3) {
            Optional<Allay> selectedByFirstArgument = selectOwnedAllay(player, args[1]);
            if (selectedByFirstArgument.isPresent()) {
                target = selectedByFirstArgument;
                nickname = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
            } else {
                target = selectOwnedAllay(player, "nearest");
                nickname = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
            }
        } else {
            target = selectOwnedAllay(player, "nearest");
            nickname = args[1].trim();
        }

        if (target.isEmpty()) {
            plugin.messages().send(player, "rename.noOwnedAllayNearby");
            return;
        }

        Allay allay = target.get();
        if (nickname.equalsIgnoreCase("reset") || nickname.equalsIgnoreCase("clear")) {
            repository.setCustomName(allay, null);
            displayService.applyDisplayName(player, allay);
            plugin.messages().send(player, "rename.reset");
            return;
        }

        if (nickname.isBlank()) {
            plugin.messages().send(player, "rename.usage", Map.of("label", label));
            return;
        }

        if (nickname.length() > MAX_CUSTOM_NAME_LENGTH) {
            plugin.messages().send(player, "rename.tooLong", Map.of("max", String.valueOf(MAX_CUSTOM_NAME_LENGTH)));
            return;
        }

        repository.setCustomName(allay, nickname);
        displayService.applyDisplayName(player, allay);
        plugin.messages().send(player, "rename.success", Map.of("name", nickname));
    }

    private Optional<Allay> selectOwnedAllay(Player player, String selector) {
        List<Allay> allays = sortedOwnedAllays(player);
        if (allays.isEmpty()) {
            return Optional.empty();
        }

        if (selector == null || selector.isBlank() || selector.equalsIgnoreCase("nearest")) {
            return Optional.of(allays.get(0));
        }

        String raw = selector.trim();
        String normalized = normalizeSelector(raw);
        try {
            int index = Integer.parseInt(raw);
            if (index >= 1 && index <= allays.size()) {
                return Optional.of(allays.get(index - 1));
            }
        } catch (NumberFormatException ignored) {
            // Not a list number.
        }

        for (Allay allay : allays) {
            UUID uuid = allay.getUniqueId();
            if (uuid.toString().equalsIgnoreCase(raw) || uuid.toString().startsWith(raw.toLowerCase(Locale.ROOT))) {
                return Optional.of(allay);
            }
        }

        List<Allay> exactNicknameMatches = new ArrayList<>();
        List<Allay> prefixNicknameMatches = new ArrayList<>();
        for (Allay allay : allays) {
            Optional<String> nickname = repository.customNameOf(allay);
            if (nickname.isEmpty()) {
                continue;
            }

            String normalizedNickname = normalizeSelector(nickname.get());
            if (normalizedNickname.equals(normalized)) {
                exactNicknameMatches.add(allay);
            } else if (normalizedNickname.startsWith(normalized)) {
                prefixNicknameMatches.add(allay);
            }
        }

        if (!exactNicknameMatches.isEmpty()) {
            return Optional.of(exactNicknameMatches.get(0));
        }
        if (!prefixNicknameMatches.isEmpty()) {
            return Optional.of(prefixNicknameMatches.get(0));
        }

        return Optional.empty();
    }

    private String selectorFromArgs(String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return "nearest";
        }
        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length)).trim();
    }

    private String displayNameForCommands(Allay allay, String fallbackOwnerName) {
        return repository.customNameOf(allay)
                .orElseGet(() -> plugin.settings().defaultAllayName()
                        .replace("{owner}", repository.ownerNameOf(allay).orElse(fallbackOwnerName)));
    }

    private String normalizeSelector(String value) {
        String colored = MessageService.color(value == null ? "" : value);
        String stripped = ChatColor.stripColor(colored);
        return (stripped == null ? "" : stripped)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private List<Allay> sortedOwnedAllays(Player player) {
        List<Allay> allays = new ArrayList<>(repository.findLoadedOwnedAllays(player.getUniqueId()));
        allays.sort(Comparator
                .comparing((Allay allay) -> !allay.getWorld().equals(player.getWorld()))
                .thenComparingDouble(allay -> distanceSquaredSafe(player, allay))
                .thenComparing(allay -> allay.getUniqueId().toString()));
        return allays;
    }

    private double distanceSquaredSafe(Player player, Allay allay) {
        if (!allay.getWorld().equals(player.getWorld())) {
            return Double.MAX_VALUE;
        }
        return allay.getLocation().distanceSquared(player.getLocation());
    }

    private String readableDistance(Player player, Allay allay) {
        World playerWorld = player.getWorld();
        World allayWorld = allay.getWorld();
        if (!playerWorld.equals(allayWorld)) {
            return allayWorld.getName();
        }
        return Math.round(Math.sqrt(allay.getLocation().distanceSquared(player.getLocation()))) + "m";
    }

    private Optional<AllayMode> parseMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        for (AllayMode mode : AllayMode.values()) {
            if (mode.name().equals(value)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            String input = args[0].toLowerCase(Locale.ROOT);
            addIfStartsWith(suggestions, "list", input);
            addIfStartsWith(suggestions, "info", input);
            addIfStartsWith(suggestions, "menu", input);
            addIfStartsWith(suggestions, "call", input);
            addIfStartsWith(suggestions, "mode", input);
            if (sender.hasPermission("usefulallays.sethome")) {
                addIfStartsWith(suggestions, "sethome", input);
                addIfStartsWith(suggestions, "sendhome", input);
            }
            if (sender.hasPermission("usefulallays.rename")) {
                addIfStartsWith(suggestions, "rename", input);
                addIfStartsWith(suggestions, "nick", input);
                addIfStartsWith(suggestions, "nickname", input);
            }
            if (sender.hasPermission("usefulallays.admin.reload")) {
                addIfStartsWith(suggestions, "reload", input);
            }
            if (sender.hasPermission("usefulallays.admin.export")) {
                addIfStartsWith(suggestions, "export", input);
            }
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            String input = args[1].toUpperCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            for (AllayMode mode : AllayMode.values()) {
                addIfStartsWith(suggestions, mode.name().toLowerCase(Locale.ROOT), input.toLowerCase(Locale.ROOT));
            }
            return suggestions;
        }

        if ((args.length == 2 && List.of("info", "menu", "gui", "sethome", "sendhome", "call", "recall", "ruf", "bring").contains(args[0].toLowerCase(Locale.ROOT)))
                || (args.length == 3 && args[0].equalsIgnoreCase("mode"))) {
            if (!(sender instanceof Player player)) {
                return List.of();
            }
            String input = args[args.length - 1].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            addIfStartsWith(suggestions, "nearest", input);
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("call") || sub.equals("recall") || sub.equals("ruf") || sub.equals("bring") || sub.equals("sendhome")) {
                addIfStartsWith(suggestions, "all", input);
            }
            List<Allay> allays = sortedOwnedAllays(player);
            for (int i = 0; i < allays.size(); i++) {
                addIfStartsWith(suggestions, String.valueOf(i + 1), input);
                addIfStartsWith(suggestions, allays.get(i).getUniqueId().toString().substring(0, 8), input);
                repository.customNameOf(allays.get(i)).ifPresent(name -> addIfStartsWith(suggestions, name, input));
            }
            return suggestions;
        }

        if (args.length == 2 && List.of("rename", "nick", "nickname").contains(args[0].toLowerCase(Locale.ROOT))) {
            String input = args[1].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            addIfStartsWith(suggestions, "reset", input);
            if (sender instanceof Player player) {
                List<Allay> allays = sortedOwnedAllays(player);
                for (int i = 0; i < allays.size(); i++) {
                    addIfStartsWith(suggestions, String.valueOf(i + 1), input);
                    addIfStartsWith(suggestions, allays.get(i).getUniqueId().toString().substring(0, 8), input);
                    repository.customNameOf(allays.get(i)).ifPresent(name -> addIfStartsWith(suggestions, name, input));
                }
            }
            return suggestions;
        }

        if (args.length == 3 && List.of("rename", "nick", "nickname").contains(args[0].toLowerCase(Locale.ROOT))) {
            String input = args[2].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();
            addIfStartsWith(suggestions, "reset", input);
            return suggestions;
        }

        return List.of();
    }

    private void addIfStartsWith(List<String> suggestions, String value, String input) {
        if (value.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
            suggestions.add(value);
        }
    }
}

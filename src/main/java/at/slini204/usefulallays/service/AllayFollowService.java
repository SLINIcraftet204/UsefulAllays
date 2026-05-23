package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.config.PluginSettings;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.model.AllayMode;
import at.slini204.usefulallays.model.LevelSettings;
import org.bukkit.Location;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Optional;

public final class AllayFollowService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayHomeService homeService;
    private PluginSettings settings;
    private BukkitTask task;

    public AllayFollowService(UsefulAllaysPlugin plugin, AllayRepository repository, PluginSettings settings, AllayHomeService homeService) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
        this.homeService = homeService;
    }

    public void start() {
        stop();

        if (!settings.followEnabled()) {
            return;
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                teleportFarLoadedAllays(player);
            }
            keepHomeAllaysNearHome();
        }, settings.followIntervalTicks(), settings.followIntervalTicks());
    }

    public void reload(PluginSettings settings) {
        this.settings = settings;
        start();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void teleportFarLoadedAllays(Player player) {
        for (Allay allay : repository.findLoadedOwnedAllays(player.getUniqueId())) {
            if (!shouldFollowPlayer(allay)) {
                continue;
            }

            if (settings.isWorldDisabled(allay.getWorld().getName()) || settings.isWorldDisabled(player.getWorld().getName())) {
                continue;
            }

            int level = repository.levelOf(allay);
            LevelSettings levelSettings = settings.level(level);
            double configuredDistance = Math.max(settings.teleportAfterDistance(), levelSettings.teleportDistance());

            if (!allay.getWorld().equals(player.getWorld()) || allay.getLocation().distanceSquared(player.getLocation()) > configuredDistance * configuredDistance) {
                teleportNear(player.getLocation(), allay);
            }
        }
    }

    public void teleportLoadedAllaysToPlayer(Player player) {
        if (!settings.followEnabled()) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Allay allay : repository.findLoadedOwnedAllays(player.getUniqueId())) {
                if (shouldFollowPlayer(allay) && !settings.isWorldDisabled(player.getWorld().getName())) {
                    teleportNear(player.getLocation(), allay);
                }
            }
        });
    }

    public RecallResult recallLoadedAllaysToPlayer(Player player, boolean includeHomeModes) {
        if (!settings.recallEnabled()) {
            return new RecallResult(0, 0, 0);
        }

        int moved = 0;
        int skippedMode = 0;
        int skippedWorld = 0;

        List<Allay> allays = repository.findLoadedOwnedAllays(player.getUniqueId());
        for (Allay allay : allays) {
            SingleRecallResult result = recallLoadedAllayToPlayer(player, allay, includeHomeModes);
            switch (result) {
                case MOVED -> moved++;
                case SKIPPED_MODE -> skippedMode++;
                case SKIPPED_WORLD -> skippedWorld++;
                case INVALID -> {
                    // Ignore invalid entities in command feedback. They disappear on the next loaded scan anyway.
                }
            }
        }

        return new RecallResult(moved, skippedMode, skippedWorld);
    }

    public SingleRecallResult recallLoadedAllayToPlayer(Player player, Allay allay, boolean includeHomeModes) {
        if (!settings.recallEnabled()) {
            return SingleRecallResult.SKIPPED_MODE;
        }
        if (!isUsable(allay)) {
            return SingleRecallResult.INVALID;
        }
        if (settings.isWorldDisabled(player.getWorld().getName()) || settings.isWorldDisabled(allay.getWorld().getName())) {
            return SingleRecallResult.SKIPPED_WORLD;
        }
        if (!includeHomeModes && homeService.usesHome(repository.modeOf(allay))) {
            return SingleRecallResult.SKIPPED_MODE;
        }

        teleportNear(player.getLocation(), allay);
        return SingleRecallResult.MOVED;
    }

    public boolean sendLoadedAllayHome(Player player, Allay allay) {
        if (!isUsable(allay)) {
            return false;
        }

        Optional<Location> home = homeService.resolveHome(player, allay);
        if (home.isEmpty() || home.get().getWorld() == null || settings.isWorldDisabled(home.get().getWorld().getName())) {
            return false;
        }

        teleportNear(home.get(), allay);
        return true;
    }

    public int sendLoadedAllaysHome(Player player) {
        int moved = 0;
        for (Allay allay : repository.findLoadedOwnedAllays(player.getUniqueId())) {
            if (sendLoadedAllayHome(player, allay)) {
                moved++;
            }
        }
        return moved;
    }

    private void keepHomeAllaysNearHome() {
        double maxDistance = settings.homeReturnDistance();
        double maxDistanceSquared = maxDistance * maxDistance;

        for (Allay allay : repository.findLoadedClaimedAllays()) {
            if (!isUsable(allay) || !homeService.usesHome(repository.modeOf(allay))) {
                continue;
            }

            Optional<Location> home = homeService.resolveHome(allay);
            if (home.isEmpty() || home.get().getWorld() == null || settings.isWorldDisabled(home.get().getWorld().getName())) {
                continue;
            }

            Location homeLocation = home.get();
            if (!allay.getWorld().equals(homeLocation.getWorld()) || allay.getLocation().distanceSquared(homeLocation) > maxDistanceSquared) {
                teleportNear(homeLocation, allay);
            }
        }
    }

    private boolean shouldFollowPlayer(Allay allay) {
        if (!isUsable(allay)) {
            return false;
        }

        AllayMode mode = repository.modeOf(allay);
        return mode == AllayMode.FOLLOW || mode == AllayMode.COLLECT_AROUND_OWNER;
    }

    private boolean isUsable(Allay allay) {
        return allay.isValid() && !allay.isDead();
    }

    private void teleportNear(Location location, Allay allay) {
        Location destination = location.clone().add(1.0, 0.25, 1.0);
        allay.teleport(destination);
    }

    public record RecallResult(int moved, int skippedMode, int skippedWorld) {
    }

    public enum SingleRecallResult {
        MOVED,
        SKIPPED_MODE,
        SKIPPED_WORLD,
        INVALID
    }
}

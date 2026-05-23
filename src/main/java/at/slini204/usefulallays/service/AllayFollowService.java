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
}

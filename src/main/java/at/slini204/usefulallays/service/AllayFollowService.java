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

public final class AllayFollowService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private PluginSettings settings;
    private BukkitTask task;

    public AllayFollowService(UsefulAllaysPlugin plugin, AllayRepository repository, PluginSettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
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
            if (!shouldFollow(allay)) {
                continue;
            }

            if (settings.isWorldDisabled(allay.getWorld().getName()) || settings.isWorldDisabled(player.getWorld().getName())) {
                continue;
            }

            int level = repository.levelOf(allay);
            LevelSettings levelSettings = settings.level(level);
            double configuredDistance = Math.max(settings.teleportAfterDistance(), levelSettings.teleportDistance());

            if (!allay.getWorld().equals(player.getWorld()) || allay.getLocation().distanceSquared(player.getLocation()) > configuredDistance * configuredDistance) {
                teleportNear(player, allay);
            }
        }
    }

    public void teleportLoadedAllaysToPlayer(Player player) {
        if (!settings.followEnabled()) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Allay allay : repository.findLoadedOwnedAllays(player.getUniqueId())) {
                if (shouldFollow(allay) && !settings.isWorldDisabled(player.getWorld().getName())) {
                    teleportNear(player, allay);
                }
            }
        });
    }

    private boolean shouldFollow(Allay allay) {
        AllayMode mode = repository.modeOf(allay);
        return mode != AllayMode.STAY && mode != AllayMode.PASSIVE && allay.isValid() && !allay.isDead();
    }

    private void teleportNear(Player player, Allay allay) {
        Location destination = player.getLocation().clone().add(1.0, 0.25, 1.0);
        allay.teleport(destination);
    }
}

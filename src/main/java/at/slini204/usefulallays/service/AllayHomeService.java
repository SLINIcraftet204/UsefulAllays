package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.config.PluginSettings;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.model.AllayMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Optional;

public final class AllayHomeService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private PluginSettings settings;

    public AllayHomeService(UsefulAllaysPlugin plugin, AllayRepository repository, PluginSettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
    }

    public void reload(PluginSettings settings) {
        this.settings = settings;
    }

    public Optional<Location> homeOf(Allay allay) {
        return repository.homeLocationOf(allay);
    }

    public Optional<Location> prepareHomeForMode(Player owner, Allay allay, AllayMode mode) {
        if (!usesHome(mode)) {
            return repository.homeLocationOf(allay);
        }

        Optional<Location> home = findPreferredHome(owner, allay);
        home.ifPresent(location -> repository.setHomeLocation(allay, location));
        return home;
    }

    public Optional<Location> setManualHome(Player owner, Allay allay) {
        Location home = owner.getLocation().clone();
        repository.setHomeLocation(allay, home);
        return Optional.of(home);
    }

    public Optional<Location> resolveHome(Player owner, Allay allay) {
        Optional<Location> stored = repository.homeLocationOf(allay);
        if (stored.isPresent()) {
            return stored;
        }
        return findPreferredHome(owner, allay).map(location -> {
            repository.setHomeLocation(allay, location);
            return location;
        });
    }

    public Optional<Location> resolveHome(Allay allay) {
        Player owner = repository.ownerOf(allay)
                .map(uuid -> plugin.getServer().getPlayer(uuid))
                .orElse(null);
        return resolveHome(owner, allay);
    }

    public boolean usesHome(AllayMode mode) {
        return mode == AllayMode.STAY || mode == AllayMode.COLLECT_AROUND_HOME;
    }

    private Optional<Location> findPreferredHome(Player owner, Allay allay) {
        if (owner != null && settings.homeUsePlayerRespawnPoint()) {
            Optional<Location> respawn = playerRespawnLocation(owner)
                    .filter(location -> location.getWorld() != null)
                    .filter(location -> !settings.isWorldDisabled(location.getWorld().getName()));
            if (respawn.isPresent()) {
                return respawn.map(location -> location.clone().add(0.5, 0.25, 0.5));
            }
        }

        if (settings.homeSearchNearbyBeds()) {
            Optional<Location> bedNearAllay = findNearestBed(allay.getLocation(), settings.homeBedSearchRadius());
            if (bedNearAllay.isPresent()) {
                return bedNearAllay;
            }

            if (owner != null) {
                Optional<Location> bedNearOwner = findNearestBed(owner.getLocation(), settings.homeBedSearchRadius());
                if (bedNearOwner.isPresent()) {
                    return bedNearOwner;
                }
            }
        }

        if (settings.homeFallbackToCurrentLocation()) {
            return Optional.of(allay.getLocation().clone());
        }

        return Optional.empty();
    }

    private Optional<Location> playerRespawnLocation(Player player) {
        Optional<Location> modern = invokeLocationMethod(player, "getRespawnLocation");
        if (modern.isPresent()) {
            return modern;
        }
        return invokeLocationMethod(player, "getBedSpawnLocation");
    }

    private Optional<Location> invokeLocationMethod(Player player, String methodName) {
        try {
            Method method = Player.class.getMethod(methodName);
            Object result = method.invoke(player);
            if (result instanceof Location location) {
                return Optional.of(location);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // Keep compatibility with Bukkit/Paper method changes.
        }
        return Optional.empty();
    }

    private Optional<Location> findNearestBed(Location center, int radius) {
        if (center == null || center.getWorld() == null) {
            return Optional.empty();
        }

        World world = center.getWorld();
        if (settings.isWorldDisabled(world.getName())) {
            return Optional.empty();
        }

        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), baseY - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, baseY + radius);

        return nearbyBedLocations(world, baseX, baseY, baseZ, minY, maxY, radius).stream()
                .min(Comparator.comparingDouble(location -> location.distanceSquared(center)));
    }

    private java.util.List<Location> nearbyBedLocations(World world, int baseX, int baseY, int baseZ, int minY, int maxY, int radius) {
        java.util.List<Location> beds = new java.util.ArrayList<>();
        int radiusSquared = radius * radius;

        for (int x = baseX - radius; x <= baseX + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                    int dx = x - baseX;
                    int dy = y - baseY;
                    int dz = z - baseZ;
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }

                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (!type.isBlock() || !type.name().endsWith("_BED")) {
                        continue;
                    }

                    beds.add(block.getLocation().add(0.5, 1.0, 0.5));
                }
            }
        }

        return beds;
    }
}

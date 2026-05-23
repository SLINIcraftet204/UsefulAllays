package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.model.AllayMode;
import at.slini204.usefulallays.util.LocationCodec;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Allay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class PackedAllayService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayDisplayService displayService;
    public PackedAllayService(UsefulAllaysPlugin plugin, AllayRepository repository, AllayDisplayService displayService) {
        this.plugin = plugin;
        this.repository = repository;
        this.displayService = displayService;
    }

    public PackResult pack(Player player, Allay allay) {
        if (!repository.ownerOf(allay).filter(player.getUniqueId()::equals).isPresent()
                && !player.hasPermission("usefulallays.admin.bypass")) {
            return PackResult.NOT_OWNER;
        }

        PackedAllay packed = PackedAllay.from(allay, repository, player.getName());
        YamlConfiguration config = load();
        writePacked(config, packed);
        save(config);

        allay.remove();
        return PackResult.PACKED;
    }

    public Optional<Allay> unpack(Player player, String selector) {
        Optional<PackedAllay> selected = selectPacked(player, selector);
        if (selected.isEmpty()) {
            return Optional.empty();
        }

        PackedAllay packed = selected.get();
        Location spawnLocation = safeSpawnLocation(player.getLocation());
        Allay allay = (Allay) player.getWorld().spawnEntity(spawnLocation, EntityType.ALLAY);

        repository.claim(allay, player.getUniqueId(), packed.ownerName().isBlank() ? player.getName() : packed.ownerName());
        repository.setLevel(allay, Math.max(1, packed.level()));
        repository.setMode(allay, packed.mode());
        repository.setFilters(allay, packed.filters());
        packed.nickname().ifPresent(name -> repository.setCustomName(allay, name));
        packed.homeLocation().ifPresent(home -> repository.setHomeLocation(allay, home));
        displayService.applyDisplayName(player, allay);

        YamlConfiguration config = load();
        config.set("packed." + packed.id(), null);
        save(config);
        return Optional.of(allay);
    }

    public List<PackedAllay> packedAllays(Player player) {
        return packedAllays(player.getUniqueId()).stream()
                .sorted(Comparator
                        .comparing((PackedAllay allay) -> allay.nickname().orElse(allay.defaultName()).toLowerCase(Locale.ROOT))
                        .thenComparing(PackedAllay::packedAt))
                .toList();
    }

    public List<PackedAllay> packedAllays(UUID ownerUuid) {
        YamlConfiguration config = load();
        ConfigurationSection root = config.getConfigurationSection("packed");
        if (root == null) {
            return List.of();
        }

        List<PackedAllay> result = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            PackedAllay packed = PackedAllay.fromSection(id, section);
            if (packed.ownerUuid().equals(ownerUuid)) {
                result.add(packed);
            }
        }
        return result;
    }

    public Optional<PackedAllay> selectPacked(Player player, String selector) {
        List<PackedAllay> packed = packedAllays(player);
        if (packed.isEmpty()) {
            return Optional.empty();
        }
        if (selector == null || selector.isBlank() || selector.equalsIgnoreCase("first") || selector.equalsIgnoreCase("nearest")) {
            return Optional.of(packed.get(0));
        }

        String raw = selector.trim();
        String normalized = normalize(raw);
        try {
            int index = Integer.parseInt(raw);
            if (index >= 1 && index <= packed.size()) {
                return Optional.of(packed.get(index - 1));
            }
        } catch (NumberFormatException ignored) {
            // Not a number.
        }

        for (PackedAllay allay : packed) {
            if (allay.id().equalsIgnoreCase(raw) || allay.id().startsWith(raw.toLowerCase(Locale.ROOT))) {
                return Optional.of(allay);
            }
        }

        List<PackedAllay> exact = new ArrayList<>();
        List<PackedAllay> prefix = new ArrayList<>();
        for (PackedAllay allay : packed) {
            String name = normalize(allay.nickname().orElse(allay.defaultName()));
            if (name.equals(normalized)) {
                exact.add(allay);
            } else if (name.startsWith(normalized)) {
                prefix.add(allay);
            }
        }
        if (!exact.isEmpty()) {
            return Optional.of(exact.get(0));
        }
        if (!prefix.isEmpty()) {
            return Optional.of(prefix.get(0));
        }
        return Optional.empty();
    }

    private YamlConfiguration load() {
        File file = file();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(YamlConfiguration config) {
        try {
            File file = file();
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save packed Allays: " + exception.getMessage());
        }
    }

    private File file() {
        return new File(plugin.getDataFolder(), plugin.settings().storagePackedFile());
    }

    private void writePacked(YamlConfiguration config, PackedAllay packed) {
        String path = "packed." + packed.id();
        config.set(path + ".ownerUuid", packed.ownerUuid().toString());
        config.set(path + ".ownerName", packed.ownerName());
        config.set(path + ".defaultName", packed.defaultName());
        config.set(path + ".nickname", packed.nickname().orElse(null));
        config.set(path + ".level", packed.level());
        config.set(path + ".mode", packed.mode().name());
        config.set(path + ".filters", packed.filters().stream().map(Material::name).toList());
        config.set(path + ".home", packed.homeLocation().map(LocationCodec::encode).orElse(null));
        config.set(path + ".lastLocation", packed.lastLocation().map(LocationCodec::encode).orElse(null));
        config.set(path + ".packedAt", packed.packedAt());
    }

    private Location safeSpawnLocation(Location location) {
        Location spawn = location.clone().add(0, 1.0, 0);
        spawn.setPitch(0);
        return spawn;
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

    public enum PackResult {
        PACKED,
        NOT_OWNER
    }

    public record PackedAllay(
            String id,
            UUID ownerUuid,
            String ownerName,
            String defaultName,
            Optional<String> nickname,
            int level,
            AllayMode mode,
            List<Material> filters,
            Optional<Location> homeLocation,
            Optional<Location> lastLocation,
            long packedAt
    ) {
        private static PackedAllay from(Allay allay, AllayRepository repository, String fallbackOwnerName) {
            String ownerName = repository.ownerNameOf(allay).orElse(fallbackOwnerName);
            String defaultName = ownerName + "'s Allay";
            return new PackedAllay(
                    UUID.randomUUID().toString(),
                    repository.ownerOf(allay).orElseThrow(),
                    ownerName,
                    defaultName,
                    repository.customNameOf(allay),
                    repository.levelOf(allay),
                    repository.modeOf(allay),
                    new ArrayList<>(repository.filtersOf(allay)),
                    repository.homeLocationOf(allay),
                    Optional.of(allay.getLocation()),
                    System.currentTimeMillis()
            );
        }

        private static PackedAllay fromSection(String id, ConfigurationSection section) {
            UUID ownerUuid;
            try {
                ownerUuid = UUID.fromString(section.getString("ownerUuid", ""));
            } catch (IllegalArgumentException exception) {
                ownerUuid = new UUID(0L, 0L);
            }

            List<Material> filters = new ArrayList<>();
            for (String raw : section.getStringList("filters")) {
                Material material = Material.matchMaterial(raw.toUpperCase(Locale.ROOT));
                if (material != null && material.isItem()) {
                    filters.add(material);
                }
            }

            return new PackedAllay(
                    id,
                    ownerUuid,
                    section.getString("ownerName", ""),
                    section.getString("defaultName", "Allay"),
                    Optional.ofNullable(section.getString("nickname")).filter(value -> !value.isBlank()),
                    Math.max(1, section.getInt("level", 1)),
                    AllayMode.from(section.getString("mode", AllayMode.FOLLOW.name())),
                    filters,
                    LocationCodec.decode(section.getString("home")),
                    LocationCodec.decode(section.getString("lastLocation")),
                    section.getLong("packedAt", 0L)
            );
        }

        public String displayName() {
            return nickname.orElse(defaultName);
        }

        public String filterText() {
            if (filters.isEmpty()) {
                return "-";
            }
            return filters.stream().map(Material::name).reduce((left, right) -> left + ", " + right).orElse("-");
        }

        public Optional<World> lastWorld() {
            return lastLocation.map(Location::getWorld);
        }
    }
}

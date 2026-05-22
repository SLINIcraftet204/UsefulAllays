package at.slini204.usefulallays.config;

import at.slini204.usefulallays.model.LevelSettings;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PluginSettings {

    private final boolean claimingEnabled;
    private final Material claimItem;
    private final boolean consumeClaimItem;
    private final int maxAllaysPerPlayer;
    private final boolean showName;
    private final String defaultAllayName;
    private final String nameFormat;
    private final boolean followEnabled;
    private final long followIntervalTicks;
    private final double teleportAfterDistance;
    private final boolean teleportOnPlayerTeleport;
    private final boolean teleportOnWorldChange;
    private final boolean teleportOnJoin;
    private final boolean teleportOnRespawn;
    private final boolean preventOtherPlayersInteract;
    private final boolean preventOtherPlayersDamage;
    private final boolean ownerCanDamage;
    private final boolean collectionEnabled;
    private final long collectionIntervalTicks;
    private final boolean collectionRequireLoadedOwner;
    private final int maxItemsPerAllayPerScan;
    private final Set<Material> collectionBlacklist;
    private final Set<String> disabledWorlds;
    private final Map<Integer, LevelSettings> levels;

    private PluginSettings(
            boolean claimingEnabled,
            Material claimItem,
            boolean consumeClaimItem,
            int maxAllaysPerPlayer,
            boolean showName,
            String defaultAllayName,
            String nameFormat,
            boolean followEnabled,
            long followIntervalTicks,
            double teleportAfterDistance,
            boolean teleportOnPlayerTeleport,
            boolean teleportOnWorldChange,
            boolean teleportOnJoin,
            boolean teleportOnRespawn,
            boolean preventOtherPlayersInteract,
            boolean preventOtherPlayersDamage,
            boolean ownerCanDamage,
            boolean collectionEnabled,
            long collectionIntervalTicks,
            boolean collectionRequireLoadedOwner,
            int maxItemsPerAllayPerScan,
            Set<Material> collectionBlacklist,
            Set<String> disabledWorlds,
            Map<Integer, LevelSettings> levels
    ) {
        this.claimingEnabled = claimingEnabled;
        this.claimItem = claimItem;
        this.consumeClaimItem = consumeClaimItem;
        this.maxAllaysPerPlayer = maxAllaysPerPlayer;
        this.showName = showName;
        this.defaultAllayName = defaultAllayName;
        this.nameFormat = nameFormat;
        this.followEnabled = followEnabled;
        this.followIntervalTicks = followIntervalTicks;
        this.teleportAfterDistance = teleportAfterDistance;
        this.teleportOnPlayerTeleport = teleportOnPlayerTeleport;
        this.teleportOnWorldChange = teleportOnWorldChange;
        this.teleportOnJoin = teleportOnJoin;
        this.teleportOnRespawn = teleportOnRespawn;
        this.preventOtherPlayersInteract = preventOtherPlayersInteract;
        this.preventOtherPlayersDamage = preventOtherPlayersDamage;
        this.ownerCanDamage = ownerCanDamage;
        this.collectionEnabled = collectionEnabled;
        this.collectionIntervalTicks = collectionIntervalTicks;
        this.collectionRequireLoadedOwner = collectionRequireLoadedOwner;
        this.maxItemsPerAllayPerScan = maxItemsPerAllayPerScan;
        this.collectionBlacklist = collectionBlacklist;
        this.disabledWorlds = disabledWorlds;
        this.levels = levels;
    }

    public static PluginSettings from(FileConfiguration config) {
        Material claimItem = material(config.getString("claiming.item", "ECHO_SHARD"), Material.ECHO_SHARD);

        return new PluginSettings(
                config.getBoolean("claiming.enabled", true),
                claimItem,
                config.getBoolean("claiming.consumeItem", true),
                Math.max(0, config.getInt("claiming.maxAllaysPerPlayer", 3)),
                config.getBoolean("display.showName", true),
                config.getString("display.defaultAllayName", "{owner}'s Allay"),
                config.getString("display.nameFormat", "&b{allay} &7[Lv. {level}]"),
                config.getBoolean("follow.enabled", true),
                Math.max(5L, config.getLong("follow.checkIntervalTicks", 40L)),
                Math.max(1.0, config.getDouble("follow.teleportAfterDistance", 48.0)),
                config.getBoolean("follow.teleportOnPlayerTeleport", true),
                config.getBoolean("follow.teleportOnWorldChange", true),
                config.getBoolean("follow.teleportOnJoin", true),
                config.getBoolean("follow.teleportOnRespawn", true),
                config.getBoolean("protection.preventOtherPlayersInteract", true),
                config.getBoolean("protection.preventOtherPlayersDamage", true),
                config.getBoolean("protection.ownerCanDamage", false),
                config.getBoolean("collection.enabled", true),
                Math.max(5L, config.getLong("collection.scanIntervalTicks", 30L)),
                config.getBoolean("collection.requireLoadedOwner", true),
                Math.max(1, config.getInt("collection.maxItemsPerAllayPerScan", 8)),
                readMaterialSet(config, "collection.blacklist"),
                new HashSet<>(config.getStringList("worlds.disabled")),
                readLevels(config)
        );
    }

    private static Map<Integer, LevelSettings> readLevels(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("levels");
        Map<Integer, LevelSettings> result = new HashMap<>();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    result.put(level, new LevelSettings(
                            Math.max(1, section.getInt(key + ".filterSlots", 1)),
                            Math.max(1.0, section.getDouble(key + ".pickupRadius", 12.0)),
                            Math.max(1, section.getInt(key + ".carryStacks", 1)),
                            Math.max(1.0, section.getDouble(key + ".teleportDistance", 48.0))
                    ));
                } catch (NumberFormatException ignored) {
                    // Ignore invalid level keys.
                }
            }
        }

        result.putIfAbsent(1, new LevelSettings(1, 12.0, 1, 48.0));
        return Collections.unmodifiableMap(result);
    }

    private static Set<Material> readMaterialSet(FileConfiguration config, String path) {
        Set<Material> materials = new HashSet<>();
        for (String value : config.getStringList(path)) {
            Material material = material(value, null);
            if (material != null) {
                materials.add(material);
            }
        }
        return Collections.unmodifiableSet(materials);
    }

    private static Material material(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    public boolean claimingEnabled() {
        return claimingEnabled;
    }

    public Material claimItem() {
        return claimItem;
    }

    public boolean consumeClaimItem() {
        return consumeClaimItem;
    }

    public int maxAllaysPerPlayer() {
        return maxAllaysPerPlayer;
    }

    public boolean showName() {
        return showName;
    }

    public String defaultAllayName() {
        return defaultAllayName;
    }

    public String nameFormat() {
        return nameFormat;
    }

    public boolean followEnabled() {
        return followEnabled;
    }

    public long followIntervalTicks() {
        return followIntervalTicks;
    }

    public double teleportAfterDistance() {
        return teleportAfterDistance;
    }

    public boolean teleportOnPlayerTeleport() {
        return teleportOnPlayerTeleport;
    }

    public boolean teleportOnWorldChange() {
        return teleportOnWorldChange;
    }

    public boolean teleportOnJoin() {
        return teleportOnJoin;
    }

    public boolean teleportOnRespawn() {
        return teleportOnRespawn;
    }

    public boolean preventOtherPlayersInteract() {
        return preventOtherPlayersInteract;
    }

    public boolean preventOtherPlayersDamage() {
        return preventOtherPlayersDamage;
    }

    public boolean ownerCanDamage() {
        return ownerCanDamage;
    }

    public boolean collectionEnabled() {
        return collectionEnabled;
    }

    public long collectionIntervalTicks() {
        return collectionIntervalTicks;
    }

    public boolean collectionRequireLoadedOwner() {
        return collectionRequireLoadedOwner;
    }

    public int maxItemsPerAllayPerScan() {
        return maxItemsPerAllayPerScan;
    }

    public Set<Material> collectionBlacklist() {
        return collectionBlacklist;
    }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName);
    }

    public LevelSettings level(int level) {
        return levels.getOrDefault(level, levels.get(1));
    }

    public int highestConfiguredLevel() {
        return levels.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }
}

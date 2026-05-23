package at.slini204.usefulallays.data;

import at.slini204.usefulallays.model.AllayMode;
import at.slini204.usefulallays.util.LocationCodec;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Allay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class PdcAllayRepository implements AllayRepository {

    private final NamespacedKey ownerKey;
    private final NamespacedKey ownerNameKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey customNameKey;
    private final NamespacedKey modeKey;
    private final NamespacedKey homeLocationKey;
    private final NamespacedKey filtersKey;
    private final NamespacedKey claimedAtKey;

    public PdcAllayRepository(JavaPlugin plugin) {
        this.ownerKey = new NamespacedKey(plugin, "owner_uuid");
        this.ownerNameKey = new NamespacedKey(plugin, "owner_name");
        this.levelKey = new NamespacedKey(plugin, "level");
        this.customNameKey = new NamespacedKey(plugin, "custom_name");
        this.modeKey = new NamespacedKey(plugin, "mode");
        this.homeLocationKey = new NamespacedKey(plugin, "home_location");
        this.filtersKey = new NamespacedKey(plugin, "filters");
        this.claimedAtKey = new NamespacedKey(plugin, "claimed_at");
    }

    @Override
    public boolean isClaimed(Allay allay) {
        return allay.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING);
    }

    @Override
    public Optional<UUID> ownerOf(Allay allay) {
        String value = allay.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> ownerNameOf(Allay allay) {
        String value = allay.getPersistentDataContainer().get(ownerNameKey, PersistentDataType.STRING);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    @Override
    public Optional<String> customNameOf(Allay allay) {
        String value = allay.getPersistentDataContainer().get(customNameKey, PersistentDataType.STRING);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    @Override
    public void setCustomName(Allay allay, String customName) {
        if (customName == null || customName.isBlank()) {
            allay.getPersistentDataContainer().remove(customNameKey);
            return;
        }

        allay.getPersistentDataContainer().set(customNameKey, PersistentDataType.STRING, customName.trim());
    }

    @Override
    public void claim(Allay allay, UUID ownerUuid, String ownerName) {
        PersistentDataContainer pdc = allay.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, ownerUuid.toString());
        pdc.set(ownerNameKey, PersistentDataType.STRING, ownerName);
        pdc.set(levelKey, PersistentDataType.INTEGER, 1);
        pdc.set(modeKey, PersistentDataType.STRING, AllayMode.FOLLOW.name());
        pdc.set(claimedAtKey, PersistentDataType.LONG, System.currentTimeMillis());
        allay.setPersistent(true);
        allay.setRemoveWhenFarAway(false);
    }

    @Override
    public void release(Allay allay) {
        PersistentDataContainer pdc = allay.getPersistentDataContainer();
        pdc.remove(ownerKey);
        pdc.remove(ownerNameKey);
        pdc.remove(levelKey);
        pdc.remove(customNameKey);
        pdc.remove(modeKey);
        pdc.remove(homeLocationKey);
        pdc.remove(filtersKey);
        pdc.remove(claimedAtKey);
        allay.setCustomName(null);
        allay.setCustomNameVisible(false);
    }

    @Override
    public int levelOf(Allay allay) {
        Integer level = allay.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return level == null || level < 1 ? 1 : level;
    }

    @Override
    public void setLevel(Allay allay, int level) {
        allay.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, Math.max(1, level));
    }

    @Override
    public AllayMode modeOf(Allay allay) {
        String value = allay.getPersistentDataContainer().get(modeKey, PersistentDataType.STRING);
        return AllayMode.from(value);
    }

    @Override
    public void setMode(Allay allay, AllayMode mode) {
        allay.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, mode.name());
    }

    @Override
    public Optional<Location> homeLocationOf(Allay allay) {
        String raw = allay.getPersistentDataContainer().get(homeLocationKey, PersistentDataType.STRING);
        return LocationCodec.decode(raw);
    }

    @Override
    public void setHomeLocation(Allay allay, Location location) {
        String encoded = LocationCodec.encode(location);
        if (encoded.isBlank()) {
            clearHomeLocation(allay);
            return;
        }
        allay.getPersistentDataContainer().set(homeLocationKey, PersistentDataType.STRING, encoded);
    }

    @Override
    public void clearHomeLocation(Allay allay) {
        allay.getPersistentDataContainer().remove(homeLocationKey);
    }

    @Override
    public List<Material> filtersOf(Allay allay) {
        String raw = allay.getPersistentDataContainer().get(filtersKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> Material.matchMaterial(value.toUpperCase(Locale.ROOT)))
                .filter(material -> material != null && material.isItem())
                .toList();
    }

    @Override
    public void setFilters(Allay allay, List<Material> filters) {
        String value = filters.stream()
                .filter(material -> material != null && material.isItem())
                .map(Material::name)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        if (value.isBlank()) {
            allay.getPersistentDataContainer().remove(filtersKey);
        } else {
            allay.getPersistentDataContainer().set(filtersKey, PersistentDataType.STRING, value);
        }
    }

    @Override
    public List<Allay> findLoadedOwnedAllays(UUID ownerUuid) {
        List<Allay> allays = new ArrayList<>();
        for (Allay allay : findLoadedClaimedAllays()) {
            if (ownerOf(allay).filter(ownerUuid::equals).isPresent()) {
                allays.add(allay);
            }
        }
        return allays;
    }

    @Override
    public List<Allay> findLoadedClaimedAllays() {
        List<Allay> allays = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            allays.addAll(world.getEntitiesByClass(Allay.class).stream()
                    .filter(this::isClaimed)
                    .toList());
        }
        return allays;
    }
}

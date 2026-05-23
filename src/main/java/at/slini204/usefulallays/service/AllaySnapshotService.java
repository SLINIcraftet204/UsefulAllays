package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.config.PluginSettings;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.util.LocationCodec;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Allay;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public final class AllaySnapshotService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private PluginSettings settings;
    private BukkitTask task;

    public AllaySnapshotService(UsefulAllaysPlugin plugin, AllayRepository repository, PluginSettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
    }

    public void start() {
        stop();

        if (!settings.storageMirrorEnabled()) {
            return;
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::writeNowSafely,
                settings.storageMirrorUpdateIntervalTicks(),
                settings.storageMirrorUpdateIntervalTicks());
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

    public SnapshotResult writeNow() throws IOException {
        List<Allay> allays = repository.findLoadedClaimedAllays();
        File file = mirrorFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create directory " + parent.getAbsolutePath());
        }

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("meta.generatedAt", Instant.now().toString());
        yaml.set("meta.note", "Readable mirror generated from loaded UsefulAllays entities. PDC on the Allay entity remains the authoritative storage.");
        yaml.set("meta.loadedClaimedAllays", allays.size());

        for (Allay allay : allays) {
            String path = "allays." + allay.getUniqueId();
            yaml.set(path + ".entityUuid", allay.getUniqueId().toString());
            yaml.set(path + ".ownerUuid", repository.ownerOf(allay).map(Object::toString).orElse(""));
            yaml.set(path + ".ownerName", repository.ownerNameOf(allay).orElse(""));
            yaml.set(path + ".nickname", repository.customNameOf(allay).orElse(""));
            yaml.set(path + ".customName", repository.customNameOf(allay).orElse(""));
            yaml.set(path + ".level", repository.levelOf(allay));
            yaml.set(path + ".mode", repository.modeOf(allay).name());
            yaml.set(path + ".filters", repository.filtersOf(allay).stream().map(Material::name).toList());
            yaml.set(path + ".location", LocationCodec.encode(allay.getLocation()));
            yaml.set(path + ".locationReadable", LocationCodec.readable(allay.getLocation()));
            Location home = repository.homeLocationOf(allay).orElse(null);
            yaml.set(path + ".home", LocationCodec.encode(home));
            yaml.set(path + ".homeReadable", LocationCodec.readable(home));
            yaml.set(path + ".valid", allay.isValid());
            yaml.set(path + ".dead", allay.isDead());
            yaml.set(path + ".lastSeen", Instant.now().toString());
        }

        yaml.save(file);
        return new SnapshotResult(file, allays.size());
    }

    public void writeNowSafely() {
        try {
            writeNow();
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not write Allay storage mirror: " + exception.getMessage());
        }
    }

    private File mirrorFile() {
        return new File(plugin.getDataFolder(), settings.storageMirrorFile());
    }

    public record SnapshotResult(File file, int allayCount) {
    }
}

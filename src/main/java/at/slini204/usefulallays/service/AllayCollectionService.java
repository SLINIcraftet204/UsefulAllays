package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.config.PluginSettings;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.model.AllayMode;
import at.slini204.usefulallays.model.LevelSettings;
import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public final class AllayCollectionService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private PluginSettings settings;
    private BukkitTask task;

    public AllayCollectionService(UsefulAllaysPlugin plugin, AllayRepository repository, PluginSettings settings) {
        this.plugin = plugin;
        this.repository = repository;
        this.settings = settings;
    }

    public void start() {
        stop();

        if (!settings.collectionEnabled()) {
            return;
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::scanLoadedAllays, settings.collectionIntervalTicks(), settings.collectionIntervalTicks());
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

    private void scanLoadedAllays() {
        for (Allay allay : repository.findLoadedClaimedAllays()) {
            if (allay.isDead() || !allay.isValid() || settings.isWorldDisabled(allay.getWorld().getName())) {
                continue;
            }

            AllayMode mode = repository.modeOf(allay);
            if (mode == AllayMode.PASSIVE) {
                continue;
            }

            List<Material> filters = repository.filtersOf(allay);
            if (filters.isEmpty()) {
                continue;
            }

            Player owner = repository.ownerOf(allay)
                    .map(uuid -> plugin.getServer().getPlayer(uuid))
                    .orElse(null);

            if (owner == null && settings.collectionRequireLoadedOwner()) {
                continue;
            }

            collectNearbyItems(allay, owner, Set.copyOf(filters));
        }
    }

    private void collectNearbyItems(Allay allay, Player owner, Set<Material> filters) {
        int level = repository.levelOf(allay);
        LevelSettings levelSettings = settings.level(level);
        int collected = 0;

        for (Entity nearby : allay.getNearbyEntities(levelSettings.pickupRadius(), levelSettings.pickupRadius(), levelSettings.pickupRadius())) {
            if (!(nearby instanceof Item item) || item.isDead() || !item.isValid()) {
                continue;
            }

            ItemStack stack = item.getItemStack();
            Material type = stack.getType();

            if (!filters.contains(type) || settings.collectionBlacklist().contains(type)) {
                continue;
            }

            if (owner != null && addToOwnerInventory(owner, stack)) {
                item.remove();
                collected++;
            }

            if (collected >= settings.maxItemsPerAllayPerScan()) {
                return;
            }
        }
    }

    private boolean addToOwnerInventory(Player owner, ItemStack stack) {
        if (!canFullyFit(owner, stack)) {
            return false;
        }

        HashMap<Integer, ItemStack> leftover = owner.getInventory().addItem(stack.clone());
        return leftover.isEmpty();
    }

    private boolean canFullyFit(Player owner, ItemStack stack) {
        int remaining = stack.getAmount();
        int maxStackSize = stack.getMaxStackSize();

        for (ItemStack content : owner.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                remaining -= maxStackSize;
            } else if (content.isSimilar(stack)) {
                remaining -= Math.max(0, maxStackSize - content.getAmount());
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }
}

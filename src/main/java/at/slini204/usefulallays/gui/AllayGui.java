package at.slini204.usefulallays.gui;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.model.AllayMode;
import at.slini204.usefulallays.service.AllayUpgradeService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AllayGui implements Listener {

    private static final String TITLE = "UsefulAllays";

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayUpgradeService upgradeService;
    private final Map<UUID, UUID> openAllays = new ConcurrentHashMap<>();

    public AllayGui(UsefulAllaysPlugin plugin, AllayRepository repository, AllayUpgradeService upgradeService) {
        this.plugin = plugin;
        this.repository = repository;
        this.upgradeService = upgradeService;
    }

    public void open(Player player, Allay allay) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        int level = repository.levelOf(allay);
        AllayMode mode = repository.modeOf(allay);
        List<Material> filters = repository.filtersOf(allay);

        inventory.setItem(10, item(Material.AMETHYST_SHARD, "§bAllay Level", List.of("§7Current level: §e" + level)));
        inventory.setItem(12, item(Material.COMPASS, "§bMode", List.of("§7Current mode: §e" + mode.name(), "§8Click to cycle")));
        inventory.setItem(14, item(Material.HOPPER, "§bItem Filters", filterLore(filters)));
        inventory.setItem(16, item(Material.EMERALD, "§aUpgrade", List.of("§7Click to upgrade this Allay.")));
        inventory.setItem(22, item(Material.BARRIER, "§cClose", List.of("§7Close this menu.")));

        openAllays.put(player.getUniqueId(), allay.getUniqueId());
        player.openInventory(inventory);
        plugin.messages().send(player, "allay.guiOpened");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID allayUuid = openAllays.get(player.getUniqueId());
        if (allayUuid == null || !event.getView().getTitle().equals(TITLE)) {
            return;
        }

        event.setCancelled(true);

        Entity entity = Bukkit.getEntity(allayUuid);
        if (!(entity instanceof Allay allay) || !allay.isValid() || allay.isDead()) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 12) {
            AllayMode next = repository.modeOf(allay).next();
            repository.setMode(allay, next);
            plugin.messages().send(player, "allay.modeChanged", Map.of("mode", next.name()));
            open(player, allay);
        } else if (slot == 14) {
            toggleFilter(player, allay, event.getCursor());
            open(player, allay);
        } else if (slot == 16) {
            if (!player.hasPermission("usefulallays.upgrade")) {
                plugin.messages().send(player, "plugin.noPermission");
                return;
            }

            AllayUpgradeService.UpgradeResult result = upgradeService.upgrade(player, allay);
            switch (result) {
                case UPGRADED -> plugin.messages().send(player, "allay.upgraded", Map.of("level", String.valueOf(repository.levelOf(allay))));
                case MAX_LEVEL -> plugin.messages().send(player, "allay.maxLevel");
                case MISSING_ITEMS -> plugin.messages().send(player, "allay.missingUpgradeItems");
            }
            open(player, allay);
        } else if (slot == 22) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openAllays.remove(event.getPlayer().getUniqueId());
    }


    private void toggleFilter(Player player, Allay allay, ItemStack cursor) {
        if (cursor == null || cursor.getType().isAir() || !cursor.getType().isItem()) {
            plugin.messages().send(player, "allay.filterNeedItem");
            return;
        }

        Material material = cursor.getType();
        if (plugin.settings().collectionBlacklist().contains(material)) {
            plugin.messages().send(player, "allay.filterBlacklisted");
            return;
        }

        List<Material> filters = new ArrayList<>(repository.filtersOf(allay));
        if (filters.remove(material)) {
            repository.setFilters(allay, filters);
            plugin.messages().send(player, "allay.filterRemoved", Map.of("item", material.name()));
            return;
        }

        int maxSlots = plugin.settings().level(repository.levelOf(allay)).filterSlots();
        if (filters.size() >= maxSlots) {
            plugin.messages().send(player, "allay.filterFull", Map.of("slots", String.valueOf(maxSlots)));
            return;
        }

        filters.add(material);
        repository.setFilters(allay, filters);
        plugin.messages().send(player, "allay.filterAdded", Map.of("item", material.name()));
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private List<String> filterLore(List<Material> filters) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Configured filters: §e" + filters.size());
        lore.add("§8Click this slot while holding an item");
        lore.add("§8on your cursor to add/remove it.");
        if (filters.isEmpty()) {
            lore.add("§8No filters yet.");
            return lore;
        }

        for (Material filter : filters) {
            lore.add("§7- §e" + filter.name());
        }
        return lore;
    }
}

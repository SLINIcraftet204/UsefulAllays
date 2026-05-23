package at.slini204.usefulallays.gui;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.model.AllayMode;
import at.slini204.usefulallays.model.LevelSettings;
import at.slini204.usefulallays.service.AllayHomeService;
import at.slini204.usefulallays.service.AllayFollowService;
import at.slini204.usefulallays.service.AllayUpgradeService;
import at.slini204.usefulallays.util.LocationCodec;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AllayGui implements Listener {

    private static final String TITLE = "UsefulAllays";
    private static final int SIZE = 45;
    private static final int INFO_SLOT = 4;
    private static final int MODE_SLOT = 10;
    private static final int CALL_SLOT = 12;
    private static final int HOME_SLOT = 14;
    private static final int UPGRADE_SLOT = 16;
    private static final int RENAME_SLOT = 31;
    private static final int CLOSE_SLOT = 44;
    private static final int[] FILTER_SLOTS = {18, 19, 20, 21, 22, 23, 24, 25, 26};

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayUpgradeService upgradeService;
    private final AllayHomeService homeService;
    private final AllayFollowService followService;

    public AllayGui(UsefulAllaysPlugin plugin, AllayRepository repository, AllayUpgradeService upgradeService, AllayHomeService homeService, AllayFollowService followService) {
        this.plugin = plugin;
        this.repository = repository;
        this.upgradeService = upgradeService;
        this.homeService = homeService;
        this.followService = followService;
    }

    public void open(Player player, Allay allay) {
        AllayMenuHolder holder = new AllayMenuHolder(allay.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inventory);
        render(player, inventory, allay);

        player.openInventory(inventory);
        plugin.messages().send(player, "allay.guiOpened");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof AllayMenuHolder holder)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() >= topSize) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        Entity entity = Bukkit.getEntity(holder.allayUuid());
        if (!(entity instanceof Allay allay) || !allay.isValid() || allay.isDead()) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot == MODE_SLOT) {
            AllayMode next = repository.modeOf(allay).next();
            repository.setMode(allay, next);
            Optional<org.bukkit.Location> home = homeService.prepareHomeForMode(player, allay, next);
            plugin.messages().send(player, "allay.modeChanged", Map.of("mode", next.name()));
            if (homeService.usesHome(next) && home.isPresent()) {
                plugin.messages().send(player, "allay.homeSaved", Map.of("location", LocationCodec.readable(home.get())));
            }
            refresh(player, event.getView().getTopInventory(), allay);
            return;
        }

        if (slot == CALL_SLOT) {
            if (!player.hasPermission("usefulallays.call")) {
                plugin.messages().send(player, "plugin.noPermission");
                return;
            }

            AllayFollowService.SingleRecallResult result = followService.recallLoadedAllayToPlayer(player, allay, true);
            if (result == AllayFollowService.SingleRecallResult.MOVED) {
                plugin.messages().send(player, "allay.calledOne");
            } else {
                plugin.messages().send(player, "allay.callFailed");
            }
            refresh(player, event.getView().getTopInventory(), allay);
            return;
        }

        if (slot == HOME_SLOT) {
            if (!player.hasPermission("usefulallays.sethome")) {
                plugin.messages().send(player, "plugin.noPermission");
                return;
            }

            if (event.isRightClick()) {
                boolean moved = followService.sendLoadedAllayHome(player, allay);
                if (moved) {
                    plugin.messages().send(player, "allay.sentHome");
                } else {
                    plugin.messages().send(player, "allay.noHome");
                }
            } else {
                org.bukkit.Location home = homeService.setManualHome(player, allay).orElse(player.getLocation());
                plugin.messages().send(player, "allay.homeSaved", Map.of("location", LocationCodec.readable(home)));
            }
            refresh(player, event.getView().getTopInventory(), allay);
            return;
        }

        if (isFilterSlot(slot)) {
            int filterIndex = filterSlotIndex(slot);
            int maxFilterSlots = Math.min(plugin.settings().level(repository.levelOf(allay)).filterSlots(), FILTER_SLOTS.length);
            if (filterIndex >= maxFilterSlots) {
                plugin.messages().send(player, "allay.filterLocked");
                return;
            }

            toggleFilter(player, allay, event.getCursor(), event.getCurrentItem());
            refresh(player, event.getView().getTopInventory(), allay);
            return;
        }

        if (slot == UPGRADE_SLOT) {
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
            refresh(player, event.getView().getTopInventory(), allay);
            return;
        }

        if (slot == RENAME_SLOT) {
            plugin.messages().send(player, "rename.guiHint");
            return;
        }

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof AllayMenuHolder)) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesMenu = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (touchesMenu) {
            event.setCancelled(true);
        }
    }

    private void refresh(Player player, Inventory inventory, Allay allay) {
        render(player, inventory, allay);
        player.updateInventory();
    }

    private void render(Player player, Inventory inventory, Allay allay) {
        inventory.clear();

        int level = repository.levelOf(allay);
        AllayMode mode = repository.modeOf(allay);
        List<Material> filters = repository.filtersOf(allay);
        int maxFilterSlots = Math.min(plugin.settings().level(level).filterSlots(), FILTER_SLOTS.length);

        inventory.setItem(INFO_SLOT, item(Material.AMETHYST_SHARD, "§bAllay Info", infoLore(player, allay, level, mode, filters, maxFilterSlots)));
        inventory.setItem(MODE_SLOT, item(Material.COMPASS, "§bMode", modeLore(mode)));
        inventory.setItem(CALL_SLOT, item(Material.ENDER_PEARL, "§bCall to me", List.of(
                "§7Teleports this loaded Allay",
                "§7close to you.",
                "§8Useful when the Allay is flying",
                "§8around and hard to click."
        )));
        inventory.setItem(HOME_SLOT, item(Material.RESPAWN_ANCHOR, "§bHome", List.of(
                "§7Left click: set home here.",
                "§7Right click: send Allay home.",
                "§8Home modes use bed/respawn",
                "§8location or this manual point."
        )));
        inventory.setItem(UPGRADE_SLOT, item(Material.EMERALD, "§aUpgrade", upgradeLore(player, allay)));
        inventory.setItem(RENAME_SLOT, item(Material.NAME_TAG, "§bRename", List.of(
                "§7Rename the nearest owned Allay:",
                "§e/ua rename <name>",
                "§7Reset name:",
                "§e/ua rename reset"
        )));
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "§cClose", List.of("§7Close this menu.")));

        for (int index = 0; index < FILTER_SLOTS.length; index++) {
            int slot = FILTER_SLOTS[index];
            if (index >= maxFilterSlots) {
                inventory.setItem(slot, item(Material.BLACK_STAINED_GLASS_PANE, "§8Locked Filter Slot", List.of("§7Upgrade this Allay to unlock", "§7more item filters.")));
                continue;
            }

            if (index < filters.size()) {
                Material material = filters.get(index);
                inventory.setItem(slot, item(material, "§e" + formatMaterial(material), List.of(
                        "§7This item is currently collected.",
                        "§8Click with empty cursor to remove.",
                        "§8Click while holding any item to toggle it."
                )));
            } else {
                inventory.setItem(slot, item(Material.GRAY_STAINED_GLASS_PANE, "§7Empty Filter Slot", List.of(
                        "§7Pick up an item from your inventory",
                        "§7and click this slot to add it.",
                        "§8Your item will stay on the cursor."
                )));
            }
        }
    }

    private List<String> modeLore(AllayMode mode) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Current mode: §e" + mode.name());
        lore.add("§8Click to cycle the mode.");
        lore.add(" ");
        lore.add("§eFOLLOW §8- §7follows you and can collect nearby filters.");
        lore.add("§eSTAY §8- §7stays at home/bed, no collection.");
        lore.add("§eCOLLECT_AROUND_OWNER §8- §7follows and collects near you.");
        lore.add("§eCOLLECT_AROUND_HOME §8- §7stays and collects near home/bed.");
        lore.add("§ePASSIVE §8- §7no collection, no auto-follow.");
        return lore;
    }

    private List<String> infoLore(Player player, Allay allay, int level, AllayMode mode, List<Material> filters, int maxFilterSlots) {
        LevelSettings levelSettings = plugin.settings().level(level);
        String owner = repository.ownerNameOf(allay).orElse(player.getName());
        String customName = repository.customNameOf(allay).orElse("-");
        String home = repository.homeLocationOf(allay).map(LocationCodec::readable).orElse("-");

        List<String> lore = new ArrayList<>();
        lore.add("§7Owner: §e" + owner);
        lore.add("§7Custom name: §e" + customName);
        lore.add("§7Level: §e" + level + "§7/§e" + plugin.settings().highestConfiguredLevel());
        lore.add("§7Mode: §e" + mode.name());
        lore.add("§7Home: §e" + home);
        lore.add("§7Filters: §e" + filters.size() + "§7/§e" + maxFilterSlots);
        lore.add("§7Pickup radius: §e" + levelSettings.pickupRadius());
        lore.add("§7Teleport distance: §e" + levelSettings.teleportDistance());
        lore.add("§8Claimed entity: " + allay.getUniqueId().toString().substring(0, 8));
        return lore;
    }

    private List<String> upgradeLore(Player player, Allay allay) {
        int currentLevel = repository.levelOf(allay);
        int nextLevel = currentLevel + 1;
        int highestLevel = plugin.settings().highestConfiguredLevel();

        List<String> lore = new ArrayList<>();
        if (nextLevel > highestLevel) {
            lore.add("§eThis Allay is already max level.");
            lore.add("§7Current level: §e" + currentLevel);
            return lore;
        }

        LevelSettings current = plugin.settings().level(currentLevel);
        LevelSettings next = plugin.settings().level(nextLevel);
        lore.add("§7Next level: §e" + currentLevel + " §7→ §e" + nextLevel);
        lore.add("§7Filter slots: §e" + current.filterSlots() + " §7→ §e" + next.filterSlots());
        lore.add("§7Pickup radius: §e" + current.pickupRadius() + " §7→ §e" + next.pickupRadius());
        lore.add("§7Teleport distance: §e" + current.teleportDistance() + " §7→ §e" + next.teleportDistance());
        lore.add(" ");
        lore.add("§7Required items:");

        Map<Material, Integer> cost = upgradeService.costForNextLevel(allay);
        if (cost.isEmpty()) {
            lore.add("§aNo item cost configured.");
        } else {
            for (Map.Entry<Material, Integer> entry : cost.entrySet()) {
                int owned = upgradeService.count(player, entry.getKey());
                String color = owned >= entry.getValue() ? "§a" : "§c";
                lore.add(color + owned + "§7/§e" + entry.getValue() + " §7" + formatMaterial(entry.getKey()));
            }
        }

        lore.add(" ");
        lore.add("§8Click to upgrade this Allay.");
        return lore;
    }

    private void toggleFilter(Player player, Allay allay, ItemStack cursor, ItemStack clickedItem) {
        Material material = null;
        if (cursor != null && !cursor.getType().isAir() && cursor.getType().isItem()) {
            material = cursor.getType();
        } else if (clickedItem != null && !clickedItem.getType().isAir()) {
            List<Material> currentFilters = repository.filtersOf(allay);
            if (currentFilters.contains(clickedItem.getType())) {
                material = clickedItem.getType();
            }
        }

        if (material == null) {
            plugin.messages().send(player, "allay.filterNeedItem");
            return;
        }

        if (plugin.settings().collectionBlacklist().contains(material)) {
            plugin.messages().send(player, "allay.filterBlacklisted");
            return;
        }

        List<Material> filters = new ArrayList<>(repository.filtersOf(allay));
        if (filters.remove(material)) {
            repository.setFilters(allay, filters);
            plugin.messages().send(player, "allay.filterRemoved", Map.of("item", formatMaterial(material)));
            return;
        }

        int maxSlots = plugin.settings().level(repository.levelOf(allay)).filterSlots();
        if (filters.size() >= maxSlots) {
            plugin.messages().send(player, "allay.filterFull", Map.of("slots", String.valueOf(maxSlots)));
            return;
        }

        filters.add(material);
        repository.setFilters(allay, filters);
        plugin.messages().send(player, "allay.filterAdded", Map.of("item", formatMaterial(material)));
    }

    private boolean isFilterSlot(int slot) {
        return filterSlotIndex(slot) != -1;
    }

    private int filterSlotIndex(int slot) {
        for (int index = 0; index < FILTER_SLOTS.length; index++) {
            if (FILTER_SLOTS[index] == slot) {
                return index;
            }
        }
        return -1;
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

    private String formatMaterial(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase() + part.substring(1));
        }
        return String.join(" ", words);
    }

    private static final class AllayMenuHolder implements InventoryHolder {
        private final UUID allayUuid;
        private Inventory inventory;

        private AllayMenuHolder(UUID allayUuid) {
            this.allayUuid = allayUuid;
        }

        private UUID allayUuid() {
            return allayUuid;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

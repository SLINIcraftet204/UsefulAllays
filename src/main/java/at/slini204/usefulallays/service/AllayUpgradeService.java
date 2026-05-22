package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.config.MessageService;
import at.slini204.usefulallays.data.AllayRepository;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AllayUpgradeService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;

    public AllayUpgradeService(UsefulAllaysPlugin plugin, AllayRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public UpgradeResult upgrade(Player player, Allay allay) {
        int currentLevel = repository.levelOf(allay);
        int nextLevel = currentLevel + 1;

        if (nextLevel > plugin.settings().highestConfiguredLevel()) {
            return UpgradeResult.MAX_LEVEL;
        }

        Map<Material, Integer> cost = readCost(nextLevel);
        if (!hasItems(player, cost)) {
            return UpgradeResult.MISSING_ITEMS;
        }

        removeItems(player, cost);
        repository.setLevel(allay, nextLevel);
        applyDisplayName(player, allay, nextLevel);
        return UpgradeResult.UPGRADED;
    }

    private Map<Material, Integer> readCost(int level) {
        Map<Material, Integer> cost = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("upgradeCosts." + level + ".items");
        if (section == null) {
            return cost;
        }

        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
            int amount = section.getInt(key, 0);
            if (material != null && amount > 0) {
                cost.put(material, amount);
            }
        }
        return cost;
    }

    private boolean hasItems(Player player, Map<Material, Integer> cost) {
        for (Map.Entry<Material, Integer> entry : cost.entrySet()) {
            if (count(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private int count(Player player, Material material) {
        int amount = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                amount += stack.getAmount();
            }
        }
        return amount;
    }

    private void removeItems(Player player, Map<Material, Integer> cost) {
        for (Map.Entry<Material, Integer> entry : cost.entrySet()) {
            int remaining = entry.getValue();
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length && remaining > 0; i++) {
                ItemStack stack = contents[i];
                if (stack == null || stack.getType() != entry.getKey()) {
                    continue;
                }

                int remove = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - remove);
                remaining -= remove;

                if (stack.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
            player.getInventory().setContents(contents);
        }
    }

    private void applyDisplayName(Player owner, Allay allay, int level) {
        if (!plugin.settings().showName()) {
            return;
        }

        String name = plugin.settings().nameFormat()
                .replace("{owner}", owner.getName())
                .replace("{level}", String.valueOf(level));

        allay.setCustomName(MessageService.color(name));
        allay.setCustomNameVisible(true);
    }

    public enum UpgradeResult {
        UPGRADED,
        MAX_LEVEL,
        MISSING_ITEMS
    }
}

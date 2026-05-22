package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.config.MessageService;
import at.slini204.usefulallays.data.AllayRepository;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;

public final class AllayDisplayService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;

    public AllayDisplayService(UsefulAllaysPlugin plugin, AllayRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void applyDisplayName(Player owner, Allay allay) {
        applyDisplayName(allay, owner.getName());
    }

    public void applyDisplayName(Allay allay, String fallbackOwnerName) {
        if (!plugin.settings().showName()) {
            allay.setCustomNameVisible(false);
            return;
        }

        int level = repository.levelOf(allay);
        String ownerName = repository.ownerNameOf(allay).orElse(fallbackOwnerName);
        String allayName = displayBaseName(allay, ownerName);

        String name = plugin.settings().nameFormat()
                .replace("{owner}", ownerName)
                .replace("{allay}", allayName)
                .replace("{level}", String.valueOf(level));

        allay.setCustomName(MessageService.color(name));
        allay.setCustomNameVisible(true);
    }

    public String displayBaseName(Allay allay, String fallbackOwnerName) {
        return repository.customNameOf(allay)
                .orElseGet(() -> plugin.settings().defaultAllayName().replace("{owner}", fallbackOwnerName));
    }
}

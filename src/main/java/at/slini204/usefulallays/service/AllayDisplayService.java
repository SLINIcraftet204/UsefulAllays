package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.config.MessageService;
import at.slini204.usefulallays.data.AllayRepository;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;

import java.util.Optional;

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
        Optional<String> nickname = repository.customNameOf(allay)
                .map(String::trim)
                .filter(value -> !value.isBlank());

        String allayName = nickname.orElseGet(() -> plugin.settings().defaultAllayName().replace("{owner}", ownerName));
        String template = nickname.isPresent()
                ? plugin.settings().nicknameNameFormat()
                : plugin.settings().nameFormat();

        String name = template
                .replace("{owner}", ownerName)
                .replace("{allay}", allayName)
                .replace("{nickname}", nickname.orElse(allayName))
                .replace("{level}", String.valueOf(level));

        allay.setCustomName(MessageService.color(name));
        allay.setCustomNameVisible(true);
    }

    public String displayBaseName(Allay allay, String fallbackOwnerName) {
        return repository.customNameOf(allay)
                .orElseGet(() -> plugin.settings().defaultAllayName().replace("{owner}", fallbackOwnerName));
    }
}

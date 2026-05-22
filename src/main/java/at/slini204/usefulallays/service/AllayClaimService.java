package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public final class AllayClaimService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayDisplayService displayService;

    public AllayClaimService(UsefulAllaysPlugin plugin, AllayRepository repository, AllayDisplayService displayService) {
        this.plugin = plugin;
        this.repository = repository;
        this.displayService = displayService;
    }

    public ClaimResult claim(Player player, Allay allay) {
        if (!plugin.settings().claimingEnabled()) {
            return new ClaimResult(ClaimResult.Status.DISABLED, plugin.settings().maxAllaysPerPlayer());
        }

        UUID owner = player.getUniqueId();

        if (repository.isClaimed(allay)) {
            return repository.ownerOf(allay)
                    .filter(owner::equals)
                    .map(ignored -> new ClaimResult(ClaimResult.Status.ALREADY_OWNED_BY_YOU, plugin.settings().maxAllaysPerPlayer()))
                    .orElseGet(() -> new ClaimResult(ClaimResult.Status.ALREADY_OWNED_BY_OTHER, plugin.settings().maxAllaysPerPlayer()));
        }

        int limit = plugin.settings().maxAllaysPerPlayer();
        if (limit > 0 && repository.findLoadedOwnedAllays(owner).size() >= limit && !player.hasPermission("usefulallays.limit.unlimited")) {
            return new ClaimResult(ClaimResult.Status.LIMIT_REACHED, limit);
        }

        repository.claim(allay, owner, player.getName());
        displayService.applyDisplayName(player, allay);
        return new ClaimResult(ClaimResult.Status.CLAIMED, limit);
    }

    public void sendClaimResult(Player player, ClaimResult result) {
        switch (result.status()) {
            case CLAIMED -> plugin.messages().send(player, "claim.success");
            case DISABLED -> plugin.messages().send(player, "claim.disabled");
            case ALREADY_OWNED_BY_YOU -> plugin.messages().send(player, "claim.alreadyYours");
            case ALREADY_OWNED_BY_OTHER -> plugin.messages().send(player, "claim.alreadyOwned");
            case LIMIT_REACHED -> plugin.messages().send(player, "claim.limitReached", Map.of("limit", String.valueOf(result.limit())));
        }
    }
}

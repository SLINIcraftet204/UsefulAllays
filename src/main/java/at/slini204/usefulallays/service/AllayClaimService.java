package at.slini204.usefulallays.service;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class AllayClaimService {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayDisplayService displayService;
    private final PackedAllayService packedAllayService;

    public AllayClaimService(UsefulAllaysPlugin plugin, AllayRepository repository, AllayDisplayService displayService, PackedAllayService packedAllayService) {
        this.plugin = plugin;
        this.repository = repository;
        this.displayService = displayService;
        this.packedAllayService = packedAllayService;
    }

    public ClaimResult claim(Player player, Allay allay) {
        if (!plugin.settings().claimingEnabled()) {
            return new ClaimResult(ClaimResult.Status.DISABLED, effectiveLimit(player));
        }

        UUID owner = player.getUniqueId();

        if (repository.isClaimed(allay)) {
            return repository.ownerOf(allay)
                    .filter(owner::equals)
                    .map(ignored -> new ClaimResult(ClaimResult.Status.ALREADY_OWNED_BY_YOU, effectiveLimit(player)))
                    .orElseGet(() -> new ClaimResult(ClaimResult.Status.ALREADY_OWNED_BY_OTHER, effectiveLimit(player)));
        }

        int limit = effectiveLimit(player);
        int ownedCount = repository.findLoadedOwnedAllays(owner).size() + packedAllayService.packedAllays(owner).size();
        if (limit > 0 && ownedCount >= limit) {
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

    private int effectiveLimit(Player player) {
        if (player.hasPermission("usefulallays.limit.unlimited")) {
            return 0;
        }

        int limit = plugin.settings().maxAllaysPerPlayer();
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            if (!permission.getValue()) {
                continue;
            }

            String name = permission.getPermission().toLowerCase(Locale.ROOT);
            if (!name.startsWith("usefulallays.limit.")) {
                continue;
            }

            String suffix = name.substring("usefulallays.limit.".length());
            if (suffix.equals("unlimited")) {
                return 0;
            }

            try {
                limit = Math.max(limit, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                // Ignore non-numeric limit permissions.
            }
        }
        return limit;
    }
}

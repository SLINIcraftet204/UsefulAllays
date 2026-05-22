package at.slini204.usefulallays.data;

import at.slini204.usefulallays.model.AllayMode;
import org.bukkit.Material;
import org.bukkit.entity.Allay;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AllayRepository {

    boolean isClaimed(Allay allay);

    Optional<UUID> ownerOf(Allay allay);

    Optional<String> ownerNameOf(Allay allay);

    Optional<String> customNameOf(Allay allay);

    void setCustomName(Allay allay, String customName);

    void claim(Allay allay, UUID ownerUuid, String ownerName);

    void release(Allay allay);

    int levelOf(Allay allay);

    void setLevel(Allay allay, int level);

    AllayMode modeOf(Allay allay);

    void setMode(Allay allay, AllayMode mode);

    List<Material> filtersOf(Allay allay);

    void setFilters(Allay allay, List<Material> filters);

    List<Allay> findLoadedOwnedAllays(UUID ownerUuid);

    List<Allay> findLoadedClaimedAllays();
}

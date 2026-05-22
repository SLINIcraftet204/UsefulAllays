package at.slini204.usefulallays.listener;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.service.AllayFollowService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class PlayerMovementListener implements Listener {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayFollowService followService;

    public PlayerMovementListener(UsefulAllaysPlugin plugin, AllayRepository repository, AllayFollowService followService) {
        this.plugin = plugin;
        this.repository = repository;
        this.followService = followService;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (plugin.settings().teleportOnPlayerTeleport()) {
            followService.teleportLoadedAllaysToPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (plugin.settings().teleportOnWorldChange()) {
            followService.teleportLoadedAllaysToPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.settings().teleportOnJoin()) {
            followService.teleportLoadedAllaysToPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (plugin.settings().teleportOnRespawn()) {
            followService.teleportLoadedAllaysToPlayer(event.getPlayer());
        }
    }
}

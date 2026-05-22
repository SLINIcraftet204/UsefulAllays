package at.slini204.usefulallays.listener;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class AllayDamageListener implements Listener {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;

    public AllayDamageListener(UsefulAllaysPlugin plugin, AllayRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Allay allay) || !repository.isClaimed(allay)) {
            return;
        }

        Player attacker = attackingPlayer(event.getDamager());
        if (attacker == null) {
            return;
        }

        boolean owner = repository.ownerOf(allay).filter(attacker.getUniqueId()::equals).isPresent();
        boolean bypass = attacker.hasPermission("usefulallays.admin.bypass");

        if (bypass) {
            return;
        }

        if (owner && !plugin.settings().ownerCanDamage()) {
            event.setCancelled(true);
            return;
        }

        if (!owner && plugin.settings().preventOtherPlayersDamage()) {
            event.setCancelled(true);
            plugin.messages().send(attacker, "allay.protected");
        }
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}

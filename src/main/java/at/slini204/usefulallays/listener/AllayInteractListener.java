package at.slini204.usefulallays.listener;

import at.slini204.usefulallays.UsefulAllaysPlugin;
import at.slini204.usefulallays.data.AllayRepository;
import at.slini204.usefulallays.gui.AllayGui;
import at.slini204.usefulallays.service.AllayClaimService;
import at.slini204.usefulallays.service.ClaimResult;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class AllayInteractListener implements Listener {

    private final UsefulAllaysPlugin plugin;
    private final AllayRepository repository;
    private final AllayClaimService claimService;
    private final AllayGui allayGui;

    public AllayInteractListener(UsefulAllaysPlugin plugin, AllayRepository repository, AllayClaimService claimService, AllayGui allayGui) {
        this.plugin = plugin;
        this.repository = repository;
        this.claimService = claimService;
        this.allayGui = allayGui;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Allay allay)) {
            return;
        }

        Player player = event.getPlayer();

        if (repository.isClaimed(allay)) {
            boolean owner = repository.ownerOf(allay).filter(player.getUniqueId()::equals).isPresent();
            boolean bypass = player.hasPermission("usefulallays.admin.bypass");

            if (!owner && !bypass && plugin.settings().preventOtherPlayersInteract()) {
                event.setCancelled(true);
                plugin.messages().send(player, "allay.protected");
                return;
            }

            if (owner && player.isSneaking()) {
                if (!player.hasPermission("usefulallays.gui")) {
                    plugin.messages().send(player, "plugin.noPermission");
                    return;
                }
                event.setCancelled(true);
                allayGui.open(player, allay);
                return;
            }
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != plugin.settings().claimItem()) {
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission("usefulallays.claim")) {
            plugin.messages().send(player, "claim.noPermission");
            return;
        }

        ClaimResult result = claimService.claim(player, allay);
        claimService.sendClaimResult(player, result);

        if (result.status() == ClaimResult.Status.CLAIMED && plugin.settings().consumeClaimItem() && !player.hasPermission("usefulallays.admin.bypass")) {
            hand.setAmount(hand.getAmount() - 1);
        }
    }
}

package at.slini204.usefulallays;

import at.slini204.usefulallays.command.UsefulAllaysCommand;
import at.slini204.usefulallays.config.MessageService;
import at.slini204.usefulallays.config.PluginSettings;
import at.slini204.usefulallays.data.PdcAllayRepository;
import at.slini204.usefulallays.gui.AllayGui;
import at.slini204.usefulallays.listener.AllayDamageListener;
import at.slini204.usefulallays.listener.AllayInteractListener;
import at.slini204.usefulallays.listener.PlayerMovementListener;
import at.slini204.usefulallays.service.AllayClaimService;
import at.slini204.usefulallays.service.AllayCollectionService;
import at.slini204.usefulallays.service.AllayFollowService;
import at.slini204.usefulallays.service.AllayUpgradeService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class UsefulAllaysPlugin extends JavaPlugin {

    private PluginSettings settings;
    private MessageService messages;
    private PdcAllayRepository repository;
    private AllayClaimService claimService;
    private AllayFollowService followService;
    private AllayCollectionService collectionService;
    private AllayUpgradeService upgradeService;
    private AllayGui allayGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");

        loadServices();
        registerListeners();
        registerCommand();

        followService.start();
        collectionService.start();

        getLogger().info("UsefulAllays enabled. Make Allays more useful and more like pets.");
    }

    @Override
    public void onDisable() {
        if (followService != null) {
            followService.stop();
        }
        if (collectionService != null) {
            collectionService.stop();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        settings = PluginSettings.from(getConfig());
        messages.reload();
        followService.reload(settings);
        collectionService.reload(settings);
    }

    private void loadServices() {
        settings = PluginSettings.from(getConfig());
        messages = new MessageService(this);
        repository = new PdcAllayRepository(this);
        claimService = new AllayClaimService(this, repository);
        upgradeService = new AllayUpgradeService(this, repository);
        followService = new AllayFollowService(this, repository, settings);
        collectionService = new AllayCollectionService(this, repository, settings);
        allayGui = new AllayGui(this, repository, upgradeService);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new AllayInteractListener(this, repository, claimService, allayGui), this);
        getServer().getPluginManager().registerEvents(new AllayDamageListener(this, repository), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(this, repository, followService), this);
        getServer().getPluginManager().registerEvents(allayGui, this);
    }

    private void registerCommand() {
        PluginCommand command = getCommand("usefulallays");
        if (command == null) {
            getLogger().warning("Command usefulallays is not registered in plugin.yml.");
            return;
        }

        UsefulAllaysCommand executor = new UsefulAllaysCommand(this, repository);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void saveResourceIfMissing(String resourceName) {
        File file = new File(getDataFolder(), resourceName);
        if (!file.exists()) {
            saveResource(resourceName, false);
        }
    }

    public PluginSettings settings() {
        return settings;
    }

    public MessageService messages() {
        return messages;
    }
}

package net.unitedlands.cosmetics;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.unitedlands.cosmetics.commands.AdminCommands;
import net.unitedlands.cosmetics.commands.TabCompleter;
import net.unitedlands.cosmetics.utils.CosmeticsPlaceholders;
import net.unitedlands.cosmetics.utils.MessageProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.utils.Logger;

import java.util.List;

public class UnitedCosmetics extends JavaPlugin {

    private static MessageProvider messageProvider;

    @Override
    public void onEnable() {

        // Plugin startup logic.
        saveDefaultConfig();
        messageProvider = new MessageProvider(getConfig());
        registerCommands();
        registerPlaceholders();

        Logger.log("UnitedCosmetics has been enabled.", "UnitedCosmetics");
    }

    private void registerCommands() {
        TabCompleter tabCompleter = new TabCompleter(this);
        AdminCommands adminCommands = new AdminCommands(this, messageProvider, tabCompleter);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            registrar.register(
                    "unitedcosmetics",
                    "Main command argument for UnitedCosmetics",
                    List.of("cosmetics"),
                    adminCommands
            );
        });
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CosmeticsPlaceholders().register();
            Logger.log("PlaceholderAPI hooked successfully.", "UnitedCosmetics");
        } else {
            Logger.logWarning("PlaceholderAPI not found! Placeholders will not work.", "UnitedCosmetics");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic.
        Logger.log("UnitedCosmetics has been disabled.", "UnitedCosmetics");
    }
}
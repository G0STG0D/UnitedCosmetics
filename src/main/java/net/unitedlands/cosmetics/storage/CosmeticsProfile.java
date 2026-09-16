package net.unitedlands.cosmetics.storage;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class CosmeticsProfile {

    private final OfflinePlayer player;
    private final File file;
    private FileConfiguration config;

    public CosmeticsProfile(OfflinePlayer player) {
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("UnitedCosmetics");
        this.player = player;
        this.file = new File(Objects.requireNonNull(plugin).getDataFolder(), "players" + File.separator + player.getUniqueId() + ".yml");
        this.config = loadConfig();
    }

    public boolean hasFile() {
        return file.exists();
    }

    public void createFile() {
        if (hasFile()) return;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                Logger.logError("Failed to create parent directory for " + file.getAbsolutePath(), "UnitedCosmetics");
                return;
            }
        }

        try {
            if (file.createNewFile()) {

                config = new YamlConfiguration();
                config.set("name", player.getName());

                // Initialise custom prefix slots.
                config.set("saved-prefixes.slot_1", null);
                config.set("saved-prefixes.slot_2", null);
                config.set("saved-prefixes.slot_3", null);

                // Initialise chat colour slots.
                config.set("saved-chat-colours.slot_1", null);
                config.set("saved-chat-colours.slot_2", null);
                config.set("saved-chat-colours.slot_3", null);

                // Initialise equipped statuses.
                config.set("equipped.prefix", null);
                config.set("equipped.chat-colour", null);

                saveConfig();
                Logger.log("Created cosmetic profile for " + player.getName(), "UnitedCosmetics");
            }
        }
        catch (IOException e) {
            Logger.logError("IOException while creating player data file: " + file.getAbsolutePath(), "UnitedCosmetics");
        }
    }

    public void deleteFile() {
        if (hasFile()) {
            if (file.delete()) {
                Logger.log("Deleted cosmetic profile for " + player.getName(), "UnitedCosmetics");
            } else {
                Logger.logError("Failed to delete cosmetic profile for " + player.getName(), "UnitedCosmetics");
            }
        }
    }

    public void reset() {
        if (!hasFile()) return;

        config.set("saved-prefixes.slot_1", null);
        config.set("saved-prefixes.slot_2", null);
        config.set("saved-prefixes.slot_3", null);

        config.set("saved-chat-colours.slot_1", null);
        config.set("saved-chat-colours.slot_2", null);
        config.set("saved-chat-colours.slot_3", null);

        saveConfig();

        Logger.log("Reset cosmetic profile for " + player.getName(), "UnitedCosmetics");
    }

    private FileConfiguration loadConfig() {
        if (!hasFile()) return new YamlConfiguration();

        FileConfiguration fileConfiguration = new YamlConfiguration();
        try {
            fileConfiguration.load(file);
            return fileConfiguration;
        } catch (IOException | InvalidConfigurationException e) {
            Logger.logError("Failed to load player data file: " + file.getAbsolutePath(), "UnitedCosmetics");
            return new YamlConfiguration();
        }
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            Logger.logError("Failed to save player data file: " + file.getAbsolutePath(), "UnitedCosmetics");
        }
    }

// +----------------------------------------+ #
// |           Data Access Methods          | #
// +----------------------------------------+ #

    // Custom Prefixes
    public String getSavedPrefix(int slot) {
        return config.getString("saved-prefixes.slot_" + slot);
    }

    public void savePrefix(int slot, String prefixRaw) {
        config.set("saved-prefixes.slot_" + slot, prefixRaw);
        saveConfig();
    }

    public void setEquippedPrefix(String slotKey) {
        config.set("equipped.prefix", slotKey);
        saveConfig();
    }

    public String getEquippedPrefix() {
        return config.getString("equipped.prefix");
    }

    // Chat Colour
    public String getSavedChatColour(int slot) {
        return config.getString("saved-chat-colours.slot_" + slot);
    }

    public void saveChatColour(int slot, String colourTag) {
        config.set("saved-chat-colours.slot_" + slot, colourTag);
        saveConfig();
    }

    public void setEquippedChatColour(String slotKey) {
        config.set("equipped.chat-colour", slotKey);
        saveConfig();
    }

    public String getEquippedChatColour() {
        return config.getString("equipped.chat-colour");
    }
}
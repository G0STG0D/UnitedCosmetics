package net.unitedlands.cosmetics.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CosmeticsPlaceholders extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "unitedcosmetics";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Litning11";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        CosmeticsProfile profile = new CosmeticsProfile(player);

        // Example: %unitedcosmetics_prefix_1%
        if (params.startsWith("prefix_")) {
            int slot = parseSlot(params.replace("prefix_", ""));
            if (slot != -1) {
                String prefix = profile.getSavedPrefix(slot);
                return prefix != null ? prefix : "None";
            }
        }

        // Example: %unitedcosmetics_has_prefix_1%
        // Returns 'yes' or 'no' so can use in menus.
        if (params.startsWith("has_prefix_")) {
            int slot = parseSlot(params.replace("has_prefix_", ""));
            if (slot != -1) {
                String prefix = profile.getSavedPrefix(slot);
                return (prefix != null && !prefix.isEmpty()) ? "Yes" : "No";
            }
        }

        return null;
    }

    private int parseSlot(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}

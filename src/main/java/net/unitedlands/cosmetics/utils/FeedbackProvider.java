package net.unitedlands.cosmetics.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.unitedlands.utils.Messenger;

import java.util.Map;

public class FeedbackProvider {

    // Send messages to Admins, console, or player.
    public static void sendFeedback(CommandSender sender, OfflinePlayer target, String message, Map<String, String> map, String prefix) {
        // Send feedback to the Admin running the command (or console if done through menus).
        Messenger.sendMessage(sender, message, map, prefix);

        // If an Admin ran it and the player is online, notify the player.
        if (target != null && target.isOnline() && target.getPlayer() != null) {
            if (!(sender instanceof Player) || !sender.getName().equals(target.getName())) {
                Messenger.sendMessage(target.getPlayer(), message, map, prefix);
            }
        }
    }
}

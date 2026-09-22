package net.unitedlands.cosmetics.commands;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import net.unitedlands.cosmetics.UnitedCosmetics;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import net.unitedlands.cosmetics.utils.MessageProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

import static net.unitedlands.cosmetics.utils.FeedbackProvider.sendFeedback;

public class ChatColourCommands {

    private final UnitedCosmetics plugin;
    private final MessageProvider messageProvider;
    private final LuckPerms luckPerms;

    public ChatColourCommands(UnitedCosmetics plugin, MessageProvider messageProvider) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
        this.luckPerms = LuckPermsProvider.get();
    }

    public void execute(CommandSender sender, String[] args) {
        String prefix = messageProvider.get("messages.prefix");
        FileConfiguration config = plugin.getConfig();

        int maxSlots = config.getInt("chatcolour.slots");
        int maxColours = config.getInt("chatcolour.colours");
        List<String> blacklistedHexes = config.getStringList("chatcolour.blacklisted-hexes");

        if (args.length < 3) {
            sendFeedback(sender, null, messageProvider.get("messages.chatcolour-profile-usage"), null, prefix);
            return;
        }

        String action = args[1].toLowerCase();
        java.util.UUID realUuid = luckPerms.getUserManager().lookupUniqueId(args[2]).join();

        if (realUuid == null) {
            sendFeedback(sender, null, messageProvider.get("messages.unknown-player"), Map.of("player", args[2]), prefix);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(realUuid);
        String targetName = target.getName() != null ? target.getName() : args[2];
        CosmeticsProfile profile = new CosmeticsProfile(target);

        if (action.equals("clear")) {
            clearLuckPermsColour(target);
            if (profile.hasFile()) profile.setEquippedChatColour(null);
            sendFeedback(sender, target, messageProvider.get("messages.chatcolour-cleared"), Map.of("player", targetName), prefix);            return;
        }

        if (args.length < 4) {
            sendFeedback(sender, target, messageProvider.get("messages.chatcolour-slot-missing"), null, prefix);
            return;
        }

        int slot = parseSlot(args[3], maxSlots);
        if (slot == -1) {
            sendFeedback(sender, target, messageProvider.get("messages.chatcolour-slot-invalid"), null, prefix);
            return;
        }

        switch (action) {
            case "delete" -> {
                if (profile.hasFile()) {
                    profile.saveChatColour(slot, null);
                    if (("slot_" + slot).equals(profile.getEquippedChatColour())) {
                        clearLuckPermsColour(target);
                        profile.setEquippedChatColour(null);
                    }
                }
                sendFeedback(sender, target, messageProvider.get("messages.chatcolour-slot-deleted"), Map.of("slot", String.valueOf(slot), "player", targetName), prefix);
                return;
            }
            case "equip" -> {
                String savedColor = profile.getSavedChatColour(slot);
                if (savedColor == null || savedColor.isEmpty()) {
                    sendFeedback(sender, target, messageProvider.get("messages.chatcolour-slot-empty"), null, prefix);
                    return;
                }
                applyLuckPermsColour(target, savedColor);
                profile.setEquippedChatColour("slot_" + slot);
                sendFeedback(sender, target, messageProvider.get("messages.chatcolour-equipped"), Map.of("slot", String.valueOf(slot)), prefix);
                return;
            }
            case "save" -> {
                if (args.length < 5) {
                    sendFeedback(sender, target, messageProvider.get("messages.chatcolour-invalid-input"), null, prefix);
                    return;
                }

                // Collect hex codes, up to config defined max.
                StringBuilder colours = new StringBuilder();
                int count = 0;
                for (int i = 4; i < args.length && count < maxColours; i++) {
                    String input = args[i].trim();
                    // Basic hex validation.
                    if (input.matches("^#([A-Fa-f0-9]{6})$")) {

                        if (blacklistedHexes.contains(input.toLowerCase())) {
                            sendFeedback(sender, target, messageProvider.get("messages.chatcolour-invalid-hex"), null, prefix);
                            return;
                        }

                        if (count > 0) colours.append(":");
                        colours.append(input);
                        count++;
                    }
                }

                if (count == 0) {
                    sendFeedback(sender, target, messageProvider.get("messages.chatcolour-invalid-hex"), null, prefix);
                    return;
                }

                // Build the tag: solid colour if 1, gradient if more than 1.
                String colourTag = count == 1 ? "<" + colours + ">" : "<gradient:" + colours + ">";

                profile.createFile();
                profile.saveChatColour(slot, colourTag);
                applyLuckPermsColour(target, colourTag);
                profile.setEquippedChatColour("slot_" + slot);
                sendFeedback(sender, target, messageProvider.get("messages.chatcolour-saved"), Map.of("slot", String.valueOf(slot)), prefix);
                return;
            }
        }
        sendFeedback(sender, null, messageProvider.get("messages.chatcolour-profile-usage"), null, prefix);
    }

    private void applyLuckPermsColour(OfflinePlayer player, String colourTag) {
        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
            // Clear existing chat colour meta first.
            user.data().clear(NodeType.META.predicate(node -> node.getMetaKey().equals("chatcolour")));
            // Apply new chat colour meta.
            user.data().add(MetaNode.builder("chatcolour", colourTag).build());
        });
    }

    private void clearLuckPermsColour(OfflinePlayer player) {
        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user ->
                user.data().clear(NodeType.META.predicate(node -> node.getMetaKey().equals("chatcolour"))));
    }

    private int parseSlot(String arg, int maxSlots) {
        try {
            int slot = Integer.parseInt(arg);
            return (slot >= 1 && slot <= maxSlots) ? slot : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
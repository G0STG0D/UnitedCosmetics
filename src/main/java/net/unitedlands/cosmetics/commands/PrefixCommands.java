package net.unitedlands.cosmetics.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.unitedlands.cosmetics.UnitedCosmetics;
import net.unitedlands.cosmetics.storage.CosmeticsProfile;
import net.unitedlands.cosmetics.utils.FeedbackProvider;
import net.unitedlands.cosmetics.utils.MessageProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.unitedlands.cosmetics.utils.FeedbackProvider.sendFeedback;

public class PrefixCommands {

    private final UnitedCosmetics plugin;
    private final MessageProvider messageProvider;
    private final LuckPerms luckPerms;

    public PrefixCommands(UnitedCosmetics plugin, MessageProvider messageProvider) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
        this.luckPerms = LuckPermsProvider.get();
    }

    // Block certain MiniMessage features to prevent abuse.
    private final MiniMessage strictMiniMessage = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.rainbow())
                    .resolver(StandardTags.reset())
                    .resolver(StandardTags.decorations(TextDecoration.BOLD))
                    .resolver(StandardTags.decorations(TextDecoration.ITALIC))
                    .resolver(StandardTags.decorations(TextDecoration.UNDERLINED))
                    .resolver(StandardTags.decorations(TextDecoration.STRIKETHROUGH))
                    .build()
            )
            .build();

    public void execute(CommandSender sender, String[] args) {

        String prefix = messageProvider.get("messages.prefix");
        FileConfiguration config = plugin.getConfig();

        if (args.length < 3) {
            sendFeedback(sender, null, messageProvider.get("messages.prefix-profile-usage"), null, prefix);
            return;
        }

        String action = args[1].toLowerCase();

        // Use LuckPerms as source of truth for UUID.
        java.util.UUID realUuid = luckPerms.getUserManager().lookupUniqueId(args[2]).join();

        if (realUuid == null) {
            FeedbackProvider.sendFeedback(sender, null, messageProvider.get("messages.unknown-player"), Map.of("player", args[2]), prefix);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(realUuid);
        String targetName = target.getName() != null ? target.getName() : args[2];
        CosmeticsProfile profile = new CosmeticsProfile(target);

        if (action.equals("clear")) {
            clearLuckPermsPrefix(target);
            if (profile.hasFile()) {
                profile.setEquippedPrefix(null);
            }
            sendFeedback(sender, target, messageProvider.get("messages.prefix-reset"), Map.of("player", targetName), prefix);
            return;
        }

        if (args.length < 4) {
            sendFeedback(sender, target, messageProvider.get("messages.prefix-slot-missing"), null, prefix);
            return;
        }

        int maxSlots = config.getInt("prefix.slots", 3);
        int slot = parseSlot(args[3], maxSlots);
        if (slot == -1) {
            sendFeedback(sender, target, messageProvider.get("messages.prefix-slot-invalid"), null, prefix);
            return;
        }

        switch (action) {
            case "delete" -> {
                if (profile.hasFile()) {
                    profile.savePrefix(slot, null);

                    if (("slot_" + slot).equals(profile.getEquippedPrefix())) {
                        clearLuckPermsPrefix(target);
                        profile.setEquippedPrefix(null);
                    }
                }
                sendFeedback(sender, target, messageProvider.get("messages.prefix-slot-deleted"), Map.of("slot", String.valueOf(slot), "player", targetName), prefix);
                return;
            }

            case "equip" -> {
                String savedPrefix = profile.getSavedPrefix(slot);
                if (savedPrefix == null || savedPrefix.isEmpty()) {
                    sendFeedback(sender, target, messageProvider.get("messages.prefix-slot-empty"), null, prefix);
                    return;
                }

                applyLuckPermsPrefix(target, savedPrefix);
                profile.setEquippedPrefix("slot_" + slot);
                sendFeedback(sender, target, messageProvider.get("messages.prefix-equipped"), Map.of("slot", String.valueOf(slot)), prefix);
                return;

            }
            case "save" -> {
                if (args.length < 5) {
                    sendFeedback(sender, target, messageProvider.get("messages.prefix-invalid-input"), null, prefix);
                    return;
                }

                StringBuilder rawInput = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    rawInput.append(args[i]).append(" ");
                }

                String input = translateLegacyColors(rawInput.toString().trim());
                Component parsed = strictMiniMessage.deserialize(input);
                String plainText = PlainTextComponentSerializer.plainText().serialize(parsed);

                String regex = config.getString("prefix.regex");
                if (!plainText.matches(Objects.requireNonNull(regex))) {
                    sendFeedback(sender, target, messageProvider.get("messages.prefix-invalid-text"), null, prefix);
                    return;
                }

                int maxLength = config.getInt("prefix.length");
                if (plainText.length() > maxLength) {
                    sendFeedback(sender, target, messageProvider.get("messages.prefix-invalid-length"), Map.of("length", String.valueOf(maxLength)), prefix);
                    return;
                }

                List<String> blacklistedWords = config.getStringList("prefix.blacklisted-words");
                String lowerPlain = plainText.toLowerCase();
                for (String word : blacklistedWords) {
                    if (lowerPlain.matches(".*\\b" + word.toLowerCase() + "\\b.*")) {
                        sendFeedback(sender, target, messageProvider.get("messages.prefix-invalid-text"), null, prefix);
                        return;
                    }
                }

                profile.createFile();
                profile.savePrefix(slot, input);

                // Auto-equip upon successful save.
                applyLuckPermsPrefix(target, input);
                profile.setEquippedPrefix("slot_" + slot);
                sendFeedback(sender, target, messageProvider.get("messages.prefix-saved"), Map.of("slot", String.valueOf(slot)), prefix);
                return;
            }
        }

        sendFeedback(sender, null, messageProvider.get("messages.prefix-profile-usage"), null, prefix);
    }

    private int parseSlot(String arg, int maxSlots) {
        try {
            int slot = Integer.parseInt(arg);
            return (slot >= 1 && slot <= maxSlots) ? slot : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void applyLuckPermsPrefix(OfflinePlayer player, String prefixString) {
        String format = plugin.getConfig().getString("prefix.format");
        String formattedPrefix = Objects.requireNonNull(format).replace("{prefix}", prefixString);

        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
            user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == 100));
            Node node = Node.builder("prefix.100." + formattedPrefix).build();
            user.data().add(node);
        });
    }

    private void clearLuckPermsPrefix(OfflinePlayer player) {
        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user ->
                user.data().clear(NodeType.PREFIX.predicate(node -> node.getPriority() == 100)));
    }

    // Translate Legacy formatting to MiniMessage.
    private String translateLegacyColors(String input) {
        // Process weird Spigot/CommandPanels translation of hex codes.
        input = input.replaceAll("[&§]x[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])", "<#$1$2$3$4$5$6>");
        // Process normal Legacy formatting codes.
        return input.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>")
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }
}
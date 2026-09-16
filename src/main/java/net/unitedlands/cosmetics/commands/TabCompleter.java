package net.unitedlands.cosmetics.commands;

import net.unitedlands.cosmetics.UnitedCosmetics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TabCompleter {

    private final UnitedCosmetics plugin;

    public TabCompleter(UnitedCosmetics plugin) {
        this.plugin = plugin;
    }

    public List<String> getSuggestions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("unitedcosmetics.admin")) {
            return completions;
        }

        if (args.length <= 1) {
            completions.addAll(List.of("reload", "profile", "prefix", "chatcolour"));

        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("profile")) {
                completions.addAll(List.of("create", "delete", "reset"));
            } else if (args[0].equalsIgnoreCase("prefix") || args[0].equalsIgnoreCase("chatcolour")) {
                completions.addAll(List.of("save", "equip", "clear", "delete"));
            }

        } else if (args.length == 3) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }

        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("prefix") || args[0].equalsIgnoreCase("chatcolour")) {
                int maxSlots = plugin.getConfig().getInt(args[0].toLowerCase() + ".slots", 3);
                for (int i = 1; i <= maxSlots; i++) {
                    completions.add(String.valueOf(i));
                }
            }

        } else {
            if ((args[0].equalsIgnoreCase("prefix") || args[0].equalsIgnoreCase("chatcolour")) && args[1].equalsIgnoreCase("save")) {
                return completions;
            }
        }

        String currentArg = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}
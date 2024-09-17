package ir.xenoncommunity.modules.listeners;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.modules.ModuleManager;
import ir.xenoncommunity.utils.Configuration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@ModuleListener public class CommandWhitelist implements Listener {

    @EventHandler public void onCommandExecution(final ChatEvent e) {
        if (!e.getMessage().startsWith("/")
                || !(e.getSender() instanceof ProxiedPlayer)
                || ((ProxiedPlayer) e.getSender()).hasPermission(XenonCore.instance.getConfigData().getCommandwhitelist().getBypass())) {
            return;
        }

        String rawCommand = e.getMessage();
        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        Configuration.CommandWhitelistData whitelistData = XenonCore.instance.getConfigData().getCommandwhitelist();

        if (whitelistData.getPergroup().entrySet().stream()
                .filter(entry -> player.hasPermission("xenoncord.commandwhitelist." + entry.getKey()))
                .map(Map.Entry::getValue)
                .noneMatch(groupData ->
                        Arrays.asList(groupData.getServers()).contains(player.getServer().getInfo().getName()) &&
                                Arrays.asList(groupData.getCommands()).contains(rawCommand)
                )) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    whitelistData.getBlockmessage()));
            e.setCancelled(true);
        }
    }
    @EventHandler public void onTabComplete(final TabCompleteEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)
                || ((ProxiedPlayer) e.getSender()).hasPermission(XenonCore.instance.getConfigData().getCommandwhitelist().getBypass())) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        String command = e.getCursor().trim();
        Configuration.CommandWhitelistData whitelistData = XenonCore.instance.getConfigData().getCommandwhitelist();

        if(command.equals("/")) {
            e.getSuggestions().clear();
            e.getSuggestions().addAll(whitelistData.getPergroup().entrySet().stream()
                    .filter(entry -> player.hasPermission("xenoncord.commandwhitelist." + entry.getKey()))
                    .map(Map.Entry::getValue)
                    .flatMap(ss -> Arrays.stream(ss.getCommands()))
                    .collect(Collectors.toList()));
        }

        if (whitelistData.getPergroup().entrySet().stream()
                .filter(entry -> player.hasPermission("xenoncord.commandwhitelist." + entry.getKey()))
                .map(Map.Entry::getValue)
                .noneMatch(groupData ->
                        Arrays.asList(groupData.getServers()).contains(player.getServer().getInfo().getName()) &&
                                Arrays.asList(groupData.getCommands()).contains(command)
                )) {
            e.getSuggestions().clear();
            e.getSuggestions().addAll(whitelistData.getPergroup().entrySet().stream()
                    .filter(entry -> player.hasPermission("xenoncord.commandwhitelist." + entry.getKey()))
                    .map(Map.Entry::getValue)
                    .flatMap(ss -> Arrays.stream(ss.getCommands()))
                    .collect(Collectors.toList()));
        }
    }
}
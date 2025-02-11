package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Configuration;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@ModuleListener(isExtended = false, isImplemented = true)
public class CommandWhitelist implements Listener {

    private final Configuration.CommandWhitelistData whitelistData = XenonCore.instance.getConfigData().getCommandwhitelist();

    @EventHandler
    public void onCommandExecution(ChatEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer) || ((ProxiedPlayer) e.getSender()).hasPermission(whitelistData.getBypass()) || !e.getMessage().startsWith("/")) {
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) e.getSender();

        if (hasperm(player, e.getMessage().split(" ")[0])) {
            Message.sendNoPermMessage(player);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer) || ((ProxiedPlayer) e.getSender()).hasPermission(whitelistData.getBypass())) {
            return;
        }
        final ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        final String command = e.getCursor().trim();

        if (command.equals("/") || hasperm(player, command)) {
            clearAdd(e, player);
        }
    }

    private boolean hasperm(ProxiedPlayer playerIn, String command) {
        return whitelistData.getPergroup().entrySet().stream()
                .filter(entry -> playerIn.hasPermission("xenoncord.commandwhitelist." + entry.getKey())
                        && playerIn.getServer().getInfo().getName().equals(entry.getKey().split("\\.")[1]))
                .map(Map.Entry::getValue)
                .noneMatch(groupData -> Arrays.asList(groupData.getCommands()).contains(command));
    }

    private void clearAdd(TabCompleteEvent e, ProxiedPlayer playerIn) {
        e.getSuggestions().clear();
        e.getSuggestions().addAll(whitelistData.getPergroup().entrySet().stream()
                .filter(entry -> playerIn.hasPermission("xenoncord.commandwhitelist." + entry.getKey())
                        && playerIn.getServer().getInfo().getName().equals(entry.getKey().split("\\.")[1]))
                .map(Map.Entry::getValue)
                .flatMap(ss -> Arrays.stream(ss.getCommands()))
                .collect(Collectors.toList()));
    }
}

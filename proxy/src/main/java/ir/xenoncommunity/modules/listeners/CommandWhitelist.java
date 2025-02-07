package ir.xenoncommunity.modules.listeners;

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
@ModuleListener
public class CommandWhitelist implements Listener {

    @EventHandler
    public void onCommandExecution(final ChatEvent e) {
        if (!e.getMessage().startsWith("/")
                || !(e.getSender() instanceof ProxiedPlayer)
                || ((ProxiedPlayer) e.getSender()).hasPermission(XenonCore.instance.getConfigData().getCommandwhitelist().getBypass())) return;

        ProxiedPlayer player = (ProxiedPlayer) e.getSender();

        if (isPermitted(player, XenonCore.instance.getConfigData().getCommandwhitelist(), e.getMessage().split(" ")[0])) {
            Message.sendNoPermMessage(player);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTabComplete(final TabCompleteEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)
                || ((ProxiedPlayer) e.getSender()).hasPermission(XenonCore.instance.getConfigData().getCommandwhitelist().getBypass()))
            return;

        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        String command = e.getCursor().trim();
        Configuration.CommandWhitelistData whitelistData = XenonCore.instance.getConfigData().getCommandwhitelist();

        if (command.equals("/"))
            clearAndAdd(e, player, whitelistData);

        if (isPermitted(player, whitelistData, command)) {
            clearAndAdd(e, player, whitelistData);
        }
    }

    private boolean isPermitted(ProxiedPlayer playerIn, Configuration.CommandWhitelistData whitelistData, String command) {
        return whitelistData.getPergroup().entrySet().stream()
                .filter(entry -> playerIn.hasPermission("xenoncord.commandwhitelist." + entry.getKey()) && playerIn.getServer().getInfo().getName().equals(entry.getKey().split("\\.")[1]))
                .map(Map.Entry::getValue)
                .noneMatch(groupData ->
                        Arrays.asList(groupData.getCommands()).contains(command)
                );
    }

    private void clearAndAdd(TabCompleteEvent e, ProxiedPlayer playerIn, Configuration.CommandWhitelistData whitelistData) {
        e.getSuggestions().clear();
        e.getSuggestions().addAll(whitelistData.getPergroup().entrySet().stream()
                .filter(entry -> playerIn.hasPermission("xenoncord.commandwhitelist." + entry.getKey()) && playerIn.getServer().getInfo().getName().equals(entry.getKey().split("\\.")[1]))
                .map(Map.Entry::getValue)
                .flatMap(ss -> Arrays.stream(ss.getCommands()))
                .collect(Collectors.toList()));
    }
}

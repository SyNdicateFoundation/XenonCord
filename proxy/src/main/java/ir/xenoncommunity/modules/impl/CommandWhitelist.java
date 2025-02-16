package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Configuration;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

@SuppressWarnings("unused")
@ModuleListener(isExtended = false, isImplemented = true)
public class CommandWhitelist implements Listener {

    private final Configuration.CommandWhitelistData whitelistData = XenonCore.instance.getConfigData().getCommand_whitelist();

    @EventHandler
    public void onCommandExecution(ChatEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer) || ((ProxiedPlayer) e.getSender()).hasPermission(whitelistData.getBypass()) || !e.getMessage().startsWith("/")) {
            return;
        }

        final ProxiedPlayer player = (ProxiedPlayer) e.getSender();

        if (doesnthavepermission(player, e.getMessage().split(" ")[0])) {
            Message.sendNoPermMessage(player);
            e.setCancelled(true);
        }
    }

    private boolean doesnthavepermission(ProxiedPlayer playerIn, String command) {
        return whitelistData.getPer_group().entrySet().stream()
                .filter(entry -> playerIn.hasPermission("xenoncord.commandwhitelist." + entry.getKey().split("\\.")[0])
                        && playerIn.getServer().getInfo().getName().equals(entry.getKey().split("\\.")[1]))
                .map(Map.Entry::getValue)
                .noneMatch(groupData -> Arrays.asList(groupData.getCommands()).contains(command));
    }
}

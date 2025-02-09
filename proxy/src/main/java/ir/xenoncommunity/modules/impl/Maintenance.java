package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@ModuleListener(isExtended = true, isImplemented = true)
public class Maintenance extends Command implements Listener {
    private final String prefix = XenonCore.instance.getConfigData().getPrefix();
    public static Set<String> downServers = new HashSet<>();

    public Maintenance() {
        super("maintenance", XenonCore.instance.getConfigData().getMaintenance().getMaintenanceperm());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(args.length == 0){
            if (downServers.add("proxy")) {
                disconnectPlayers("proxy");
                Message.send(sender, XenonCore.instance.getConfigData().getMaintenance().getMaintenanceaddcommandmessage().replace("SERVER", "the whole proxy"), true);
            } else {
                downServers.remove("proxy");
                Message.send(sender, XenonCore.instance.getConfigData().getMaintenance().getMaintenanceremovecommandmessage().replace("SERVER", "the whole proxy"), true);
            }
            return;
        }
        final String action = args[0];
        final boolean add = "add".equalsIgnoreCase(action);
        if(!add && !"remove".equalsIgnoreCase(action)) {
            Message.send(sender, XenonCore.instance.getConfigData().getUnknownoptionmessage().replace("OPTIONS", "add, remove, blank (to set the whole proxy)"), false);
            return;
        }
        final String targetServer = args[1];
        if(targetServer.isEmpty()) {
            Message.send(sender, "Please enter a server name", false);
            return;
        }
        if(add){
            downServers.add(args[1]);
            disconnectPlayers(targetServer);
            Message.send(sender,
                    XenonCore.instance.getConfigData().getMaintenance().getMaintenanceaddcommandmessage()
                            .replace("SERVER", args[1]), true);

        } else {
            downServers.remove(targetServer);
            Message.send(sender, XenonCore.instance.getConfigData().getMaintenance().getMaintenanceremovecommandmessage().replace("SERVER", targetServer), true);
        }
    }

    private void disconnectPlayers(String serverName) {
        XenonCore.instance.getBungeeInstance().getPlayers().forEach(player -> {
            if (!player.hasPermission(XenonCore.instance.getConfigData().getMaintenance().getMaintenancebypassperm()) && player.getServer().getInfo().getName().equals(serverName)) {
                player.disconnect(ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getMaintenance().getMaintenancedisconnectmessage()));
            }
        });
    }

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        if (downServers.contains("proxy") || downServers.contains(e.getTarget().getName())) {
            if (!e.getPlayer().hasPermission(XenonCore.instance.getConfigData().getMaintenance().getMaintenancebypassperm())) {
                e.getPlayer().disconnect(ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getMaintenance().getMaintenancedisconnectmessage()));
            }
        }
    }

    @EventHandler
    public void onServerSwitch(ServerConnectEvent e) {
        if (downServers.contains(e.getTarget().getName()) && !e.getPlayer().hasPermission(XenonCore.instance.getConfigData().getMaintenance().getMaintenancebypassperm())) {
            e.setCancelled(true);
            Message.send(e.getPlayer(), XenonCore.instance.getConfigData().getMaintenance().getMaintenancedisconnectmessage(), false);
        }
    }
}

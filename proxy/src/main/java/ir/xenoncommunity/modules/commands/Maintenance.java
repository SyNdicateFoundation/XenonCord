package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;

@SuppressWarnings("unused") public class Maintenance extends Command implements Listener {
    public static ArrayList<String> downServers;


    public Maintenance() {
        super("maintenance", XenonCore.instance.getConfigData().getModules().getMaintenanceperm());
        downServers = new ArrayList<>();
        XenonCore.instance.getBungeeInstance().pluginManager.registerListener(null, this);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getMaintenanceperm()))
            return;

        if(args.length <= 0) {
            if (!downServers.contains("proxy")) {
                downServers.add("proxy");
                Message.send(sender,
                        "PREFIX &cthe whole proxy is now on maintenance mode."
                                .replace("PREFIX",
                                        XenonCore.instance.getConfigData().getPrefix()), true);
                XenonCore.instance.getBungeeInstance().getPlayers().forEach(e -> {
                    if (!e.hasPermission(
                            XenonCore.instance.getConfigData().getModules().getMaintenancebypassperm())){
                        e.disconnect(ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getModules().getMaintenancedisconnectmessage()
                                .replace("PREFIX",
                                        XenonCore.instance.getConfigData().getPrefix())));
                    }
                });

            }
            else {
                downServers.remove("proxy");
                Message.send(sender,
                        "PREFIX &cthe whole proxy is no longer on maintenance mode."
                                .replace("PREFIX",
                                        XenonCore.instance.getConfigData().getPrefix()), true);
            }
            return;
        }
        switch (args[0]){
            case "add":
                if(args[1].isEmpty()) Message.send(sender, "Please enter a server name", false);
                downServers.add(args[1]);
                Message.send(sender,
                        "PREFIX &cAdded SERVER to maintenance mode server list.."
                                .replace("PREFIX",  XenonCore.instance.getConfigData().getPrefix())
                                .replace("SERVER", args[1]), true);
                XenonCore.instance.getBungeeInstance().getPlayers().forEach(e -> {
                    if (!e.hasPermission(
                            XenonCore.instance.getConfigData().getModules().getMaintenancebypassperm())
                    && e.getServer().getInfo().getName().equals(args[1])){
                        e.disconnect(ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getModules().getMaintenancedisconnectmessage()
                                .replace("PREFIX",
                                        XenonCore.instance.getConfigData().getPrefix())));
                    }
                });
                break;
            case "remove":
                if(args[1].isEmpty()) Message.send(sender, "Please enter a server name", false);
                downServers.remove(args[1]);
                Message.send(sender,
                        "PREFIX &cRemoved SERVER from maintenance mode server list.."
                            .replace("PREFIX",  XenonCore.instance.getConfigData().getPrefix())
                            .replace("SERVER", args[1]), true);
                break;

        }

    }
    @EventHandler
    public void onJoin(PostLoginEvent e){
        if(!Maintenance.downServers.contains("proxy")) return;

        if(!e.getPlayer().hasPermission(
                XenonCore.instance.getConfigData().getModules().getMaintenancebypassperm())) {
            e.getPlayer().disconnect(
                    ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getModules().getMaintenancedisconnectmessage()
                            .replace("PREFIX",
                                    XenonCore.instance.getConfigData().getPrefix())));
        }
    }
    @EventHandler
    public void onServerSwitch(ServerSwitchEvent e){
        if(!downServers.contains(e.getPlayer().getServer().getInfo().getName())) return;
        if(!e.getPlayer().hasPermission(
                XenonCore.instance.getConfigData().getModules().getMaintenancebypassperm())
        && downServers.contains(e.getPlayer().getServer().getInfo().getName())) {
            e.getPlayer().connect(e.getFrom(), ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);
            Message.send(e.getPlayer(), XenonCore.instance.getConfigData().getModules().getMaintenancedisconnectmessage()
                    .replace("PREFIX",
                            XenonCore.instance.getConfigData().getPrefix()), false);
        }
    }
}

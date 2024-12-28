package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;

@SuppressWarnings("unused") public class Maintenance extends Command implements Listener {
    public static ArrayList<String> downServers = null;
    private final String prefix = XenonCore.instance.getConfigData().getPrefix();


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
                        XenonCore.instance.getConfigData().getModules().getMaintenanceaddcommandmessage()
                                .replace("SERVER", "the whole proxy")
                                , true);
                XenonCore.instance.getBungeeInstance().getPlayers().forEach(e -> {
                    if (!e.hasPermission(
                            XenonCore.instance.getConfigData().getModules().getMaintenancebypassperm())){
                        e.disconnect(ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getModules().getMaintenancedisconnectmessage()));
                    }
                });

            }
            else {
                downServers.remove("proxy");
                Message.send(sender,
                        XenonCore.instance.getConfigData().getModules().getMaintenanceremovecommandmessage()
                                .replace("SERVER", "the whole proxy")
                        , true);
            }
            return;
        }
        switch (args[0]){
            case "add":
                if(args[1].isEmpty()) Message.send(sender, "Please enter a server name", false);
                downServers.add(args[1]);
                Message.send(sender,
                        XenonCore.instance.getConfigData().getModules().getMaintenanceaddcommandmessage()
                                .replace("SERVER", args[1]), true);
                XenonCore.instance.getBungeeInstance().getPlayers().forEach(e -> {
                    if (!e.hasPermission(
                            XenonCore.instance.getConfigData().getModules().getMaintenancebypassperm())
                    && e.getServer().getInfo().getName().equals(args[1])){
                        e.disconnect(ChatColor.translateAlternateColorCodes('&',
                                XenonCore.instance.getConfigData().getModules().getMaintenancedisconnectmessage()));
                    }
                });
                break;
            case "remove":
                if(args[1].isEmpty()) Message.send(sender, "Please enter a server name", false);
                downServers.remove(args[1]);
                Message.send(sender,
                        XenonCore.instance.getConfigData().getModules().getMaintenanceremovecommandmessage()
                                .replace("SERVER", args[1]), true);
                break;
            default:
                Message.send(sender,XenonCore.instance.getConfigData().getUnknownoptionmessage()
                                .replace("OPTIONS", "add, remove, blank (to set the whole proxy)"),
                        false);
        }

    }
    @EventHandler
    public void onJoin(PostLoginEvent e){
        if(!(Maintenance.downServers.contains("proxy") || Maintenance.downServers.contains(e.getTarget().getName()))) return;

        if(!e.getPlayer().hasPermission(
                XenonCore.instance.getConfigData().getModules().getMaintenancebypassperm())) {
            e.getPlayer().disconnect(
                    ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getModules().getMaintenancedisconnectmessage()));
        }
    }
    @EventHandler
    public void onServerSwitch(ServerConnectEvent e){
        if(!downServers.contains(e.getTarget().getName())) return;

        if(!e.getPlayer().hasPermission(
                XenonCore.instance.getConfigData().getModules().getMaintenancebypassperm())) {
            e.setCancelled(true);
            Message.send(e.getPlayer(), XenonCore.instance.getConfigData().getModules().getMaintenancedisconnectmessage(), false);

        }
    }
}

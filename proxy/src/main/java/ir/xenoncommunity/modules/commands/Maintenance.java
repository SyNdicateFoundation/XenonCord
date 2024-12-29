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
    private final String prefix = XenonCore.instance.getConfigData().getPrefix();


    public Maintenance() {
        super("maintenance", XenonCore.instance.getConfigData().getMaintenance().getMaintenanceperm());
        XenonCore.instance.setDownServers(new ArrayList<>());
        XenonCore.instance.getBungeeInstance().pluginManager.registerListener(null, this);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission(XenonCore.instance.getConfigData().getMaintenance().getMaintenanceperm()))
            return;

        if(args.length <= 0) {
            if (!XenonCore.instance.getDownServers().contains("proxy")) {
                XenonCore.instance.getDownServers().add("proxy");
                Message.send(sender,
                        XenonCore.instance.getConfigData().getMaintenance().getMaintenanceaddcommandmessage()
                                .replace("SERVER", "the whole proxy")
                                , true);
                XenonCore.instance.getBungeeInstance().getPlayers().forEach(e -> {
                    if (!e.hasPermission(
                            XenonCore.instance.getConfigData().getMaintenance().getMaintenancebypassperm())){
                        e.disconnect(ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getMaintenance().getMaintenancedisconnectmessage()));
                    }
                });

            }
            else {
                XenonCore.instance.getDownServers().remove("proxy");
                Message.send(sender,
                        XenonCore.instance.getConfigData().getMaintenance().getMaintenanceremovecommandmessage()
                                .replace("SERVER", "the whole proxy")
                        , true);
            }
            return;
        }
        switch (args[0]){
            case "add":
                if(args[1].isEmpty()) Message.send(sender, "Please enter a server name", false);
                XenonCore.instance.getDownServers().add(args[1]);
                Message.send(sender,
                        XenonCore.instance.getConfigData().getMaintenance().getMaintenanceaddcommandmessage()
                                .replace("SERVER", args[1]), true);
                XenonCore.instance.getBungeeInstance().getPlayers().forEach(e -> {
                    if (!e.hasPermission(
                            XenonCore.instance.getConfigData().getMaintenance().getMaintenancebypassperm())
                    && e.getServer().getInfo().getName().equals(args[1])){
                        e.disconnect(ChatColor.translateAlternateColorCodes('&',
                                XenonCore.instance.getConfigData().getMaintenance().getMaintenancedisconnectmessage()));
                    }
                });
                break;
            case "remove":
                if(args[1].isEmpty()) Message.send(sender, "Please enter a server name", false);
                XenonCore.instance.getDownServers().remove(args[1]);
                Message.send(sender,
                        XenonCore.instance.getConfigData().getMaintenance().getMaintenanceremovecommandmessage()
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
        if(!(XenonCore.instance.getDownServers().contains("proxy") || XenonCore.instance.getDownServers().contains(e.getTarget().getName()))) return;

        if(!e.getPlayer().hasPermission(
                XenonCore.instance.getConfigData().getMaintenance().getMaintenancebypassperm())) {
            e.getPlayer().disconnect(
                    ChatColor.translateAlternateColorCodes('&', XenonCore.instance.getConfigData().getMaintenance().getMaintenancedisconnectmessage()));
        }
    }
    @EventHandler
    public void onServerSwitch(ServerConnectEvent e){
        if(!XenonCore.instance.getDownServers().contains(e.getTarget().getName())) return;

        if(!e.getPlayer().hasPermission(
                XenonCore.instance.getConfigData().getMaintenance().getMaintenancebypassperm())) {
            e.setCancelled(true);
            Message.send(e.getPlayer(), XenonCore.instance.getConfigData().getMaintenance().getMaintenancedisconnectmessage(), false);

        }
    }
}

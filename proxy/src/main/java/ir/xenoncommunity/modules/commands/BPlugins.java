package ir.xenoncommunity.modules.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;

public class BPlugins extends Command implements Listener {
    public BPlugins(){
        super("BPlugins", XenonCore.instance.getConfigData().getModules().getPingperm());
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getPluginsperm()))
            return;

        if(args.length == 0){
            final StringBuilder sb = new StringBuilder();

            XenonCore.instance.getBungeeInstance().getPluginManager().getPlugins().forEach(e -> sb.append(e.getDescription().getName()).append(", "));

            Message.send(sender, String.format("%s &cPLUGINS: %s.",
                    XenonCore.instance.getConfigData().getPrefix(),
                    sb.substring(0, sb.length() - 2)) , false);
            return;
        }

        if(!sender.hasPermission(XenonCore.instance.getConfigData().getModules().getPluginstoggleperm())) return;

        switch(args[0]){
            case "load":
                XenonCore.instance.getBungeeInstance().getPluginManager().getPlugin(args[1]).onLoad();
                XenonCore.instance.getBungeeInstance().getPluginManager().getPlugin(args[1]).onEnable();
                Message.send(sender,
                        "PREFIX &cLoading PLUGIN."
                                .replace("PREFIX",  XenonCore.instance.getConfigData().getPrefix())
                                .replace("PLUGIN", args[1]),
                        true);
                break;
            case "unload":
                XenonCore.instance.getBungeeInstance().getPluginManager().getPlugin(args[1]).onDisable();
                XenonCore.instance.getBungeeInstance().getPluginManager().unregisterCommands(XenonCore.instance.getBungeeInstance().getPluginManager().getPlugin(args[1]));
                XenonCore.instance.getBungeeInstance().getPluginManager().unregisterListeners(XenonCore.instance.getBungeeInstance().getPluginManager().getPlugin(args[1]));
                Message.send(sender,
                        "PREFIX &cUnloading PLUGIN."
                                .replace("PREFIX",  XenonCore.instance.getConfigData().getPrefix())
                                .replace("PLUGIN", args[1]),
                        true);
                break;
            default:
                Message.send(sender,
                        "PREFIX &cUnknown option, available: load, unload, blank (to see plugins list)"
                                .replace("PREFIX",  XenonCore.instance.getConfigData().getPrefix()),
                        false);
                break;
        }
    }
}

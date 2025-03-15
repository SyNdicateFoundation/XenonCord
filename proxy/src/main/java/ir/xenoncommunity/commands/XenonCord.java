package ir.xenoncommunity.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class XenonCord extends Command {
    public static final Set<String> ABstatusPlayers = new HashSet<>();
    public boolean log = false;

    public XenonCord() {
        super("xenoncord", XenonCore.instance.getConfigData().getXenoncord_permission());
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Message.send(sender, "&7This server is using &b&lXenonCord &r&7by &fRealStresser.&7\nPlease, report bugs on:\nhttps://github.com/SyNdicateFoundation/XenonCord", false);
            return;
        }

        switch (args[0]) {
            case "reload":
                if (!sender.hasPermission(XenonCore.instance.getConfigData().getReload_permission())) return;
                Message.send(sender, XenonCore.instance.getConfigData().getReload_message(), true);
                XenonCore.instance.setConfigData(XenonCore.instance.getConfiguration().init());
                Message.send(sender, XenonCore.instance.getConfigData().getReload_complete_message(), true);
                break;
        }
    }
}

package ir.xenoncommunity.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.antibot.AntibotCheck;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class XenonCord extends Command {
    public static final Set<String> ABstatusPlayers = new HashSet<>();

    public XenonCord() {
        super("xenoncord", XenonCore.instance.getConfigData().getXenoncordperm());
        XenonCore.instance.getTaskManager().repeatingTask(() -> {
            if(XenonCord.ABstatusPlayers.isEmpty()) return;

            AntibotCheck.sendStats();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0L, 1000L, TimeUnit.MILLISECONDS);
    }
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Message.send(sender, "&7This server is using &b&lXenonCord &r&7by &fRealStresser.\nPlease, report bugs on:\nhttps://github.com/SyNdicateFoundation/XenonCord", false);
            return;
        }

        switch (args[0]) {
            case "reload":
                if (!sender.hasPermission(XenonCore.instance.getConfigData().getReloadperm())) return;
                Message.send(sender,
                        XenonCore.instance.getConfigData().getReloadmessage(),
                        true);
                XenonCore.instance.setConfigData(XenonCore.instance.getConfiguration().init());
                Message.send(sender,
                        XenonCore.instance.getConfigData().getReloadcompletemessage(),
                        true);
                break;
            case "antibot":
                if (!Arrays.asList(XenonCore.instance.getConfigData().getModules().getEnables()).contains("Antibot"))
                    return;
                switch (args[1]) {
                    case "stats": {
                        if(ABstatusPlayers.add(sender.getName())) {
                        }else ABstatusPlayers.remove(sender.getName());
                        break;
                    }
                }
                break;
        }
    }
}
// BELOW IS PLANS

//package ir.xenoncommunity.commands;
//
//import net.md_5.bungee.api.CommandSender;
//import net.md_5.bungee.api.ProxyServer;
//import net.md_5.bungee.api.chat.ClickEvent;
//import net.md_5.bungee.api.chat.ComponentBuilder;
//import net.md_5.bungee.api.connection.ProxiedPlayer;
//import net.md_5.bungee.command.PlayerCommand;
//
//import java.util.Collections;
//
//@SuppressWarnings({"unused", "deprecation"})
//public class CommandIP extends PlayerCommand {
//
//    public CommandIP() {
//        super("ip", "bungeecord.command.ip");
//    }
//
//    @Override
//    public void execute(CommandSender sender, String[] args) {
//        if (args.length < 1) {
//            sender.sendMessage(ProxyServer.getInstance().getTranslation("username_needed"));
//            return;
//        }
//        ProxiedPlayer user = ProxyServer.getInstance().getPlayer(args[0]);
//        if (user == null) {
//            sender.sendMessage(ProxyServer.getInstance().getTranslation("user_not_online"));
//        } else {
//            sender.sendMessage(new ComponentBuilder()
//                    .appendLegacy(ProxyServer.getInstance().getTranslation("command_ip", user.getName(), user.getSocketAddress()))
//                    .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, user.getSocketAddress().toString()))
//                    .create()
//            );
//        }
//    }
//
//    @Override
//    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
//        return (args.length == 1) ? super.onTabComplete(sender, args) : Collections.emptyList();
//    }
//}

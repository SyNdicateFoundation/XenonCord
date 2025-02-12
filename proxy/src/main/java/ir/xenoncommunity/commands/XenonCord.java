package ir.xenoncommunity.commands;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.antibot.AntibotCheck;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class XenonCord extends Command {
    public static final Set<String> ABstatusPlayers = new HashSet<>();
    public boolean log = false;

    public XenonCord() {
        super("xenoncord", XenonCore.instance.getConfigData().getXenoncordperm());
        XenonCore.instance.getTaskManager().repeatingTask(() -> {
            if (!log ) return;

            AntibotCheck.log();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 0L, 1000L, TimeUnit.MILLISECONDS);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Message.send(sender, "&7This server is using &b&lXenonCord &r&7by &fRealStresser.&7\nPlease, report bugs on:\nhttps://github.com/SyNdicateFoundation/XenonCord", false);
            return;
        }

        switch (args[0]) {
            case "reload":
                if (!sender.hasPermission(XenonCore.instance.getConfigData().getReloadperm())) return;
                Message.send(sender, XenonCore.instance.getConfigData().getReloadmessage(), true);
                XenonCore.instance.setConfigData(XenonCore.instance.getConfiguration().init());
                Message.send(sender, XenonCore.instance.getConfigData().getReloadcompletemessage(), true);
                break;
            case "antibot":
                if (!Arrays.asList(XenonCore.instance.getConfigData().getModules().getEnables()).contains("Antibot")) return;
                if (args.length < 2) return;
                switch (args[1]) {
                    case "stats":
                        if (sender instanceof ConsoleCommandSender) {
                            log = !log;
                        } else {
                            if (ABstatusPlayers.add(sender.getName())) {
                                log = true;
                            } else {
                                ABstatusPlayers.remove(sender.getName());
                                log = false;
                            }
                        }
                        break;
                }
                break;
        }
    }
}

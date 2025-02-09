package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;

@SuppressWarnings("unused")
@ModuleListener(isExtended = true, isImplemented = false)
public class Ping extends Command {

    public Ping() {
        super("Ping", XenonCore.instance.getConfigData().getPing().getPingperm());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            Message.send(sender, XenonCore.instance.getConfigData().getCannotexecasconsoleerrormessage(), false);
            return;
        }

        if (args.length == 0) sendPingMessage(sender, ((ProxiedPlayer) sender).getPing());
        else sendOtherPlayerPingMessage(sender, args[0]);
    }

    private void sendPingMessage(CommandSender sender, int ping) {
        Message.send(sender, XenonCore.instance.getConfigData().getPing().getPingmessage()
                .replace("PING", String.valueOf(ping)), false);
    }

    private void sendOtherPlayerPingMessage(CommandSender sender, String targetPlayerName) {
        final ProxiedPlayer targetPlayer = XenonCore.instance.getBungeeInstance().getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            Message.send(sender, "Player not found.", false);
            return;
        }

        if (!sender.hasPermission(XenonCore.instance.getConfigData().getPing().getPingothersperm())) {
            return;
        }

        Message.send(sender, XenonCore.instance.getConfigData().getPing().getPingothersmessage()
                .replace("PING", String.valueOf(targetPlayer.getPing()))
                .replace("USERNAME", targetPlayerName), false);
    }
}

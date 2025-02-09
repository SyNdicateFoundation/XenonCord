package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.command.ConsoleCommandSender;

@SuppressWarnings({"unused", "deprecation"})
@UtilityClass
public class Message {
    public void send(CommandSender senderIn, String message, boolean console) {
        final String fmt = ChatColor.translateAlternateColorCodes('&', message);

        if (!(senderIn instanceof ConsoleCommandSender)) {
            senderIn.sendMessage(fmt);
        }

        if (console || senderIn instanceof ConsoleCommandSender) {
            XenonCore.instance.getLogger().info(fmt);
        }
    }

    public void send(String message) {
        XenonCore.instance.getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendNoPermMessage(CommandSender senderIn) {
        if ((senderIn instanceof ConsoleCommandSender)) return;

        senderIn.sendMessage(ChatColor.translateAlternateColorCodes('&',
                XenonCore.instance.getConfigData().getCommandwhitelist().getBlockmessage()));
    }
}

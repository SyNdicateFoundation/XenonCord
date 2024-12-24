package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.command.ConsoleCommandSender;

@UtilityClass public class Message {
    public void send(final CommandSender senderIn, final String message, boolean console){
        if(!(senderIn instanceof ConsoleCommandSender))
            senderIn.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

        if(console || senderIn instanceof ConsoleCommandSender)
            XenonCore.instance.getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
    }
    public void sendNoPermMessage(final CommandSender senderIn){
        if(!(senderIn instanceof ConsoleCommandSender))
            senderIn.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    XenonCore.instance.getConfigData().getCommandwhitelist().getBlockmessage()));
    }
}

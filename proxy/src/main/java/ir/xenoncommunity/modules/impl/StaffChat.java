package ir.xenoncommunity.modules.impl;

import ir.xenoncommunity.XenonCore;
import ir.xenoncommunity.annotations.ModuleListener;
import ir.xenoncommunity.utils.Message;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.command.ConsoleCommandSender;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("unused")
@ModuleListener(isExtended = true, isImplemented = true)
public class StaffChat extends Command implements Listener {
    public static ArrayList<String> toggles;

    public StaffChat() {
        super("staffchat", XenonCore.instance.getConfigData().getStaff_chat().getStaff_chat_perm(), "sc");
        toggles = new ArrayList<>();
    }

    private final String toggleMessage = XenonCore.instance.getConfigData().getStaff_chat().getToggle_message();
    private final String staffChatMessage = XenonCore.instance.getConfigData().getStaff_chat().getStaff_chat_message();
    @Override
    public void execute(CommandSender sender, String[] args) {
        final String senderName = sender.getName();

        if (args.length == 0) {
            if (sender instanceof ConsoleCommandSender) return;

            if (toggles.contains(senderName)) {
                Message.send(sender, toggleMessage
                        .replace("STATE", "disabled"), false);
                toggles.remove(senderName);
            } else {
                Message.send(sender, toggleMessage
                        .replace("STATE", "enabled"), false);
                toggles.add(senderName);
            }

            return;
        }
        final StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(args).forEach(string -> stringBuilder.append(string).append(" "));
        sendMessage(stringBuilder.toString(), senderName);

    }

    @EventHandler
    public void onChat(ChatEvent e) {
        final ProxiedPlayer sender = (ProxiedPlayer) e.getSender();
        final String senderName = sender.getName();

        if (!toggles.contains(senderName)
                || e.getMessage().startsWith("/")) {
            return;
        }

        e.setCancelled(true);

        if (AdminChat.toggles != null && AdminChat.toggles.contains(senderName)) return;

        sendMessage(e.getMessage(), senderName);
    }

    private void sendMessage(String msg, String senderName) {
        XenonCore.instance.getTaskManager().add(() ->
                XenonCore.instance.getBungeeInstance().getPlayers().stream()
                        .filter(proxiedPlayer -> proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getStaff_chat().getStaff_chat_perm()))
                        .forEach(proxiedPlayer -> Message.send(proxiedPlayer,
                                staffChatMessage
                                        .replace("PLAYER", senderName)
                                        .replace("MESSAGE", msg), false)));
        Message.send(staffChatMessage
                .replace("PLAYER", senderName)
                .replace("MESSAGE", msg));
    }
}

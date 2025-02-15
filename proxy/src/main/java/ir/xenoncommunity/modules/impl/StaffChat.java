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

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
@ModuleListener(isExtended = true, isImplemented = true)
public class StaffChat extends Command implements Listener {
    public static final Set<String> toggles = new HashSet<>();

    public StaffChat() {
        super("staffchat", XenonCore.instance.getConfigData().getStaff_chat().getStaff_chat_perm(), "sc");
    }

    private final String staffChatMessage = XenonCore.instance.getConfigData().getStaff_chat().getStaff_chat_message();
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            handleToggle(sender);
            return;
        }
        sendMessage(String.join(" ", args), sender.getName());
    }

    private void handleToggle(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return;

        String senderName = sender.getName();
        boolean isEnabled = toggles.contains(senderName);
        String state = isEnabled ? "disabled" : "enabled";

        Message.send(sender, XenonCore.instance.getConfigData().getStaff_chat().getToggle_message().replace("STATE", state), false);
        if (isEnabled) {
            toggles.remove(senderName);
        } else {
            toggles.add(senderName);
        }
    }

    @EventHandler
    public void onChat(ChatEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)) return;

        final ProxiedPlayer sender = (ProxiedPlayer) e.getSender();
        final String senderName = sender.getName();

        if (!sender.hasPermission(XenonCore.instance.getConfigData().getStaff_chat().getStaff_chat_perm())
                || !toggles.contains(senderName)
                || e.getMessage().startsWith("/")) {
            return;
        }

        if (AdminChat.toggles.contains(senderName)) return;

        sendMessage(e.getMessage(), senderName);

        e.setCancelled(true);
    }

    private void sendMessage(String msg, String senderName) {
        XenonCore.instance.getTaskManager().add(() -> XenonCore.instance.getBungeeInstance().getPlayers().stream()
                .filter(proxiedPlayer -> proxiedPlayer.hasPermission(XenonCore.instance.getConfigData().getStaff_chat().getStaff_chat_perm()))
                .forEach(proxiedPlayer -> Message.send(proxiedPlayer,
                        XenonCore.instance.getConfigData().getStaff_chat().getStaff_chat_message()
                                .replace("PLAYER", senderName)
                                .replace("MESSAGE", msg), false)));

        Message.send(staffChatMessage
                .replace("PLAYER", senderName)
                .replace("MESSAGE", msg));
    }
}
